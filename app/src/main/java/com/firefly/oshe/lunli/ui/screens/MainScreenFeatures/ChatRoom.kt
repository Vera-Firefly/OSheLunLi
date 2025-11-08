package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.text.InputType
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.data.ChatRoom.Message
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.ChatRoom.cache.MessageCacheManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.SeparateUserCacheManager
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.dialog.CropDialog
import com.firefly.oshe.lunli.utils.ImageUtils
import com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures.RoomAdapterView
import com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures.ChatAdapterView
import com.firefly.oshe.lunli.utils.Iso8601Converter
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
import java.util.*
import kotlin.collections.mutableListOf

class ChatRoom(
    private val context: Context,
    private val userData: UserData
) {

    private lateinit var mainView: LinearLayout
    private lateinit var client: Client
    private lateinit var cropDialog: CropDialog

    private var roomAdapter: RecyclerView.Adapter<*>? = null
    private var chatAdapter: RecyclerView.Adapter<*>? = null
    private var roomRecyclerView: RecyclerView? = null
    private var chatRecyclerView: RecyclerView? = null

    private lateinit var roomAdapterView: RoomAdapterView
    private lateinit var chatAdapterView: ChatAdapterView

    private var roomSelection: LinearLayout? = null
    private var chatRoom: LinearLayout? = null
    private var exitRoom: LinearLayout? = null
    private var roomStatus: LinearLayout? = null
    private var refreshRoom: ShapeableImageView? = null
    private var roomStatus_add: ShapeableImageView? = null
    private var roomStatus_done: ShapeableImageView? = null

    private var currentRoomId: String? = null
    private val userCacheManager by lazy {
        SeparateUserCacheManager(context)
    }
    private val messageCacheManager by lazy {
        MessageCacheManager(context, userData.userId)
    }

    private val interaction by lazy {
        Interaction(context)
    }

    private var messageSubscription: Job? = null
    private var currentSubscription: Job? = null

    private var pollingJob: Job? = null

    private val processedMessageIds = mutableSetOf<String>()

    fun interface OnBackClickListener {
        fun onBackClicked()
    }

    private var backClickListener: OnBackClickListener? = null

    fun setOnBackClickListener(listener: OnBackClickListener) {
        this.backClickListener = listener
    }

    fun interface OnRoomSelectedListener {
        fun onRoomSelected()
    }

    private var roomSelectedListener: OnRoomSelectedListener? = null

    fun setOnRoomSelectedListener(listener: OnRoomSelectedListener) {
        this.roomSelectedListener = listener
    }

    private var isAddNewRoom: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            roomAdapterView.setAddNewRoomMode(value)
        }

    private var isLoading: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

            initializeAdapters()
            createRoomSelectionView()
            createChatRoomView()
        }
        return mainView
    }

    private fun initializeAdapters() {
        loadRooms { callback ->
            if (callback) {
                loadRoomsFromClient(false) {
                    if (it) loadRoomsFromClient(true) {}
                }
            } else {
                // TODO:
            }
        }

        roomAdapterView = RoomAdapterView(
            context,
            userData,
            messageCacheManager,
            Client(context),
            { room ->
                currentRoomId = room.id
                mainView.removeAllViews()
                mainView.addView(chatRoom)
                loadRoomMessages(room.id)
                addExitRoom(room)
                roomSelectedListener?.onRoomSelected()
            },
            { room ->
                // 留着有用?
            },
            { room ->
                // 留着有用?
            }
        )

        chatAdapterView = ChatAdapterView()
    }

    private fun addExitRoom(roomInfo: RoomInfo) {
        exitRoom = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            orientation = HORIZONTAL
            gravity = Gravity.START

            val exitButton = ShapeableImageView(context).apply {
                setImageResource(R.drawable.arrow_left_bold)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(8f.dp)
                    .build()
                setOnClickListener {
                    unsubscribeFromMessages()
                    (chatAdapter as? ChatAdapterView.ChatAdapter)?.getMessages()?.let { messages ->
                        CoroutineScope(Dispatchers.IO).launch {
                            messageCacheManager.saveMessagesToCache(roomInfo.id, messages)
                        }
                    }
                    backClickListener?.onBackClicked()
                    mainView.removeAllViews()
                    roomSelection?.let { mainView.addView(it) }
                }
                setPadding(6.dp, 6.dp, 6.dp, 6.dp)
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            val title = MaterialTextView(context).apply {
                text = roomInfo.title
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            addView(exitButton, 36.dp, 36.dp)
            addView(title)
        }
    }

    fun getTopBarComponent(): LinearLayout? {
        return exitRoom
    }

    fun createInputContainer(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(2.dp, 2.dp, 2.dp, 2.dp)
            }
            orientation = HORIZONTAL

            val inputBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }

            val inputEditText = TextInputEditText(context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    weight = 1f
                }
                background = inputBackground
                hint = "输入(支持MarkDown)..."
                setTextColor(Color.BLACK)
                setPadding(2.dp, 2.dp, 2.dp, 2.dp)
                minLines = 1
                maxLines = 3
            }

            val sendButton = ImageView(context).apply {
                layoutParams = LayoutParams(30.dp, 30.dp).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    marginStart = 2.dp
                }
                setImageResource(R.drawable.send)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                isClickable = true
                isFocusable = true
                contentDescription = "SendButton"

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .scaleX(0.7f)
                                .scaleY(0.7f)
                                .setDuration(60)
                                .withStartAction {
                                    v.pivotX = v.width / 2f
                                    v.pivotY = v.height / 2f
                                }
                                .start()
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            if (isAttachedToWindow) performClick()
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            true
                        }
                        else -> false
                    }
                }

                setOnClickListener {
                    val message = inputEditText.text.toString()
                    sendMessageToClient(message)
                    inputEditText.text?.clear()
                }
            }

            addView(inputEditText)
            addView(sendButton)
        }
    }

    private fun sendMessageToClient(message: String) {
        val currentId = UUID.randomUUID().toString()
        val inf = UserInformationPref(context).getInformation(userData.userId)
        var image = "NULL"
        inf?.userImage?.let { image = it }
        if (message.isNotEmpty()) {
            addMessage(
                Message(
                    currentId,
                    userData.userName + " (" + userData.userId + ")",
                    image,
                    message,
                    Iso8601Converter.nowAsUtcZeroOffset()
                )
            )
            currentRoomId?.let { roomId ->
                SBClient.sendMessage(currentId, roomId, userData.userId, message) {
                    if (!it) {
                        context.ShowToast("$currentId: 发送失败")
                    }
                }
            }
        }
    }

    fun createEndBarContainer(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER

            val buttons = listOf(
                createImageButton(R.drawable.picture) {
                    interaction.ImageRequestCallBack("ChatRoom") { bitmap ->
                        cropDialog = CropDialog(context)
                        showCropDialog(bitmap) { cropBitmap ->
                            val base64 = ImageUtils.bitmapToBase64(cropBitmap)
                            sendMessageToClient(base64)
                        }
                    }
                },
                createImageButton(R.drawable.camera) {
                    context.ShowToast("摄像功能待开发中")
                },
                createImageButton(R.drawable.smile) {
                    context.ShowToast("表情包功能待开发中")
                },
                createImageButton(R.drawable.add_btn) {
                    context.ShowToast("更多功能待开发中")
                }
            )

            buttons.forEach { button ->
                addView(button, LayoutParams(0, 28.dp, 1f).apply {
                    gravity = Gravity.CENTER
                })
            }
        }
    }

    private fun createImageButton(iconRes: Int, onClick: () -> Unit = {}): ShapeableImageView {
        return ShapeableImageView(context).apply {
            setImageResource(iconRes)
            layoutParams = LayoutParams(28.dp, 28.dp)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(4f.dp)
                .build()

            setOnClickListener { onClick() }
        }
    }

    private fun showCropDialog(bitmap: Bitmap, callBack: (Bitmap) -> Unit = {}) {
        cropDialog.showCropDialog(bitmap) { it ->
            it?.let {
                callBack(it)
            }
        }
        cropDialog.showAtLocation(mainView)
    }

    fun setRoomStatus(): LinearLayout? {
        onRoomStatus()
        return roomStatus
    }

    private fun onRoomStatus() {
        roomStatus = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            orientation = HORIZONTAL
            gravity = Gravity.END
        }

        refreshRoom = ShapeableImageView(context).apply {
            layoutParams = LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT
            ).apply {
                marginEnd = 4.dp
            }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)

            setImageResource(R.drawable.refresh)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                loadRooms { callback ->
                    if (callback) {
                        loadRoomsFromClient(false) {
                            if (it) loadRoomsFromClient(true) {}
                        }
                    } else {
                        // TODO:
                    }
                }
            }
        }

        roomStatus_add = ShapeableImageView(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginEnd = 4.dp
            }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)

            setImageResource(R.drawable.add_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                showRoomStatusSet(it)
            }
        }

        roomStatus_done = ShapeableImageView(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginEnd = 4.dp
            }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)

            setImageResource(R.drawable.select_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                isAddNewRoom = true
                roomStatus?.removeAllViews()
                refreshRoom?.let {
                    roomStatus?.addView(it, 36.dp, 36.dp)
                }
                roomStatus_add?.let {
                    roomStatus?.addView(it, 36.dp, 36.dp)
                }
            }
        }

        if (isAddNewRoom) {
            roomStatus?.removeAllViews()
            refreshRoom?.let {
                roomStatus?.addView(it, 36.dp, 36.dp)
            }
            roomStatus_add?.let {
                roomStatus?.addView(it, 36.dp, 36.dp)
            }
        } else {
            roomStatus?.removeAllViews()
            roomStatus_done?.let {
                roomStatus?.addView(it, 36.dp, 36.dp)
            }
        }
    }

    private fun showRoomStatusSet(view: View) {
        PopupMenu(context, view).apply {
            menu.add("新建房间").setOnMenuItemClickListener {
                addRoomDialog()
                true
            }
            menu.add("删除/离开房间").setOnMenuItemClickListener {
                context.ShowToast("单击房间以删除, 注意: 只能删除自己创建的房间, 否则你将被骗(PS: 这是AS推荐我加的一句)")
                isAddNewRoom = false
                roomStatus?.removeAllViews()
                roomStatus_done?.let {
                    roomStatus?.addView(it, 36.dp, 36.dp)
                }
                true
            }
            menu.add("加入房间").setOnMenuItemClickListener {
                showJoinHiddenRoomDialog()
                true
            }
            show()
        }
    }

    private fun showJoinHiddenRoomDialog() {
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }
        val input = TextInputEditText(context).apply {
            hint = "房间ID"
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
            .setTitle("输入房间ID")
            .setView(view)
            .setPositiveButton("加入") { _, _ ->
                val roomId = input.text.toString().trim()
                if (roomId.isNotEmpty()) {
                    joinHiddenRoom(roomId)
                } else {
                    context.ShowToast("请输入房间ID")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun joinHiddenRoom(roomId: String) {
        client.getData("HideRoomInfo", roomId, object : Client.ResultCallback {
            override fun onSuccess(content: String?) {
                content?.let { roomJson ->
                    try {
                        val roomInfo = parseRoomInfo(roomJson)
                        CoroutineScope(Dispatchers.IO).launch {
                            messageCacheManager.saveRoom(roomInfo, true)
                        }
                        roomAdapterView.addRoomIfNotExists(roomInfo)
                        context.ShowToast("已加入房间: $roomId")
                    } catch (e: Exception) {
                        context.ShowToast("房间信息解析失败")
                    }
                } ?: run {
                    context.ShowToast("未找到该房间")
                }
            }

            override fun onFailure(error: String?) {
                context.ShowToast("加入房间失败: $error")
            }
        })
    }

    private fun addRoomDialog() {
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }
        val titleInput = TextInputEditText(context).apply {
            hint = "房间标题"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }
        dialogView.addView(
            titleInput,
            LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp
            }
        )
        val messageInput = TextInputEditText(context).apply {
            hint = "房间描述"
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }
        dialogView.addView(
            messageInput,
            LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp
            }
        )

        val passwordInput = TextInputEditText(context).apply {
            hint = "房间密码, 留空则视为无密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }
        dialogView.addView(passwordInput, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val hideRoomCheckbox = CheckBox(context).apply {
            text = "隐藏房间"
            setTextColor(Color.BLACK)
            setPadding(0, 8.dp, 0, 8.dp)
        }
        dialogView.addView(hideRoomCheckbox, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setTitle("创建新房间")
            .setPositiveButton("确定") { _, _ ->
                val title = titleInput.text.toString()
                val roomMessage = messageInput.text.toString()
                val roomPassword = passwordInput.text.toString()
                val isHiddenRoom = hideRoomCheckbox.isChecked

                if (title.isEmpty()) {
                    context.ShowToast("请输入房间标题")
                    addRoomDialog()
                    return@setPositiveButton
                }

                val newRoom = RoomInfo(
                    "${userData.userId}-${System.currentTimeMillis()}",
                    title,
                    userData.userName + " (${userData.userId})",
                    roomMessage,
                    roomPassword.takeIf { it.isNotBlank() } ?: "Null"
                )

                uploadRoomToClient(isHiddenRoom, newRoom, object : Client.ResultCallback {
                    override fun onSuccess(content: String?) {
                        roomAdapterView.addRoom(newRoom)
                        SBClient.createRoom(newRoom.id)

                        if (isHiddenRoom) {
                            CoroutineScope(Dispatchers.IO).launch {
                                messageCacheManager.saveRoom(newRoom, true)
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("RoomID", newRoom.id)
                            clipboard.setPrimaryClip(clip)
                            context.ShowToast("房间ID已复制到剪切板")
                        } else {
                            context.ShowToast("房间创建成功")
                        }
                    }

                    override fun onFailure(error: String?) {
                        context.ShowToast("创建失败, 请重新创建")
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun LinearLayout.createRoomSelectionView() {
        roomSelection = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL

            roomRecyclerView = RecyclerView(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                roomAdapter = roomAdapterView.createAdapter()
                adapter = roomAdapter
            }
            addView(roomRecyclerView)
        }
        addView(roomSelection)
    }

    private fun LinearLayout.createChatRoomView() {
        chatRoom = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL

            chatRecyclerView = RecyclerView(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                chatAdapter = chatAdapterView.createAdapter()
                adapter = chatAdapter
            }
            addView(chatRecyclerView)
        }
    }

    private fun addMessage(message: Message) {
        (chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessage(message)
        chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
    }

    private fun loadRooms(callback: (Boolean) -> Unit = {}) {
        client = Client(context)
        if (isLoading) {
            context.ShowToast("正在加载房间列表, 请稍后...")
            callback(false)
        } else {
            isLoading = true
            callback(true)
        }
    }

    private fun loadRoomsFromClient(hide: Boolean, callback: (Boolean) -> Unit) {
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

    private fun uploadRoomToClient(hide: Boolean, roomInfo: RoomInfo, callback: Client.ResultCallback) {
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

    private fun processRoomDirectory(path: String, content: String) {
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
                        processRoomMessage(path, content)
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

    private fun processRoomMessage(path: String, content: String) {
        try {
            val result = JSONObject(content)
            val keys = result.keys()
            val serverRoomIds = mutableListOf<String>()

            while (keys.hasNext()) {
                val key = keys.next()
                val roomJson = result.getString(key)
                val roomInfo = parseRoomInfo(roomJson)
                serverRoomIds.add(roomInfo.id)

                CoroutineScope(Dispatchers.IO).launch {
                    val localRoom = messageCacheManager.getRoomById(roomInfo.id)
                    if (localRoom != null && localRoom.roomPassword != roomInfo.roomPassword) {
                        messageCacheManager.updateRoomPassword(roomInfo.id, roomInfo.roomPassword)
                        context.ShowToast("房间 ${roomInfo.title} 密码已更新，请重新输入")
                    }
                }

                if (path != "HideRoomInfo") roomAdapterView.addRoomIfNotExists(roomInfo)
            }

            CoroutineScope(Dispatchers.IO).launch {
                val allLocalRooms = messageCacheManager.getAllRooms()
                allLocalRooms.forEach { localRoom ->
                    if (!serverRoomIds.contains(localRoom.id) &&
                        !allLocalRooms.any { it.id == localRoom.id }
                    ) {
                        messageCacheManager.updateRoom(localRoom)
                    }
                }
            }

        } catch (e: Exception) {
            isLoading = false
        } finally {
            isLoading = false
        }
    }

    private fun parseRoomInfo(json: String): RoomInfo {
        val obj = JSONObject(json)
        return RoomInfo(
            obj.optString("id", ""),
            obj.optString("title", ""),
            obj.optString("creator", ""),
            obj.optString("roomMessage", ""),
            obj.optString("roomPassword", "")
        )
    }

    private suspend fun getUserInf(userId: String): UserInformation {
        return try {
            val (userName, userImage) = userCacheManager.getUserDisplayInfo(userId)
            UserInformation(userId, userName, userImage, "")
        } catch (e: Exception) {
            UserInformation(userId, "未知用户", "NULL", "")
        }
    }

    private fun loadRoomMessages(roomId: String) {
        unsubscribeFromMessages()
        currentRoomId = roomId
        processedMessageIds.clear()
        (chatAdapter as? ChatAdapterView.ChatAdapter)?.clearMessages()

        CoroutineScope(Dispatchers.IO).launch {
            val cachedMessages = messageCacheManager.loadCachedMessages(roomId)
            val lastTimestamp = messageCacheManager.getLastMessageTime(roomId)
            CoroutineScope(Dispatchers.Main).launch {
                cachedMessages.forEach { message ->
                    (chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
                    chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
                }
            }

            if (cachedMessages.isEmpty() || lastTimestamp == 0L) {
                loadExistingMessages(roomId)
                startPollingMessages(roomId)
            }
            else
            {
                startPollingMessages(roomId, Iso8601Converter.toUtcZeroOffsetFormat(lastTimestamp))
            }

        }
    }

    /**
     * 订阅消息: 功能不可用, 待SupaBase数据路完善RealTime后可使用
     */
    private fun subscribeToMessages(roomId: String) {
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
                                message.content,
                                message.created_at
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (currentRoomId == roomId) {
                                (chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(
                                    processedMessage
                                )
                                chatRecyclerView?.scrollToPosition(
                                    (chatAdapter?.itemCount ?: 1) - 1
                                )
                            }
                            (chatAdapter as? ChatAdapterView.ChatAdapter)?.getMessages()?.let { messages ->
                                messageCacheManager.saveMessagesToCache(roomId, messages)
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

    private fun startPollingMessages(roomId: String, sinceTimestamp: String = "") {
        unsubscribeFromMessages()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentRoomId == roomId) {
                try {
                    val messageIds = SBClient.fetchMessageId(roomId, sinceTimestamp)

                    if (messageIds.isNotEmpty()) {
                        val newMessages = mutableListOf<SBClient.Message>()

                        messageIds.forEach { messageIdObj ->
                            val messages = SBClient.fetchMessages(messageIdObj.id, roomId)
                            newMessages.addAll(messages)

                            processedMessageIds.add(messageIdObj.id)
                        }

                        if (newMessages.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                processNewMessages(roomId, newMessages)
                            }
                        }
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

    private fun processNewMessages(roomId: String, messages: List<SBClient.Message>) {
        CoroutineScope(Dispatchers.IO).launch {
            messages.forEach { dbMessage ->
                val sender = getUserInf(dbMessage.user_id)
                val message = Message(
                    dbMessage.id,
                    "${sender.userName} (${dbMessage.user_id})",
                    sender.userImage,
                    dbMessage.content,
                     dbMessage.created_at
                )
                withContext(Dispatchers.Main) {
                    (chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
                    chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
                }
                messageCacheManager.saveSingleMessage(roomId, message, Iso8601Converter.toUtcZeroOffsetTimestamp(dbMessage.created_at))
            }
        }
    }

    private suspend fun loadExistingMessages(roomId: String) {
        try {
            val messageIds = withContext(Dispatchers.IO) {
                SBClient.fetchMessageId(roomId)
            }

            processedMessageIds.clear()

            if (messageIds.isNotEmpty()) {
                val allMessages = withContext(Dispatchers.IO) {
                    SBClient.fetchMessages(roomId)
                }

                messageIds.forEach { messageId ->
                    processedMessageIds.add(messageId.id)
                }

                allMessages.forEach { dbMessage ->
                    val sender = getUserInf(dbMessage.user_id)
                    val message = Message(
                        dbMessage.id,
                        "${sender.userName} (${dbMessage.user_id})",
                        sender.userImage,
                        dbMessage.content,
                        dbMessage.created_at
                    )
                    if (currentRoomId == roomId) {
                        withContext(Dispatchers.Main) {
                            (chatAdapter as? ChatAdapterView.ChatAdapter)?.addMessageIfNotExists(message)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                context.ShowToast("加载消息失败: ${e.message}")
            }
        }
    }

    private fun unsubscribeFromMessages() {
        /**
         * 取消轮询, SupaBase RealTime未实现
         */
        pollingJob?.cancel()
        pollingJob = null
        processedMessageIds.clear()

        /**
         * 取消订阅
         */
        messageSubscription?.cancel()
        messageSubscription = null
        currentSubscription?.cancel()
        currentSubscription = null
    }

}