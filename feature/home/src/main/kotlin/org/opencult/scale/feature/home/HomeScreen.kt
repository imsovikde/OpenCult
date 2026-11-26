package org.opencult.scale.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.ble.CultBleController
import org.opencult.scale.core.ble.SavedScaleTarget
import org.opencult.scale.core.designsystem.components.HeroWeightSparkChart
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultCalendarSheet
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorSheet
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSectionHeader
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.OverviewWaveSparkline
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.SparkBarEntry
import org.opencult.scale.core.designsystem.components.dockScrollClearance
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.DailyHealthContext
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SupabaseSyncEngine
import org.opencult.scale.core.sync.SyncStatusSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class HomeDashboardUiState(
    val profiles: List<HouseholdProfileEntity> = emptyList(),
    val activeProfile: HouseholdProfileEntity? = null,
    val memberLockEnabled: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val measurements: List<MeasurementEntity> = emptyList(),
    val pendingReviewMeasurements: List<MeasurementEntity> = emptyList(),
    val datesWithMeasurements: Set<LocalDate> = emptySet(),
    val savedDevices: List<ScaleDeviceEntity> = emptyList(),
    val dailyHealthContext: DailyHealthContext = DailyHealthContext(LocalDate.now(), null, null, null),
    val hcGrantedCount: Int = 0,
    val syncStatus: SyncStatusSnapshot = SyncStatusSnapshot(),
    val pendingOutboxCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val activeMemberSession: ActiveMemberSession,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
    private val bleController: CultBleController,
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val healthContextFlow = MutableStateFlow(DailyHealthContext(LocalDate.now(), null, null, null))
    private val hcGrantedCountFlow = MutableStateFlow(0)

    private val baseFlow = combine(
        repository.observeActiveProfiles(),
        repository.observeAllMeasurements(),
        selectedDateFlow,
        activeMemberSession.activeProfileId,
        activeMemberSession.memberLockEnabled,
    ) { profiles, allMeasurements, selDate, activeId, locked ->
        val activeProfile = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        val forMember = if (activeProfile != null) {
            allMeasurements.filter { it.profileId == activeProfile.id }
        } else {
            emptyList()
        }
        val pendingReview = allMeasurements.filter { it.assignmentState == "pending_review" }
        val dates = forMember.mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }.toSet()
        HomeDashboardUiState(
            profiles = profiles,
            activeProfile = activeProfile,
            memberLockEnabled = locked,
            selectedDate = selDate,
            measurements = forMember,
            pendingReviewMeasurements = pendingReview,
            datesWithMeasurements = dates,
        )
    }

    val uiState: StateFlow<HomeDashboardUiState> = combine(
        baseFlow,
        repository.observeDevices(),
        healthContextFlow,
        hcGrantedCountFlow,
        syncEngine.status,
    ) { base, devices, hcContext, hcPerms, sync ->
        base.copy(
            savedDevices = devices,
            dailyHealthContext = hcContext,
            hcGrantedCount = hcPerms,
            syncStatus = sync,
        )
    }.combine(repository.observePendingOutboxCount()) { state, outboxCount ->
        state.copy(pendingOutboxCount = outboxCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDashboardUiState())

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfileIfEmpty(syncEngine.currentAccountIdOrGuest())
            refreshHealthContext(LocalDate.now())
        }
    }

    fun selectActiveProfile(profileId: String) {
        activeMemberSession.selectActiveProfile(profileId)
    }

    fun setMemberLockEnabled(enabled: Boolean) {
        activeMemberSession.setMemberLockEnabled(enabled)
    }

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
        viewModelScope.launch { refreshHealthContext(date) }
    }

    fun startForegroundPassiveBleIfSaved(devices: List<ScaleDeviceEntity>) {
        if (devices.isEmpty()) return
        val targets = devices.map { dev ->
            SavedScaleTarget(
                address = dev.bleMacOrId,
                advertisedName = dev.advertisedName,
                tier = if (dev.modelTier == "cult_bf01_4e") ScaleTier.CULT_BF01_4E else ScaleTier.CULT_PRO_8E,
            )
        }
        bleController.startPassiveAutoReconnect(targets) {
            val profile = uiState.value.activeProfile
                ?: repository.ensureDefaultProfileIfEmpty(syncEngine.currentAccountIdOrGuest())
            with(repository) { profile.toAnthropometry(healthContextFlow.value.steps?.toInt()) }
        }
    }

    fun stopForegroundPassiveBle() {
        bleController.stopPassiveAutoReconnect()
    }

    private suspend fun refreshHealthContext(date: LocalDate) {
        hcGrantedCountFlow.value = runCatching { healthConnectGateway.getGrantedPermissions().size }.getOrDefault(0)
        val includeRhr = activeMemberSession.restingHrOptInEnabled.value
        healthContextFlow.value = healthConnectGateway.readTodayContext(date, includeRestingHr = includeRhr)
    }

    suspend fun reassignPendingMeasurement(measurementId: String, profileId: String) {
        healthConnectGateway.reassignMeasurementAndSyncHealthConnect(measurementId, profileId)
        syncEngine.enqueueOutboxDrain()
    }

    fun decodeMetric(m: MeasurementEntity, key: String): Double? =
        repository.decodeMetricsJson(m.metricsJson)[key]
}

@Composable
fun HomeScreen(
    onOpenWeighIn: () -> Unit,
    onOpenMeasurementDetail: (String) -> Unit,
    onOpenInspector: () -> Unit,
    onOpenProfiles: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, state.savedDevices) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startForegroundPassiveBleIfSaved(state.savedDevices)
                Lifecycle.Event.ON_STOP -> viewModel.stopForegroundPassiveBle()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopForegroundPassiveBle()
        }
    }

    HomeScreenContent(
        state = state,
        decodeMetric = viewModel::decodeMetric,
        onSelectDate = viewModel::selectDate,
        onSelectProfile = viewModel::selectActiveProfile,
        onToggleMemberLock = viewModel::setMemberLockEnabled,
        onReassignPending = viewModel::reassignPendingMeasurement,
        onOpenWeighIn = onOpenWeighIn,
        onOpenMeasurementDetail = onOpenMeasurementDetail,
        onOpenInspector = onOpenInspector,
        onOpenProfiles = onOpenProfiles,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenContent(
    state: HomeDashboardUiState,
    decodeMetric: (MeasurementEntity, String) -> Double?,
    onSelectDate: (LocalDate) -> Unit,
    onSelectProfile: (String) -> Unit,
    onToggleMemberLock: (Boolean) -> Unit,
    onReassignPending: suspend (String, String) -> Unit,
    onOpenWeighIn: () -> Unit,
    onOpenMeasurementDetail: (String) -> Unit,
    onOpenInspector: () -> Unit,
    onOpenProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var showCalendar by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var pendingAssignTarget by remember { mutableStateOf<MeasurementEntity?>(null) }
    var overviewPage by remember { mutableIntStateOf(0) }
    var musclePage by remember { mutableIntStateOf(0) }

    val latest = remember(state.measurements, state.selectedDate) {
        state.measurements.firstOrNull { it.localDate == state.selectedDate.toString() }
            ?: state.measurements.firstOrNull()
    }
    val previous = remember(state.measurements, latest) {
        if (latest == null) null else state.measurements.drop(1).firstOrNull()
    }

    val sparkBars: ImmutableList<SparkBarEntry> = remember(state.measurements) {
        val recent = state.measurements.take(9).reversed()
        recent.mapIndexed { idx, m ->
            val prev = if (idx > 0) recent[idx - 1].weightKg else m.weightKg
            SparkBarEntry(
                value = m.weightKg.toFloat(),
                isSpike = idx > 0 && abs(m.weightKg - prev) >= 0.8,
            )
        }.toImmutableList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 14.dp)
            .dockScrollClearance(),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        // Header Row: Member Switcher Pill + Member Lock + Calendar + Inspector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(OpenCultTheme.shapes.pill)
                    .background(colors.cardSurface)
                    .openCultMicroPress(pressedScale = OpenCultMotion.RowPressScale, role = Role.Button) {
                        showProfileSheet = true
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(colors.brandCobalt),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.activeProfile?.displayName?.take(1)?.uppercase() ?: "O",
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.dockActiveIcon,
                    )
                }
                Text(
                    text = state.activeProfile?.displayName ?: "OpenCult",
                    style = OpenCultTheme.typography.buttonLabel,
                    color = colors.textPrimary,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (state.memberLockEnabled) colors.cobaltSubtle else colors.cardSurface)
                        .openCultMicroPress(role = Role.Switch) { onToggleMemberLock(!state.memberLockEnabled) }
                        .semantics { contentDescription = "Lock weigh-ins to active member" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.memberLockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = if (state.memberLockEnabled) colors.brandCobalt else colors.textSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.cardSurface)
                        .openCultMicroPress(role = Role.Button) { showCalendar = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = colors.textPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.cardSurface)
                        .openCultMicroPress(role = Role.Button) { onOpenInspector() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.syncStatus.isSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.Tune,
                        contentDescription = "Open Inspector and Sync",
                        tint = if (state.pendingOutboxCount > 0) colors.brandCobalt else colors.textPrimary,
                    )
                }
            }
        }

        // Member Lock / Health Connect active strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpenCultStatusChip(
                label = if (state.memberLockEnabled) {
                    "Locked -> ${state.activeProfile?.displayName ?: "Member"}"
                } else {
                    "Auto-Match On"
                },
                containerColor = if (state.memberLockEnabled) colors.cobaltSubtle else colors.subtlePillSurface,
                contentColor = if (state.memberLockEnabled) colors.brandCobalt else colors.textSecondary,
            )
            OpenCultStatusChip(
                label = if (state.hcGrantedCount > 0) "HC Connected (${state.hcGrantedCount})" else "HC Needs Perms",
                containerColor = if (state.hcGrantedCount > 0) colors.bentoSageSurface else colors.bentoPeachSurface,
                contentColor = if (state.hcGrantedCount > 0) colors.bentoSageMuted else colors.bentoPeachFlame,
                modifier = Modifier.openCultMicroPress(role = Role.Button) { onOpenInspector() },
            )
        }

        // Pending Ambiguous Household Match Banner (INV-09)
        state.pendingReviewMeasurements.firstOrNull()?.let { pending ->
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.cobaltSubtle,
                borderColor = colors.brandCobalt,
                onClick = { pendingAssignTarget = pending },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confirm Weigh-In Member",
                            style = OpenCultTheme.typography.sectionTitle,
                            color = colors.brandCobalt,
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f kg needs manual assignment", pending.weightKg),
                            style = OpenCultTheme.typography.body,
                            color = colors.textPrimary,
                        )
                    }
                    OpenCultStatusChip(label = "Assign")
                }
            }
        }

        // DRD:C-01 Hero Weight Card (28.dp squircle)
        OpenCultBentoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.elevatedSurface,
            shape = OpenCultTheme.shapes.heroSquircle,
            contentPadding = OpenCultTheme.spacing.cardLargePadding,
            onClick = { if (latest != null) onOpenMeasurementDetail(latest.id) else onOpenWeighIn() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenCultStatusChip(
                    label = when (latest?.source) {
                        "ble_8e_handles" -> "Cult Pro 8E"
                        "ble_4e_feet" -> "Feet / 4E BIA"
                        "health_connect_import" -> "Health Connect"
                        else -> "No Reading Today"
                    },
                )
                Text(
                    text = state.selectedDate.toString(),
                    style = OpenCultTheme.typography.microLabel,
                    color = colors.textMuted,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (latest != null) {
                val delta = previous?.let { latest.weightKg - it.weightKg }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", latest.weightKg),
                        style = OpenCultTheme.typography.heroMetric,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "kg",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    if (delta != null) {
                        val sign = if (delta >= 0) "+" else ""
                        OpenCultStatusChip(
                            label = String.format(Locale.US, "%s%.2f kg", sign, delta),
                            containerColor = if (abs(delta) >= 0.8) colors.bentoPeachSurface else colors.bentoSageSurface,
                            contentColor = if (abs(delta) >= 0.8) colors.bentoPeachFlame else colors.bentoSageMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HeroWeightSparkChart(entries = sparkBars)
            } else {
                Text(
                    text = "--.- kg",
                    style = OpenCultTheme.typography.heroMetric,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Step on your Cult scale via the + Cobalt button below to capture your first weigh-in.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OpenCultPrimaryButton(
                    text = "Start Weigh-In",
                    onClick = onOpenWeighIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // DRD:C-02 Overview Bento Grid — ALWAYS visible even when empty so Health Connect Page 2 shows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
        ) {
            val bf = latest?.let { decodeMetric(it, "body_fat_pct") }
            val smm = latest?.let { decodeMetric(it, "skeletal_muscle_mass_kg") }
            val tbw = latest?.let { decodeMetric(it, "tbw_pct") }
            val overviewMetricTitle = when (overviewPage % 3) {
                0 -> "Body Fat"
                1 -> "Skeletal Muscle"
                else -> "Body Water"
            }
            val overviewMetricVal = when {
                latest == null -> "--"
                overviewPage % 3 == 0 -> bf?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"
                overviewPage % 3 == 1 -> smm?.let { String.format(Locale.US, "%.1f kg", it) } ?: "--"
                else -> tbw?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"
            }
            val waveSeries = remember(state.measurements, overviewPage) {
                val key = when (overviewPage % 3) {
                    0 -> "body_fat_pct"
                    1 -> "skeletal_muscle_mass_kg"
                    else -> "tbw_pct"
                }
                val pts = state.measurements.take(8).reversed().mapNotNull { decodeMetric(it, key)?.toFloat() }
                if (pts.size >= 2) pts.toImmutableList() else persistentListOf(1f, 1f)
            }

            OpenCultBentoCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = colors.bentoLavenderSurface,
                borderColor = colors.bentoLavenderWave.copy(alpha = 0.35f),
                onClick = { if (latest != null) onOpenMeasurementDetail(latest.id) else onOpenWeighIn() },
            ) {
                Text(
                    text = overviewMetricTitle,
                    style = OpenCultTheme.typography.microLabel,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = overviewMetricVal,
                    style = OpenCultTheme.typography.bentoStat,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                // 48.dp touch target pager buttons enclosing crisp 32.dp circles (INV-18)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .openCultMicroPress(role = Role.Button) { overviewPage = (overviewPage + 2) % 3 }
                            .semantics { contentDescription = "Previous overview metric" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.cardSurface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .openCultMicroPress(role = Role.Button) { overviewPage = (overviewPage + 1) % 3 }
                            .semantics { contentDescription = "Next overview metric" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.cardSurface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OverviewWaveSparkline(values = waveSeries)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
            ) {
                val ecwRatio = latest?.let { decodeMetric(it, "ecw_tbw_ratio") }
                val bmr = latest?.let { decodeMetric(it, "bmr_kcal") }
                val sageTitle = if (musclePage % 2 == 0) "Lean / BMR" else "Hydration ECW"
                val sageValue = when {
                    latest == null -> "-- kcal"
                    musclePage % 2 == 0 -> bmr?.let { "${it.toInt()} kcal" } ?: "-- kcal"
                    else -> ecwRatio?.let { String.format(Locale.US, "%.3f", it) } ?: "8E Only"
                }

                OpenCultBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = colors.bentoSageSurface,
                    borderColor = colors.bentoSageAccent.copy(alpha = 0.35f),
                    onClick = { musclePage = (musclePage + 1) % 2 },
                ) {
                    Text(
                        text = sageTitle,
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sageValue,
                        style = OpenCultTheme.typography.bentoStat,
                        color = colors.textPrimary,
                    )
                }

                // Health Connect Page 2 Context Card (Steps, Sleep, Active/Total Calories)
                OpenCultBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = colors.bentoPeachSurface,
                    borderColor = colors.bentoPeachFlame.copy(alpha = 0.35f),
                    onClick = onOpenInspector,
                ) {
                    Text(
                        text = "Activity Context",
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    val steps = state.dailyHealthContext.steps
                    val sleepMins = state.dailyHealthContext.sleepMinutes
                    val kcal = state.dailyHealthContext.totalCaloriesKcal
                    Text(
                        text = steps?.let { "$it steps" } ?: "Connect HC",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    val secondaryCtx = buildList {
                        sleepMins?.let { add("${it / 60}h ${it % 60}m sleep") }
                        kcal?.let { add("$it kcal") }
                    }.joinToString(" | ")
                    Text(
                        text = secondaryCtx.ifBlank { "Tap to grant Steps & Sleep" },
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        // Recent Log entries
        OpenCultSectionHeader(
            title = "Recent Measurements",
            subtitle = "${state.measurements.size} recorded entries",
            trailing = {
                OpenCultStatusChip(
                    label = "All Profiles",
                    modifier = Modifier.openCultMicroPress { onOpenProfiles() },
                )
            },
        )

        if (state.measurements.isEmpty()) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No weigh-ins saved yet for ${state.activeProfile?.displayName ?: "this profile"}. Use + below to pair your scale or import from Health Connect.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        } else {
            val fmt = remember { DateTimeFormatter.ofPattern("MMM dd, HH:mm") }
            state.measurements.take(8).forEach { m ->
                val bf = decodeMetric(m, "body_fat_pct")
                val score = decodeMetric(m, "composite_score")?.toInt()
                val timeStr = Instant.ofEpochMilli(m.measuredAtMs).atZone(ZoneId.systemDefault()).format(fmt)

                OpenCultBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenMeasurementDetail(m.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%.2f kg", m.weightKg),
                                style = OpenCultTheme.typography.sectionTitle,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "$timeStr  |  ${if (bf != null) String.format(Locale.US, "Fat %.1f%%", bf) else "--"}",
                                style = OpenCultTheme.typography.body,
                                color = colors.textSecondary,
                            )
                        }
                        OpenCultStatusChip(
                            label = if (score != null) "Score $score" else m.assignmentState,
                        )
                    }
                }
            }
        }
    }

    if (showCalendar) {
        OpenCultCalendarSheet(
            selectedDate = state.selectedDate,
            datesWithMeasurements = state.datesWithMeasurements.toImmutableSet(),
            onSelectDate = onSelectDate,
            onDismiss = { showCalendar = false },
        )
    }

    if (showProfileSheet && state.profiles.isNotEmpty()) {
        OpenCultPillSelectorSheet(
            title = "Switch Household Member",
            options = state.profiles.map { p ->
                PillOption(
                    value = p.id,
                    label = p.displayName,
                    subtitle = "${p.biologicalSex.replaceFirstChar { it.uppercase() }} - ${p.heightCm.toInt()} cm",
                )
            }.toImmutableList(),
            selected = state.activeProfile?.id ?: "",
            onSelect = onSelectProfile,
            onDismiss = { showProfileSheet = false },
        )
    }

    val assignTarget = pendingAssignTarget
    if (assignTarget != null && state.profiles.isNotEmpty()) {
        OpenCultPillSelectorSheet(
            title = String.format(Locale.US, "Assign %.2f kg to Member", assignTarget.weightKg),
            options = state.profiles.map { p ->
                PillOption(p.id, p.displayName, if (p.isPrimaryHcUser) "Primary Health Connect user" else null)
            }.toImmutableList(),
            selected = assignTarget.profileId,
            onSelect = { chosenProfileId ->
                scope.launch {
                    onReassignPending(assignTarget.id, chosenProfileId)
                }
            },
            onDismiss = { pendingAssignTarget = null },
        )
    }
}

@Preview(name = "Home Empty Daylight", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeEmptyDaylightPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        HomeScreenContent(
            state = HomeDashboardUiState(
                dailyHealthContext = DailyHealthContext(LocalDate.now(), 6840L, 425L, 2040),
                hcGrantedCount = 13,
            ),
            decodeMetric = { _, _ -> null },
            onSelectDate = {},
            onSelectProfile = {},
            onToggleMemberLock = {},
            onReassignPending = { _, _ -> },
            onOpenWeighIn = {},
            onOpenMeasurementDetail = {},
            onOpenInspector = {},
            onOpenProfiles = {},
        )
    }
}
