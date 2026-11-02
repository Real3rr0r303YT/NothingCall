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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingcall.ui.components.dotGrid
import com.real3rr0r303yt.nothingcall.ui.theme.NColors

/** Shown while the app lock is active – nothing of the app's content is rendered behind it. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(NColors.Black).dotGrid().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Lock, null, tint = NColors.Red, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(24.dp))
        DotMatrixText(stringResource(R.string.lock_title), dotSize = 4.dp, autoFit = false)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onUnlock) {
            Text(stringResource(R.string.lock_unlock), color = NColors.White)
        }
    }
}
