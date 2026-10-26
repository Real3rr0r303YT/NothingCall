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

data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val durationSec: Long,
)

/** Reads the system call log. Nothing is copied, synced or uploaded. */
object CallLogRepository {
    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    fun load(context: Context, limit: Int = 300): List<CallLogEntry> {
        if (!hasPermission(context)) return emptyList()
        val result = ArrayList<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                while (c.moveToNext() && result.size < limit) {
                    result += CallLogEntry(
                        id = c.getLong(0),
                        number = c.getString(1).orEmpty(),
                        name = c.getString(2)?.takeIf { it.isNotBlank() },
                        type = c.getInt(3),
                        date = c.getLong(4),
                        durationSec = c.getLong(5),
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return result
    }
}
