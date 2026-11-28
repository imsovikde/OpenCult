package org.opencult.scale.core.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
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
import kotlinx.coroutines.CoroutineExceptionHandler
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
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
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
    private val bleContext: Context = BleReceiverCompatContext(context)
    private val bleThread = HandlerThread("OpenCultBleThread").apply {
        uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
            _state.update {
                it.copy(
                    isScanning = false,
                    isPassiveAutoConnecting = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "BLE worker error: ${throwable.message ?: throwable.javaClass.simpleName}",
                    ),
                )
            }
        }
        start()
    }
    private val bleHandler = Handler(bleThread.looper)
    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        _state.update {
            it.copy(
                isScanning = false,
                session = it.session.copy(
                    phase = WeighInPhase.ERROR,
                    errorMessage = "BLE coroutine error: ${throwable.message ?: throwable.javaClass.simpleName}",
                ),
            )
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineErrorHandler)

    private var centralManager: BluetoothCentralManager? = null
    private var activePeripheral: BluetoothPeripheral? = null
    private var stateMachine: ScaleSessionStateMachine? = null
    private var activeAnthropometry: UserAnthropometry? = null
    private var settleTimeoutJob: Job? = null
    private var dfGraceJob: Job? = null
    private var passiveReconnectTargetMacs: Map<String, SavedScaleTarget> = emptyMap()
    private var passiveAnthropometryProvider: (suspend () -> UserAnthropometry)? = null
    private val runstarOutgoingSeq = AtomicInteger(0)
    private val cacheLock = Any()

    private val _state = MutableStateFlow(BleHardwareUiState())
    val state: StateFlow<BleHardwareUiState> = _state.asStateFlow()

    private val discoveredMap = LinkedHashMap<String, DiscoveredScaleDevice>()
    private val peripheralCache = HashMap<String, BluetoothPeripheral>()

    private fun hasBlePermissions(): Boolean {
        val scanGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        val connectGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        return scanGranted && connectGranted
    }

    private fun readAdapterEnabledSafely(): Boolean =
        centralManager?.let { mgr ->
            runCatching { mgr.isBluetoothEnabled }.getOrDefault(false)
        } ?: runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.isEnabled == true
        }.getOrDefault(false)

    fun refreshPermissionAndAdapterState() {
        val granted = hasBlePermissions()
        val btOn = readAdapterEnabledSafely()
        _state.update { it.copy(hasPermissions = granted, isBluetoothEnabled = btOn) }
    }

    @SuppressLint("MissingPermission")
    fun startScan(targetAnthropometry: UserAnthropometry, preferredTier: ScaleTier = ScaleTier.CULT_PRO_8E) {
        refreshPermissionAndAdapterState()
        if (!_state.value.hasPermissions) {
            _state.update {
                it.copy(
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth scan and connect permissions are required.",
                    ),
                )
            }
            return
        }
        val mgr = ensureCentralManager() ?: return
        val btOn = runCatching { mgr.isBluetoothEnabled }.getOrDefault(false)
        if (!btOn) {
            _state.update {
                it.copy(
                    isBluetoothEnabled = false,
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth adapter is disabled. Enable Bluetooth to connect your Cult scale.",
                    ),
                )
            }
            return
        }
        passiveReconnectTargetMacs = emptyMap()
        synchronized(cacheLock) {
            discoveredMap.clear()
        }
        activeAnthropometry = targetAnthropometry
        stateMachine = ScaleSessionStateMachine(targetAnthropometry, preferredTier)
        _state.update {
            it.copy(
                isBluetoothEnabled = true,
                isScanning = true,
                isPassiveAutoConnecting = false,
                discoveredDevices = emptyList(),
                session = ScaleSessionSnapshot(phase = WeighInPhase.SCANNING, detectedTier = preferredTier),
            )
        }
        runCatching {
            mgr.scanForPeripherals()
        }.onFailure { err ->
            _state.update {
                it.copy(
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Could not start BLE scan: ${err.message ?: err.javaClass.simpleName}",
                    ),
                )
            }
        }
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
        val mgr = ensureCentralManager() ?: return

        passiveReconnectTargetMacs = savedTargets.associateBy { it.address.uppercase() }
        passiveAnthropometryProvider = anthropometryProvider
        _state.update { it.copy(isScanning = true, isPassiveAutoConnecting = true) }
        runCatching {
            mgr.scanForPeripherals()
        }.onFailure {
            _state.update { st -> st.copy(isScanning = false, isPassiveAutoConnecting = false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopPassiveAutoReconnect() {
        passiveReconnectTargetMacs = emptyMap()
        if (_state.value.isPassiveAutoConnecting) {
            runCatching { centralManager?.stopScan() }
            _state.update { it.copy(isScanning = false, isPassiveAutoConnecting = false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        runCatching { centralManager?.stopScan() }
        _state.update { it.copy(isScanning = false, isPassiveAutoConnecting = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        address: String,
        anthropometry: UserAnthropometry,
        tier: ScaleTier,
    ) {
        if (!hasBlePermissions()) {
            _state.update {
                it.copy(
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth permissions were revoked. Grant Nearby devices access to connect.",
                    ),
                )
            }
            return
        }
        val peripheral = synchronized(cacheLock) { peripheralCache[address] } ?: return
        val mgr = ensureCentralManager() ?: return
        if (!runCatching { mgr.isBluetoothEnabled }.getOrDefault(false)) {
            _state.update {
                it.copy(
                    isBluetoothEnabled = false,
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth adapter is disabled. Enable Bluetooth to connect your Cult scale.",
                    ),
                )
            }
            return
        }
        stopScan()
        dfGraceJob?.cancel()
        settleTimeoutJob?.cancel()
        activeAnthropometry = anthropometry
        val resolvedName = safePeripheralName(peripheral)
            ?: synchronized(cacheLock) { discoveredMap[address]?.name }
            ?: "Cult Scale"
        val sm = ScaleSessionStateMachine(anthropometry, tier).also { stateMachine = it }
        val snap = sm.markConnecting(resolvedName, address)
        _state.update { it.copy(session = snap) }
        runCatching {
            mgr.connect(peripheral, peripheralCallback)
        }.onFailure { err ->
            _state.update {
                it.copy(
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Could not connect to $resolvedName: ${err.message ?: err.javaClass.simpleName}",
                    ),
                )
            }
        }
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

    private fun safePeripheralName(
        peripheral: BluetoothPeripheral,
        scanResult: ScanResult? = null,
    ): String? {
        val fromPeripheral = runCatching { peripheral.name.takeIf { it.isNotBlank() } }.getOrNull()
        if (fromPeripheral != null) return fromPeripheral
        return runCatching { scanResult?.scanRecord?.deviceName?.takeIf { it.isNotBlank() } }.getOrNull()
    }

    private fun ensureCentralManager(): BluetoothCentralManager? {
        centralManager?.let { return it }
        return runCatching {
            BluetoothCentralManager(bleContext, centralCallback, bleHandler).also {
                centralManager = it
            }
        }.onFailure { err ->
            _state.update {
                it.copy(
                    isScanning = false,
                    session = it.session.copy(
                        phase = WeighInPhase.ERROR,
                        errorMessage = "Bluetooth stack initialization failed: ${err.message ?: err.javaClass.simpleName}",
                    ),
                )
            }
        }.getOrNull()
    }

    internal fun evaluateDiscoveredScale(
        name: String?,
        serviceUuids: List<UUID>,
        address: String,
        rssi: Int,
        savedTarget: SavedScaleTarget?,
    ): DiscoveredScaleDevice? {
        val supported = savedTarget != null ||
            CultBleConstants.isSupportedDeviceName(name) ||
            CultBleConstants.isSupportedScaleService(serviceUuids)
        if (!supported) return null
        val resolvedName = name ?: savedTarget?.advertisedName ?: when {
            CultBleConstants.SERVICE_RUNSTAR_FFB0 in serviceUuids -> "Smart Scale (0xFFB0)"
            CultBleConstants.SERVICE_SCALE in serviceUuids -> "Smart Scale (0xFFF0)"
            else -> "Smart Body Scale"
        }
        val tier = savedTarget?.tier ?: CultBleConstants.suggestedTierForDiscovery(name, serviceUuids)
        return DiscoveredScaleDevice(
            name = resolvedName,
            address = address,
            rssi = rssi,
            suggestedTier = tier,
        )
    }

    private val centralCallback = object : BluetoothCentralManagerCallback() {
        override fun onDiscovered(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
            val name = safePeripheralName(peripheral, scanResult)
            val serviceUuids = runCatching {
                scanResult.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
            }.getOrDefault(emptyList())
            val addrUpper = peripheral.address.uppercase()
            val savedTarget = passiveReconnectTargetMacs[addrUpper]
            val device = evaluateDiscoveredScale(
                name = name,
                serviceUuids = serviceUuids,
                address = peripheral.address,
                rssi = scanResult.rssi,
                savedTarget = savedTarget,
            ) ?: return

            val devicesSnapshot = synchronized(cacheLock) {
                peripheralCache[peripheral.address] = peripheral
                val existing = discoveredMap[peripheral.address]
                val finalName = if (name != null) name else existing?.name ?: device.name
                discoveredMap[peripheral.address] = device.copy(name = finalName)
                discoveredMap.values.toList()
            }
            _state.update { it.copy(discoveredDevices = devicesSnapshot) }

            if (savedTarget != null && _state.value.isPassiveAutoConnecting) {
                passiveReconnectTargetMacs = emptyMap()
                val provider = passiveAnthropometryProvider ?: return
                scope.launch {
                    val anthro = provider()
                    connectToDevice(peripheral.address, anthro, device.suggestedTier)
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

    private fun resolveWriteType(characteristic: BluetoothGattCharacteristic?): WriteType {
        val props = characteristic?.properties ?: return WriteType.WITHOUT_RESPONSE
        return if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            WriteType.WITHOUT_RESPONSE
        } else if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            WriteType.WITH_RESPONSE
        } else {
            WriteType.WITHOUT_RESPONSE
        }
    }

    private val peripheralCallback = object : BluetoothPeripheralCallback() {
        override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
            runCatching {
                val services = runCatching { peripheral.services }.getOrDefault(emptyList())
                val fff0Service = services.firstOrNull { it.uuid == CultBleConstants.SERVICE_SCALE }
                val ffb0Service = services.firstOrNull { it.uuid == CultBleConstants.SERVICE_RUNSTAR_FFB0 }

                if (fff0Service != null) {
                    val notifyChar = peripheral.getCharacteristic(
                        CultBleConstants.SERVICE_SCALE,
                        CultBleConstants.CHAR_NOTIFY,
                    )
                    if (notifyChar != null) {
                        peripheral.observe(notifyChar, true) { bytes ->
                            handleNotification(peripheral, CultBleConstants.CHAR_NOTIFY_SHORT, bytes)
                        }
                    }

                    val writeChar = peripheral.getCharacteristic(
                        CultBleConstants.SERVICE_SCALE,
                        CultBleConstants.CHAR_WRITE,
                    )
                    if (writeChar != null) {
                        val writeType = resolveWriteType(writeChar)
                        peripheral.writeCharacteristic(
                            writeChar,
                            CultBleConstants.buildModeSetupCommand(unitCode = CultBleConstants.UNIT_KG),
                            writeType,
                        )
                        val zdt = Instant.now().atZone(ZoneId.systemDefault())
                        peripheral.writeCharacteristic(
                            writeChar,
                            CultBleConstants.buildRtcSyncCommand(
                                year = zdt.year,
                                month = zdt.monthValue,
                                day = zdt.dayOfMonth,
                                hour = zdt.hour,
                                minute = zdt.minute,
                                second = zdt.second,
                            ),
                            writeType,
                        )
                        val tier = stateMachine?.current()?.detectedTier
                        val devName = safePeripheralName(peripheral)
                        if (tier == ScaleTier.CULT_BF01_4E || CultBleConstants.isOneByoneOrBf01DeviceName(devName)) {
                            settleTimeoutJob?.cancel()
                            settleTimeoutJob = scope.launch {
                                delay(CultBleConstants.HANDSHAKE_SETTLE_DELAY_MS)
                                runCatching {
                                    peripheral.writeCharacteristic(
                                        writeChar,
                                        CultBleConstants.buildRequestHistoryCommand(),
                                        writeType,
                                    )
                                }
                            }
                        }
                    }
                }

                if (ffb0Service != null) {
                    peripheral.getCharacteristic(
                        CultBleConstants.SERVICE_RUNSTAR_FFB0,
                        CultBleConstants.CHAR_RUNSTAR_LIVE,
                    )?.let { liveChar ->
                        peripheral.observe(liveChar, true) { bytes ->
                            handleNotification(peripheral, CultBleConstants.CHAR_RUNSTAR_LIVE_SHORT, bytes)
                        }
                    }
                    peripheral.getCharacteristic(
                        CultBleConstants.SERVICE_RUNSTAR_FFB0,
                        CultBleConstants.CHAR_RUNSTAR_RESULT,
                    )?.let { resultChar ->
                        peripheral.observe(resultChar, true) { bytes ->
                            handleNotification(peripheral, CultBleConstants.CHAR_RUNSTAR_RESULT_SHORT, bytes)
                        }
                    }
                }

                if (fff0Service == null && ffb0Service == null) {
                    services.forEach { svc ->
                        svc.characteristics.orEmpty().forEach { ch ->
                            val canNotify = (ch.properties and
                                (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                            if (canNotify) {
                                val shortUuid = ch.uuid.toString().take(8)
                                runCatching {
                                    peripheral.observe(ch, true) { bytes ->
                                        handleNotification(peripheral, shortUuid, bytes)
                                    }
                                }
                            }
                        }
                    }
                }

                peripheral.getCharacteristic(
                    CultBleConstants.SERVICE_BATTERY,
                    CultBleConstants.CHAR_BATTERY_LEVEL,
                )?.let { batteryChar ->
                    peripheral.readCharacteristic(batteryChar) { bytes, status ->
                        if (status == GattStatus.SUCCESS && bytes.isNotEmpty()) {
                            val pct = bytes[0].toInt() and 0xFF
                            stateMachine?.let { sm -> _state.update { it.copy(session = sm.onBatteryRead(pct)) } }
                        }
                    }
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        session = it.session.copy(
                            phase = WeighInPhase.ERROR,
                            errorMessage = "GATT service setup failed: ${err.message ?: err.javaClass.simpleName}",
                        ),
                    )
                }
            }
        }

        private fun handleNotification(
            peripheral: BluetoothPeripheral,
            charUuid: String,
            bytes: ByteArray,
        ) {
            val sm = stateMachine ?: return
            if (charUuid == CultBleConstants.CHAR_RUNSTAR_LIVE_SHORT ||
                charUuid == CultBleConstants.CHAR_RUNSTAR_RESULT_SHORT
            ) {
                maybeAckRunstarFrame(peripheral, bytes)
            }
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

        private fun maybeAckRunstarFrame(peripheral: BluetoothPeripheral, bytes: ByteArray) {
            if (bytes.size != 20) return
            val checksumValid = (bytes[19].toInt() and 0x1F) == CultBleConstants.runstarChecksum(bytes)
            if (!checksumValid) return
            val partFlag = bytes[2].toInt() and 0xFF
            val frameType = bytes[3].toInt() and 0xFF
            val needsAck = partFlag == 0x01 || (partFlag == 0x00 && (frameType == 0xA1 || frameType == 0xA3))
            if (!needsAck) return
            val writeChar = runCatching {
                peripheral.getCharacteristic(
                    CultBleConstants.SERVICE_RUNSTAR_FFB0,
                    CultBleConstants.CHAR_RUNSTAR_WRITE,
                )
            }.getOrNull() ?: return
            val writeType = resolveWriteType(writeChar)
            val seqToAck = bytes[0].toInt() and 0xFF
            runCatching {
                peripheral.writeCharacteristic(
                    writeChar,
                    CultBleConstants.buildRunstarAck(
                        seqToAck = seqToAck,
                        outgoingSeq = runstarOutgoingSeq.getAndIncrement(),
                    ),
                    writeType,
                )
                if (partFlag == 0x00 && frameType == 0xA1) {
                    val anthro = activeAnthropometry
                    if (anthro != null) {
                        peripheral.writeCharacteristic(
                            writeChar,
                            CultBleConstants.buildRunstarUserInfoFrame(
                                outgoingSeq = runstarOutgoingSeq.getAndIncrement(),
                                epochSeconds = System.currentTimeMillis() / 1000L,
                                heightCm = anthro.heightCm.toInt(),
                                ageYears = anthro.ageYears,
                                isMale = anthro.sex == BiologicalSex.MALE,
                            ),
                            writeType,
                        )
                    }
                }
            }
        }
    }
}
