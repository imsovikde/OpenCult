package org.opencult.scale.feature.bodymap

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.DualFreqImpedance
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyMapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bodymap_gates_segmental_limbs_when_no_8e_measurement_is_available() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
                BodyMapContent(
                    state = BodyMapUiState(memberName = "Souvik"),
                )
            }
        }

        composeRule.onNodeWithText("8-Electrode Body Map").assertIsDisplayed()
        composeRule.onNodeWithText("8E Locked").assertIsDisplayed()
        composeRule.onNodeWithText("8-Electrode Handle Capture Required").assertIsDisplayed()
    }

    @Test
    fun bodymap_switches_between_muscle_and_fat_layers_on_8e_reading() {
        val report = BodyMetricsEngine.calculate(
            UserAnthropometry(BiologicalSex.MALE, 30, 178.0),
            ScaleTier.CULT_PRO_8E,
            74.8,
            68,
            DualFreqImpedance(307.4, 311.3, 26.8, 242.1, 256.2, 267.6, 275.5, 24.5, 219.1, 229.5),
        )
        val measurement = MeasurementEntity(
            id = "m-8e",
            accountId = "acc",
            profileId = "p1",
            scaleDeviceId = null,
            measuredAtMs = 1_760_000_000_000L,
            localDate = "2026-11-26",
            source = ScaleTier.CULT_PRO_8E.sourceWireName,
            assignmentState = "confirmed",
            weightKg = 74.8,
            metricsJson = "{}",
            provenanceJson = "{}",
            mutationId = "mut-1",
            createdAtMs = 1_760_000_000_000L,
            updatedAtMs = 1_760_000_000_000L,
        )

        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                BodyMapContent(
                    state = BodyMapUiState(
                        memberName = "Souvik",
                        latestMeasurement = measurement,
                        report = report,
                    ),
                )
            }
        }

        composeRule.onNodeWithText("8E Verified").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Segment Muscle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fat").performScrollTo().performClick()
        composeRule.onNodeWithText("Regional Fat Mass").performScrollTo().assertIsDisplayed()
    }
}
