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

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.real3rr0r303yt.nothingcall.util.Privileged

/**
 * Android 14+ system API `AudioManager.getCallUplinkInjectionAudioTrack()` – the same mechanism
 * OEM features like One UI's in-call sounds rely on. Audio written to this track goes straight
 * into the call uplink, so the other party hears it cleanly (no loudspeaker, no echo canceller).
 *
 * Only usable in system mode (CALL_AUDIO_INTERCEPTION granted to a privileged app).
 */
object CallAudioInjector {
    private val RATES = intArrayOf(48_000, 16_000, 8_000)

    fun isAvailable(context: Context) = Privileged.canInjectCallAudio(context)

    /** Returns an uplink track and its sample rate, or null when injection is not possible. */
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi", "SoonBlockedPrivateApi")
    fun open(context: Context): Pair<AudioTrack, Int>? {
        if (!isAvailable(context)) return null
        val am = context.getSystemService(AudioManager::class.java) ?: return null
        val method = runCatching {
            AudioManager::class.java.getMethod("getCallUplinkInjectionAudioTrack", AudioFormat::class.java)
        }.getOrNull() ?: return null
        for (rate in RATES) {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = runCatching { method.invoke(am, format) as? AudioTrack }.getOrNull()
            if (track != null) return track to rate
        }
        return null
    }
}
