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

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.util.toast

data class SimOption(val handle: PhoneAccountHandle, val label: String)

object CallActions {
    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isDefaultDialer(context: Context): Boolean =
        context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true

    fun placeCall(context: Context, number: String, account: PhoneAccountHandle? = null) {
        if (number.isBlank()) return
        if (!granted(context, Manifest.permission.CALL_PHONE)) {
            context.toast(R.string.call_permission_missing)
            return
        }
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        val extras = Bundle()
        if (account != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
        try {
            telecom.placeCall(Uri.fromParts("tel", number, null), extras)
        } catch (e: SecurityException) {
            context.toast(R.string.call_permission_missing)
        }
    }

    /** SIM choices – empty when there is a default SIM or only one SIM. */
    fun simChoices(context: Context): List<SimOption> {
        if (!granted(context, Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return emptyList()
        return try {
            if (telecom.getDefaultOutgoingPhoneAccount("tel") != null) return emptyList()
            val accounts = telecom.callCapablePhoneAccounts
            if (accounts.size < 2) return emptyList()
            accounts.mapIndexed { i, handle ->
                SimOption(handle, telecom.getPhoneAccount(handle)?.label?.toString() ?: "SIM ${i + 1}")
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** All call-capable SIMs (for calls in STATE_SELECT_PHONE_ACCOUNT). */
    fun allSims(context: Context): List<SimOption> {
        if (!granted(context, Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return emptyList()
        return try {
            telecom.callCapablePhoneAccounts.mapIndexed { i, handle ->
                SimOption(handle, telecom.getPhoneAccount(handle)?.label?.toString() ?: "SIM ${i + 1}")
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
