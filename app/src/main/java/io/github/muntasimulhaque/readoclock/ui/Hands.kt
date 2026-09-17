package io.github.muntasimulhaque.readoclock.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.muntasimulhaque.readoclock.core.ClockFace

/** The three hands, drawn the way the takes draw them. */
fun DrawScope.drawHands(
    center: Offset,
    radius: Float,
    hour: Double,
    minute: Double,
    second: Double,
) {
    drawBaton(
        center, radius, hour,
        ClockFace.HourHandLength, ClockFace.HourHandTail, ClockFace.HourHandWidth,
        ClockColors.hands,
    )
    drawBaton(
        center, radius, minute,
        ClockFace.MinuteHandLength, ClockFace.MinuteHandTail, ClockFace.MinuteHandWidth,
        ClockColors.hands,
    )
    drawSecond(center, radius, second)
    drawCircle(ClockColors.hands, radius = radius * ClockFace.CenterCap.toFloat(), center = center)
    drawCircle(ClockColors.secondHand, radius = radius * ClockFace.CenterPin.toFloat(), center = center)
}

private fun DrawScope.drawBaton(
    center: Offset,
    radius: Float,
    angleDegrees: Double,
    lengthFraction: Double,
    tailFraction: Double,
    widthFraction: Double,
    color: Color,
) {
    rotate(degrees = angleDegrees.toFloat(), pivot = center) {
        val length = radius * lengthFraction.toFloat()
        val tail = radius * tailFraction.toFloat()
        val baseWidth = radius * widthFraction.toFloat()
        val tipWidth = baseWidth * ClockFace.TipTaper.toFloat()
        val shoulder = length - tipWidth / 2f
        val path = Path().apply {
            moveTo(center.x - baseWidth / 2f, center.y + tail)
            lineTo(center.x + baseWidth / 2f, center.y + tail)
            lineTo(center.x + baseWidth / 2f, center.y)
            lineTo(center.x + tipWidth / 2f, center.y - shoulder)
            lineTo(center.x - tipWidth / 2f, center.y - shoulder)
            lineTo(center.x - baseWidth / 2f, center.y)
            close()
        }
        // The tip is filled separately: appending an oval to the polygon can
        // cancel it under the non-zero winding rule and leave a notch.
        drawPath(path, color)
        drawCircle(color, radius = tipWidth / 2f, center = Offset(center.x, center.y - shoulder))
    }
}

private fun DrawScope.drawSecond(center: Offset, radius: Float, angleDegrees: Double) {
    rotate(degrees = angleDegrees.toFloat(), pivot = center) {
        val length = radius * ClockFace.SecondHandLength.toFloat()
        val tail = radius * ClockFace.SecondHandTail.toFloat()
        val weight = radius * ClockFace.SecondHandWeight.toFloat()
        drawLine(
            color = ClockColors.secondHand,
            start = Offset(center.x, center.y + tail - weight),
            end = Offset(center.x, center.y - length),
            strokeWidth = radius * ClockFace.SecondHandWidth.toFloat(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = ClockColors.secondHand,
            radius = weight,
            center = Offset(center.x, center.y + tail - weight),
        )
    }
}
