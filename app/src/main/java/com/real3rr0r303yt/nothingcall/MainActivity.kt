/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.security.AppLock
import com.real3rr0r303yt.nothingcall.ui.screens.LockScreen
import com.real3rr0r303yt.nothingcall.ui.screens.MainScreen
import com.real3rr0r303yt.nothingcall.ui.theme.NothingTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    /** Number handed over via ACTION_DIAL / tel: links. */
    private val dialRequest = mutableStateOf<String?>(null)

    private val locked = mutableStateOf(false)
    private var firstStart = true
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        lifecycleScope.launch {
            AppPrefs.secureScreen.flow.collect { AppLock.applySecureFlag(this@MainActivity, it) }
        }
        if (savedInstanceState == null) handleIntent(intent) else firstStart = false
        setContent {
            NothingTheme {
                val isLocked by locked
                if (isLocked) {
                    LockScreen(onUnlock = { promptUnlock() })
                } else {
                    MainScreen(dialRequest = dialRequest)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (AppLock.shouldLock(this, firstStart)) locked.value = true
        firstStart = false
    }

    override fun onResume() {
        super.onResume()
        if (locked.value) promptUnlock()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !prompting) AppLock.onBackground()
    }

    private fun promptUnlock() {
        if (prompting) return
        prompting = true
        AppLock.authenticate(
            this,
            onSuccess = {
                prompting = false
                locked.value = false
            },
            onFailure = { prompting = false },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_DIAL && action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        if (data.scheme == "tel") dialRequest.value = data.schemeSpecificPart.orEmpty()
    }
}
