package org.opencult.scale.core.audio

import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.log10
import org.junit.Test

class PcmSoundSynthesizerTest {

    @Test
    fun all_cues_start_and_end_at_zero_have_zero_dc_offset_and_respect_dbfs_headroom() {
        OpenCultSoundCue.entries.forEach { cue ->
            val samples = PcmSoundSynthesizer.synthesize(cue)
            assertThat(samples.isNotEmpty()).isTrue()
            assertThat(samples.first().toInt()).isEqualTo(0)
            assertThat(samples.last().toInt()).isEqualTo(0)

            val peak = samples.maxOf { abs(it.toInt()) }
            val peakDbfs = 20.0 * log10(peak / 32767.0)
            assertThat(peakDbfs).isIn(com.google.common.collect.Range.closed(-20.0, -12.0))

            // Mean must stay tight around 0 (no DC click or asymmetry)
            val mean = samples.map { it.toDouble() }.average()
            assertThat(abs(mean)).isLessThan(80.0)

            // Raised-cosine envelope keeps first nonzero boundary step gentle
            val firstStep = abs(samples[1].toInt() - samples[0].toInt())
            assertThat(firstStep).isLessThan(120)
        }
    }

    @Test
    fun pro_50_chime_has_three_note_duration_greater_than_basic_20() {
        val pro = PcmSoundSynthesizer.synthesize(OpenCultSoundCue.METRICS_LOGGED_PRO_50)
        val basic = PcmSoundSynthesizer.synthesize(OpenCultSoundCue.METRICS_LOGGED_BASIC_20)
        val tick = PcmSoundSynthesizer.synthesize(OpenCultSoundCue.SETTLING_TICK)

        assertThat(pro.size).isGreaterThan(basic.size)
        assertThat(tick.size).isLessThan(1500)
    }
}
