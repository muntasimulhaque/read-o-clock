package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockSetterTest {

    @Test
    fun aFreshClockReadsLiveTime() {
        val setter = ClockSetter()
        assertEquals(37_425.0, setter.reading(37_425.0), 1e-9)
    }

    @Test
    fun settingTheMinuteHandToTheTwelveLandsAnExactHour() {
        val setter = ClockSetter()
        val live = 10 * 3600.0 + 23 * 60.0 + 45.5
        setter.setFromMinuteAngle(live, 0.0)
        // The whole gear train lands on the hour: minute hand on twelve,
        // second hand on twelve, hour hand on ten.
        assertEquals(36000.0, setter.reading(live), 1e-9)
    }

    @Test
    fun theClockKeepsRunningFromTheTaughtTime() {
        val setter = ClockSetter()
        val live = 10 * 3600.0 + 23 * 60.0 + 45.5
        setter.setFromMinuteAngle(live, 0.0)
        val taught = setter.reading(live)
        assertEquals(taught + 10.0, setter.reading(live + 10.0), 1e-9)
        assertEquals(taught + 3600.0, setter.reading(live + 3600.0), 1e-9)
    }

    @Test
    fun theHourHandCarriesTheMinutes() {
        val setter = ClockSetter()
        val live = 3600.0 // one o'clock
        setter.setFromHourAngle(live, 15.0) // halfway between 12 and 1
        assertEquals(1800.0, setter.reading(live), 1e-9)
    }

    @Test
    fun theAfternoonStaysTheAfternoon() {
        val setter = ClockSetter()
        val live = 46_800.0 // one o'clock in the afternoon
        setter.setFromHourAngle(live, 30.0)
        assertEquals(46_800.0, setter.reading(live), 1e-9)
    }

    @Test
    fun movingTheHandsWrapsAcrossMidnight() {
        val setter = ClockSetter()
        val live = 23 * 3600.0 + 59 * 60.0 + 50.0
        setter.moveBy(live, 30.0)
        assertEquals(20.0, setter.reading(live), 1e-9)
    }
}
