package com.firefly.oshe.lunli.data.ChatRoom.cache

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Room
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.firefly.oshe.lunli.data.ChatRoom.Message
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private class ChatData {

    @Entity(
        tableName = "chat_messages",
        indices = [
            Index(value = ["room_id", "timestamp"]),
            Index(value = ["timestamp"])
        ]
    )
    data class MessageEntity(
        @PrimaryKey val id: String,
        @ColumnInfo(name = "room_id") val roomId: String,
        @ColumnInfo(name = "sender") val sender: String,
        @ColumnInfo(name = "sender_image") val senderImage: String,
        @ColumnInfo(name = "content") val content: String,
        @ColumnInfo(name = "timestamp") val timestamp: Long,
        @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
    ) {
        fun toMessage(): Message {
            return Message(id, sender, senderImage, content)
        }
    }

    @Entity(tableName = "chat_rooms")
    data class RoomInfoEntity(
        @PrimaryKey val id: String,
        @ColumnInfo(name = "title") val title: String,
        @ColumnInfo(name = "creator") val creator: String,
        @ColumnInfo(name = "room_message") val roomMessage: String,
        @ColumnInfo(name = "room_password") val roomPassword: String,
        @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
        @ColumnInfo(name = "last_activity") val lastActivity: Long,
        @ColumnInfo(name = "created_time") val createdTime: Long = System.currentTimeMillis()
    ) {
        fun toRoomInfo(): RoomInfo {
            return RoomInfo(id, title, creator, roomMessage, roomPassword)
        }
    }

    @Dao
    interface ChatDao {
        @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY timestamp ASC")
        suspend fun getMessagesByRoom(roomId: String): List<MessageEntity>

        @Query("SELECT * FROM chat_messages WHERE room_id = :roomId AND timestamp > :sinceTime ORDER BY timestamp ASC")
        suspend fun getMessagesSince(roomId: String, sinceTime: Long): List<MessageEntity>

        @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY timestamp DESC LIMIT :limit")
        suspend fun getRecentMessages(roomId: String, limit: Int): List<MessageEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertMessage(message: MessageEntity)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertMessages(message: List<MessageEntity>)

        @Query("DELETE FROM chat_messages WHERE room_id = :roomId")
        suspend fun deleteMessagesByRoom(roomId: String)

        @Query("DELETE FROM chat_messages WHERE timestamp < :expireTime")
        suspend fun deleteExpiredMessages(expireTime: Long): Int

        @Query("SELECT COUNT(*) FROM chat_messages WHERE room_id = :roomId")
        suspend fun getMessageCount(roomId: String): Long?

        @Query("SELECT MAX(timestamp) FROM chat_messages WHERE room_id = :roomId")
        suspend fun getLastMessageTime(roomId: String): Long?

        @Query("SELECT COUNT(*) FROM chat_messages")
        suspend fun getTotalMessageCount(): Int

        @Query("SELECT * FROM chat_rooms")
        suspend fun getAllRooms(): List<RoomInfoEntity>

        @Query("SELECT * FROM chat_rooms WHERE is_hidden = :isHidden")
        suspend fun getRoomsByVisibility(isHidden: Boolean): List<RoomInfoEntity>

        @Query("SELECT * FROM chat_rooms WHERE id = :roomId")
        suspend fun getRoomById(roomId: String): RoomInfoEntity?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertRoom(room: RoomInfoEntity)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertRooms(rooms: List<RoomInfoEntity>)

        @Query("DELETE FROM chat_rooms WHERE id = :roomId")
        suspend fun deleteRoom(roomId: String)

        @Query("DELETE FROM chat_rooms WHERE last_activity < :expireTime")
        suspend fun deleteInactiveRooms(expireTime: Long)

        @Query("UPDATE chat_rooms SET last_activity = :timestamp WHERE id = :roomId")
        suspend fun updateRoomActivity(roomId: String, timestamp: Long)

        @Query("SELECT COUNT(*) FROM chat_rooms")
        suspend fun getTotalRoomCount(): Int
    }

    @Database(
        entities = [MessageEntity::class, RoomInfoEntity::class],
        version = 1,
        exportSchema = false
    )

    abstract class ChatDataBase : RoomDatabase() {
        abstract fun chatDao(): ChatDao

        companion object {
            @Volatile
            private var INSTANCE: ChatDataBase? = null

            fun getInstance(context: Context, userId: String): ChatDataBase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ChatDataBase::class.java,
                        "chat_database_$userId.db"
                    )
                        .setQueryExecutor(Executors.newSingleThreadExecutor())
                        .addCallback(
                            object : RoomDatabase.Callback() {
                                override fun onCreate(connection: SQLiteConnection) {
                                    super.onCreate(connection)
                                }
                            }
                        )
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}

class MessageCacheManager(private val context: Context, private val userId: String) {

    private val database: ChatData.ChatDataBase by lazy {
        ChatData.ChatDataBase.getInstance(context, userId)
    }

    private val chatDao: ChatData.ChatDao by lazy { database.chatDao() }

    companion object {
        private const val CACHE_DURATION = 30L * 24 * 60 * 60 * 1000
        private const val ROOM_INACTIVE_DURATION = 90L * 24 * 60 * 60 * 1000
        private const val BATCH_SIZE = 100
        private const val RECENT_MESSAGES_LIMIT = 500
    }


    suspend fun saveMessagesToCache(roomId: String, messages: List<Message>) {
        withContext(Dispatchers.IO) {
            try {
                val entities = messages.map { message ->
                    ChatData.MessageEntity(
                        message.id,
                        roomId,
                        message.sender,
                        message.senderImage,
                        message.content,
                        System.currentTimeMillis()
                    )
                }

                entities.chunked(BATCH_SIZE).forEach { batch ->
                    chatDao.insertMessages(batch)
                }

                chatDao.updateRoomActivity(roomId, System.currentTimeMillis())

            } catch (e: Exception) { }
        }
    }

    suspend fun saveSingleMessage(roomId: String, message: Message) {
        withContext(Dispatchers.IO) {
            try {
                val entity = ChatData.MessageEntity(
                    message.id,
                    roomId,
                    message.sender,
                    message.senderImage,
                    message.content,
                    System.currentTimeMillis()
                )
                chatDao.insertMessage(entity)
                chatDao.updateRoomActivity(roomId, System.currentTimeMillis())
            } catch (e: Exception) { }
        }
    }

    suspend fun loadCachedMessages(roomId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = chatDao.getRecentMessages(roomId, RECENT_MESSAGES_LIMIT)
                entities.map { it.toMessage() }.reversed()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun loadMessagesSince(roomId: String, sinceTime: Long): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = chatDao.getMessagesSince(roomId, sinceTime)
                entities.map { it.toMessage() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getLastMessageTime(roomId: String): Long {
        return withContext(Dispatchers.IO) {
            chatDao.getLastMessageTime(roomId) ?: 0
        }
    }

    suspend fun saveRoom(roomInfo: RoomInfo, isHidden: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val entity = ChatData.RoomInfoEntity(
                    roomInfo.id,
                    roomInfo.title,
                    roomInfo.creator,
                    roomInfo.roomMessage,
                    roomInfo.roomPassword,
                    isHidden,
                    System.currentTimeMillis()
                )
                chatDao.insertRoom(entity)
            } catch (e: Exception) { }
        }
    }

    suspend fun saveRooms(rooms: List<RoomInfo>, isHidden: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val entities = rooms.map { roomInfo ->
                    ChatData.RoomInfoEntity(
                        roomInfo.id,
                        roomInfo.title,
                        roomInfo.creator,
                        roomInfo.roomMessage,
                        roomInfo.roomPassword,
                        isHidden,
                        System.currentTimeMillis()
                    )
                }
                chatDao.insertRooms(entities)
            } catch (e: Exception) { }
        }
    }

    suspend fun getAllRooms(): List<RoomInfo> {
        return withContext(Dispatchers.IO) {
            try {
                chatDao.getAllRooms().map { it.toRoomInfo() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getRoomsByVisibility(isHidden: Boolean): List<RoomInfo> {
        return withContext(Dispatchers.IO) {
            try {
                chatDao.getRoomsByVisibility(isHidden).map { it.toRoomInfo() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getRoomById(roomId: String): RoomInfo? {
        return withContext(Dispatchers.IO) {
            try {
                chatDao.getRoomById(roomId)?.toRoomInfo()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun updateRoomActivity(roomId: String) {
        withContext(Dispatchers.IO) {
            chatDao.updateRoomActivity(roomId, System.currentTimeMillis())
        }
    }

    suspend fun clearRoomMessages(roomId: String) {
        withContext(Dispatchers.IO) {
            try {
                chatDao.deleteMessagesByRoom(roomId)
                chatDao.deleteRoom(roomId)
            } catch (e: Exception) { }
        }
    }

    suspend fun clearExpiredMessages() {
        withContext(Dispatchers.IO) {
            try {
                val expireTime = System.currentTimeMillis() - CACHE_DURATION
                chatDao.deleteExpiredMessages(expireTime)
            } catch (e: Exception) { }
        }
    }

    suspend fun clearInactiveRooms() {
        withContext(Dispatchers.IO) {
            try {
                val expireTime = System.currentTimeMillis() - ROOM_INACTIVE_DURATION
                chatDao.deleteInactiveRooms(expireTime)
            } catch (e: Exception) { }
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            try {
                val allRooms = chatDao.getAllRooms()
                allRooms.forEach { room ->
                    chatDao.deleteMessagesByRoom(room.id)
                }

                allRooms.forEach { room ->
                    chatDao.deleteRoom(room.id)
                }
            } catch (e: Exception) { }
        }
    }

    suspend fun clearUserData() {
        withContext(Dispatchers.IO) {
            clearAllData()
        }
    }

    suspend fun getCacheStatistics(): CacheStats {
        return withContext(Dispatchers.IO) {
            try {
                val totalMessages = chatDao.getTotalMessageCount()
                val totalRooms = chatDao.getTotalRoomCount()

                CacheStats(
                    totalRooms,
                    totalMessages,
                    0
                )
            } catch (e: Exception) {
                CacheStats(0, 0, 0)
            }
        }
    }

    suspend fun hasCachedMessages(roomId: String): Boolean {
        return withContext(Dispatchers.IO) {
            chatDao.getMessageCount(roomId)!! > 0
        }
    }

    suspend fun getCachedRoomIds(): List<String> {
        return withContext(Dispatchers.IO) {
            chatDao.getAllRooms().map { it.id }
        }
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