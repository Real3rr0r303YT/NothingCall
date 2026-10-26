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
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.real3rr0r303yt.nothingcall.security.CryptoManager
import com.real3rr0r303yt.nothingcall.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Recording(
    val id: String,
    val number: String,
    val name: String?,
    val timestamp: Long,
    val durationMs: Long,
    val sizeBytes: Long,
) {
    val title: String? get() = name?.takeIf { it.isNotBlank() } ?: number.takeIf { it.isNotBlank() }
}

/** Encrypted on-device storage for call recordings (audio + an encrypted JSON index). */
object RecordingRepository {
    private val _items = MutableStateFlow<List<Recording>>(emptyList())
    val items: StateFlow<List<Recording>> = _items.asStateFlow()
    private val lock = Any()
    private var loaded = false

    private fun dir(context: Context) = File(context.filesDir, "recordings").apply { mkdirs() }
    private fun indexFile(context: Context) = File(dir(context), "index.enc")
    private fun fileFor(context: Context, recording: Recording) = File(dir(context), "${recording.id}.enc")

    fun load(context: Context) = synchronized(lock) {
        val list = mutableListOf<Recording>()
        SecureStore.readText(indexFile(context))?.let { text ->
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Recording(
                    id = o.getString("id"),
                    number = o.optString("number"),
                    name = o.optString("name").takeIf { it.isNotBlank() },
                    timestamp = o.getLong("ts"),
                    durationMs = o.optLong("duration"),
                    sizeBytes = o.optLong("size"),
                )
            }
        }
        _items.value = list.filter { fileFor(context, it).exists() }.sortedByDescending { it.timestamp }
        loaded = true
    }

    private fun persist(context: Context, list: List<Recording>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("number", it.number)
                    .put("name", it.name ?: "")
                    .put("ts", it.timestamp)
                    .put("duration", it.durationMs)
                    .put("size", it.sizeBytes)
            )
        }
        SecureStore.writeText(indexFile(context), arr.toString())
        _items.value = list.sortedByDescending { it.timestamp }
    }

    private fun readDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Encrypts the plaintext temp file, deletes it and adds the recording to the index. */
    fun add(context: Context, plain: File, number: String, name: String?, startedAt: Long) = synchronized(lock) {
        try {
            if (!loaded) load(context)
            val recording = Recording(
                id = UUID.randomUUID().toString(),
                number = number,
                name = name,
                timestamp = startedAt,
                durationMs = readDuration(plain),
                sizeBytes = plain.length(),
            )
            plain.inputStream().use { CryptoManager.encryptToFile(it, fileFor(context, recording)) }
            persist(context, _items.value + recording)
        } finally {
            plain.delete()
        }
    }

    fun delete(context: Context, recording: Recording) = synchronized(lock) {
        fileFor(context, recording).delete()
        persist(context, _items.value.filterNot { it.id == recording.id })
    }

    fun decrypt(context: Context, recording: Recording): ByteArray =
        CryptoManager.decryptFile(fileFor(context, recording))

    /** Writes a decrypted copy to a location the user picked explicitly (SAF). */
    fun export(context: Context, recording: Recording, target: Uri) {
        context.contentResolver.openOutputStream(target)?.use { it.write(decrypt(context, recording)) }
    }

    fun wipe(context: Context) = synchronized(lock) {
        dir(context).deleteRecursively()
        _items.value = emptyList()
    }

    /** Removes plaintext leftovers of recordings interrupted by a crash. */
    fun cleanupTemp(context: Context) {
        context.cacheDir.listFiles()?.filter { it.name.startsWith("rec_") }?.forEach { it.delete() }
    }
}
