package org.opencult.scale.core.bia

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BodyMetricsEngineTest {

    private val defaultUser = UserAnthropometry(
        sex = BiologicalSex.MALE,
        ageYears = 30,
        heightCm = 178.0,
        activityLevel = ActivityLevel.MODERATE,
        athleteMode = false,
        dailyStepsAvg = 7800,
        targetWeightKg = 72.0,
    )

    private val bodyPod10ChannelImpedance = DualFreqImpedance(
        lowRightArmOhm = 307.4,
        lowLeftArmOhm = 311.3,
        lowTrunkOhm = 26.8,
        lowRightLegOhm = 242.1,
        lowLeftLegOhm = 256.2,
        highRightArmOhm = 267.6,
        highLeftArmOhm = 275.5,
        highTrunkOhm = 24.5,
        highRightLegOhm = 219.1,
        highLeftLegOhm = 229.5,
    )

    @Test
    fun emits_all_56_prd_metrics_aligned_1_to_1_with_prd_catalog_and_mass_conservation() {
        val report = BodyMetricsEngine.calculate(
            anthropometry = defaultUser,
            requestedTier = ScaleTier.CULT_PRO_8E,
            weightKg = 74.8,
            heartRateBpm = 68,
            dualFreqImpedance = bodyPod10ChannelImpedance,
        )

        assertThat(report.effectiveTier).isEqualTo(ScaleTier.CULT_PRO_8E)
        assertThat(report.plausibility.passed).isTrue()
        assertThat(report.metrics).hasSize(56)
        assertThat(report.metrics.map { it.prdCode }.distinct()).hasSize(56)
        assertThat(report.metrics.map { it.id }.distinct()).hasSize(56)
        assertThat(report.metrics.first().prdCode).isEqualTo("M-01")
        assertThat(report.metrics.first { it.prdCode == "M-03" }.id).isEqualTo("who_bmi_category")
        assertThat(report.metrics.first { it.prdCode == "M-03" }.formattedValue).isEqualTo("Normal")
        assertThat(report.metrics.first { it.prdCode == "M-08" }.id).isEqualTo("impedance_50khz_ohm")
        assertThat(report.metrics.first { it.prdCode == "M-23" }.id).isEqualTo("tbw_liters")
        assertThat(report.metrics.first { it.prdCode == "M-41" }.id).isEqualTo("bmr_kcal")
        assertThat(report.metrics.first { it.prdCode == "M-41" }.formattedValue).doesNotContain(".")
        assertThat(report.metrics.last().prdCode).isEqualTo("M-56")
        assertThat(report.metrics.last().id).isEqualTo("bilateral_muscle_symmetry_idx")
        assertThat(report.metrics.none { it.id == "whr_estimate" }).isTrue()

        // Cole fluid conservation: ECW + ICW == TBW and R0 > Zlow > Zhigh > Rinf
        assertThat(report.coleR0Ohm!!).isGreaterThan(bodyPod10ChannelImpedance.wholeBodyLowOhm)
        assertThat(report.coleRinfOhm!!).isLessThan(bodyPod10ChannelImpedance.wholeBodyHighOhm)
        assertThat(report.ecwKg).isNotNull()
        assertThat(report.icwKg).isNotNull()
        assertThat(report.ecwKg!! + report.icwKg!!).isWithin(0.001).of(report.tbwKg)
        assertThat(report.ecwTbwRatio!!).isIn(com.google.common.collect.Range.closed(0.34, 0.45))

        // 4-compartment mass closure: Fat + TBW + Protein + Mineral == Weight
        val mineralKg = report.metrics.first { it.id == "mineral_mass_kg" }.value!!
        assertThat(report.fatMassKg + report.tbwKg + report.proteinKg + mineralKg)
            .isWithin(0.05)
            .of(report.weightKg)

        // Segmental 5-part mass conservation (M-46..M-55)
        assertThat(report.segmentalMuscle!!.sumKg).isWithin(0.001).of(report.skeletalMuscleMassKg)
        assertThat(report.segmentalFat!!.sumKg).isWithin(0.001).of(report.fatMassKg)
        // Asymmetry sensitivity check: highRightLegOhm (219.1) != highLeftLegOhm (229.5) -> RL > LL muscle
        assertThat(report.segmentalMuscle.rightLegKg).isGreaterThan(report.segmentalMuscle.leftLegKg)
        assertThat(report.heartRateBpm).isEqualTo(68)
    }

    @Test
    fun whole_body_dual_freq_averages_match_bodypod_10_channel_vector() {
        assertThat(bodyPod10ChannelImpedance.wholeBodyLowOhm).isWithin(0.01).of(585.3)
        assertThat(bodyPod10ChannelImpedance.wholeBodyHighOhm).isWithin(0.01).of(520.35)
    }

    @Test
    fun implausible_8e_impedance_downgrades_to_feet_only_and_suppresses_segmental_ecw_claims() {
        val zeroedHandle = bodyPod10ChannelImpedance.copy(
            lowRightArmOhm = 0.0,
            highRightArmOhm = 0.0,
        )

        val report = BodyMetricsEngine.calculate(
            anthropometry = defaultUser,
            requestedTier = ScaleTier.CULT_PRO_8E,
            weightKg = 74.8,
            dualFreqImpedance = zeroedHandle,
        )

        assertThat(report.plausibility.passed).isFalse()
        assertThat(report.plausibility.downgradedToFeetOnly).isTrue()
        assertThat(report.effectiveTier).isEqualTo(ScaleTier.CULT_PRO_FEET_ONLY)
        assertThat(report.ecwKg).isNull()
        assertThat(report.icwKg).isNull()
        assertThat(report.segmentalMuscle).isNull()
        val gatedMetric = report.metrics.first { it.prdCode == "M-24" }
        assertThat(gatedMetric.provenance).isEqualTo(MetricProvenance.UNAVAILABLE_ON_TIER)
        assertThat(gatedMetric.value).isNull()
    }

    @Test
    fun bf01_with_valid_impedance_marks_derived_4e_and_suppresses_8e_exclusives() {
        val report = BodyMetricsEngine.calculate(
            anthropometry = defaultUser.copy(sex = BiologicalSex.FEMALE, heightCm = 165.0),
            requestedTier = ScaleTier.CULT_BF01_4E,
            weightKg = 61.5,
            heartRateBpm = 72,
            singleFreq50KhzOhm = 540.0,
        )

        assertThat(report.effectiveTier).isEqualTo(ScaleTier.CULT_BF01_4E)
        assertThat(report.ecwKg).isNull()
        assertThat(report.segmentalMuscle).isNull()
        assertThat(report.metrics.first { it.prdCode == "M-12" }.provenance).isEqualTo(MetricProvenance.DERIVED_4E_BIA)
        assertThat(report.metrics.first { it.prdCode == "M-46" }.provenance).isEqualTo(MetricProvenance.UNAVAILABLE_ON_TIER)
        assertThat(report.bmrKcal).isGreaterThan(1000)
    }

    @Test
    fun extreme_light_and_heavy_subjects_stay_bounded_and_non_nan() {
        val light = BodyMetricsEngine.calculate(
            anthropometry = UserAnthropometry(BiologicalSex.FEMALE, ageYears = 19, heightCm = 150.0),
            requestedTier = ScaleTier.CULT_PRO_FEET_ONLY,
            weightKg = 42.0,
        )
        val heavy = BodyMetricsEngine.calculate(
            anthropometry = UserAnthropometry(BiologicalSex.MALE, ageYears = 55, heightCm = 192.0),
            requestedTier = ScaleTier.CULT_BF01_4E,
            weightKg = 148.0,
            singleFreq50KhzOhm = 390.0,
        )

        assertThat(light.bodyFatPct).isIn(com.google.common.collect.Range.closed(5.0, 58.0))
        assertThat(heavy.bodyFatPct).isIn(com.google.common.collect.Range.closed(5.0, 58.0))
        assertThat(heavy.metrics.all { it.formattedValue != "NaN" }).isTrue()
    }

    @Test
    fun recalculate_for_profile_updates_body_composition_when_reassigned_between_sexes() {
        val maleReport = BodyMetricsEngine.recalculateForProfile(
            anthropometry = defaultUser,
            sourceWireName = ScaleTier.CULT_BF01_4E.sourceWireName,
            weightKg = 70.0,
            heartRateBpm = 70,
            impedance50KhzOhm = 510.0,
            dualFreqImpedance = null,
        )
        val femaleReport = BodyMetricsEngine.recalculateForProfile(
            anthropometry = defaultUser.copy(sex = BiologicalSex.FEMALE, heightCm = 162.0),
            sourceWireName = ScaleTier.CULT_BF01_4E.sourceWireName,
            weightKg = 70.0,
            heartRateBpm = 70,
            impedance50KhzOhm = 510.0,
            dualFreqImpedance = null,
        )

        assertThat(femaleReport.bodyFatPct).isGreaterThan(maleReport.bodyFatPct)
        assertThat(femaleReport.idealWeightKg).isLessThan(maleReport.idealWeightKg)
    }
}
