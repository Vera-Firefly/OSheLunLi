package com.firefly.oshe.lunli.data.ChatRoom.cache

import android.content.Context
import android.content.SharedPreferences
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeparateUserCacheManager(private val context: Context) {
    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences("SeparateUserCache", Context.MODE_PRIVATE)
    }

    private val basicInfoMemoryCache = mutableMapOf<String, UserBasicInfo>()
    private val imageMemoryCache = mutableMapOf<String, UserImageInfo>()

    companion object {
        private const val CACHE_DURATION = 7 * 24 * 60 * 60 * 1000L
        private const val PREFIX_BASIC = "basic_"
        private const val PREFIX_IMAGE = "image_"
    }

    suspend fun getUserBasicInfo(userId: String): UserBasicInfo? {
        basicInfoMemoryCache[userId]?.let { cachedInfo ->
            if (!cachedInfo.isExpired()) {
                return cachedInfo
            }
        }

        val cachedJson = sharedPref.getString("${PREFIX_BASIC}$userId", null)
        if (cachedJson != null) {
            UserBasicInfo.fromJson(cachedJson)?.let { cachedInfo ->
                basicInfoMemoryCache[userId] = cachedInfo
                if (!cachedInfo.isExpired()) {
                    return cachedInfo
                }
            }
        }

        return fetchAndCacheBasicInfo(userId)
    }

    private suspend fun fetchAndCacheBasicInfo(userId: String): UserBasicInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val user = SBClient.fetchUser(userId)
                if (user != null) {
                    val basicInfo = UserBasicInfo(
                        userId = userId,
                        userName = user.name
                    )
                    saveUserBasicInfo(basicInfo)
                    basicInfo
                } else {
                    val defaultInfo = UserBasicInfo(
                        userId = userId,
                        userName = "未知用户"
                    )
                    saveUserBasicInfo(defaultInfo)
                    defaultInfo
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun saveUserBasicInfo(userInfo: UserBasicInfo) {
        basicInfoMemoryCache[userInfo.userId] = userInfo
        sharedPref.edit()
            .putString("${PREFIX_BASIC}${userInfo.userId}", userInfo.toJson())
            .apply()
    }

    suspend fun getUserImageInfo(userId: String): UserImageInfo? {
        imageMemoryCache[userId]?.let { cachedInfo ->
            if (!cachedInfo.isExpired()) {
                return cachedInfo
            }
        }

        val cachedJson = sharedPref.getString("${PREFIX_IMAGE}$userId", null)
        if (cachedJson != null) {
            UserImageInfo.fromJson(cachedJson)?.let { cachedInfo ->
                imageMemoryCache[userId] = cachedInfo
                if (!cachedInfo.isExpired()) {
                    return cachedInfo
                }
            }
        }

        return fetchAndCacheImageInfo(userId)
    }

    private suspend fun fetchAndCacheImageInfo(userId: String): UserImageInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val user = SBClient.fetchUser(userId)
                if (user != null && user.image.isNotBlank()) {
                    val imageInfo = UserImageInfo(
                        userId = userId,
                        userImage = user.image
                    )
                    saveUserImageInfo(imageInfo)
                    imageInfo
                } else {
                    val defaultImage = getDefaultUserImage()
                    val imageInfo = UserImageInfo(
                        userId = userId,
                        userImage = defaultImage
                    )
                    saveUserImageInfo(imageInfo)
                    imageInfo
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getDefaultUserImage(): String {
        return withContext(Dispatchers.Main) {
            val drawable = context.getDrawable(android.R.drawable.ic_menu_report_image)
            ImageUtils.drawableToBase64(drawable!!) ?: "NULL"
        }
    }

    fun saveUserImageInfo(imageInfo: UserImageInfo) {
        imageMemoryCache[imageInfo.userId] = imageInfo
        sharedPref.edit()
            .putString("${PREFIX_IMAGE}${imageInfo.userId}", imageInfo.toJson())
            .apply()
    }

    suspend fun getUserDisplayInfo(userId: String): Pair<String, String> {
        val basicInfo = getUserBasicInfo(userId)
        val imageInfo = getUserImageInfo(userId)

        return Pair(
            basicInfo?.userName ?: "未知用户",
            imageInfo?.userImage ?: "NULL"
        )
    }

    fun clearExpiredCache() {
        val allKeys = sharedPref.all.keys.toList()
        val editor = sharedPref.edit()
        val currentTime = System.currentTimeMillis()

        allKeys.forEach { key ->
            when {
                key.startsWith(PREFIX_BASIC) -> {
                    val cachedJson = sharedPref.getString(key, null)
                    cachedJson?.let { json ->
                        UserBasicInfo.fromJson(json)?.let { info ->
                            if (currentTime - info.lastUpdateTime > CACHE_DURATION) {
                                editor.remove(key)
                                basicInfoMemoryCache.remove(info.userId)
                            }
                        } ?: run {
                            editor.remove(key)
                        }
                    }
                }
                key.startsWith(PREFIX_IMAGE) -> {
                    val cachedJson = sharedPref.getString(key, null)
                    cachedJson?.let { json ->
                        UserImageInfo.fromJson(json)?.let { info ->
                            if (currentTime - info.lastUpdateTime > CACHE_DURATION) {
                                editor.remove(key)
                                imageMemoryCache.remove(info.userId)
                            }
                        } ?: run {
                            editor.remove(key)
                        }
                    }
                }
            }
        }
        editor.apply()
    }

    fun clearAllCache() {
        basicInfoMemoryCache.clear()
        imageMemoryCache.clear()

        val allKeys = sharedPref.all.keys.toList()
        val editor = sharedPref.edit()
        allKeys.forEach { key ->
            if (key.startsWith(PREFIX_BASIC) || key.startsWith(PREFIX_IMAGE)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun clearUserCache(userId: String) {
        basicInfoMemoryCache.remove(userId)
        imageMemoryCache.remove(userId)

        val editor = sharedPref.edit()
        editor.remove("${PREFIX_BASIC}$userId")
        editor.remove("${PREFIX_IMAGE}$userId")
        editor.apply()
    }

    fun getCacheStats(): String {
        val basicCount = sharedPref.all.keys.count { it.startsWith(PREFIX_BASIC) }
        val imageCount = sharedPref.all.keys.count { it.startsWith(PREFIX_IMAGE) }
        return "基本信息: $basicCount, 用户图像: $imageCount, 内存缓存: ${basicInfoMemoryCache.size}"
    }

    suspend fun refreshUserInfo(userId: String) {
        clearUserCache(userId)
        getUserBasicInfo(userId)
        getUserImageInfo(userId)
    }
}