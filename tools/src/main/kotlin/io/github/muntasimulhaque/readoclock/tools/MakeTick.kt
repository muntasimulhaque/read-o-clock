package io.github.muntasimulhaque.readoclock.tools

import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates the quartz tick: the little click the second hand makes as it
 * steps. A real quartz movement snaps twice inside one step, about ten
 * milliseconds apart, and each snap is a noise-rich impact rather than a
 * tone; this models both, so the app sounds like the clock on the wall.
 * Synthesized here so the app carries no third-party audio and the
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

// 44.1 kHz: the snap's leading edge lives above 10 kHz, and the clip is
// still under four kilobytes.
private const val SampleRate = 44100
private const val DurationSeconds = 0.036
private const val PeakAmplitude = 0.45
private const val FadeSeconds = 0.004
private const val TwoPi = 2.0 * PI

// The two snaps of one quartz step, and the weak third rattle the gear
// train leaves behind. Measured from recordings of real quartz clocks:
// the pair is ten to thirteen milliseconds apart and the second snap is
// nearly as loud as the first.
private const val SecondImpactSeconds = 0.0105
private const val SecondImpactAmplitude = 0.90
private const val SecondImpactScale = 0.94
private const val ThirdImpactSeconds = 0.021
private const val ThirdImpactAmplitude = 0.15
private const val ThirdImpactScale = 0.88

// One impact: a bright noise snap, a short noisy grain, damped partials
// for the case, and a low body underneath. The partials follow the
// spectrum of real quartz clocks, with each one detuned a little from its
// companion so the bank reads as an impact, not as a chord.
private const val SnapLevel = 0.95
private const val SnapTau = 0.00038
private const val GrainLevel = 0.50
private const val GrainTau = 0.0026
private const val BodyHz = 180.0
private const val BodyLevel = 0.12
private const val BodyTau = 0.008
private const val Detune = 1.021
private const val CompanionPhase = 1.3
private val PartialHz = doubleArrayOf(420.0, 780.0, 1250.0, 2100.0, 3300.0, 5000.0)
private val PartialLevel = doubleArrayOf(0.26, 0.40, 0.32, 0.24, 0.16, 0.09)
private val PartialTau = doubleArrayOf(0.0060, 0.0045, 0.0030, 0.0020, 0.0014, 0.0010)

private fun tickWav(): ByteArray {
    val count = (SampleRate * DurationSeconds).toInt()
    val raw = DoubleArray(count)
    val noise = Random(42L)
    addImpact(raw, noise, 0.0, 1.0, 1.0)
    addImpact(raw, noise, SecondImpactSeconds, SecondImpactAmplitude, SecondImpactScale)
    addImpact(raw, noise, ThirdImpactSeconds, ThirdImpactAmplitude, ThirdImpactScale)

    // The clip must end in silence: the next tick lands on this one.
    val fadeStart = count - (SampleRate * FadeSeconds).toInt()
    for (i in fadeStart until count) {
        raw[i] *= (count - i).toDouble() / (count - fadeStart)
    }

    val peak = raw.maxOf { abs(it) }
    val samples = ShortArray(raw.size) {
        (raw[it] / peak * PeakAmplitude * Short.MAX_VALUE).toInt().toShort()
    }
    return wavBytes(samples)
}

/**
 * Adds one impact to the buffer, starting after [startSeconds]. Each impact
 * draws its own noise, so the second snap is not a copy of the first.
 */
private fun addImpact(
    raw: DoubleArray,
    noise: Random,
    startSeconds: Double,
    amplitude: Double,
    scale: Double,
) {
    val start = (startSeconds * SampleRate).roundToInt()
    var previous = noise.nextDouble() * 2.0 - 1.0
    for (i in start until raw.size) {
        val t = (i - start).toDouble() / SampleRate
        val current = noise.nextDouble() * 2.0 - 1.0
        raw[i] += impact(t, amplitude, scale, current, previous)
        previous = current
    }
}

/**
 * A tick is an impact, not a tone: a sub-millisecond noise snap, a short
 * noisy grain, inharmonic case partials that ring for a few milliseconds,
 * and a low soft body underneath, all gone inside the clip. StrictMath,
 * because the byte-exact pin must hold on every JVM and platform.
 */
private fun impact(t: Double, amplitude: Double, scale: Double, noise: Double, previous: Double): Double {
    val snap = noise * StrictMath.exp(-t / SnapTau) * SnapLevel
    val grain = (noise - previous) * StrictMath.exp(-t / GrainTau) * GrainLevel
    var ring = 0.0
    for (i in PartialHz.indices) {
        val decay = StrictMath.exp(-t / PartialTau[i])
        ring += StrictMath.sin(TwoPi * PartialHz[i] * scale * t) * PartialLevel[i] * decay
        ring += StrictMath.sin(TwoPi * PartialHz[i] * scale * Detune * t + CompanionPhase) *
            PartialLevel[i] * 0.5 * decay
    }
    val body = StrictMath.sin(TwoPi * BodyHz * t) * BodyLevel * StrictMath.exp(-t / BodyTau)
    return amplitude * (snap + grain + ring + body)
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
