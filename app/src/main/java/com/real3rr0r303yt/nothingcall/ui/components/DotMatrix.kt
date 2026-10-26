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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.real3rr0r303yt.nothingcall.ui.theme.NColors

/** A 5x7 dot-matrix font, rendered dot by dot – inspired by Nothing's Glyph typography. */
object DotFont {
    private val raw: Map<Char, List<String>> = mapOf(
        'A' to listOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
        'B' to listOf("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."),
        'C' to listOf(".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."),
        'D' to listOf("###..", "#..#.", "#...#", "#...#", "#...#", "#..#.", "###.."),
        'E' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
        'F' to listOf("#####", "#....", "#....", "####.", "#....", "#....", "#...."),
        'G' to listOf(".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"),
        'H' to listOf("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
        'I' to listOf(".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."),
        'J' to listOf("..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."),
        'K' to listOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
        'L' to listOf("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
        'M' to listOf("#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"),
        'N' to listOf("#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"),
        'O' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        'P' to listOf("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
        'Q' to listOf(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
        'R' to listOf("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
        'S' to listOf(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
        'T' to listOf("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
        'U' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        'V' to listOf("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."),
        'W' to listOf("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#."),
        'X' to listOf("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
        'Y' to listOf("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
        'Z' to listOf("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
        'Ä' to listOf("#...#", ".....", ".###.", "#...#", "#####", "#...#", "#...#"),
        'Ö' to listOf("#...#", ".....", ".###.", "#...#", "#...#", "#...#", ".###."),
        'Ü' to listOf("#...#", ".....", "#...#", "#...#", "#...#", "#...#", ".###."),
        '0' to listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
        '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
        '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
        '3' to listOf("#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."),
        '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
        '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
        '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
        '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
        '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
        '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
        '+' to listOf(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."),
        '-' to listOf(".....", ".....", ".....", "#####", ".....", ".....", "....."),
        '*' to listOf(".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", "....."),
        '#' to listOf(".#.#.", ".#.#.", "#####", ".#.#.", "#####", ".#.#.", ".#.#."),
        ':' to listOf(".....", ".##..", ".##..", ".....", ".##..", ".##..", "....."),
        '.' to listOf(".....", ".....", ".....", ".....", ".....", ".##..", ".##.."),
        ',' to listOf(".....", ".....", ".....", ".....", ".##..", "..#..", ".#..."),
        '/' to listOf(".....", "....#", "...#.", "..#..", ".#...", "#....", "....."),
        '?' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#.."),
        '!' to listOf("..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#.."),
        '(' to listOf("...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#."),
        ')' to listOf(".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#..."),
        '\'' to listOf("..#..", "..#..", ".#...", ".....", ".....", ".....", "....."),
        '_' to listOf(".....", ".....", ".....", ".....", ".....", ".....", "#####"),
        '●' to listOf(".....", ".###.", "#####", "#####", "#####", ".###.", "....."),
        ' ' to listOf(".....", ".....", ".....", ".....", ".....", ".....", "....."),
    )

    private val glyphs: Map<Char, IntArray> = raw.mapValues { (_, rows) ->
        IntArray(7) { y -> rows[y].fold(0) { acc, c -> (acc shl 1) or (if (c == '#') 1 else 0) } }
    }

    private val fallback = glyphs.getValue('?')

    fun normalize(text: String): String = text.uppercase().replace("ß", "SS")

    fun glyph(c: Char): IntArray = glyphs[c] ?: fallback
}

/**
 * Renders [text] as a dot-matrix display. With [autoFit] the dot size shrinks so that long
 * strings (e.g. phone numbers) always fit the available width.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 4.dp,
    color: Color = NColors.White,
    unlitColor: Color = Color.Transparent,
    autoFit: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
) {
    val glyphText = remember(text) { DotFont.normalize(text) }
    val columns = (glyphText.length * 6 - 1).coerceAtLeast(1)
    BoxWithConstraints(
        modifier = modifier.semantics { contentDescription = text },
        contentAlignment = contentAlignment,
    ) {
        val cell = if (autoFit && constraints.hasBoundedWidth) minOf(dotSize, maxWidth / columns) else dotSize
        Canvas(Modifier.size(cell * columns, cell * 7)) {
            val c = size.width / columns
            val radius = c * 0.38f
            glyphText.forEachIndexed { index, ch ->
                val rows = DotFont.glyph(ch)
                for (y in 0 until 7) {
                    for (x in 0 until 5) {
                        val lit = ((rows[y] shr (4 - x)) and 1) == 1
                        val dotColor = if (lit) color else unlitColor
                        if (dotColor.alpha > 0f) {
                            drawCircle(dotColor, radius, Offset((index * 6 + x) * c + c / 2, y * c + c / 2))
                        }
                    }
                }
            }
        }
    }
}

/** Subtle dot-grid backdrop. */
fun Modifier.dotGrid(
    color: Color = Color.White.copy(alpha = 0.06f),
    spacing: Dp = 18.dp,
    radius: Dp = 1.dp,
): Modifier = drawWithCache {
    val step = spacing.toPx()
    val points = ArrayList<Offset>()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            points += Offset(x, y)
            x += step
        }
        y += step
    }
    val stroke = radius.toPx() * 2
    onDrawBehind {
        drawPoints(points, PointMode.Points, color, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
