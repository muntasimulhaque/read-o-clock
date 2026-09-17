package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTimeTest {

    @Test
    fun secondsOfDayReadsTheLocalClock() {
        assertEquals(0.0, ClockTime.secondsOfDay(0L, 0), 1e-9)
        assertEquals(1.0, ClockTime.secondsOfDay(1000L, 0), 1e-9)
        assertEquals(3661.5, ClockTime.secondsOfDay(3661500L, 0), 1e-9)
        // A negative epoch instant still lands inside the day.
        assertEquals(86_399.999, ClockTime.secondsOfDay(-1L, 0), 1e-6)
        // A zone offset shifts the reading.
        assertEquals(3600.0, ClockTime.secondsOfDay(0L, 3_600_000), 1e-9)
    }

    @Test
    fun hourHandMovesHalfADegreePerMinute() {
        assertEquals(0.0, ClockTime.hourAngleDegrees(0.0), 1e-9)
        assertEquals(30.0, ClockTime.hourAngleDegrees(3600.0), 1e-9)
        assertEquals(45.0, ClockTime.hourAngleDegrees(5400.0), 1e-9)
        assertEquals(180.0, ClockTime.hourAngleDegrees(21600.0), 1e-9)
        assertEquals(30.5, ClockTime.hourAngleDegrees(3660.0), 1e-9)
    }

    @Test
    fun minuteHandMovesSixDegreesPerMinute() {
        assertEquals(0.0, ClockTime.minuteAngleDegrees(0.0), 1e-9)
        assertEquals(60.0, ClockTime.minuteAngleDegrees(36_600.0), 1e-9)
        assertEquals(31.2, ClockTime.minuteAngleDegrees(312.0), 1e-9)
    }

    @Test
    fun secondHandMovesSixDegreesPerSecond() {
        assertEquals(0.0, ClockTime.secondAngleDegrees(0.0), 1e-9)
        assertEquals(270.0, ClockTime.secondAngleDegrees(45.0), 1e-9)
        assertEquals(357.0, ClockTime.secondAngleDegrees(59.5), 1e-9)
    }

    @Test
    fun settingTheMinuteHandSolvesTheWholeGearTrain() {
        // Pointing the minute hand at the twelve lands an exact hour, with
        // the second hand on its twelve too: the hands are one gear train.
        val set = ClockTime.timeFromMinuteAngle(43_170.0, 0.0)
        assertEquals(43_200.0, set, 1e-9) // 12:00:00
        assertEquals(0.0, set % 60.0, 1e-9)
    }

    @Test
    fun settingTheMinuteHandRollsTheHourAcrossTwelve() {
        // On the hour, pointing the minute hand at 12 stays on the hour.
        assertEquals(3600.0, ClockTime.timeFromMinuteAngle(3600.0, 0.0), 1e-9)
        // Five past one follows the gear.
        assertEquals(3900.0, ClockTime.timeFromMinuteAngle(3600.0, 30.0), 1e-9)
        // Pointing the minute hand at 12 from 11:59:30 rolls to 12:00:00.
        assertEquals(43_200.0, ClockTime.timeFromMinuteAngle(43_170.0, 0.0), 1e-9)
    }

    @Test
    fun settingTheMinuteHandCarriesTheHourHand() {
        val before = ClockTime.hourAngleDegrees(3600.0)
        val after = ClockTime.hourAngleDegrees(ClockTime.timeFromMinuteAngle(3600.0, 6.0))
        assertEquals(0.5, after - before, 1e-9)
    }

    @Test
    fun settingTheHourHandCarriesTheMinuteHand() {
        // Halfway between 12 and 1 is 12:30, minute hand at six.
        assertEquals(1800.0, ClockTime.timeFromHourAngle(3600.0, 15.0), 1e-9)
        // The same angle in the afternoon stays in the afternoon.
        assertEquals(46_800.0, ClockTime.timeFromHourAngle(46_800.0, 30.0), 1e-9)
        // One thirty under the finger.
        assertEquals(5400.0, ClockTime.timeFromHourAngle(3600.0, 45.0), 1e-9)
    }

    @Test
    fun theMagneticSettleEasesOntoWholeMinutes() {
        assertEquals(3660.0, ClockTime.snapMinute(3672.0), 1e-9) // 1:01:12 settles back
        assertEquals(3720.0, ClockTime.snapMinute(3696.0), 1e-9) // 1:01:36 settles forward
        assertEquals(3720.0, ClockTime.snapMinute(3708.0), 1e-9) // 1:01:48 settles forward
        assertEquals(3630.0, ClockTime.snapMinute(3630.0), 1e-9) // half a minute stays put
        // A whole minute settles to itself.
        assertEquals(4320.0, ClockTime.snapMinute(4320.0), 1e-9)
    }

    @Test
    fun theQuartzStepStartsAndEndsExactly() {
        assertEquals(0.0, ClockTime.quartzEase(0.0), 1e-9)
        assertEquals(1.0, ClockTime.quartzEase(1.0), 1e-9)
        // Out of range progress cannot move past the step.
        assertEquals(0.0, ClockTime.quartzEase(-1.0), 1e-9)
        assertEquals(1.0, ClockTime.quartzEase(2.0), 1e-9)
    }

    @Test
    fun theQuartzStepOvershootsThenSettles() {
        var peak = 0.0
        for (step in 0..200) {
            peak = maxOf(peak, ClockTime.quartzEase(step / 200.0))
        }
        assertTrue("the hand must overshoot, peak was $peak", peak > 1.0)
        assertTrue("the overshoot must stay quartz-sized, peak was $peak", peak < 1.10)
    }

    @Test
    fun theDisplayedSecondHandRidesTheBounce() {
        // At a whole second the hand still sits at the previous step.
        assertEquals(354.0, ClockTime.displayedSecondAngleDegrees(0.0), 1e-9)
        // The step ends exactly on the new second.
        assertEquals(60.0, ClockTime.displayedSecondAngleDegrees(10.16), 1e-9)
        // Mid bounce the hand is briefly past its mark.
        val mid = ClockTime.displayedSecondAngleDegrees(10.08)
        assertTrue("mid bounce was $mid", mid > 60.0 && mid < 63.0)
        // Between ticks the hand waits on its mark.
        assertEquals(60.0, ClockTime.displayedSecondAngleDegrees(10.9), 1e-9)
    }

    @Test
    fun angleOfAPointReadsClockwiseFromTwelve() {
        assertEquals(0.0, ClockTime.angleDegrees(0.0, -10.0, 0.0, 0.0), 1e-9)
        assertEquals(90.0, ClockTime.angleDegrees(10.0, 0.0, 0.0, 0.0), 1e-9)
        assertEquals(180.0, ClockTime.angleDegrees(0.0, 10.0, 0.0, 0.0), 1e-9)
        assertEquals(270.0, ClockTime.angleDegrees(-10.0, 0.0, 0.0, 0.0), 1e-9)
        assertEquals(45.0, ClockTime.angleDegrees(10.0, -10.0, 0.0, 0.0), 1e-9)
    }
}
