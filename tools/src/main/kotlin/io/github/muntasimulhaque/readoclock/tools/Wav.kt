package io.github.muntasimulhaque.readoclock.tools

import java.io.File

/**
 * A 16 bit mono PCM WAV, written by hand so no encoder can vary the bytes.
 * The tick and its audition both go through here, and the committed WAV is
 * pinned with `:tools:checkTick`.
 */
fun wavBytes(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataBytes = samples.size * 2
    val out = ByteArray(44 + dataBytes)
    ascii(out, 0, "RIFF")
    putInt(out, 4, 36 + dataBytes)
    ascii(out, 8, "WAVE")
    ascii(out, 12, "fmt ")
    putInt(out, 16, 16)
    putShort(out, 20, 1)
    putShort(out, 22, 1)
    putInt(out, 24, sampleRate)
    putInt(out, 28, sampleRate * 2)
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

/**
 * Reads a 16 bit mono PCM WAV back, for the tuning harness. Null when the
 * file is missing or is not one; the harness prints that and moves on
 * rather than failing a build over an optional file.
 */
fun readWavMono(file: File): ShortArray? {
    if (!file.exists()) return null
    val bytes = file.readBytes()
    var offset = 12
    var payload: ByteArray? = null
    while (offset + 8 <= bytes.size) {
        val id = String(bytes, offset, 4, Charsets.US_ASCII)
        val size = readInt(bytes, offset + 4)
        if (size < 0) return null
        if (id == "data") {
            payload = bytes.copyOfRange(offset + 8, (offset + 8 + size).coerceAtMost(bytes.size))
        }
        offset += 8 + size + (size % 2)
    }
    val data = payload ?: return null
    return ShortArray(data.size / 2) {
        ((data[it * 2].toInt() and 0xFF) or (data[it * 2 + 1].toInt() shl 8)).toShort()
    }
}

private fun readInt(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
