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

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.call.CallActions
import com.real3rr0r303yt.nothingcall.call.SimOption
import com.real3rr0r303yt.nothingcall.data.Contact
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import com.real3rr0r303yt.nothingcall.ui.components.NCard
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.ui.theme.NType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Tab(@StringRes val label: Int, val icon: ImageVector) {
    KEYPAD(R.string.tab_keypad, Icons.Filled.Dialpad),
    RECENTS(R.string.tab_recents, Icons.Filled.History),
    CONTACTS(R.string.tab_contacts, Icons.Filled.Person),
    RECORDINGS(R.string.tab_recordings, Icons.Filled.GraphicEq),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

val requiredPermissions: List<String> = buildList {
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.READ_CALL_LOG)
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

fun missingPermissions(context: Context) = requiredPermissions.filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
}

/** Places calls and asks for the SIM when the device has several and no default. */
@Stable
class CallLauncher(private val context: Context) {
    var pending by mutableStateOf<Pair<String, List<SimOption>>?>(null)

    fun call(number: String) {
        if (number.isBlank()) return
        val sims = CallActions.simChoices(context)
        if (sims.size > 1) pending = number to sims else CallActions.placeCall(context, number)
    }
}

@Composable
fun MainScreen(dialRequest: MutableState<String?>) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.KEYPAD) }
    var number by rememberSaveable { mutableStateOf("") }
    var resumeTick by remember { mutableIntStateOf(0) }
    val launcher = remember { CallLauncher(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }

    LaunchedEffect(dialRequest.value) {
        dialRequest.value?.let {
            number = it
            tab = Tab.KEYPAD
            dialRequest.value = null
        }
    }

    val missing = remember(resumeTick) { missingPermissions(context) }
    val isDefault = remember(resumeTick) { CallActions.isDefaultDialer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resumeTick++ }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resumeTick++ }
    val requestRole: () -> Unit = {
        context.getSystemService(RoleManager::class.java)?.let { rm ->
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        }
    }

    val contacts by produceState(initialValue = emptyList<Contact>(), resumeTick, missing.size) {
        value = withContext(Dispatchers.IO) { ContactsRepository.loadSystem(context) }
    }

    Scaffold(
        containerColor = NColors.Black,
        bottomBar = {
            NavigationBar(containerColor = NColors.Black, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = stringResource(t.label)) },
                        label = {
                            Text(
                                stringResource(t.label).uppercase(),
                                style = NType.label.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                maxLines = 1,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NColors.Black,
                            selectedTextColor = NColors.White,
                            indicatorColor = NColors.White,
                            unselectedIconColor = NColors.Gray,
                            unselectedTextColor = NColors.Gray,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!isDefault && tab != Tab.SETTINGS) {
                Banner(
                    title = stringResource(R.string.banner_default_title),
                    text = stringResource(R.string.banner_default_text),
                    action = stringResource(R.string.banner_default_action),
                    onAction = requestRole,
                )
            } else if (missing.isNotEmpty() && tab != Tab.SETTINGS) {
                Banner(
                    title = stringResource(R.string.banner_perm_title),
                    text = stringResource(R.string.banner_perm_text),
                    action = stringResource(R.string.banner_perm_action),
                    onAction = { permissionLauncher.launch(missing.toTypedArray()) },
                )
            }
            Column(Modifier.weight(1f)) {
                when (tab) {
                    Tab.KEYPAD -> DialerScreen(
                        number = number,
                        onNumberChange = { number = it },
                        contacts = contacts,
                        onCall = launcher::call,
                    )
                    Tab.RECENTS -> RecentsScreen(refreshKey = resumeTick, onCall = launcher::call)
                    Tab.CONTACTS -> ContactsScreen(contacts = contacts, onCall = launcher::call)
                    Tab.RECORDINGS -> RecordingsScreen()
                    Tab.SETTINGS -> SettingsScreen(isDefaultDialer = isDefault, onRequestDefault = requestRole)
                }
            }
        }
    }

    launcher.pending?.let { (pendingNumber, sims) ->
        SimChooserDialog(
            sims = sims,
            onPick = { sim ->
                launcher.pending = null
                CallActions.placeCall(context, pendingNumber, sim.handle)
            },
            onDismiss = { launcher.pending = null },
        )
    }
}

@Composable
private fun Banner(title: String, text: String, action: String, onAction: () -> Unit) {
    NCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), borderColor = NColors.Red) {
        SectionLabel(title, color = NColors.Red)
        Spacer(Modifier.height(6.dp))
        Text(text, color = NColors.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAction) { Text(action, color = NColors.White) }
    }
}

@Composable
fun SimChooserDialog(sims: List<SimOption>, onPick: (SimOption) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NColors.SurfaceHigh,
        title = { Text(stringResource(R.string.choose_sim)) },
        text = {
            Column {
                sims.forEach { sim ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(sim) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.SimCard, null, tint = NColors.Red, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(sim.label, color = NColors.White)
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
