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
 * them follow. The second hand is part of the same gear train, so turning
 * the hands turns the seconds too: on a real clock there is no way to hold
 * the minute hand on the twelve while the second hand sits at forty five,
 * because the minute hand's position is the minutes plus the seconds.
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
     * The time that puts the minute hand under the finger. The minute hand's
     * angle is the whole time within the hour, seconds included, so solving
     * for it solves for the whole gear train; the 12 hour ambiguity resolves
     * to whichever side of the day was nearer.
     */
    fun timeFromMinuteAngle(currentSeconds: Double, angle: Double): Double {
        val secondsWithinHour = normalize(angle) * 10.0
        val hour = round((currentSeconds - secondsWithinHour) / 3600.0)
        return secondsWithinHour + hour * 3600.0
    }

    /**
     * The time that puts the hour hand under the finger. The hour hand's
     * angle carries the minutes and seconds inside it, so the other hands
     * follow, and the 12 hour ambiguity resolves to whichever half of the
     * day was nearer.
     */
    fun timeFromHourAngle(currentSeconds: Double, angle: Double): Double {
        val secondsWithinHalfDay = normalize(angle) * 120.0
        val half = round((currentSeconds - secondsWithinHalfDay) / 43200.0)
        return secondsWithinHalfDay + half * 43200.0
    }

    /**
     * The light magnetic settle: when the hands are released close to a whole
     * minute, they ease onto it, so one o'clock can be exactly one o'clock
     * with the second hand on its twelve too. Anywhere else the time stays
     * where the finger left it.
     */
    fun snapMinute(secondsOfDay: Double, windowDegrees: Double = MinuteSnapWindowDegrees): Double {
        val nearest = round(secondsOfDay / 60.0) * 60.0
        val offDegrees = (secondsOfDay - nearest) / 10.0
        return if (abs(offDegrees) <= windowDegrees + 1e-9) nearest else secondsOfDay
    }

    private fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
}
