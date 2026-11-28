/*
 * OpenCult BLE Frame Parser (GPL-3.0-or-later)
 * Protocol frame decoding adapted from openScale (oliexdev/openScale: OneByoneHandler,
 * BodyPod2Handler, CultSmartScaleProHandler) and verified against Occult reference notes.
 * Copyright (C) 2026 OpenCult Contributors (org.opencult.scale)
 */
package org.opencult.scale.core.ble

import kotlinx.serialization.Serializable
import org.opencult.scale.core.bia.ScaleTier
import java.util.Locale

@Serializable
data class RawCapturedFrame(
    val seqNo: Int,
    val characteristicUuid: String,
    val packetTypeHex: String,
    val payloadHex: String,
    val checksumValid: Boolean?,
    val decodedImpedancesOhm: List<Double> = emptyList(),
    val capturedAtEpochMs: Long,
)

sealed interface ParsedScaleFrame {
    val checksumValid: Boolean
    val packetTypeHex: String

    data class WeightFrame(
        val weightKg: Double,
        val statusByte: Int,
        val isSettling: Boolean,
        val isSettledWeightOnly: Boolean,
        val isHandleReading: Boolean,
        val isHandleComplete: Boolean,
        val isBf01Locked: Boolean,
        val heartRateBpm: Int?,
        val singleFreq50KhzOhm: Double?,
        val isHistory: Boolean,
        val historyIsoTimestamp: String?,
        val coalescedDuplicateDropped: Boolean,
        override val checksumValid: Boolean,
        override val packetTypeHex: String = "0xCF",
    ) : ParsedScaleFrame

    data class DualFreqImpedancePacket(
        val packetIndex: Int,
        val rawUint16Channels: List<Int>,
        val scaledOhmChannels: List<Double>,
        val scaleFactorUsed: Double,
        override val checksumValid: Boolean,
    ) : ParsedScaleFrame {
        override val packetTypeHex: String = "0xBE"
    }

    /**
     * Proprietary 0xDF encrypted payload (`17` bytes). Per INV-01 & INV-02, stored verbatim
     * without inventing field decodes when unsupported by captured vendor trace.
     */
    data class ProprietaryEncryptedFrame(
        val seqA: Int,
        val seqB: Int,
        val payloadHex: String,
        override val checksumValid: Boolean,
    ) : ParsedScaleFrame {
        override val packetTypeHex: String = "0xDF"
    }

    data class RunstarDeviceInfoFrame(
        val seq: Int,
        override val checksumValid: Boolean = true,
        override val packetTypeHex: String = "0xA1",
    ) : ParsedScaleFrame

    data class RunstarFinalFrame(
        val seq: Int,
        val isPart1OfTwoPart: Boolean,
        val isPart2OfTwoPart: Boolean,
        val status: Int,
        val weightKg: Double,
        val heartRateBpm: Int?,
        val singleFreq50KhzOhm: Double?,
        val rawPartBytes: ByteArray,
        override val checksumValid: Boolean,
        override val packetTypeHex: String = "0xA3",
    ) : ParsedScaleFrame {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RunstarFinalFrame) return false
            return seq == other.seq &&
                isPart1OfTwoPart == other.isPart1OfTwoPart &&
                isPart2OfTwoPart == other.isPart2OfTwoPart &&
                status == other.status &&
                weightKg == other.weightKg &&
                heartRateBpm == other.heartRateBpm &&
                singleFreq50KhzOhm == other.singleFreq50KhzOhm &&
                checksumValid == other.checksumValid &&
                rawPartBytes.contentEquals(other.rawPartBytes)
        }

        override fun hashCode(): Int {
            var res = seq
            res = 31 * res + isPart1OfTwoPart.hashCode()
            res = 31 * res + isPart2OfTwoPart.hashCode()
            res = 31 * res + rawPartBytes.contentHashCode()
            return res
        }
    }

    data class UnknownFrame(
        override val packetTypeHex: String,
        val payloadHex: String,
        override val checksumValid: Boolean = true,
    ) : ParsedScaleFrame
}

object ScaleFrameParser {

    fun parse(
        bytes: ByteArray,
        expectedTier: ScaleTier = ScaleTier.CULT_PRO_8E,
        deviceName: String = "",
        characteristicShortUuid: String = "",
    ): ParsedScaleFrame {
        if (bytes.isEmpty()) {
            return ParsedScaleFrame.UnknownFrame("0x00", "", checksumValid = false)
        }
        val shortUuid = characteristicShortUuid.lowercase(Locale.US)
        if (shortUuid == CultBleConstants.CHAR_RUNSTAR_LIVE_SHORT ||
            shortUuid == CultBleConstants.CHAR_RUNSTAR_RESULT_SHORT
        ) {
            return parseRunstarFfb0(bytes)
        }
        val opcode = bytes[0].toInt() and 0xFF
        return when (opcode) {
            0xCF -> parse0xCf(bytes, expectedTier, deviceName)
            0xBE -> parse0xBe(bytes)
            0xDF -> parse0xDf(bytes)
            else -> ParsedScaleFrame.UnknownFrame(
                packetTypeHex = String.format(Locale.US, "0x%02X", opcode),
                payloadHex = bytes.toHexLower(),
                checksumValid = true,
            )
        }
    }

    /**
     * Parses `0xFFB0` (`0xFFB2`/`0xFFB3`) 20-byte Runstar / Icomon scale frames
     * (adapted from openScale GPL-3.0 `RunstarR6Handler`).
     */
    fun parseRunstarFfb0(bytes: ByteArray): ParsedScaleFrame {
        if (bytes.size != 20) {
            val head = String.format(Locale.US, "0x%02X", bytes[0].toInt() and 0xFF)
            return ParsedScaleFrame.UnknownFrame(head, bytes.toHexLower(), checksumValid = false)
        }
        val partFlag = bytes[2].toInt() and 0xFF
        val checksumOk = (bytes[19].toInt() and 0x1F) == CultBleConstants.runstarChecksum(bytes)
        val seq = bytes[0].toInt() and 0xFF
        if (partFlag == 0x01) {
            return ParsedScaleFrame.RunstarFinalFrame(
                seq = seq,
                isPart1OfTwoPart = false,
                isPart2OfTwoPart = true,
                status = 0x00,
                weightKg = 0.0,
                heartRateBpm = null,
                singleFreq50KhzOhm = null,
                rawPartBytes = bytes.copyOf(),
                checksumValid = checksumOk,
                packetTypeHex = "0xA3",
            )
        }
        if (partFlag != 0x00 || !checksumOk) {
            val typeByte = bytes[3].toInt() and 0xFF
            return ParsedScaleFrame.UnknownFrame(
                packetTypeHex = String.format(Locale.US, "0x%02X", typeByte),
                payloadHex = bytes.toHexLower(),
                checksumValid = checksumOk,
            )
        }
        val frameType = bytes[3].toInt() and 0xFF
        return when (frameType) {
            0xA1 -> ParsedScaleFrame.RunstarDeviceInfoFrame(seq = seq, checksumValid = true)
            0xA2 -> {
                val state = bytes[4].toInt() and 0xFF
                val grams = u24be(bytes, 6) and 0x07FFFF
                ParsedScaleFrame.WeightFrame(
                    weightKg = grams / 1000.0,
                    statusByte = state,
                    isSettling = state == 0x01 || state == 0x02,
                    isSettledWeightOnly = state == 0x04,
                    isHandleReading = false,
                    isHandleComplete = false,
                    isBf01Locked = false,
                    heartRateBpm = null,
                    singleFreq50KhzOhm = null,
                    isHistory = false,
                    historyIsoTimestamp = null,
                    coalescedDuplicateDropped = false,
                    checksumValid = true,
                    packetTypeHex = "0xA2",
                )
            }
            0xA3 -> {
                val lenByte = bytes[1].toInt() and 0xFF
                val isTwoPart1 = lenByte > 0x10
                val status = bytes[4].toInt() and 0xFF
                val grams = u24be(bytes, 5) and 0x07FFFF
                val hr = (bytes[8].toInt() and 0xFF).takeIf { it in 35..220 }
                val imp = (((bytes[9].toInt() and 0xFF) shl 8) or (bytes[10].toInt() and 0xFF))
                val impOhm = if (!isTwoPart1 && imp in 200..1200) imp.toDouble() else null
                ParsedScaleFrame.RunstarFinalFrame(
                    seq = seq,
                    isPart1OfTwoPart = isTwoPart1,
                    isPart2OfTwoPart = false,
                    status = status,
                    weightKg = grams / 1000.0,
                    heartRateBpm = hr,
                    singleFreq50KhzOhm = impOhm,
                    rawPartBytes = bytes.copyOf(),
                    checksumValid = true,
                    packetTypeHex = "0xA3",
                )
            }
            else -> ParsedScaleFrame.UnknownFrame(
                packetTypeHex = String.format(Locale.US, "0x%02X", frameType),
                payloadHex = bytes.toHexLower(),
                checksumValid = true,
            )
        }
    }

    private fun u24be(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            (bytes[offset + 2].toInt() and 0xFF)

    /**
     * Parses `0xCF` weight/status/history frames across both `CS-BF01` (`bytes[1]` HR 35..220,
     * `bytes[5..6]` LE impedance 150..1200 ohm), legacy `OneByone` (`((b2<<8)+b1)*0.1`), and Pro 8E,
     * enforcing INV-21 (SHAME-005) coalesced 20-byte duplicate disambiguation.
     */
    fun parse0xCf(
        bytes: ByteArray,
        expectedTier: ScaleTier,
        deviceName: String = "",
    ): ParsedScaleFrame {
        if (bytes.size < 11) {
            return ParsedScaleFrame.UnknownFrame("0xCF", bytes.toHexLower(), checksumValid = false)
        }
        val liveXor = xorRange(bytes, 0, 10)
        val liveChecksumOk = liveXor == (bytes[10].toInt() and 0xFF)
        val isCoalescedDuplicate = bytes.size >= 18 &&
            liveChecksumOk &&
            (bytes[11].toInt() and 0xFF) == 0xCF

        val isHistory = bytes.size >= 18 && !isCoalescedDuplicate
        val statusByte = bytes[9].toInt() and 0xFF
        val weightRaw = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
        val weightKg = weightRaw / 100.0

        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val impField56 = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
        val oneByoneImpedanceRaw = ((b2 shl 8) or b1)
        val isCsBf01Explicit = deviceName.contains("CS-BF", ignoreCase = true) ||
            deviceName.contains("BF01", ignoreCase = true)
        val isSettling = statusByte == 0x01

        val singleFreqImp: Double? = when {
            isSettling -> null
            expectedTier == ScaleTier.CULT_BF01_4E -> when {
                impField56 in 150..1200 -> impField56.toDouble()
                impField56 in 1500..12000 && (isCsBf01Explicit || oneByoneImpedanceRaw !in 1500..12000) ->
                    impField56 / 10.0
                !isCsBf01Explicit && oneByoneImpedanceRaw in 1500..12000 ->
                    oneByoneImpedanceRaw / 10.0
                else -> null
            }
            expectedTier == ScaleTier.CULT_PRO_FEET_ONLY -> when {
                impField56 in 150..1200 -> impField56.toDouble()
                impField56 in 1500..12000 -> impField56 / 10.0
                else -> null
            }
            else -> null
        }

        val isProHandleFlagByte = expectedTier == ScaleTier.CULT_PRO_8E &&
            b1 == 0x80 &&
            (statusByte == 0x05 || statusByte == 0x08)
        val heartRate = when {
            isSettling || isProHandleFlagByte -> null
            expectedTier == ScaleTier.CULT_BF01_4E &&
                statusByte in setOf(0x00, 0x36) &&
                b1 in 35..220 &&
                (isCsBf01Explicit || impField56 in 150..1200 || impField56 == 0) -> b1
            expectedTier != ScaleTier.CULT_BF01_4E &&
                b1 in 35..220 &&
                statusByte in setOf(0x00, 0x05, 0x08) -> b1
            else -> null
        }

        val historyTimestamp = if (isHistory) {
            parseHistoryTimestamp(bytes)
        } else {
            null
        }

        val checksumValid = if (isHistory) {
            historyTimestamp != null || liveChecksumOk
        } else {
            liveChecksumOk
        }

        return ParsedScaleFrame.WeightFrame(
            weightKg = weightKg,
            statusByte = statusByte,
            isSettling = isSettling,
            isSettledWeightOnly = statusByte == 0x00,
            isHandleReading = statusByte == 0x08,
            isHandleComplete = statusByte == 0x05,
            isBf01Locked = statusByte == 0x36 || (expectedTier == ScaleTier.CULT_BF01_4E && statusByte == 0x00 && weightKg > 2.0),
            heartRateBpm = heartRate,
            singleFreq50KhzOhm = singleFreqImp,
            isHistory = isHistory,
            historyIsoTimestamp = historyTimestamp,
            coalescedDuplicateDropped = isCoalescedDuplicate,
            checksumValid = checksumValid,
        )
    }

    /**
     * Parses `0xBE` 13-byte dual-frequency BIA packets (Set A `0x00` low freq, Set B `0x01` high freq)
     * retaining all 5 channels per packet in deci-ohms (`* 0.1`), fixing openScale SHAME-004.
     */
    fun parse0xBe(bytes: ByteArray): ParsedScaleFrame {
        if (bytes.size < 13) {
            return ParsedScaleFrame.UnknownFrame("0xBE", bytes.toHexLower(), checksumValid = false)
        }
        val expectedXor = xorRange(bytes, 0, 12)
        val actualXor = bytes[12].toInt() and 0xFF
        val packetIndex = bytes[1].toInt() and 0xFF
        val rawChannels = List(5) { idx ->
            val lo = bytes[2 + idx * 2].toInt() and 0xFF
            val hi = bytes[3 + idx * 2].toInt() and 0xFF
            (hi shl 8) or lo
        }
        val factor = 0.1
        val scaled = rawChannels.map { it / 10.0 }
        return ParsedScaleFrame.DualFreqImpedancePacket(
            packetIndex = packetIndex,
            rawUint16Channels = rawChannels,
            scaledOhmChannels = scaled,
            scaleFactorUsed = factor,
            checksumValid = expectedXor == actualXor,
        )
    }

    fun parse0xDf(bytes: ByteArray): ParsedScaleFrame {
        if (bytes.size < 5) {
            return ParsedScaleFrame.UnknownFrame("0xDF", bytes.toHexLower(), checksumValid = false)
        }
        val expectedXor = xorRange(bytes, 0, bytes.lastIndex)
        val actualXor = bytes.last().toInt() and 0xFF
        return ParsedScaleFrame.ProprietaryEncryptedFrame(
            seqA = bytes[2].toInt() and 0xFF,
            seqB = bytes[3].toInt() and 0xFF,
            payloadHex = bytes.toHexLower(),
            checksumValid = expectedXor == actualXor,
        )
    }

    private fun parseHistoryTimestamp(bytes: ByteArray): String? {
        if (bytes.size < 18) return null
        val year = ((bytes[11].toInt() and 0xFF) shl 8) or (bytes[12].toInt() and 0xFF)
        val month = bytes[13].toInt() and 0xFF
        val day = bytes[14].toInt() and 0xFF
        val hour = bytes[15].toInt() and 0xFF
        val minute = bytes[16].toInt() and 0xFF
        val second = bytes[17].toInt() and 0xFF
        if (year !in 2020..2099 || month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..59) {
            return null
        }
        return String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second)
    }

    private fun xorRange(bytes: ByteArray, start: Int, endExclusive: Int): Int {
        var acc = 0
        for (i in start until endExclusive) {
            acc = acc xor (bytes[i].toInt() and 0xFF)
        }
        return acc and 0xFF
    }
}

fun ByteArray.toHexLower(): String = joinToString(separator = "") { b ->
    String.format(Locale.US, "%02x", b.toInt() and 0xFF)
}

fun String.hexToByteArray(): ByteArray {
    val clean = replace(" ", "").replace("\n", "").trim()
    require(clean.length % 2 == 0) { "Odd hex string length: ${clean.length}" }
    return ByteArray(clean.length / 2) { idx ->
        clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
    }
}
