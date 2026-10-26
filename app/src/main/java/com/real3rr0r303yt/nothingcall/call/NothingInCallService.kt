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

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.recording.RecordingService
import com.real3rr0r303yt.nothingcall.soundboard.Soundboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Bound by Telecom while NothingCall is the default phone app. Forwards every call to
 * [CallManager] and keeps the call notification in sync.
 */
class NothingInCallService : InCallService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastNotificationKey: String? = null

    override fun onCreate() {
        super.onCreate()
        CallManager.attach(this)
        scope.launch { CallManager.calls.collect { updateNotification(it) } }
    }

    override fun onDestroy() {
        scope.cancel()
        CallManager.detach(this)
        Notifications.cancelCall(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(applicationContext, call)
        // Incoming calls are surfaced via the full-screen notification; everything else opens the UI.
        if (call.stateCompat() != Call.STATE_RINGING) launchInCallUi()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.onCallRemoved(call)
        if (CallManager.calls.value.isEmpty()) {
            RecordingService.stop(this)
            Soundboard.stop()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallManager.onAudioStateChanged(audioState)
    }

    private fun launchInCallUi() {
        try {
            startActivity(Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Background activity start blocked – the notification still leads to the call.
        }
    }

    private fun updateNotification(calls: List<CallSnapshot>) {
        val primary = CallManager.primary(calls)
        if (primary == null || primary.state == Call.STATE_DISCONNECTED) {
            Notifications.cancelCall(this)
            lastNotificationKey = null
            return
        }
        val title = primary.title ?: getString(R.string.unknown_number)
        val ringing = primary.state == Call.STATE_RINGING
        val key = "$ringing|$title|${primary.connectTimeMillis}"
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        if (ringing) Notifications.showIncoming(this, title)
        else Notifications.showOngoing(this, title, primary.connectTimeMillis)
    }
}
