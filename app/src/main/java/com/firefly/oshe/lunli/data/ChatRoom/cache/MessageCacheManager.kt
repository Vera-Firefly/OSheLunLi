package com.firefly.oshe.lunli.data.ChatRoom.cache

import android.content.Context
import android.content.SharedPreferences
import com.firefly.oshe.lunli.data.ChatRoom.Message
import org.json.JSONArray
import org.json.JSONObject

class MessageCacheManager(private val context: Context, private val userId: String) {

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences("ChatRoomPrefs_$userId", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFIX_ROOM = "room_"
        private const val CACHE_DURATION = 7 * 24 * 60 * 60 * 1000L // 7天缓存
    }

    fun saveMessagesToCache(roomId: String, messages: List<Message>) {
        val jsonArray = JSONArray()
        messages.forEach { message ->
            val obj = JSONObject().apply {
                put("id", message.id)
                put("sender", message.sender)
                put("image", message.senderImage)
                put("content", message.content)
                put("timestamp", System.currentTimeMillis()) // 添加时间戳用于过期清理
            }
            jsonArray.put(obj)
        }
        sharedPref.edit().putString("${PREFIX_ROOM}$roomId", jsonArray.toString()).apply()
    }

    fun loadCachedMessages(roomId: String): List<Message> {
        val json = sharedPref.getString("${PREFIX_ROOM}$roomId", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Message(
                    obj.getString("id"),
                    obj.getString("sender"),
                    obj.getString("image"),
                    obj.getString("content")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearRoomMessagesCache(roomId: String) {
        sharedPref.edit().remove("${PREFIX_ROOM}$roomId").apply()
    }

    fun clearAllMessagesCache() {
        val allKeys = sharedPref.all.keys.toList()
        val editor = sharedPref.edit()

        allKeys.forEach { key ->
            if (key.startsWith(PREFIX_ROOM)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun clearExpiredMessagesCache(maxAgeDays: Int = 7) {
        val allKeys = sharedPref.all.keys.toList()
        val currentTime = System.currentTimeMillis()
        val maxAgeMillis = maxAgeDays * 24 * 60 * 60 * 1000L
        val editor = sharedPref.edit()

        allKeys.forEach { key ->
            if (key.startsWith(PREFIX_ROOM)) {
                val json = sharedPref.getString(key, null)
                json?.let {
                    try {
                        val jsonArray = JSONArray(it)
                        if (jsonArray.length() > 0) {
                            val firstMessage = jsonArray.getJSONObject(0)
                            val timestamp = firstMessage.optLong("timestamp", currentTime)
                            if (currentTime - timestamp > maxAgeMillis) {
                                editor.remove(key)
                            } else {
                                // ???
                            }
                        } else {
                            // ???
                        }
                    } catch (e: Exception) {
                        editor.remove(key)
                    }
                }
            }
        }
        editor.apply()
    }

    fun getCacheStatistics(): CacheStats {
        val allKeys = sharedPref.all.keys.toList()
        val roomCacheCount = allKeys.count { it.startsWith(PREFIX_ROOM) }
        var totalMessages = 0
        var totalSize = 0

        allKeys.forEach { key ->
            if (key.startsWith(PREFIX_ROOM)) {
                val json = sharedPref.getString(key, null)
                json?.let {
                    totalSize += it.length
                    try {
                        val jsonArray = JSONArray(it)
                        totalMessages += jsonArray.length()
                    } catch (e: Exception) {
                        // 好吧, 我想不到能出啥错, 但这里必须有个try, 往死里try!!!
                    }
                }
            }
        }

        return CacheStats(
            roomCount = roomCacheCount,
            totalMessages = totalMessages,
            totalSize = totalSize
        )
    }

    fun hasCachedMessages(roomId: String): Boolean {
        return sharedPref.contains("${PREFIX_ROOM}$roomId")
    }

    fun getCachedRoomIds(): List<String> {
        return sharedPref.all.keys
            .filter { it.startsWith(PREFIX_ROOM) }
            .map { it.removePrefix(PREFIX_ROOM) }
    }

    data class CacheStats(
        val roomCount: Int,
        val totalMessages: Int,
        val totalSize: Int
    ) {
        override fun toString(): String {
            return "缓存房间: $roomCount 个, 消息总数: $totalMessages 条, 总大小: ${totalSize / 1024} KB"
        }
    }
}