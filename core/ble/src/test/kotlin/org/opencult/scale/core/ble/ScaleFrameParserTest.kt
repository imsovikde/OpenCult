package org.opencult.scale.core.ble

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry

class ScaleFrameParserTest {

    private val defaultUser = UserAnthropometry(
        sex = BiologicalSex.MALE,
        ageYears = 31,
        heightCm = 176.0,
    )

    @Test
    fun parses_onebyone_live_and_socks_zero_impedance_frames() {
        val live = "CF 50 0F B0 27 10 13 51 00 00 55".hexToByteArray()
        val parsedLive = ScaleFrameParser.parse(live, ScaleTier.CULT_BF01_4E)
            as ParsedScaleFrame.WeightFrame

        assertThat(parsedLive.checksumValid).isTrue()
        assertThat(parsedLive.weightKg).isWithin(0.01).of(101.60)
        assertThat(parsedLive.singleFreq50KhzOhm).isWithin(0.1).of(392.0)
        assertThat(parsedLive.isHistory).isFalse()
        assertThat(parsedLive.coalescedDuplicateDropped).isFalse()

        val socks = "CF 00 00 3C 28 00 00 00 01 00 DA".hexToByteArray()
        val parsedSocks = ScaleFrameParser.parse(socks, ScaleTier.CULT_BF01_4E)
            as ParsedScaleFrame.WeightFrame
        assertThat(parsedSocks.checksumValid).isTrue()
        assertThat(parsedSocks.weightKg).isWithin(0.01).of(103.00)
        assertThat(parsedSocks.singleFreq50KhzOhm).isNull()
    }

    @Test
    fun parses_cs_bf01_occult_body_phase_hr_and_le_impedance_and_ignores_noise() {
        // Occult CS-BF01 frame: 75.0 kg, byte 1 = 0x3C (60 bpm), bytes 5..6 LE = 0x01F4 (500 ohm), status 0x00
        val bf01Body = "CF 3C C0 4C 1D F4 01 00 00 00 97".hexToByteArray()
        val parsed = ScaleFrameParser.parse(bf01Body, ScaleTier.CULT_BF01_4E, deviceName = "CS-BF01")
            as ParsedScaleFrame.WeightFrame

        assertThat(parsed.checksumValid).isTrue()
        assertThat(parsed.weightKg).isWithin(0.01).of(75.00)
        assertThat(parsed.heartRateBpm).isEqualTo(60)
        assertThat(parsed.singleFreq50KhzOhm).isWithin(0.1).of(500.0)
        assertThat(parsed.isBf01Locked).isTrue()

        // Occult noise frame on CS-BF01 with out-of-range random [5..7]: suppressed cleanly
        val noisy = byteArrayOf(
            0xCF.toByte(), 0x42, 0x10, 0x4C, 0x1D,
            0x32, 0x33, 0xDF.toByte(), 0x00, 0x00, 0x00,
        ).also { arr ->
            var x = 0
            for (i in 0 until 10) x = x xor (arr[i].toInt() and 0xFF)
            arr[10] = x.toByte()
        }
        val parsedNoisy = ScaleFrameParser.parse(noisy, ScaleTier.CULT_BF01_4E, deviceName = "CS-BF01")
            as ParsedScaleFrame.WeightFrame
        assertThat(parsedNoisy.checksumValid).isTrue()
        assertThat(parsedNoisy.heartRateBpm).isEqualTo(66)
        assertThat(parsedNoisy.singleFreq50KhzOhm).isNull()
    }

    @Test
    fun build_mode_setup_computes_xor_dynamically_across_kg_lb_and_st() {
        val kg = CultBleConstants.buildModeSetupCommand(CultBleConstants.UNIT_KG)
        val lb = CultBleConstants.buildModeSetupCommand(CultBleConstants.UNIT_LB)
        val st = CultBleConstants.buildModeSetupCommand(CultBleConstants.UNIT_ST)

        fun xor0To9(b: ByteArray): Byte {
            var acc = 0
            for (i in 0 until 10) acc = acc xor (b[i].toInt() and 0xFF)
            return acc.toByte()
        }
        assertThat(kg[10]).isEqualTo(xor0To9(kg))
        assertThat(lb[10]).isEqualTo(xor0To9(lb))
        assertThat(st[10]).isEqualTo(xor0To9(st))
        assertThat(kg[10]).isNotEqualTo(lb[10])
    }

    @Test
    fun rejects_20_byte_coalesced_duplicate_live_frames_as_history_inv21() {
        val coalesced1 = "CF 32 0F B0 27 FD F7 0D 00 00 62 CF 32 0F B0 27 FD F7 0D 00".hexToByteArray()
        val parsed1 = ScaleFrameParser.parse(coalesced1, ScaleTier.CULT_BF01_4E)
            as ParsedScaleFrame.WeightFrame
        assertThat(parsed1.checksumValid).isTrue()
        assertThat(parsed1.isHistory).isFalse()
        assertThat(parsed1.coalescedDuplicateDropped).isTrue()
        assertThat(parsed1.weightKg).isWithin(0.01).of(101.60)

        val coalesced2 = "CF 76 0C 1E 28 DF D6 07 01 00 8C CF 76 0C 1E 28 DF D6 07 01".hexToByteArray()
        val parsed2 = ScaleFrameParser.parse(coalesced2, ScaleTier.CULT_BF01_4E)
            as ParsedScaleFrame.WeightFrame
        assertThat(parsed2.isHistory).isFalse()
        assertThat(parsed2.coalescedDuplicateDropped).isTrue()
        assertThat(parsed2.weightKg).isWithin(0.01).of(102.70)
        assertThat(parsed2.singleFreq50KhzOhm).isWithin(0.1).of(319.0)
    }

    @Test
    fun parses_genuine_18_byte_onebyone_history_frame_with_iso_timestamp() {
        val history = "CF 60 0E 9C 27 04 02 F0 00 00 11 07 EA 08 0C 09 0B 2B".hexToByteArray()
        val parsed = ScaleFrameParser.parse(history, ScaleTier.CULT_BF01_4E)
            as ParsedScaleFrame.WeightFrame

        assertThat(parsed.isHistory).isTrue()
        assertThat(parsed.coalescedDuplicateDropped).isFalse()
        assertThat(parsed.weightKg).isWithin(0.01).of(101.40)
        assertThat(parsed.historyIsoTimestamp).isEqualTo("2026-08-12T09:11:43Z")
    }

    @Test
    fun parses_bodypod_10_channel_dual_frequency_0xbe_packets_with_valid_xor() {
        val pkt0 = "be 00 02 0c 29 0c 0c 01 75 09 02 0a ec".hexToByteArray()
        val pkt1 = "be 01 74 0a c3 0a f5 00 8f 08 f7 08 85".hexToByteArray()

        val low = ScaleFrameParser.parse(pkt0, ScaleTier.CULT_PRO_8E)
            as ParsedScaleFrame.DualFreqImpedancePacket
        val high = ScaleFrameParser.parse(pkt1, ScaleTier.CULT_PRO_8E)
            as ParsedScaleFrame.DualFreqImpedancePacket

        assertThat(low.checksumValid).isTrue()
        assertThat(low.packetIndex).isEqualTo(0)
        assertThat(low.scaledOhmChannels).containsExactly(307.4, 311.3, 26.8, 242.1, 256.2).inOrder()

        assertThat(high.checksumValid).isTrue()
        assertThat(high.packetIndex).isEqualTo(1)
        assertThat(high.scaledOhmChannels).containsExactly(267.6, 275.5, 24.5, 219.1, 229.5).inOrder()
    }

    @Test
    fun stores_all_4_0xdf_packets_verbatim_before_locking_and_supports_grace_deadline_inv01() {
        val sm = ScaleSessionStateMachine(defaultUser, ScaleTier.CULT_PRO_8E)
        sm.markConnecting("CULT_PRO_01", "AA:BB:CC:DD:EE:01").also { sm.markConnected() }

        val cfBytes = byteArrayOf(
            0xCF.toByte(), 68.toByte(), 0x00, 0x38, 0x1D, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00,
        ).also { arr ->
            var x = 0
            for (i in 0 until 10) x = x xor (arr[i].toInt() and 0xFF)
            arr[10] = x.toByte()
        }
        val be0 = "be 00 02 0c 29 0c 0c 01 75 09 02 0a ec".hexToByteArray()
        val be1 = "be 01 74 0a c3 0a f5 00 8f 08 f7 08 85".hexToByteArray()
        fun dfPacket(seqA: Int): ByteArray = ByteArray(17) { idx ->
            when (idx) {
                0 -> 0xDF.toByte()
                2 -> seqA.toByte()
                3 -> 0x04
                else -> (idx * 7 + seqA).toByte()
            }
        }.also { arr ->
            var x = 0
            for (i in 0 until 16) x = x xor (arr[i].toInt() and 0xFF)
            arr[16] = x.toByte()
        }

        sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, cfBytes)
        sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, be0)
        val afterBe1 = sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, be1)
        assertThat(afterBe1.phase).isEqualTo(WeighInPhase.HOLD_HANDLES_BIA)
        assertThat(afterBe1.completedReport).isNull()
        assertThat(sm.has8EImpedanceReadyAndAwaitingDf()).isTrue()

        // 1..3 0xDF frames keep session waiting for 4th frame
        assertThat(sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, dfPacket(1)).phase).isEqualTo(WeighInPhase.HOLD_HANDLES_BIA)
        assertThat(sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, dfPacket(2)).phase).isEqualTo(WeighInPhase.HOLD_HANDLES_BIA)
        assertThat(sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, dfPacket(3)).phase).isEqualTo(WeighInPhase.HOLD_HANDLES_BIA)

        // 4th 0xDF frame completes immediately
        val endSnap = sm.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, dfPacket(4))
        assertThat(endSnap.phase).isEqualTo(WeighInPhase.LOCKED_COMPLETE)
        assertThat(endSnap.completedReport?.metrics).hasSize(56)
        assertThat(endSnap.capturedFrames).hasSize(7)
        assertThat(endSnap.dfPacketCount).isEqualTo(4)
        assertThat(endSnap.capturedFrames[6].packetTypeHex).isEqualTo("0xDF")
        assertThat(endSnap.capturedFrames[6].checksumValid).isTrue()

        // Grace timeout path completes with fewer than 4 0xDF frames
        val smGrace = ScaleSessionStateMachine(defaultUser, ScaleTier.CULT_PRO_8E)
        smGrace.markConnecting("CULT_PRO_01", "AA:BB:CC:DD:EE:02").also { smGrace.markConnected() }
        smGrace.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, cfBytes)
        smGrace.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, be0)
        smGrace.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, be1)
        smGrace.onRawPacket(CultBleConstants.CHAR_NOTIFY_SHORT, dfPacket(1))
        val graceSnap = smGrace.complete8EOnGraceDeadline()
        assertThat(graceSnap.phase).isEqualTo(WeighInPhase.LOCKED_COMPLETE)
        assertThat(graceSnap.capturedFrames).hasSize(4)
    }
}
