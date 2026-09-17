package io.github.muntasimulhaque.readoclock.core

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class ClockHand { Hour, Minute }

/**
 * Which hand a finger grabbed. The point is given in case-radius units from
 * the dial center, y downward, and the answer is the hand whose segment the
 * point is nearest; a tie goes to the minute hand, because that is the hand
 * a parent sets. Any touch on the dial picks a hand, so a child's tap never
 * lands on nothing.
 */
object HandPick {
    fun nearest(pointX: Double, pointY: Double, hourAngle: Double, minuteAngle: Double): ClockHand {
        val toMinute = distanceToHand(pointX, pointY, minuteAngle, ClockFace.MinuteHandLength)
        val toHour = distanceToHand(pointX, pointY, hourAngle, ClockFace.HourHandLength)
        return if (toMinute <= toHour) ClockHand.Minute else ClockHand.Hour
    }

    /** Distance from the point to the hand's segment, center to tip. */
    fun distanceToHand(pointX: Double, pointY: Double, angleDegrees: Double, length: Double): Double {
        val radians = Math.toRadians(angleDegrees)
        val alongX = sin(radians)
        val alongY = -cos(radians)
        val projection = (pointX * alongX + pointY * alongY).coerceIn(0.0, length)
        return hypot(pointX - projection * alongX, pointY - projection * alongY)
    }
}
