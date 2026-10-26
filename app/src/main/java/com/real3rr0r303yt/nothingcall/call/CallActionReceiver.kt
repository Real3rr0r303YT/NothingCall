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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles "decline" / "hang up" taps on the call notification. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> CallManager.rejectRinging()
            ACTION_HANGUP -> CallManager.hangupPrimary()
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.real3rr0r303yt.nothingcall.action.DECLINE"
        const val ACTION_HANGUP = "com.real3rr0r303yt.nothingcall.action.HANGUP"
    }
}
