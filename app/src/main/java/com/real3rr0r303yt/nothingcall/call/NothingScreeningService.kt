/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.call

import android.telecom.Call
import android.telecom.CallScreeningService
import com.real3rr0r303yt.nothingcall.data.AppPrefs
import com.real3rr0r303yt.nothingcall.spam.BlockRepository
import com.real3rr0r303yt.nothingcall.spam.SpamFilter

/**
 * Offline spam protection. Telecom asks this service about every incoming call before it rings.
 * Blocked calls are rejected (or silenced) and written to the encrypted blocked-calls log.
 */
class NothingScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            val reason = runCatching { SpamFilter.check(this, callDetails) }.getOrNull()
            if (reason != null) {
                if (AppPrefs.silenceInsteadOfReject.value) {
                    response.setSilenceCall(true)
                } else {
                    response.setDisallowCall(true).setRejectCall(true).setSkipNotification(true)
                }
                val number = CallActions.stripClir(callDetails.handle?.schemeSpecificPart.orEmpty())
                runCatching { BlockRepository.logBlocked(applicationContext, number, reason) }
            }
        }
        respondToCall(callDetails, response.build())
    }
}
