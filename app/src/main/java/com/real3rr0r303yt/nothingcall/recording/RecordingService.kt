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

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.call.Notifications
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.util.AppScope
import com.real3rr0r303yt.nothingcall.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Foreground service (type microphone) that keeps recording while the screen is off. */
class RecordingService : Service() {

    data class State(val recording: Boolean = false, val startedAt: Long = 0L)

    private var recorder: CallRecorder? = null
    private var number: String = ""
    private var name: String? = null
    private var startedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent)
            ACTION_STOP -> finish()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun begin(intent: Intent) {
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(0xFFD71921.toInt())
            .setContentTitle(getString(R.string.notif_recording))
            .setContentText(getString(R.string.notif_recording_text))
            .setOngoing(true)
            .setSilent(true)
            .build()
        try {
            ServiceCompat.startForeground(
                this, Notifications.ID_RECORDING, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            )
        } catch (e: Exception) {
            toast(R.string.recording_failed)
            stopSelf()
            return
        }
        if (recorder != null) return

        number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        name = intent.getStringExtra(EXTRA_NAME)
        val r = CallRecorder(this)
        if (!r.start(AppPrefs.recordingSource.value)) {
            toast(R.string.recording_failed)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        recorder = r
        startedAt = System.currentTimeMillis()
        _state.value = State(recording = true, startedAt = startedAt)
    }

    private fun finish() {
        val r = recorder
        recorder = null
        _state.value = State()
        val file = r?.stop()
        if (file != null) {
            val app = applicationContext
            val num = number
            val nm = name
            val started = startedAt
            AppScope.io.launch {
                val ok = runCatching { RecordingRepository.add(app, file, num, nm, started) }.isSuccess
                withContext(Dispatchers.Main) {
                    app.toast(if (ok) R.string.recording_saved else R.string.recording_failed)
                }
            }
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (recorder != null) finish()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.real3rr0r303yt.nothingcall.recording.START"
        private const val ACTION_STOP = "com.real3rr0r303yt.nothingcall.recording.STOP"
        private const val EXTRA_NUMBER = "number"
        private const val EXTRA_NAME = "name"

        private val _state = MutableStateFlow(State())
        val state: StateFlow<State> = _state.asStateFlow()

        fun hasPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        fun start(context: Context, number: String, name: String?): Boolean {
            if (!hasPermission(context)) return false
            return try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, RecordingService::class.java)
                        .setAction(ACTION_START)
                        .putExtra(EXTRA_NUMBER, number)
                        .putExtra(EXTRA_NAME, name)
                )
                true
            } catch (e: Exception) {
                false
            }
        }

        fun stop(context: Context) {
            if (!_state.value.recording) return
            try {
                context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                // Service already gone.
            }
        }
    }
}
