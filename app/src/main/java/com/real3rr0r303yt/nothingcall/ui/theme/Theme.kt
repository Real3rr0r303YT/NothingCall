/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0D0D0D)
    val SurfaceHigh = Color(0xFF181818)
    val Outline = Color(0xFF2A2A2A)
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFF8A8A8A)
    val DimGray = Color(0xFF4A4A4A)
    val Red = Color(0xFFD71921)
}

object NType {
    /** Small, tracked-out monospace labels – the Nothing OS "system font" look. */
    val label = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Medium,
    )
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 0.5.sp)
}

private val OledScheme = darkColorScheme(
    primary = NColors.White,
    onPrimary = NColors.Black,
    primaryContainer = NColors.SurfaceHigh,
    onPrimaryContainer = NColors.White,
    secondary = NColors.Gray,
    onSecondary = NColors.Black,
    secondaryContainer = NColors.SurfaceHigh,
    onSecondaryContainer = NColors.White,
    tertiary = NColors.Red,
    onTertiary = NColors.White,
    error = NColors.Red,
    onError = NColors.White,
    background = NColors.Black,
    onBackground = NColors.White,
    surface = NColors.Black,
    onSurface = NColors.White,
    surfaceVariant = NColors.SurfaceHigh,
    onSurfaceVariant = NColors.Gray,
    surfaceTint = NColors.Black,
    outline = NColors.Outline,
    outlineVariant = NColors.Outline,
    surfaceContainerLowest = NColors.Black,
    surfaceContainerLow = NColors.Surface,
    surfaceContainer = NColors.Surface,
    surfaceContainerHigh = NColors.SurfaceHigh,
    surfaceContainerHighest = Color(0xFF202020),
)

private val NothingTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Normal),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Normal),
        labelSmall = NType.label,
    )
}

/** Always-dark, true-black OLED theme. */
@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OledScheme, typography = NothingTypography, content = content)
}
