package org.opencult.scale.feature.weighin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.opencult.scale.core.audio.OpenCultSoundCue
import org.opencult.scale.core.audio.OpenCultSoundEngine
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.ble.CultBleConstants
import org.opencult.scale.core.ble.CultBleController
import org.opencult.scale.core.ble.WeighInPhase
import org.opencult.scale.core.ble.hexToByteArray
import org.opencult.scale.core.database.HouseholdMatchOutcome
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.RawPacketDraft
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorSheet
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SupabaseSyncEngine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class WeighInMemberContext(
    val activeProfile: HouseholdProfileEntity? = null,
    val memberLockEnabled: Boolean = false,
)

@HiltViewModel
class WeighInViewModel @Inject constructor(
    private val bleController: CultBleController,
    private val repository: OpenCultRepository,
    private val db: OpenCultDatabase,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
    private val activeMemberSession: ActiveMemberSession,
    private val soundEngine: OpenCultSoundEngine,
) : ViewModel() {

    val bleState = bleController.state

    val memberContext: StateFlow<WeighInMemberContext> = combine(
        repository.observeActiveProfiles(),
        activeMemberSession.activeProfileId,
        activeMemberSession.memberLockEnabled,
    ) { profiles, activeId, locked ->
        val chosen = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        WeighInMemberContext(chosen, locked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeighInMemberContext())

    fun setMemberLockEnabled(enabled: Boolean) {
        activeMemberSession.setMemberLockEnabled(enabled)
    }

    fun playCue(cue: OpenCultSoundCue) {
        soundEngine.play(cue)
    }

    suspend fun getActiveAnthropometry(): org.opencult.scale.core.bia.UserAnthropometry {
        val activeId = activeMemberSession.activeProfileId.value
        val profiles = db.householdProfileDao().getActiveProfiles()
        val p = profiles.firstOrNull { it.id == activeId }
            ?: profiles.firstOrNull()
            ?: repository.ensureDefaultProfileIfEmpty(syncEngine.currentAccountIdOrGuest())
        return with(repository) { p.toAnthropometry() }
    }

    fun startRealBleScan(tier: ScaleTier, anthro: org.opencult.scale.core.bia.UserAnthropometry) {
        bleController.startScan(anthro, tier)
    }

    fun connectToDiscovered(address: String, tier: ScaleTier, anthro: org.opencult.scale.core.bia.UserAnthropometry) {
        bleController.connectToDevice(address, anthro, tier)
    }

    fun confirmFeetOnly() {
        bleController.confirmFeetOnlyReading()
    }

    /** Replays verified byte vectors for local QA verification when no physical scale is nearby. */
    fun launchSimulatedFixture(tier: ScaleTier, anthro: org.opencult.scale.core.bia.UserAnthropometry) {
        val frames = when (tier) {
            ScaleTier.CULT_PRO_8E -> {
                val settling = buildCfFrame(74.8, 0, 0x01)
                val handle = buildCfFrame(74.8, 68, 0x08)
                val be0 = "be 00 02 0c 29 0c 0c 01 75 09 02 0a ec".hexToByteArray()
                val be1 = "be 01 74 0a c3 0a f5 00 8f 08 f7 08 85".hexToByteArray()
                val dfFrames = (1..4).map { seq ->
                    ByteArray(17) { idx ->
                        when (idx) {
                            0 -> 0xDF.toByte()
                            2 -> seq.toByte()
                            3 -> 0x04
                            else -> (idx * 5 + seq).toByte()
                        }
                    }.also { fixXor(it) }
                }
                buildList {
                    add(CultBleConstants.CHAR_NOTIFY_SHORT to settling)
                    add(CultBleConstants.CHAR_NOTIFY_SHORT to handle)
                    add(CultBleConstants.CHAR_NOTIFY_SHORT to be0)
                    add(CultBleConstants.CHAR_NOTIFY_SHORT to be1)
                    dfFrames.forEach { df -> add(CultBleConstants.CHAR_NOTIFY_SHORT to df) }
                }
            }
            ScaleTier.CULT_BF01_4E -> {
                val coalesced = "CF 76 0C 1E 28 DF D6 07 01 00 8C CF 76 0C 1E 28 DF D6 07 01".hexToByteArray()
                listOf(
                    CultBleConstants.CHAR_NOTIFY_SHORT to coalesced,
                    CultBleConstants.CHAR_NOTIFY_SHORT to coalesced,
                    CultBleConstants.CHAR_NOTIFY_SHORT to coalesced,
                )
            }
            else -> {
                val settling = buildCfFrame(71.4, 0, 0x01)
                val feetOnly = buildCfFrame(71.4, 70, 0x00)
                listOf(
                    CultBleConstants.CHAR_NOTIFY_SHORT to settling,
                    CultBleConstants.CHAR_NOTIFY_SHORT to feetOnly,
                )
            }
        }
        bleController.replayFixtureSequence(
            anthropometry = anthro,
            tier = tier,
            deviceName = "SIM_CULT_${tier.name}",
            deviceMac = "SIM-00:11:22:33:44:55",
            frames = frames,
        )
    }

    suspend fun commitCompletedSession(overrideProfileId: String? = null): Pair<String, List<HouseholdProfileEntity>?> {
        val snap = bleController.state.value.session
        val report = snap.completedReport ?: error("No completed report in session")
        val accountId = syncEngine.currentAccountIdOrGuest()
        val now = System.currentTimeMillis()

        val deviceEntityId = snap.deviceMac.takeIf { it.isNotBlank() && !it.startsWith("SIM-") }?.let { mac ->
            val existing = db.scaleDeviceDao().findByMac(accountId, mac)
            val id = existing?.id ?: UUID.randomUUID().toString()
            repository.upsertScaleDevice(
                ScaleDeviceEntity(
                    id = id,
                    accountId = accountId,
                    bleMacOrId = mac,
                    advertisedName = snap.deviceName.ifBlank { "Cult Scale" },
                    modelTier = if (snap.detectedTier == ScaleTier.CULT_BF01_4E) "cult_bf01_4e" else "cult_pro_8e",
                    electrodeCount = if (snap.detectedTier == ScaleTier.CULT_BF01_4E) 4 else 8,
                    lastBatteryPct = snap.batteryPct,
                    lastSeenAtMs = now,
                    createdAtMs = existing?.createdAtMs ?: now,
                    updatedAtMs = now,
                ),
            )
            id
        }

        val ctx = memberContext.value
        val lockedProfile = if (ctx.memberLockEnabled && overrideProfileId == null) {
            ctx.activeProfile
        } else {
            null
        }

        val outcome = when {
            overrideProfileId != null -> {
                val chosen = db.householdProfileDao().getById(overrideProfileId)
                if (chosen != null) HouseholdMatchOutcome.Unique(chosen) else repository.evaluateMatchForWeight(report.weightKg)
            }
            lockedProfile != null -> HouseholdMatchOutcome.Unique(lockedProfile)
            else -> repository.evaluateMatchForWeight(report.weightKg)
        }

        val targetProfile = when (outcome) {
            is HouseholdMatchOutcome.Unique -> outcome.profile
            is HouseholdMatchOutcome.Ambiguous -> outcome.fallbackProfile
        }

        val rawDrafts = snap.capturedFrames.map {
            RawPacketDraft(
                seqNo = it.seqNo,
                characteristicUuid = it.characteristicUuid,
                packetTypeHex = it.packetTypeHex,
                payloadHex = it.payloadHex,
                checksumValid = it.checksumValid,
                decodedImpedancesOhm = it.decodedImpedancesOhm,
                capturedAtMs = it.capturedAtEpochMs,
            )
        }

        val measurement = repository.commitMeasurementAtomic(
            accountId = accountId,
            targetProfileId = targetProfile.id,
            assignmentState = outcome.assignmentState,
            scaleDeviceId = deviceEntityId,
            report = report,
            measuredAtMs = now,
            imp50KhzOhm = report.z50KhzOhm,
            impHighOhm = report.zHighKhzOhm,
            rawPackets = rawDrafts,
            notes = if (snap.deviceMac.startsWith("SIM-")) "Simulated Replay Fixture" else null,
        )

        if (outcome is HouseholdMatchOutcome.Unique) {
            healthConnectGateway.syncMeasurement(measurement, targetProfile, snap.deviceName.ifBlank { "Cult Scale" })
        } else {
            soundEngine.play(OpenCultSoundCue.AMBIGUOUS_MEMBER_PROMPT)
        }
        syncEngine.enqueueOutboxDrain()

        val candidates = (outcome as? HouseholdMatchOutcome.Ambiguous)?.candidateProfiles
        return measurement.id to candidates
    }

    suspend fun resolveAmbiguousMember(measurementId: String, profileId: String) {
        healthConnectGateway.reassignMeasurementAndSyncHealthConnect(measurementId, profileId)
        syncEngine.enqueueOutboxDrain()
    }

    fun resetBle() {
        bleController.disconnectAndReset()
    }

    private fun buildCfFrame(weightKg: Double, b1: Int, status: Int): ByteArray {
        val rawW = (weightKg * 100.0).toInt()
        val arr = byteArrayOf(
            0xCF.toByte(),
            b1.toByte(),
            0x00,
            (rawW and 0xFF).toByte(),
            ((rawW shr 8) and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x00,
            status.toByte(),
            0x00,
        )
        fixXor(arr)
        return arr
    }

    private fun fixXor(arr: ByteArray) {
        var acc = 0
        for (i in 0 until arr.lastIndex) acc = acc xor (arr[i].toInt() and 0xFF)
        arr[arr.lastIndex] = acc.toByte()
    }
}

@Composable
fun WeighInScreen(
    onRequestBlePermissions: () -> Unit,
    onClose: () -> Unit,
    onMeasurementCommitted: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeighInViewModel = hiltViewModel(),
) {
    val bleState by viewModel.bleState.collectAsStateWithLifecycle()
    val memberContext by viewModel.memberContext.collectAsStateWithLifecycle()
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var selectedTier by remember { mutableStateOf(ScaleTier.CULT_PRO_8E) }
    var ambiguousChooserState by remember { mutableStateOf<Pair<String, List<HouseholdProfileEntity>>?>(null) }
    var isCommitting by remember { mutableStateOf(false) }

    var prevPhase by remember { mutableStateOf(WeighInPhase.IDLE) }
    var prevStreak by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.resetBle()
    }

    // Clinical Audio Cue triggers bound strictly to state transitions
    LaunchedEffect(bleState.session.phase, bleState.session.stableFrameCount) {
        val s = bleState.session
        if (s.phase != prevPhase) {
            when (s.phase) {
                WeighInPhase.STEP_ON -> viewModel.playCue(OpenCultSoundCue.SCALE_CONNECTED)
                WeighInPhase.HOLD_HANDLES_BIA, WeighInPhase.FEET_ONLY_LOCKED -> {
                    if (prevPhase == WeighInPhase.WEIGHING_SETTLING || prevPhase == WeighInPhase.STEP_ON) {
                        viewModel.playCue(OpenCultSoundCue.WEIGHT_LOCKED)
                    }
                }
                WeighInPhase.LOCKED_COMPLETE -> {
                    val cue = if (s.completedReport?.effectiveTier == ScaleTier.CULT_PRO_8E) {
                        OpenCultSoundCue.METRICS_LOGGED_PRO_50
                    } else {
                        OpenCultSoundCue.METRICS_LOGGED_BASIC_20
                    }
                    viewModel.playCue(cue)
                }
                WeighInPhase.ERROR -> viewModel.playCue(OpenCultSoundCue.CAPTURE_ERROR)
                else -> Unit
            }
            prevPhase = s.phase
        } else if (s.phase == WeighInPhase.WEIGHING_SETTLING && s.stableFrameCount > prevStreak) {
            viewModel.playCue(OpenCultSoundCue.SETTLING_TICK)
        }
        prevStreak = s.stableFrameCount
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 14.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Live Weigh-In",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close weigh-in",
                tint = colors.textPrimary,
                modifier = Modifier
                    .openCultMicroPress(role = Role.Button) {
                        viewModel.resetBle()
                        onClose()
                    }
                    .padding(10.dp),
            )
        }

        // Prominent Lock to Active Member control (Defect #10)
        OpenCultBentoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (memberContext.memberLockEnabled) colors.cobaltSubtle else colors.cardSurface,
            borderColor = if (memberContext.memberLockEnabled) colors.brandCobalt else colors.borderSubtle,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lock to Active Member (${memberContext.activeProfile?.displayName ?: "Primary"})",
                        style = OpenCultTheme.typography.buttonLabel,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = if (memberContext.memberLockEnabled) {
                            "Bypasses weight-window auto-match and attributes this weigh-in straight to ${memberContext.activeProfile?.displayName ?: "active member"}."
                        } else {
                            "Auto-match picks household member by rolling baseline weight."
                        },
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = memberContext.memberLockEnabled,
                    onCheckedChange = { viewModel.setMemberLockEnabled(it) },
                )
            }
        }

        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(ScaleTier.CULT_PRO_8E, "Cult Pro (8E)"),
                PillOption(ScaleTier.CULT_BF01_4E, "CS-BF01 (4E)"),
            ),
            selected = if (selectedTier == ScaleTier.CULT_BF01_4E) ScaleTier.CULT_BF01_4E else ScaleTier.CULT_PRO_8E,
            onSelect = { selectedTier = it },
        )

        val session = bleState.session
        OpenCultBentoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.elevatedSurface,
            shape = OpenCultTheme.shapes.heroSquircle,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center,
            ) {
                AmbientRadarRings(
                    active = session.phase != WeighInPhase.IDLE && session.phase != WeighInPhase.LOCKED_COMPLETE,
                    reducedMotion = reducedMotion,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = session.liveWeightKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "Ready",
                        style = OpenCultTheme.typography.heroMetric,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    OpenCultStatusChip(
                        label = when (session.phase) {
                            WeighInPhase.IDLE -> "Choose Scan or Replay"
                            WeighInPhase.SCANNING -> "Scanning GATT 0xFFF0..."
                            WeighInPhase.CONNECTING -> "Connecting..."
                            WeighInPhase.STEP_ON -> "Step onto Scale"
                            WeighInPhase.WEIGHING_SETTLING -> "Settling (${session.stableFrameCount}/3)..."
                            WeighInPhase.HOLD_HANDLES_BIA -> "8E BIA (${session.dfPacketCount}/4 0xDF)"
                            WeighInPhase.FEET_ONLY_LOCKED -> "Weight Locked (Feet-Only)"
                            WeighInPhase.LOCKED_COMPLETE -> "Locked & Ready to Save"
                            WeighInPhase.ERROR -> session.errorMessage ?: "Hardware Error"
                        },
                    )
                    session.heartRateBpm?.let { hr ->
                        Spacer(Modifier.height(6.dp))
                        Text("$hr bpm", style = OpenCultTheme.typography.numericInline, color = colors.coralAlert)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OpenCultPrimaryButton(
                text = if (bleState.isScanning) "Scanning..." else "Scan Cult Scale",
                onClick = {
                    scope.launch {
                        val anthro = viewModel.getActiveAnthropometry()
                        viewModel.startRealBleScan(selectedTier, anthro)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            OpenCultSecondaryButton(
                text = "BLE Perms",
                onClick = onRequestBlePermissions,
                modifier = Modifier.weight(0.6f),
            )
        }

        bleState.discoveredDevices.forEach { dev ->
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val anthro = viewModel.getActiveAnthropometry()
                        viewModel.connectToDiscovered(dev.address, dev.suggestedTier, anthro)
                    }
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = colors.brandCobalt)
                        Column {
                            Text(dev.name, style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                            Text("${dev.address}  (${dev.rssi} dBm)", style = OpenCultTheme.typography.monoHex, color = colors.textMuted)
                        }
                    }
                    OpenCultStatusChip(label = "Connect")
                }
            }
        }

        if (session.phase == WeighInPhase.FEET_ONLY_LOCKED) {
            OpenCultSecondaryButton(
                text = "Lock Feet-Only Now (No Handle BIA)",
                onClick = { viewModel.confirmFeetOnly() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (session.completedReport != null) {
            OpenCultPrimaryButton(
                text = if (isCommitting) "Committing..." else "Commit & Sync Reading",
                enabled = !isCommitting,
                onClick = {
                    isCommitting = true
                    scope.launch {
                        val (measurementId, ambiguous) = viewModel.commitCompletedSession()
                        isCommitting = false
                        if (ambiguous != null && ambiguous.isNotEmpty()) {
                            ambiguousChooserState = measurementId to ambiguous
                        } else {
                            onMeasurementCommitted(measurementId)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Simulated Hardware Fixture Replay",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Text(
                text = "Replays recorded Cult 8E / Pro Feet-Only / CS-BF01 byte streams locally when no physical scale is nearby.",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpenCultSecondaryButton(
                    text = "Sim ${if (selectedTier == ScaleTier.CULT_BF01_4E) "CS-BF01" else "Pro 8E"}",
                    onClick = {
                        scope.launch {
                            val anthro = viewModel.getActiveAnthropometry()
                            viewModel.launchSimulatedFixture(selectedTier, anthro)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                OpenCultSecondaryButton(
                    text = "Sim Feet-Only",
                    onClick = {
                        scope.launch {
                            val anthro = viewModel.getActiveAnthropometry()
                            viewModel.launchSimulatedFixture(ScaleTier.CULT_PRO_FEET_ONLY, anthro)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    val chooser = ambiguousChooserState
    if (chooser != null) {
        OpenCultPillSelectorSheet(
            title = "Multiple Members Within Weight Window",
            options = chooser.second.map { PillOption(it.id, it.displayName, "Confirm assignment") }.toImmutableList(),
            selected = chooser.second.first().id,
            onSelect = { profileId ->
                scope.launch {
                    viewModel.resolveAmbiguousMember(chooser.first, profileId)
                    ambiguousChooserState = null
                    onMeasurementCommitted(chooser.first)
                }
            },
            onDismiss = {
                ambiguousChooserState = null
                onMeasurementCommitted(chooser.first)
            },
        )
    }
}

@Composable
private fun AmbientRadarRings(
    active: Boolean,
    reducedMotion: Boolean,
) {
    val colors = OpenCultTheme.colors
    if (reducedMotion || !active) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            listOf(0.45f, 0.72f, 0.98f).forEach { factor ->
                drawCircle(
                    color = colors.brandCobalt.copy(alpha = 0.28f),
                    radius = (size.minDimension / 2f) * factor,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = dash),
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "radar_pulse")
    val p1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(OpenCultMotion.AmbientRadarCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_1",
    )
    val p2 = ((p1 - 0.33f) + 1f) % 1f
    val p3 = ((p1 - 0.66f) + 1f) % 1f

    Canvas(modifier = Modifier.size(190.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        listOf(p1, p2, p3).forEach { phase ->
            drawCircle(
                color = colors.brandCobalt.copy(alpha = (1f - phase) * 0.45f),
                radius = maxR * phase,
                center = center,
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }
    }
}
