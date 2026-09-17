package io.github.muntasimulhaque.readoclock.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.round

/**
 * The clock's laws, pure and testable.
 *
 * One number, the local seconds since midnight, drives all three hands: the
 * hands are not independent state, they are views of that one value. This is
 * the gear law. Setting a hand solves for the time that would put that hand
 * under the finger, and the other hands then follow the way real gears make
 * them follow.
 *
 * The quartz tick: a real quartz movement advances the second hand once per
 * second, with a visible overshoot and settle because the hand has inertia.
 * Hour and minute hands move continuously, geared to the same movement.
 */
object ClockTime {
    const val SecondsPerDay = 86_400.0
    private const val MillisPerDay = 86_400_000L

    /** How long the second hand takes to travel one step, including its bounce. */
    const val QuartzTickSeconds = 0.16

    /** How close to a whole minute the hand must be before it settles onto it. */
    const val MinuteSnapWindowDegrees = 2.4

    // Ease-out-back, the shape a spring-settled hand makes: quick travel,
    // a small overshoot, then a settle. Overshoot is about 7 percent of a
    // step, roughly 0.4 degrees, which is what a quartz wall clock shows.
    private const val OvershootC1 = 1.4
    private const val OvershootC3 = OvershootC1 + 1.0

    /** Local seconds since midnight for an epoch timestamp and zone offset. */
    fun secondsOfDay(epochMillis: Long, zoneOffsetMillis: Int): Double {
        val localMillis = epochMillis + zoneOffsetMillis
        val sinceMidnight = ((localMillis % MillisPerDay) + MillisPerDay) % MillisPerDay
        return sinceMidnight / 1000.0
    }

    /** Degrees clockwise from 12 for the hour hand, continuous, 0.5 per minute. */
    fun hourAngleDegrees(secondsOfDay: Double): Double =
        normalize((secondsOfDay / 120.0) % 360.0)

    /** Degrees clockwise from 12 for the minute hand, continuous, 6 per minute. */
    fun minuteAngleDegrees(secondsOfDay: Double): Double =
        normalize((secondsOfDay / 10.0) % 360.0)

    /** Degrees clockwise from 12 for the second hand, stepped by the tick. */
    fun secondAngleDegrees(secondsOfDay: Double): Double =
        normalize((secondsOfDay % 60.0) * 6.0)

    /** The second hand as the eye sees it: the current step, mid-bounce. */
    fun displayedSecondAngleDegrees(secondsOfDay: Double): Double {
        val base = floor(secondsOfDay)
        val progress = ((secondsOfDay - base) / QuartzTickSeconds).coerceAtMost(1.0)
        return normalize((base - 1.0 + quartzEase(progress)) * 6.0)
    }

    /**
     * The bounce curve of one quartz step. 0 at the start, exactly 1 at the
     * end, and briefly above 1 while the hand overshoots. Tests pin all three.
     */
    fun quartzEase(progress: Double): Double {
        val u = progress.coerceIn(0.0, 1.0) - 1.0
        return 1.0 + OvershootC3 * u * u * u + OvershootC1 * u * u
    }

    /** Degrees clockwise from 12 of the point (x, y) around a center. */
    fun angleDegrees(x: Double, y: Double, centerX: Double, centerY: Double): Double =
        normalize(Math.toDegrees(atan2(x - centerX, centerY - y)))

    /**
     * The time that puts the minute hand under the finger, keeping the second
     * hand's phase, because setting a clock never touches the second hand. The
     * hour follows, and crossing 12 rolls the hour, which is the gear law.
     */
    fun timeFromMinuteAngle(currentSeconds: Double, angle: Double): Double {
        val minuteOfHour = normalize(angle) / 6.0
        val secondPhase = normalizeSeconds(currentSeconds % 60.0)
        val base = minuteOfHour * 60.0 + secondPhase
        val hours = round((currentSeconds - base) / 3600.0)
        return base + hours * 3600.0
    }

    /**
     * The time that puts the hour hand under the finger. Because the hour hand
     * carries the minutes inside its angle, the minute hand follows it, and
     * the 12 hour ambiguity resolves to whichever side of noon was nearer.
     */
    fun timeFromHourAngle(currentSeconds: Double, angle: Double): Double {
        val hours = normalize(angle) / 30.0
        val wholeHours = floor(hours)
        val minutes = (hours - wholeHours) * 60.0
        val secondPhase = normalizeSeconds(currentSeconds % 60.0)
        val near = wholeHours * 3600.0 + minutes * 60.0 + secondPhase
        val far = near + 43200.0
        return if (distanceAcrossDay(near, currentSeconds) <=
            distanceAcrossDay(far, currentSeconds)
        ) near else far
    }

    /**
     * The light magnetic settle: when the minute hand is released close to a
     * whole minute, it eases onto it, so one o'clock can be exactly one
     * o'clock. Anywhere else the time stays where the finger left it.
     */
    fun snapMinute(secondsOfDay: Double, windowDegrees: Double = MinuteSnapWindowDegrees): Double {
        val minuteOfHour = (((secondsOfDay % 3600.0) + 3600.0) % 3600.0) / 60.0
        val nearest = round(minuteOfHour)
        val offDegrees = (minuteOfHour - nearest) * 6.0
        return if (abs(offDegrees) <= windowDegrees + 1e-9) {
            secondsOfDay + (nearest - minuteOfHour) * 60.0
        } else {
            secondsOfDay
        }
    }

    private fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    private fun normalizeSeconds(seconds: Double): Double = ((seconds % 60.0) + 60.0) % 60.0

    private fun distanceAcrossDay(a: Double, b: Double): Double {
        val raw = abs(a - b) % SecondsPerDay
        return minOf(raw, SecondsPerDay - raw)
    }
}
