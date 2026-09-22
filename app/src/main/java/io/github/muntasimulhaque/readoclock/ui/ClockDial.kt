package io.github.muntasimulhaque.readoclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import io.github.muntasimulhaque.readoclock.core.ClockFace
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dial, drawn from the same geometry and the same palette the offline
 * generators use, so what the owner approved in the take is what the app
 * shows.
 */
@Composable
fun ClockDial(
    hour: Double,
    minute: Double,
    second: Double,
    radius: Float,
    modifier: Modifier = Modifier,
) {
    val numerals = rememberNumeralLayouts(radius)
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCaseAndDial(center, radius)
        drawTicks(center, radius)
        drawNumerals(center, radius, numerals)
        drawHands(center, radius, hour, minute, second)
    }
}

private fun DrawScope.drawCaseAndDial(center: Offset, radius: Float) {
    // The clock floats a little off the wall: one soft pool, narrower than
    // the case so it never rings the sides, its dark gathered just below the
    // bottom rim and its lightest right under the rim, so the shade reads as
    // cast, not as a smudge touching the clock. It dies at 1.095 r because
    // on a short window the case nearly touches the screen and a hard clip
    // would show at the edge. The takes renderer draws the same pool from
    // the same numbers, so the app and the store art stay one look.
    val shadowCenter = Offset(center.x, center.y + radius * 0.93f)
    val reach = radius * 0.84f
    scale(scaleX = 1f, scaleY = 0.196f, pivot = shadowCenter) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0f, 0f, 0f, 8f / 255f),
                    0.50f to Color(0f, 0f, 0f, 10f / 255f),
                    0.78f to Color(0f, 0f, 0f, 26f / 255f),
                    1f to Color.Transparent,
                ),
                center = shadowCenter,
                radius = reach,
            ),
            radius = reach,
            center = shadowCenter,
        )
    }

    // The case: a radial light from above, so the rim has a top and a bottom.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to lerp(ClockColors.caseFill, Color.White, 0.12f),
                0.55f to ClockColors.caseFill,
                1f to lerp(ClockColors.caseFill, Color.Black, 0.16f),
            ),
            center = Offset(center.x, center.y - radius * 0.45f),
            radius = radius * 1.55f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = ClockColors.caseEdge.copy(alpha = 210f / 255f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.014f),
    )

    // The dial: enamel glow in the middle, a shade toward the rim, a faint
    // rim line, the recess shadow, and one slow glass sweep.
    val dialRadius = radius * ClockFace.DialRadius.toFloat()
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to lerp(ClockColors.dial, Color.White, 0.035f),
                0.70f to ClockColors.dial,
                1f to lerp(ClockColors.dial, Color.Black, 0.06f),
            ),
            center = Offset(center.x, center.y - dialRadius * 0.18f),
            radius = dialRadius * 1.32f,
        ),
        radius = dialRadius,
        center = center,
    )
    drawCircle(
        color = ClockColors.dialEdge.copy(alpha = 170f / 255f),
        radius = dialRadius,
        center = center,
        style = Stroke(width = radius * 0.006f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.90f to Color.Transparent,
                1f to Color(0f, 0f, 0f, 18f / 255f),
            ),
            center = center,
            radius = dialRadius,
        ),
        radius = dialRadius,
        center = center,
    )
    val dialPath = Path().apply { addOval(Rect(center, dialRadius)) }
    clipPath(dialPath) {
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.18f to Color.Transparent,
                    0.42f to Color(1f, 1f, 1f, 26f / 255f),
                    0.66f to Color.Transparent,
                ),
                start = Offset(center.x - dialRadius, center.y - dialRadius),
                end = Offset(center.x + dialRadius, center.y + dialRadius),
            ),
            topLeft = Offset(center.x - dialRadius, center.y - dialRadius),
            size = Size(dialRadius * 2f, dialRadius * 2f),
        )
    }
}

private fun DrawScope.drawTicks(center: Offset, radius: Float) {
    for (index in 0 until 60) {
        val five = index % 5 == 0
        val width = radius * (if (five) ClockFace.FiveTickWidth else ClockFace.MinuteTickWidth).toFloat()
        val inner = radius * (if (five) ClockFace.FiveTickInner else ClockFace.MinuteTickInner).toFloat()
        val outer = radius * ClockFace.TickOuter.toFloat()
        val angle = Math.toRadians(index * 6.0)
        val alongX = sin(angle).toFloat()
        val alongY = -cos(angle).toFloat()
        drawLine(
            color = ClockColors.ticks,
            start = Offset(center.x + alongX * inner, center.y + alongY * inner),
            end = Offset(center.x + alongX * outer, center.y + alongY * outer),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}
