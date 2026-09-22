package io.github.muntasimulhaque.readoclock.tools

import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renders the dial design takes into build/takes, for the owner to point at.
 * Scratch only, never committed: the chosen look is then encoded in the app's
 * theme, and the store art keeps drawing from it.
 *
 * Each take is rendered twice: a phone screen at 10:10, hands open, to judge
 * the whole look; and a close dial at 1:00, the first lesson, to judge
 * numerals, ticks and hands at size.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val fontFile = File(root, "app/src/main/res/font/baloo2_extrabold.ttf")
    require(fontFile.exists()) { "missing numeral font: $fontFile" }
    val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
    val outDir = File(root, "build/takes").apply { mkdirs() }

    val openTime = 10 * 3600.0 + 10 * 60.0 + 0.5 // ten past ten, hands open
    val lessonTime = 3600.0 // one o'clock exactly, the first lesson

    for (skin in Skins.all) {
        writeScreen(File(outDir, "${skin.id}-screen.png"), skin, font, openTime)
        writeDial(File(outDir, "${skin.id}-dial.png"), skin, font, lessonTime)
        println("${skin.name}: ${skin.id}-screen.png, ${skin.id}-dial.png")
    }
    println("takes written to ${outDir.absolutePath}")
}

private fun newGraphics(width: Int, height: Int, fieldColor: java.awt.Color): Pair<BufferedImage, java.awt.Graphics2D> {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.color = fieldColor
    g.fillRect(0, 0, width, height)
    return image to g
}

private fun writeScreen(file: File, skin: Skin, font: Font, secondsOfDay: Double) {
    val width = 1080
    val height = 2340
    val (image, g) = newGraphics(width, height, skin.field)
    DialRenderer.drawClock(
        g,
        centerX = width / 2.0,
        centerY = height * 0.46,
        caseRadius = width * 0.435,
        skin = skin,
        secondsOfDay = secondsOfDay,
        numeralFont = font,
    )
    g.dispose()
    ImageIO.write(image, "png", file)
}

private fun writeDial(file: File, skin: Skin, font: Font, secondsOfDay: Double) {
    val size = 1400
    val (image, g) = newGraphics(size, size, skin.field)
    DialRenderer.drawClock(
        g,
        centerX = size / 2.0,
        centerY = size / 2.0,
        // 0.44, not 0.47: the case must keep the same margin the app gives
        // it, or the floating shadow runs off the bottom of the take.
        caseRadius = size * 0.44,
        skin = skin,
        secondsOfDay = secondsOfDay,
        numeralFont = font,
    )
    g.dispose()
    ImageIO.write(image, "png", file)
}
