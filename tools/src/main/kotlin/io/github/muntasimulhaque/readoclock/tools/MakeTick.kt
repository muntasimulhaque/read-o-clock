package io.github.muntasimulhaque.readoclock.tools

import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs

/**
 * Generates the quartz tick: the little click the second hand makes as it
 * steps. Synthesized here so the app carries no third-party audio and the
 * committed WAV needs no license note. Every byte is arithmetic, so the
 * output is identical on every machine and `--check` can ban drift.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val out = File(root, "app/src/main/res/raw/tick.wav")
    val fresh = tickWav()

    if (args.contains("--check")) {
        val committed = if (out.exists()) out.readBytes() else null
        if (committed == null || !committed.contentEquals(fresh)) {
            println("tick drift: ${out.relativeTo(root)}")
            println("regenerate with :tools:makeTick")
            kotlin.system.exitProcess(1)
        }
        println("tick matches a fresh regeneration")
        return
    }

    out.parentFile.mkdirs()
    out.writeBytes(fresh)
    println("tick written: ${out.relativeTo(root)} (${fresh.size} bytes)")
}

// 22 kHz is plenty: the click lives under 6 kHz and the WAV stays tiny.
private const val SampleRate = 22050
private const val DurationSeconds = 0.032
private const val PeakAmplitude = 0.45
private const val TwoPi = 2.0 * PI

private fun tickWav(): ByteArray {
    val raw = DoubleArray((SampleRate * DurationSeconds).toInt())
    val noise = Random(42L)
    for (i in raw.indices) {
        raw[i] = tickSample(i.toDouble() / SampleRate, noise.nextDouble() * 2.0 - 1.0)
    }
    val peak = raw.maxOf { abs(it) }
    val samples = ShortArray(raw.size) {
        (raw[it] / peak * PeakAmplitude * Short.MAX_VALUE).toInt().toShort()
    }
    return wavBytes(samples)
}

/**
 * A tick is an impact, not a tone: a half millisecond noise snap, a few
 * inharmonic plastic partials that ring for a few milliseconds, and a low
 * soft body underneath, all gone inside the 32 ms clip. StrictMath, because
 * the byte-exact pin must hold on every JVM and platform.
 */
private fun tickSample(t: Double, noise: Double): Double {
    val snap = noise * StrictMath.exp(-t / 0.0005)
    val ring = (
        StrictMath.sin(TwoPi * 1800.0 * t) * 0.55 +
            StrictMath.sin(TwoPi * 2600.0 * t) * 0.40 +
            StrictMath.sin(TwoPi * 3800.0 * t) * 0.28 +
            StrictMath.sin(TwoPi * 5200.0 * t) * 0.16
        ) * StrictMath.exp(-t / 0.003)
    val body = StrictMath.sin(TwoPi * 420.0 * t) * 0.22 * StrictMath.exp(-t / 0.010)
    return snap + ring + body
}

/** A 16 bit mono PCM WAV, written by hand so no encoder can vary it. */
private fun wavBytes(samples: ShortArray): ByteArray {
    val dataBytes = samples.size * 2
    val out = ByteArray(44 + dataBytes)
    ascii(out, 0, "RIFF")
    putInt(out, 4, 36 + dataBytes)
    ascii(out, 8, "WAVE")
    ascii(out, 12, "fmt ")
    putInt(out, 16, 16)
    putShort(out, 20, 1)
    putShort(out, 22, 1)
    putInt(out, 24, SampleRate)
    putInt(out, 28, SampleRate * 2)
    putShort(out, 32, 2)
    putShort(out, 34, 16)
    ascii(out, 36, "data")
    putInt(out, 40, dataBytes)
    for (i in samples.indices) {
        putShort(out, 44 + i * 2, samples[i].toInt())
    }
    return out
}

private fun ascii(out: ByteArray, offset: Int, text: String) {
    for (i in text.indices) {
        out[offset + i] = text[i].code.toByte()
    }
}

private fun putInt(out: ByteArray, offset: Int, value: Int) {
    out[offset] = (value and 0xFF).toByte()
    out[offset + 1] = (value ushr 8 and 0xFF).toByte()
    out[offset + 2] = (value ushr 16 and 0xFF).toByte()
    out[offset + 3] = (value ushr 24 and 0xFF).toByte()
}

private fun putShort(out: ByteArray, offset: Int, value: Int) {
    out[offset] = (value and 0xFF).toByte()
    out[offset + 1] = (value ushr 8 and 0xFF).toByte()
}
