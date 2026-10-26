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
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.call.CallManager
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.security.CryptoManager
import com.real3rr0r303yt.nothingcall.security.SecureStore
import com.real3rr0r303yt.nothingcall.util.AppScope
import com.real3rr0r303yt.nothingcall.util.ByteArrayMediaDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val pcmCache = HashMap<String, ShortArray>()
    private var track: AudioTrack? = null
    private var player: MediaPlayer? = null
    private val lock = Any()

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun dir(context: Context) = File(context.filesDir, "soundboard").apply { mkdirs() }
    private fun indexFile(context: Context) = File(dir(context), "index.enc")
    private fun fileFor(context: Context, clip: SoundClip) = File(dir(context), "${clip.id}.enc")

    fun play(context: Context, clip: SoundClip) {
        stop()
        if (AppPrefs.soundboardSpeaker.value && CallManager.calls.value.isNotEmpty() && !CallManager.isSpeakerOn) {
            CallManager.setSpeaker(true)
        }
        _playing.value = clip.id
        val app = context.applicationContext
        AppScope.io.launch {
            try {
                if (clip.builtIn) {
                    val pcm = synchronized(lock) { pcmCache.getOrPut(clip.id) { SoundSynth.generate(clip.id) } }
                    withContext(Dispatchers.Main) { if (_playing.value == clip.id) playPcm(clip.id, pcm) }
                } else {
                    val bytes = CryptoManager.decryptFile(fileFor(app, clip))
                    withContext(Dispatchers.Main) { if (_playing.value == clip.id) playBytes(clip.id, bytes) }
                }
            } catch (e: Exception) {
                _playing.value = null
            }
        }
    }

    private fun playPcm(id: String, pcm: ShortArray) {
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
        t.setNotificationMarkerPosition(pcm.size - 1)
        t.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                if (_playing.value == id) stop()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
        track = t
        t.play()
    }

    private fun playBytes(id: String, bytes: ByteArray) {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(ByteArrayMediaDataSource(bytes))
            mp.setOnCompletionListener { if (_playing.value == id) stop() }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            mp.release()
            _playing.value = null
        }
    }

    fun stop() {
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
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
        fileFor(context, clip).delete()
        persist(context, _custom.value.filterNot { it.id == clip.id })
    }

    fun wipe(context: Context) = synchronized(lock) {
        stop()
        dir(context).deleteRecursively()
        _custom.value = emptyList()
    }
}
