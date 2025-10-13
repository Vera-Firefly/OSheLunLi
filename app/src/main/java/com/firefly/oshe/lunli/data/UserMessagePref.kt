package com.firefly.oshe.lunli.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.SharedPreferences

class UserMessagePref(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveMessage(userMessage: UserMessage) {
        val messages = getAllMessages().toMutableMap()
        messages[userMessage.userId] = userMessage
        saveAllMessages(messages)
    }

    fun getMessage(userId: String): UserMessage? {
        return getAllMessages()[userId]
    }

    fun getAllMessages(): Map<String, UserMessage> {
        val json = prefs.getString(KEY_MESSAGES, "") ?: ""
        return try {
            Gson().fromJson(json, object : TypeToken<Map<String, UserMessage>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun deleteMessage(userId: String) {
        val messages = getAllMessages().toMutableMap()
        messages.remove(userId)
        saveAllMessages(messages)
    }

    fun clearAllMessages() {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }

    private fun saveAllMessages(messages: Map<String, UserMessage>) {
        val json = Gson().toJson(messages)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    companion object {
        private const val PREF_NAME = "user_messages_prefs"
        private const val KEY_MESSAGES = "user_messages_data"
    }
}