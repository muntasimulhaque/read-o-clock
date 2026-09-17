package io.github.muntasimulhaque.readoclock.tools

import io.github.muntasimulhaque.readoclock.core.DialPalette
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.cos
import kotlin.math.sin

/**
 * The launcher mark: the Schoolhouse dial in miniature. Flat fills and plain
 * paths only, no gradients, no strokes and no Area booleans, because the icon
 * bytes are pinned across machines and rasterizers (Puzzlet's lesson).
 *
 * The mark is sized in fractions of the icon canvas: the adaptive layers keep
 * a full-bleed disc inside the mask, and the legacy bitmaps carry their own
 * background, rounded square or circle, so pre-26 launchers see a finished
 * icon.
 */
object IconMark {

    enum class Mode { Foreground, Monochrome, LegacySquare, LegacyRound }

    private const val Canvas = 108.0
    private const val LegacyCanvas = 88.0

    private const val DialRadius = 33.0
    private const val RingThickness = 4.4
    private const val DotRing = 27.0
    private const val DotRadius = 1.9
    private const val HourLength = 15.5
    private const val HourWidth = 4.8
    private const val MinuteLength = 23.0
    private const val MinuteWidth = 2.8
    private const val SecondLength = 27.0
    private const val SecondWidth = 1.4
    private const val CenterRadius = 3.0

    private val dial = Color(DialPalette.dial.toInt())
    private val ink = Color(DialPalette.hands.toInt())
    private val red = Color(DialPalette.secondHand.toInt())
    private val dark = Color(DialPalette.caseFill.toInt())

    fun draw(g: Graphics2D, size: Int, mode: Mode) {
        val center = size / 2.0
        when (mode) {
            Mode.LegacySquare -> {
                val radius = size * 0.24
                g.color = dark
                g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), radius, radius))
            }
            Mode.LegacyRound -> {
                g.color = dark
                g.fill(Ellipse2D.Double(0.0, 0.0, size.toDouble(), size.toDouble()))
            }
            Mode.Foreground, Mode.Monochrome -> Unit
        }
        val scale = size / (if (mode == Mode.LegacySquare || mode == Mode.LegacyRound) LegacyCanvas else Canvas)
        when (mode) {
            Mode.Monochrome -> drawRing(g, center, scale)
            else -> {
                disc(g, center, DialRadius * scale, dial)
                dots(g, center, scale, ink)
            }
        }
        hand(g, center, scale, 305.0, HourLength, HourWidth, ink)
        hand(g, center, scale, 60.0, MinuteLength, MinuteWidth, ink)
        hand(g, center, scale, 0.0, SecondLength, SecondWidth, red)
        disc(g, center, CenterRadius * scale, ink)
    }

    /** A filled donut, one even-odd path: the themed icon's silhouette. */
    private fun drawRing(g: Graphics2D, center: Double, scale: Double) {
        val outer = DialRadius * scale
        val inner = (DialRadius - RingThickness) * scale
        val path = Path2D.Double()
        path.windingRule = Path2D.WIND_EVEN_ODD
        path.append(Ellipse2D.Double(center - outer, center - outer, outer * 2, outer * 2), false)
        path.append(Ellipse2D.Double(center - inner, center - inner, inner * 2, inner * 2), false)
        g.color = Color.BLACK
        g.fill(path)
        dots(g, center, scale, Color.BLACK)
        hand(g, center, scale, 305.0, HourLength, HourWidth, Color.BLACK)
        hand(g, center, scale, 60.0, MinuteLength, MinuteWidth, Color.BLACK)
        disc(g, center, CenterRadius * scale, Color.BLACK)
    }

    private fun dots(g: Graphics2D, center: Double, scale: Double, color: Color) {
        g.color = color
        val ring = DotRing * scale
        val radius = DotRadius * scale
        for (hour in 0 until 12) {
            val angle = Math.toRadians(hour * 30.0)
            val x = center + sin(angle) * ring
            val y = center - cos(angle) * ring
            g.fill(Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2))
        }
    }

    private fun hand(
        g: Graphics2D,
        center: Double,
        scale: Double,
        angleDegrees: Double,
        length: Double,
        width: Double,
        color: Color,
    ) {
        val len = length * scale
        val baseWidth = width * scale
        val tipWidth = baseWidth * 0.62
        val shoulder = len - tipWidth / 2
        val path = Path2D.Double()
        path.moveTo(-baseWidth / 2, 0.0)
        path.lineTo(baseWidth / 2, 0.0)
        path.lineTo(tipWidth / 2, -shoulder)
        path.lineTo(-tipWidth / 2, -shoulder)
        path.closePath()
        val tip = Ellipse2D.Double(-tipWidth / 2, -shoulder - tipWidth / 2, tipWidth, tipWidth)
        val transform = AffineTransform.getTranslateInstance(center, center)
        transform.rotate(Math.toRadians(angleDegrees))
        g.color = color
        g.fill(path.createTransformedShape(transform))
        g.fill(transform.createTransformedShape(tip))
    }

    private fun disc(g: Graphics2D, center: Double, radius: Double, color: Color) {
        g.color = color
        g.fill(Ellipse2D.Double(center - radius, center - radius, radius * 2, radius * 2))
    }
}
