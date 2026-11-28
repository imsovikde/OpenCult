package org.opencult.scale.core.ble

import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.DualFreqImpedance
import org.opencult.scale.core.bia.FullBodyCompositionReport
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import kotlin.math.abs

enum class WeighInPhase {
    IDLE,
    SCANNING,
    CONNECTING,
    STEP_ON,
    WEIGHING_SETTLING,
    HOLD_HANDLES_BIA,
    FEET_ONLY_LOCKED,
    LOCKED_COMPLETE,
    ERROR,
}

data class ScaleSessionSnapshot(
    val phase: WeighInPhase = WeighInPhase.IDLE,
    val detectedTier: ScaleTier = ScaleTier.CULT_PRO_8E,
    val deviceName: String = "",
    val deviceMac: String = "",
    val batteryPct: Int? = null,
    val liveWeightKg: Double? = null,
    val stableFrameCount: Int = 0,
    val heartRateBpm: Int? = null,
    val lowFreqPacket: ParsedScaleFrame.DualFreqImpedancePacket? = null,
    val highFreqPacket: ParsedScaleFrame.DualFreqImpedancePacket? = null,
    val dfPacketCount: Int = 0,
    val capturedFrames: List<RawCapturedFrame> = emptyList(),
    val completedReport: FullBodyCompositionReport? = null,
    val errorMessage: String? = null,
)

/**
 * Deterministic protocol session state machine collecting live BLE frames,
 * stabilizing `WeightLockFrames = 3` (`CS-BF01`) and `0xBE` Sets A/B + `4 x 0xDF`
 * (or 1200 ms grace deadline after Sets A/B arrive on `Cult Pro` for INV-01),
 * persisting all raw packets verbatim, and producing a verified [FullBodyCompositionReport].
 */
class ScaleSessionStateMachine(
    private val anthropometry: UserAnthropometry,
    private val requestedTier: ScaleTier,
) {
    private var snapshot = ScaleSessionSnapshot(
        phase = WeighInPhase.STEP_ON,
        detectedTier = requestedTier,
    )
    private var lastWeightKg: Double? = null
    private var sameWeightStreak = 0
    private var latestSingleFreqImp: Double? = null
    private var seqCounter = 0
    private var pendingRunstarPart1: ByteArray? = null

    fun current(): ScaleSessionSnapshot = snapshot

    fun markConnecting(name: String, mac: String): ScaleSessionSnapshot {
        snapshot = snapshot.copy(
            phase = WeighInPhase.CONNECTING,
            deviceName = name,
            deviceMac = mac,
            errorMessage = null,
        )
        return snapshot
    }

    fun markConnected(): ScaleSessionSnapshot {
        snapshot = snapshot.copy(phase = WeighInPhase.STEP_ON)
        return snapshot
    }

    fun onBatteryRead(batteryPct: Int): ScaleSessionSnapshot {
        snapshot = snapshot.copy(batteryPct = batteryPct.coerceIn(0, 100))
        return snapshot
    }

    fun onRawPacket(
        characteristicUuid: String,
        bytes: ByteArray,
        timestampMs: Long = System.currentTimeMillis(),
    ): ScaleSessionSnapshot {
        val parsed = ScaleFrameParser.parse(bytes, requestedTier, snapshot.deviceName, characteristicUuid)
        val impedList = when (parsed) {
            is ParsedScaleFrame.DualFreqImpedancePacket -> parsed.scaledOhmChannels
            is ParsedScaleFrame.WeightFrame -> parsed.singleFreq50KhzOhm?.let { listOf(it) } ?: emptyList()
            is ParsedScaleFrame.RunstarFinalFrame -> parsed.singleFreq50KhzOhm?.let { listOf(it) } ?: emptyList()
            else -> emptyList()
        }
        val captured = RawCapturedFrame(
            seqNo = seqCounter++,
            characteristicUuid = characteristicUuid,
            packetTypeHex = parsed.packetTypeHex,
            payloadHex = bytes.toHexLower(),
            checksumValid = parsed.checksumValid,
            decodedImpedancesOhm = impedList,
            capturedAtEpochMs = timestampMs,
        )
        val nextFrames = snapshot.capturedFrames + captured
        snapshot = snapshot.copy(capturedFrames = nextFrames)

        if (!parsed.checksumValid && parsed !is ParsedScaleFrame.WeightFrame) {
            return snapshot
        }

        when (parsed) {
            is ParsedScaleFrame.WeightFrame -> handleWeightFrame(parsed)
            is ParsedScaleFrame.DualFreqImpedancePacket -> handleDualFreqImpedance(parsed)
            is ParsedScaleFrame.ProprietaryEncryptedFrame -> {
                snapshot = snapshot.copy(dfPacketCount = snapshot.dfPacketCount + 1)
                maybeComplete8ESession(forceAfterGrace = false)
            }
            is ParsedScaleFrame.RunstarDeviceInfoFrame -> Unit
            is ParsedScaleFrame.RunstarFinalFrame -> handleRunstarFinalFrame(parsed)
            is ParsedScaleFrame.UnknownFrame -> Unit
        }
        return snapshot
    }

    fun has8EImpedanceReadyAndAwaitingDf(): Boolean =
        snapshot.completedReport == null &&
            snapshot.liveWeightKg != null &&
            snapshot.lowFreqPacket != null &&
            snapshot.highFreqPacket != null &&
            snapshot.dfPacketCount < CultBleConstants.EXPECTED_8E_DF_FRAMES

    fun complete8EOnGraceDeadline(): ScaleSessionSnapshot {
        maybeComplete8ESession(forceAfterGrace = true)
        return snapshot
    }

    fun confirmFeetOnlyAfterTimeout(): ScaleSessionSnapshot {
        val w = snapshot.liveWeightKg ?: return snapshot
        if (snapshot.completedReport != null) return snapshot
        if (snapshot.lowFreqPacket != null && snapshot.highFreqPacket != null) {
            maybeComplete8ESession(forceAfterGrace = true)
            return snapshot
        }
        finalizeFeetOnly(w, snapshot.heartRateBpm)
        return snapshot
    }

    private fun handleWeightFrame(frame: ParsedScaleFrame.WeightFrame) {
        if (!frame.checksumValid || frame.weightKg < 2.0) return

        if (frame.singleFreq50KhzOhm != null) {
            latestSingleFreqImp = frame.singleFreq50KhzOhm
        }

        val prevW = lastWeightKg
        sameWeightStreak = if (prevW != null && abs(prevW - frame.weightKg) <= 0.05) {
            sameWeightStreak + 1
        } else {
            1
        }
        lastWeightKg = frame.weightKg

        val updatedHr = frame.heartRateBpm ?: snapshot.heartRateBpm
        snapshot = snapshot.copy(
            liveWeightKg = frame.weightKg,
            stableFrameCount = sameWeightStreak,
            heartRateBpm = updatedHr,
        )

        if (requestedTier == ScaleTier.CULT_BF01_4E) {
            val readyByStreak = sameWeightStreak >= CultBleConstants.WEIGHT_LOCK_FRAMES
            val bodyFrameSettled = frame.isBf01Locked && !frame.isSettling
            val locked = readyByStreak || (bodyFrameSettled && (frame.singleFreq50KhzOhm != null || updatedHr != null))
            if (locked) {
                val report = BodyMetricsEngine.calculate(
                    anthropometry = anthropometry,
                    requestedTier = ScaleTier.CULT_BF01_4E,
                    weightKg = frame.weightKg,
                    heartRateBpm = updatedHr,
                    singleFreq50KhzOhm = latestSingleFreqImp,
                )
                snapshot = snapshot.copy(
                    phase = WeighInPhase.LOCKED_COMPLETE,
                    detectedTier = report.effectiveTier,
                    completedReport = report,
                )
            } else {
                snapshot = snapshot.copy(phase = WeighInPhase.WEIGHING_SETTLING)
            }
            return
        }

        when {
            frame.isSettling -> {
                snapshot = snapshot.copy(phase = WeighInPhase.WEIGHING_SETTLING)
            }
            frame.isHandleReading || frame.isHandleComplete -> {
                snapshot = snapshot.copy(phase = WeighInPhase.HOLD_HANDLES_BIA)
                maybeComplete8ESession(forceAfterGrace = false)
            }
            frame.isSettledWeightOnly -> {
                if (requestedTier == ScaleTier.CULT_PRO_FEET_ONLY) {
                    finalizeFeetOnly(frame.weightKg, updatedHr)
                } else {
                    snapshot = snapshot.copy(phase = WeighInPhase.FEET_ONLY_LOCKED)
                }
            }
        }
    }

    private fun handleDualFreqImpedance(pkt: ParsedScaleFrame.DualFreqImpedancePacket) {
        if (!pkt.checksumValid) return
        snapshot = when (pkt.packetIndex) {
            0x00 -> snapshot.copy(
                phase = WeighInPhase.HOLD_HANDLES_BIA,
                lowFreqPacket = pkt,
            )
            0x01 -> snapshot.copy(
                phase = WeighInPhase.HOLD_HANDLES_BIA,
                highFreqPacket = pkt,
            )
            else -> snapshot
        }
        maybeComplete8ESession(forceAfterGrace = false)
    }

    private fun finalizeFeetOnly(weightKg: Double, hr: Int?) {
        val report = BodyMetricsEngine.calculate(
            anthropometry = anthropometry,
            requestedTier = ScaleTier.CULT_PRO_FEET_ONLY,
            weightKg = weightKg,
            heartRateBpm = hr,
        )
        snapshot = snapshot.copy(
            phase = WeighInPhase.LOCKED_COMPLETE,
            detectedTier = ScaleTier.CULT_PRO_FEET_ONLY,
            completedReport = report,
        )
    }

    private fun maybeComplete8ESession(forceAfterGrace: Boolean) {
        if (snapshot.completedReport != null) return
        val w = snapshot.liveWeightKg ?: return
        val low = snapshot.lowFreqPacket
        val high = snapshot.highFreqPacket
        if (low != null && high != null && low.scaledOhmChannels.size == 5 && high.scaledOhmChannels.size == 5) {
            // INV-01: Wait for all 4 x 0xDF frames unless the 1200 ms grace deadline elapsed
            if (snapshot.dfPacketCount < CultBleConstants.EXPECTED_8E_DF_FRAMES && !forceAfterGrace) {
                return
            }
            val dualFreq = DualFreqImpedance(
                lowRightArmOhm = low.scaledOhmChannels[0],
                lowLeftArmOhm = low.scaledOhmChannels[1],
                lowTrunkOhm = low.scaledOhmChannels[2],
                lowRightLegOhm = low.scaledOhmChannels[3],
                lowLeftLegOhm = low.scaledOhmChannels[4],
                highRightArmOhm = high.scaledOhmChannels[0],
                highLeftArmOhm = high.scaledOhmChannels[1],
                highTrunkOhm = high.scaledOhmChannels[2],
                highRightLegOhm = high.scaledOhmChannels[3],
                highLeftLegOhm = high.scaledOhmChannels[4],
            )
            val report = BodyMetricsEngine.calculate(
                anthropometry = anthropometry,
                requestedTier = ScaleTier.CULT_PRO_8E,
                weightKg = w,
                heartRateBpm = snapshot.heartRateBpm,
                dualFreqImpedance = dualFreq,
            )
            snapshot = snapshot.copy(
                phase = WeighInPhase.LOCKED_COMPLETE,
                detectedTier = report.effectiveTier,
                completedReport = report,
            )
        } else if (forceAfterGrace && snapshot.phase == WeighInPhase.FEET_ONLY_LOCKED) {
            finalizeFeetOnly(w, snapshot.heartRateBpm)
        }
    }

    private fun handleRunstarFinalFrame(frame: ParsedScaleFrame.RunstarFinalFrame) {
        if (snapshot.completedReport != null) return
        if (frame.isPart1OfTwoPart) {
            pendingRunstarPart1 = frame.rawPartBytes
            snapshot = snapshot.copy(
                phase = WeighInPhase.HOLD_HANDLES_BIA,
                liveWeightKg = frame.weightKg.takeIf { it >= 2.0 } ?: snapshot.liveWeightKg,
                heartRateBpm = frame.heartRateBpm ?: snapshot.heartRateBpm,
            )
            return
        }
        if (frame.isPart2OfTwoPart) {
            val part1 = pendingRunstarPart1 ?: return
            pendingRunstarPart1 = null
            val gramsPart1 = (((part1[5].toInt() and 0xFF) shl 16) or
                ((part1[6].toInt() and 0xFF) shl 8) or
                (part1[7].toInt() and 0xFF)) and 0x07FFFF
            val weightKg = gramsPart1 / 1000.0
            if (weightKg < 2.0) return
            val hr = (part1[8].toInt() and 0xFF).takeIf { it in 35..220 } ?: snapshot.heartRateBpm

            if (!frame.checksumValid || frame.rawPartBytes.isEmpty() || frame.rawPartBytes[0] != part1[0]) {
                finalizeFeetOnly(weightKg, hr)
                return
            }
            val combined = part1.copyOfRange(9, 19) + frame.rawPartBytes.copyOfRange(3, 13)
            val imps = List(10) { idx ->
                val hi = combined[idx * 2].toInt() and 0xFF
                val lo = combined[idx * 2 + 1].toInt() and 0xFF
                ((hi shl 8) or lo) / 10.0
            }
            val limbsValid = imps.size == 10 &&
                imps[0] >= 1.0 &&
                imps[5] >= 1.0 &&
                listOf(1, 2, 3, 4, 6, 7, 8, 9).all { imps[it] >= 100.0 }
            if (limbsValid) {
                val dualFreq = DualFreqImpedance(
                    lowRightArmOhm = imps[1],
                    lowLeftArmOhm = imps[2],
                    lowTrunkOhm = imps[0],
                    lowRightLegOhm = imps[3],
                    lowLeftLegOhm = imps[4],
                    highRightArmOhm = imps[6],
                    highLeftArmOhm = imps[7],
                    highTrunkOhm = imps[5],
                    highRightLegOhm = imps[8],
                    highLeftLegOhm = imps[9],
                )
                val report = BodyMetricsEngine.calculate(
                    anthropometry = anthropometry,
                    requestedTier = ScaleTier.CULT_PRO_8E,
                    weightKg = weightKg,
                    heartRateBpm = hr,
                    dualFreqImpedance = dualFreq,
                )
                snapshot = snapshot.copy(
                    phase = WeighInPhase.LOCKED_COMPLETE,
                    liveWeightKg = weightKg,
                    heartRateBpm = hr,
                    detectedTier = report.effectiveTier,
                    completedReport = report,
                )
            } else {
                finalizeFeetOnly(weightKg, hr)
            }
            return
        }

        if (frame.weightKg < 2.0) return
        val hr = frame.heartRateBpm ?: snapshot.heartRateBpm
        val report = BodyMetricsEngine.calculate(
            anthropometry = anthropometry,
            requestedTier = requestedTier,
            weightKg = frame.weightKg,
            heartRateBpm = hr,
            singleFreq50KhzOhm = frame.singleFreq50KhzOhm,
        )
        snapshot = snapshot.copy(
            phase = WeighInPhase.LOCKED_COMPLETE,
            liveWeightKg = frame.weightKg,
            heartRateBpm = hr,
            detectedTier = report.effectiveTier,
            completedReport = report,
        )
    }
}
