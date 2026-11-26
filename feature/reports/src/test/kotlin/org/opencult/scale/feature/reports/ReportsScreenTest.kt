package org.opencult.scale.feature.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

        composeRule.onNodeWithText("--%").assertIsDisplayed()
        composeRule.onNodeWithText("No Readings").assertIsDisplayed()
        composeRule.onNodeWithText("7D").assertIsDisplayed()
        composeRule.onNodeWithText("30D").assertIsDisplayed()
        composeRule.onNodeWithText("All").assertIsDisplayed()
    }
}
