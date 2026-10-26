/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.soundboard

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates the built-in soundboard clips procedurally – no bundled audio files, no licensing
 * questions, a few kilobytes of code instead of megabytes of samples.
 */
object SoundSynth {
    const val SAMPLE_RATE = 44_100
    private const val TAU = 2 * PI

    private val sine: (Double) -> Double = { sin(it) }
    private val saw: (Double) -> Double = { p ->
        // Band-limited-ish saw: sum of the first harmonics.
        var v = 0.0
        for (n in 1..6) v += sin(p * n) / n
        v * 0.6
    }
    private val square: (Double) -> Double = { p ->
        var v = 0.0
        for (n in 1..7 step 2) v += sin(p * n) / n
        v * 0.9
    }

    fun generate(id: String): ShortArray {
        val buffer = when (id) {
            "bleep" -> bleep()
            "dingdong" -> dingDong()
            "badumtss" -> baDumTss()
            "sadtrombone" -> sadTrombone()
            "airhorn" -> airHorn()
            "tada" -> taDa()
            "siren" -> siren()
            "crickets" -> crickets()
            else -> FloatArray(SAMPLE_RATE / 10)
        }
        return toPcm(buffer)
    }

    private fun buffer(seconds: Double) = FloatArray((seconds * SAMPLE_RATE).toInt())

    private fun tone(
        out: FloatArray,
        start: Double,
        duration: Double,
        amp: Double,
        wave: (Double) -> Double = sine,
        attack: Double = 0.005,
        release: Double = 0.03,
        decay: Double = 0.0,
        freq: (Double) -> Double,
    ) {
        val s0 = (start * SAMPLE_RATE).toInt()
        val n = (duration * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= out.size) break
            val t = i.toDouble() / SAMPLE_RATE
            phase += TAU * freq(t) / SAMPLE_RATE
            var env = 1.0
            if (t < attack) env *= t / attack
            if (duration - t < release) env *= max(0.0, (duration - t) / release)
            if (decay > 0) env *= exp(-t / decay)
            out[idx] += (wave(phase) * amp * env).toFloat()
        }
    }

    private fun noise(out: FloatArray, start: Double, duration: Double, amp: Double, decay: Double, highPass: Boolean) {
        val rnd = Random(42)
        val s0 = (start * SAMPLE_RATE).toInt()
        val n = (duration * SAMPLE_RATE).toInt()
        var prevIn = 0.0
        var prevOut = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= out.size) break
            val t = i.toDouble() / SAMPLE_RATE
            val x = rnd.nextDouble(-1.0, 1.0)
            val y = if (highPass) 0.85 * (prevOut + x - prevIn) else x
            prevIn = x
            prevOut = y
            out[idx] += (y * amp * exp(-t / decay)).toFloat()
        }
    }

    private fun bleep() = buffer(0.7).also { tone(it, 0.0, 0.7, 0.6, release = 0.01) { 1000.0 } }

    private fun dingDong() = buffer(2.0).also {
        tone(it, 0.0, 1.2, 0.5, decay = 0.45) { 659.25 }
        tone(it, 0.0, 1.2, 0.12, decay = 0.25) { 1318.5 }
        tone(it, 0.55, 1.45, 0.5, decay = 0.55) { 523.25 }
        tone(it, 0.55, 1.45, 0.12, decay = 0.3) { 1046.5 }
    }

    private fun baDumTss() = buffer(1.8).also {
        tone(it, 0.0, 0.3, 0.9, decay = 0.09) { t -> 120 + 110 * exp(-t * 30) }
        tone(it, 0.2, 0.3, 0.9, decay = 0.09) { t -> 90 + 90 * exp(-t * 30) }
        noise(it, 0.0, 0.12, 0.25, 0.03, highPass = true)
        noise(it, 0.2, 0.12, 0.25, 0.03, highPass = true)
        noise(it, 0.45, 1.3, 0.55, 0.35, highPass = true)
    }

    private fun sadTrombone() = buffer(3.0).also {
        val notes = listOf(293.66 to 0.42, 277.18 to 0.42, 261.63 to 0.42)
        var t0 = 0.05
        for ((f, d) in notes) {
            tone(it, t0, d, 0.35, saw, attack = 0.03, release = 0.08) { f }
            t0 += d + 0.04
        }
        tone(it, t0, 1.5, 0.35, saw, attack = 0.03, release = 0.4) { t ->
            246.94 * (1 + 0.025 * sin(TAU * 5.5 * t) * min(1.0, t * 2))
        }
    }

    private fun airHorn() = buffer(2.1).also {
        val blasts = listOf(0.0 to 0.3, 0.38 to 0.3, 0.76 to 1.2)
        for ((start, dur) in blasts) {
            for (f in listOf(415.0, 419.0, 523.0)) {
                tone(it, start, dur, 0.22, square, attack = 0.01, release = 0.05) { t ->
                    f * (0.94 + 0.06 * min(1.0, t * 25))
                }
            }
        }
    }

    private fun taDa() = buffer(1.9).also {
        tone(it, 0.0, 0.14, 0.35, saw, release = 0.02) { 523.25 }
        tone(it, 0.14, 0.14, 0.35, saw, release = 0.02) { 659.25 }
        tone(it, 0.28, 0.14, 0.35, saw, release = 0.02) { 783.99 }
        for (f in listOf(523.25, 659.25, 783.99, 1046.5)) {
            tone(it, 0.42, 1.45, 0.22, saw, attack = 0.02, release = 0.5) { t ->
                f * (1 + 0.006 * sin(TAU * 6 * t))
            }
        }
    }

    private fun siren() = buffer(2.6).also {
        tone(it, 0.0, 2.6, 0.45, { p -> sin(p) + 0.25 * sin(3 * p) }, attack = 0.05, release = 0.2) { t ->
            950 + 350 * sin(TAU * 0.9 * t)
        }
    }

    private fun crickets() = buffer(2.6).also { out ->
        for (i in out.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val cycle = t % 0.65
            if (cycle > 0.22) continue
            val pulse = abs(sin(PI * 32 * cycle)) // ~32 chirp pulses per second
            val env = sin(PI * cycle / 0.22)
            out[i] += (sin(TAU * 4400 * t) * pulse * env * 0.45).toFloat()
        }
    }

    private fun toPcm(buffer: FloatArray): ShortArray {
        var peak = 0f
        for (v in buffer) peak = max(peak, abs(v))
        val gain = if (peak > 0f) 0.9f / peak else 1f
        return ShortArray(buffer.size) { i ->
            (buffer[i] * gain * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
