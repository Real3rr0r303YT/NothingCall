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

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Envelope encryption for everything NothingCall stores on disk.
 *
 * A random 256-bit data key encrypts the payloads with AES-256-GCM. The data key itself is
 * wrapped by a non-exportable master key that lives in the Android Keystore (hardware-backed
 * where available). Nothing ever leaves the device.
 *
 * File format: [12 byte IV][ciphertext + 16 byte GCM tag]
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_ALIAS = "nothingcall.master.v1"
    private const val PREFS = "nc_crypto"
    private const val PREF_WRAPPED_KEY = "wrapped_data_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    private lateinit var appContext: Context
    private val random = SecureRandom()

    @Volatile
    private var dataKey: SecretKey? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun masterKey(): SecretKey {
        (keyStore().getEntry(MASTER_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    @Synchronized
    private fun key(): SecretKey {
        dataKey?.let { return it }
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(PREF_WRAPPED_KEY, null)
        val raw = if (wrapped != null) {
            unwrap(Base64.decode(wrapped, Base64.NO_WRAP))
        } else {
            ByteArray(32).also { fresh ->
                random.nextBytes(fresh)
                prefs.edit()
                    .putString(PREF_WRAPPED_KEY, Base64.encodeToString(wrap(fresh), Base64.NO_WRAP))
                    .commit()
            }
        }
        return SecretKeySpec(raw, "AES").also { dataKey = it }
    }

    private fun wrap(raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey()) // Keystore picks a random IV
        return cipher.iv + cipher.doFinal(raw)
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(TAG_BITS, blob, 0, IV_LEN))
        return cipher.doFinal(blob, IV_LEN, blob.size - IV_LEN)
    }

    private fun encryptCipher(): Cipher {
        val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
    }

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = encryptCipher()
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_LEN) { "Ciphertext too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob, 0, IV_LEN))
        return cipher.doFinal(blob, IV_LEN, blob.size - IV_LEN)
    }

    /** Streams [input] into an encrypted [target] file (written atomically). */
    fun encryptToFile(input: InputStream, target: File) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        val cipher = encryptCipher()
        tmp.outputStream().use { out ->
            out.write(cipher.iv)
            CipherOutputStream(out, cipher).use { input.copyTo(it) }
        }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            error("Could not write ${target.name}")
        }
    }

    fun decryptFile(file: File): ByteArray = decrypt(file.readBytes())

    /** Destroys the keys. Everything encrypted so far becomes unreadable. */
    @Synchronized
    fun destroyKeys() {
        dataKey = null
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        runCatching { keyStore().deleteEntry(MASTER_ALIAS) }
    }
}
