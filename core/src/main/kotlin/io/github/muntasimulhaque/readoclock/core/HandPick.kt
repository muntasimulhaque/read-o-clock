package io.github.muntasimulhaque.readoclock.core

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class ClockHand { Hour, Minute }

/**
 * Which hand a finger grabbed. The point is given in case-radius units from
 * the dial center, y downward. A touch counts only when it lands on a hand,
 * the way it would on a real clock: a finger's pad reaches a little past the
 * hand's own width, and a touch on the dial itself picks nothing, so a drag
 * that begins on the case or off the clock cannot move the time. When the
 * hands are close together the nearer one wins; a tie goes to the minute
 * hand, the one a parent sets.
 */
object HandPick {
    /** How far past a hand's edge a touch still holds it, case-radius units. */
    const val FingerReach = 0.07

    /** The hand the finger holds, or null when the touch landed on the dial. */
    fun nearest(pointX: Double, pointY: Double, hourAngle: Double, minuteAngle: Double): ClockHand? {
        val toMinute = distanceToHand(pointX, pointY, minuteAngle, ClockFace.MinuteHandLength)
        val toHour = distanceToHand(pointX, pointY, hourAngle, ClockFace.HourHandLength)
        return when {
            toMinute <= grabRadius(ClockHand.Minute) && toMinute <= toHour -> ClockHand.Minute
            toHour <= grabRadius(ClockHand.Hour) -> ClockHand.Hour
            else -> null
        }
    }

    /** Distance from the point to the hand's segment, center to tip. */
    fun distanceToHand(pointX: Double, pointY: Double, angleDegrees: Double, length: Double): Double {
        val radians = Math.toRadians(angleDegrees)
        val alongX = sin(radians)
        val alongY = -cos(radians)
        val projection = (pointX * alongX + pointY * alongY).coerceIn(0.0, length)
        return hypot(pointX - projection * alongX, pointY - projection * alongY)
    }

    /** How far from the hand's centerline a touch still holds it. */
    fun grabRadius(hand: ClockHand): Double = when (hand) {
        ClockHand.Hour -> ClockFace.HourHandWidth / 2.0 + FingerReach
        ClockHand.Minute -> ClockFace.MinuteHandWidth / 2.0 + FingerReach
    }
}
