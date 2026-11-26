package org.opencult.scale.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricItem
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.designsystem.components.BodyFatArcGauge
import org.opencult.scale.core.designsystem.components.MetricTrendLineChart
import org.opencult.scale.core.designsystem.components.MetricTrendPoint
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultCalendarSheet
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorSheet
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.StippledBarChart
import org.opencult.scale.core.designsystem.components.StippledBarEntry
import org.opencult.scale.core.designsystem.components.dockScrollClearance
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.sync.ActiveMemberSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class ReportRange(val days: Int?, val label: String) {
    DAYS_7(7, "7D"),
    DAYS_30(30, "30D"),
    DAYS_90(90, "90D"),
    DAYS_365(365, "1Y"),
    ALL(null, "All"),
}

enum class ReportSourceFilter(val wireSource: String?, val label: String) {
    ALL(null, "All Scales"),
    PRO_8E("ble_8e_handles", "Pro 8E"),
    FEET_4E("ble_4e_feet", "4E / Feet"),
    HC_IMPORT("health_connect_import", "Health Connect"),
}

data class ReportsUiState(
    val profiles: List<HouseholdProfileEntity> = emptyList(),
    val activeProfile: HouseholdProfileEntity? = null,
    val allMeasurements: List<MeasurementEntity> = emptyList(),
    val selectableMetrics: List<BodyMetricItem> = BodyMetricsEngine.calculate(
        UserAnthropometry(BiologicalSex.MALE, 30, 176.0),
        ScaleTier.CULT_PRO_8E,
        74.0,
        heartRateBpm = 68,
        dualFreqImpedance = org.opencult.scale.core.bia.DualFreqImpedance(
            307.4, 311.3, 26.8, 242.1, 256.2,
            267.6, 275.5, 24.5, 219.1, 229.5,
        ),
    ).metrics.filter { it.id != "who_bmi_category" },
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    activeMemberSession: ActiveMemberSession,
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.observeActiveProfiles(),
        repository.observeAllMeasurements(),
        activeMemberSession.activeProfileId,
    ) { profiles, all, activeId ->
        val p = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        ReportsUiState(
            profiles = profiles,
            activeProfile = p,
            allMeasurements = all,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun metricValue(m: MeasurementEntity, metricId: String): Double? = when (metricId) {
        "weight_kg" -> m.weightKg
        else -> repository.decodeMetricsJson(m.metricsJson)[metricId]
    }
}

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreenContent(
        state = state,
        metricLookup = viewModel::metricValue,
        modifier = modifier,
    )
}

@Composable
fun ReportsScreenContent(
    state: ReportsUiState,
    metricLookup: (MeasurementEntity, String) -> Double?,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()

    var range by remember { mutableStateOf(ReportRange.DAYS_30) }
    var sourceFilter by remember { mutableStateOf(ReportSourceFilter.ALL) }
    var selectedMetricId by remember { mutableStateOf("weight_kg") }
    var selectedMemberFilterId by remember(state.activeProfile?.id) {
        mutableStateOf(state.activeProfile?.id ?: "ALL_MEMBERS")
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showCalendar by remember { mutableStateOf(false) }
    var showMetricPicker by remember { mutableStateOf(false) }

    val activeMetricMeta = remember(selectedMetricId, state.selectableMetrics) {
        state.selectableMetrics.firstOrNull { it.id == selectedMetricId } ?: state.selectableMetrics.first()
    }

    val memberScoped = remember(state.allMeasurements, selectedMemberFilterId) {
        if (selectedMemberFilterId == "ALL_MEMBERS") {
            state.allMeasurements
        } else {
            state.allMeasurements.filter { it.profileId == selectedMemberFilterId }
        }
    }

    // Selected calendar date acts as upper window boundary (default: today)
    val windowBounds = remember(range, selectedDate) {
        val zone = ZoneId.systemDefault()
        val endMs = selectedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startMs = range.days?.let { days ->
            selectedDate.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        startMs to endMs
    }

    val filtered = remember(memberScoped, sourceFilter, windowBounds) {
        val (startMs, endMs) = windowBounds
        memberScoped
            .filter { m ->
                val inTime = m.measuredAtMs < endMs && (startMs == null || m.measuredAtMs >= startMs)
                val inSource = sourceFilter.wireSource == null || m.source == sourceFilter.wireSource
                inTime && inSource
            }
            .reversed()
    }

    val bars = remember(filtered, activeMetricMeta) {
        val dayFmt = DateTimeFormatter.ofPattern("d/M")
        filtered.takeLast(7).mapNotNull { m ->
            val v = metricLookup(m, activeMetricMeta.id) ?: return@mapNotNull null
            val lbl = Instant.ofEpochMilli(m.measuredAtMs).atZone(ZoneId.systemDefault()).format(dayFmt)
            StippledBarEntry(
                id = m.id,
                label = lbl,
                value = v.toFloat(),
                badgeText = BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, v),
            )
        }.toImmutableList()
    }
    val trendPoints = remember(filtered, activeMetricMeta) {
        val dayFmt = DateTimeFormatter.ofPattern("d/M")
        filtered.mapNotNull { m ->
            val v = metricLookup(m, activeMetricMeta.id) ?: return@mapNotNull null
            val lbl = Instant.ofEpochMilli(m.measuredAtMs).atZone(ZoneId.systemDefault()).format(dayFmt)
            MetricTrendPoint(
                label = lbl,
                value = v.toFloat(),
                badgeText = "${BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, v)} ${activeMetricMeta.unit}".trim(),
            )
        }.toImmutableList()
    }

    var selectedBarIndex by remember(bars) { mutableIntStateOf((bars.size - 1).coerceAtLeast(0)) }
    var selectedTrendIndex by remember(trendPoints) { mutableIntStateOf((trendPoints.size - 1).coerceAtLeast(0)) }

    val latestFiltered = filtered.lastOrNull()
    val latestBf = latestFiltered?.let { metricLookup(it, "body_fat_pct") }
    val zoneLabel = when {
        latestBf == null -> "No Readings"
        latestBf <= 16.0 -> "Optimal"
        latestBf <= 25.0 -> "Elevated"
        else -> "High"
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reports & Trends",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
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
                    contentDescription = "Calendar filter",
                    tint = colors.textPrimary,
                )
            }
        }

        if (selectedDate != LocalDate.now()) {
            OpenCultStatusChip(
                label = "Window End: $selectedDate (Tap to Reset)",
                containerColor = colors.cobaltSubtle,
                contentColor = colors.brandCobalt,
                modifier = Modifier.openCultMicroPress(role = Role.Button) { selectedDate = LocalDate.now() },
            )
        }

        // DRD:C-03 3-Zone Segmented Arc Gauge (honest empty state when latestBf == null)
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.elevatedSurface) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Body Fat Classification", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                    Text(
                        text = latestBf?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--%",
                        style = OpenCultTheme.typography.bentoStat,
                        color = if (latestBf != null) colors.textPrimary else colors.textMuted,
                    )
                }
                OpenCultStatusChip(
                    label = zoneLabel,
                    containerColor = if (latestBf == null) colors.subtlePillSurface else colors.cobaltSubtle,
                    contentColor = if (latestBf == null) colors.textSecondary else colors.brandCobalt,
                )
            }
            Spacer(Modifier.height(4.dp))
            BodyFatArcGauge(
                bodyFatPct = latestBf?.toFloat() ?: 8f,
                hasData = latestBf != null,
            )
            if (latestBf == null) {
                Text(
                    text = "No measurements in this window yet. Record a weigh-in or change filters.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }

        // Multi-axis filters: Range, Scale Source, Member Scope, & 56-Parameter Metric Picker
        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(ReportRange.DAYS_7, "7D"),
                PillOption(ReportRange.DAYS_30, "30D"),
                PillOption(ReportRange.DAYS_90, "90D"),
                PillOption(ReportRange.DAYS_365, "1Y"),
                PillOption(ReportRange.ALL, "All"),
            ),
            selected = range,
            onSelect = { range = it },
        )

        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(ReportSourceFilter.ALL, "All Scales"),
                PillOption(ReportSourceFilter.PRO_8E, "Pro 8E"),
                PillOption(ReportSourceFilter.FEET_4E, "4E / Feet"),
                PillOption(ReportSourceFilter.HC_IMPORT, "Health Connect"),
            ),
            selected = sourceFilter,
            onSelect = { sourceFilter = it },
        )

        if (state.profiles.size > 1) {
            val memberOpts = (
                listOf(PillOption("ALL_MEMBERS", "All Members")) +
                    state.profiles.map { PillOption(it.id, it.displayName) }
                ).toImmutableList()
            OpenCultPillSelectorRow(
                options = memberOpts,
                selected = selectedMemberFilterId,
                onSelect = { selectedMemberFilterId = it },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f)) {
                OpenCultPillSelectorRow(
                    options = persistentListOf(
                        PillOption("weight_kg", "Weight"),
                        PillOption("body_fat_pct", "Fat %"),
                        PillOption("skeletal_muscle_mass_kg", "SMM"),
                        PillOption("visceral_fat_index", "Visceral"),
                    ),
                    selected = selectedMetricId,
                    onSelect = { selectedMetricId = it },
                )
            }
            OpenCultStatusChip(
                label = "All 56",
                modifier = Modifier.openCultMicroPress(role = Role.Button) { showMetricPicker = true },
            )
        }

        // DRD:C-04 Staggered Stippled Dot-Matrix Bar Chart
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${activeMetricMeta.prdCode} ${activeMetricMeta.label}",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                OpenCultStatusChip(label = activeMetricMeta.unit.ifBlank { "idx" })
            }
            Spacer(Modifier.height(8.dp))
            if (bars.isEmpty()) {
                Text(
                    text = "No values for ${activeMetricMeta.label} inside this window (${sourceFilter.label}).",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
            } else {
                StippledBarChart(
                    entries = bars,
                    selectedIndex = selectedBarIndex,
                    onSelectIndex = { selectedBarIndex = it },
                )
            }
        }

        // Clickable Trend Line Explorer across full filtered window
        if (trendPoints.size >= 2) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Longitudinal Trend (${trendPoints.size} points)",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                MetricTrendLineChart(
                    points = trendPoints,
                    selectedIndex = selectedTrendIndex,
                    onSelectIndex = { selectedTrendIndex = it },
                )
            }
        }

        if (trendPoints.isNotEmpty()) {
            val values = trendPoints.map { it.value.toDouble() }
            val net = values.last() - values.first()
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Range Avg", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                        Text(
                            text = "${BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, values.average())} ${activeMetricMeta.unit}".trim(),
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.textPrimary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Min / Max", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                        Text(
                            text = "${BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, values.min())} / ${BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, values.max())}",
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.textPrimary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Delta", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                        val sign = if (net >= 0) "+" else ""
                        Text(
                            text = "$sign${BodyMetricsEngine.formatStoredMetricValue(activeMetricMeta, net)}",
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.brandCobalt,
                        )
                    }
                }
            }
        }
    }

    if (showCalendar) {
        val dates = memberScoped.mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }.toImmutableSet()
        OpenCultCalendarSheet(
            selectedDate = selectedDate,
            datesWithMeasurements = dates,
            onSelectDate = { selectedDate = it },
            onDismiss = { showCalendar = false },
        )
    }

    if (showMetricPicker) {
        OpenCultPillSelectorSheet(
            title = "Select Any Body Metric",
            options = state.selectableMetrics.map {
                PillOption(it.id, "${it.prdCode} ${it.label}", it.category.title)
            }.toImmutableList(),
            selected = selectedMetricId,
            onSelect = { selectedMetricId = it },
            onDismiss = { showMetricPicker = false },
        )
    }
}

@Preview(name = "Reports Empty Honest Daylight", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ReportsEmptyPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        ReportsScreenContent(
            state = ReportsUiState(),
            metricLookup = { _, _ -> null },
        )
    }
}
