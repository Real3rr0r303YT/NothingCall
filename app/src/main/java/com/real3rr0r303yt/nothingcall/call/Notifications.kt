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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.real3rr0r303yt.nothingcall.R

object Notifications {
    const val CHANNEL_INCOMING = "nc_incoming"
    const val CHANNEL_ONGOING = "nc_ongoing"
    const val CHANNEL_RECORDING = "nc_recording"
    const val ID_CALL = 4201
    const val ID_RECORDING = 4202
    private const val RED = 0xFFD71921.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val incoming = NotificationChannel(
            CHANNEL_INCOMING, context.getString(R.string.notif_channel_incoming),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            // Telecom plays the ringtone itself.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val ongoing = NotificationChannel(
            CHANNEL_ONGOING, context.getString(R.string.notif_channel_ongoing),
            NotificationManager.IMPORTANCE_LOW
        )
        val recording = NotificationChannel(
            CHANNEL_RECORDING, context.getString(R.string.notif_channel_recording),
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannels(listOf(incoming, ongoing, recording))
    }

    private fun inCallIntent(context: Context, answer: Boolean): PendingIntent {
        val intent = Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (answer) intent.action = InCallActivity.ACTION_ANSWER
        return PendingIntent.getActivity(
            context, if (answer) 1 else 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun showIncoming(context: Context, title: String) {
        val person = Person.Builder().setName(title).setImportant(true).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(RED)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.incall_incoming))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(inCallIntent(context, answer = false))
            .setFullScreenIntent(inCallIntent(context, answer = false), true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    actionIntent(context, CallActionReceiver.ACTION_DECLINE, 10),
                    inCallIntent(context, answer = true)
                )
            )
            .build()
        notify(context, ID_CALL, notification)
    }

    fun showOngoing(context: Context, title: String, connectTime: Long) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(RED)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_ongoing))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(inCallIntent(context, answer = false))
            .addAction(
                R.drawable.ic_stat_call, context.getString(R.string.incall_hangup),
                actionIntent(context, CallActionReceiver.ACTION_HANGUP, 11)
            )
        if (connectTime > 0) builder.setWhen(connectTime).setUsesChronometer(true).setShowWhen(true)
        notify(context, ID_CALL, builder.build())
    }

    fun cancelCall(context: Context) = NotificationManagerCompat.from(context).cancel(ID_CALL)

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission revoked – the in-call screen still works.
        } catch (e: IllegalArgumentException) {
            // Some OEM builds reject CallStyle without a foreground service; ignore.
        }
    }
}
