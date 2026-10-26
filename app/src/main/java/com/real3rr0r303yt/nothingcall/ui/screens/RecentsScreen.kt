/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

@file:Suppress("DEPRECATION")

package com.real3rr0r303yt.nothingcall.ui.screens

import android.provider.CallLog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.CallLogEntry
import com.real3rr0r303yt.nothingcall.data.CallLogRepository
import com.real3rr0r303yt.nothingcall.ui.components.EmptyState
import com.real3rr0r303yt.nothingcall.ui.components.ScreenHeader
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecentsScreen(refreshKey: Int, onCall: (String) -> Unit) {
    val context = LocalContext.current
    val entries by produceState<List<CallLogEntry>?>(initialValue = null, refreshKey) {
        value = withContext(Dispatchers.IO) { CallLogRepository.load(context) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.tab_recents))
        val list = entries
        when {
            !CallLogRepository.hasPermission(context) -> EmptyState(stringResource(R.string.permission_missing))
            list == null -> Unit
            list.isEmpty() -> EmptyState(stringResource(R.string.recents_empty))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { entry -> RecentRow(entry, onCall) }
            }
        }
    }
}

@Composable
private fun RecentRow(entry: CallLogEntry, onCall: (String) -> Unit) {
    val context = LocalContext.current
    val missed = entry.type == CallLog.Calls.MISSED_TYPE
    val (icon, typeLabel) = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Filled.CallReceived to R.string.call_type_incoming
        CallLog.Calls.OUTGOING_TYPE -> Icons.Filled.CallMade to R.string.call_type_outgoing
        CallLog.Calls.MISSED_TYPE -> Icons.Filled.CallMissed to R.string.call_type_missed
        CallLog.Calls.REJECTED_TYPE -> Icons.Filled.CallMissed to R.string.call_type_rejected
        CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block to R.string.call_type_blocked
        else -> Icons.Filled.CallReceived to R.string.call_type_incoming
    }
    val title = entry.name ?: entry.number.ifBlank { stringResource(R.string.unknown_number) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.number.isNotBlank()) { onCall(entry.number) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (missed) NColors.Red else NColors.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (missed) NColors.Red else NColors.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildString {
                append(stringResource(typeLabel))
                append(" · ")
                append(Format.dateTime(context, entry.date))
                if (entry.durationSec > 0) {
                    append(" · ")
                    append(Format.duration(entry.durationSec * 1000))
                }
            }
            Text(meta, color = NColors.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (entry.number.isNotBlank()) {
            IconButton(onClick = { onCall(entry.number) }) {
                Icon(Icons.Filled.Call, stringResource(R.string.dialer_call), tint = NColors.White)
            }
        }
    }
}
