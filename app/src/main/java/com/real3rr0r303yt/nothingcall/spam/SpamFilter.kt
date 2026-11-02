/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.spam

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.Connection
import android.telecom.TelecomManager
import com.real3rr0r303yt.nothingcall.call.CallActions
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.data.ContactsRepository

/** Decides – fully offline – whether an incoming call should be blocked. */
object SpamFilter {
    const val REASON_LIST = "list"
    const val REASON_HIDDEN = "hidden"
    const val REASON_UNKNOWN = "unknown"
    const val REASON_SPOOFED = "spoofed"

    /** Returns a reason code when the call should be blocked, otherwise null. */
    fun check(context: Context, details: Call.Details): String? {
        val number = CallActions.stripClir(details.handle?.schemeSpecificPart.orEmpty())
        val hidden = number.isBlank() || details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED

        if (!hidden && BlockRepository.isBlocked(number)) return REASON_LIST
        if (hidden) return if (AppPrefs.blockHidden.value) REASON_HIDDEN else null

        if (AppPrefs.blockUnverified.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            details.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
        ) {
            return REASON_SPOOFED
        }
        if (AppPrefs.blockUnknown.value && ContactsRepository.lookupName(context, number) == null) {
            return REASON_UNKNOWN
        }
        return null
    }
}
