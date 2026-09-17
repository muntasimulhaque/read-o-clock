package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HandPickTest {

    @Test
    fun aFingerOnTheMinuteHandPicksTheMinuteHand() {
        // Minute hand at 3 (90 degrees), touch along it.
        assertEquals(ClockHand.Minute, HandPick.nearest(0.6, 0.0, hourAngle = 180.0, minuteAngle = 90.0))
    }

    @Test
    fun aFingerOnTheHourHandPicksTheHourHand() {
        // Hour hand at 3 (90 degrees), touch along it, minute hand opposite.
        assertEquals(ClockHand.Hour, HandPick.nearest(0.4, 0.0, hourAngle = 90.0, minuteAngle = 270.0))
    }

    @Test
    fun aTieGoesToTheMinuteHand() {
        assertEquals(ClockHand.Minute, HandPick.nearest(0.0, 0.0, hourAngle = 0.0, minuteAngle = 0.0))
    }

    @Test
    fun aTouchBeyondATipStillCountsAsThatHand() {
        // Well beyond the hour hand's tip, still along its direction.
        assertEquals(ClockHand.Hour, HandPick.nearest(0.0, -1.0, hourAngle = 0.0, minuteAngle = 180.0))
    }

    @Test
    fun distanceToAHandMeasuresToTheSegment() {
        // Point on the hour hand's line, past its tip.
        assertEquals(0.495, HandPick.distanceToHand(0.0, -1.0, 0.0, ClockFace.HourHandLength), 1e-9)
        // Point level with the hand's middle.
        assertEquals(0.2, HandPick.distanceToHand(0.2, -0.3, 0.0, ClockFace.HourHandLength), 1e-9)
    }
}
