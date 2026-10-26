/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.call

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Immutable view of a telecom [Call] for the UI. */
data class CallSnapshot(
    val call: Call,
    val state: Int,
    val number: String,
    val name: String?,
    val connectTimeMillis: Long,
    val canHold: Boolean,
) {
    val title: String? get() = name?.takeIf { it.isNotBlank() } ?: number.takeIf { it.isNotBlank() }
}

@Suppress("DEPRECATION")
fun Call.stateCompat(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) details.state else state

/**
 * Single source of truth for all calls handed to us by Telecom through [NothingInCallService].
 * Everything runs on the main thread (Telecom delivers callbacks there).
 */
object CallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rawCalls = mutableListOf<Call>()
    private val names = ConcurrentHashMap<String, String>()

    private val _calls = MutableStateFlow<List<CallSnapshot>>(emptyList())
    val calls: StateFlow<List<CallSnapshot>> = _calls.asStateFlow()

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState.asStateFlow()

    private var service: InCallService? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
    }

    fun attach(service: InCallService) {
        this.service = service
    }

    fun detach(service: InCallService) {
        if (this.service === service) this.service = null
    }

    fun onCallAdded(context: Context, call: Call) {
        rawCalls += call
        call.registerCallback(callback)
        publish()
        val number = numberOf(call)
        if (number.isNotBlank() && !names.containsKey(number)) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { ContactsRepository.lookupName(context, number) }
                if (name != null) {
                    names[number] = name
                    publish()
                }
            }
        }
    }

    fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        rawCalls.remove(call)
        publish()
    }

    fun onAudioStateChanged(state: CallAudioState?) {
        _audioState.value = state
    }

    private fun numberOf(call: Call): String = call.details?.handle?.schemeSpecificPart.orEmpty()

    private fun publish() {
        _calls.value = rawCalls.map { call ->
            val details = call.details
            val number = numberOf(call)
            val systemName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                details?.contactDisplayName
            } else {
                details?.callerDisplayName
            }
            CallSnapshot(
                call = call,
                state = call.stateCompat(),
                number = number,
                name = names[number] ?: systemName,
                connectTimeMillis = details?.connectTimeMillis ?: 0L,
                canHold = details?.can(Call.Details.CAPABILITY_HOLD) == true,
            )
        }
    }

    /** The call the in-call screen should focus on. */
    fun primary(list: List<CallSnapshot> = _calls.value): CallSnapshot? =
        list.firstOrNull { it.state == Call.STATE_RINGING }
            ?: list.firstOrNull {
                it.state == Call.STATE_DIALING || it.state == Call.STATE_CONNECTING ||
                    it.state == Call.STATE_SELECT_PHONE_ACCOUNT || it.state == Call.STATE_NEW ||
                    it.state == Call.STATE_PULLING_CALL
            }
            ?: list.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: list.firstOrNull { it.state == Call.STATE_HOLDING }
            ?: list.firstOrNull()

    fun answer(call: Call) = call.answer(VideoProfile.STATE_AUDIO_ONLY)

    fun answerRinging() {
        _calls.value.firstOrNull { it.state == Call.STATE_RINGING }?.let { answer(it.call) }
    }

    fun rejectRinging() {
        _calls.value.firstOrNull { it.state == Call.STATE_RINGING }?.call?.reject(false, null)
    }

    fun hangup(snapshot: CallSnapshot) {
        if (snapshot.state == Call.STATE_RINGING) snapshot.call.reject(false, null)
        else snapshot.call.disconnect()
    }

    fun hangupPrimary() {
        primary()?.let { hangup(it) }
    }

    fun toggleHold(snapshot: CallSnapshot) {
        if (snapshot.state == Call.STATE_HOLDING) snapshot.call.unhold() else snapshot.call.hold()
    }

    /** Resumes the held call; Telecom automatically holds the active one. */
    fun swap() {
        _calls.value.firstOrNull { it.state == Call.STATE_HOLDING }?.call?.unhold()
    }

    fun selectAccount(call: Call, handle: PhoneAccountHandle) = call.phoneAccountSelected(handle, false)

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    @Suppress("DEPRECATION")
    fun setSpeaker(on: Boolean) {
        service?.setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }

    val isSpeakerOn: Boolean get() = _audioState.value?.route == CallAudioState.ROUTE_SPEAKER

    fun playDtmf(call: Call, digit: Char) {
        call.playDtmfTone(digit)
        scope.launch {
            delay(160)
            call.stopDtmfTone()
        }
    }
}
