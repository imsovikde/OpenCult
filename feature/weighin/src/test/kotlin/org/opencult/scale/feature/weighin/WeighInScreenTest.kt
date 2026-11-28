package org.opencult.scale.feature.weighin

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.ble.BleHardwareUiState
import org.opencult.scale.core.ble.DiscoveredScaleDevice
import org.opencult.scale.core.ble.ParsedScaleFrame
import org.opencult.scale.core.ble.ScaleFrameParser
import org.opencult.scale.core.ble.ScaleSessionSnapshot
import org.opencult.scale.core.ble.WeighInPhase
import org.opencult.scale.core.ble.hexToByteArray
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SecureVault
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class WeighInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun member_lock_and_active_profile_persist_across_shared_active_member_session() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = SecureVault(context)
        val session = ActiveMemberSession(vault)

        session.selectActiveProfile("member-alex")
        session.setMemberLockEnabled(true)

        val reloaded = ActiveMemberSession(SecureVault(context))
        val uiContext = WeighInMemberContext(activeProfile = null, memberLockEnabled = reloaded.memberLockEnabled.value)
        assertThat(reloaded.activeProfileId.value).isEqualTo("member-alex")
        assertThat(uiContext.memberLockEnabled).isTrue()
        assertThat(ScaleTier.CULT_PRO_8E.sourceWireName).isEqualTo("ble_8e_handles")
        assertThat(ScaleTier.CULT_BF01_4E.sourceWireName).isEqualTo("ble_4e_feet")
    }

    @Test
    fun simulated_cs_bf01_and_pro_8e_vectors_compute_honest_active_metric_counts() {
        val csFrame = "CF 3C C0 4C 1D F4 01 00 00 00 97".hexToByteArray()
        val parsed = ScaleFrameParser.parse(csFrame, ScaleTier.CULT_BF01_4E, deviceName = "CS-BF01")
            as ParsedScaleFrame.WeightFrame
        val report4e = BodyMetricsEngine.calculate(
            UserAnthropometry(BiologicalSex.MALE, 30, 178.0),
            ScaleTier.CULT_BF01_4E,
            parsed.weightKg,
            parsed.heartRateBpm,
            singleFreq50KhzOhm = parsed.singleFreq50KhzOhm,
        )
        assertThat(report4e.metrics.count { it.value != null }).isAtLeast(35)
        assertThat(report4e.segmentalMuscle).isNull()
    }

    @Test
    fun ready_stage_radar_and_text_have_zero_overlap_and_no_simulator_buttons_present() {
        var permRequested = false
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
                WeighInContent(
                    bleState = BleHardwareUiState(
                        hasPermissions = false,
                        isBluetoothEnabled = true,
                        discoveredDevices = listOf(
                            DiscoveredScaleDevice(
                                name = "Cult Smart Scale Pro Very Long Edition",
                                address = "AA:BB:CC:DD:EE:01",
                                rssi = -54,
                                suggestedTier = ScaleTier.CULT_PRO_8E,
                            ),
                        ),
                        session = ScaleSessionSnapshot(phase = WeighInPhase.IDLE),
                    ),
                    memberContext = WeighInMemberContext(memberLockEnabled = false),
                    selectedTier = ScaleTier.CULT_PRO_8E,
                    bleGranted = false,
                    bluetoothEnabled = true,
                    isCommitting = false,
                    onSelectTier = {},
                    onToggleMemberLock = {},
                    onScanOrGateClick = { permRequested = true },
                    onRequestBlePermissions = { permRequested = true },
                    onOpenAppSettings = {},
                    onConnectDiscovered = {},
                    onConfirmFeetOnly = {},
                    onCommitMeasurement = {},
                    onClose = {},
                )
            }
        }

        val radarBounds = composeRule.onNodeWithTag("weighin_radar_container").getUnclippedBoundsInRoot()
        val statusBounds = composeRule.onNodeWithTag("weighin_status_container").getUnclippedBoundsInRoot()
        assertThat(radarBounds.bottom.value).isLessThan(statusBounds.top.value)

        // Discovered Connect chip stays visible alongside long scale name
        composeRule.onNodeWithText("Connect").performScrollTo().assertIsDisplayed()

        // No simulation replay controls leaked on production WeighInScreen
        assertThat(composeRule.onAllNodesWithText("Sim Pro 8E").fetchSemanticsNodes()).isEmpty()
        assertThat(composeRule.onAllNodesWithText("Simulated Hardware Fixture Replay").fetchSemanticsNodes()).isEmpty()

        // Missing BLE permission redirects primary button directly to permission request
        composeRule.onNodeWithText("Allow Bluetooth Access").performScrollTo().performClick()
        assertThat(permRequested).isTrue()
    }

    @Test
    fun stage_2_and_stage_3_render_live_weight_and_contact_sweep() {
        composeRule.setContent {
            OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
                WeighInContent(
                    bleState = BleHardwareUiState(
                        hasPermissions = true,
                        isBluetoothEnabled = true,
                        session = ScaleSessionSnapshot(
                            phase = WeighInPhase.HOLD_HANDLES_BIA,
                            liveWeightKg = 74.80,
                            heartRateBpm = 68,
                            dfPacketCount = 3,
                        ),
                    ),
                    memberContext = WeighInMemberContext(memberLockEnabled = true),
                    selectedTier = ScaleTier.CULT_PRO_8E,
                    bleGranted = true,
                    bluetoothEnabled = true,
                    isCommitting = false,
                    onSelectTier = {},
                    onToggleMemberLock = {},
                    onScanOrGateClick = {},
                    onRequestBlePermissions = {},
                    onOpenAppSettings = {},
                    onConnectDiscovered = {},
                    onConfirmFeetOnly = {},
                    onCommitMeasurement = {},
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("74.80 kg").assertIsDisplayed()
        composeRule.onNodeWithText("68 bpm").assertIsDisplayed()
        composeRule.onNodeWithText("3/4").assertIsDisplayed()
    }
}
