package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.ChatRoom.Message
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import com.firefly.oshe.lunli.data.ChatRoom.cache.MessageCacheManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.RoomPrefManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.SeparateUserCacheManager
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures.RoomAdapterView.RoomAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.forEach

class MessageLoading(private val context: Context, private val userData: UserData) {

    private val client by lazy { Client(context) }
    private var currentRoomId: String? = null
    private var messageSubscription: Job? = null
    private var currentSubscription: Job? = null
    private var pollingJob: Job? = null
    private var lastPollTime: Long = 0
    private val roomPrefManager by lazy { RoomPrefManager(context, userData.userId) }
    private val userCacheManager by lazy { SeparateUserCacheManager(context) }
    private val userMessageCacheManager by lazy { MessageCacheManager(context, userData.userId) }

    private val roomAdapterView by lazy { RoomAdapterView(context, userData) }
    private var isLoading: Boolean = false

    private val chatAdapterView by lazy { ChatAdapterView(context) }

    fun parseRoomInfo(json: String): RoomInfo {
        val obj = JSONObject(json)
        return RoomInfo(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            creator = obj.optString("creator", ""),
            roomMessage = obj.optString("roomMessage", ""),
            roomPassword = obj.optString("roomPassword", "")
        )
    }

    suspend fun getUserInf(userId: String): UserInformation {
        return try {
            val (userName, userImage) = userCacheManager.getUserDisplayInfo(userId)
            UserInformation(userId, userName, userImage, "")
        } catch (e: Exception) {
            UserInformation(userId, "未知用户", "NULL", "")
        }
    }


    fun loadRooms(callback: (Boolean) -> Unit = {}) {
        if (isLoading) {
            context.ShowToast("正在加载房间列表, 请稍后...")
            callback(false)
        } else {
            isLoading = true
            callback(true)
        }
    }

    fun loadRoomsFromClient(hide: Boolean, callback: (Boolean) -> Unit) {
        val path =  if (hide){"HideRoomInfo"} else "RoomInfo"
        client.getDir(path, object : Client.ResultCallback {
            override fun onSuccess(content: String) {
                processRoomDirectory(path, content)
                callback(true)
            }

            override fun onFailure(error: String) {
                isLoading = false
                callback(false)
            }
        })
    }

    fun uploadRoomToClient(hide: Boolean, roomInfo: RoomInfo, callback: Client.ResultCallback) {
        try {
            val room = JSONObject().apply {
                put("id", roomInfo.id)
                put("title", roomInfo.title)
                put("creator", roomInfo.creator)
                put("roomMessage", roomInfo.roomMessage)
                put("roomPassword", roomInfo.roomPassword)
            }.toString()

            val path =  if (hide){"HideRoomInfo"} else "RoomInfo"

            client.uploadData(path, roomInfo.id, room, callback)
        } catch (e: Exception) {
            callback.onFailure("Failed To Create Room: ${e.message}")
        }
    }

    fun processRoomDirectory(path: String, content: String) {
        try {
            val jsonArray = JSONArray(content)
            val rooms = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val roomName = json.getString("name")
                if (roomName.endsWith(".json"))
                    rooms.add(roomName.replace(".json", ""))
            }

            if (rooms.isNotEmpty()) {
                client.getMultipleFiles(path, rooms, object : Client.ResultCallback {
                    override fun onSuccess(content: String) {
                        processRoomMessage(content)
                    }

                    override fun onFailure(error: String) {
                        // TODO:
                    }
                })
            }
        } catch (e: Exception) {
            // TODO:
        }
    }

    fun processRoomMessage(content: String) {
        try {
            val result = JSONObject(content)
            val keys = result.keys()
            val serverRoomIds = mutableListOf<String>()

            while (keys.hasNext()) {
                val key = keys.next()
                val roomJson = result.getString(key)
                val roomInfo = parseRoomInfo(roomJson)
                serverRoomIds.add(roomInfo.id)

                val localLockedRoom = roomPrefManager.getRoomById(roomInfo.id)
                val localHideRoom = roomPrefManager.getRoomById(roomInfo.id)
                if (localLockedRoom != null && localLockedRoom.roomPassword != roomInfo.roomPassword) {
                    roomPrefManager.removeSavedRoom(roomInfo.id)
                    context.ShowToast("房间 ${roomInfo.title} 密码已更新，请重新输入")
                }

                if (localHideRoom != null && localHideRoom.roomPassword != roomInfo.roomPassword) {
                    roomPrefManager.removeHiddenRoom(roomInfo.id)
                    context.ShowToast("房间 ${roomInfo.title} 密码已更新，请重新输入")
                }

                (roomAdapterView.roomAdapter as? RoomAdapter)?.addRoomIfNotExists(roomInfo)
            }

            val allLocalRooms = roomPrefManager.getSavedRooms() + roomPrefManager.getHiddenRooms()
            allLocalRooms.forEach { localRoom ->
                if (!serverRoomIds.contains(localRoom.id) &&
                    !roomPrefManager.getHiddenRooms().any { it.id == localRoom.id }) {
                    roomPrefManager.removeSavedRoom(localRoom.id)
                }
            }

        } catch (e: Exception) {
            isLoading = false
        } finally {
            isLoading = false
        }
    }

    fun detectRoomPassword(room: RoomInfo, callback: (Boolean) -> Unit) {
        if (room.roomPassword.equals("Null")) {
            currentRoomId = room.id
            loadRoomMessages(room.id)
            callback(true)
        } else {
            val savedRoom = roomPrefManager.getRoomById(room.id)
            if (savedRoom != null && savedRoom.roomPassword == room.roomPassword) {
                currentRoomId = room.id
                loadRoomMessages(room.id)
                context.ShowToast("Hello!\n${userData.userName} (${userData.userId})")
                callback(true)
            } else {
                val view = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
                }
                val input = TextInputEditText(context).apply {
                    hint = "房间密码"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setTextColor(Color.BLACK)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.WHITE)
                        cornerRadius = 8.dp.toFloat()
                        setStroke(1, Color.argb(50, 0, 0, 0))
                    }
                    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                }
                view.addView(input, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

                MaterialAlertDialogBuilder(context)
                    .setTitle("请输入房间密码")
                    .setView(view)
                    .setPositiveButton("确认") { _, _ ->
                        val password = input.text.toString()
                        if (password == room.roomPassword) {
                            roomPrefManager.saveRoom(room)
                            currentRoomId = room.id
                            loadRoomMessages(room.id)
                            context.ShowToast("Hello!\n${userData.userName} (${userData.userId})")
                            callback(true)
                        } else {
                            context.ShowToast("错误的密码")
                            callback(false)
                        }
                    }
                    .setNegativeButton("取消") { _, _ ->
                        callback(false)
                    }
                    .show()
            }
        }
    }

    fun loadRoomMessages(roomId: String) {
        unsubscribeFromMessages()
        currentRoomId = roomId
        (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.clearMessages()

        val cachedMessages = userMessageCacheManager.loadCachedMessages(roomId)
        cachedMessages.forEach { message ->
            (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
        }
        loadRoomMessagesll(roomId)
    }

    fun loadRoomMessagesll(roomId: String) {
        unsubscribeFromMessages()

        messageSubscription = CoroutineScope(Dispatchers.Main).launch {
            loadExistingMessages(roomId)
            // subscribeToMessages(roomId)
            startPollingMessages(roomId)
        }
    }

    /**
     * 订阅消息: 功能不可用, 待SupaBase数据路完善RealTime后可使用
     */
    fun subscribeToMessages(roomId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            currentSubscription = launch {
                try {
                    SBClient.subscribeMessages(roomId).collect { message ->
                        val processedMessage = withContext(Dispatchers.IO) {
                            val sender = getUserInf(message.user_id)
                            Message(
                                message.id,
                                "${sender.userName} (${message.user_id})",
                                sender.userImage,
                                message.content
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (currentRoomId == roomId) {
                                (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(
                                    processedMessage
                                )/*
                                chatRecyclerView?.scrollToPosition(
                                    (chatAdapter?.itemCount ?: 1) - 1
                                )*/
                            }
                            (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.getMessages()?.let { messages ->
                                userMessageCacheManager.saveMessagesToCache(roomId, messages)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (currentRoomId == roomId) {
                        withContext(Dispatchers.Main) {
                            context.ShowToast("订阅消息失败: ${e.message}")
                        }
                    }
                } finally {
                    context.ShowToast("订阅消息结束")
                }
            }
        }
    }

    fun startPollingMessages(roomId: String) {
        unsubscribeFromMessages()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentRoomId == roomId) {
                try {
                    val messages = SBClient.fetchMessages(roomId)

                    val newMessages = if (lastPollTime > 0) {
                        messages.filter { dbMessage ->
                            val messageTime = parseIso8601WithTimezone(dbMessage.created_at)
                            messageTime > lastPollTime
                            // it.created_at.toLongOrNull() ?: 0 > lastPollTime
                        }
                    } else {
                        emptyList()
                    }

                    if (newMessages.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            processNewMessages(newMessages)
                        }

                        lastPollTime = newMessages.maxOf { dbMessage ->
                            parseIso8601WithTimezone(dbMessage.created_at)
                        }
                    } else {
                        lastPollTime = System.currentTimeMillis()
                    }

                    delay(3000L)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        context.ShowToast("轮询消息失败: ${e.message}")
                    }
                    delay(5000L)
                }
            }
        }
    }

    fun parseIso8601WithTimezone(isoString: String): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(isoString).toEpochMilli()
            } else {
                val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                formatter.parse(isoString)?.time ?: 0L
            }
        } catch (e: Exception) {
            try {
                val simplifiedString = isoString.substringBefore(".") + "Z"
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
                simpleDateFormat.parse(simplifiedString)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    fun processNewMessages(messages: List<SBClient.Message>) {
        CoroutineScope(Dispatchers.IO).launch {
            messages.forEach { dbMessage ->
                val sender = getUserInf(dbMessage.user_id)
                val message = Message(
                    dbMessage.id,
                    "${sender.userName} (${dbMessage.user_id})",
                    sender.userImage,
                    dbMessage.content
                )
                withContext(Dispatchers.Main) {
                    (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
                    // chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
                }

                (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.getMessages()?.let { allMessages ->
                    userMessageCacheManager.saveMessagesToCache(currentRoomId ?: return@let, allMessages)
                }
            }
        }
    }

    suspend fun loadExistingMessages(roomId: String) {
        try {
            val messages = withContext(Dispatchers.IO) {
                SBClient.fetchMessages(roomId)
            }

            /**
             * 轮询时间戳
             */
            if (messages.isNotEmpty()) {
                lastPollTime = messages.maxOf { dbMessage ->
                    parseIso8601WithTimezone(dbMessage.created_at)
                }
            } else {
                lastPollTime = System.currentTimeMillis()
            }

            messages.forEach { dbMessage ->
                val sender = getUserInf(dbMessage.user_id)
                val message = Message(
                    dbMessage.id,
                    "${sender.userName} (${dbMessage.user_id})",
                    sender.userImage,
                    dbMessage.content
                )
                if (currentRoomId == roomId) {
                    (chatAdapterView.chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
                }
            }
            withContext(Dispatchers.Main) {
                // chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                context.ShowToast("加载消息失败: ${e.message}")
            }
        }
    }

    fun unsubscribeFromMessages() {
        /**
         * 取消轮询, SupaBase RealTime未实现
         */
        pollingJob?.cancel()
        pollingJob = null
        lastPollTime = 0

        /**
         * 取消订阅
         */
        messageSubscription?.cancel()
        messageSubscription = null
        currentSubscription?.cancel()
        currentSubscription = null
    }
}