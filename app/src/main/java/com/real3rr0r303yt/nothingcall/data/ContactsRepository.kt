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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.real3rr0r303yt.nothingcall.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Contact(
    val key: String,
    val name: String,
    val numbers: List<String>,
    val starred: Boolean = false,
    val isPrivate: Boolean = false,
)

/**
 * System contacts are read (never written or uploaded). "Private" contacts live only inside
 * NothingCall in an AES-256-GCM encrypted file and are invisible to every other app.
 */
object ContactsRepository {
    private val _private = MutableStateFlow<List<Contact>>(emptyList())
    val privateContacts: StateFlow<List<Contact>> = _private.asStateFlow()
    private val lock = Any()

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun loadSystem(context: Context): List<Contact> {
        if (!hasPermission(context)) return emptyList()
        val byId = LinkedHashMap<Long, Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
        )
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val number = c.getString(2)?.trim().orEmpty()
                    if (number.isEmpty()) continue
                    val starred = c.getInt(3) == 1
                    val existing = byId[id]
                    byId[id] = if (existing == null) {
                        Contact("sys:$id", name, listOf(number), starred)
                    } else if (existing.numbers.none { PhoneNumberUtils.compare(it, number) }) {
                        existing.copy(numbers = existing.numbers + number)
                    } else {
                        existing
                    }
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return byId.values.toList()
    }

    fun lookupName(context: Context, number: String): String? {
        if (number.isBlank()) return null
        _private.value.firstOrNull { c -> c.numbers.any { PhoneNumberUtils.compare(it, number) } }
            ?.let { return it.name }
        if (!hasPermission(context)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    // ---- private, encrypted contacts ----

    private fun file(context: Context) = File(context.filesDir, "private/contacts.enc")

    fun loadPrivate(context: Context) = synchronized(lock) {
        val list = mutableListOf<Contact>()
        SecureStore.readText(file(context))?.let { text ->
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Contact(
                    key = o.getString("key"),
                    name = o.getString("name"),
                    numbers = listOf(o.getString("number")),
                    isPrivate = true,
                )
            }
        }
        _private.value = list.sortedBy { it.name.lowercase() }
    }

    private fun persist(context: Context, list: List<Contact>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("key", it.key).put("name", it.name).put("number", it.numbers.first()))
        }
        SecureStore.writeText(file(context), arr.toString())
        _private.value = list.sortedBy { it.name.lowercase() }
    }

    fun addPrivate(context: Context, name: String, number: String) = synchronized(lock) {
        val contact = Contact("prv:${UUID.randomUUID()}", name.trim(), listOf(number.trim()), isPrivate = true)
        persist(context, _private.value + contact)
    }

    fun deletePrivate(context: Context, contact: Contact) = synchronized(lock) {
        persist(context, _private.value.filterNot { it.key == contact.key })
    }

    fun wipePrivate(context: Context) = synchronized(lock) {
        file(context).parentFile?.deleteRecursively()
        _private.value = emptyList()
    }
}
