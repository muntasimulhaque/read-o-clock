package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TickScheduleTest {

    @Test
    fun ticksOncePerWholeSecond() {
        val schedule = TickSchedule()
        schedule.arm(10.0)
        assertFalse("the arming reading is not a step", schedule.ticked(10.2, dragging = false))
        assertFalse(schedule.ticked(10.9, dragging = false))
        assertTrue("crossing 11 ticks", schedule.ticked(11.0, dragging = false))
        assertFalse("the same second does not tick twice", schedule.ticked(11.4, dragging = false))
        assertTrue(schedule.ticked(12.05, dragging = false))
    }

    @Test
    fun aStallDoesNotStealTheTick() {
        // The main thread can miss a wake; the hand then steps over more than
        // one boundary at once. The movement struck once, so one tick is the
        // truthful answer, and silence is the bug this class exists to fix.
        val schedule = TickSchedule()
        schedule.arm(10.0)
        assertTrue("three seconds in one wake still ticks once", schedule.ticked(13.5, dragging = false))
        assertFalse("and only once", schedule.ticked(13.6, dragging = false))
    }

    @Test
    fun draggingIsSilent() {
        val schedule = TickSchedule()
        schedule.arm(10.0)
        assertFalse(schedule.ticked(11.0, dragging = true))
        assertFalse(schedule.ticked(12.0, dragging = true))
        // The drag turned the hands by hand; the running clock resumes after.
        assertTrue(schedule.ticked(13.0, dragging = false))
    }

    @Test
    fun aJumpBackwardsIsNotAStep() {
        val schedule = TickSchedule()
        schedule.arm(10.5)
        assertFalse("a TalkBack move back is not a quartz step", schedule.ticked(9.0, dragging = false))
        assertTrue(schedule.ticked(10.0, dragging = false))
    }

    @Test
    fun aReadingBeforeArmingIsSilent() {
        val schedule = TickSchedule()
        assertFalse("no arm, no tick", schedule.ticked(10.0, dragging = false))
        assertFalse(schedule.ticked(10.5, dragging = false))
        // Once the reading crosses a boundary after the first reading, it ticks.
        assertTrue(schedule.ticked(11.0, dragging = false))
    }

    @Test
    fun aJumpForwardDoesNotTick() {
        // A TalkBack move jumps the clock by a minute or an hour in one
        // frame, and the drag release may settle onto a whole minute. The
        // screen arms after each of those, so none of them is a step.
        val schedule = TickSchedule()
        schedule.arm(10.0)
        schedule.arm(3700.0)
        assertFalse(schedule.ticked(3700.5, dragging = false))
        assertTrue(schedule.ticked(3701.0, dragging = false))
    }

    @Test
    fun aTalkBackJumpOfOneMinuteDoesNotTick() {
        val schedule = TickSchedule()
        schedule.arm(100.0)
        // The action moves a minute, then the screen arms the new reading.
        schedule.arm(160.0)
        assertFalse("a 60 second move is not a step", schedule.ticked(160.4, dragging = false))
        assertTrue(schedule.ticked(161.0, dragging = false))
    }

    @Test
    fun midnightRollsOver() {
        val schedule = TickSchedule()
        schedule.arm(86_399.0)
        assertTrue(schedule.ticked(0.4, dragging = false))
        assertTrue(schedule.ticked(1.0, dragging = false))
    }
}
