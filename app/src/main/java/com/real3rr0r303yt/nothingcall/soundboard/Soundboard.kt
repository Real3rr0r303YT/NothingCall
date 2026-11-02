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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.call.CallManager
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.security.CryptoManager
import com.real3rr0r303yt.nothingcall.security.SecureStore
import com.real3rr0r303yt.nothingcall.util.AppScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SoundClip(
    val id: String,
    val name: String = "",
    @StringRes val nameRes: Int = 0,
    val builtIn: Boolean = false,
) {
    fun label(context: Context): String = if (nameRes != 0) context.getString(nameRes) else name
}

/**
 * In-call soundboard. Built-in clips are synthesized; imported clips are stored encrypted and
 * decrypted into memory only for playback.
 */
object Soundboard {
    val builtIns = listOf(
        SoundClip("bleep", nameRes = R.string.clip_bleep, builtIn = true),
        SoundClip("dingdong", nameRes = R.string.clip_dingdong, builtIn = true),
        SoundClip("badumtss", nameRes = R.string.clip_badumtss, builtIn = true),
        SoundClip("sadtrombone", nameRes = R.string.clip_sadtrombone, builtIn = true),
        SoundClip("airhorn", nameRes = R.string.clip_airhorn, builtIn = true),
        SoundClip("tada", nameRes = R.string.clip_tada, builtIn = true),
        SoundClip("siren", nameRes = R.string.clip_siren, builtIn = true),
        SoundClip("crickets", nameRes = R.string.clip_crickets, builtIn = true),
    )

    private val _custom = MutableStateFlow<List<SoundClip>>(emptyList())
    val custom: StateFlow<List<SoundClip>> = _custom.asStateFlow()

    private val _playing = MutableStateFlow<String?>(null)
    val playing: StateFlow<String?> = _playing.asStateFlow()

    /** How clips reach the other party. */
    enum class Mode { INJECT, SPEAKER }

    private val pcmCache = HashMap<String, ShortArray>()
    private val lock = Any()

    @Volatile private var token: Any? = null
    @Volatile private var job: Job? = null
    @Volatile private var localTrack: AudioTrack? = null
    @Volatile private var uplinkTrack: AudioTrack? = null

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun dir(context: Context) = File(context.filesDir, "soundboard").apply { mkdirs() }
    private fun indexFile(context: Context) = File(dir(context), "index.enc")
    private fun fileFor(context: Context, clip: SoundClip) = File(dir(context), "${clip.id}.enc")

    fun mode(context: Context): Mode =
        if (CallAudioInjector.isAvailable(context)) Mode.INJECT else Mode.SPEAKER

    private fun pcmFor(context: Context, clip: SoundClip): ShortArray {
        synchronized(lock) { pcmCache[clip.id]?.let { return it } }
        val pcm = if (clip.builtIn) {
            SoundSynth.generate(clip.id)
        } else {
            AudioDecoder.decodeMono(CryptoManager.decryptFile(fileFor(context, clip)), SoundSynth.SAMPLE_RATE)
        }
        synchronized(lock) { pcmCache[clip.id] = pcm }
        return pcm
    }

    private fun localTrack(pcm: ShortArray, volume: Float): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SoundSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setVolume(volume)
        return t
    }

    fun play(context: Context, clip: SoundClip) {
        stop()
        val app = context.applicationContext
        val inCall = CallManager.calls.value.isNotEmpty()
        val inject = inCall && CallAudioInjector.isAvailable(app)
        if (!inject && inCall && AppPrefs.soundboardSpeaker.value && !CallManager.isSpeakerOn) {
            CallManager.setSpeaker(true)
        }
        val myToken = Any()
        token = myToken
        _playing.value = clip.id
        job = AppScope.io.launch {
            try {
                val pcm = pcmFor(app, clip)
                val durationMs = pcm.size * 1000L / SoundSynth.SAMPLE_RATE
                val uplink = if (inject) CallAudioInjector.open(app) else null
                if (uplink != null) {
                    // Straight into the call. The local copy is just a quiet monitor for you.
                    val (track, rate) = uplink
                    uplinkTrack = track
                    val data = Resampler.resample(pcm, SoundSynth.SAMPLE_RATE, rate)
                    localTrack = localTrack(pcm, 0.35f).also { it.play() }
                    track.play()
                    var offset = 0
                    while (offset < data.size && isActive && token === myToken) {
                        val written = track.write(data, offset, minOf(2048, data.size - offset))
                        if (written <= 0) break
                        offset += written
                    }
                    delay(200)
                } else {
                    localTrack = localTrack(pcm, 1f).also { it.play() }
                    delay(durationMs + 100)
                }
            } catch (e: Exception) {
                // Clip could not be decoded or played – just reset the pad.
            } finally {
                releaseTracks()
                if (token === myToken) {
                    token = null
                    _playing.value = null
                }
            }
        }
    }

    private fun releaseTracks() {
        localTrack?.let { runCatching { it.stop() }; it.release() }
        localTrack = null
        uplinkTrack?.let { runCatching { it.stop() }; it.release() }
        uplinkTrack = null
    }

    fun stop() {
        token = null
        job?.cancel()
        job = null
        localTrack?.let { runCatching { it.pause() } }
        uplinkTrack?.let { runCatching { it.pause(); it.flush() } }
        _playing.value = null
    }

    // ---- imported clips ----

    fun loadCustom(context: Context) = synchronized(lock) {
        val list = mutableListOf<SoundClip>()
        SecureStore.readText(indexFile(context))?.let { text ->
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += SoundClip(id = o.getString("id"), name = o.getString("name"))
            }
        }
        _custom.value = list.filter { fileFor(context, it).exists() }
    }

    private fun persist(context: Context, list: List<SoundClip>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
        SecureStore.writeText(indexFile(context), arr.toString())
        _custom.value = list
    }

    fun importClip(context: Context, uri: Uri): Boolean {
        try {
            val displayName = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            val clip = SoundClip(
                id = UUID.randomUUID().toString(),
                name = (displayName ?: "Clip").substringBeforeLast('.').take(24),
            )
            val input = context.contentResolver.openInputStream(uri) ?: return false
            synchronized(lock) {
                input.use { CryptoManager.encryptToFile(it, fileFor(context, clip)) }
                persist(context, _custom.value + clip)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun delete(context: Context, clip: SoundClip) = synchronized(lock) {
        pcmCache.remove(clip.id)
        fileFor(context, clip).delete()
        persist(context, _custom.value.filterNot { it.id == clip.id })
    }

    fun wipe(context: Context) = synchronized(lock) {
        stop()
        pcmCache.clear()
        dir(context).deleteRecursively()
        _custom.value = emptyList()
    }
}
