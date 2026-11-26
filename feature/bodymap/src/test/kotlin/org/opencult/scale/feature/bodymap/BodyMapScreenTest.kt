package org.opencult.scale.feature.bodymap

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
}
