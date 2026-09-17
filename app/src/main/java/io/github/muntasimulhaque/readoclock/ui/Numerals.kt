package io.github.muntasimulhaque.readoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.muntasimulhaque.readoclock.core.ClockFace
import kotlin.math.cos
import kotlin.math.sin

/**
 * The twelve numerals, measured once per size. The dial is graphics, so its
 * numerals do not follow the system font scale: they are sized to the clock,
 * and TalkBack carries the reading.
 */
@Composable
fun rememberNumeralLayouts(radius: Float): List<TextLayoutResult> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(radius, density, measurer) {
        val size = with(density) { (radius * ClockFace.NumeralHeight).toFloat().toSp() }
        (1..12).map { hour ->
            measurer.measure(
                text = AnnotatedString(hour.toString()),
                style = TextStyle(fontFamily = NumeralFont, fontSize = size),
            )
        }
    }
}

fun DrawScope.drawNumerals(center: Offset, radius: Float, layouts: List<TextLayoutResult>) {
    for (hour in 1..12) {
        val layout = layouts[hour - 1]
        val angle = Math.toRadians(hour * 30.0)
        val x = center.x + (sin(angle) * radius * ClockFace.NumeralRadius).toFloat()
        val y = center.y - (cos(angle) * radius * ClockFace.NumeralRadius).toFloat()
        val first = layout.getBoundingBox(0)
        val last = layout.getBoundingBox(layout.layoutInput.text.length - 1)
        val visualCenter = (first.top + last.bottom) / 2f
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(x - layout.size.width / 2f, y - visualCenter),
        )
    }
}
