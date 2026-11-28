package org.opencult.scale.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_carousel_traverses_4_steps_validates_zero_default_profile_and_saves_selected_sku() {
        var savedName: String? = null
        var savedHeight = 0.0
        var savedTier: ScaleTier? = null

        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                OnboardingContent(
                    bleGranted = false,
                    hcGrantedCount = 3,
                    onRequestBlePermissions = {},
                    onRequestHealthConnectPermissions = {},
                    onSaveProfile = { name, _, _, height, _, _, _, tier ->
                        savedName = name
                        savedHeight = height
                        savedTier = tier
                    },
                )
            }
        }

        // Step 1: Welcome
        composeRule.onNodeWithText("Own Every Metric.").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started").performScrollTo().performClick()

        // Step 2: Account & Email Verification -> proceed offline
        composeRule.onNodeWithText("Account & Sync").assertIsDisplayed()
        composeRule.onNodeWithText("Continue Offline (Local Storage Only)").performScrollTo().performClick()

        // Step 3: Equal-height Permissions & HC 8-schema disclosure
        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Health Connect").assertIsDisplayed()
        composeRule.onNodeWithText("8 Health Connect Schemas vs Full Scale Dataset").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next: Scale & Profile").performScrollTo().performClick()

        // Step 4: Only two physical Cult SKUs & empty anthropometry validation
        composeRule.onNodeWithText("Cult Smart Scale Pro (8E)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cult Scale CS-BF01 (4E)").performScrollTo().assertIsDisplayed()

        // Submitting empty form shows validation and does not fabricate defaults
        composeRule.onNodeWithText("Enter OpenCult Dashboard").performScrollTo().performClick()
        assertThat(savedName).isNull()

        composeRule.onNodeWithText("Your name").performScrollTo().performTextInput("Alex")
        composeRule.onNodeWithText("170").performScrollTo().performTextInput("174")
        composeRule.onNodeWithText("Cult Scale CS-BF01 (4E)").performScrollTo().performClick()
        composeRule.onNodeWithText("Enter OpenCult Dashboard").performScrollTo().performClick()

        assertThat(savedName).isEqualTo("Alex")
        assertThat(savedHeight).isWithin(0.01).of(174.0)
        assertThat(savedTier).isEqualTo(ScaleTier.CULT_BF01_4E)
    }

    @Test
    fun onboarding_account_step_blocks_disposable_email_domain_inline() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                OnboardingContent(
                    bleGranted = false,
                    hcGrantedCount = 0,
                    authState = OnboardingAuthState(
                        backendConfigured = true,
                        turnstileRequired = false,
                    ),
                    onRequestBlePermissions = {},
                    onRequestHealthConnectPermissions = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    initialPage = 1,
                )
            }
        }

        composeRule.onNodeWithText("name@gmail.com").performTextInput("bot@mailinator.com")
        composeRule.onNodeWithText("8+ chars, upper, lower, number").performTextInput("OpenCult2026")
        composeRule.onNodeWithText("Temporary or disposable email addresses are blocked. Please use a permanent email address.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Create Verified Account").assertIsNotEnabled()
    }
}
