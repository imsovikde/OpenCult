package org.opencult.scale.core.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.HciStatus
import com.welie.blessed.ScanFailure
import com.welie.blessed.WriteType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredScaleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val suggestedTier: ScaleTier,
)

data class SavedScaleTarget(
    val address: String,
    val advertisedName: String,
    val tier: ScaleTier,
)

data class BleHardwareUiState(
    val hasPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isPassiveAutoConnecting: Boolean = false,
    val discoveredDevices: List<DiscoveredScaleDevice> = emptyList(),
    val session: ScaleSessionSnapshot = ScaleSessionSnapshot(),
)

/**
 * Coordinates `blessed-kotlin` GATT scanning, `0xFFF0`/`0xFFF1`/`0xFFF4` handshake,
 * raw frame recording, foreground lifecycle-gated passive reconnect to known Cult MACs,
 * and `ScaleSessionStateMachine` lifecycle (`1200 ms` 0xDF grace deadline for INV-01)
 * on a dedicated BLE looper thread.
 */
@Singleton
class CultBleController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bleThread = HandlerThread("OpenCultBleThread").apply { start() }
    private val bleHandler = Handler(bleThread.looper)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var centralManager: BluetoothCentralManager? = null
    private var activePeripheral: BluetoothPeripheral? = null
    private var stateMachine: ScaleSessionStateMachine? = null
    private var settleTimeoutJob: Job? = null
    private var dfGraceJob: Job? = null
    private var passiveReconnectTargetMacs: Map<String, SavedScaleTarget> = emptyMap()
    private var passiveAnthropometryProvider: (suspend () -> UserAnthropometry)? = null

    private val _state = MutableStateFlow(BleHardwareUiState())
    val state: StateFlow<BleHardwareUiState> = _state.asStateFlow()

    private val discoveredMap = LinkedHashMap<String, DiscoveredScaleDevice>()
    private val peripheralCache = HashMap<String, BluetoothPeripheral>()

    fun refreshPermissionAndAdapterState() {
        val scanGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        val connectGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        val granted = scanGranted && connectGranted
        val mgr = ensureCentralManager()
        val btOn = if (granted) runCatching { mgr.isBluetoothEnabled }.getOrDefault(false) else false
        _state.update { it.copy(hasPermissions = granted, isBluetoothEnabled = btOn) }
    }

    @SuppressLint("MissingPermission")
    fun startScan(targetAnthropometry: UserAnthropometry, preferredTier: ScaleTier = ScaleTier.CULT_PRO_8E) {
        refreshPermissionAndAdapterState()
        if (!_state.value.hasPermissions) {
            _state.update {
                it.copy(
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth scan and connect permissions are required.",
                    ),
                )
            }
            return
        }
        val mgr = ensureCentralManager()
        if (!mgr.isBluetoothEnabled) {
            _state.update {
                it.copy(
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth adapter is disabled. Enable Bluetooth to connect your Cult scale.",
                    ),
                )
            }
            return
        }
        passiveReconnectTargetMacs = emptyMap()
        discoveredMap.clear()
        stateMachine = ScaleSessionStateMachine(targetAnthropometry, preferredTier)
        _state.update {
            it.copy(
                isScanning = true,
                isPassiveAutoConnecting = false,
                discoveredDevices = emptyList(),
                session = ScaleSessionSnapshot(phase = WeighInPhase.SCANNING, detectedTier = preferredTier),
            )
        }
        mgr.scanForPeripherals()
    }

    /**
     * Starts passive foreground-lifecycle-gated scanning for known saved Cult MACs (`INV-20`).
     * Automatically connects when any saved Cult scale wakes up when the user steps onto it.
     */
    @SuppressLint("MissingPermission")
    fun startPassiveAutoReconnect(
        savedTargets: List<SavedScaleTarget>,
        anthropometryProvider: suspend () -> UserAnthropometry,
    ) {
        if (savedTargets.isEmpty()) return
        val phase = _state.value.session.phase
        if (phase != WeighInPhase.IDLE && phase != WeighInPhase.ERROR) return
        refreshPermissionAndAdapterState()
        if (!_state.value.hasPermissions || !_state.value.isBluetoothEnabled) return

        passiveReconnectTargetMacs = savedTargets.associateBy { it.address.uppercase() }
        passiveAnthropometryProvider = anthropometryProvider
        _state.update { it.copy(isScanning = true, isPassiveAutoConnecting = true) }
        ensureCentralManager().scanForPeripherals()
    }

    @SuppressLint("MissingPermission")
    fun stopPassiveAutoReconnect() {
        passiveReconnectTargetMacs = emptyMap()
        if (_state.value.isPassiveAutoConnecting) {
            centralManager?.stopScan()
            _state.update { it.copy(isScanning = false, isPassiveAutoConnecting = false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        centralManager?.stopScan()
        _state.update { it.copy(isScanning = false, isPassiveAutoConnecting = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        address: String,
        anthropometry: UserAnthropometry,
        tier: ScaleTier,
    ) {
        val peripheral = peripheralCache[address] ?: return
        stopScan()
        dfGraceJob?.cancel()
        settleTimeoutJob?.cancel()
        val sm = ScaleSessionStateMachine(anthropometry, tier).also { stateMachine = it }
        val snap = sm.markConnecting(peripheral.name.ifBlank { "Cult Scale" }, address)
        _state.update { it.copy(session = snap) }
        ensureCentralManager().connect(peripheral, peripheralCallback)
    }

    fun confirmFeetOnlyReading() {
        val sm = stateMachine ?: return
        settleTimeoutJob?.cancel()
        dfGraceJob?.cancel()
        val snap = sm.confirmFeetOnlyAfterTimeout()
        _state.update { it.copy(session = snap) }
    }

    /**
     * Replays an exact captured BLE packet sequence deterministically (usable ONLY from the
     * debug/live-inspector harness when no physical scale is on hand — never mislabeled hardware).
     */
    fun replayFixtureSequence(
        anthropometry: UserAnthropometry,
        tier: ScaleTier,
        deviceName: String,
        deviceMac: String,
        frames: List<Pair<String, ByteArray>>,
    ) {
        stopScan()
        dfGraceJob?.cancel()
        settleTimeoutJob?.cancel()
        val sm = ScaleSessionStateMachine(anthropometry, tier).also { stateMachine = it }
        sm.markConnecting(deviceName, deviceMac)
        sm.markConnected()
        sm.onBatteryRead(86)
        scope.launch {
            var latest = sm.current()
            _state.update { it.copy(session = latest) }
            frames.forEach { (charUuid, bytes) ->
                delay(180L)
                latest = sm.onRawPacket(charUuid, bytes)
                _state.update { it.copy(session = latest) }
            }
            if (sm.has8EImpedanceReadyAndAwaitingDf()) {
                delay(CultBleConstants.DF_GRACE_DEADLINE_MS)
                _state.update { it.copy(session = sm.complete8EOnGraceDeadline()) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectAndReset() {
        settleTimeoutJob?.cancel()
        dfGraceJob?.cancel()
        passiveReconnectTargetMacs = emptyMap()
        runCatching { stopScan() }
        activePeripheral?.let { runCatching { centralManager?.cancelConnection(it) } }
        activePeripheral = null
        stateMachine = null
        _state.update { it.copy(session = ScaleSessionSnapshot()) }
    }

    private fun ensureCentralManager(): BluetoothCentralManager =
        centralManager ?: BluetoothCentralManager(context, centralCallback, bleHandler).also {
            centralManager = it
        }

    private val centralCallback = object : BluetoothCentralManagerCallback() {
        override fun onDiscovered(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
            val name = peripheral.name.takeIf { it.isNotBlank() } ?: scanResult.scanRecord?.deviceName
            val addrUpper = peripheral.address.uppercase()
            val savedTarget = passiveReconnectTargetMacs[addrUpper]
            if (savedTarget == null && !CultBleConstants.isSupportedDeviceName(name)) return

            val resolvedName = name ?: savedTarget?.advertisedName ?: "CULT Scale"
            val tier = savedTarget?.tier ?: when {
                CultBleConstants.isOneByoneOrBf01DeviceName(resolvedName) -> ScaleTier.CULT_BF01_4E
                else -> ScaleTier.CULT_PRO_8E
            }
            peripheralCache[peripheral.address] = peripheral
            discoveredMap[peripheral.address] = DiscoveredScaleDevice(
                name = resolvedName,
                address = peripheral.address,
                rssi = scanResult.rssi,
                suggestedTier = tier,
            )
            _state.update { it.copy(discoveredDevices = discoveredMap.values.toList()) }

            if (savedTarget != null && _state.value.isPassiveAutoConnecting) {
                passiveReconnectTargetMacs = emptyMap()
                val provider = passiveAnthropometryProvider ?: return
                scope.launch {
                    val anthro = provider()
                    connectToDevice(peripheral.address, anthro, tier)
                }
            }
        }

        override fun onConnected(peripheral: BluetoothPeripheral) {
            activePeripheral = peripheral
            stateMachine?.let { sm ->
                _state.update { it.copy(session = sm.markConnected()) }
            }
        }

        override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
            _state.update {
                it.copy(
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Connection failed (${status.name})",
                    ),
                )
            }
        }

        override fun onDisconnected(peripheral: BluetoothPeripheral, status: HciStatus) {
            activePeripheral = null
            settleTimeoutJob?.cancel()
            dfGraceJob?.cancel()
            stateMachine?.let { sm ->
                val cur = sm.current()
                if (cur.completedReport == null && cur.liveWeightKg != null) {
                    val finalized = if (sm.has8EImpedanceReadyAndAwaitingDf()) {
                        sm.complete8EOnGraceDeadline()
                    } else {
                        sm.confirmFeetOnlyAfterTimeout()
                    }
                    _state.update { it.copy(session = finalized) }
                }
            }
        }

        override fun onScanFailed(scanFailure: ScanFailure) {
            _state.update {
                it.copy(
                    isScanning = false,
                    isPassiveAutoConnecting = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "BLE scan failed (${scanFailure.name})",
                    ),
                )
            }
        }
    }

    private val peripheralCallback = object : BluetoothPeripheralCallback() {
        override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
            peripheral.observe(
                CultBleConstants.SERVICE_SCALE,
                CultBleConstants.CHAR_NOTIFY,
                true,
            ) { bytes ->
                handleNotification(CultBleConstants.CHAR_NOTIFY_SHORT, bytes)
            }

            peripheral.readCharacteristic(
                CultBleConstants.SERVICE_BATTERY,
                CultBleConstants.CHAR_BATTERY_LEVEL,
            ) { bytes, status ->
                if (status == GattStatus.SUCCESS && bytes.isNotEmpty()) {
                    val pct = bytes[0].toInt() and 0xFF
                    stateMachine?.let { sm -> _state.update { it.copy(session = sm.onBatteryRead(pct)) } }
                }
            }

            peripheral.writeCharacteristic(
                CultBleConstants.SERVICE_SCALE,
                CultBleConstants.CHAR_WRITE,
                CultBleConstants.buildModeSetupCommand(unitCode = CultBleConstants.UNIT_KG),
                WriteType.WITHOUT_RESPONSE,
            )

            val zdt = Instant.now().atZone(ZoneId.systemDefault())
            peripheral.writeCharacteristic(
                CultBleConstants.SERVICE_SCALE,
                CultBleConstants.CHAR_WRITE,
                CultBleConstants.buildRtcSyncCommand(
                    year = zdt.year,
                    month = zdt.monthValue,
                    day = zdt.dayOfMonth,
                    hour = zdt.hour,
                    minute = zdt.minute,
                    second = zdt.second,
                ),
                WriteType.WITHOUT_RESPONSE,
            )

            // Only BF01 / OneByone hardware uses F2 00 history queries; Cult Smart Scale Pro streams automatically
            val tier = stateMachine?.current()?.detectedTier
            if (tier == ScaleTier.CULT_BF01_4E || CultBleConstants.isOneByoneOrBf01DeviceName(peripheral.name)) {
                settleTimeoutJob?.cancel()
                settleTimeoutJob = scope.launch {
                    delay(CultBleConstants.HANDSHAKE_SETTLE_DELAY_MS)
                    peripheral.writeCharacteristic(
                        CultBleConstants.SERVICE_SCALE,
                        CultBleConstants.CHAR_WRITE,
                        CultBleConstants.buildRequestHistoryCommand(),
                        WriteType.WITHOUT_RESPONSE,
                    )
                }
            }
        }

        private fun handleNotification(charUuid: String, bytes: ByteArray) {
            val sm = stateMachine ?: return
            val updated = sm.onRawPacket(charUuid, bytes)
            _state.update { it.copy(session = updated) }

            if (updated.completedReport != null) {
                dfGraceJob?.cancel()
                settleTimeoutJob?.cancel()
                return
            }

            if (sm.has8EImpedanceReadyAndAwaitingDf()) {
                if (dfGraceJob?.isActive != true) {
                    dfGraceJob = scope.launch {
                        delay(CultBleConstants.DF_GRACE_DEADLINE_MS)
                        _state.update { it.copy(session = sm.complete8EOnGraceDeadline()) }
                    }
                }
            } else if (updated.phase == WeighInPhase.FEET_ONLY_LOCKED) {
                settleTimeoutJob?.cancel()
                settleTimeoutJob = scope.launch {
                    delay(CultBleConstants.HANDSHAKE_SETTLE_DELAY_MS)
                    if (stateMachine?.current()?.phase == WeighInPhase.FEET_ONLY_LOCKED) {
                        confirmFeetOnlyReading()
                    }
                }
            }
        }
    }
}
