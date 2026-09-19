package io.github.muntasimulhaque.readoclock.tools

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The numbers that decide whether a tick reads as the clock on the wall.
 *
 * They were established by measuring public domain recordings of real
 * quartz wall clocks (Wikimedia Commons, PDsounds): half of the tick's
 * energy sits between 1 and 2.5 kHz, about a third of the envelope's
 * energy is spent inside the first five milliseconds, and the case keeps
 * ringing for twenty to forty milliseconds. A synthesized click is usually
 * wrong in one of those three places at once, so the tuner prints all
 * three side by side and does not need an ear to catch a regression.
 */
object TickMetrics {

    /** Band edges in Hz; the printed shares follow them. */
    val BandEdges = intArrayOf(100, 400, 1000, 2500, 6000, 12000)

    private const val TwoPi = 2.0 * PI
    private const val HopMs = 0.25

    /**
     * The real-clock targets, measured with this same yardstick. Two
     * public domain recordings (Wikimedia Commons, PDsounds) of quartz
     * clocks: their band shares, how much of the envelope is spent in the
     * first five milliseconds, and how long the case keeps ringing.
     */
    const val TargetBands = "8/35/40/10/2"
    const val TargetHeadShare = 0.36
    const val TargetSustainMs = 36.0

    data class Report(
        val milliseconds: Double,
        val bandShares: DoubleArray,
        val headShare: Double,
        val sustainMs: Double,
        val firstLobes: List<Double>,
        val secondLobeLevel: Double,
    )

    fun report(samples: ShortArray, sampleRate: Int): Report {
        val window = aroundPeak(samples)
        val x = DoubleArray(window.size) { window[it] / 32768.0 }
        return Report(
            milliseconds = window.size.toDouble() / sampleRate * 1000.0,
            bandShares = bandShares(x, sampleRate),
            headShare = headShare(x, sampleRate),
            sustainMs = sustainMs(x, sampleRate),
            firstLobes = firstLobes(x, sampleRate),
            secondLobeLevel = secondLobeLevel(x, sampleRate),
        )
    }

    /**
     * The window the yardstick measures. A recording of a real clock holds
     * thousands of ticks and the committed clip holds exactly one, so both
     * are windowed the same way: a little before the loudest sample and
     * enough after it to watch the case die. Measuring one whole clip and a
     * slice of a recording would compare two different things.
     */
    private fun aroundPeak(samples: ShortArray): ShortArray {
        if (samples.size <= BeforePeak + AfterPeak) return samples
        var peak = 0
        for (i in samples.indices) {
            if (abs(samples[i].toInt()) > abs(samples[peak].toInt())) peak = i
        }
        val from = (peak - BeforePeak).coerceAtLeast(0)
        val to = (peak + AfterPeak).coerceAtMost(samples.size)
        return samples.copyOfRange(from, to)
    }

    /**
     * Band shares in percent, weighted per octave so a band reads as what
     * the ear hears rather than as raw bin power, over a Hann window.
     */
    private fun bandShares(x: DoubleArray, sampleRate: Int): DoubleArray {
        val bins = logBins(90.0, 12000.0)
        val window = hann(x.size)
        val shares = DoubleArray(BandEdges.size - 1)
        for (hz in bins) {
            val omega = TwoPi * hz / sampleRate
            var real = 0.0
            var imaginary = 0.0
            for (i in x.indices) {
                val sample = x[i] * window[i]
                real += sample * cos(omega * i)
                imaginary -= sample * sin(omega * i)
            }
            val power = (real * real + imaginary * imaginary) * hz
            for (band in shares.indices) {
                if (hz >= BandEdges[band] && hz < BandEdges[band + 1]) shares[band] += power
            }
        }
        val total = shares.sum().takeIf { it > 0.0 } ?: 1.0
        return DoubleArray(shares.size) { shares[it] / total * 100.0 }
    }

    /** Share of the envelope's energy spent in the first five milliseconds. */
    private fun headShare(x: DoubleArray, sampleRate: Int): Double {
        val envelope = envelope(x, sampleRate)
        val peak = envelope.peak()
        if (peak <= 0.0) return 0.0
        val peakIndex = envelope.indexOfPeak()
        var head = 0.0
        var all = 0.0
        for (i in peakIndex until envelope.size) {
            val value = envelope[i] / peak
            val timeMs = (i - peakIndex) * HopMs
            if (timeMs < 5.0) head += value * value
            all += value * value
        }
        return if (all > 0.0) head / all else 0.0
    }

    /** How long the case rings: the last moment the envelope is above 10 percent. */
    private fun sustainMs(x: DoubleArray, sampleRate: Int): Double {
        val envelope = envelope(x, sampleRate)
        val peak = envelope.peak()
        if (peak <= 0.0) return 0.0
        for (i in envelope.indices.reversed()) {
            if (envelope[i] > peak * 0.1) return i * HopMs
        }
        return 0.0
    }

    /** The ragged start: local maxima of the envelope in the first 1.25 ms. */
    private fun firstLobes(x: DoubleArray, sampleRate: Int): List<Double> {
        val envelope = envelope(x, sampleRate)
        val peak = envelope.peak()
        if (peak <= 0.0) return emptyList()
        val peakIndex = envelope.indexOfPeak()
        val lobes = mutableListOf<Double>()
        val from = (peakIndex - 4).coerceAtLeast(1)
        val to = (peakIndex + 13).coerceAtMost(envelope.size - 1)
        for (i in from until to) {
            val value = envelope[i] / peak
            if (value > 0.2 && value > envelope[i - 1] / peak && value >= envelope[i + 1] / peak) {
                lobes += (i - peakIndex) * HopMs
            }
        }
        return lobes
    }

    /** The level of the case's answer, five to fifteen milliseconds out. */
    private fun secondLobeLevel(x: DoubleArray, sampleRate: Int): Double {
        val envelope = envelope(x, sampleRate)
        val peak = envelope.peak()
        if (peak <= 0.0) return 0.0
        val peakIndex = envelope.indexOfPeak()
        val from = (peakIndex + (5.0 / HopMs).toInt()).coerceAtMost(envelope.size)
        val to = (peakIndex + (15.0 / HopMs).toInt()).coerceAtMost(envelope.size)
        if (from >= to) return 0.0
        var loudest = 0.0
        for (i in from until to) loudest = maxOf(loudest, envelope[i])
        return loudest / peak
    }

    /** Peak amplitude of the envelope, in 0.25 ms steps. */
    private fun envelope(x: DoubleArray, sampleRate: Int): DoubleArray {
        val hop = (sampleRate * HopMs / 1000.0).toInt().coerceAtLeast(1)
        val out = ArrayList<Double>(x.size / hop + 1)
        var i = 0
        while (i < x.size) {
            var peak = 0.0
            for (j in i until (i + hop).coerceAtMost(x.size)) {
                peak = maxOf(peak, abs(x[j]))
            }
            out += peak
            i += hop
        }
        return out.toDoubleArray()
    }

    private fun hann(size: Int) = DoubleArray(size) {
        0.5 - 0.5 * cos(TwoPi * it / size)
    }

    private fun logBins(from: Double, to: Double): List<Double> {
        val bins = ArrayList<Double>()
        var hz = from
        while (hz < to) {
            bins += hz
            hz *= 1.02
        }
        return bins
    }
}

private fun DoubleArray.peak(): Double {
    var loudest = 0.0
    for (value in this) loudest = maxOf(loudest, value)
    return loudest
}

private fun DoubleArray.indexOfPeak(): Int {
    var index = 0
    var loudest = -1.0
    for (i in indices) {
        if (this[i] > loudest) {
            loudest = this[i]
            index = i
        }
    }
    return index
}

private const val HopMs = 0.25

/** Samples kept before the peak of the window: 5 ms, to see the attack rise. */
private const val BeforePeak = 220

/** Samples kept after it: 44 ms, which is as long as any real tick rings. */
private const val AfterPeak = 1936

/** The first twenty milliseconds of the envelope, for the printed picture. */
fun envelopeSamples(samples: ShortArray, sampleRate: Int, milliseconds: Double): List<Int> {
    val hop = (sampleRate * HopMs / 1000.0).toInt().coerceAtLeast(1)
    val peak = samples.maxOf { abs(it.toInt()) }.takeIf { it > 0 } ?: 1
    val out = ArrayList<Int>()
    var i = 0
    while (i < samples.size && out.size * HopMs < milliseconds) {
        var window = 0
        for (j in i until (i + hop).coerceAtMost(samples.size)) {
            window = maxOf(window, abs(samples[j].toInt()))
        }
        out += window * 1000 / peak
        i += hop
    }
    return out
}
