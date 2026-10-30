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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.BuildConfig
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import com.real3rr0r303yt.nothingcall.data.LanguageManager
import com.real3rr0r303yt.nothingcall.recording.RecordingRepository
import com.real3rr0r303yt.nothingcall.security.CryptoManager
import com.real3rr0r303yt.nothingcall.soundboard.Soundboard
import com.real3rr0r303yt.nothingcall.ui.components.LegalNoticeDialog
import com.real3rr0r303yt.nothingcall.ui.components.NCard
import com.real3rr0r303yt.nothingcall.ui.components.NSwitch
import com.real3rr0r303yt.nothingcall.ui.components.ScreenHeader
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.components.SettingRow
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.util.AppScope
import com.real3rr0r303yt.nothingcall.util.openUrl
import com.real3rr0r303yt.nothingcall.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SUPPORT_URL = "https://buymeacoffee.com/3rr0r303YT"
const val SOURCE_URL = "https://github.com/Real3rr0r303YT/NothingCall"
const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun SettingsScreen(isDefaultDialer: Boolean, onRequestDefault: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("main") }
    if (page == "soundboard") {
        SoundboardManageScreen(onBack = { page = "main" })
    } else {
        SettingsMain(isDefaultDialer, onRequestDefault, onOpenSoundboard = { page = "soundboard" })
    }
}

@Composable
private fun SettingsMain(isDefaultDialer: Boolean, onRequestDefault: () -> Unit, onOpenSoundboard: () -> Unit) {
    val context = LocalContext.current
    val autoRecord by AppPrefs.autoRecord.flow.collectAsState()
    val source by AppPrefs.recordingSource.flow.collectAsState()
    val legalAccepted by AppPrefs.legalAccepted.flow.collectAsState()
    val speaker by AppPrefs.soundboardSpeaker.flow.collectAsState()
    val tones by AppPrefs.dialpadTones.flow.collectAsState()

    var showLanguage by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    var showWipe by remember { mutableStateOf(false) }

    val languageLabel = when (LanguageManager.current()) {
        "de" -> stringResource(R.string.lang_de)
        "en" -> stringResource(R.string.lang_en)
        else -> stringResource(R.string.lang_system)
    }
    val sourceOptions = listOf(
        AppPrefs.SOURCE_AUTO to R.string.source_auto,
        AppPrefs.SOURCE_VOICE_CALL to R.string.source_voice_call,
        AppPrefs.SOURCE_VOICE_COMM to R.string.source_voice_comm,
        AppPrefs.SOURCE_MIC to R.string.source_mic,
    )
    val sourceLabel = stringResource(sourceOptions.firstOrNull { it.first == source }?.second ?: R.string.source_auto)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.tab_settings))

        Group(stringResource(R.string.settings_general))
        SettingRow(
            title = stringResource(R.string.settings_default_app),
            subtitle = stringResource(if (isDefaultDialer) R.string.settings_default_yes else R.string.settings_default_no),
            icon = Icons.Filled.Phone,
            onClick = if (isDefaultDialer) null else onRequestDefault,
            trailing = { if (isDefaultDialer) Icon(Icons.Filled.Check, null, tint = NColors.Red) },
        )
        SettingRow(
            title = stringResource(R.string.settings_language),
            subtitle = languageLabel,
            icon = Icons.Filled.Translate,
            onClick = { showLanguage = true },
        )
        SettingRow(
            title = stringResource(R.string.settings_dialpad_tones),
            icon = Icons.Filled.Dialpad,
            onClick = { AppPrefs.dialpadTones.set(!tones) },
            trailing = { NSwitch(tones) { AppPrefs.dialpadTones.set(it) } },
        )

        Group(stringResource(R.string.settings_recording))
        val toggleAuto: (Boolean) -> Unit = { enabled ->
            if (enabled && !legalAccepted) showLegal = true else AppPrefs.autoRecord.set(enabled)
        }
        SettingRow(
            title = stringResource(R.string.settings_auto_record),
            icon = Icons.Filled.FiberManualRecord,
            onClick = { toggleAuto(!autoRecord) },
            trailing = { NSwitch(autoRecord, toggleAuto) },
        )
        SettingRow(
            title = stringResource(R.string.settings_audio_source),
            subtitle = sourceLabel,
            icon = Icons.Filled.Mic,
            onClick = { showSource = true },
        )
        Text(
            stringResource(R.string.settings_recording_info),
            color = NColors.Gray, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        Group(stringResource(R.string.settings_soundboard))
        SettingRow(
            title = stringResource(R.string.settings_soundboard_manage),
            icon = Icons.Filled.LibraryMusic,
            onClick = onOpenSoundboard,
            trailing = { Icon(Icons.Filled.ChevronRight, null, tint = NColors.Gray) },
        )
        SettingRow(
            title = stringResource(R.string.settings_soundboard_speaker),
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = { AppPrefs.soundboardSpeaker.set(!speaker) },
            trailing = { NSwitch(speaker) { AppPrefs.soundboardSpeaker.set(it) } },
        )

        Group(stringResource(R.string.settings_privacy))
        NCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            listOf(
                R.string.privacy_point_1, R.string.privacy_point_2,
                R.string.privacy_point_3, R.string.privacy_point_4,
            ).forEach { res ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Icon(Icons.Filled.Shield, null, tint = NColors.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(res), color = NColors.White, fontSize = 13.sp)
                }
            }
        }
        SettingRow(
            title = stringResource(R.string.settings_wipe),
            icon = Icons.Filled.DeleteForever,
            onClick = { showWipe = true },
        )

        Group(stringResource(R.string.settings_about))
        SettingRow(
            title = stringResource(R.string.settings_version),
            subtitle = BuildConfig.VERSION_NAME,
            icon = Icons.Filled.Info,
        )
        SettingRow(
            title = stringResource(R.string.settings_license),
            subtitle = stringResource(R.string.settings_license_value),
            icon = Icons.Filled.Gavel,
            onClick = { context.openUrl(LICENSE_URL) },
        )
        SettingRow(
            title = stringResource(R.string.settings_source),
            subtitle = SOURCE_URL.removePrefix("https://"),
            icon = Icons.Filled.Code,
            onClick = { context.openUrl(SOURCE_URL) },
        )

        HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = NColors.Outline)

        // Discreet support section
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.settings_support_text),
                color = NColors.Gray, fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { context.openUrl(SUPPORT_URL) }) {
                Icon(Icons.Filled.LocalCafe, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.settings_support), color = NColors.White, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.settings_copyright))
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLanguage) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = listOf(
                LanguageManager.SYSTEM to stringResource(R.string.lang_system),
                "de" to stringResource(R.string.lang_de),
                "en" to stringResource(R.string.lang_en),
            ),
            selected = LanguageManager.current(),
            onSelect = {
                showLanguage = false
                LanguageManager.set(it)
            },
            onDismiss = { showLanguage = false },
        )
    }

    if (showSource) {
        ChoiceDialog(
            title = stringResource(R.string.settings_audio_source),
            options = sourceOptions.map { it.first to stringResource(it.second) },
            selected = source,
            onSelect = {
                showSource = false
                AppPrefs.recordingSource.set(it)
            },
            onDismiss = { showSource = false },
        )
    }

    if (showLegal) {
        LegalNoticeDialog(
            onAccept = {
                showLegal = false
                AppPrefs.legalAccepted.set(true)
                AppPrefs.autoRecord.set(true)
            },
            onDismiss = { showLegal = false },
        )
    }

    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            containerColor = NColors.SurfaceHigh,
            title = { Text(stringResource(R.string.settings_wipe)) },
            text = { Text(stringResource(R.string.settings_wipe_confirm), color = NColors.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    showWipe = false
                    AppScope.io.launch {
                        RecordingRepository.wipe(context)
                        Soundboard.wipe(context)
                        ContactsRepository.wipePrivate(context)
                        CryptoManager.destroyKeys()
                        withContext(Dispatchers.Main) { context.toast(R.string.settings_wiped) }
                    }
                }) { Text(stringResource(R.string.delete), color = NColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showWipe = false }) { Text(stringResource(R.string.cancel), color = NColors.White) }
            },
        )
    }
}

@Composable
private fun Group(title: String) {
    SectionLabel(title, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp), color = NColors.Red)
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NColors.SurfaceHigh,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = NColors.Red, unselectedColor = NColors.Gray),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(label, color = NColors.White)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = NColors.White) }
        },
    )
}
