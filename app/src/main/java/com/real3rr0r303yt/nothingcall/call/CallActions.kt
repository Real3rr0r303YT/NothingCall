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
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.util.toast

data class SimOption(val handle: PhoneAccountHandle, val label: String)

object CallActions {
    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isDefaultDialer(context: Context): Boolean =
        context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true

    /** GSM/UMTS/LTE "CLIR" prefix: suppresses your number for this single call. */
    const val CLIR_PREFIX = "#31#"
    private val CLIR_PREFIXES = listOf("#31#", "*31#")

    /** Removes CLIR prefixes for display purposes. */
    fun stripClir(number: String): String {
        var n = number.trim()
        CLIR_PREFIXES.forEach { if (n.startsWith(it)) n = n.removePrefix(it) }
        return n
    }

    private val FALLBACK_EMERGENCY = setOf("110", "112", "911", "999", "000", "118", "119", "116117")

    fun isEmergency(context: Context, number: String): Boolean {
        val digits = number.filter { it.isDigit() || it == '+' }
        return try {
            context.getSystemService(TelephonyManager::class.java)?.isEmergencyNumber(digits) == true
        } catch (e: Exception) {
            digits in FALLBACK_EMERGENCY
        } || digits in FALLBACK_EMERGENCY
    }

    /**
     * Number that is actually dialed. Emergency numbers and USSD/MMI codes are never touched –
     * hiding your number from emergency services is neither possible nor wanted.
     */
    fun dialString(context: Context, number: String, anonymous: Boolean): String {
        val clean = stripClir(number)
        if (!anonymous) return clean
        if (clean.startsWith("*") || clean.startsWith("#")) return clean
        if (isEmergency(context, clean)) return clean
        return CLIR_PREFIX + clean
    }

    /** Opens the carrier caller-ID settings ("Rufnummer anzeigen"). */
    fun openNetworkCallSettings(context: Context): Boolean = try {
        context.startActivity(
            android.content.Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: Exception) {
        false
    }

    fun placeCall(
        context: Context,
        number: String,
        account: PhoneAccountHandle? = null,
        anonymous: Boolean = false,
    ) {
        if (number.isBlank()) return
        if (!granted(context, Manifest.permission.CALL_PHONE)) {
            context.toast(R.string.call_permission_missing)
            return
        }
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        val extras = Bundle()
        if (account != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
        try {
            telecom.placeCall(Uri.fromParts("tel", dialString(context, number, anonymous), null), extras)
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
