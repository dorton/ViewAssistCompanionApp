package com.msp1974.vacompanion.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Converts interleaved 48kHz stereo PCM to 16kHz mono: averages the channels, low-pass
 * filters below the new Nyquist and keeps every third sample. Filter history and decimation
 * phase carry across calls, so reads of any size join up seamlessly.
 */
class Stereo48kToMono16k {
    companion object {
        const val INPUT_RATE_HZ = 48000
        // Input shorts consumed per output short (2 channels x 3:1 decimation)
        const val RATIO = 6
        private const val DECIMATION = 3
        private const val TAPS = 63
        private const val CUTOFF_HZ = 7000.0

        // Hamming-windowed sinc, normalised to unity gain at DC
        private val coefficients: FloatArray = run {
            val fc = CUTOFF_HZ / INPUT_RATE_HZ
            val mid = (TAPS - 1) / 2.0
            val h = DoubleArray(TAPS) { i ->
                val x = i - mid
                val sinc = if (x == 0.0) 2 * fc else sin(2 * PI * fc * x) / (PI * x)
                sinc * (0.54 - 0.46 * cos(2 * PI * i / (TAPS - 1)))
            }
            val sum = h.sum()
            FloatArray(TAPS) { (h[it] / sum).toFloat() }
        }
    }

    private val history = FloatArray(TAPS)
    private var pos = 0
    private var phase = 0

    fun process(interleaved: ShortArray, count: Int): ShortArray {
        val out = ShortArray(count / RATIO + 1)
        var n = 0
        var i = 0
        while (i + 1 < count) {
            history[pos] = (interleaved[i] + interleaved[i + 1]) / 2f
            pos = (pos + 1) % TAPS
            i += 2
            if (++phase == DECIMATION) {
                phase = 0
                var acc = 0f
                var h = pos
                for (c in coefficients) {
                    h = if (h == 0) TAPS - 1 else h - 1
                    acc += c * history[h]
                }
                out[n++] = acc.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out.copyOf(n)
    }
}
