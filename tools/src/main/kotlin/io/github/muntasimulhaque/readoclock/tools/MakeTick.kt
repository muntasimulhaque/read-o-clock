package io.github.muntasimulhaque.readoclock.tools

import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generates the quartz tick: the little click the second hand makes as it
 * steps. A real quartz movement lands its step as a short mechanical
 * sentence, not as one click: the rotor hits, the gear train settles about
 * ten milliseconds later, and the case keeps ringing after both. This models
 * the whole sentence, so the app sounds like the clock on the wall.
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
// still under five kilobytes.
private const val SampleRate = 44100
private const val DurationSeconds = 0.052
private const val PeakAmplitude = 0.45
private const val FadeSeconds = 0.010
private const val TwoPi = 2.0 * PI

// The three lands of one step: the rotor's hit, the duller settle when the
// gear train catches up, and the weak rattle the case leaves behind.
// Measured from recordings of real quartz wall clocks: the settle arrives
// about ten milliseconds after the hit, and it is quieter than the first
// reading of the recording suggested. When it was nearly as loud as the
// hit, the tick read as a synthetic double click; the clock on the wall
// sounds like one knock and a small catch, not two knocks.
private val ImpactAt = doubleArrayOf(0.0, 0.0105, 0.0185)
private val ImpactAmp = doubleArrayOf(1.00, 0.50, 0.20)
private val ImpactEdge = doubleArrayOf(1.00, 0.60, 0.35)

// The case modes: a broad bank from the low thud to the sharp top, each
// with a companion detuned a little so the bank reads as an impact, not a
// chord. The taus run from sixteen milliseconds at the bottom to two at the
// top, so the tail stays warm for about a third of the clip and then lets
// go, the way an enamel case does.
private val ModeHz = doubleArrayOf(300.0, 620.0, 980.0, 1500.0, 2100.0, 2900.0, 4200.0, 6000.0)
private val ModeLevel = doubleArrayOf(0.09, 0.16, 0.28, 0.34, 0.26, 0.18, 0.12, 0.06)
private val ModeTau = doubleArrayOf(0.016, 0.013, 0.011, 0.008, 0.006, 0.0045, 0.0032, 0.0022)
private const val Detune = 1.013
private const val CompanionLevel = 0.55
private const val CompanionPhase = 1.3

// One impact: a bright noise snap, a short noisy grain, the case modes
// underneath, and a low body that gives the tick its weight on a speaker
// too small to reproduce the bottom of the bank.
private const val SnapTau = 0.00035
private const val GrainTau = 0.0022
private const val GrainLevel = 0.40
private const val BodyHz = 210.0
private const val BodyLevel = 0.10
private const val BodyTau = 0.014

private fun tickWav(): ByteArray {
    val count = (SampleRate * DurationSeconds).toInt()
    val raw = DoubleArray(count)
    val noise = Random(42L)
    for (i in ImpactAt.indices) {
        addImpact(raw, noise, ImpactAt[i], ImpactAmp[i], ImpactEdge[i])
    }

    // The clip must end in silence: the next tick lands on this one. The
    // fade is long enough that the case seems to die away, not to be cut.
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
 * draws its own noise, so no two lands of the step are a copy of each other.
 */
private fun addImpact(
    raw: DoubleArray,
    noise: Random,
    startSeconds: Double,
    amplitude: Double,
    edge: Double,
) {
    val start = (startSeconds * SampleRate).roundToInt()
    var previous = noise.nextDouble() * 2.0 - 1.0
    for (i in start until raw.size) {
        val t = (i - start).toDouble() / SampleRate
        val current = noise.nextDouble() * 2.0 - 1.0
        raw[i] += impact(t, amplitude, edge, current, previous)
        previous = current
    }
}

/**
 * A tick is an impact, not a tone: a sub-millisecond noise snap, a short
 * noisy grain, inharmonic case modes that ring for milliseconds to tens of
 * milliseconds, and a low soft body underneath, all gone inside the clip.
 * StrictMath, because the byte-exact pin must hold on every JVM and
 * platform.
 */
private fun impact(t: Double, amplitude: Double, edge: Double, noise: Double, previous: Double): Double {
    val snap = noise * StrictMath.exp(-t / SnapTau) * edge
    val grain = (noise - previous) * StrictMath.exp(-t / GrainTau) * GrainLevel
    var ring = 0.0
    for (i in ModeHz.indices) {
        val envelope = StrictMath.exp(-t / ModeTau[i]) * ModeLevel[i]
        ring += StrictMath.sin(TwoPi * ModeHz[i] * t) * envelope
        ring += StrictMath.sin(TwoPi * ModeHz[i] * Detune * t + CompanionPhase) *
            envelope * CompanionLevel
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
