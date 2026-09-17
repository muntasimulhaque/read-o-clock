package io.github.muntasimulhaque.readoclock.core

import kotlin.math.floor

/**
 * The time in the words a person says it, for TalkBack. The tick is the
 * clock's only other sound, so this is the one place the app speaks words,
 * and it speaks the way a parent teaches: "half past three", "twenty five
 * minutes past three", "quarter to four". Seconds are not spoken; they are
 * not part of reading a clock.
 */
object SpokenTime {
    private val ones = listOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight",
        "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
        "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf(
        "", "", "twenty", "thirty", "forty", "fifty",
    )

    fun spoken(secondsOfDay: Double): String {
        val totalMinutes = floor(secondsOfDay / 60.0).toInt()
        val minute = ((totalMinutes % 60) + 60) % 60
        val hour24 = ((totalMinutes / 60) % 24 + 24) % 24
        val hour = if (hour24 % 12 == 0) 12 else hour24 % 12
        val nextHour = if (hour == 12) 1 else hour + 1
        return when {
            minute == 0 -> "${word(hour)} o'clock"
            minute == 15 -> "quarter past ${word(hour)}"
            minute == 30 -> "half past ${word(hour)}"
            minute == 45 -> "quarter to ${word(nextHour)}"
            minute < 30 -> "${minutesWord(minute)} past ${word(hour)}"
            else -> "${minutesWord(60 - minute)} to ${word(nextHour)}"
        }
    }

    private fun minutesWord(minutes: Int): String = when {
        minutes == 1 -> "one minute"
        minutes % 5 == 0 -> word(minutes)
        else -> "${word(minutes)} minutes"
    }

    /** One to fifty nine in words, the only range this app ever speaks. */
    private fun word(number: Int): String = when {
        number < 20 -> ones[number]
        number % 10 == 0 -> tens[number / 10]
        else -> "${tens[number / 10]} ${ones[number % 10]}"
    }
}
