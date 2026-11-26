package org.opencult.scale.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-latency `AudioTrack` (`MODE_STATIC`) clinical sound engine.
 * Honors ringer mode (`RINGER_MODE_NORMAL`) and the user's `soundFeedbackEnabled` toggle.
 */
@Singleton
class OpenCultSoundEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "OpenCultAudioThread").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + audioDispatcher)
    private val cache = ConcurrentHashMap<OpenCultSoundCue, ShortArray>()

    @Volatile
    private var currentTrack: AudioTrack? = null

    @Volatile
    var isEnabled: Boolean = true

    fun play(cue: OpenCultSoundCue) {
        if (!isEnabled) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return
        }
        scope.launch {
            runCatching {
                val pcm = cache.getOrPut(cue) { PcmSoundSynthesizer.synthesize(cue) }
                if (pcm.isEmpty()) return@runCatching
                releaseCurrentTrack()

                val bufferSizeBytes = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(PcmSoundSynthesizer.SAMPLE_RATE_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .build()

                val written = track.write(pcm, 0, pcm.size)
                if (written > 0) {
                    currentTrack = track
                    track.play()
                } else {
                    track.release()
                }
            }
        }
    }

    fun release() {
        scope.launch { releaseCurrentTrack() }
    }

    private fun releaseCurrentTrack() {
        val prev = currentTrack
        currentTrack = null
        if (prev != null) {
            runCatching {
                if (prev.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    prev.stop()
                }
                prev.release()
            }
        }
    }
}
