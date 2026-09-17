package io.github.muntasimulhaque.readoclock.tools

import io.github.muntasimulhaque.readoclock.core.ClockFace
import io.github.muntasimulhaque.readoclock.core.ClockTime
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RadialGradientPaint
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.font.TextLayout
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dial, drawn with Java2D from the same geometry and the same laws the
 * app uses on the device. This renderer serves the design takes, and later it
 * draws the launcher icon and the store art, so what the owner approves here
 * is exactly what the app shows.
 */
object DialRenderer {

    fun drawClock(
        g: Graphics2D,
        centerX: Double,
        centerY: Double,
        caseRadius: Double,
        skin: Skin,
        secondsOfDay: Double,
        numeralFont: Font,
    ) {
        drawShadow(g, centerX, centerY, caseRadius)
        drawCase(g, centerX, centerY, caseRadius, skin)
        drawDial(g, centerX, centerY, caseRadius, skin)
        drawTicks(g, centerX, centerY, caseRadius, skin)
        drawNumerals(g, centerX, centerY, caseRadius, skin, numeralFont)
        drawHands(g, centerX, centerY, caseRadius, skin, secondsOfDay)
    }

    private fun drawShadow(g: Graphics2D, cx: Double, cy: Double, r: Double) {
        // The clock hangs in light from above: a broad pool of shade under
        // the case, and a tighter, darker pool right under the bottom rim.
        // Neither may reach past 1.10 r, because on a short window the case
        // nearly touches the screen and a hard clip would show at the edge.
        val haloCenter = Point2D.Double(cx, cy + r * 0.05)
        val haloRadius = r * 1.05
        g.paint = RadialGradientPaint(
            haloCenter,
            haloRadius.toFloat(),
            floatArrayOf(0.80f, 1.0f),
            arrayOf(Color(0, 0, 0, 40), Color(0, 0, 0, 0)),
        )
        g.fill(Ellipse2D.Double(haloCenter.x - haloRadius, haloCenter.y - haloRadius, haloRadius * 2, haloRadius * 2))

        val previous = g.transform
        g.translate(cx, cy + r * 0.95)
        g.scale(1.0, 0.167)
        val contactRadius = r * 0.90
        g.paint = RadialGradientPaint(
            Point2D.Double(0.0, 0.0),
            contactRadius.toFloat(),
            floatArrayOf(0.0f, 1.0f),
            arrayOf(Color(0, 0, 0, 34), Color(0, 0, 0, 0)),
        )
        g.fill(Ellipse2D.Double(-contactRadius, -contactRadius, contactRadius * 2, contactRadius * 2))
        g.transform = previous
    }

    private fun drawCase(g: Graphics2D, cx: Double, cy: Double, r: Double, skin: Skin) {
        val circle = Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2)
        g.paint = RadialGradientPaint(
            Point2D.Double(cx, cy - r * 0.45),
            (r * 1.55).toFloat(),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            arrayOf(lighten(skin.caseFill, 0.12), skin.caseFill, darken(skin.caseFill, 0.16)),
        )
        g.fill(circle)
        g.stroke = BasicStroke((r * 0.014).toFloat())
        g.color = withAlpha(skin.caseEdge, 210)
        g.draw(circle)
    }

    private fun drawDial(g: Graphics2D, cx: Double, cy: Double, r: Double, skin: Skin) {
        val dr = r * ClockFace.DialRadius
        val circle = Ellipse2D.Double(cx - dr, cy - dr, dr * 2, dr * 2)
        g.paint = RadialGradientPaint(
            Point2D.Double(cx, cy - dr * 0.18),
            (dr * 1.32).toFloat(),
            floatArrayOf(0.0f, 0.70f, 1.0f),
            arrayOf(lighten(skin.dial, 0.035), skin.dial, darken(skin.dial, 0.06)),
        )
        g.fill(circle)
        g.stroke = BasicStroke((r * 0.006).toFloat())
        g.color = withAlpha(skin.dialEdge, 170)
        g.draw(circle)

        // The dial sits recessed under the case: a faint shadow gathers at
        // its rim, which is what gives the face its dish.
        g.paint = RadialGradientPaint(
            Point2D.Double(cx, cy),
            dr.toFloat(),
            floatArrayOf(0.90f, 1.0f),
            arrayOf(Color(0, 0, 0, 0), Color(0, 0, 0, 18)),
        )
        g.fill(circle)

        val previousClip = g.clip
        g.clip = circle
        g.paint = LinearGradientPaint(
            Point2D.Double(cx - dr, cy - dr),
            Point2D.Double(cx + dr, cy + dr),
            floatArrayOf(0.18f, 0.42f, 0.66f),
            arrayOf(Color(255, 255, 255, 0), Color(255, 255, 255, 26), Color(255, 255, 255, 0)),
        )
        g.fill(Rectangle2D.Double(cx - dr, cy - dr, dr * 2, dr * 2))
        g.clip = previousClip
    }

    private fun drawTicks(g: Graphics2D, cx: Double, cy: Double, r: Double, skin: Skin) {
        g.color = skin.ticks
        for (index in 0 until 60) {
            val five = index % 5 == 0
            val width = r * (if (five) ClockFace.FiveTickWidth else ClockFace.MinuteTickWidth)
            val inner = r * (if (five) ClockFace.FiveTickInner else ClockFace.MinuteTickInner)
            val outer = r * ClockFace.TickOuter
            val angle = Math.toRadians(index * 6.0)
            val sx = sin(angle)
            val sy = -cos(angle)
            g.stroke = BasicStroke(width.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(Line2D.Double(cx + sx * inner, cy + sy * inner, cx + sx * outer, cy + sy * outer))
        }
    }

    private fun drawNumerals(
        g: Graphics2D,
        cx: Double,
        cy: Double,
        r: Double,
        skin: Skin,
        baseFont: Font,
    ) {
        val size = r * ClockFace.NumeralHeight
        val font = baseFont.deriveFont(Font.PLAIN, size.toFloat())
        val context = g.fontRenderContext
        for (hour in 1..12) {
            val angle = Math.toRadians(hour * 30.0)
            val x = cx + sin(angle) * r * ClockFace.NumeralRadius
            val y = cy - cos(angle) * r * ClockFace.NumeralRadius
            val layout = TextLayout(hour.toString(), font, context)
            val bounds = layout.bounds
            g.color = skin.numerals
            layout.draw(
                g,
                (x - bounds.width / 2 - bounds.x).toFloat(),
                (y - bounds.height / 2 - bounds.y).toFloat(),
            )
        }
    }

    private fun drawHands(
        g: Graphics2D,
        cx: Double,
        cy: Double,
        r: Double,
        skin: Skin,
        secondsOfDay: Double,
    ) {
        drawBaton(
            g, cx, cy, r,
            ClockTime.hourAngleDegrees(secondsOfDay),
            ClockFace.HourHandLength, ClockFace.HourHandTail, ClockFace.HourHandWidth,
            skin.hands,
        )
        drawBaton(
            g, cx, cy, r,
            ClockTime.minuteAngleDegrees(secondsOfDay),
            ClockFace.MinuteHandLength, ClockFace.MinuteHandTail, ClockFace.MinuteHandWidth,
            skin.hands,
        )
        drawSecond(
            g, cx, cy, r,
            ClockTime.displayedSecondAngleDegrees(secondsOfDay),
            skin.secondHand,
        )
        val cap = r * ClockFace.CenterCap
        val pin = r * ClockFace.CenterPin
        g.color = skin.hands
        g.fill(Ellipse2D.Double(cx - cap, cy - cap, cap * 2, cap * 2))
        g.color = skin.secondHand
        g.fill(Ellipse2D.Double(cx - pin, cy - pin, pin * 2, pin * 2))
    }

    private fun drawBaton(
        g: Graphics2D,
        cx: Double,
        cy: Double,
        r: Double,
        angleDegrees: Double,
        length: Double,
        tail: Double,
        width: Double,
        color: Color,
    ) {
        val len = r * length
        val back = r * tail
        val baseWidth = r * width
        val tipWidth = baseWidth * ClockFace.TipTaper
        val shoulder = len - tipWidth / 2
        val path = Path2D.Double()
        path.moveTo(-baseWidth / 2, back)
        path.lineTo(baseWidth / 2, back)
        path.lineTo(baseWidth / 2, 0.0)
        path.lineTo(tipWidth / 2, -shoulder)
        path.lineTo(-tipWidth / 2, -shoulder)
        path.lineTo(-baseWidth / 2, 0.0)
        path.closePath()
        val shape = Area(path)
        shape.add(Area(Ellipse2D.Double(-tipWidth / 2, -shoulder - tipWidth / 2, tipWidth, tipWidth)))
        val transform = AffineTransform.getTranslateInstance(cx, cy)
        transform.rotate(Math.toRadians(angleDegrees))
        g.color = color
        g.fill(shape.createTransformedArea(transform))
    }

    private fun drawSecond(
        g: Graphics2D,
        cx: Double,
        cy: Double,
        r: Double,
        angleDegrees: Double,
        color: Color,
    ) {
        val len = r * ClockFace.SecondHandLength
        val tail = r * ClockFace.SecondHandTail
        val width = r * ClockFace.SecondHandWidth
        val weight = r * ClockFace.SecondHandWeight
        val transform = AffineTransform.getTranslateInstance(cx, cy)
        transform.rotate(Math.toRadians(angleDegrees))
        val previous = g.transform
        g.transform = transform
        g.color = color
        g.stroke = BasicStroke(width.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Line2D.Double(0.0, tail - weight, 0.0, -len))
        g.fill(Ellipse2D.Double(-weight, tail - weight * 2, weight * 2, weight * 2))
        g.transform = previous
    }

    private fun lighten(color: Color, amount: Double): Color =
        Color(
            (color.red + (255 - color.red) * amount).toInt().coerceIn(0, 255),
            (color.green + (255 - color.green) * amount).toInt().coerceIn(0, 255),
            (color.blue + (255 - color.blue) * amount).toInt().coerceIn(0, 255),
        )

    private fun darken(color: Color, amount: Double): Color =
        Color(
            (color.red * (1 - amount)).toInt().coerceIn(0, 255),
            (color.green * (1 - amount)).toInt().coerceIn(0, 255),
            (color.blue * (1 - amount)).toInt().coerceIn(0, 255),
        )

    private fun withAlpha(color: Color, alpha: Int): Color =
        Color(color.red, color.green, color.blue, alpha)
}
