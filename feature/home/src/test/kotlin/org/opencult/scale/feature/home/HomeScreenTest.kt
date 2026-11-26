package org.opencult.scale.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.DailyHealthContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_empty_state_renders_honest_prompt_and_keeps_health_connect_visible() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                HomeScreenContent(
                    state = HomeDashboardUiState(
                        dailyHealthContext = DailyHealthContext(LocalDate.now(), 6100L, 380L, 1850),
                        hcGrantedCount = 10,
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

        composeRule.onNodeWithText("No Reading Today").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("--.- kg").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start Weigh-In").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("6100 steps").performScrollTo().assertIsDisplayed()
    }
}
