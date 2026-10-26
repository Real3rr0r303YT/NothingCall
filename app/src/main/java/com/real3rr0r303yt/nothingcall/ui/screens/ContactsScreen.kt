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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.Contact
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import com.real3rr0r303yt.nothingcall.ui.components.EmptyState
import com.real3rr0r303yt.nothingcall.ui.components.LetterAvatar
import com.real3rr0r303yt.nothingcall.ui.components.ScreenHeader
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.util.AppScope
import kotlinx.coroutines.launch

@Composable
fun nothingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NColors.White,
    unfocusedBorderColor = NColors.Outline,
    cursorColor = NColors.Red,
    focusedLabelColor = NColors.White,
    unfocusedLabelColor = NColors.Gray,
    focusedContainerColor = NColors.Black,
    unfocusedContainerColor = NColors.Black,
)

@Composable
fun ContactsScreen(contacts: List<Contact>, onCall: (String) -> Unit) {
    val context = LocalContext.current
    val privateContacts by ContactsRepository.privateContacts.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var chooseFor by remember { mutableStateOf<Contact?>(null) }
    var deleteCandidate by remember { mutableStateOf<Contact?>(null) }

    fun matches(c: Contact) = query.isBlank() ||
        c.name.contains(query, ignoreCase = true) ||
        c.numbers.any { it.filter(Char::isDigit).contains(query.filter(Char::isDigit).ifEmpty { "\u0000" }) }

    val filteredPrivate = privateContacts.filter(::matches)
    val filtered = contacts.filter(::matches)
    val favorites = filtered.filter { it.starred }

    val open: (Contact) -> Unit = { c ->
        if (c.numbers.size == 1) onCall(c.numbers.first()) else chooseFor = c
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(stringResource(R.string.tab_contacts))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.contacts_search), color = NColors.Gray) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = NColors.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = nothingFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                if (filteredPrivate.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.contacts_private)) }
                    items(filteredPrivate, key = { it.key }) { c ->
                        ContactRow(c, onClick = { open(c) }, onLongClick = { deleteCandidate = c })
                    }
                }
                if (favorites.isNotEmpty() && query.isBlank()) {
                    item { SectionHeader(stringResource(R.string.contacts_favorites)) }
                    items(favorites, key = { "fav:" + it.key }) { c -> ContactRow(c, onClick = { open(c) }) }
                }
                if (filtered.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.contacts_all)) }
                    items(filtered, key = { it.key }) { c -> ContactRow(c, onClick = { open(c) }) }
                }
                if (filtered.isEmpty() && filteredPrivate.isEmpty()) {
                    item {
                        EmptyState(
                            stringResource(
                                if (ContactsRepository.hasPermission(context)) R.string.contacts_empty
                                else R.string.permission_missing
                            )
                        )
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            containerColor = NColors.White,
            contentColor = NColors.Black,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, stringResource(R.string.contacts_add_private))
        }
    }

    if (showAdd) {
        AddPrivateContactDialog(
            onSave = { name, number ->
                showAdd = false
                AppScope.io.launch { runCatching { ContactsRepository.addPrivate(context, name, number) } }
            },
            onDismiss = { showAdd = false },
        )
    }

    chooseFor?.let { c ->
        AlertDialog(
            onDismissRequest = { chooseFor = null },
            containerColor = NColors.SurfaceHigh,
            title = { Text(stringResource(R.string.choose_number)) },
            text = {
                Column {
                    c.numbers.forEach { num ->
                        TextButton(onClick = { chooseFor = null; onCall(num) }) {
                            Text(num, color = NColors.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { chooseFor = null }) { Text(stringResource(R.string.cancel), color = NColors.White) }
            },
        )
    }

    deleteCandidate?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = NColors.SurfaceHigh,
            title = { Text(c.name) },
            text = { Text(stringResource(R.string.contact_delete_confirm), color = NColors.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    AppScope.io.launch { runCatching { ContactsRepository.deletePrivate(context, c) } }
                }) { Text(stringResource(R.string.delete), color = NColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.cancel), color = NColors.White) }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    SectionLabel(text, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterAvatar(contact.name, accent = contact.isPrivate)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.name, color = NColors.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                contact.numbers.joinToString(" · "),
                color = NColors.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            contact.isPrivate -> Icon(Icons.Filled.Lock, null, tint = NColors.Red, modifier = Modifier.size(16.dp))
            contact.starred -> Icon(Icons.Filled.Star, null, tint = NColors.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddPrivateContactDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NColors.SurfaceHigh,
        title = { Text(stringResource(R.string.contacts_add_private)) },
        text = {
            Column {
                Text(stringResource(R.string.contacts_private_hint), color = NColors.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name)) },
                    singleLine = true, colors = nothingFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = number, onValueChange = { number = it },
                    label = { Text(stringResource(R.string.contact_number)) },
                    singleLine = true, colors = nothingFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, number) },
                enabled = name.isNotBlank() && number.isNotBlank(),
            ) { Text(stringResource(R.string.save), color = NColors.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = NColors.White) }
        },
    )
}
