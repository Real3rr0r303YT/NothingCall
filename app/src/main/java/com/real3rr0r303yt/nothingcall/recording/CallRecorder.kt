/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import java.io.File

/**
 * Records the call into a temporary AAC file in the app's private cache. The file is encrypted
 * and the plaintext deleted as soon as the recording stops (see [RecordingRepository.add]).
 *
 * Android only grants VOICE_CALL to system apps, so we fall back through sources that work for
 * regular apps on most devices.
 */
class CallRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    private fun sourcesFor(pref: String): List<Int> = when (pref) {
        AppPrefs.SOURCE_VOICE_CALL -> listOf(MediaRecorder.AudioSource.VOICE_CALL)
        AppPrefs.SOURCE_VOICE_COMM -> listOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        AppPrefs.SOURCE_MIC -> listOf(MediaRecorder.AudioSource.MIC)
        else -> listOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    fun start(sourcePref: String): Boolean {
        for (source in sourcesFor(sourcePref)) {
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            val r = newRecorder()
            try {
                r.setAudioSource(source)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioChannels(1)
                r.setAudioSamplingRate(44_100)
                r.setAudioEncodingBitRate(96_000)
                r.setOutputFile(file.absolutePath)
                r.prepare()
                r.start()
                recorder = r
                output = file
                return true
            } catch (e: Exception) {
                r.release()
                file.delete()
            }
        }
        return false
    }

    /** Stops recording and returns the plaintext temp file (or null on failure). */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        var file = output
        try {
            r.stop()
        } catch (e: RuntimeException) {
            // Stopped too early – no valid data was written.
            file?.delete()
            file = null
        } finally {
            r.release()
        }
        output = null
        return file
    }
}
