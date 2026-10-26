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

import android.Manifest
import android.content.Context
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.call.CallActions
import com.real3rr0r303yt.nothingcall.call.CallManager
import com.real3rr0r303yt.nothingcall.call.CallSnapshot
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.recording.RecordingService
import com.real3rr0r303yt.nothingcall.soundboard.Soundboard
import com.real3rr0r303yt.nothingcall.ui.components.BlinkingDot
import com.real3rr0r303yt.nothingcall.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingcall.ui.components.Keypad
import com.real3rr0r303yt.nothingcall.ui.components.LegalNoticeDialog
import com.real3rr0r303yt.nothingcall.ui.components.NCard
import com.real3rr0r303yt.nothingcall.ui.components.RoundIconButton
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.components.dotGrid
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.ui.theme.NType
import com.real3rr0r303yt.nothingcall.util.Format
import com.real3rr0r303yt.nothingcall.util.toast
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InCallScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val calls by CallManager.calls.collectAsStateWithLifecycle()
    val audio by CallManager.audioState.collectAsStateWithLifecycle()
    val rec by RecordingService.state.collectAsStateWithLifecycle()
    val autoRecord by AppPrefs.autoRecord.flow.collectAsStateWithLifecycle()

    val primary = CallManager.primary(calls)
    var lastShown by remember { mutableStateOf<CallSnapshot?>(null) }
    LaunchedEffect(primary) { if (primary != null) lastShown = primary }
    val shown = primary ?: lastShown
    val ended = primary == null || primary.state == Call.STATE_DISCONNECTED

    LaunchedEffect(calls.isEmpty()) {
        if (calls.isEmpty()) {
            delay(900)
            onFinished()
        }
    }

    var showKeypad by rememberSaveable { mutableStateOf(false) }
    var showSoundboard by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    var dtmfDigits by rememberSaveable { mutableStateOf("") }

    val muted = audio?.isMuted == true
    val speakerOn = audio?.route == CallAudioState.ROUTE_SPEAKER

    val startRecording: () -> Unit = {
        val p = CallManager.primary()
        if (p != null && !RecordingService.start(context, p.number, p.name)) {
            context.toast(R.string.recording_failed)
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else context.toast(R.string.permission_missing)
    }
    val toggleRecording: () -> Unit = {
        when {
            rec.recording -> RecordingService.stop(context)
            !AppPrefs.legalAccepted.value -> showLegal = true
            !RecordingService.hasPermission(context) -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
            else -> startRecording()
        }
    }

    // Auto-record every call once it is connected.
    val autoStarted = remember { mutableSetOf<Call>() }
    LaunchedEffect(primary?.call, primary?.state) {
        val p = primary ?: return@LaunchedEffect
        if (p.state == Call.STATE_ACTIVE && autoRecord && AppPrefs.legalAccepted.value &&
            !rec.recording && autoStarted.add(p.call)
        ) {
            if (!RecordingService.start(context, p.number, p.name)) context.toast(R.string.recording_failed)
        }
    }

    ProximityLock(
        enabled = primary != null && !speakerOn && !showKeypad &&
            audio?.route == CallAudioState.ROUTE_EARPIECE &&
            (primary.state == Call.STATE_ACTIVE || primary.state == Call.STATE_DIALING)
    )

    Box(Modifier.fillMaxSize().background(NColors.Black).dotGrid()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Status line
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(stateLabel(shown, ended), Modifier.weight(1f))
                if (rec.recording) {
                    BlinkingDot()
                    Spacer(Modifier.width(6.dp))
                    val recElapsed = ticker(true) - rec.startedAt
                    SectionLabel(
                        "${stringResource(R.string.incall_rec)} ${Format.duration(recElapsed)}",
                        color = NColors.Red,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Caller
            val title = shown?.title ?: stringResource(R.string.unknown_number)
            DotMatrixText(
                title,
                Modifier.fillMaxWidth(),
                dotSize = 5.dp,
                contentAlignment = Alignment.Center,
            )
            if (shown?.name != null && shown.number.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(shown.number, style = NType.mono, color = NColors.Gray)
            }
            Spacer(Modifier.height(20.dp))
            if (shown != null && shown.state == Call.STATE_ACTIVE && shown.connectTimeMillis > 0) {
                val now = ticker(true)
                DotMatrixText(
                    Format.duration(now - shown.connectTimeMillis),
                    dotSize = 3.dp,
                    color = NColors.Gray,
                    autoFit = false,
                )
            }

            // Second call (call waiting / on hold)
            val other = calls.firstOrNull { it.call != primary?.call && it.state != Call.STATE_DISCONNECTED }
            if (other != null && primary != null && primary.state != Call.STATE_RINGING) {
                Spacer(Modifier.height(20.dp))
                NCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel(stateLabel(other, false))
                            Text(
                                other.title ?: stringResource(R.string.unknown_number),
                                color = NColors.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (other.state == Call.STATE_HOLDING) {
                            OutlinedButton(onClick = { CallManager.swap() }) {
                                Icon(Icons.Filled.SwapCalls, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.incall_swap), color = NColors.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val current = if (ended) null else primary
            when {
                current == null -> {
                    DotMatrixText(stringResource(R.string.incall_ended), dotSize = 3.dp, color = NColors.Red)
                    Spacer(Modifier.height(64.dp))
                }
                current.state == Call.STATE_RINGING -> IncomingActions(current)
                current.state == Call.STATE_SELECT_PHONE_ACCOUNT -> SelectSim(current)
                showKeypad -> {
                    DotMatrixText(
                        dtmfDigits.takeLast(16).ifEmpty { " " },
                        Modifier.fillMaxWidth().height(28.dp),
                        dotSize = 4.dp,
                        contentAlignment = Alignment.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Keypad(onKey = { c ->
                        dtmfDigits += c
                        CallManager.playDtmf(current.call, c)
                    })
                    TextButton(onClick = { showKeypad = false }) {
                        Text(stringResource(R.string.incall_hide), color = NColors.Gray)
                    }
                    EndCallButton(current)
                }
                else -> {
                    ControlsGrid(
                        muted = muted,
                        speakerOn = speakerOn,
                        onHold = current.state == Call.STATE_HOLDING,
                        canHold = current.canHold,
                        recording = rec.recording,
                        onMute = { CallManager.setMuted(!muted) },
                        onKeypad = { showKeypad = true },
                        onSpeaker = { CallManager.setSpeaker(!speakerOn) },
                        onHoldToggle = { CallManager.toggleHold(current) },
                        onRecord = toggleRecording,
                        onSoundboard = { showSoundboard = true },
                    )
                    Spacer(Modifier.height(28.dp))
                    EndCallButton(current)
                }
            }
        }
    }

    if (showSoundboard) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSoundboard = false },
            sheetState = sheetState,
            containerColor = NColors.Black,
            contentColor = NColors.White,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText(stringResource(R.string.soundboard_title), Modifier.weight(1f), dotSize = 3.dp)
                    TextButton(onClick = { Soundboard.stop() }) {
                        Text(stringResource(R.string.soundboard_stop).uppercase(), style = NType.label, color = NColors.Red)
                    }
                }
                Spacer(Modifier.height(8.dp))
                SoundboardGrid(Modifier.fillMaxWidth().height(420.dp))
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showLegal) {
        LegalNoticeDialog(
            onAccept = {
                showLegal = false
                AppPrefs.legalAccepted.set(true)
                toggleRecording()
            },
            onDismiss = { showLegal = false },
        )
    }
}

@Composable
private fun ticker(active: Boolean): Long {
    val now by produceState(System.currentTimeMillis(), active) {
        while (active) {
            value = System.currentTimeMillis()
            delay(500)
        }
    }
    return now
}

@Composable
private fun stateLabel(snapshot: CallSnapshot?, ended: Boolean): String = stringResource(
    when {
        ended || snapshot == null -> R.string.incall_ended
        else -> when (snapshot.state) {
            Call.STATE_RINGING -> R.string.incall_incoming
            Call.STATE_DIALING -> R.string.incall_dialing
            Call.STATE_CONNECTING, Call.STATE_NEW, Call.STATE_PULLING_CALL -> R.string.incall_connecting
            Call.STATE_SELECT_PHONE_ACCOUNT -> R.string.incall_select_sim
            Call.STATE_HOLDING -> R.string.incall_on_hold
            Call.STATE_ACTIVE -> R.string.incall_active
            else -> R.string.incall_ended
        }
    }
)

@Composable
private fun IncomingActions(call: CallSnapshot) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LabeledButton(
            icon = Icons.Filled.CallEnd,
            label = stringResource(R.string.incall_decline),
            container = NColors.Red,
            content = NColors.White,
            onClick = { CallManager.hangup(call) },
        )
        LabeledButton(
            icon = Icons.Filled.Call,
            label = stringResource(R.string.incall_answer),
            container = NColors.White,
            content = NColors.Black,
            onClick = { CallManager.answer(call.call) },
        )
    }
}

@Composable
private fun SelectSim(call: CallSnapshot) {
    val context = LocalContext.current
    val sims = remember { CallActions.allSims(context) }
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        sims.forEach { sim ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { CallManager.selectAccount(call.call, sim.handle) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SimCard, null, tint = NColors.Red)
                Spacer(Modifier.width(12.dp))
                Text(sim.label, color = NColors.White, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        EndCallButton(call)
    }
}

@Composable
private fun EndCallButton(call: CallSnapshot) {
    Box(Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
        RoundIconButton(
            icon = Icons.Filled.CallEnd,
            contentDescription = stringResource(R.string.incall_hangup),
            onClick = { CallManager.hangup(call) },
            size = 76.dp,
            container = NColors.Red,
        )
    }
}

@Composable
private fun ControlsGrid(
    muted: Boolean,
    speakerOn: Boolean,
    onHold: Boolean,
    canHold: Boolean,
    recording: Boolean,
    onMute: () -> Unit,
    onKeypad: () -> Unit,
    onSpeaker: () -> Unit,
    onHoldToggle: () -> Unit,
    onRecord: () -> Unit,
    onSoundboard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Control(Icons.Filled.MicOff, stringResource(R.string.incall_mute), muted, onMute)
            Control(Icons.Filled.Dialpad, stringResource(R.string.incall_keypad), false, onKeypad)
            Control(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.incall_speaker), speakerOn, onSpeaker)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Control(Icons.Filled.Pause, stringResource(R.string.incall_hold), onHold, onHoldToggle, enabled = canHold)
            Control(
                Icons.Filled.FiberManualRecord, stringResource(R.string.incall_record), recording, onRecord,
                accent = true,
            )
            Control(Icons.Filled.LibraryMusic, stringResource(R.string.incall_soundboard), false, onSoundboard)
        }
    }
}

@Composable
private fun Control(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RoundIconButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            size = 64.dp,
            enabled = enabled,
            container = when {
                active && accent -> NColors.Red
                active -> NColors.White
                else -> NColors.SurfaceHigh
            },
            contentColor = when {
                active && !accent -> NColors.Black
                accent && !active -> NColors.Red
                else -> NColors.White
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label.uppercase(),
            style = NType.label.copy(fontSize = 9.sp, letterSpacing = 1.sp),
            color = if (active) NColors.White else NColors.Gray,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LabeledButton(
    icon: ImageVector,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundIconButton(icon, label, onClick, size = 76.dp, container = container, contentColor = content)
        Spacer(Modifier.height(10.dp))
        SectionLabel(label, color = NColors.White)
    }
}

/** Turns the screen off while the phone is held to the ear. */
@Composable
private fun ProximityLock(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = if (enabled && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "nothingcall:proximity").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } else {
            null
        }
        onDispose {
            lock?.let { if (it.isHeld) it.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) }
        }
    }
}
