/*
 * NothingCall – privacy-first phone app
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file for details.
 */

package com.real3rr0r303yt.nothingcall.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.view.WindowManager
import com.real3rr0r303yt.nothingcall.R
import com.real3rr0r303yt.nothingcall.data.AppPrefs

/** OPSEC helpers: app lock (biometrics / device credential) and screenshot protection. */
object AppLock {
    private const val GRACE_MS = 10_000L
    private var backgroundedAt = 0L

    fun isDeviceSecure(context: Context) =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /** Called from onStop – remembers when the app left the foreground. */
    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** Whether the lock screen has to be shown when coming back. */
    fun shouldLock(context: Context, firstStart: Boolean): Boolean {
        if (!AppPrefs.appLock.value || !isDeviceSecure(context)) return false
        return firstStart || SystemClock.elapsedRealtime() - backgroundedAt > GRACE_MS
    }

    fun authenticate(activity: Activity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.app_name))
            .setSubtitle(activity.getString(R.string.lock_prompt))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        try {
            builder.build().authenticate(
                CancellationSignal(),
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onFailure()
                },
            )
        } catch (e: Exception) {
            onFailure()
        }
    }

    fun applySecureFlag(activity: Activity, secure: Boolean) {
        if (secure) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
