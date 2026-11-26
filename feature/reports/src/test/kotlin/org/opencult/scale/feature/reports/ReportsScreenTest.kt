package org.opencult.scale.feature.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reports_empty_state_shows_dash_percent_and_filter_pills_without_fake_fallback() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                ReportsScreenContent(
                    state = ReportsUiState(),
                    metricLookup = { _, _ -> null },
                )
            }
        }

        composeRule.onNodeWithText("--%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("No Readings").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0 pts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("7D").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("30D").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("All").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("All 55").performScrollTo().assertIsDisplayed()
    }
}
