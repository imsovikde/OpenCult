package org.opencult.scale.core.designsystem

import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.designsystem.components.AnatomicalBodyMapCanvas
import org.opencult.scale.core.designsystem.components.AnatomicalSegment
import org.opencult.scale.core.designsystem.components.BodyFatArcGauge
import org.opencult.scale.core.designsystem.components.DetachedObsidianDock
import org.opencult.scale.core.designsystem.components.HeroWeightSparkChart
import org.opencult.scale.core.designsystem.components.MetricTrendLineChart
import org.opencult.scale.core.designsystem.components.MetricTrendPoint
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultDestination
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultTextField
import org.opencult.scale.core.designsystem.components.SegmentTier
import org.opencult.scale.core.designsystem.components.SparkBarEntry
import org.opencult.scale.core.designsystem.components.StippledBarChart
import org.opencult.scale.core.designsystem.components.StippledBarEntry
import org.opencult.scale.core.designsystem.theme.DaylightColors
import org.opencult.scale.core.designsystem.theme.DefaultOpenCultTypography
import org.opencult.scale.core.designsystem.theme.ObsidianColors
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignSystemGovernanceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun daylight_and_obsidian_tokens_match_drd_gauge_palette_and_tnum() {
        assertThat(DaylightColors.canvas.toArgb()).isEqualTo(0xFFF4F6FA.toInt())
        assertThat(DaylightColors.gaugeOptimalBlue.toArgb()).isEqualTo(0xFF2185F7.toInt())
        assertThat(DaylightColors.gaugeElevatedAmber.toArgb()).isEqualTo(0xFFF4B428.toInt())
        assertThat(DaylightColors.gaugeHighCoral.toArgb()).isEqualTo(0xFFEF4444.toInt())
        assertThat(ObsidianColors.canvas.toArgb()).isEqualTo(0xFF0A0D12.toInt())
        assertThat(DefaultOpenCultTypography.heroMetric.fontFeatureSettings).isEqualTo("tnum")
        assertThat(DefaultOpenCultTypography.bentoStat.fontFeatureSettings).isEqualTo("tnum")
        assertThat(DefaultOpenCultTypography.monoHex.fontFeatureSettings).isEqualTo("tnum")
    }

    @Test
    fun reduced_motion_collapses_all_specs_to_snap_zero_ms() {
        assertThat(OpenCultMotion.spatialDefault<Float>(true)).isInstanceOf(SnapSpec::class.java)
        assertThat(OpenCultMotion.pressFeedback<Float>(true)).isInstanceOf(SnapSpec::class.java)
        assertThat(OpenCultMotion.telemetryGlide<Float>(true)).isInstanceOf(SnapSpec::class.java)
        assertThat(OpenCultMotion.staggerDelay(3, reducedMotion = true)).isEqualTo(0)
        assertThat(OpenCultMotion.staggerDelay(10, reducedMotion = false)).isEqualTo(160)
    }

    @Test
    fun dock_buttons_and_primary_cta_meet_48dp_minimum_touch_targets_on_360dp_width() {
        var selected by mutableStateOf(OpenCultDestination.HOME)
        var measured = false

        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                Column(modifier = Modifier.width(360.dp)) {
                    OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                        OpenCultPrimaryButton(text = "Commit Measurement", onClick = { measured = true })
                    }
                    Spacer(Modifier.height(16.dp))
                    DetachedObsidianDock(
                        selectedDestination = selected,
                        onSelectDestination = { selected = it },
                        onMeasureClick = { measured = true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Commit Measurement").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Start weigh-in")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertThat(measured).isTrue()

        composeRule.onNodeWithContentDescription("Reports")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertThat(selected).isEqualTo(OpenCultDestination.REPORTS)
    }

    @Test
    fun custom_canvas_charts_render_cleanly_in_obsidian_and_daylight_without_overflow() {
        var barSelection by mutableIntStateOf(2)
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
                Column(modifier = Modifier.width(360.dp)) {
                    HeroWeightSparkChart(
                        entries = persistentListOf(
                            SparkBarEntry(72.4f),
                            SparkBarEntry(72.1f),
                            SparkBarEntry(72.9f, isSpike = true),
                            SparkBarEntry(71.8f),
                        ),
                    )
                    BodyFatArcGauge(bodyFatPct = 18.4f)
                    StippledBarChart(
                        entries = persistentListOf(
                            StippledBarEntry("1", "M", 72.1f, "72.1"),
                            StippledBarEntry("2", "T", 71.8f, "71.8"),
                            StippledBarEntry("3", "W", 71.4f, "71.4"),
                        ),
                        selectedIndex = barSelection,
                        onSelectIndex = { barSelection = it },
                    )
                    AnatomicalBodyMapCanvas(
                        selectedSegment = AnatomicalSegment.LEFT_ARM,
                        segmentTiers = mapOf(AnatomicalSegment.LEFT_ARM to SegmentTier.OPTIMAL),
                        supports8Electrode = true,
                        onSelectSegment = {},
                    )
                    MetricTrendLineChart(
                        points = persistentListOf(
                            MetricTrendPoint("1/11", 75.2f, "75.2 kg"),
                            MetricTrendPoint("8/11", 74.8f, "74.8 kg"),
                        ),
                        selectedIndex = 1,
                        onSelectIndex = {},
                    )
                    OpenCultTextField(
                        value = "72.0",
                        onValueChange = {},
                        label = "Target (kg)",
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Recent weight spark-bars").assertExists()
        composeRule.onNodeWithContentDescription("Body fat classification arc gauge at 18.4 percent").assertExists()
        composeRule.onNodeWithContentDescription("Trend stippled bar chart").assertExists()
        composeRule.onNodeWithContentDescription("Five-segment anatomical body composition map").assertExists()
        composeRule.onNodeWithContentDescription("Interactive metric trend explorer").assertExists()
    }
}
