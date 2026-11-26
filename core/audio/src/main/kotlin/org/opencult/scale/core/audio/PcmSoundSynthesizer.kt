package org.opencult.scale.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class OpenCultSoundCue {
    SCALE_CONNECTED,
    SETTLING_TICK,
    WEIGHT_LOCKED,
    METRICS_LOGGED_PRO_50,
    METRICS_LOGGED_BASIC_20,
    AMBIGUOUS_MEMBER_PROMPT,
    CAPTURE_ERROR,
}

/**
 * Pure JVM procedural PCM tone synthesizer (`44.1 kHz`, 16-bit signed mono) for calm clinical audio cues.
 * Every segment enforces raised-cosine Hann attack/release envelopes (`>= 6 ms`) and `-12..-18 dBFS`
 * headroom with zero clipping or DC click boundaries.
 */
object PcmSoundSynthesizer {
    const val SAMPLE_RATE_HZ = 44_100
    private const val FULL_SCALE = 32767.0

    fun synthesize(cue: OpenCultSoundCue): ShortArray = when (cue) {
        OpenCultSoundCue.SCALE_CONNECTED -> concatNotes(
            listOf(
                ToneSpec(freqHz = 528.0, durationMs = 68, peakDbfs = -15.0, attackMs = 7, releaseMs = 18),
                ToneSpec(freqHz = 792.0, durationMs = 92, peakDbfs = -14.0, attackMs = 7, releaseMs = 28),
            ),
            gapMs = 12,
        )
        OpenCultSoundCue.SETTLING_TICK -> renderTone(
            ToneSpec(freqHz = 1120.0, durationMs = 24, peakDbfs = -17.5, attackMs = 6, releaseMs = 12, glideToHz = 860.0),
        )
        OpenCultSoundCue.WEIGHT_LOCKED -> renderTone(
            ToneSpec(freqHz = 659.25, durationMs = 135, peakDbfs = -13.5, attackMs = 7, releaseMs = 45, secondHarmonicMix = 0.22),
        )
        OpenCultSoundCue.METRICS_LOGGED_PRO_50 -> concatNotes(
            listOf(
                ToneSpec(freqHz = 523.25, durationMs = 90, peakDbfs = -14.5, attackMs = 7, releaseMs = 25, secondHarmonicMix = 0.18),
                ToneSpec(freqHz = 659.25, durationMs = 90, peakDbfs = -14.0, attackMs = 7, releaseMs = 25, secondHarmonicMix = 0.20),
                ToneSpec(freqHz = 783.99, durationMs = 165, peakDbfs = -13.0, attackMs = 8, releaseMs = 70, secondHarmonicMix = 0.24),
            ),
            gapMs = 14,
        )
        OpenCultSoundCue.METRICS_LOGGED_BASIC_20 -> concatNotes(
            listOf(
                ToneSpec(freqHz = 523.25, durationMs = 85, peakDbfs = -14.5, attackMs = 7, releaseMs = 24, secondHarmonicMix = 0.16),
                ToneSpec(freqHz = 783.99, durationMs = 140, peakDbfs = -13.5, attackMs = 7, releaseMs = 55, secondHarmonicMix = 0.20),
            ),
            gapMs = 14,
        )
        OpenCultSoundCue.AMBIGUOUS_MEMBER_PROMPT -> concatNotes(
            listOf(
                ToneSpec(freqHz = 493.88, durationMs = 95, peakDbfs = -15.0, attackMs = 8, releaseMs = 30),
                ToneSpec(freqHz = 587.33, durationMs = 115, peakDbfs = -14.5, attackMs = 8, releaseMs = 40),
            ),
            gapMs = 32,
        )
        OpenCultSoundCue.CAPTURE_ERROR -> concatNotes(
            listOf(
                ToneSpec(freqHz = 440.0, durationMs = 90, peakDbfs = -15.0, attackMs = 7, releaseMs = 25),
                ToneSpec(freqHz = 329.63, durationMs = 145, peakDbfs = -14.5, attackMs = 7, releaseMs = 55),
            ),
            gapMs = 18,
        )
    }

    data class ToneSpec(
        val freqHz: Double,
        val durationMs: Int,
        val peakDbfs: Double,
        val attackMs: Int = 6,
        val releaseMs: Int = 18,
        val glideToHz: Double? = null,
        val secondHarmonicMix: Double = 0.12,
    )

    fun renderTone(spec: ToneSpec): ShortArray {
        val totalSamples = (SAMPLE_RATE_HZ * spec.durationMs) / 1000
        if (totalSamples <= 2) return ShortArray(0)
        val attackSamples = ((SAMPLE_RATE_HZ * spec.attackMs) / 1000).coerceIn(1, totalSamples / 2)
        val releaseSamples = ((SAMPLE_RATE_HZ * spec.releaseMs) / 1000).coerceIn(1, totalSamples / 2)
        val linearAmp = 10.0.pow(spec.peakDbfs.coerceIn(-24.0, -12.0) / 20.0)
        val out = ShortArray(totalSamples)
        var phase = 0.0
        val normMix = 1.0 + spec.secondHarmonicMix

        for (i in 0 until totalSamples) {
            if (i == 0 || i == totalSamples - 1) {
                out[i] = 0
                continue
            }
            val progress = i.toDouble() / (totalSamples - 1).toDouble()
            val instFreq = if (spec.glideToHz != null) {
                spec.freqHz + (spec.glideToHz - spec.freqHz) * progress
            } else {
                spec.freqHz
            }
            phase += 2.0 * PI * instFreq / SAMPLE_RATE_HZ

            val env = when {
                i < attackSamples -> 0.5 * (1.0 - cos(PI * i.toDouble() / attackSamples.toDouble()))
                i >= totalSamples - releaseSamples -> {
                    val rem = (totalSamples - 1 - i).toDouble()
                    0.5 * (1.0 - cos(PI * rem / releaseSamples.toDouble()))
                }
                else -> 1.0
            }
            val wave = (sin(phase) + spec.secondHarmonicMix * sin(2.0 * phase)) / normMix
            val scaled = (wave * env * linearAmp * FULL_SCALE).roundToInt()
                .coerceIn(Short.MIN_VALUE + 1, Short.MAX_VALUE.toInt())
            out[i] = scaled.toShort()
        }
        return out
    }

    private fun concatNotes(notes: List<ToneSpec>, gapMs: Int): ShortArray {
        val gapSamples = (SAMPLE_RATE_HZ * gapMs) / 1000
        val segments = notes.map { renderTone(it) }
        val total = segments.sumOf { it.size } + (gapSamples * (segments.size - 1).coerceAtLeast(0))
        val out = ShortArray(total)
        var offset = 0
        segments.forEachIndexed { idx, seg ->
            seg.copyInto(out, destinationOffset = offset)
            offset += seg.size
            if (idx < segments.lastIndex) {
                offset += gapSamples
            }
        }
        return out
    }
}
