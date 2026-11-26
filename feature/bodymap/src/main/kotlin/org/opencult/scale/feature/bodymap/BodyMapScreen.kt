package org.opencult.scale.feature.bodymap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.opencult.scale.core.bia.FullBodyCompositionReport
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.designsystem.components.AnatomicalBodyMapCanvas
import org.opencult.scale.core.designsystem.components.AnatomicalSegment
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultSectionHeader
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.SegmentTier
import org.opencult.scale.core.designsystem.components.dockScrollClearance
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.sync.ActiveMemberSession
import java.util.Locale
import javax.inject.Inject

data class BodyMapUiState(
    val memberName: String = "Member",
    val latestMeasurement: MeasurementEntity? = null,
    val metrics: Map<String, Double?> = emptyMap(),
    val report: FullBodyCompositionReport? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BodyMapViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    activeMemberSession: ActiveMemberSession,
) : ViewModel() {

    val uiState: StateFlow<BodyMapUiState> = combine(
        repository.observeActiveProfiles(),
        repository.observeAllMeasurements(),
        activeMemberSession.activeProfileId,
    ) { profiles, all, activeId ->
        val p = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        val latest = p?.let { prof -> all.firstOrNull { it.profileId == prof.id } }
        Triple(p, latest, latest?.let { repository.decodeMetricsJson(it.metricsJson) } ?: emptyMap())
    }.mapLatest { (profile, latest, metricsMap) ->
        val report = if (latest != null && profile != null) {
            repository.recalculateReportFromStored(latest, profile)
        } else {
            null
        }
        BodyMapUiState(
            memberName = profile?.displayName ?: "Member",
            latestMeasurement = latest,
            metrics = metricsMap,
            report = report,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BodyMapUiState())
}

@Composable
fun BodyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: BodyMapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BodyMapContent(state = state, modifier = modifier)
}

@Composable
fun BodyMapContent(
    state: BodyMapUiState,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()
    var selectedSegment by remember { mutableStateOf(AnatomicalSegment.TRUNK) }

    val latest = state.latestMeasurement
    val is8E = latest?.source == ScaleTier.CULT_PRO_8E.sourceWireName && state.report?.segmentalMuscle != null
    val report = state.report
    val pctStd = report?.segmentalMusclePctStd.orEmpty()

    fun tierForPct(pct: Double?): SegmentTier = when {
        pct == null -> SegmentTier.LOCKED
        pct >= 102.0 -> SegmentTier.OPTIMAL
        pct >= 94.0 -> SegmentTier.BALANCED
        else -> SegmentTier.ELEVATED
    }

    val segmentTiers = remember(pctStd, is8E) {
        if (!is8E) {
            AnatomicalSegment.entries.associateWith { SegmentTier.LOCKED }
        } else {
            mapOf(
                AnatomicalSegment.LEFT_ARM to tierForPct(pctStd["left_arm"]),
                AnatomicalSegment.RIGHT_ARM to tierForPct(pctStd["right_arm"]),
                AnatomicalSegment.TRUNK to tierForPct(pctStd["trunk"]),
                AnatomicalSegment.LEFT_LEG to tierForPct(pctStd["left_leg"]),
                AnatomicalSegment.RIGHT_LEG to tierForPct(pctStd["right_leg"]),
            )
        }
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
                text = "8-Electrode Body Map",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            OpenCultStatusChip(
                label = if (is8E) "8E Verified" else "8E Locked",
                containerColor = if (is8E) colors.bentoSageSurface else colors.bentoPeachSurface,
                contentColor = if (is8E) colors.bentoSageMuted else colors.bentoPeachFlame,
            )
        }

        if (!is8E) {
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.bentoPeachSurface,
                borderColor = colors.bentoPeachFlame.copy(alpha = 0.4f),
            ) {
                Text(
                    text = "8-Electrode Handle Capture Required",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.bentoPeachFlame,
                )
                Text(
                    text = "Latest reading for ${state.memberName} used ${latest?.source ?: "no scale"}. Segmental limb muscle/fat and dual-frequency ECW/ICW stay suppressed on 4-electrode or Feet-Only hardware.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textPrimary,
                )
            }
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.elevatedSurface) {
            AnatomicalBodyMapCanvas(
                selectedSegment = selectedSegment,
                segmentTiers = segmentTiers,
                supports8Electrode = is8E,
                onSelectSegment = { selectedSegment = it },
            )
        }

        val segMuscle = report?.segmentalMuscle
        val segFat = report?.segmentalFat
        val (segKey, mKg, fKg) = when (selectedSegment) {
            AnatomicalSegment.LEFT_ARM -> Triple("left_arm", segMuscle?.leftArmKg, segFat?.leftArmKg)
            AnatomicalSegment.RIGHT_ARM -> Triple("right_arm", segMuscle?.rightArmKg, segFat?.rightArmKg)
            AnatomicalSegment.TRUNK -> Triple("trunk", segMuscle?.trunkKg, segFat?.trunkKg)
            AnatomicalSegment.LEFT_LEG -> Triple("left_leg", segMuscle?.leftLegKg, segFat?.leftLegKg)
            AnatomicalSegment.RIGHT_LEG -> Triple("right_leg", segMuscle?.rightLegKg, segFat?.rightLegKg)
        }
        val mPct = pctStd[segKey]

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bentoLavenderSurface) {
            Text(
                text = "${selectedSegment.displayName} (${state.memberName})",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Segment Muscle", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                    Text(
                        text = mKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--",
                        style = OpenCultTheme.typography.bentoStat,
                        color = colors.textPrimary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("% of Standard", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                    Text(
                        text = mPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--",
                        style = OpenCultTheme.typography.bentoStat,
                        color = colors.textPrimary,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Regional Fat: " + (fKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--"),
                style = OpenCultTheme.typography.numericInline,
                color = colors.textSecondary,
            )
        }

        OpenCultSectionHeader(title = "Bilateral Symmetry & Fluid Balance")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
        ) {
            OpenCultBentoCard(modifier = Modifier.weight(1f), containerColor = colors.bentoSageSurface) {
                Text("Upper Symmetry", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                Text(
                    text = report?.upperSymmetryPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--",
                    style = OpenCultTheme.typography.bentoStat,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Lower: " + (report?.lowerSymmetryPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"),
                    style = OpenCultTheme.typography.numericInline,
                    color = colors.textSecondary,
                )
            }

            OpenCultBentoCard(modifier = Modifier.weight(1f), containerColor = colors.cardSurface) {
                Text("Dual-Freq ECW/TBW", style = OpenCultTheme.typography.microLabel, color = colors.textSecondary)
                Text(
                    text = report?.ecwTbwRatio?.let { String.format(Locale.US, "%.3f", it) } ?: "--",
                    style = OpenCultTheme.typography.bentoStat,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ALM: " + (report?.appendicularLeanMassKg?.let { String.format(Locale.US, "%.2f kg", it) } ?: "--"),
                    style = OpenCultTheme.typography.numericInline,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Preview(name = "BodyMap Daylight Locked", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun BodyMapLockedPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        BodyMapContent(BodyMapUiState(memberName = "Souvik"))
    }
}
