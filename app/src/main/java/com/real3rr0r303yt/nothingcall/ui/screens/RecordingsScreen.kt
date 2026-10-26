/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.recording.Recording
import com.real3rr0r303yt.nothingcall.recording.RecordingRepository
import com.real3rr0r303yt.nothingcall.ui.components.EmptyState
import com.real3rr0r303yt.nothingcall.ui.components.LetterAvatar
import com.real3rr0r303yt.nothingcall.ui.components.RoundIconButton
import com.real3rr0r303yt.nothingcall.ui.components.ScreenHeader
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.util.ByteArrayMediaDataSource
import com.real3rr0r303yt.nothingcall.util.Format
import com.real3rr0r303yt.nothingcall.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plays decrypted recordings from memory. */
@Stable
class RecordingPlayer(private val context: Context) {
    var playingId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    private var player: MediaPlayer? = null

    fun toggle(recording: Recording, scope: CoroutineScope) {
        val current = player
        if (playingId == recording.id && current != null) {
            if (isPlaying) current.pause() else current.start()
            isPlaying = !isPlaying
            return
        }
        release()
        playingId = recording.id
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { RecordingRepository.decrypt(context, recording) }.getOrNull()
            }
            if (bytes == null || playingId != recording.id) {
                if (bytes == null) context.toast(R.string.recording_play_failed)
                if (playingId == recording.id) playingId = null
                return@launch
            }
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(ByteArrayMediaDataSource(bytes))
                mp.setOnCompletionListener { release() }
                mp.prepare()
                mp.start()
                player = mp
                duration = mp.duration.toLong().coerceAtLeast(1L)
                position = 0L
                isPlaying = true
            } catch (e: Exception) {
                mp.release()
                playingId = null
                context.toast(R.string.recording_play_failed)
            }
        }
    }

    fun seek(ms: Long) {
        player?.seekTo(ms.toInt())
        position = ms
    }

    fun tick() {
        player?.let { if (isPlaying) position = it.currentPosition.toLong() }
    }

    fun release() {
        player?.release()
        player = null
        playingId = null
        isPlaying = false
        position = 0L
    }
}

@Composable
fun RecordingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recordings by RecordingRepository.items.collectAsState()
    val player = remember { RecordingPlayer(context) }
    var exportTarget by remember { mutableStateOf<Recording?>(null) }
    var deleteTarget by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.tick()
            delay(200)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/mp4")
    ) { uri ->
        val rec = exportTarget
        exportTarget = null
        if (uri != null && rec != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { RecordingRepository.export(context, rec, uri) }.isSuccess
                }
                context.toast(if (ok) R.string.recording_exported else R.string.recording_play_failed)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.tab_recordings))
        if (recordings.isEmpty()) {
            EmptyState(stringResource(R.string.recordings_empty))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(recordings, key = { it.id }) { rec ->
                    RecordingRow(
                        recording = rec,
                        player = player,
                        onToggle = { player.toggle(rec, scope) },
                        onExport = {
                            exportTarget = rec
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(rec.timestamp))
                            val who = (rec.title ?: "call").filter { it.isLetterOrDigit() || it == '+' }
                            exportLauncher.launch("NothingCall_${who}_$stamp.m4a")
                        },
                        onDelete = { deleteTarget = rec },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Lock, null, tint = NColors.Red, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(8.dp))
            SectionLabel(stringResource(R.string.recordings_footer))
        }
    }

    deleteTarget?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = NColors.SurfaceHigh,
            title = { Text(rec.title ?: stringResource(R.string.unknown_number)) },
            text = { Text(stringResource(R.string.recording_delete_confirm), color = NColors.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    if (player.playingId == rec.id) player.release()
                    scope.launch(Dispatchers.IO) { runCatching { RecordingRepository.delete(context, rec) } }
                }) { Text(stringResource(R.string.delete), color = NColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel), color = NColors.White) }
            },
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    player: RecordingPlayer,
    onToggle: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val active = player.playingId == recording.id
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(recording.title, accent = active)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    recording.title ?: stringResource(R.string.unknown_number),
                    color = NColors.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${Format.dateTime(context, recording.timestamp)} · ${Format.duration(recording.durationMs)}",
                    color = NColors.Gray, fontSize = 12.sp, maxLines = 1,
                )
            }
            RoundIconButton(
                icon = if (active && player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                onClick = onToggle,
                size = 44.dp,
                container = if (active) NColors.White else NColors.SurfaceHigh,
                contentColor = if (active) NColors.Black else NColors.White,
            )
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, null, tint = NColors.Gray)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recording_export)) },
                        leadingIcon = { Icon(Icons.Filled.FileDownload, null) },
                        onClick = { menu = false; onExport() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = NColors.Red) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = NColors.Red) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
        if (active && player.duration > 0) {
            Spacer(Modifier.height(4.dp))
            Slider(
                value = player.position.toFloat().coerceIn(0f, player.duration.toFloat()),
                onValueChange = { player.seek(it.toLong()) },
                valueRange = 0f..player.duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NColors.White,
                    activeTrackColor = NColors.Red,
                    inactiveTrackColor = NColors.Outline,
                ),
            )
            Row {
                SectionLabel(Format.duration(player.position))
                Spacer(Modifier.weight(1f))
                SectionLabel(Format.duration(player.duration))
            }
        }
    }
}
