/*
 * OpenCult Body Composition Engine (GPL-3.0-or-later)
 * Adapted and extended from openScale (oliexdev/openScale):
 * StandardImpedanceLib.kt, S400BodyComposition.kt, and Wla25BodyComposition.kt.
 * Copyright (C) 2026 OpenCult Contributors (org.opencult.scale)
 * Upstream openScale copyright (C) 2014-2026 oliexdev & openScale contributors.
 */
package org.opencult.scale.core.bia

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure JVM body composition calculator emitting all 56 PRD:M-01..M-56 parameters
 * aligned 1:1 with `PRD.md`, explicit [MetricProvenance], and [INV-03] plausibility gating.
 */
object BodyMetricsEngine {

    private const val WATER_DENSITY_36C = 0.99513
    private const val BAND_LOW_PRO_KHZ = 20.0
    private const val BAND_HIGH_PRO_KHZ = 100.0
    private const val COLE_FC_KHZ_M = 40.0
    private const val COLE_FC_KHZ_F = 48.0
    private const val COLE_ALPHA_M = 0.676
    private const val COLE_ALPHA_F = 0.664
    private const val K_ECW_M = 0.306
    private const val K_ECW_F = 0.316
    private const val R0_RINF_MAX_BRACKET = 5.0

    private data class ColeFit(val r0: Double, val rInf: Double)

    private fun coleMagnitude(r: Double, fKHz: Double, fcKHz: Double, alpha: Double): Double {
        val u = (fKHz / fcKHz).pow(alpha)
        val quarterTurn = alpha * PI / 2.0
        val denomRe = 1.0 + u * cos(quarterTurn)
        val denomIm = u * sin(quarterTurn)
        val denomSq = denomRe * denomRe + denomIm * denomIm
        val spread = r - 1.0
        return hypot(1.0 + spread * denomRe / denomSq, -spread * denomIm / denomSq)
    }

    private fun invertColeTwoBand(zLow: Double, zHigh: Double, male: Boolean): ColeFit? {
        if (zHigh <= 0.0 || zLow <= zHigh) return null
        val measured = zLow / zHigh
        val fc = if (male) COLE_FC_KHZ_M else COLE_FC_KHZ_F
        val alpha = if (male) COLE_ALPHA_M else COLE_ALPHA_F
        fun bandRatio(r: Double) =
            coleMagnitude(r, BAND_LOW_PRO_KHZ, fc, alpha) / coleMagnitude(r, BAND_HIGH_PRO_KHZ, fc, alpha)

        var lo = 1.0 + 1e-9
        var hi = R0_RINF_MAX_BRACKET
        if (bandRatio(hi) < measured) return null
        repeat(60) {
            val mid = (lo + hi) * 0.5
            if (bandRatio(mid) < measured) lo = mid else hi = mid
        }
        val ratio = (lo + hi) * 0.5
        val rInf = zLow / coleMagnitude(ratio, BAND_LOW_PRO_KHZ, fc, alpha)
        return ColeFit(r0 = ratio * rInf, rInf = rInf)
    }

    fun whoBmiCategory(bmi: Double): Pair<Double, String> = when {
        bmi < 18.5 -> 1.0 to "Underweight"
        bmi < 25.0 -> 2.0 to "Normal"
        bmi < 30.0 -> 3.0 to "Overweight"
        bmi < 35.0 -> 4.0 to "Obese I"
        bmi < 40.0 -> 5.0 to "Obese II"
        else -> 6.0 to "Obese III"
    }

    fun whoBmiLabelFromCode(code: Double?): String = when (code?.roundToInt()) {
        1 -> "Underweight"
        2 -> "Normal"
        3 -> "Overweight"
        4 -> "Obese I"
        5 -> "Obese II"
        6 -> "Obese III"
        else -> "--"
    }

    fun validatePlausibility(
        requestedTier: ScaleTier,
        weightKg: Double,
        heightCm: Double,
        dualFreqImpedance: DualFreqImpedance?,
        singleFreq50KhzOhm: Double?,
    ): Pair<ScaleTier, BiaPlausibilityResult> {
        require(weightKg in 2.0..400.0) { "weightKg out of physiological bounds: $weightKg" }
        require(heightCm in 50.0..260.0) { "heightCm out of bounds: $heightCm" }

        if (requestedTier == ScaleTier.CULT_PRO_8E) {
            if (dualFreqImpedance == null) {
                return ScaleTier.CULT_PRO_FEET_ONLY to BiaPlausibilityResult(
                    passed = false,
                    downgradedToFeetOnly = true,
                    reason = "8E handle impedance missing; downgraded to Feet-Only",
                )
            }
            val limbs = listOf(
                dualFreqImpedance.lowRightArmOhm,
                dualFreqImpedance.lowLeftArmOhm,
                dualFreqImpedance.lowRightLegOhm,
                dualFreqImpedance.lowLeftLegOhm,
                dualFreqImpedance.highRightArmOhm,
                dualFreqImpedance.highLeftArmOhm,
                dualFreqImpedance.highRightLegOhm,
                dualFreqImpedance.highLeftLegOhm,
            )
            val trunks = listOf(dualFreqImpedance.lowTrunkOhm, dualFreqImpedance.highTrunkOhm)
            val wb = dualFreqImpedance.wholeBody50KhzEquivalentOhm
            val limbsValid = limbs.all { it in 50.0..700.0 }
            val trunkValid = trunks.all { it in 5.0..120.0 }
            val wbValid = wb in 150.0..1200.0
            if (!limbsValid || !trunkValid || !wbValid) {
                return ScaleTier.CULT_PRO_FEET_ONLY to BiaPlausibilityResult(
                    passed = false,
                    downgradedToFeetOnly = true,
                    reason = "Impedance out of 8E handle bounds (wb=${wb.round1()} ohm); downgraded to Feet-Only",
                )
            }
            return ScaleTier.CULT_PRO_8E to BiaPlausibilityResult(passed = true, downgradedToFeetOnly = false)
        }

        if (requestedTier == ScaleTier.CULT_BF01_4E && singleFreq50KhzOhm != null) {
            if (singleFreq50KhzOhm !in 150.0..1200.0) {
                return ScaleTier.CULT_PRO_FEET_ONLY to BiaPlausibilityResult(
                    passed = false,
                    downgradedToFeetOnly = true,
                    reason = "4E impedance $singleFreq50KhzOhm ohm outside 150..1200 ohm",
                )
            }
            return ScaleTier.CULT_BF01_4E to BiaPlausibilityResult(passed = true, downgradedToFeetOnly = false)
        }

        return requestedTier to BiaPlausibilityResult(passed = true, downgradedToFeetOnly = false)
    }

    fun calculate(
        anthropometry: UserAnthropometry,
        requestedTier: ScaleTier,
        weightKg: Double,
        heartRateBpm: Int? = null,
        dualFreqImpedance: DualFreqImpedance? = null,
        singleFreq50KhzOhm: Double? = null,
    ): FullBodyCompositionReport {
        val (tier, plausibility) = validatePlausibility(
            requestedTier = requestedTier,
            weightKg = weightKg,
            heightCm = anthropometry.heightCm,
            dualFreqImpedance = dualFreqImpedance,
            singleFreq50KhzOhm = singleFreq50KhzOhm,
        )

        val heightM = anthropometry.heightCm / 100.0
        val sexCode = if (anthropometry.sex == BiologicalSex.MALE) 1.0 else 0.0
        val bmi = weightKg / (heightM * heightM)
        val (bmiCategoryCode, bmiCategoryLabel) = whoBmiCategory(bmi)

        // Devine (1974) Ideal Body Weight (M-04)
        val heightInches = anthropometry.heightCm / 2.54
        val devineBase = if (anthropometry.sex == BiologicalSex.MALE) 50.0 else 45.5
        val idealWeightKg = (devineBase + 2.3 * (heightInches - 60.0)).coerceIn(35.0, 140.0)
        val weightDeltaToTarget = weightKg - (anthropometry.targetWeightKg ?: idealWeightKg)
        val obesityDegreePct = ((weightKg - idealWeightKg) / idealWeightKg) * 100.0

        val validHr = heartRateBpm?.takeIf { it in 25..240 }
        val cardiacIndexScore = validHr?.let { hr ->
            val idealHr = if (anthropometry.athleteMode) 56.0 else 66.0
            (100.0 - abs(hr - idealHr) * 1.15).roundToInt().coerceIn(30, 99)
        }

        val z50 = when (tier) {
            ScaleTier.CULT_PRO_8E -> dualFreqImpedance!!.wholeBody50KhzEquivalentOhm
            ScaleTier.CULT_BF01_4E -> singleFreq50KhzOhm?.takeIf { it in 150.0..1200.0 }
            else -> null
        }
        val zHigh = if (tier == ScaleTier.CULT_PRO_8E && dualFreqImpedance != null) {
            dualFreqImpedance.wholeBodyHighOhm
        } else {
            null
        }

        val coleFit: ColeFit?
        val coleR0: Double?
        val coleRinf: Double?
        if (tier == ScaleTier.CULT_PRO_8E && dualFreqImpedance != null) {
            val zLow = dualFreqImpedance.wholeBodyLowOhm
            val zHi = dualFreqImpedance.wholeBodyHighOhm
            coleFit = invertColeTwoBand(zLow, zHi, male = anthropometry.sex == BiologicalSex.MALE)
            coleR0 = coleFit?.r0 ?: (zLow + (zLow - zHi) * 0.42).coerceIn(zLow, 1400.0)
            coleRinf = coleFit?.rInf ?: (zHi - (zLow - zHi) * 0.36).coerceIn(120.0, zHi)
        } else {
            coleFit = null
            coleR0 = null
            coleRinf = null
        }

        val hasBia = z50 != null && (tier == ScaleTier.CULT_PRO_8E || tier == ScaleTier.CULT_BF01_4E)
        val compositionProvenance = when {
            tier == ScaleTier.CULT_PRO_8E -> MetricProvenance.DERIVED_8E_BIA
            hasBia && tier == ScaleTier.CULT_BF01_4E -> MetricProvenance.DERIVED_4E_BIA
            else -> MetricProvenance.ANTHROPOMETRIC_ESTIMATE
        }

        val rawFfm = if (hasBia) {
            val h2OverZ = (anthropometry.heightCm * anthropometry.heightCm) / z50
            val baseFfm = if (anthropometry.sex == BiologicalSex.MALE) {
                -10.68 + 0.65 * h2OverZ + 0.26 * weightKg + 0.02 * z50
            } else {
                -9.53 + 0.69 * h2OverZ + 0.17 * weightKg + 0.02 * z50
            }
            val athleteOffset = if (anthropometry.athleteMode) 1.8 else 0.0
            (baseFfm + athleteOffset).coerceIn(weightKg * 0.42, weightKg * 0.94)
        } else {
            val deurenbergFatPct = (1.20 * bmi + 0.23 * anthropometry.ageYears - 10.8 * sexCode - 5.4)
                .coerceIn(5.0, 58.0)
            weightKg * (1.0 - deurenbergFatPct / 100.0)
        }

        val fatMassKg = (weightKg - rawFfm).coerceIn(weightKg * 0.05, weightKg * 0.58)
        val fatFreeMassKg = (weightKg - fatMassKg).coerceAtLeast(1.0)
        val bodyFatPct = ((fatMassKg / weightKg) * 100.0).coerceIn(5.0, 58.0)
        val fmi = fatMassKg / (heightM * heightM)
        val ffmi = fatFreeMassKg / (heightM * heightM)

        val visceralFatIndex = ((bodyFatPct * 0.32) + (bmi * 0.18) + (if (anthropometry.sex == BiologicalSex.MALE) 1.2 else 0.4) - 4.5)
            .coerceIn(1.0, 30.0)
        val visceralFatMassKg = (fatMassKg * (0.06 + visceralFatIndex * 0.008).coerceIn(0.05, 0.28))
        val subcutaneousFatPct = (bodyFatPct * 0.86).coerceIn(3.5, bodyFatPct)
        val subcutaneousFatMassKg = (fatMassKg - visceralFatMassKg).coerceAtLeast(0.1)

        // Hydration & Cole dual-frequency ECW/ICW model (8E-exclusive for ECW/ICW/BCM)
        val tbwKg = if (hasBia) {
            (fatFreeMassKg * 0.732).coerceIn(weightKg * 0.28, weightKg * 0.72)
        } else {
            val watson = if (anthropometry.sex == BiologicalSex.MALE) {
                2.447 - 0.09156 * anthropometry.ageYears + 0.1074 * anthropometry.heightCm + 0.3362 * weightKg
            } else {
                -2.097 + 0.1069 * anthropometry.heightCm + 0.2466 * weightKg
            }
            watson.coerceIn(weightKg * 0.28, weightKg * 0.72)
        }
        val tbwLiters = tbwKg / WATER_DENSITY_36C
        val tbwPct = (tbwKg / weightKg) * 100.0

        val ecwTbwRatio: Double?
        val ecwKg: Double?
        val icwKg: Double?
        val ecwLiters: Double?
        val ecwPct: Double?
        val icwLiters: Double?
        val icwPct: Double?
        val bcmKg: Double?
        if (tier == ScaleTier.CULT_PRO_8E && dualFreqImpedance != null && coleR0 != null && coleR0 > 0.0) {
            // De Lorenzo (1997) Eq. B2 on two-band inverted Cole R0 with clinical ECW/TBW bounds
            val kEcw = if (anthropometry.sex == BiologicalSex.MALE) K_ECW_M else K_ECW_F
            val ecwDeLorenzo = kEcw * ((anthropometry.heightCm * anthropometry.heightCm * sqrt(weightKg)) / coleR0).pow(2.0 / 3.0)
            val ratio = (ecwDeLorenzo / tbwKg.coerceAtLeast(1.0)).coerceIn(0.34, 0.45)
            ecwTbwRatio = ratio
            ecwKg = tbwKg * ratio
            icwKg = tbwKg - ecwKg
            ecwLiters = ecwKg / WATER_DENSITY_36C
            icwLiters = icwKg / WATER_DENSITY_36C
            ecwPct = (ecwKg / weightKg) * 100.0
            icwPct = (icwKg / weightKg) * 100.0
            bcmKg = (icwKg / 0.70).coerceAtMost(fatFreeMassKg * 0.72)
        } else {
            ecwTbwRatio = null
            ecwKg = null
            icwKg = null
            ecwLiters = null
            ecwPct = null
            icwLiters = null
            icwPct = null
            bcmKg = null
        }

        // Janssen SMM (2000) when BIA impedance exists, otherwise Lean Soft Tissue fraction
        val skeletalMuscleMassKg = if (hasBia) {
            val janssen = ((anthropometry.heightCm * anthropometry.heightCm) / z50 * 0.401) +
                (sexCode * 3.825) + (anthropometry.ageYears * -0.071) + 5.102
            janssen.coerceIn(weightKg * 0.18, fatFreeMassKg * 0.68)
        } else {
            (fatFreeMassKg * if (anthropometry.sex == BiologicalSex.MALE) 0.56 else 0.50)
                .coerceIn(weightKg * 0.18, fatFreeMassKg * 0.68)
        }
        val smmPct = (skeletalMuscleMassKg / weightKg) * 100.0
        val boneMassKg = (fatFreeMassKg * (if (anthropometry.sex == BiologicalSex.MALE) 0.053 else 0.049)).coerceIn(1.0, 5.8)
        val boneMineralPct = (boneMassKg / weightKg) * 100.0
        val mineralMassKg = (boneMassKg * 1.18).coerceIn(1.2, 6.8)
        val muscleMassKg = (fatFreeMassKg - boneMassKg).coerceAtLeast(skeletalMuscleMassKg)
        val totalMuscleRatePct = (muscleMassKg / weightKg) * 100.0
        val smi = skeletalMuscleMassKg / (heightM * heightM)
        // 4-compartment mass conservation: Weight = Fat + TBW + Protein + Mineral (ash)
        val proteinMassKg = (fatFreeMassKg - tbwKg - mineralMassKg).coerceAtLeast(weightKg * 0.08)
        val proteinPct = (proteinMassKg / weightKg) * 100.0
        val muscleToFatRatio = skeletalMuscleMassKg / fatMassKg.coerceAtLeast(0.5)

        val bmr = if (hasBia) {
            (370.0 + 21.6 * fatFreeMassKg).roundToInt()
        } else {
            (10.0 * weightKg + 6.25 * anthropometry.heightCm - 5.0 * anthropometry.ageYears + if (sexCode > 0.5) 5.0 else -161.0)
                .roundToInt()
                .coerceAtLeast(800)
        }
        val tdeeSedentary = (bmr * 1.2).roundToInt()
        val stepBonus = anthropometry.dailyStepsAvg?.let { ((it - 5000).coerceIn(0, 12000) * 0.04).roundToInt() } ?: 0
        val tdee = (bmr * anthropometry.activityLevel.multiplier).roundToInt() + stepBonus

        val idealBf = if (anthropometry.sex == BiologicalSex.MALE) 15.0 else 23.0
        val metabolicAge = (anthropometry.ageYears + ((bodyFatPct - idealBf) * 0.45).roundToInt())
            .coerceIn(18, 90)

        val score = (
            100.0 -
                abs(bodyFatPct - idealBf) * 1.4 -
                abs(bmi - 22.0) * 1.6 -
                (visceralFatIndex - 8.0).coerceAtLeast(0.0) * 1.5
            ).roundToInt().coerceIn(25, 99)

        // 8E Segmental 5-part mass conservation: sum of 5 limbs + trunk equals SMM & Fat Mass
        val segMuscle: SegmentalMass?
        val segFat: SegmentalMass?
        val segMusclePctStd: Map<String, Double>
        val segFatPctOfMass: Map<String, Double>
        val bilateralSym: Double?
        val upperSym: Double?
        val lowerSym: Double?
        val almKg: Double?
        val asmi: Double?

        val refArmM = weightKg * 0.036
        val refTrunkM = weightKg * 0.20
        val refLegM = weightKg * 0.088

        if (tier == ScaleTier.CULT_PRO_8E && dualFreqImpedance != null) {
            val hCm = anthropometry.heightCm
            val cRa = ((0.38 * hCm).pow(2)) / dualFreqImpedance.highRightArmOhm
            val cLa = ((0.38 * hCm).pow(2)) / dualFreqImpedance.highLeftArmOhm
            val cTr = ((0.34 * hCm).pow(2)) / dualFreqImpedance.highTrunkOhm
            val cRl = ((0.52 * hCm).pow(2)) / dualFreqImpedance.highRightLegOhm
            val cLl = ((0.52 * hCm).pow(2)) / dualFreqImpedance.highLeftLegOhm

            val wRa = 0.08 * cRa / ((cRa + cLa) / 2.0)
            val wLa = 0.08 * cLa / ((cRa + cLa) / 2.0)
            val wTr = 0.45 * (cTr / ((cRa + cLa + cTr + cRl + cLl) / 5.0)).coerceIn(0.85, 1.15)
            val wRl = 0.195 * cRl / ((cRl + cLl) / 2.0)
            val wLl = 0.195 * cLl / ((cRl + cLl) / 2.0)
            val sumW = wRa + wLa + wTr + wRl + wLl

            val mLa = skeletalMuscleMassKg * (wLa / sumW)
            val mRa = skeletalMuscleMassKg * (wRa / sumW)
            val mTr = skeletalMuscleMassKg * (wTr / sumW)
            val mLl = skeletalMuscleMassKg * (wLl / sumW)
            val mRl = skeletalMuscleMassKg - mLa - mRa - mTr - mLl
            segMuscle = SegmentalMass(leftArmKg = mLa, rightArmKg = mRa, trunkKg = mTr, leftLegKg = mLl, rightLegKg = mRl)

            val fLaRaw = 0.075 * (2.0 - (cLa / ((cRa + cLa) / 2.0))).coerceIn(0.5, 1.5)
            val fRaRaw = 0.075 * (2.0 - (cRa / ((cRa + cLa) / 2.0))).coerceIn(0.5, 1.5)
            val fTrRaw = 0.53
            val fLlRaw = 0.16 * (2.0 - (cLl / ((cRl + cLl) / 2.0))).coerceIn(0.5, 1.5)
            val fRlRaw = 0.16 * (2.0 - (cRl / ((cRl + cLl) / 2.0))).coerceIn(0.5, 1.5)
            val sumF = fLaRaw + fRaRaw + fTrRaw + fLlRaw + fRlRaw

            val fLa = fatMassKg * (fLaRaw / sumF)
            val fRa = fatMassKg * (fRaRaw / sumF)
            val fTr = fatMassKg * (fTrRaw / sumF)
            val fLl = fatMassKg * (fLlRaw / sumF)
            val fRl = fatMassKg - fLa - fRa - fTr - fLl
            segFat = SegmentalMass(leftArmKg = fLa, rightArmKg = fRa, trunkKg = fTr, leftLegKg = fLl, rightLegKg = fRl)

            segMusclePctStd = mapOf(
                "left_arm" to (mLa / refArmM * 100.0),
                "right_arm" to (mRa / refArmM * 100.0),
                "trunk" to (mTr / refTrunkM * 100.0),
                "left_leg" to (mLl / refLegM * 100.0),
                "right_leg" to (mRl / refLegM * 100.0),
            )
            segFatPctOfMass = mapOf(
                "left_arm" to (fLa / (weightKg * 0.055) * 100.0),
                "right_arm" to (fRa / (weightKg * 0.055) * 100.0),
                "trunk" to (fTr / (weightKg * 0.45) * 100.0),
                "left_leg" to (fLl / (weightKg * 0.18) * 100.0),
                "right_leg" to (fRl / (weightKg * 0.18) * 100.0),
            )

            upperSym = (minOf(mLa, mRa) / maxOf(mLa, mRa).coerceAtLeast(0.01)) * 100.0
            lowerSym = (minOf(mLl, mRl) / maxOf(mLl, mRl).coerceAtLeast(0.01)) * 100.0
            val armDiffPct = abs(mLa - mRa) / ((mLa + mRa) / 2.0).coerceAtLeast(0.01) * 100.0
            val legDiffPct = abs(mLl - mRl) / ((mLl + mRl) / 2.0).coerceAtLeast(0.01) * 100.0
            bilateralSym = ((armDiffPct + legDiffPct) / 2.0)
            almKg = (mLa + mRa + mLl + mRl) * 1.18
            asmi = almKg / (heightM * heightM)
        } else {
            segMuscle = null
            segFat = null
            segMusclePctStd = emptyMap()
            segFatPctOfMass = emptyMap()
            bilateralSym = null
            upperSym = null
            lowerSym = null
            almKg = null
            asmi = null
        }

        fun item(
            prdCode: String,
            id: String,
            label: String,
            value: Double?,
            unit: String,
            category: MetricCategory,
            provenance: MetricProvenance,
            statusLabel: String = if (value == null) "Unavailable" else "Recorded",
            decimals: Int = 1,
            formattedOverride: String? = null,
        ): BodyMetricItem {
            val fmt = when {
                value == null -> "--"
                formattedOverride != null -> formattedOverride
                else -> formatNumber(value, decimals)
            }
            return BodyMetricItem(
                id = id,
                prdCode = prdCode,
                label = label,
                value = value,
                formattedValue = fmt,
                unit = unit,
                category = category,
                provenance = provenance,
                statusLabel = statusLabel,
                decimals = decimals,
            )
        }

        val metricList = listOf(
            // PRD:M-01..M-05 Core & Weight
            item("M-01", "weight_kg", "Weight", weightKg, "kg", MetricCategory.CORE_WEIGHT, MetricProvenance.DIRECT_HARDWARE, "Measured", 2),
            item("M-02", "bmi", "BMI", bmi, "kg/m2", MetricCategory.CORE_WEIGHT, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, if (bmi in 18.5..24.9) "Optimal" else "Monitor", 1),
            item("M-03", "who_bmi_category", "WHO BMI Category", bmiCategoryCode, "", MetricCategory.CORE_WEIGHT, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, bmiCategoryLabel, 0, bmiCategoryLabel),
            item("M-04", "ideal_weight_kg", "Ideal Body Weight (Devine)", idealWeightKg, "kg", MetricCategory.CORE_WEIGHT, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, "Devine 1974", 1),
            item(
                "M-05",
                "weight_delta_to_target_kg",
                "Weight Delta to Target",
                weightDeltaToTarget,
                "kg",
                MetricCategory.CORE_WEIGHT,
                MetricProvenance.ANTHROPOMETRIC_ESTIMATE,
                if (abs(weightDeltaToTarget) <= 1.5) "On Target" else "Offset",
                1,
                String.format(Locale.US, "%+.1f", weightDeltaToTarget),
            ),

            // PRD:M-06..M-07 Vitals
            item("M-06", "heart_rate_bpm", "Heart Rate", validHr?.toDouble(), "bpm", MetricCategory.CORE_WEIGHT, if (validHr != null) MetricProvenance.DIRECT_HARDWARE else MetricProvenance.UNAVAILABLE_ON_TIER, if (validHr != null) "Scale Sensor" else "No HR Frame", 0),
            item("M-07", "cardiac_index_score", "Cardiac Index Score", cardiacIndexScore?.toDouble(), "score", MetricCategory.CORE_WEIGHT, if (cardiacIndexScore != null) MetricProvenance.DERIVED_4E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (cardiacIndexScore != null) "HR Calibration" else "No HR Frame", 0),

            // PRD:M-08..M-11 Raw & Cole Impedance
            item("M-08", "impedance_50khz_ohm", "Whole-Body Impedance (50 kHz)", z50, "ohm", MetricCategory.FLUID_BALANCE, if (z50 != null) MetricProvenance.DIRECT_HARDWARE else MetricProvenance.UNAVAILABLE_ON_TIER, if (z50 != null) "BIA Carrier" else "Feet / Unmeasured", 1),
            item("M-09", "impedance_high_ohm", "Whole-Body Impedance (High Freq)", zHigh, "ohm", MetricCategory.FLUID_BALANCE, if (zHigh != null) MetricProvenance.DIRECT_HARDWARE else MetricProvenance.UNAVAILABLE_ON_TIER, if (zHigh != null) "8E Set B" else "Requires 8E", 1),
            item("M-10", "cole_r0_ohm", "Cole Extracellular R0", coleR0, "ohm", MetricCategory.FLUID_BALANCE, if (coleR0 != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (coleR0 != null) "Cole Two-Band" else "Requires 8E", 1),
            item("M-11", "cole_rinf_ohm", "Cole Intracellular Rinf", coleRinf, "ohm", MetricCategory.FLUID_BALANCE, if (coleRinf != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (coleRinf != null) "Cole Two-Band" else "Requires 8E", 1),

            // PRD:M-12..M-20 Adiposity & Obesity
            item("M-12", "body_fat_pct", "Body Fat %", bodyFatPct, "%", MetricCategory.ADIPOSITY, compositionProvenance, if (bodyFatPct <= idealBf + 4) "Optimal" else "Elevated", 1),
            item("M-13", "fat_mass_kg", "Fat Mass", fatMassKg, "kg", MetricCategory.ADIPOSITY, compositionProvenance, "Recorded", 2),
            item("M-14", "fmi_kg_m2", "Fat Mass Index (FMI)", fmi, "kg/m2", MetricCategory.ADIPOSITY, compositionProvenance, "Fat / H^2", 1),
            item("M-15", "fat_free_mass_kg", "Fat-Free Mass (FFM / LBM)", fatFreeMassKg, "kg", MetricCategory.ADIPOSITY, compositionProvenance, "Lean Total", 2),
            item("M-16", "ffmi_kg_m2", "Fat-Free Mass Index (FFMI)", ffmi, "kg/m2", MetricCategory.ADIPOSITY, compositionProvenance, "FFM / H^2", 1),
            item("M-17", "subcutaneous_fat_pct", "Subcutaneous Fat %", subcutaneousFatPct, "%", MetricCategory.ADIPOSITY, compositionProvenance, "Estimated", 1),
            item("M-18", "subcutaneous_fat_mass_kg", "Subcutaneous Fat Mass", subcutaneousFatMassKg, "kg", MetricCategory.ADIPOSITY, compositionProvenance, "Recorded", 2),
            item("M-19", "visceral_fat_index", "Visceral Fat Level", visceralFatIndex, "lvl", MetricCategory.ADIPOSITY, compositionProvenance, if (visceralFatIndex <= 10.0) "Optimal" else "Elevated", 0),
            item("M-20", "obesity_degree_pct", "Obesity Degree", obesityDegreePct, "%", MetricCategory.ADIPOSITY, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, if (obesityDegreePct in -10.0..10.0) "Balanced" else "Delta vs Ideal", 1),

            // PRD:M-21..M-28 Fluid Compartments
            item("M-21", "tbw_pct", "Total Body Water %", tbwPct, "%", MetricCategory.FLUID_BALANCE, compositionProvenance, if (tbwPct in 48.0..66.0) "Hydrated" else "Monitor", 1),
            item("M-22", "tbw_kg", "Total Body Water Mass", tbwKg, "kg", MetricCategory.FLUID_BALANCE, compositionProvenance, "Recorded", 2),
            item("M-23", "tbw_liters", "Total Body Water Volume", tbwLiters, "L", MetricCategory.FLUID_BALANCE, compositionProvenance, "36.5 C Density", 1),
            item("M-24", "ecw_liters", "Extracellular Water (ECW)", ecwLiters, "L", MetricCategory.FLUID_BALANCE, if (ecwLiters != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (ecwLiters != null) "Dual-Freq Cole" else "Requires 8E", 1),
            item("M-25", "ecw_pct", "ECW %", ecwPct, "%", MetricCategory.FLUID_BALANCE, if (ecwPct != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (ecwPct != null) "Dual-Freq Cole" else "Requires 8E", 1),
            item("M-26", "icw_liters", "Intracellular Water (ICW)", icwLiters, "L", MetricCategory.FLUID_BALANCE, if (icwLiters != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (icwLiters != null) "Dual-Freq Cole" else "Requires 8E", 1),
            item("M-27", "icw_pct", "ICW %", icwPct, "%", MetricCategory.FLUID_BALANCE, if (icwPct != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (icwPct != null) "Dual-Freq Cole" else "Requires 8E", 1),
            item("M-28", "ecw_tbw_ratio", "ECW / TBW Ratio", ecwTbwRatio, "ratio", MetricCategory.FLUID_BALANCE, if (ecwTbwRatio != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (ecwTbwRatio != null) (if (ecwTbwRatio in 0.36..0.40) "Balanced" else "Monitor") else "Requires 8E", 3),

            // PRD:M-29..M-35 Muscle & Cellular
            item("M-29", "skeletal_muscle_mass_kg", "Skeletal Muscle Mass", skeletalMuscleMassKg, "kg", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Janssen BIA", 2),
            item("M-30", "skeletal_muscle_pct", "Skeletal Muscle %", smmPct, "%", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Recorded", 1),
            item("M-31", "smi_kg_m2", "Skeletal Muscle Index (SMI)", smi, "kg/m2", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Recorded", 2),
            item("M-32", "muscle_mass_kg", "Total Muscle Mass", muscleMassKg, "kg", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "FFM Less Bone", 2),
            item("M-33", "muscle_rate_pct", "Total Muscle Rate", totalMuscleRatePct, "%", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Recorded", 1),
            item("M-34", "muscle_to_fat_ratio", "Muscle-to-Fat Ratio", muscleToFatRatio, "ratio", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Recorded", 2),
            item("M-35", "bcm_kg", "Body Cell Mass (BCM)", bcmKg, "kg", MetricCategory.MUSCLE_METABOLIC, if (bcmKg != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, if (bcmKg != null) "Wang ICW/0.7" else "Requires 8E", 2),

            // PRD:M-36..M-40 Skeletal & Proteins
            item("M-36", "bone_mass_kg", "Bone Mass", boneMassKg, "kg", MetricCategory.SKELETAL_PROTEIN, compositionProvenance, "Recorded", 2),
            item("M-37", "bone_mineral_pct", "Bone Mass %", boneMineralPct, "%", MetricCategory.SKELETAL_PROTEIN, compositionProvenance, "Recorded", 2),
            item("M-38", "protein_mass_kg", "Protein Mass", proteinMassKg, "kg", MetricCategory.SKELETAL_PROTEIN, compositionProvenance, "Recorded", 2),
            item("M-39", "protein_pct", "Protein %", proteinPct, "%", MetricCategory.SKELETAL_PROTEIN, compositionProvenance, "Recorded", 1),
            item("M-40", "mineral_mass_kg", "Inorganic Salt / Mineral Mass", mineralMassKg, "kg", MetricCategory.SKELETAL_PROTEIN, compositionProvenance, "Estimated", 2),

            // PRD:M-41..M-45 Energy & Composite Score
            item("M-41", "bmr_kcal", "Basal Metabolic Rate", bmr.toDouble(), "kcal/d", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, if (hasBia) "Katch-McArdle" else "Mifflin-St Jeor", 0),
            item("M-42", "tdee_sedentary_kcal", "TDEE Sedentary (1.2x)", tdeeSedentary.toDouble(), "kcal/d", MetricCategory.MUSCLE_METABOLIC, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, "1.2x Baseline", 0),
            item("M-43", "tdee_kcal", "TDEE Active", tdee.toDouble(), "kcal/d", MetricCategory.MUSCLE_METABOLIC, MetricProvenance.ANTHROPOMETRIC_ESTIMATE, anthropometry.activityLevel.label, 0),
            item("M-44", "metabolic_age_years", "Metabolic Body Age", metabolicAge.toDouble(), "yrs", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, "Recorded", 0),
            item("M-45", "composite_score", "Composite Body Score", score.toDouble(), "/100", MetricCategory.MUSCLE_METABOLIC, compositionProvenance, if (score >= 80) "Strong" else "Baseline", 0),

            // PRD:M-46..M-50 Segmental Muscle (kg)
            item("M-46", "left_arm_muscle_kg", "Left Arm Muscle", segMuscle?.leftArmKg, "kg", MetricCategory.SEGMENTAL, if (segMuscle != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segMusclePctStd["left_arm"]?.let { "${it.round1()}% std" } ?: "Requires 8E", 2),
            item("M-47", "right_arm_muscle_kg", "Right Arm Muscle", segMuscle?.rightArmKg, "kg", MetricCategory.SEGMENTAL, if (segMuscle != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segMusclePctStd["right_arm"]?.let { "${it.round1()}% std" } ?: "Requires 8E", 2),
            item("M-48", "trunk_muscle_kg", "Trunk Muscle", segMuscle?.trunkKg, "kg", MetricCategory.SEGMENTAL, if (segMuscle != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segMusclePctStd["trunk"]?.let { "${it.round1()}% std" } ?: "Requires 8E", 2),
            item("M-49", "left_leg_muscle_kg", "Left Leg Muscle", segMuscle?.leftLegKg, "kg", MetricCategory.SEGMENTAL, if (segMuscle != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segMusclePctStd["left_leg"]?.let { "${it.round1()}% std" } ?: "Requires 8E", 2),
            item("M-50", "right_leg_muscle_kg", "Right Leg Muscle", segMuscle?.rightLegKg, "kg", MetricCategory.SEGMENTAL, if (segMuscle != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segMusclePctStd["right_leg"]?.let { "${it.round1()}% std" } ?: "Requires 8E", 2),

            // PRD:M-51..M-55 Segmental Fat (kg + % in statusLabel)
            item("M-51", "left_arm_fat_kg", "Left Arm Fat", segFat?.leftArmKg, "kg", MetricCategory.SEGMENTAL, if (segFat != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segFat?.let { "${it.leftArmKg.round2()} kg / ${segFatPctOfMass["left_arm"]?.round1() ?: "--"}%" } ?: "Requires 8E", 2),
            item("M-52", "right_arm_fat_kg", "Right Arm Fat", segFat?.rightArmKg, "kg", MetricCategory.SEGMENTAL, if (segFat != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segFat?.let { "${it.rightArmKg.round2()} kg / ${segFatPctOfMass["right_arm"]?.round1() ?: "--"}%" } ?: "Requires 8E", 2),
            item("M-53", "trunk_fat_kg", "Trunk Fat", segFat?.trunkKg, "kg", MetricCategory.SEGMENTAL, if (segFat != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segFat?.let { "${it.trunkKg.round2()} kg / ${segFatPctOfMass["trunk"]?.round1() ?: "--"}%" } ?: "Requires 8E", 2),
            item("M-54", "left_leg_fat_kg", "Left Leg Fat", segFat?.leftLegKg, "kg", MetricCategory.SEGMENTAL, if (segFat != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segFat?.let { "${it.leftLegKg.round2()} kg / ${segFatPctOfMass["left_leg"]?.round1() ?: "--"}%" } ?: "Requires 8E", 2),
            item("M-55", "right_leg_fat_kg", "Right Leg Fat", segFat?.rightLegKg, "kg", MetricCategory.SEGMENTAL, if (segFat != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER, segFat?.let { "${it.rightLegKg.round2()} kg / ${segFatPctOfMass["right_leg"]?.round1() ?: "--"}%" } ?: "Requires 8E", 2),

            // PRD:M-56 Bilateral Symmetry
            item(
                "M-56",
                "bilateral_muscle_symmetry_idx",
                "Bilateral Muscle Symmetry",
                bilateralSym,
                "% diff",
                MetricCategory.CIRCUMFERENCE_INDICES,
                if (bilateralSym != null) MetricProvenance.DERIVED_8E_BIA else MetricProvenance.UNAVAILABLE_ON_TIER,
                if (bilateralSym != null) "Arm ${upperSym!!.round1()}% / Leg ${lowerSym!!.round1()}%" else "Requires 8E",
                1,
            ),
        )

        return FullBodyCompositionReport(
            effectiveTier = tier,
            plausibility = plausibility,
            weightKg = weightKg,
            heartRateBpm = validHr,
            cardiacIndexScore = cardiacIndexScore,
            bmi = bmi,
            whoBmiCategory = bmiCategoryLabel,
            idealWeightKg = idealWeightKg,
            weightDeltaToTargetKg = weightDeltaToTarget,
            z50KhzOhm = z50,
            zHighKhzOhm = zHigh,
            coleR0Ohm = coleR0,
            coleRinfOhm = coleRinf,
            bodyFatPct = bodyFatPct,
            fatMassKg = fatMassKg,
            fmiKgPerM2 = fmi,
            fatFreeMassKg = fatFreeMassKg,
            ffmiKgPerM2 = ffmi,
            subcutaneousFatPct = subcutaneousFatPct,
            visceralFatIndex = visceralFatIndex,
            obesityDegreePct = obesityDegreePct,
            tbwPct = tbwPct,
            tbwKg = tbwKg,
            tbwLiters = tbwLiters,
            ecwLiters = ecwLiters,
            ecwPct = ecwPct,
            icwLiters = icwLiters,
            icwPct = icwPct,
            ecwKg = ecwKg,
            icwKg = icwKg,
            ecwTbwRatio = ecwTbwRatio,
            bcmKg = bcmKg,
            skeletalMuscleMassKg = skeletalMuscleMassKg,
            smiKgPerM2 = smi,
            boneMassKg = boneMassKg,
            proteinKg = proteinMassKg,
            bmrKcal = bmr,
            tdeeSedentaryKcal = tdeeSedentary,
            tdeeKcal = tdee,
            metabolicAgeYears = metabolicAge,
            compositeScore = score,
            segmentalMuscle = segMuscle,
            segmentalFat = segFat,
            segmentalMusclePctStd = segMusclePctStd,
            segmentalFatPctOfMass = segFatPctOfMass,
            bilateralSymmetryPct = bilateralSym,
            upperSymmetryPct = upperSym,
            lowerSymmetryPct = lowerSym,
            appendicularLeanMassKg = almKg,
            asmiKgPerM2 = asmi,
            metrics = metricList,
        )
    }

    /**
     * Recomputes all 56 parameters when a measurement is reassigned to another member or when profile
     * anthropometry changes, preserving stored hardware tier and raw impedance.
     */
    fun recalculateForProfile(
        anthropometry: UserAnthropometry,
        sourceWireName: String,
        weightKg: Double,
        heartRateBpm: Int?,
        impedance50KhzOhm: Double?,
        dualFreqImpedance: DualFreqImpedance?,
    ): FullBodyCompositionReport {
        val requestedTier = when {
            sourceWireName == ScaleTier.CULT_PRO_8E.sourceWireName && dualFreqImpedance != null -> ScaleTier.CULT_PRO_8E
            sourceWireName == ScaleTier.CULT_BF01_4E.sourceWireName && impedance50KhzOhm != null -> ScaleTier.CULT_BF01_4E
            else -> ScaleTier.CULT_PRO_FEET_ONLY
        }
        return calculate(
            anthropometry = anthropometry,
            requestedTier = requestedTier,
            weightKg = weightKg,
            heartRateBpm = heartRateBpm,
            dualFreqImpedance = dualFreqImpedance,
            singleFreq50KhzOhm = impedance50KhzOhm,
        )
    }

    fun formatStoredMetricValue(item: BodyMetricItem, storedValue: Double?): String {
        if (storedValue == null) return "--"
        if (item.id == "who_bmi_category") return whoBmiLabelFromCode(storedValue)
        if (item.id == "weight_delta_to_target_kg") return String.format(Locale.US, "%+.1f", storedValue)
        return formatNumber(storedValue, item.decimals)
    }

    private fun formatNumber(value: Double, decimals: Int): String = when {
        decimals <= 0 -> value.roundToInt().toString()
        else -> String.format(Locale.US, "%.${decimals}f", value)
    }

    private fun Double.round1(): String = String.format(Locale.US, "%.1f", this)
    private fun Double.round2(): String = String.format(Locale.US, "%.2f", this)
}
