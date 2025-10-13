package com.firefly.oshe.lunli.data

import android.R
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserImagePref(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveImage(userImage: UserImage) {
        val images = getAllImages().toMutableMap()
        images[userImage.userId] = userImage
        saveAllImages(images)
    }

    fun getImage(userId: String): UserImage? {
        return getAllImages()[userId]
    }

    fun getAllImages(): Map<String, UserImage> {
        val json = prefs.getString(KEY_IMAGE, "") ?: ""
        return try {
            Gson().fromJson(json, object : TypeToken<Map<String, UserImage>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun deleteImage(userId: String) {
        val images = getAllImages().toMutableMap()
        images.remove(userId)
        saveAllImages(images)
    }

    fun clearAllImages() {
        prefs.edit().remove(KEY_IMAGE).apply()
    }

    private fun saveAllImages (images: Map<String, UserImage>) {
        val json = Gson().toJson(images)
        prefs.edit().putString(KEY_IMAGE, json).apply()
    }

    companion object {
        private const val PREF_NAME = "user_images_prefs"
        private const val KEY_IMAGE = "user_images_data"
    }
}