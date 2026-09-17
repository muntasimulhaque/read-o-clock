package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun aTouchWithinAFingerOfTheHandPicksIt() {
        // A finger pad beside the minute hand, not exactly on the baton.
        assertEquals(ClockHand.Minute, HandPick.nearest(0.6, 0.05, hourAngle = 180.0, minuteAngle = 90.0))
    }

    @Test
    fun aTouchOnTheDialAwayFromBothHandsPicksNothing() {
        assertNull(HandPick.nearest(0.6, 0.0, hourAngle = 0.0, minuteAngle = 180.0))
    }

    @Test
    fun aTouchBeyondAHandsTipPicksNothing() {
        assertNull(HandPick.nearest(0.0, -1.0, hourAngle = 0.0, minuteAngle = 180.0))
    }

    @Test
    fun theGrabRadiusCoversTheBatonAndAFinger() {
        assertEquals(0.083, HandPick.grabRadius(ClockHand.Minute), 1e-9)
        assertEquals(0.090, HandPick.grabRadius(ClockHand.Hour), 1e-9)
    }

    @Test
    fun distanceToAHandMeasuresToTheSegment() {
        // Point on the hour hand's line, past its tip.
        assertEquals(0.495, HandPick.distanceToHand(0.0, -1.0, 0.0, ClockFace.HourHandLength), 1e-9)
        // Point level with the hand's middle.
        assertEquals(0.2, HandPick.distanceToHand(0.2, -0.3, 0.0, ClockFace.HourHandLength), 1e-9)
    }
}
