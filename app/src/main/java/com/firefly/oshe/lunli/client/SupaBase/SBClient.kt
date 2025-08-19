package com.firefly.oshe.lunli.client.SupaBase

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
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

object SBClient {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = Token.supabaseAPI(),
        supabaseKey = Token.supabaseToken()
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(io.github.jan.supabase.realtime.Realtime)
        httpEngine = Android.create()
    }

    fun createUser(userId: String, username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["users"].insert(
                    User(id = userId, name = username)
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

    fun sendMessage(roomId: String, userId: String, content: String, callback: (Boolean) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.postgrest["messages"].insert(
                    NewMessage(room_id = roomId, user_id = userId, content = content)
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

    suspend fun fetchMessages(roomId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            client.from("messages")
                .select {
                    filter {
                        eq("room_id", roomId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Message>()
        }
    }

    fun subscribeMessages(roomId: String, onNewMessage: (Message) -> Unit = {}): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {

                val channel = client.realtime.channel("messages:room_$roomId")

                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter = "room_id=eq.$roomId"
                }.collectLatest { change ->
                    val newMessage = change.record
                    val message = Message(
                        id = newMessage["id"].toString(),
                        room_id = newMessage["room_id"].toString(),
                        user_id = newMessage["user_id"].toString(),
                        content = newMessage["content"].toString(),
                        created_at = newMessage["created_at"].toString()
                    )
                    onNewMessage(message)
                }

                channel.subscribe()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Serializable
data class User(val id: String, val name: String)
@Serializable
data class RoomId(val id: String)
@Serializable
data class NewMessage(val room_id: String, val user_id: String, val content: String)
@Serializable
data class Message(
    val id: String,
    val room_id: String,
    val user_id: String,
    val content: String,
    val created_at: String
)
