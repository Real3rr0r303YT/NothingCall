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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.real3rr0r303yt.nothingcall.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.ROOT, "%d:%02d", m, s)
        }
    }

    fun dateTime(context: Context, timestamp: Long): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(timestamp))
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        val sameYear = then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && dayDiff == 0 -> "${context.getString(R.string.today)}, $time"
            sameYear && dayDiff == 1 -> "${context.getString(R.string.yesterday)}, $time"
            else -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
                .format(Date(timestamp))
        }
    }
}

fun Context.toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(R.string.no_app_found)
    }
}
