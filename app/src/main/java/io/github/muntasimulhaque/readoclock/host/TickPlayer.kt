package io.github.muntasimulhaque.readoclock.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.muntasimulhaque.readoclock.R
import io.github.muntasimulhaque.readoclock.core.TickRotation

// The samples peak at 0.45; 0.7 on top of that sits a tick under the room
// instead of over it.
private const val TickVolume = 0.7f

/**
 * The quartz pieces, in the order the player rotates them. Four variants of
 * the same movement: the same character a little apart in level, tilt and
 * timing, so a tick a second does not read as a loop.
 */
private val TickSamples = intArrayOf(R.raw.tick_1, R.raw.tick_2, R.raw.tick_3, R.raw.tick_4)

/**
 * The quartz tick: one soft click for each step of the second hand.
 *
 * SoundPool keeps the little samples decoded in memory, so a step can sound
 * the instant the hand moves. The player is built with the screen and
 * released with it, so nothing ticks in the background. A click needs no
 * permission and no audio focus.
 *
 * Two streams, not one. The tick runs sixty six milliseconds, so a step that
 * lands soon after the previous one would otherwise be cut off by its own
 * successor, which reads as a dropped tick. Two streams let a step overlap
 * the tail of the one before it, the way a real movement's pieces do.
 */
class TickPlayer(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(Streams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** Which piece sounds next; the cycle and its rules live in core. */
    private val rotation = TickRotation(TickSamples.size)

    /** One entry per variant: the sample id, or 0 until it has decoded. */
    private val sampleIds = IntArray(TickSamples.size)
    private var pending = false
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released || status != 0) return@setOnLoadCompleteListener
            val index = sampleIds.indexOf(sampleId)
            if (index < 0) return@setOnLoadCompleteListener
            rotation.loaded(index)
            // A step that arrived while the pieces were still decoding sounds
            // as soon as there is something to sound.
            if (pending) {
                pending = false
                sound()
            }
        }
        for (i in TickSamples.indices) {
            sampleIds[i] = pool.load(context.applicationContext, TickSamples[i], 1)
        }
    }

    /**
     * Sounds one step. If no piece has decoded yet, the step is remembered
     * and sounded on load, because a missing tick is the one thing the player
     * must never do.
     */
    fun tick() {
        if (!sound()) pending = true
    }

    /** Frees the samples; the screen calls this when it is gone for good. */
    fun release() {
        released = true
        pending = false
        pool.release()
    }

    /** Plays the next decoded piece, or reports that none is ready yet. */
    private fun sound(): Boolean {
        if (released) return false
        val index = rotation.take() ?: return false
        val sample = sampleIds[index]
        if (sample == 0) return false
        pool.play(sample, TickVolume, TickVolume, 1, 0, 1f)
        return true
    }

    private companion object {
        /**
         * Two steps can overlap: the piece runs sixty six milliseconds and a
         * step arrives every second, but a released hand settles onto the
         * minute, and the step after that can land early.
         */
        const val Streams = 2
    }
}
