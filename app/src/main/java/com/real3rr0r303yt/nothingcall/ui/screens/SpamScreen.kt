/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.ui.screens

import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.spam.BlockRepository
import com.real3rr0r303yt.nothingcall.spam.SpamFilter
import com.real3rr0r303yt.nothingcall.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingcall.ui.components.NSwitch
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.components.SettingRow
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.ui.theme.NType
import com.real3rr0r303yt.nothingcall.util.AppScope
import com.real3rr0r303yt.nothingcall.util.Format
import kotlinx.coroutines.launch

/** Offline spam protection: filters, block list and the encrypted blocked-calls log. */
@Composable
fun SpamScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    val blockHidden by AppPrefs.blockHidden.flow.collectAsState()
    val blockUnknown by AppPrefs.blockUnknown.flow.collectAsState()
    val blockUnverified by AppPrefs.blockUnverified.flow.collectAsState()
    val silence by AppPrefs.silenceInsteadOfReject.flow.collectAsState()
    val rules by BlockRepository.rules.collectAsState()
    val log by BlockRepository.log.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var roleTick by remember { mutableIntStateOf(0) }

    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val screeningRoleHeld = remember(roleTick) {
        roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true ||
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { roleTick++ }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = NColors.White)
            }
            DotMatrixText(stringResource(R.string.settings_spam), Modifier.weight(1f), dotSize = 3.5.dp)
        }
        Text(
            stringResource(R.string.spam_info),
            color = NColors.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (!screeningRoleHeld && roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
            OutlinedButton(
                onClick = { roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Shield, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.spam_role), color = NColors.White)
            }
        }

        Group(stringResource(R.string.settings_general))
        SettingRow(
            title = stringResource(R.string.spam_block_hidden),
            icon = Icons.Filled.PhoneDisabled,
            onClick = { AppPrefs.blockHidden.set(!blockHidden) },
            trailing = { NSwitch(blockHidden) { AppPrefs.blockHidden.set(it) } },
        )
        SettingRow(
            title = stringResource(R.string.spam_block_unknown),
            icon = Icons.Filled.PersonOff,
            onClick = { AppPrefs.blockUnknown.set(!blockUnknown) },
            trailing = { NSwitch(blockUnknown) { AppPrefs.blockUnknown.set(it) } },
        )
        SettingRow(
            title = stringResource(R.string.spam_block_unverified),
            icon = Icons.Filled.VerifiedUser,
            onClick = { AppPrefs.blockUnverified.set(!blockUnverified) },
            trailing = { NSwitch(blockUnverified) { AppPrefs.blockUnverified.set(it) } },
        )
        SettingRow(
            title = stringResource(R.string.spam_silence),
            subtitle = stringResource(R.string.spam_silence_sub),
            icon = Icons.Filled.NotificationsOff,
            onClick = { AppPrefs.silenceInsteadOfReject.set(!silence) },
            trailing = { NSwitch(silence) { AppPrefs.silenceInsteadOfReject.set(it) } },
        )

        Group(stringResource(R.string.spam_rules))
        if (rules.isEmpty()) {
            Text(
                stringResource(R.string.spam_rules_empty), color = NColors.Gray, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
        rules.forEach { rule ->
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Block, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(14.dp))
                Text(rule.pattern, style = NType.mono, color = NColors.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { AppScope.io.launch { runCatching { BlockRepository.removeRule(context, rule) } } }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.delete), tint = NColors.Gray)
                }
            }
        }
        OutlinedButton(
            onClick = { showAdd = true },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.spam_add_rule), color = NColors.White)
        }

        Row(
            Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { Group(stringResource(R.string.spam_log)) }
            if (log.isNotEmpty()) {
                TextButton(onClick = { AppScope.io.launch { runCatching { BlockRepository.clearLog(context) } } }) {
                    Text(stringResource(R.string.spam_log_clear).uppercase(), style = NType.label, color = NColors.Gray)
                }
            }
        }
        if (log.isEmpty()) {
            Text(
                stringResource(R.string.spam_log_empty), color = NColors.Gray, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
        log.take(100).forEach { entry ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    entry.number.ifBlank { stringResource(R.string.unknown_number) },
                    color = NColors.White, fontSize = 15.sp,
                )
                SectionLabel("${reasonLabel(entry.reason)} · ${Format.dateTime(context, entry.timestamp)}")
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showAdd) {
        var pattern by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = NColors.SurfaceHigh,
            title = { Text(stringResource(R.string.spam_add_rule)) },
            text = {
                Column {
                    Text(stringResource(R.string.spam_rule_hint), color = NColors.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { v -> pattern = v.filter { it.isDigit() || it == '+' || it == '*' } },
                        label = { Text(stringResource(R.string.spam_rule_label)) },
                        singleLine = true,
                        colors = nothingFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = pattern
                        showAdd = false
                        AppScope.io.launch { runCatching { BlockRepository.addRule(context, value) } }
                    },
                    enabled = pattern.any { it.isDigit() },
                ) { Text(stringResource(R.string.save), color = NColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel), color = NColors.White) }
            },
        )
    }
}

@Composable
private fun reasonLabel(reason: String): String = stringResource(
    when (reason) {
        SpamFilter.REASON_HIDDEN -> R.string.spam_reason_hidden
        SpamFilter.REASON_UNKNOWN -> R.string.spam_reason_unknown
        SpamFilter.REASON_SPOOFED -> R.string.spam_reason_spoofed
        else -> R.string.spam_reason_list
    }
)
