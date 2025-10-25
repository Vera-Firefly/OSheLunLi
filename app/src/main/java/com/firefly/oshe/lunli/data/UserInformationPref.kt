package com.firefly.oshe.lunli.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserInformationPref(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveInformation(information: UserInformation) {
        val infs = getAllInformation().toMutableMap()
        infs[information.userId] = information
        saveAllInformation(infs)
    }

    fun getInformation(userId: String): UserInformation? {
        return getAllInformation()[userId]
    }

    fun getAllInformation(): Map<String, UserInformation> {
        val json = prefs.getString(KEY_DATA, "") ?: ""
        return try {
            Gson().fromJson(json, object : TypeToken<Map<String, UserInformation>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun deleteInformation(userId: String) {
        val infs = getAllInformation().toMutableMap()
        infs.remove(userId)
        saveAllInformation(infs)
    }

    fun clearAllInformation() {
        prefs.edit().remove(KEY_DATA).apply()
    }

    private fun saveAllInformation (infors: Map<String, UserInformation>) {
        val json = Gson().toJson(infors)
        prefs.edit().putString(KEY_DATA, json).apply()
    }

    companion object {
        private const val PREF_NAME = "user_information_prefs"
        private const val KEY_DATA = "user_information_data"
    }
}