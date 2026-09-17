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
    val width = 1024
    val height = 500
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    hints(g)

    // The case color is the ground, so the banner and the launcher tile are
    // the same object.
    g.color = Color(DialPalette.caseFill.toInt())
    g.fillRect(0, 0, width, height)

    // The dial on the left, exactly as the app hangs it: a real dial at
    // 10:10 with the second hand settled on its twelve.
    DialRenderer.drawClock(
        g,
        centerX = 214.0,
        centerY = height / 2.0,
        caseRadius = 174.0,
        skin = Skins.all.first(),
        secondsOfDay = 10 * 3600.0 + 10 * 60.0 + 0.5,
        numeralFont = numeralFont,
    )

    // The name on the right, in the dial's own cream, nothing behind it.
    val nameFont = numeralFont.deriveFont(Font.PLAIN, 76f)
    val layout = TextLayout("Read-o-Clock", nameFont, g.fontRenderContext)
    val bounds = layout.bounds
    g.color = Color(DialPalette.dial.toInt())
    val x = width - 72.0 - bounds.width
    val baseline = height / 2.0 - (bounds.y + bounds.height / 2.0)
    layout.draw(g, x.toFloat(), baseline.toFloat())

    g.dispose()
    ImageIO.write(image, "png", file)
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
