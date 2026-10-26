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

import java.io.File

/** Small helper to persist encrypted text (JSON) files. */
object SecureStore {
    fun readText(file: File): String? {
        if (!file.exists()) return null
        return try {
            String(CryptoManager.decryptFile(file), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(CryptoManager.encrypt(text.toByteArray(Charsets.UTF_8)))
        if (!tmp.renameTo(file)) {
            tmp.delete()
            error("Could not write ${file.name}")
        }
    }
}
