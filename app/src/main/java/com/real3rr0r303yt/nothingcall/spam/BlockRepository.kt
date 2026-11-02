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
import android.telephony.PhoneNumberUtils
import com.real3rr0r303yt.nothingcall.security.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * A block rule: either an exact number ("+4917012345") or a prefix ending in '*' ("+44*", "0900*").
 */
data class BlockRule(val pattern: String) {
    val isPrefix: Boolean get() = pattern.endsWith("*")

    fun matches(number: String): Boolean {
        if (number.isBlank()) return false
        return if (isPrefix) {
            val prefix = normalize(pattern.dropLast(1))
            prefix.isNotEmpty() && normalize(number).startsWith(prefix)
        } else {
            PhoneNumberUtils.compare(pattern, number)
        }
    }

    companion object {
        fun normalize(n: String) = n.filter { it.isDigit() || it == '+' }
    }
}

data class BlockedCall(val number: String, val timestamp: Long, val reason: String)

/** Local, AES-256-GCM encrypted block list and blocked-call log. Nothing is ever uploaded. */
object BlockRepository {
    private const val MAX_LOG = 200
    private val lock = Any()

    private val _rules = MutableStateFlow<List<BlockRule>>(emptyList())
    val rules: StateFlow<List<BlockRule>> = _rules.asStateFlow()

    private val _log = MutableStateFlow<List<BlockedCall>>(emptyList())
    val log: StateFlow<List<BlockedCall>> = _log.asStateFlow()

    private fun dir(context: Context) = File(context.filesDir, "spam").apply { mkdirs() }
    private fun rulesFile(context: Context) = File(dir(context), "rules.enc")
    private fun logFile(context: Context) = File(dir(context), "log.enc")

    fun load(context: Context) = synchronized(lock) {
        _rules.value = SecureStore.readText(rulesFile(context))?.let { text ->
            val arr = JSONArray(text)
            List(arr.length()) { BlockRule(arr.getString(it)) }
        } ?: emptyList()
        _log.value = SecureStore.readText(logFile(context))?.let { text ->
            val arr = JSONArray(text)
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                BlockedCall(o.optString("number"), o.getLong("ts"), o.optString("reason"))
            }
        } ?: emptyList()
    }

    private fun saveRules(context: Context, list: List<BlockRule>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.pattern) }
        SecureStore.writeText(rulesFile(context), arr.toString())
        _rules.value = list
    }

    private fun saveLog(context: Context, list: List<BlockedCall>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("number", it.number).put("ts", it.timestamp).put("reason", it.reason)) }
        SecureStore.writeText(logFile(context), arr.toString())
        _log.value = list
    }

    fun addRule(context: Context, pattern: String) = synchronized(lock) {
        val clean = pattern.trim().let { p ->
            if (p.endsWith("*")) BlockRule.normalize(p.dropLast(1)) + "*" else BlockRule.normalize(p)
        }
        if (clean.isEmpty() || clean == "*") return@synchronized
        if (_rules.value.any { it.pattern == clean }) return@synchronized
        saveRules(context, _rules.value + BlockRule(clean))
    }

    fun removeRule(context: Context, rule: BlockRule) = synchronized(lock) {
        saveRules(context, _rules.value.filterNot { it.pattern == rule.pattern })
    }

    fun isBlocked(number: String): Boolean = _rules.value.any { it.matches(number) }

    fun logBlocked(context: Context, number: String, reason: String) = synchronized(lock) {
        saveLog(context, (listOf(BlockedCall(number, System.currentTimeMillis(), reason)) + _log.value).take(MAX_LOG))
    }

    fun clearLog(context: Context) = synchronized(lock) { saveLog(context, emptyList()) }

    fun wipe(context: Context) = synchronized(lock) {
        dir(context).deleteRecursively()
        _rules.value = emptyList()
        _log.value = emptyList()
    }
}
