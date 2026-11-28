package org.opencult.scale.core.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CultBleControllerTest {

    private val defaultUser = UserAnthropometry(
        sex = BiologicalSex.MALE,
        ageYears = 31,
        heightCm = 176.0,
    )

    @Test
    fun ble_receiver_compat_context_appends_exported_flag_and_constructs_central_manager_without_crash() {
        val baseContext: Context = ApplicationProvider.getApplicationContext()
        val compat = BleReceiverCompatContext(baseContext)
        assertThat(compat.applicationContext).isSameInstanceAs(compat)

        var receivedFlag = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                receivedFlag = true
            }
        }

        // Two-arg overload would throw SecurityException on SDK >= 34 with targetSdk 36 if unpatched.
        compat.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        compat.unregisterReceiver(receiver)
        compat.unregisterReceiver(receiver) // Idempotent duplicate unregister does not crash.
        assertThat(receivedFlag).isFalse()

        val mgr = BluetoothCentralManager(
            compat,
            object : BluetoothCentralManagerCallback() {},
            Handler(Looper.getMainLooper()),
        )
        assertThat(mgr).isNotNull()
        mgr.close()
    }

    @Test
    fun refresh_and_connect_without_runtime_permissions_never_crash_and_surface_error() {
        val baseContext: Context = ApplicationProvider.getApplicationContext()
        val controller = CultBleController(baseContext)

        controller.refreshPermissionAndAdapterState()
        assertThat(controller.state.value.hasPermissions).isFalse()

        controller.startScan(defaultUser, ScaleTier.CULT_PRO_8E)
        assertThat(controller.state.value.session.phase).isEqualTo(WeighInPhase.ERROR)
        assertThat(controller.state.value.session.errorMessage).contains("permissions")

        controller.connectToDevice("AA:BB:CC:DD:EE:FF", defaultUser, ScaleTier.CULT_BF01_4E)
        assertThat(controller.state.value.session.phase).isEqualTo(WeighInPhase.ERROR)
    }

    @Test
    fun evaluates_discovered_devices_by_service_uuid_even_without_advertised_name() {
        val baseContext: Context = ApplicationProvider.getApplicationContext()
        val controller = CultBleController(baseContext)

        val unnamedFff0 = controller.evaluateDiscoveredScale(
            name = null,
            serviceUuids = listOf(CultBleConstants.SERVICE_SCALE),
            address = "11:22:33:44:55:66",
            rssi = -55,
            savedTarget = null,
        )
        assertThat(unnamedFff0).isNotNull()
        assertThat(unnamedFff0?.suggestedTier).isEqualTo(ScaleTier.CULT_PRO_8E)

        val unnamedFfb0 = controller.evaluateDiscoveredScale(
            name = null,
            serviceUuids = listOf(CultBleConstants.SERVICE_RUNSTAR_FFB0),
            address = "11:22:33:44:55:67",
            rssi = -58,
            savedTarget = null,
        )
        assertThat(unnamedFfb0).isNotNull()

        val randomSpeaker = controller.evaluateDiscoveredScale(
            name = "JBL Flip",
            serviceUuids = listOf(CultBleConstants.SERVICE_BATTERY),
            address = "11:22:33:44:55:68",
            rssi = -40,
            savedTarget = null,
        )
        assertThat(randomSpeaker).isNull()
    }

    @Test
    fun parses_and_completes_runstar_0xffb0_two_part_8e_and_single_part_frames() {
        val sm = ScaleSessionStateMachine(defaultUser, ScaleTier.CULT_PRO_8E)
        sm.markConnecting("Runstar R6", "AA:BB:CC:DD:EE:09").also { sm.markConnected() }

        // Build two-part 0xA3 frames matching RunstarR6Handler 20-byte checksum: sum(bytes[3..18]) & 0x1F
        fun withRunstarChecksum(raw: ByteArray): ByteArray = raw.also {
            var sum = 0
            for (i in 3..18) sum += it[i].toInt() and 0xFF
            it[19] = (sum and 0x1F).toByte()
        }

        // Part 1: seq=1, len=0x16 (>0x10), part=0x00, type=0xA3, status=0x00, weight=74800g (0x012430), hr=68
        // channels[0..4] (0.1 ohm): trunk=22.0 (220=0x00DC), limbs=310.0(0x0C1C), 305.0(0x0BEA), 255.0(0x09F6), 260.0(0x0A28)
        val part1 = withRunstarChecksum(
            byteArrayOf(
                0x01, 0x16, 0x00, 0xA3.toByte(),
                0x00,
                0x01, 0x24, 0x30,
                68.toByte(),
                0x00, 0xDC.toByte(),
                0x0C, 0x1C,
                0x0B, 0xEA.toByte(),
                0x09, 0xF6.toByte(),
                0x0A, 0x28,
                0x00,
            ),
        )
        // Part 2: seq=1, len=0x16, part=0x01, channels[5..9]: trunk=20.0(200=0x00C8), limbs=270.0(0x0A8C), 272.0(0x0AA0), 220.0(0x0898), 225.0(0x08CA)
        val part2 = withRunstarChecksum(
            byteArrayOf(
                0x01, 0x16, 0x01,
                0x00, 0xC8.toByte(),
                0x0A, 0x8C.toByte(),
                0x0A, 0xA0.toByte(),
                0x08, 0x98.toByte(),
                0x08, 0xCA.toByte(),
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00,
            ),
        )

        val midSnap = sm.onRawPacket(CultBleConstants.CHAR_RUNSTAR_RESULT_SHORT, part1)
        assertThat(midSnap.phase).isEqualTo(WeighInPhase.HOLD_HANDLES_BIA)
        assertThat(midSnap.completedReport).isNull()

        val doneSnap = sm.onRawPacket(CultBleConstants.CHAR_RUNSTAR_RESULT_SHORT, part2)
        assertThat(doneSnap.phase).isEqualTo(WeighInPhase.LOCKED_COMPLETE)
        assertThat(doneSnap.completedReport?.weightKg).isWithin(0.01).of(74.80)
        assertThat(doneSnap.completedReport?.heartRateBpm).isEqualTo(68)
        assertThat(doneSnap.completedReport?.metrics).hasSize(56)
    }
}
