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

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.ContactsContract
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.data.Contact
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import com.real3rr0r303yt.nothingcall.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingcall.ui.components.Keypad
import com.real3rr0r303yt.nothingcall.ui.components.LetterAvatar
import com.real3rr0r303yt.nothingcall.ui.components.RoundIconButton
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.components.dotGrid
import com.real3rr0r303yt.nothingcall.ui.theme.NColors

private val DTMF_TONES = mapOf(
    '0' to ToneGenerator.TONE_DTMF_0, '1' to ToneGenerator.TONE_DTMF_1, '2' to ToneGenerator.TONE_DTMF_2,
    '3' to ToneGenerator.TONE_DTMF_3, '4' to ToneGenerator.TONE_DTMF_4, '5' to ToneGenerator.TONE_DTMF_5,
    '6' to ToneGenerator.TONE_DTMF_6, '7' to ToneGenerator.TONE_DTMF_7, '8' to ToneGenerator.TONE_DTMF_8,
    '9' to ToneGenerator.TONE_DTMF_9, '*' to ToneGenerator.TONE_DTMF_S, '#' to ToneGenerator.TONE_DTMF_P,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    number: String,
    onNumberChange: (String) -> Unit,
    contacts: List<Contact>,
    onCall: (String) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val tonesEnabled by AppPrefs.dialpadTones.flow.collectAsState()
    val privateContacts by ContactsRepository.privateContacts.collectAsState()

    val toneGenerator = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGenerator?.release() } }

    val digits = number.filter { it.isDigit() }
    val suggestions = remember(digits, contacts, privateContacts) {
        if (digits.length < 2) emptyList()
        else (privateContacts + contacts).mapNotNull { c ->
            c.numbers.firstOrNull { it.filter(Char::isDigit).contains(digits) }?.let { c to it }
        }.take(3)
    }

    fun press(c: Char) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (tonesEnabled) DTMF_TONES[c]?.let { toneGenerator?.startTone(it, 120) }
        onNumberChange(number + c)
    }

    Column(Modifier.fillMaxSize().dotGrid()) {
        // Number display
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (number.isEmpty()) {
                    SectionLabel(stringResource(R.string.dialer_hint))
                } else {
                    DotMatrixText(
                        number,
                        Modifier.fillMaxWidth(),
                        dotSize = 6.dp,
                        contentAlignment = Alignment.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.heightIn(min = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    suggestions.forEach { (contact, num) ->
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .clickable { onCall(num) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LetterAvatar(contact.name, size = 28.dp, accent = contact.isPrivate)
                            Spacer(Modifier.size(10.dp))
                            Text(contact.name, color = NColors.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.size(8.dp))
                            Text(num, color = NColors.Gray, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                    if (number.isNotEmpty() && suggestions.isEmpty()) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                                        .putExtra(ContactsContract.Intents.Insert.PHONE, number)
                                    runCatching { context.startActivity(intent) }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, tint = NColors.Red, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.dialer_add_contact), color = NColors.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Keypad(
            onKey = ::press,
            onLongKey = { c ->
                if (c == '0') {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onNumberChange(number + '+')
                    true
                } else {
                    false
                }
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(64.dp))
            RoundIconButton(
                icon = Icons.Filled.Call,
                contentDescription = stringResource(R.string.dialer_call),
                onClick = { onCall(number) },
                size = 72.dp,
                container = NColors.Red,
                enabled = number.isNotBlank(),
            )
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = number.isNotEmpty(),
                        onClick = { onNumberChange(number.dropLast(1)) },
                        onLongClick = { onNumberChange("") },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (number.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.dialer_delete),
                        tint = NColors.Gray,
                    )
                }
            }
        }
    }
}
