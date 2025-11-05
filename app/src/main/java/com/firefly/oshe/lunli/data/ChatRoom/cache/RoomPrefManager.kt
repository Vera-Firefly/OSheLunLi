package com.firefly.oshe.lunli.data.ChatRoom.cache

import android.content.Context
import android.content.SharedPreferences
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import org.json.JSONArray
import org.json.JSONObject

class RoomPrefManager(private val context: Context, private val userId: String) {

    companion object {
        private const val KEY_SAVED_ROOMS = "saved_rooms"
        private const val KEY_HIDDEN_ROOMS = "hidden_rooms"
    }

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences("RoomPrefs_$userId", Context.MODE_PRIVATE)
    }

    fun saveRoom(roomInfo: RoomInfo) {
        val savedRooms = getSavedRooms().toMutableList()
        val existingIndex = savedRooms.indexOfFirst { it.id == roomInfo.id }
        if (existingIndex != -1) {
            savedRooms[existingIndex] = roomInfo
        } else {
            savedRooms.add(roomInfo)
        }
        saveRoomsToPref(KEY_SAVED_ROOMS, savedRooms)
    }

    fun saveHiddenRoom(roomInfo: RoomInfo) {
        val hiddenRooms = getHiddenRooms().toMutableList()
        val existingIndex = hiddenRooms.indexOfFirst { it.id == roomInfo.id }
        if (existingIndex != -1) {
            hiddenRooms[existingIndex] = roomInfo
        } else {
            hiddenRooms.add(roomInfo)
        }
        saveRoomsToPref(KEY_HIDDEN_ROOMS, hiddenRooms)
    }

    fun getSavedRooms(): List<RoomInfo> {
        return getRoomsFromPref(KEY_SAVED_ROOMS)
    }

    fun getHiddenRooms(): List<RoomInfo> {
        return getRoomsFromPref(KEY_HIDDEN_ROOMS)
    }

    fun removeSavedRoom(roomId: String) {
        val savedRooms = getSavedRooms().toMutableList()
        savedRooms.removeAll { it.id == roomId }
        saveRoomsToPref(KEY_SAVED_ROOMS, savedRooms)
    }

    fun removeHiddenRoom(roomId: String) {
        val hiddenRooms = getHiddenRooms().toMutableList()
        hiddenRooms.removeAll { it.id == roomId }
        saveRoomsToPref(KEY_HIDDEN_ROOMS, hiddenRooms)
    }

    fun containsRoom(roomId: String): Boolean {
        return getSavedRooms().any { it.id == roomId } || getHiddenRooms().any { it.id == roomId }
    }

    fun getRoomById(roomId: String): RoomInfo? {
        return getSavedRooms().find { it.id == roomId } ?: getHiddenRooms().find { it.id == roomId }
    }

    private fun saveRoomsToPref(key: String, rooms: List<RoomInfo>) {
        val roomsJsonArray = rooms.joinToString(",") { roomInfo ->
            JSONObject().apply {
                put("id", roomInfo.id)
                put("title", roomInfo.title)
                put("creator", roomInfo.creator)
                put("roomMessage", roomInfo.roomMessage)
                put("roomPassword", roomInfo.roomPassword)
            }.toString()
        }
        sharedPref.edit().putString(key, "[$roomsJsonArray]").apply()
    }

    private fun getRoomsFromPref(key: String): List<RoomInfo> {
        val roomsJsonString = sharedPref.getString(key, "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(roomsJsonString)
            List(jsonArray.length()) { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                RoomInfo(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    creator = jsonObject.getString("creator"),
                    roomMessage = jsonObject.getString("roomMessage"),
                    roomPassword = jsonObject.getString("roomPassword")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAllRooms() {
        sharedPref.edit().remove(KEY_SAVED_ROOMS).remove(KEY_HIDDEN_ROOMS).apply()
    }
}