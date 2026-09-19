package io.github.muntasimulhaque.readoclock.tools

import java.io.File

/**
 * Auditions a tick shape: writes the four variants and prints the numbers
 * against the real-clock targets, so the sound is tuned by measurement
 * rather than by taste alone.
 *
 * Recordings of real clocks, kept in `build/tick-ref/` and never bundled,
 * are measured through the same yardstick when they are there. Nothing in
 * this task ships: it is the tuning desk, not a build step.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val names = args.drop(1).filterNot { it.startsWith("--") }
    if (names.isNotEmpty()) {
        for (name in names) report(File(root, name))
        return
    }

    val out = File(root, "build/tick-tune")
    out.mkdirs()
    val references = File(root, "build/tick-ref").listFiles { file -> file.extension == "wav" }
    if (!references.isNullOrEmpty()) {
        println("Reference recordings of real clocks, measured through the same yardstick:")
        for (file in references.sortedBy { it.name }) report(file)
        println()
    }

    println("Real-clock targets: bands ${TickMetrics.TargetBands}, " +
        "head ${(TickMetrics.TargetHeadShare * 100).toInt()}% inside 5 ms, " +
        "sustain about ${TickMetrics.TargetSustainMs.toInt()} ms, " +
        "and the case answering at half the knock.")
    println()
    for (variant in 1..TickSynth.Variants) {
        val samples = TickSynth.samples(variant)
        File(out, "tick_$variant.wav").writeBytes(wavBytes(samples, TickSynth.SampleRate))
        println("tick_$variant.wav")
        report(samples, TickSynth.SampleRate)
        println()
    }
    println("written to ${out.relativeTo(root)}")
}

private fun report(file: File) {
    val samples = readWavMono(file)
    if (samples == null) {
        println("${file.name}: not a readable 16 bit mono WAV")
        return
    }
    println("${file.name} (${samples.size} samples, ${samples.size * 1000 / TickSynth.SampleRate} ms)")
    report(samples, TickSynth.SampleRate)
}

private fun report(samples: ShortArray, sampleRate: Int) {
    val metrics = TickMetrics.report(samples, sampleRate)
    val shares = metrics.bandShares.joinToString("/") { it.toInt().toString() }
    println(
        "  bands ${shares.padEnd(18)}" +
            " head<5ms ${(metrics.headShare * 100).toInt()}%" +
            "  sustain ${metrics.sustainMs.toInt()}ms" +
            "  caseAnswer ${"%.2f".format(metrics.secondLobeLevel)}" +
            "  firstLobes ${metrics.firstLobes.joinToString(",") { it.toInt().toString() }}",
    )
}
