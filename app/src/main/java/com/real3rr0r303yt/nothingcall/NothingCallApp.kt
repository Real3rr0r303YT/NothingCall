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

import android.app.Application
import com.real3rr0r303yt.nothingcall.call.Notifications
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.data.ContactsRepository
import com.real3rr0r303yt.nothingcall.recording.RecordingRepository
import com.real3rr0r303yt.nothingcall.security.CryptoManager
import com.real3rr0r303yt.nothingcall.soundboard.Soundboard
import com.real3rr0r303yt.nothingcall.util.AppScope
import kotlinx.coroutines.launch

class NothingCallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CryptoManager.init(this)
        AppPrefs.init(this)
        Notifications.createChannels(this)
        AppScope.io.launch {
            RecordingRepository.cleanupTemp(this@NothingCallApp)
            runCatching { RecordingRepository.load(this@NothingCallApp) }
            runCatching { ContactsRepository.loadPrivate(this@NothingCallApp) }
            runCatching { Soundboard.loadCustom(this@NothingCallApp) }
        }
    }
}
