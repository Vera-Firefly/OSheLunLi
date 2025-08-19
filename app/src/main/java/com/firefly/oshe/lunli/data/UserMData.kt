package com.firefly.oshe.lunli.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserMData(private val context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_user_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isUserExist(userId: String): Boolean {
        return getAllUsers().containsKey(userId)
    }

    fun saveUser(userData: UserData) {
        val users = getAllUsers().toMutableMap()
        users[userData.userId] = userData
        saveAllUsers(users)
    }

    fun getAllUsers(): Map<String, UserData> {
        val json = prefs.getString(KEY_USERS, "") ?: ""
        return try {
            Gson().fromJson(json, object : TypeToken<Map<String, UserData>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getUser(userId: String): UserData? {
        return getAllUsers()[userId]
    }

    fun deleteUser(userId: String) {
        val users = getAllUsers().toMutableMap()
        users.remove(userId)
        saveAllUsers(users)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun validateUser(user: UserData): Boolean {
        val storedUser = getUser(user.userId)
        val isValid = storedUser?.password == user.password
        if (!isValid) {
            storedUser?.hasPasswordError = true
            storedUser?.let { saveUser(it) }
        }
        return isValid
    }

    fun clearPasswordError(userId: String) {
        getUser(userId)?.let {
            it.hasPasswordError = false
            saveUser(it)
        }
    }

    private fun saveAllUsers(users: Map<String, UserData>) {
        val json = Gson().toJson(users)
        prefs.edit().putString(KEY_USERS, json).apply()
    }

    companion object {
        private const val KEY_USERS = "user_data_map"
        private const val KEY_LAST_USER = "last_login_user"
        
        fun setLastUser(context: Context, userId: String) {
            context.getSharedPreferences("user_status", Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_USER, userId)
                .apply()
        }

        fun getLastUser(context: Context): String? {
            return context.getSharedPreferences("user_status", Context.MODE_PRIVATE)
                .getString(KEY_LAST_USER, null)
        }
    }
}