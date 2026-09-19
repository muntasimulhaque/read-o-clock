package io.github.muntasimulhaque.readoclock.core

/**
 * Which piece of the movement sounds next.
 *
 * One sample repeated once a second for a minute is heard as a loop even when
 * the sample itself is right, so the clock carries several variants of the
 * same tick and cycles through them. The cycle must also survive a piece that
 * never decoded: the player would rather play a neighbour twice than fall
 * silent on a step, because a missing tick is the one thing the ear notices
 * immediately.
 */
class TickRotation(size: Int) {
    private val ready = BooleanArray(size)
    private var next = 0

    val size: Int get() = ready.size

    /**
     * Marks the piece at [index] as playable. The first piece to arrive is
     * used immediately if a step is already waiting on it.
     */
    fun loaded(index: Int) {
        if (index in ready.indices) ready[index] = true
    }

    /** How many pieces have arrived. */
    fun readyCount(): Int = ready.count { it }

    /**
     * The index to play, advancing the cycle past it, or null when no piece
     * has decoded yet. Every decoded piece is used before any of them
     * repeats, so the ear never hears the same variant twice in a row while
     * another one is available.
     */
    fun take(): Int? {
        if (ready.isEmpty()) return null
        for (attempt in ready.indices) {
            val candidate = next
            next = (next + 1) % ready.size
            if (ready[candidate]) return candidate
        }
        return null
    }
}
