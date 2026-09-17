package io.github.muntasimulhaque.readoclock.core

/**
 * The chosen dial, Schoolhouse enamel (docs/decisions.md D-006), as opaque
 * ARGB values. The app theme, the offline generators and the launcher icon
 * all read this one palette, so the store art, the icon and the app can
 * never drift apart.
 */
object DialPalette {
    val field = 0xFFE0DBD0
    val dial = 0xFFF7F2E6
    val dialEdge = 0xFFE6DECB
    val numerals = 0xFF26282C
    val ticks = 0xFF2E3136
    val hands = 0xFF202329
    val secondHand = 0xFFC6402F
    val caseFill = 0xFF322D27
    val caseEdge = 0xFF171410
}
