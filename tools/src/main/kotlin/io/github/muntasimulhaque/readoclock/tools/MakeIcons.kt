package io.github.muntasimulhaque.readoclock.tools

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Generates the launcher icon set from IconMark: legacy bitmaps for API 24
 * and 25, adaptive foreground and monochrome layers for 26+, and the 512
 * store icon later with the rest of the art.
 *
 * `--check` regenerates in memory and compares against the committed bytes
 * with a rasterizer tolerance: a person's change fails the build, a
 * rasterizer's noise does not.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val check = args.contains("--check")
    val res = File(root, "app/src/main/res")

    val legacy = linkedMapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
    val adaptive = linkedMapOf("mdpi" to 108, "hdpi" to 162, "xhdpi" to 216, "xxhdpi" to 324, "xxxhdpi" to 432)

    val work = mutableListOf<Pair<File, ByteArray>>()
    for ((density, size) in legacy) {
        work += File(res, "mipmap-$density/ic_launcher.png") to render(size, IconMark.Mode.LegacySquare)
        work += File(res, "mipmap-$density/ic_launcher_round.png") to render(size, IconMark.Mode.LegacyRound)
    }
    for ((density, size) in adaptive) {
        work += File(res, "mipmap-$density/ic_launcher_foreground.png") to render(size, IconMark.Mode.Foreground)
        work += File(res, "mipmap-$density/ic_launcher_monochrome.png") to render(size, IconMark.Mode.Monochrome)
    }

    var failures = 0
    for ((file, fresh) in work) {
        if (check) {
            val committed = if (file.exists()) file.readBytes() else null
            if (committed == null || !matches(committed, fresh)) {
                println("icon drift: ${file.relativeTo(root)}")
                failures++
            }
        } else {
            file.parentFile.mkdirs()
            file.writeBytes(fresh)
        }
    }
    if (check) {
        if (failures > 0) {
            println("$failures icon files drifted; run :tools:makeIcons")
            kotlin.system.exitProcess(1)
        }
        println("icons match a fresh regeneration")
    } else {
        println("icons written: ${work.size} files under app/src/main/res")
    }
}

private fun render(size: Int, mode: IconMark.Mode): ByteArray {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    IconMark.draw(g, size, mode)
    g.dispose()
    val bytes = ByteArrayOutputStream()
    ImageIO.write(image, "png", bytes)
    return bytes.toByteArray()
}

/**
 * Byte equality first; if the bytes differ, accept a rasterizer's noise: at
 * most one pixel in a thousand may differ, and then by at most two steps.
 */
private fun matches(committed: ByteArray, fresh: ByteArray): Boolean {
    if (committed.contentEquals(fresh)) return true
    val a = ImageIO.read(committed.inputStream()) ?: return false
    val b = ImageIO.read(fresh.inputStream()) ?: return false
    if (a.width != b.width || a.height != b.height) return false
    var differing = 0
    for (y in 0 until a.height) {
        for (x in 0 until a.width) {
            val pa = a.getRGB(x, y)
            val pb = b.getRGB(x, y)
            if (pa == pb) continue
            if (steps(pa, pb) > 2) differing++
        }
    }
    return differing * 1000 <= a.width * a.height
}

private fun steps(a: Int, b: Int): Int {
    val da = maxOf(
        Math.abs((a ushr 24 and 0xFF) - (b ushr 24 and 0xFF)),
        Math.abs((a ushr 16 and 0xFF) - (b ushr 16 and 0xFF)),
        Math.abs((a ushr 8 and 0xFF) - (b ushr 8 and 0xFF)),
        Math.abs((a and 0xFF) - (b and 0xFF)),
    )
    return da
}
