package com.firefly.oshe.lunli.client.SupaBase

import android.system.ErrnoException
import com.firefly.oshe.lunli.client.Token
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.ktor.http.content.Version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.Dispatcher
import java.sql.Timestamp

object SBClient {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = Token.supabaseAPI(),
        supabaseKey = Token.supabaseToken()
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(io.github.jan.supabase.realtime.Realtime)
        httpEngine = io.ktor.client.engine.cio.CIO.create()
    }

    fun createUser(userId: String, username: String, userImage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["users"].insert(
                    User(id = userId, name = username, image = userImage)
                )
                println("User inserted: $userId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchUser(userId: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                client.from("users")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<User>()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun updateUser(userId: String, userName: String, userImage: String, callback: (Boolean) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["users"]
                    .update({
                        set("name", userName)
                        set("image", userImage)
                    }
                    ) {
                        filter {
                            eq("id",userId)
                        }
                    }
                CoroutineScope(Dispatchers.Main).launch {
                    callback(true)
                }
                println("User Name Update: $userName")
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(false)
                }
                e.printStackTrace()
            }
        }
    }

    fun createRoom(roomId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["rooms"].insert(
                    RoomId(id = roomId)
                )
                println("Room created: $roomId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(messageId: String, roomId: String, userId: String, content: String, callback: (Boolean) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["messages"].insert(
                    NewMessage(messageId, roomId, userId, content)
                )
                client.postgrest["messageid"].insert(
                    NewMessageId(messageId, roomId)
                )
                withContext(Dispatchers.Main) {
                    callback(true)
                }
                println("Message sent")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchMessageId(roomId: String, timestamp: String = ""): List<MessageId> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("messageid")
                    .select {
                        filter {
                            eq("room_id", roomId)
                            if (timestamp != "") gt("created_at", timestamp.replace("+0000", "+00:00"))
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<MessageId>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchMessages(messageId: String, roomId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("messages")
                    .select {
                        filter {
                            eq("id", messageId)
                            eq("room_id", roomId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Message>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchMessages(roomId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("messages")
                    .select {
                        filter {
                            eq("room_id", roomId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Message>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // SupaBase数据库并未实现RealTime, 暂时搁置
    fun subscribeMessages(roomId: String): Flow<Message> = callbackFlow {
        val channel = client.realtime.channel("messages:$roomId")
        channel.subscribe()
        val job = launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }.collect { change ->
                val newMessage = change.record
                val message = Message(
                    id = newMessage["id"]?.toString().orEmpty(),
                    room_id = newMessage["room_id"]?.toString().orEmpty(),
                    user_id = newMessage["user_id"]?.toString().orEmpty(),
                    content = newMessage["content"]?.toString().orEmpty(),
                    created_at = newMessage["created_at"]?.toString().orEmpty()
                )
                println("Received message: $message")
                trySend(message)
            }
        }

        awaitClose {
            println("Unsubscribed from messages")
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun subscribeNewVersion(version: String): List<NewVersion> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("version")
                    .select {
                        filter {
                            gt("tag_name", version)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<NewVersion>()
            } catch (e : ErrnoException) {
                emptyList()
            }
        }
    }

    suspend fun subscribeAnnouncement(date: String): List<Announcement> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("announcement")
                    .select {
                        filter {
                            gt("date", date)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Announcement>()
            } catch (e: ErrnoException) {
                emptyList()
            }
        }
    }

    @Serializable
    data class User(val id: String, val name: String, val image: String)

    @Serializable
    data class RoomId(val id: String)

    @Serializable
    data class NewMessage(
        val id: String,
        val room_id: String,
        val user_id: String,
        val content: String
    )

    @Serializable
    data class Message(
        val id: String,
        val room_id: String,
        val user_id: String,
        val content: String,
        val created_at: String
    )

    @Serializable
    data class NewMessageId(
        val id: String,
        val room_id: String
    )

    @Serializable
    data class MessageId(
        val id: String,
        val room_id: String,
        val created_at: String
    )

    @Serializable
    data class NewVersion(
        val tag_name: String,
        val name: String,
        val body: String,
        val url: String,
        val created_at: String
    )

    @Serializable
    data class Announcement(
        val date: String,
        val body: String,
        val created_at: String
    )
}

