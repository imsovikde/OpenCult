package org.opencult.scale.core.bia

import kotlinx.serialization.Serializable

@Serializable
enum class BiologicalSex {
    MALE,
    FEMALE,
}

@Serializable
enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sedentary"),
    MILD(1.375, "Lightly Active"),
    MODERATE(1.55, "Moderately Active"),
    HEAVY(1.725, "Very Active"),
    EXTREME(1.9, "Athlete / Heavy"),
}

@Serializable
enum class ScaleTier(val sourceWireName: String) {
    CULT_PRO_8E("ble_8e_handles"),
    CULT_PRO_FEET_ONLY("ble_4e_feet"),
    CULT_BF01_4E("ble_4e_feet"),
    MANUAL_OR_IMPORT("manual"),
}

@Serializable
enum class MetricProvenance {
    DIRECT_HARDWARE,
    DERIVED_8E_BIA,
    DERIVED_4E_BIA,
    ANTHROPOMETRIC_ESTIMATE,
    UNAVAILABLE_ON_TIER,
}

@Serializable
enum class MetricCategory(val title: String) {
    CORE_WEIGHT("Core & Weight"),
    ADIPOSITY("Adiposity & Visceral Fat"),
    FLUID_BALANCE("Hydration & Dual-Freq Fluids"),
    MUSCLE_METABOLIC("Muscle & Metabolic Energy"),
    SKELETAL_PROTEIN("Skeletal & Minerals"),
    SEGMENTAL("8-Electrode Segmental Split"),
    CIRCUMFERENCE_INDICES("Symmetry & Limb Indices"),
}

@Serializable
data class UserAnthropometry(
    val sex: BiologicalSex,
    val ageYears: Int,
    val heightCm: Double,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val athleteMode: Boolean = false,
    val dailyStepsAvg: Int? = null,
    val targetWeightKg: Double? = null,
)

/**
 * Ten impedance channels across Set A (20 kHz low frequency) and Set B (high frequency 50/100 kHz),
 * mapped 1:1 from 0xBE packets 0x00 and 0x01 (`BodyPod2` / Cult Pro 8-electrode layout).
 */
@Serializable
data class DualFreqImpedance(
    val lowRightArmOhm: Double,
    val lowLeftArmOhm: Double,
    val lowTrunkOhm: Double,
    val lowRightLegOhm: Double,
    val lowLeftLegOhm: Double,
    val highRightArmOhm: Double,
    val highLeftArmOhm: Double,
    val highTrunkOhm: Double,
    val highRightLegOhm: Double,
    val highLeftLegOhm: Double,
) {
    val wholeBodyLowOhm: Double
        get() = ((lowRightArmOhm + lowLeftArmOhm) / 2.0) + lowTrunkOhm + ((lowRightLegOhm + lowLeftLegOhm) / 2.0)

    val wholeBodyHighOhm: Double
        get() = ((highRightArmOhm + highLeftArmOhm) / 2.0) + highTrunkOhm + ((highRightLegOhm + highLeftLegOhm) / 2.0)

    val wholeBody50KhzEquivalentOhm: Double
        get() = wholeBodyHighOhm + 0.35 * (wholeBodyLowOhm - wholeBodyHighOhm)

    fun toLowList(): List<Double> = listOf(lowRightArmOhm, lowLeftArmOhm, lowTrunkOhm, lowRightLegOhm, lowLeftLegOhm)
    fun toHighList(): List<Double> = listOf(highRightArmOhm, highLeftArmOhm, highTrunkOhm, highRightLegOhm, highLeftLegOhm)
}

@Serializable
data class BiaPlausibilityResult(
    val passed: Boolean,
    val downgradedToFeetOnly: Boolean,
    val reason: String? = null,
)

@Serializable
data class BodyMetricItem(
    val id: String,
    val prdCode: String,
    val label: String,
    val value: Double?,
    val formattedValue: String,
    val unit: String,
    val category: MetricCategory,
    val provenance: MetricProvenance,
    val statusLabel: String,
    val decimals: Int = 1,
)

@Serializable
data class SegmentalMass(
    val leftArmKg: Double,
    val rightArmKg: Double,
    val trunkKg: Double,
    val leftLegKg: Double,
    val rightLegKg: Double,
) {
    val sumKg: Double get() = leftArmKg + rightArmKg + trunkKg + leftLegKg + rightLegKg
}

@Serializable
data class FullBodyCompositionReport(
    val effectiveTier: ScaleTier,
    val plausibility: BiaPlausibilityResult,
    val weightKg: Double,
    val heartRateBpm: Int?,
    val cardiacIndexScore: Int?,
    val bmi: Double,
    val whoBmiCategory: String,
    val idealWeightKg: Double,
    val weightDeltaToTargetKg: Double,
    val z50KhzOhm: Double?,
    val zHighKhzOhm: Double?,
    val coleR0Ohm: Double?,
    val coleRinfOhm: Double?,
    val bodyFatPct: Double,
    val fatMassKg: Double,
    val fmiKgPerM2: Double,
    val fatFreeMassKg: Double,
    val ffmiKgPerM2: Double,
    val subcutaneousFatPct: Double,
    val visceralFatIndex: Double,
    val obesityDegreePct: Double,
    val tbwPct: Double,
    val tbwKg: Double,
    val tbwLiters: Double,
    val ecwLiters: Double?,
    val ecwPct: Double?,
    val icwLiters: Double?,
    val icwPct: Double?,
    val ecwKg: Double?,
    val icwKg: Double?,
    val ecwTbwRatio: Double?,
    val bcmKg: Double?,
    val skeletalMuscleMassKg: Double,
    val smiKgPerM2: Double,
    val boneMassKg: Double,
    val proteinKg: Double,
    val bmrKcal: Int,
    val tdeeSedentaryKcal: Int,
    val tdeeKcal: Int,
    val metabolicAgeYears: Int,
    val compositeScore: Int,
    val segmentalMuscle: SegmentalMass?,
    val segmentalFat: SegmentalMass?,
    val segmentalMusclePctStd: Map<String, Double> = emptyMap(),
    val segmentalFatPctOfMass: Map<String, Double> = emptyMap(),
    val bilateralSymmetryPct: Double?,
    val upperSymmetryPct: Double?,
    val lowerSymmetryPct: Double?,
    val appendicularLeanMassKg: Double?,
    val asmiKgPerM2: Double?,
    val metrics: List<BodyMetricItem>,
)
