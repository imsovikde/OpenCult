package org.opencult.scale.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.opencult.scale.core.bia.BodyMetricItem
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.MetricCategory
import org.opencult.scale.core.bia.MetricProvenance
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.RawBlePacketEntity
import org.opencult.scale.core.designsystem.components.MetricTrendLineChart
import org.opencult.scale.core.designsystem.components.MetricTrendPoint
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorSheet
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.SupabaseSyncEngine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MeasurementDetailState(
    val measurement: MeasurementEntity? = null,
    val profile: HouseholdProfileEntity? = null,
    val allProfiles: List<HouseholdProfileEntity> = emptyList(),
    val profileHistory: List<MeasurementEntity> = emptyList(),
    val metricsByCategory: Map<MetricCategory, List<BodyMetricItem>> = emptyMap(),
    val rawPackets: List<RawBlePacketEntity> = emptyList(),
    val hcLedger: List<HealthConnectSyncLedgerEntity> = emptyList(),
)

@HiltViewModel
class MeasurementDetailViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val db: OpenCultDatabase,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
) : ViewModel() {

    suspend fun load(measurementId: String): MeasurementDetailState {
        val m = repository.getMeasurement(measurementId) ?: return MeasurementDetailState()
        val profile = repository.getProfile(m.profileId)
        val profiles = db.householdProfileDao().getActiveProfiles()
        val pkts = repository.getPacketsForMeasurement(measurementId)
        val ledger = db.healthConnectLedgerDao().getForMeasurement(measurementId)
        val history = db.measurementDao().getRecentForProfile(m.profileId, limit = 60).reversed()

        val storedMap = repository.decodeMetricsJson(m.metricsJson)
        val provMap = repository.decodeProvenanceJson(m.provenanceJson)

        val baseReport = repository.recalculateReportFromStored(m, profile)
        val catalog = baseReport?.metrics ?: BodyMetricsEngine.calculate(
            anthropometry = org.opencult.scale.core.bia.UserAnthropometry(
                org.opencult.scale.core.bia.BiologicalSex.MALE,
                30,
                176.0,
            ),
            requestedTier = ScaleTier.CULT_PRO_FEET_ONLY,
            weightKg = m.weightKg,
            heartRateBpm = m.heartRateBpm,
        ).metrics

        val resolved = catalog.map { base ->
            val v = storedMap[base.id]
            val prov = provMap[base.id] ?: base.provenance
            val fmtVal = if (storedMap.containsKey(base.id)) {
                BodyMetricsEngine.formatStoredMetricValue(base, v)
            } else {
                base.formattedValue
            }
            base.copy(
                value = if (storedMap.containsKey(base.id)) v else base.value,
                formattedValue = fmtVal,
                provenance = prov,
                statusLabel = when {
                    base.statusLabel.isNotBlank() && v != null && prov == base.provenance -> base.statusLabel
                    prov == MetricProvenance.DIRECT_HARDWARE -> "Direct"
                    prov == MetricProvenance.DERIVED_8E_BIA -> "8E BIA"
                    prov == MetricProvenance.DERIVED_4E_BIA -> "4E BIA"
                    prov == MetricProvenance.ANTHROPOMETRIC_ESTIMATE -> "Estimated"
                    else -> "Requires 8E"
                },
            )
        }.groupBy { it.category }

        return MeasurementDetailState(
            measurement = m,
            profile = profile,
            allProfiles = profiles,
            profileHistory = history,
            metricsByCategory = resolved,
            rawPackets = pkts,
            hcLedger = ledger,
        )
    }

    fun buildTrendSeries(
        history: List<MeasurementEntity>,
        metric: BodyMetricItem,
    ): ImmutableList<MetricTrendPoint> {
        val dayFmt = DateTimeFormatter.ofPattern("d/M")
        return history.mapNotNull { row ->
            val v = if (metric.id == "weight_kg") row.weightKg else repository.decodeMetricsJson(row.metricsJson)[metric.id]
            v ?: return@mapNotNull null
            val lbl = Instant.ofEpochMilli(row.measuredAtMs).atZone(ZoneId.systemDefault()).format(dayFmt)
            MetricTrendPoint(
                label = lbl,
                value = v.toFloat(),
                badgeText = "${BodyMetricsEngine.formatStoredMetricValue(metric, v)} ${metric.unit}".trim(),
            )
        }.toImmutableList()
    }

    suspend fun reassign(measurementId: String, newProfileId: String) {
        healthConnectGateway.reassignMeasurementAndSyncHealthConnect(measurementId, newProfileId)
        syncEngine.enqueueOutboxDrain()
    }

    suspend fun deleteMeasurement(measurementId: String) {
        healthConnectGateway.deleteSyncedRecordsForMeasurement(measurementId)
        repository.softDeleteMeasurement(measurementId)
        syncEngine.enqueueOutboxDrain()
    }

    suspend fun syncMeasurementToHealthConnect(measurementId: String): String {
        val m = repository.getMeasurement(measurementId) ?: return "Measurement not found"
        val profile = repository.getProfile(m.profileId) ?: return "Profile not found"
        val modelName = when (m.source) {
            ScaleTier.CULT_PRO_8E.sourceWireName -> "Cult Smart Scale Pro (8E)"
            "ble_4e_feet" -> "Cult Scale (4E / Feet)"
            else -> "OpenCult Reading"
        }
        val summary = healthConnectGateway.syncMeasurement(m, profile, modelName)
        return if (summary.writtenRecords > 0) {
            "Synced ${summary.writtenRecords} records to Health Connect."
        } else {
            summary.skippedReason ?: "Health Connect sync skipped."
        }
    }
}

private enum class DetailMetricFilter {
    ALL,
    ACTIVE_ONLY,
}

/**
 * DRD:S-02 Measurement Detail: 56 parameters grouped across 7 accordion categories,
 * true stored tier/decimals precision, clickable Canvas Trend Explorer drill-down per metric,
 * orphan-safe member reassignment, Health Connect sync ledger, and raw BLE packet link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDetailScreen(
    measurementId: String,
    onBack: () -> Unit,
    onOpenInspector: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementDetailViewModel = hiltViewModel(),
) {
    val colors = OpenCultTheme.colors
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var detail by remember { mutableStateOf(MeasurementDetailState()) }
    var showReassign by remember { mutableStateOf(false) }
    var trendInspectMetric by remember { mutableStateOf<BodyMetricItem?>(null) }
    var metricFilter by remember { mutableStateOf(DetailMetricFilter.ALL) }
    var hcSyncFeedback by remember { mutableStateOf<String?>(null) }
    val expandedCategories = remember {
        mutableStateMapOf(
            MetricCategory.CORE_WEIGHT to true,
            MetricCategory.ADIPOSITY to true,
            MetricCategory.FLUID_BALANCE to true,
            MetricCategory.MUSCLE_METABOLIC to true,
            MetricCategory.SKELETAL_PROTEIN to true,
            MetricCategory.SEGMENTAL to true,
            MetricCategory.CIRCUMFERENCE_INDICES to true,
        )
    }

    LaunchedEffect(measurementId) {
        detail = viewModel.load(measurementId)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier
                    .openCultMicroPress(role = Role.Button) { onBack() }
                    .padding(10.dp),
            )
            Text(
                text = "Measurement Detail",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
        }

        val m = detail.measurement
        if (m == null) {
            Text("Measurement not found or deleted.", style = OpenCultTheme.typography.body, color = colors.textSecondary)
            return@Column
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.elevatedSurface) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f kg", m.weightKg),
                    style = OpenCultTheme.typography.heroMetric,
                    color = colors.textPrimary,
                )
                OpenCultStatusChip(label = detail.profile?.displayName ?: "Unassigned")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Source: ${m.source}  |  State: ${m.assignmentState}",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpenCultSecondaryButton(
                    text = "Reassign",
                    onClick = { showReassign = true },
                    modifier = Modifier.weight(1f),
                )
                OpenCultSecondaryButton(
                    text = "Packets (${detail.rawPackets.size})",
                    onClick = onOpenInspector,
                    modifier = Modifier.weight(1f),
                )
                OpenCultSecondaryButton(
                    text = "Delete",
                    onClick = {
                        scope.launch {
                            viewModel.deleteMeasurement(m.id)
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OpenCultSecondaryButton(
                text = "Sync Health Connect",
                onClick = {
                    scope.launch {
                        hcSyncFeedback = viewModel.syncMeasurementToHealthConnect(m.id)
                        detail = viewModel.load(measurementId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            hcSyncFeedback?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(msg, style = OpenCultTheme.typography.body, color = colors.brandCobalt)
            }
        }

        val allItems = detail.metricsByCategory.values.flatten()
        val activeCount = allItems.count { it.value != null }
        if (allItems.isNotEmpty()) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Biometric Parameters",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    OpenCultStatusChip(
                        label = "$activeCount / ${allItems.size} Active",
                        containerColor = if (activeCount == allItems.size) colors.bentoSageSurface else colors.cobaltSubtle,
                        contentColor = if (activeCount == allItems.size) colors.bentoSageMuted else colors.brandCobalt,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OpenCultPillSelectorRow(
                    options = persistentListOf(
                        PillOption(DetailMetricFilter.ALL, "All ${allItems.size}"),
                        PillOption(DetailMetricFilter.ACTIVE_ONLY, "Active ($activeCount)"),
                    ),
                    selected = metricFilter,
                    onSelect = { metricFilter = it },
                )
            }
        }

        if (detail.hcLedger.isNotEmpty()) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Text("Health Connect Sync Ledger", style = OpenCultTheme.typography.sectionTitle, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                detail.hcLedger.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(row.hcRecordType, style = OpenCultTheme.typography.body, color = colors.textSecondary)
                        OpenCultStatusChip(label = "${row.syncStatus} v${row.clientRecordVersion}")
                    }
                }
            }
        }

        MetricCategory.entries.forEach { category ->
            val rawRows = detail.metricsByCategory[category].orEmpty()
            val rows = if (metricFilter == DetailMetricFilter.ACTIVE_ONLY) {
                rawRows.filter { it.value != null }
            } else {
                rawRows
            }
            if (rows.isNotEmpty()) {
                val isExpanded = expandedCategories[category] ?: false
                OpenCultBentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expandedCategories[category] = !isExpanded },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${category.title} (${rows.size})",
                            style = OpenCultTheme.typography.sectionTitle,
                            color = colors.textPrimary,
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Toggle section",
                            tint = colors.textSecondary,
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rows.forEach { metric ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .openCultMicroPress(role = Role.Button) {
                                            if (metric.value != null) trendInspectMetric = metric
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${metric.prdCode} ${metric.label}",
                                            style = OpenCultTheme.typography.body,
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            text = "${metric.statusLabel} - ${metric.provenance.name.replace('_', ' ').lowercase()}",
                                            style = OpenCultTheme.typography.microLabel,
                                            color = colors.textMuted,
                                        )
                                    }
                                    Text(
                                        text = if (metric.value == null) "--" else "${metric.formattedValue} ${metric.unit}".trim(),
                                        style = OpenCultTheme.typography.numericInline,
                                        color = if (metric.value == null) colors.textMuted else colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val reassignTarget = detail.measurement
    if (showReassign && detail.allProfiles.isNotEmpty() && reassignTarget != null) {
        OpenCultPillSelectorSheet(
            title = "Reassign Measurement",
            options = detail.allProfiles.map { PillOption(it.id, it.displayName) }.toImmutableList(),
            selected = reassignTarget.profileId,
            onSelect = { newId ->
                scope.launch {
                    viewModel.reassign(reassignTarget.id, newId)
                    detail = viewModel.load(measurementId)
                }
            },
            onDismiss = { showReassign = false },
        )
    }

    val inspectTarget = trendInspectMetric
    if (inspectTarget != null) {
        val points = remember(detail.profileHistory, inspectTarget) {
            viewModel.buildTrendSeries(detail.profileHistory, inspectTarget)
        }
        var selectedPointIdx by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
        ModalBottomSheet(
            onDismissRequest = { trendInspectMetric = null },
            containerColor = colors.elevatedSurface,
            shape = OpenCultTheme.shapes.sheetSquircle,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${inspectTarget.prdCode} ${inspectTarget.label}",
                            style = OpenCultTheme.typography.sectionTitle,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = inspectTarget.statusLabel,
                            style = OpenCultTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                    OpenCultStatusChip(
                        label = "${inspectTarget.formattedValue} ${inspectTarget.unit}".trim(),
                    )
                }

                if (points.isEmpty()) {
                    Text(
                        text = "No historical readings recorded for this metric yet.",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                } else {
                    MetricTrendLineChart(
                        points = points,
                        selectedIndex = selectedPointIdx,
                        onSelectIndex = { selectedPointIdx = it },
                    )
                    val vals = points.map { it.value.toDouble() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Min: ${BodyMetricsEngine.formatStoredMetricValue(inspectTarget, vals.min())}",
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = "Avg: ${BodyMetricsEngine.formatStoredMetricValue(inspectTarget, vals.average())}",
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = "Max: ${BodyMetricsEngine.formatStoredMetricValue(inspectTarget, vals.max())}",
                            style = OpenCultTheme.typography.numericInline,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
