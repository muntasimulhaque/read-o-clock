package io.github.muntasimulhaque.readoclock.tools

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TwoPi = 2.0 * PI
private const val CompanionPhase = 1.3

/**
 * The quartz tick: the little mechanical sentence the movement says as the
 * second hand steps. A real quartz wall clock does not click; it knocks
 * once, and the case answers.
 *
 * The shape comes from measuring real clocks. Two public domain recordings
 * (Wikimedia Commons, PDsounds: `LA2 kitchen clock.ogg` and
 * `Alarm clock ticking.ogg`) were put through the yardstick in
 * `TickMetrics`, used as a measuring stick and never bundled: no recorded
 * audio ships in the app.
 *
 * Three things separate a real tick from a synthetic click, and all three
 * are modeled here.
 *
 * First, the knock is mid-heavy. Its energy sits between 400 Hz and
 * 2.5 kHz, on a short low skirt, with only a little above 4 kHz. 0.4's
 * tick had 38 percent of its energy in the 400 Hz to 1 kHz band and 38
 * percent above 1 kHz, which is a tap on wood; the measured clocks put
 * 35 to 40 percent in each of those bands and almost nothing up top.
 *
 * Second, the case keeps ringing. The measured ticks still fluctuate at
 * a fifth to a half of their peak twenty to forty milliseconds in. 0.4's
 * tick was dead by twenty. The ring here is 3.5 times the impact in the
 * mix, with the dominant modes at 1.2 and 1.75 kHz lasting seventeen to
 * twenty milliseconds.
 *
 * Third, the front is ragged, but it is a scrape, not a drum hit: several
 * parts of the movement land inside the first two milliseconds, each with
 * its own noise. That is why the tick keeps some attack without the
 * synthetic single impulse sound.
 *
 * The piece is deterministic: the same variant is byte-identical on every
 * machine, so `:tools:checkTick` can ban drift. There are four variants
 * because one sample repeated once a second for a minute is heard as a
 * loop even when the sample itself is right; the player rotates them.
 */
object TickSynth {
    const val SampleRate = 44100
    const val Variants = 4
    const val DurationSeconds = 0.066

    /** The committed clips peak here, matching TickPlayer's headroom note. */
    const val ClipPeak = 0.45

    private const val FadeSeconds = 0.012

    /** How much of the noise stream each contact may read from. */
    private const val StreamLength = 4096

    /** How far apart the per-contact noise reads start, in samples. */
    private const val ContactStride = 1777

    /** The committed resource for one variant. */
    fun resourceName(variant: Int): String = "tick_$variant.wav"

    fun samples(variant: Int, shape: TickShape = TickShape()): ShortArray {
        val rng = Rng(7000L + variant * 131L)
        val voice = Voice(
            shape = shape,
            freqScale = 1.0 + 0.010 * rng.signed(),
            tauScale = 1.0 + 0.05 * rng.signed(),
            detune = shape.detune * (1.0 + 0.004 * rng.signed()),
            noise = Rng(42L + variant * 977L),
        )
        val count = (SampleRate * DurationSeconds).roundToInt()
        val base = DoubleArray(count) { voice.impact(it.toDouble() / SampleRate) }
        val raw = withRoom(base, shape.reflections)
        val fadeStart = count - (SampleRate * FadeSeconds).roundToInt()
        for (i in fadeStart until count) {
            raw[i] *= (count - i).toDouble() / (count - fadeStart)
        }
        return toPcm(raw)
    }

    /**
     * The room answers the knock: each reflection is a delayed copy of the
     * impact, low-passed and quiet, added to the case's own ring.
     */
    private fun withRoom(base: DoubleArray, reflections: Array<Reflection>): DoubleArray {
        val raw = base.copyOf()
        for (reflection in reflections) {
            val start = (reflection.atSeconds * SampleRate).roundToInt()
            if (start <= 0 || start >= raw.size) continue
            val a = 1.0 - StrictMath.exp(-TwoPi * reflection.dampingHz / SampleRate)
            var lowPassed = 0.0
            for (i in start until raw.size) {
                lowPassed += a * (base[i - start] - lowPassed)
                raw[i] += lowPassed * reflection.level
            }
        }
        return raw
    }

    /** Normalized to the clip peak, so the committed WAV always fits. */
    private fun toPcm(raw: DoubleArray): ShortArray {
        val peak = raw.maxOf { abs(it) }
        val scale = if (peak > 0.0) ClipPeak / peak else 0.0
        return ShortArray(raw.size) {
            (raw[it] * scale * Short.MAX_VALUE).roundToInt().coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * One impact: the movement's parts landing, each with its own noise, over
     * the case's ring and a short low body.
     */
    private class Voice(
        private val shape: TickShape,
        private val freqScale: Double,
        private val tauScale: Double,
        private val detune: Double,
        noise: Rng,
    ) {
        private val stream = DoubleArray(StreamLength * 3) { noise.signed() }
        private val reads = IntArray(shape.contactAt.size) { it * ContactStride % StreamLength }
        private val previous = DoubleArray(shape.contactAt.size)

        init {
            for (i in previous.indices) previous[i] = stream[StreamLength + reads[i]]
        }

        fun impact(t: Double): Double {
            val at = (t * SampleRate).roundToInt().coerceIn(0, StreamLength - 1)
            var value = 0.0
            for (i in shape.contactAt.indices) {
                val since = t - shape.contactAt[i]
                if (since < 0.0) continue
                // Each contact reads the noise stream from its own offset, so
                // the parts of the impact are not copies of one another.
                val current = stream[StreamLength + reads[i] + at]
                val envelope = StrictMath.exp(-since / (shape.snapTau * tauScale)) * shape.contactLevel[i]
                value += current * envelope + (current - previous[i]) * envelope * shape.grainLevel
            }
            for (i in shape.modeHz.indices) {
                val envelope = StrictMath.exp(-t / (shape.modeTau[i] * tauScale)) * shape.modeLevel[i]
                value += StrictMath.sin(TwoPi * shape.modeHz[i] * freqScale * t) * envelope
                value += StrictMath.sin(TwoPi * shape.modeHz[i] * freqScale * detune * t + CompanionPhase) *
                    envelope * CompanionLevel
            }
            value += StrictMath.sin(TwoPi * shape.bodyHz * t) * shape.bodyLevel *
                StrictMath.exp(-t / BodyTau)
            // Remember this sample for the difference term of the next one.
            for (i in reads.indices) {
                previous[i] = stream[StreamLength + reads[i] + at]
            }
            return value
        }
    }
}

private const val CompanionLevel = 0.5
private const val BodyTau = 0.008

/**
 * The character of one tick: what the piece is made of. Every field is a
 * number the tuning harness can move, so the sound is measured against the
 * real recordings instead of being guessed by ear.
 */
data class TickShape(
    /** When each part of the movement lands, in seconds from the knock. */
    val contactAt: DoubleArray = doubleArrayOf(0.0, 0.00012, 0.00028, 0.0005, 0.0008, 0.0012, 0.0018),
    val contactLevel: DoubleArray = doubleArrayOf(1.0, 0.72, 0.5, 0.36, 0.26, 0.18, 0.12),
    /** How fast one part lets go: a scrape a third of a millisecond long. */
    val snapTau: Double = 0.00030,
    val grainLevel: Double = 0.35,
    /** The case's modes: frequency, level, and how long each rings. */
    val modeHz: DoubleArray = doubleArrayOf(250.0, 430.0, 720.0, 1150.0, 1750.0, 2600.0, 3800.0, 5400.0, 7400.0),
    val modeLevel: DoubleArray = doubleArrayOf(0.35, 0.49, 0.70, 1.05, 1.19, 0.98, 0.63, 0.35, 0.175),
    val modeTau: DoubleArray = doubleArrayOf(0.034, 0.028, 0.023, 0.020, 0.018, 0.012, 0.007, 0.004, 0.0022),
    val detune: Double = 1.012,
    val bodyHz: Double = 170.0,
    val bodyLevel: Double = 0.05,
    val reflections: Array<Reflection> = arrayOf(
        Reflection(0.0068, 0.40, 1800.0),
        Reflection(0.014, 0.20, 1300.0),
        Reflection(0.026, 0.08, 950.0),
    ),
) {
    // Arrays in a data class compare by identity; the shape is only ever
    // constructed and read, never compared.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/** One delayed answer of the case: when it arrives, how loud, how dull. */
data class Reflection(val atSeconds: Double, val level: Double, val dampingHz: Double)

/**
 * The variant jitter: a small deterministic generator so a variant is
 * reproducible byte for byte on every machine. java.util.Random would do,
 * but carrying the arithmetic here keeps the pin independent of the JDK.
 */
class Rng(seed: Long) {
    private var state = seed.toInt()

    fun signed(): Double = next() * 2.0 - 1.0

    private fun next(): Double {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)) ushr 0).toLong().and(0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}
