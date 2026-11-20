/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT – Made by 3rr0r303YT on TikTok
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

package com.real3rr0r303yt.nothingcall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.real3rr0r303yt.nothingcall.MainActivity
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.ContactsRepository

/**
 * Up to four starred contacts as one-tap buttons plus a keypad button.
 * A tap opens NothingCall's dialer with the number filled in – one more tap places the call,
 * so a pocket touch never dials anybody. Private (encrypted) contacts are never shown here.
 */
class FavoritesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Contacts query off the main thread.
        val pending = goAsync()
        Thread {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        private const val SLOTS = 4

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FavoritesWidget::class.java))
            if (ids.isNotEmpty()) Thread { render(context.applicationContext, manager, ids) }.start()
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val density = context.resources.displayMetrics.density
            val views = RemoteViews(context.packageName, R.layout.widget_favorites)
            views.setImageViewBitmap(R.id.widget_title, DotRender.text("CALL", 5f * density) { i, _ ->
                if (i == 0) DotRender.RED else DotRender.WHITE
            })

            val favorites = ContactsRepository.loadSystem(context).filter { it.starred && it.numbers.isNotEmpty() }
            val slots = intArrayOf(R.id.widget_fav1, R.id.widget_fav2, R.id.widget_fav3, R.id.widget_fav4)
            for (i in 0 until SLOTS) {
                val c = favorites.getOrNull(i)
                if (c == null) {
                    views.setViewVisibility(slots[i], View.GONE)
                    continue
                }
                views.setViewVisibility(slots[i], View.VISIBLE)
                views.setTextViewText(slots[i], c.name)
                views.setOnClickPendingIntent(slots[i], dial(context, 10 + i, c.numbers.first()))
            }
            val hint = when {
                !ContactsRepository.hasPermission(context) -> R.string.widget_fav_permission
                favorites.isEmpty() -> R.string.widget_fav_empty
                else -> 0
            }
            if (hint != 0) views.setTextViewText(R.id.widget_hint, context.getString(hint))
            views.setViewVisibility(R.id.widget_hint, if (hint != 0) View.VISIBLE else View.GONE)

            views.setOnClickPendingIntent(R.id.widget_keypad, dial(context, 1, null))
            views.setOnClickPendingIntent(R.id.widget_root, dial(context, 0, null))
            manager.updateAppWidget(ids, views)
        }

        private fun dial(context: Context, request: Int, number: String?): PendingIntent {
            val intent = Intent(Intent.ACTION_DIAL)
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (number != null) intent.data = Uri.fromParts("tel", number, null)
            return PendingIntent.getActivity(
                context, request, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
