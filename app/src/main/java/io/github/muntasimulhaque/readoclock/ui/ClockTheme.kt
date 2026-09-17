package io.github.muntasimulhaque.readoclock.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import io.github.muntasimulhaque.readoclock.R
import io.github.muntasimulhaque.readoclock.core.DialPalette

/** Schoolhouse enamel (docs/decisions.md D-006), from the one palette in core. */
object ClockColors {
    val field = Color(DialPalette.field)
    val dial = Color(DialPalette.dial)
    val dialEdge = Color(DialPalette.dialEdge)
    val numerals = Color(DialPalette.numerals)
    val ticks = Color(DialPalette.ticks)
    val hands = Color(DialPalette.hands)
    val secondHand = Color(DialPalette.secondHand)
    val caseFill = Color(DialPalette.caseFill)
    val caseEdge = Color(DialPalette.caseEdge)
}

/** The numeral face: Baloo 2 ExtraBold, bundled and OFL. */
val NumeralFont = FontFamily(Font(R.font.baloo2_extrabold))
