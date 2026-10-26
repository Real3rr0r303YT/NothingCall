/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.ui.theme.NColors
import com.real3rr0r303yt.nothingcall.ui.theme.NType

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = NColors.Gray) {
    Text(text.uppercase(), style = NType.label, color = color, modifier = modifier)
}

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DotMatrixText(title, Modifier.weight(1f), dotSize = 3.5.dp)
        Box(Modifier.size(6.dp).clip(CircleShape).background(NColors.Red))
    }
}

@Composable
fun NCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = NColors.Outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .background(NColors.Surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    container: Color = NColors.SurfaceHigh,
    contentColor: Color = NColors.White,
    outlined: Boolean = false,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .then(if (outlined) Modifier.border(1.dp, NColors.Outline, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = contentColor, modifier = Modifier.size(size * 0.4f))
    }
}

@Composable
fun LetterAvatar(name: String?, modifier: Modifier = Modifier, size: Dp = 44.dp, accent: Boolean = false) {
    val letter = name?.trim()?.firstOrNull()?.takeIf { it.isLetterOrDigit() }?.toString() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (accent) NColors.Red else NColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        DotMatrixText(letter, dotSize = size / 14, autoFit = false)
    }
}

@Composable
fun BlinkingDot(modifier: Modifier = Modifier, color: Color = NColors.Red, size: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "blink")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "blinkAlpha"
    )
    Box(modifier.size(size).alpha(alpha).clip(CircleShape).background(color))
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotMatrixText("...", dotSize = 5.dp, color = NColors.DimGray, autoFit = false)
        Spacer(Modifier.height(16.dp))
        Text(text, color = NColors.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = NColors.Gray, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = NColors.White, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = NColors.Gray, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.size(12.dp))
            trailing()
        }
    }
}

@Composable
fun NSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = NColors.White,
            checkedTrackColor = NColors.Red,
            checkedBorderColor = NColors.Red,
            uncheckedThumbColor = NColors.Gray,
            uncheckedTrackColor = NColors.Black,
            uncheckedBorderColor = NColors.Outline,
        ),
    )
}

@Composable
fun LegalNoticeDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NColors.SurfaceHigh,
        title = { Text(stringResource(R.string.legal_title)) },
        text = { Text(stringResource(R.string.legal_text), color = NColors.Gray) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.legal_accept), color = NColors.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = NColors.White) }
        },
    )
}

private val KEYS = listOf(
    '1' to "", '2' to "ABC", '3' to "DEF",
    '4' to "GHI", '5' to "JKL", '6' to "MNO",
    '7' to "PQRS", '8' to "TUV", '9' to "WXYZ",
    '*' to "", '0' to "+", '#' to "",
)

/** Dot-matrix keypad shared by the dialer and the in-call DTMF pad. */
@Composable
fun Keypad(
    onKey: (Char) -> Unit,
    modifier: Modifier = Modifier,
    onLongKey: ((Char) -> Boolean)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KEYS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { (digit, letters) ->
                    DialKey(digit, letters, onClick = { onKey(digit) }, onLongClick = onLongKey?.let { handler -> { handler(digit) } })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(digit: Char, letters: String, onClick: () -> Unit, onLongClick: (() -> Boolean)?) {
    Column(
        Modifier
            .size(width = 88.dp, height = 64.dp)
            .clip(RoundedCornerShape(32.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { handler -> { if (!handler()) onClick() } },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DotMatrixText(digit.toString(), dotSize = 3.4.dp, autoFit = false)
        Spacer(Modifier.height(4.dp))
        Text(
            letters.ifEmpty { " " },
            style = NType.label.copy(fontSize = 9.sp, letterSpacing = 2.sp),
            color = NColors.Gray,
        )
    }
}
