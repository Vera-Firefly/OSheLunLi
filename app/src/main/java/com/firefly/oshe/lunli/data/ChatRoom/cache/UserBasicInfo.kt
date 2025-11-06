package com.firefly.oshe.lunli.data.ChatRoom.cache

import org.json.JSONObject

data class UserBasicInfo(
    val userId: String,
    val userName: String,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        val sevenDaysInMillis = 24 * 60 * 60 * 1000L // 1天更新一次
        return System.currentTimeMillis() - lastUpdateTime > sevenDaysInMillis
    }

    fun toJson(): String {
        return JSONObject().apply {
            put("userId", userId)
            put("userName", userName)
            put("lastUpdateTime", lastUpdateTime)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): UserBasicInfo? {
            return try {
                val obj = JSONObject(json)
                UserBasicInfo(
                    userId = obj.getString("userId"),
                    userName = obj.getString("userName"),
                    lastUpdateTime = obj.getLong("lastUpdateTime")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class UserImageInfo(
    val userId: String,
    val userImage: String,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastUpdateTime > sevenDaysInMillis
    }

    fun toJson(): String {
        return JSONObject().apply {
            put("userId", userId)
            put("userImage", userImage)
            put("lastUpdateTime", lastUpdateTime)
        }.toString()
    }

    companion object {
        fun fromJson(json: String): UserImageInfo? {
            return try {
                val obj = JSONObject(json)
                UserImageInfo(
                    userId = obj.getString("userId"),
                    userImage = obj.getString("userImage"),
                    lastUpdateTime = obj.getLong("lastUpdateTime")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}