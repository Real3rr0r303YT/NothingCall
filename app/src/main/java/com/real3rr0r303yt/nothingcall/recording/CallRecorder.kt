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
import com.real3rr0r303yt.nothingcall.util.Privileged
import java.io.File

/**
 * Records the call into a temporary AAC file in the app's private cache. The file is encrypted
 * and the plaintext deleted as soon as the recording stops (see [RecordingRepository.add]).
 *
 * Source strategy:
 *  - System mode (CAPTURE_AUDIO_OUTPUT granted): VOICE_CALL → both sides, clean digital audio.
 *  - Regular app: several sources are tried in order. Many devices hand out a *silent* stream
 *    instead of failing, so [RecordingService] watches the level and calls [switchToNextSource]
 *    when nothing but silence arrives.
 */
class CallRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var sources: List<Int> = emptyList()
    private var index = -1

    /** True when the current source captures both sides of the call. */
    var bothSides: Boolean = false
        private set

    private fun sourcesFor(pref: String): List<Int> {
        val system = Privileged.canRecordCallAudio(context)
        return when (pref) {
            AppPrefs.SOURCE_VOICE_CALL -> listOf(MediaRecorder.AudioSource.VOICE_CALL)
            AppPrefs.SOURCE_VOICE_COMM -> listOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            AppPrefs.SOURCE_MIC -> listOf(MediaRecorder.AudioSource.MIC)
            else -> buildList {
                add(MediaRecorder.AudioSource.VOICE_CALL)
                if (!system) {
                    add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    add(MediaRecorder.AudioSource.MIC)
                    add(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    add(MediaRecorder.AudioSource.CAMCORDER)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    fun start(sourcePref: String): Boolean {
        sources = sourcesFor(sourcePref)
        index = -1
        return startFrom(0)
    }

    private fun startFrom(first: Int): Boolean {
        for (i in first until sources.size) {
            val source = sources[i]
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            val r = newRecorder()
            try {
                r.setAudioSource(source)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioChannels(1)
                r.setAudioSamplingRate(if (source == MediaRecorder.AudioSource.VOICE_CALL) 16_000 else 44_100)
                r.setAudioEncodingBitRate(if (source == MediaRecorder.AudioSource.VOICE_CALL) 64_000 else 96_000)
                r.setOutputFile(file.absolutePath)
                r.prepare()
                r.start()
                recorder = r
                output = file
                index = i
                bothSides = source == MediaRecorder.AudioSource.VOICE_CALL
                r.maxAmplitude // reset the peak meter
                return true
            } catch (e: Exception) {
                r.release()
                file.delete()
            }
        }
        return false
    }

    /** Peak level since the last call (0 = pure silence). */
    fun maxAmplitude(): Int = try {
        recorder?.maxAmplitude ?: 0
    } catch (e: Exception) {
        0
    }

    val hasFallback: Boolean get() = index in 0 until sources.size - 1

    /** Discards the (silent) current take and continues with the next audio source. */
    fun switchToNextSource(): Boolean {
        val r = recorder ?: return false
        recorder = null
        runCatching { r.stop() }
        r.release()
        output?.delete()
        output = null
        return startFrom(index + 1)
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
