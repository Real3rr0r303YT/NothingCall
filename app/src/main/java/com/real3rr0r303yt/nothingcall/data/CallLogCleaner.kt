/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat

/** OPSEC: removes call history from the system call log after the chosen retention time. */
object CallLogCleaner {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns the number of deleted entries. */
    fun apply(context: Context): Int {
        val cutoff = when (AppPrefs.callLogRetention.value) {
            AppPrefs.RETENTION_IMMEDIATE -> Long.MAX_VALUE
            AppPrefs.RETENTION_DAY -> System.currentTimeMillis() - DAY_MS
            AppPrefs.RETENTION_WEEK -> System.currentTimeMillis() - 7 * DAY_MS
            else -> return 0
        }
        return wipeOlderThan(context, cutoff)
    }

    fun wipeOlderThan(context: Context, cutoff: Long): Int {
        if (!hasPermission(context)) return 0
        return try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI, "${CallLog.Calls.DATE} < ?", arrayOf(cutoff.toString())
            )
        } catch (e: Exception) {
            0
        }
    }
}
