package io.github.muntasimulhaque.readoclock.tools

import io.github.muntasimulhaque.readoclock.core.DialPalette
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private const val FeatureWidth = 1024
private const val FeatureHeight = 500

// Play cuts the outer fifteen percent of a feature graphic in some
// formats. The case color is the ground and may reach the edges;
// the clock and the name are key content, so they are laid out to
// stay inside the middle seventy percent, with this much air.
private const val FeatureCutoff = 0.15
private const val FeatureMargin = 12.0

// The composition before it is scaled to fit the safe area.
private const val BaseClockCenterX = 214.0
private const val BaseCaseRadius = 174.0
private const val BaseNameSize = 76f
private const val BaseGap = 64.0
private const val StoreName = "Read-o-Clock"

/**
 * The store art: the feature graphic and the 512 listing icon, drawn from the
 * same dial, the same mark and the same palette the app shows, so the banner,
 * the icon and the screen are one sentence. Regenerate with :tools:makeArt;
 * never hand edit the PNGs.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val fontFile = File(root, "app/src/main/res/font/baloo2_extrabold.ttf")
    require(fontFile.exists()) { "missing numeral font: $fontFile" }
    val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
    val out = File(root, "play-store").apply { mkdirs() }
    writeFeatureGraphic(File(out, "feature-graphic-1024x500.png"), font)
    writePlayIcon(File(out, "play-icon-512.png"))
    println("store art written to ${out.absolutePath}")
}

private fun writeFeatureGraphic(file: File, numeralFont: Font) {
    val image = BufferedImage(FeatureWidth, FeatureHeight, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    hints(g)

    // The case color is the ground, so the banner and the launcher
    // tile are the same object.
    g.color = Color(DialPalette.caseFill.toInt())
    g.fillRect(0, 0, FeatureWidth, FeatureHeight)

    val layout = featureGraphicLayout(g, numeralFont)
    DialRenderer.drawClock(
        g,
        centerX = layout.clockCenterX,
        centerY = layout.clockCenterY,
        caseRadius = layout.caseRadius,
        skin = Skins.all.first(),
        secondsOfDay = 10 * 3600.0 + 10 * 60.0 + 0.5,
        numeralFont = numeralFont,
    )
    drawName(g, layout)

    g.dispose()
    ImageIO.write(image, "png", file)
}

private data class FeatureGraphicLayout(
    val clockCenterX: Double,
    val clockCenterY: Double,
    val caseRadius: Double,
    val nameFont: Font,
    val nameX: Double,
    val nameBaseline: Double,
)

private fun featureGraphicLayout(g: Graphics2D, numeralFont: Font): FeatureGraphicLayout {
    val baseClockY = FeatureHeight / 2.0
    val baseName = nameLayout(numeralFont, BaseNameSize, g)
    val baseNameX = BaseClockCenterX + BaseCaseRadius + BaseGap - baseName.bounds.x
    val baseNameBaseline = baseClockY - (baseName.bounds.y + baseName.bounds.height / 2.0)

    // The composition's outer bounds, the shade included, before it
    // is scaled. The clock is a real dial at 10:10 with the second
    // hand settled on its twelve; the name sits in the dial's cream.
    val baseLeft = BaseClockCenterX - BaseCaseRadius * DialRenderer.ShadeReach
    val baseRight = baseNameX + baseName.bounds.x + baseName.bounds.width
    val baseTop = baseClockY + BaseCaseRadius * DialRenderer.ShadeDrop - BaseCaseRadius * DialRenderer.ShadeReach
    val baseBottom = baseClockY + BaseCaseRadius * DialRenderer.ShadeDrop + BaseCaseRadius * DialRenderer.ShadeReach

    val scale = featureGraphicScale(baseLeft, baseRight, baseTop, baseBottom)
    val centerX = FeatureWidth / 2.0
    val centerY = FeatureHeight / 2.0
    val baseCenterX = (baseLeft + baseRight) / 2.0
    val baseCenterY = (baseTop + baseBottom) / 2.0
    fun x(base: Double) = centerX + (base - baseCenterX) * scale
    fun y(base: Double) = centerY + (base - baseCenterY) * scale

    return FeatureGraphicLayout(
        clockCenterX = x(BaseClockCenterX),
        clockCenterY = y(baseClockY),
        caseRadius = BaseCaseRadius * scale,
        nameFont = numeralFont.deriveFont(Font.PLAIN, (BaseNameSize * scale).toFloat()),
        nameX = x(baseNameX),
        nameBaseline = y(baseNameBaseline),
    )
}

private fun featureGraphicScale(
    baseLeft: Double,
    baseRight: Double,
    baseTop: Double,
    baseBottom: Double,
): Double {
    val safeWidth = FeatureWidth * (1.0 - 2.0 * FeatureCutoff) - 2.0 * FeatureMargin
    val safeHeight = FeatureHeight * (1.0 - 2.0 * FeatureCutoff) - 2.0 * FeatureMargin
    return minOf(
        safeWidth / (baseRight - baseLeft),
        safeHeight / (baseBottom - baseTop),
    )
}

private fun nameLayout(numeralFont: Font, size: Float, g: Graphics2D): TextLayout {
    return TextLayout(StoreName, numeralFont.deriveFont(Font.PLAIN, size), g.fontRenderContext)
}

private fun drawName(g: Graphics2D, layout: FeatureGraphicLayout) {
    val name = TextLayout(StoreName, layout.nameFont, g.fontRenderContext)
    g.color = Color(DialPalette.dial.toInt())
    name.draw(g, layout.nameX.toFloat(), layout.nameBaseline.toFloat())
}

private fun writePlayIcon(file: File) {
    val size = 512
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    hints(g)
    // Full bleed and no pre-rounded corners: Play applies its own mask.
    IconMark.draw(g, size, IconMark.Mode.FullBleed)
    g.dispose()
    ImageIO.write(image, "png", file)
}

private fun hints(g: Graphics2D) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
}
