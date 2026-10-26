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

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.soundboard.SoundClip
import com.real3rr0r303yt.nothingcall.soundboard.Soundboard
import com.real3rr0r303yt.nothingcall.ui.components.BlinkingDot
import com.real3rr0r303yt.nothingcall.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingcall.ui.components.SectionLabel
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.ui.theme.NType
import com.real3rr0r303yt.nothingcall.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Grid of soundboard pads – used in the in-call sheet and in the settings preview. */
@Composable
fun SoundboardGrid(
    modifier: Modifier = Modifier,
    onDelete: ((SoundClip) -> Unit)? = null,
    header: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val custom by Soundboard.custom.collectAsState()
    val playing by Soundboard.playing.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel(stringResource(R.string.soundboard_builtin)) }
        items(Soundboard.builtIns, key = { it.id }) { clip ->
            SoundPad(clip.label(context), playing == clip.id, onClick = { toggle(context, clip, playing) })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionLabel(stringResource(R.string.soundboard_custom), Modifier.padding(top = 8.dp))
        }
        if (custom.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.soundboard_custom_empty), color = NColors.Gray, fontSize = 13.sp)
            }
        }
        items(custom, key = { it.id }) { clip ->
            SoundPad(
                clip.label(context), playing == clip.id,
                onClick = { toggle(context, clip, playing) },
                onDelete = onDelete?.let { handler -> { handler(clip) } },
            )
        }
    }
}

private fun toggle(context: android.content.Context, clip: SoundClip, playing: String?) {
    if (playing == clip.id) Soundboard.stop() else Soundboard.play(context, clip)
}

@Composable
private fun SoundPad(label: String, active: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (active) NColors.Red else NColors.Surface)
            .border(1.dp, if (active) NColors.Red else NColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        if (active) BlinkingDot(Modifier.align(Alignment.TopStart), color = NColors.White, size = 6.dp)
        if (onDelete != null) {
            Icon(
                Icons.Filled.Close, stringResource(R.string.delete),
                tint = NColors.Gray,
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp).clickable(onClick = onDelete),
            )
        }
        Text(
            label.uppercase(),
            style = NType.label.copy(fontSize = 10.sp),
            color = NColors.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Settings sub-page: import / delete own clips and preview everything. */
@Composable
fun SoundboardManageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { Soundboard.importClip(context, uri) }
                context.toast(if (ok) R.string.soundboard_imported else R.string.soundboard_import_failed)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = NColors.White)
            }
            DotMatrixText(stringResource(R.string.soundboard_title), Modifier.weight(1f), dotSize = 3.5.dp)
            IconButton(onClick = { Soundboard.stop() }) {
                Icon(Icons.Filled.Stop, stringResource(R.string.soundboard_stop), tint = NColors.White)
            }
        }
        SoundboardGrid(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            onDelete = { clip -> scope.launch(Dispatchers.IO) { runCatching { Soundboard.delete(context, clip) } } },
            header = {
                Column {
                    Text(stringResource(R.string.soundboard_hint), color = NColors.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Filled.Add, null, tint = NColors.Red)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.soundboard_add), color = NColors.White)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            },
        )
    }
}
