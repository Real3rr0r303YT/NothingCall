/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * "System mode": when NothingCall is installed as a privileged system app (e.g. via the bundled
 * Magisk module), Android grants permissions that regular apps can never get:
 *  - CAPTURE_AUDIO_OUTPUT  → record BOTH sides of a call (AudioSource.VOICE_CALL)
 *  - CALL_AUDIO_INTERCEPTION (Android 14+) → inject soundboard audio straight into the call uplink
 */
object Privileged {
    const val CAPTURE_AUDIO_OUTPUT = "android.permission.CAPTURE_AUDIO_OUTPUT"
    const val CALL_AUDIO_INTERCEPTION = "android.permission.CALL_AUDIO_INTERCEPTION"

    private fun granted(context: Context, permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Both call sides can be recorded. */
    fun canRecordCallAudio(context: Context) = granted(context, CAPTURE_AUDIO_OUTPUT)

    /** Soundboard clips can be injected into the uplink (the other side hears them cleanly). */
    fun canInjectCallAudio(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && granted(context, CALL_AUDIO_INTERCEPTION)

    fun isSystemMode(context: Context) = canRecordCallAudio(context)
}
