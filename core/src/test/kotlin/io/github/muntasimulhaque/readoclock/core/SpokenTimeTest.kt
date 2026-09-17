package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTimeTest {

    private fun at(hour: Int, minute: Int, second: Int = 0): Double =
        hour * 3600.0 + minute * 60.0 + second

    @Test
    fun oClockHasItsOwnWords() {
        assertEquals("twelve o'clock", SpokenTime.spoken(at(0, 0)))
        assertEquals("one o'clock", SpokenTime.spoken(at(1, 0)))
        assertEquals("twelve o'clock", SpokenTime.spoken(at(12, 0)))
        // Seconds do not change what the clock reads.
        assertEquals("one o'clock", SpokenTime.spoken(at(1, 0, 30)))
    }

    @Test
    fun quartersAndHalvesSoundTheWayPeopleSayThem() {
        assertEquals("quarter past three", SpokenTime.spoken(at(3, 15)))
        assertEquals("half past three", SpokenTime.spoken(at(3, 30)))
        assertEquals("quarter to four", SpokenTime.spoken(at(3, 45)))
        assertEquals("quarter to one", SpokenTime.spoken(at(12, 45)))
    }

    @Test
    fun pastAndToCountTheMinutes() {
        assertEquals("five past three", SpokenTime.spoken(at(3, 5)))
        assertEquals("twenty three minutes past three", SpokenTime.spoken(at(3, 23)))
        assertEquals("thirteen minutes to four", SpokenTime.spoken(at(3, 47)))
        assertEquals("twenty five to one", SpokenTime.spoken(at(12, 35)))
        assertEquals("one minute past one", SpokenTime.spoken(at(1, 1)))
        assertEquals("one minute to two", SpokenTime.spoken(at(1, 59)))
    }

    @Test
    fun midnightAnnoysNobody() {
        assertEquals("ten past twelve", SpokenTime.spoken(at(0, 10)))
        assertEquals("ten to twelve", SpokenTime.spoken(at(23, 50)))
    }
}
