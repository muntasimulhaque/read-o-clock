package io.github.muntasimulhaque.readoclock.core

/**
 * When the movement ticks: one click for each whole second the running clock
 * crosses.
 *
 * The schedule exists because a hand that is set is not a hand that runs
 * (D-009). A drag turns the gear train by hand and stays silent, and so does
 * a jump: a resume, a cold start, or a TalkBack move, none of which is a
 * quartz step.
 *
 * It also exists because the frame loop cannot promise to see every second
 * boundary. The loop wakes near each boundary, but a stalled main thread can
 * carry the reading past two or three boundaries between wakes, and 0.4's
 * rule, "tick only when the second advanced by exactly one", then dropped the
 * tick entirely: the hand stepped and the clock stayed silent, which is what
 * "the sound sometimes does not play" was. The movement struck once during
 * that stall, so one tick is the truthful answer, never a burst of them.
 *
 * Readings are local seconds of the day, so they wrap at midnight. The
 * schedule unwraps them into a continuous count, which is why a step across
 * midnight is a step and a jump backwards is not.
 */
class TickSchedule {
    private var unwrappedSeconds: Double? = null
    private var lastSecond: Long? = null

    /**
     * Arms the schedule at a reading without ticking. A reading that arrives
     * from a resume, a cold start or a TalkBack move is not a step, so the
     * screen arms rather than ticks.
     */
    fun arm(secondsOfDay: Double) {
        unwrappedSeconds = secondsOfDay
        lastSecond = secondsIn(secondsOfDay)
    }

    /**
     * True when this reading crossed a whole second in the running direction.
     * A reading that moved backwards, from a drag or a TalkBack move, is not
     * a step. A reading before any call to [arm] is treated as already armed,
     * so a caller that forgets gets a silent first reading rather than a
     * stray tick.
     */
    fun ticked(secondsOfDay: Double, dragging: Boolean): Boolean {
        val previous = unwrappedSeconds ?: secondsOfDay
        val current = previous + signedDelta(previous, secondsOfDay)
        unwrappedSeconds = current
        val second = secondsIn(current)
        val was = lastSecond ?: second
        lastSecond = second
        return !dragging && second > was
    }

    /**
     * The step from one reading to the next, with the day's wrap undone: a
     * reading that fell a long way back is the clock reaching midnight, not
     * the hands turning backwards.
     */
    private fun signedDelta(from: Double, to: Double): Double {
        val delta = to - from
        return when {
            delta < -WrapThreshold -> delta + ClockTime.SecondsPerDay
            delta > WrapThreshold -> delta - ClockTime.SecondsPerDay
            else -> delta
        }
    }

    private fun secondsIn(seconds: Double): Long = Math.floor(seconds).toLong()

    private companion object {
        /** Half a day: nearer than this, a move is a real move. */
        const val WrapThreshold = ClockTime.SecondsPerDay / 2.0
    }
}
