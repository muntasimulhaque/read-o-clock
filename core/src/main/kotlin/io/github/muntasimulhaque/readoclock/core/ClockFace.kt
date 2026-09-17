package io.github.muntasimulhaque.readoclock.core

/**
 * The dial's proportions. Every length is a fraction of the case's outer
 * radius, measured from the dial center; every angle is degrees clockwise
 * from 12. The app and the offline generators in :tools read these same
 * numbers, so a take and the app can never disagree about geometry.
 *
 * The values follow a common quartz wall clock: the numeral ring sits inside
 * a ring of sixty ticks, the five minute ticks are longer and heavier, the
 * hour hand reaches a little past the numerals, the minute hand reaches just
 * inside the ticks, and the second hand is the longest with a small
 * counterweight behind the center.
 */
object ClockFace {
    const val CaseOuter = 1.0
    const val CaseRim = 0.055
    const val DialRadius = CaseOuter - CaseRim

    const val TickOuter = 0.885
    const val MinuteTickInner = 0.850
    const val FiveTickInner = 0.815
    const val MinuteTickWidth = 0.007
    const val FiveTickWidth = 0.016

    const val NumeralRadius = 0.705
    const val NumeralHeight = 0.115

    const val HourHandLength = 0.505
    const val HourHandWidth = 0.040
    const val HourHandTail = 0.055

    const val MinuteHandLength = 0.790
    const val MinuteHandWidth = 0.026
    const val MinuteHandTail = 0.070

    const val SecondHandLength = 0.855
    const val SecondHandWidth = 0.0075
    const val SecondHandTail = 0.155
    const val SecondHandWeight = 0.021

    const val CenterCap = 0.036
    const val CenterPin = 0.020

    /** Taper of a baton hand: the tip is this much of the base width. */
    const val TipTaper = 0.62
}
