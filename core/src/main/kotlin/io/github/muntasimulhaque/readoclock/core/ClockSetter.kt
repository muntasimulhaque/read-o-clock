package io.github.muntasimulhaque.readoclock.core

/**
 * The one piece of state the app owns: how far the clock has been set away
 * from live time. A cold start is zero, which is live time (D-004). Setting
 * a hand solves for the time under the finger, stores the offset, and from
 * then on the reading runs at the same rate as the wall clock, second hand
 * phase untouched.
 */
class ClockSetter {
    private var offsetSeconds: Double = 0.0

    /** The displayed time for a live wall clock reading. */
    fun reading(liveSeconds: Double): Double = normalize(liveSeconds + offsetSeconds)

    fun setFromMinuteAngle(liveSeconds: Double, angle: Double) {
        val current = reading(liveSeconds)
        val target = ClockTime.snapMinute(ClockTime.timeFromMinuteAngle(current, angle))
        setTarget(liveSeconds, target)
    }

    fun setFromHourAngle(liveSeconds: Double, angle: Double) {
        val current = reading(liveSeconds)
        setTarget(liveSeconds, ClockTime.timeFromHourAngle(current, angle))
    }

    /** The TalkBack actions move the whole clock, both hands at once. */
    fun moveBy(liveSeconds: Double, deltaSeconds: Double) {
        setTarget(liveSeconds, reading(liveSeconds) + deltaSeconds)
    }

    private fun setTarget(liveSeconds: Double, target: Double) {
        offsetSeconds = normalize(target) - normalize(liveSeconds)
    }

    private fun normalize(seconds: Double): Double =
        ((seconds % ClockTime.SecondsPerDay) + ClockTime.SecondsPerDay) % ClockTime.SecondsPerDay
}
