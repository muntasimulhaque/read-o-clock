package io.github.muntasimulhaque.readoclock.core

/**
 * How large the clock is on a given window: as large as the screen allows,
 * with a margin that keeps the case off the edge, on both tall and wide
 * windows. The numbers are window fractions of the case radius.
 */
object ClockLayout {
    fun caseRadius(width: Double, height: Double): Double =
        minOf(width * 0.44, height * 0.45)
}
