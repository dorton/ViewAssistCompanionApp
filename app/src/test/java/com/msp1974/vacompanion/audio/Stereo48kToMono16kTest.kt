package com.msp1974.vacompanion.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class Stereo48kToMono16kTest {

    private fun stereoTone(freqHz: Double, seconds: Double, amplitude: Double = 10000.0): ShortArray {
        val frames = (48000 * seconds).toInt()
        return ShortArray(frames * 2) { i ->
            (amplitude * sin(2 * PI * freqHz * (i / 2) / 48000)).roundToInt().toShort()
        }
    }

    private fun rms(x: ShortArray, skip: Int = 100): Double =
        sqrt(x.drop(skip).sumOf { it.toDouble() * it } / (x.size - skip))

    // Goertzel power of one frequency, normalised so a full-scale-A tone gives ~A^2/2
    private fun toneLevel(x: ShortArray, freqHz: Double, rate: Int = 16000, skip: Int = 100): Double {
        val s = x.drop(skip)
        var re = 0.0
        var im = 0.0
        s.forEachIndexed { n, v ->
            re += v * kotlin.math.cos(2 * PI * freqHz * n / rate)
            im += v * sin(2 * PI * freqHz * n / rate)
        }
        return 2 * (re * re + im * im) / (s.size.toDouble() * s.size)
    }

    @Test
    fun outputIsOneSixthOfInputShorts() {
        val out = Stereo48kToMono16k().process(stereoTone(1000.0, 1.0), 96000)
        assertEquals(16000, out.size)
    }

    @Test
    fun passbandToneKeepsPitchAndLevel() {
        val out = Stereo48kToMono16k().process(stereoTone(1000.0, 1.0), 96000)
        val level = toneLevel(out, 1000.0)
        assertEquals(10000.0 * 10000.0 / 2, level, 0.05 * level)
        // essentially all energy is at 1kHz (not at the 6x-slowed 167Hz the HAL bug produced)
        assertTrue(toneLevel(out, 1000.0 / 6) < 0.001 * level)
    }

    @Test
    fun aboveNyquistToneIsFilteredNotAliased() {
        // 10kHz would alias to 6kHz at 16kHz without the low-pass
        val out = Stereo48kToMono16k().process(stereoTone(10000.0, 1.0), 96000)
        assertTrue("rms=${rms(out)}", rms(out) < 100)
    }

    @Test
    fun chunkedReadsMatchSingleRead() {
        val input = stereoTone(1234.0, 0.5)
        val whole = Stereo48kToMono16k().process(input, input.size)

        val chunked = Stereo48kToMono16k()
        val parts = mutableListOf<Short>()
        var i = 0
        val sizes = intArrayOf(2, 6, 314, 1000, 7680, 4)
        var k = 0
        while (i < input.size) {
            val n = minOf(sizes[k++ % sizes.size], input.size - i)
            parts += chunked.process(input.copyOfRange(i, i + n), n).toList()
            i += n
        }
        assertArrayEquals(whole, parts.toShortArray())
    }
}
