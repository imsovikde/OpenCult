package org.opencult.scale.feature.weighin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.ble.BleHardwareUiState
import org.opencult.scale.core.ble.CultBleController
import org.opencult.scale.core.ble.DiscoveredScaleDevice
import org.opencult.scale.core.ble.ScaleSessionSnapshot
import org.opencult.scale.core.ble.WeighInPhase
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
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
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

    suspend fun getActiveAnthropometry(): UserAnthropometry {
        val activeId = activeMemberSession.activeProfileId.value
        val profiles = db.householdProfileDao().getActiveProfiles()
        val p = profiles.firstOrNull { it.id == activeId }
            ?: profiles.firstOrNull()
            ?: repository.ensureDefaultProfileIfEmpty(syncEngine.currentAccountIdOrGuest())
        return with(repository) { p.toAnthropometry() }
    }

    fun refreshPermissions() {
        bleController.refreshPermissionAndAdapterState()
    }

    fun startRealBleScan(tier: ScaleTier, anthro: UserAnthropometry) {
        bleController.startScan(anthro, tier)
    }

    fun stopScan() {
        bleController.stopScan()
    }

    fun connectToDiscovered(address: String, tier: ScaleTier, anthro: UserAnthropometry) {
        bleController.connectToDevice(address, anthro, tier)
    }

    fun confirmFeetOnly() {
        bleController.confirmFeetOnlyReading()
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
            notes = null,
        )

        if (outcome is HouseholdMatchOutcome.Unique) {
            val resolvedModel = when {
                report.effectiveTier == ScaleTier.CULT_PRO_8E -> "Cult Smart Scale Pro (8E)"
                snap.detectedTier == ScaleTier.CULT_BF01_4E -> "Cult Scale CS-BF01 (4E)"
                else -> snap.deviceName.ifBlank { "Cult Smart Scale" }
            }
            healthConnectGateway.syncMeasurement(measurement, targetProfile, resolvedModel)
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
}

@Composable
fun WeighInScreen(
    onRequestBlePermissions: () -> Unit,
    onClose: () -> Unit,
    onMeasurementCommitted: (String) -> Unit,
    modifier: Modifier = Modifier,
    bleGranted: Boolean = true,
    bluetoothEnabled: Boolean = true,
    onEnableBluetooth: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    viewModel: WeighInViewModel = hiltViewModel(),
) {
    val bleState by viewModel.bleState.collectAsStateWithLifecycle()
    val memberContext by viewModel.memberContext.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedTier by remember { mutableStateOf(ScaleTier.CULT_PRO_8E) }
    var ambiguousChooserState by remember { mutableStateOf<Pair<String, List<HouseholdProfileEntity>>?>(null) }
    var isCommitting by remember { mutableStateOf(false) }

    var prevPhase by remember { mutableStateOf(WeighInPhase.IDLE) }
    var prevStreak by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.resetBle()
        viewModel.refreshPermissions()
    }

    LaunchedEffect(bleGranted, bluetoothEnabled) {
        viewModel.refreshPermissions()
    }

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

    val effectiveBleGranted = bleGranted && bleState.hasPermissions
    val effectiveBtOn = bluetoothEnabled && bleState.isBluetoothEnabled

    WeighInContent(
        bleState = bleState,
        memberContext = memberContext,
        selectedTier = selectedTier,
        bleGranted = effectiveBleGranted,
        bluetoothEnabled = effectiveBtOn,
        isCommitting = isCommitting,
        onSelectTier = { selectedTier = it },
        onToggleMemberLock = { viewModel.setMemberLockEnabled(it) },
        onScanOrGateClick = {
            when {
                !effectiveBleGranted -> onRequestBlePermissions()
                !effectiveBtOn -> onEnableBluetooth()
                bleState.isScanning -> viewModel.stopScan()
                else -> scope.launch {
                    val anthro = viewModel.getActiveAnthropometry()
                    viewModel.startRealBleScan(selectedTier, anthro)
                }
            }
        },
        onRequestBlePermissions = onRequestBlePermissions,
        onOpenAppSettings = onOpenAppSettings,
        onConnectDiscovered = { dev ->
            when {
                !effectiveBleGranted -> onRequestBlePermissions()
                !effectiveBtOn -> onEnableBluetooth()
                else -> scope.launch {
                    val anthro = viewModel.getActiveAnthropometry()
                    viewModel.connectToDiscovered(dev.address, dev.suggestedTier, anthro)
                }
            }
        },
        onConfirmFeetOnly = { viewModel.confirmFeetOnly() },
        onCommitMeasurement = {
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
        onClose = {
            viewModel.resetBle()
            onClose()
        },
        modifier = modifier,
    )

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
fun WeighInContent(
    bleState: BleHardwareUiState,
    memberContext: WeighInMemberContext,
    selectedTier: ScaleTier,
    bleGranted: Boolean,
    bluetoothEnabled: Boolean,
    isCommitting: Boolean,
    onSelectTier: (ScaleTier) -> Unit,
    onToggleMemberLock: (Boolean) -> Unit,
    onScanOrGateClick: () -> Unit,
    onRequestBlePermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onConnectDiscovered: (DiscoveredScaleDevice) -> Unit,
    onConfirmFeetOnly: () -> Unit,
    onCommitMeasurement: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val scroll = rememberScrollState()
    val session = bleState.session

    val stageIndex = when (session.phase) {
        WeighInPhase.IDLE, WeighInPhase.SCANNING, WeighInPhase.CONNECTING, WeighInPhase.ERROR -> 0
        WeighInPhase.STEP_ON, WeighInPhase.WEIGHING_SETTLING -> 1
        WeighInPhase.HOLD_HANDLES_BIA, WeighInPhase.FEET_ONLY_LOCKED -> 2
        WeighInPhase.LOCKED_COMPLETE -> 3
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
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(CircleShape)
                    .background(colors.elevatedSurface)
                    .border(1.dp, colors.borderSubtle, CircleShape)
                    .openCultMicroPress(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close weigh-in",
                    tint = colors.textPrimary,
                )
            }
        }

        // 4-step workflow progression stepper: Search -> Settle -> Contact -> Synced
        WeighInStepperBar(currentStep = stageIndex)

        // Hero stage card with zero-overlap stacked radar + separate text/status column
        OpenCultBentoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.elevatedSurface,
            shape = OpenCultTheme.shapes.heroSquircle,
        ) {
            AnimatedContent(
                targetState = stageIndex,
                transitionSpec = { OpenCultMotion.tabContentTransition(forward = targetState >= initialState, reducedMotion = reducedMotion) },
                label = "weigh_in_hero_stage",
            ) { stage ->
                when (stage) {
                    0 -> WeighInStageSearchCard(
                        bleState = bleState,
                        bleGranted = bleGranted,
                        bluetoothEnabled = bluetoothEnabled,
                        reducedMotion = reducedMotion,
                    )
                    1 -> WeighInStageSettleCard(
                        session = session,
                        reducedMotion = reducedMotion,
                    )
                    2 -> WeighInStageContactCard(
                        session = session,
                        reducedMotion = reducedMotion,
                    )
                    else -> WeighInStageCompleteCard(
                        session = session,
                    )
                }
            }
        }

        if (session.phase == WeighInPhase.FEET_ONLY_LOCKED) {
            OpenCultSecondaryButton(
                text = "Lock Feet-Only Now (No Handle BIA)",
                onClick = onConfirmFeetOnly,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (session.completedReport != null) {
            OpenCultPrimaryButton(
                text = if (isCommitting) "Committing..." else "Commit & Sync Reading",
                enabled = !isCommitting,
                onClick = onCommitMeasurement,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Primary hardware action button with automatic BLE permission / adapter gating
        val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.25f
        val primaryLabel = when {
            !bleGranted -> "Allow Bluetooth Access"
            !bluetoothEnabled -> "Turn On Bluetooth"
            bleState.isScanning -> "Stop Scanning"
            else -> "Scan Cult Scale"
        }
        val secondaryLabel = if (!bleGranted) "Settings" else "Perms"
        val secondaryAction = if (!bleGranted) onOpenAppSettings else onRequestBlePermissions
        if (largeFont) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpenCultPrimaryButton(
                    text = primaryLabel,
                    onClick = onScanOrGateClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                OpenCultSecondaryButton(
                    text = if (!bleGranted) "Open App Settings" else "Bluetooth Permissions",
                    onClick = secondaryAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OpenCultPrimaryButton(
                    text = primaryLabel,
                    onClick = onScanOrGateClick,
                    modifier = Modifier.weight(1f),
                )
                OpenCultSecondaryButton(
                    text = secondaryLabel,
                    onClick = secondaryAction,
                    modifier = Modifier.weight(0.5f),
                )
            }
        }

        bleState.discoveredDevices.forEach { dev ->
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onConnectDiscovered(dev) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = colors.brandCobalt,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dev.name,
                            style = OpenCultTheme.typography.buttonLabel,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${dev.address}  (${dev.rssi} dBm)",
                            style = OpenCultTheme.typography.monoHex,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OpenCultStatusChip(label = "Connect")
                }
            }
        }

        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(ScaleTier.CULT_PRO_8E, "Cult Pro (8E)"),
                PillOption(ScaleTier.CULT_BF01_4E, "CS-BF01 (4E)"),
            ),
            selected = if (selectedTier == ScaleTier.CULT_BF01_4E) ScaleTier.CULT_BF01_4E else ScaleTier.CULT_PRO_8E,
            onSelect = onSelectTier,
        )

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
                            "Bypasses weight-window auto-match and attributes this weigh-in directly to ${memberContext.activeProfile?.displayName ?: "active member"}."
                        } else {
                            "Auto-match picks household member by rolling baseline weight."
                        },
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = memberContext.memberLockEnabled,
                    onCheckedChange = onToggleMemberLock,
                )
            }
        }
    }
}

@Composable
private fun WeighInStepperBar(currentStep: Int) {
    val colors = OpenCultTheme.colors
    val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.25f
    val steps = listOf("Search", "Settle", "Contact", "Synced")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OpenCultTheme.shapes.bentoSquircle)
            .background(colors.cardSurface)
            .border(1.dp, colors.borderSubtle, OpenCultTheme.shapes.bentoSquircle)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { idx, label ->
            val done = idx < currentStep
            val active = idx == currentStep
            val itemWeight = if (largeFont) {
                if (active) 2.4f else 0.7f
            } else {
                1f
            }
            Row(
                modifier = Modifier.weight(itemWeight),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> colors.brandCobalt
                                done -> colors.bentoSageAccent
                                else -> colors.subtlePillSurface
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.dockActiveIcon,
                            modifier = Modifier.size(12.dp),
                        )
                    } else {
                        Text(
                            text = "${idx + 1}",
                            style = OpenCultTheme.typography.microLabel,
                            color = if (active) colors.dockActiveIcon else colors.textMuted,
                        )
                    }
                }
                if (!largeFont || active) {
                    Text(
                        text = label,
                        style = OpenCultTheme.typography.microLabel,
                        color = if (active || done) colors.textPrimary else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeighInStageSearchCard(
    bleState: BleHardwareUiState,
    bleGranted: Boolean,
    bluetoothEnabled: Boolean,
    reducedMotion: Boolean,
) {
    val colors = OpenCultTheme.colors
    val session = bleState.session
    val radarActive = (bleState.isScanning || session.phase == WeighInPhase.CONNECTING) && bleGranted && bluetoothEnabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Dedicated radar ring container separated vertically from headline & status labels
        Box(
            modifier = Modifier
                .size(148.dp)
                .testTag("weighin_radar_container"),
            contentAlignment = Alignment.Center,
        ) {
            AmbientRadarRings(
                active = radarActive,
                reducedMotion = reducedMotion,
                sizeDp = 148.dp,
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.cobaltSubtle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = colors.brandCobalt,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weighin_status_container"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val headline = when {
                !bleGranted -> "Bluetooth Access Needed"
                !bluetoothEnabled -> "Bluetooth Is Turned Off"
                session.phase == WeighInPhase.CONNECTING -> "Connecting to Scale..."
                session.phase == WeighInPhase.ERROR -> "Hardware Notice"
                bleState.isScanning -> "Scanning Nearby Scales..."
                else -> "Ready to Connect"
            }
            Text(
                text = headline,
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            OpenCultStatusChip(
                label = when {
                    !bleGranted -> "Permission Required"
                    !bluetoothEnabled -> "Adapter Off"
                    session.phase == WeighInPhase.CONNECTING -> session.deviceMac.ifBlank { "Pairing..." }
                    session.phase == WeighInPhase.ERROR -> "Retry Available"
                    bleState.isScanning -> "GATT 0xFFF0 / 0xFFB0"
                    else -> "Step On To Wake Scale"
                },
                containerColor = if (session.phase == WeighInPhase.ERROR) colors.bentoPeachSurface else colors.cobaltSubtle,
                contentColor = if (session.phase == WeighInPhase.ERROR) colors.bentoPeachFlame else colors.brandCobalt,
            )
            Text(
                text = when {
                    !bleGranted -> "Tap Allow Bluetooth Access below so OpenCult can discover your scale."
                    !bluetoothEnabled -> "Enable Bluetooth to scan for Cult Pro 8E or CS-BF01 scales."
                    session.phase == WeighInPhase.ERROR -> session.errorMessage ?: "Unable to communicate with scale."
                    bleState.isScanning -> "Step briefly onto the glass platform to wake advertisements."
                    else -> "Tap Scan Cult Scale below, then select your discovered scale."
                },
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeighInStageSettleCard(
    session: ScaleSessionSnapshot,
    reducedMotion: Boolean,
) {
    val colors = OpenCultTheme.colors
    val targetFraction = (session.stableFrameCount.coerceIn(0, 3) / 3f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = OpenCultMotion.telemetryGlide(reducedMotion),
        label = "settle_progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OpenCultStatusChip(label = "Step 2  |  Stabilizing Weight")
        Text(
            text = session.liveWeightKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--.-- kg",
            style = OpenCultTheme.typography.heroMetric,
            color = colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(8.dp)
                .clip(OpenCultTheme.shapes.pill)
                .background(colors.subtlePillSurface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.coerceIn(0.08f, 1f))
                    .fillMaxHeight()
                    .clip(OpenCultTheme.shapes.pill)
                    .background(colors.brandCobalt),
            )
        }
        Text(
            text = "Hold steady (${session.stableFrameCount}/3 locked frames)",
            style = OpenCultTheme.typography.body,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun WeighInStageContactCard(
    session: ScaleSessionSnapshot,
    reducedMotion: Boolean,
) {
    val colors = OpenCultTheme.colors
    val isFeetOnly = session.phase == WeighInPhase.FEET_ONLY_LOCKED
    val sweepFraction = if (isFeetOnly) 1f else (session.dfPacketCount.coerceIn(0, 4) / 4f).coerceAtLeast(0.2f)
    val sweepAnimated by animateFloatAsState(
        targetValue = sweepFraction,
        animationSpec = OpenCultMotion.telemetryGlide(reducedMotion),
        label = "contact_sweep_progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OpenCultStatusChip(
            label = if (isFeetOnly) "Weight Locked (Feet-Only)" else "Hold Handlebar Electrodes",
        )
        Text(
            text = session.liveWeightKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--.-- kg",
            style = OpenCultTheme.typography.heroMetric,
            color = colors.textPrimary,
        )
        session.heartRateBpm?.let { hr ->
            Text("$hr bpm", style = OpenCultTheme.typography.numericInline, color = colors.coralAlert)
        }
        Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                drawArc(
                    color = colors.subtlePillSurface,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = colors.brandCobalt,
                    startAngle = -90f,
                    sweepAngle = 360f * sweepAnimated,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Text(
                text = if (isFeetOnly) "4 Pads" else "${session.dfPacketCount}/4",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpenCultStatusChip(
                label = "Feet Pads OK",
                containerColor = colors.bentoSageSurface,
                contentColor = colors.bentoSageMuted,
            )
            OpenCultStatusChip(
                label = if (isFeetOnly) "Handles Open" else "8E Dual-Freq Active",
            )
        }
    }
}

@Composable
private fun WeighInStageCompleteCard(
    session: ScaleSessionSnapshot,
) {
    val colors = OpenCultTheme.colors
    val report = session.completedReport
    val metricCount = report?.metrics?.count { it.value != null } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OpenCultStatusChip(
            label = "Locked & Ready to Sync",
            containerColor = colors.bentoSageSurface,
            contentColor = colors.bentoSageMuted,
        )
        Text(
            text = (report?.weightKg ?: session.liveWeightKg)?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--.-- kg",
            style = OpenCultTheme.typography.heroMetric,
            color = colors.textPrimary,
        )
        session.heartRateBpm?.let { hr ->
            Text("$hr bpm", style = OpenCultTheme.typography.numericInline, color = colors.coralAlert)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpenCultStatusChip(label = "$metricCount active metrics")
            OpenCultStatusChip(
                label = if (session.detectedTier == ScaleTier.CULT_PRO_8E) "Cult Pro 8E" else "4E / Feet",
            )
        }
    }
}

@Composable
private fun AmbientRadarRings(
    active: Boolean,
    reducedMotion: Boolean,
    sizeDp: Dp = 148.dp,
) {
    val colors = OpenCultTheme.colors
    if (reducedMotion || !active) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            listOf(0.48f, 0.74f, 0.98f).forEach { factor ->
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

    Canvas(modifier = Modifier.size(sizeDp)) {
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

@Preview(name = "WeighIn Stage 1 Daylight 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun WeighInDaylight360Preview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        WeighInContent(
            bleState = BleHardwareUiState(
                hasPermissions = true,
                isBluetoothEnabled = true,
                isScanning = true,
                discoveredDevices = listOf(
                    DiscoveredScaleDevice("Cult Smart Scale Pro 8E Edition", "AA:BB:CC:DD:EE:01", -52, ScaleTier.CULT_PRO_8E),
                ),
                session = ScaleSessionSnapshot(phase = WeighInPhase.SCANNING),
            ),
            memberContext = WeighInMemberContext(memberLockEnabled = false),
            selectedTier = ScaleTier.CULT_PRO_8E,
            bleGranted = true,
            bluetoothEnabled = true,
            isCommitting = false,
            onSelectTier = {},
            onToggleMemberLock = {},
            onScanOrGateClick = {},
            onRequestBlePermissions = {},
            onOpenAppSettings = {},
            onConnectDiscovered = {},
            onConfirmFeetOnly = {},
            onCommitMeasurement = {},
            onClose = {},
        )
    }
}

@Preview(name = "WeighIn Obsidian 1.5x", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun WeighInObsidian15Preview() {
    OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
        WeighInContent(
            bleState = BleHardwareUiState(
                hasPermissions = true,
                isBluetoothEnabled = true,
                isScanning = false,
                session = ScaleSessionSnapshot(
                    phase = WeighInPhase.HOLD_HANDLES_BIA,
                    liveWeightKg = 74.80,
                    heartRateBpm = 68,
                    dfPacketCount = 2,
                ),
            ),
            memberContext = WeighInMemberContext(memberLockEnabled = true),
            selectedTier = ScaleTier.CULT_PRO_8E,
            bleGranted = true,
            bluetoothEnabled = true,
            isCommitting = false,
            onSelectTier = {},
            onToggleMemberLock = {},
            onScanOrGateClick = {},
            onRequestBlePermissions = {},
            onOpenAppSettings = {},
            onConnectDiscovered = {},
            onConfirmFeetOnly = {},
            onCommitMeasurement = {},
            onClose = {},
        )
    }
}
