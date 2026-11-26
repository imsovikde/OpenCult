package org.opencult.scale.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_shows_only_two_physical_cult_skus_and_triggers_save_callback() {
        var savedTier: ScaleTier? = null
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                OnboardingContent(
                    bleGranted = false,
                    hcGrantedCount = 3,
                    onRequestBlePermissions = {},
                    onRequestHealthConnectPermissions = {},
                    onSaveProfile = { _, _, _, _, _, _, _, tier -> savedTier = tier },
                )
            }
        }

        composeRule.onNodeWithText("Bluetooth").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Health Connect").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cult Smart Scale Pro (8E)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cult Scale CS-BF01 (4E)").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Cult Scale CS-BF01 (4E)").performClick()
        composeRule.onNodeWithText("Enter OpenCult Dashboard").performScrollTo().performClick()
        assertThat(savedTier).isEqualTo(ScaleTier.CULT_BF01_4E)
    }
}
