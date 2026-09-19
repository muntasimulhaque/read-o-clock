package io.github.muntasimulhaque.readoclock.tools

import java.io.File
import kotlin.system.exitProcess

/**
 * Generates the quartz tick used by the app: four pieces of the same
 * movement, written as `tick_1.wav` through `tick_4.wav`. The player
 * rotates them, because one sample repeated once a second for a minute is
 * heard as a loop even when the sample itself is right.
 *
 * The shape and its justification live in `TickSynth`; the four variants
 * differ only in the tiny level, tilt and timing jitter described there.
 * Every byte is arithmetic, so the output is identical on every machine
 * and `--check` can ban drift.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".")
    val check = args.contains("--check")

    var drifted = false
    for (variant in 1..TickSynth.Variants) {
        val out = File(root, "app/src/main/res/raw/${TickSynth.resourceName(variant)}")
        val fresh = wavBytes(TickSynth.samples(variant), TickSynth.SampleRate)
        if (check) {
            val committed = if (out.exists()) out.readBytes() else null
            if (committed == null || !committed.contentEquals(fresh)) {
                println("tick drift: ${out.relativeTo(root)}")
                drifted = true
            }
        } else {
            out.parentFile.mkdirs()
            out.writeBytes(fresh)
            println("tick written: ${out.relativeTo(root)} (${fresh.size} bytes)")
        }
    }

    if (check) {
        if (drifted) {
            println("regenerate with :tools:makeTick")
            exitProcess(1)
        }
        println("tick set matches a fresh regeneration")
    }
}
