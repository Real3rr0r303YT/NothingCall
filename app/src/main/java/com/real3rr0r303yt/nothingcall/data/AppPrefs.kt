/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Non-sensitive app settings (plain SharedPreferences, never backed up). */
object AppPrefs {
    const val SOURCE_AUTO = "auto"
    const val SOURCE_VOICE_CALL = "voice_call"
    const val SOURCE_VOICE_COMM = "voice_comm"
    const val SOURCE_MIC = "mic"

    private lateinit var prefs: SharedPreferences

    class Pref<T>(private val key: String, private val default: T) {
        private val state = MutableStateFlow(default)
        val flow: StateFlow<T> = state.asStateFlow()
        val value: T get() = state.value

        @Suppress("UNCHECKED_CAST")
        internal fun load() {
            state.value = when (default) {
                is Boolean -> prefs.getBoolean(key, default) as T
                is String -> (prefs.getString(key, default) ?: default) as T
                else -> default
            }
        }

        fun set(value: T) {
            state.value = value
            prefs.edit().apply {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                }
            }.apply()
        }
    }

    val autoRecord = Pref("auto_record", false)
    val recordingSource = Pref("recording_source", SOURCE_AUTO)
    val legalAccepted = Pref("legal_accepted", false)
    val soundboardSpeaker = Pref("soundboard_speaker", true)
    val dialpadTones = Pref("dialpad_tones", true)

    fun init(context: Context) {
        prefs = context.getSharedPreferences("nc_settings", Context.MODE_PRIVATE)
        listOf(autoRecord, recordingSource, legalAccepted, soundboardSpeaker, dialpadTones).forEach { it.load() }
    }
}
