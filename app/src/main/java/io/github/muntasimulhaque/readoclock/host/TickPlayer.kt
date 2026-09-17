package io.github.muntasimulhaque.readoclock.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.muntasimulhaque.readoclock.R

// The sample peaks at 0.45; 0.7 on top of that sits a tick under the room
// instead of over it.
private const val TickVolume = 0.7f

/**
 * The quartz tick: one soft click for each step of the second hand.
 *
 * SoundPool keeps the little sample decoded in memory, so a step can sound
 * the instant the hand moves. The player is built with the screen and
 * released with it, so nothing ticks in the background. A click needs no
 * permission and no audio focus.
 */
class TickPlayer(context: Context) {

    private val pool = SoundPool.Builder()
        // One stream: a new step replaces an old one instead of stacking.
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sampleId: Int
    private var ready = false
    private var pending = false

    init {
        pool.setOnLoadCompleteListener { _, _, status ->
            ready = status == 0
            if (ready && pending) {
                pending = false
                play()
            }
        }
        sampleId = pool.load(context.applicationContext, R.raw.tick, 1)
    }

    /**
     * Sounds one step. A step that lands while the sample is still decoding
     * is kept and sounded on load, so the first tick is never lost.
     */
    fun tick() {
        if (ready) {
            play()
        } else {
            pending = true
        }
    }

    /** Frees the sample; the screen calls this when it is gone for good. */
    fun release() {
        pending = false
        pool.release()
    }

    private fun play() {
        pool.play(sampleId, TickVolume, TickVolume, 1, 0, 1f)
    }
}
