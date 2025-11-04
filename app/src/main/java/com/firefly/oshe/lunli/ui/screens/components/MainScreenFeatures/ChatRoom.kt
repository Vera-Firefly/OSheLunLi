package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.text.TextUtils
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.data.ChatRoom.Message
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.MarkdownRenderer
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.ChatRoom.cache.MessageCacheManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.RoomPrefManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.SeparateUserCacheManager
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.dialog.CropDialog
import com.firefly.oshe.lunli.utils.Ciallo
import com.firefly.oshe.lunli.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.*
import java.util.*
import kotlin.collections.mutableListOf

class ChatRoom(
    private val context: Context,
    private val userData: UserData
) {

    private lateinit var mainView: LinearLayout
    private lateinit var client: Client
    private lateinit var cropDialog: CropDialog

    private var roomAdapter: BaseRoomAdapter? = null
    private var chatAdapter: BaseChatAdapter? = null
    private var roomRecyclerView: RecyclerView? = null
    private var chatRecyclerView: RecyclerView? = null

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
    private val userMessageCacheManager by lazy {
        MessageCacheManager(context, userData.userId)
    }

    private val roomPrefManager by lazy {
        RoomPrefManager(context)
    }

    private val interaction by lazy {
        Interaction(context)
    }

    private var messageSubscription: Job? = null
    private var currentSubscription: Job? = null

    private var pollingJob: Job? = null
    private var lastPollTime: Long = 0

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
        }

    private var isLoading: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

            createRoomSelectionView()
            createChatRoomView()
        }
        return mainView
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
                    message
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
                    (chatAdapter as? ChatAdapter)?.getMessages()?.let { messages ->
                        userMessageCacheManager.saveMessagesToCache(roomInfo.id, messages)
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

    private fun showRoomStatusSet(view: View) {
        PopupMenu(context, view).apply {
            menu.add("新建Room").setOnMenuItemClickListener {
                addRoomDialog()
                true
            }
            menu.add("删除Room").setOnMenuItemClickListener {
                context.ShowToast("单击房间以删除, 注意: 只能删除自己创建的房间, 否则你将被骗(PS: 这是AS推荐我加的一句)")
                isAddNewRoom = false
                roomStatus?.removeAllViews()
                roomStatus_done?.let {
                    roomStatus?.addView(it, 36.dp, 36.dp)
                }
                true
            }
            menu.add("加入隐藏房间").setOnMenuItemClickListener {
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
            hint = "隐藏房间ID"
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
            .setTitle("输入隐藏房间ID")
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
                        roomPrefManager.saveHiddenRoom(roomInfo)
                        (roomAdapter as? RoomAdapter)?.addRoomIfNotExists(roomInfo)
                        context.ShowToast("隐藏房间加入成功")
                    } catch (e: Exception) {
                        context.ShowToast("房间信息解析失败")
                    }
                } ?: run {
                    context.ShowToast("未找到该隐藏房间")
                }
            }

            override fun onFailure(error: String?) {
                context.ShowToast("加入隐藏房间失败: $error")
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
                    id = "${userData.userId}-${System.currentTimeMillis()}",
                    title = title,
                    creator = userData.userName + " (${userData.userId})",
                    roomMessage = roomMessage,
                    roomPassword = roomPassword.takeIf { it.isNotBlank() } ?: "Null"
                )

                uploadRoomToClient(isHiddenRoom, newRoom, object : Client.ResultCallback {
                    override fun onSuccess(content: String?) {
                        addRoom(newRoom)
                        SBClient.createRoom(newRoom.id)

                        if (isHiddenRoom) {
                            roomPrefManager.saveHiddenRoom(newRoom)
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
                roomAdapter = RoomAdapter()
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
                chatAdapter = ChatAdapter()
                adapter = chatAdapter
            }
            addView(chatRecyclerView)
        }
    }

    private fun addRoom(roomInfo: RoomInfo) {
        (roomAdapter as? RoomAdapter)?.addRoom(roomInfo)
        roomRecyclerView?.scrollToPosition((roomAdapter?.itemCount ?: 1) - 1)
    }

    private abstract inner class BaseRoomAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        abstract fun addRoom(roomInfo: RoomInfo)
    }

    private inner class RoomAdapter : BaseRoomAdapter() {
        private val rooms = mutableListOf<RoomInfo>()

        init {
            loadRooms { callback ->
                if (callback) {
                    loadRoomsFromClient(false) {
                        if (it) loadRoomsFromClient(true) {}
                    }
                } else {
                    // TODO:
                }
            }
            rooms.addAll(listOf(
                RoomInfo(
                    "1",
                    "公共聊天室",
                    "系统管理员",
                    "欢迎所有用户加入讨论",
                    "Null"
                )
            ))
            loadLocalRooms()
        }

        private fun loadLocalRooms() {
            roomPrefManager.getSavedRooms().forEach { room ->
                addRoomIfNotExists(room)
            }
            roomPrefManager.getHiddenRooms().forEach { room ->
                addRoomIfNotExists(room)
            }
        }

        override fun addRoom(roomInfo: RoomInfo) {
            rooms.add(roomInfo)
            notifyItemInserted(rooms.size - 1)
        }

        fun addRoomIfNotExists(roomInfo: RoomInfo) {
            val existingRoomIndex = rooms.indexOfFirst { it.id == roomInfo.id }
            if (existingRoomIndex == -1) {
                addRoom(roomInfo)
            } else {
                val existingRoom = rooms[existingRoomIndex]
                if (existingRoom.roomPassword != roomInfo.roomPassword) {
                    rooms[existingRoomIndex] = roomInfo
                    notifyItemChanged(existingRoomIndex)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8.dp, 0, 8.dp)
            }

            // 房间标题
            val titleView = TextView(context).apply {
                id = R.id.room_title
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }
            root.addView(titleView)
            
            // 创建者
            val creatorView = TextView(context).apply {
                id = R.id.room_creator
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4.dp, 0, 0)
            }
            root.addView(creatorView)
            
            // 房间消息
            val messageView = TextView(context).apply {
                id = R.id.room_message
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4.dp, 0, 0)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            root.addView(messageView)
            
            // 分隔线
            val dividing = View(context).apply {
                setBackgroundColor(Color.LTGRAY)
                layoutParams = LayoutParams(MATCH_PARENT, 1.dp).apply {
                    topMargin = 8.dp
                }
            }
            root.addView(dividing)

            return object : RecyclerView.ViewHolder(root) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val room = rooms[position]
            val rootView = holder.itemView
        
            val RoomTitle = rootView.findViewById<TextView>(R.id.room_title)
            val RoomCreator = rootView.findViewById<TextView>(R.id.room_creator)
            val RoomMessage = rootView.findViewById<TextView>(R.id.room_message)

            RoomTitle.text = room.title
            RoomCreator.text = "创建者: ${room.creator}"
            RoomMessage.text = room.roomMessage

            val isHideRoom = roomPrefManager.getHiddenRooms().any { it.id == room.id }
            val path =  if (isHideRoom) {"HideRoomInfo"} else "RoomInfo"

            rootView.setOnClickListener {
                if (isAddNewRoom) {
                    detectRoomPassword(room) { callback ->
                        if (callback) {
                            mainView.removeAllViews()
                            mainView.addView(chatRoom)
                            addExitRoom(room)
                            roomSelectedListener?.onRoomSelected()
                        }
                    }
                } else {
                    if (room.id.startsWith("${userData.userId}-")) {
                        showRoomDeleteDialog(room) { callback ->
                            if (callback) {
                                client.deleteData(
                                    path,
                                    room.id,
                                    object : Client.ResultCallback {
                                        override fun onSuccess(content: String?) {
                                            rooms.remove(room)
                                            notifyItemRemoved(holder.bindingAdapterPosition)
                                            if (isHideRoom) roomPrefManager.removeHiddenRoom(room.id)
                                            context.ShowToast("删除成功")
                                        }

                                        override fun onFailure(error: String?) {
                                            context.ShowToast("貌似删除失败了🤔, 请尝试重新删除")
                                        }
                                    })
                            } else {
                                // TODO:
                            }
                        }
                    } else {
                        if (isHideRoom) {
                            showHiddenRoomLeaveDialog(room) { callback ->
                                if (callback) {
                                    roomPrefManager.removeHiddenRoom(room.id)
                                    rooms.remove(room)
                                    notifyItemRemoved(holder.bindingAdapterPosition)
                                    context.ShowToast("已离开隐藏房间")
                                }
                            }
                        } else {
                            context.ShowToast("别人的房间你删逆🐎呢")
                        }
                    }
                }
            }
        }

        override fun getItemCount() = rooms.size
    }

    private fun addMessage(message: Message) {
        (chatAdapter as? ChatAdapter)?.addMessage(message)
        chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
    }

    private abstract inner class BaseChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        abstract fun addMessage(message: Message)
    }

    private enum class ContentType {
        IMAGE_BASE64,
        IMAGE_URL,
        TEXT
    }

    private inner class ChatAdapter : BaseChatAdapter() {
        private val messages = mutableListOf<Message>()
        // 注意: 禁止打开这个base64!!!!!!!!!!!! 否则后果很严重!!!!!!!!!!!!!!
        private val base64 = Ciallo().ciallo
        private val systemMessages = listOf(
            Message(
                "1",
                "系统",
                "NULL",
                content = """
                    ## 欢迎使用Markdown聊天室
                    - 支持层级显示
                    - 支持**粗体**和*斜体*
                    - 支持[链接](https://example.com)
                    - 支持图片显示(需要图床)
                    ![]($base64)
                    - 支持表格
                    ### 表格示例:
                    | A | B |
                    |---------|---------|
                    | 1 | ○ |
                    | 2 | ● |
                    | 3 | ♥ |
                    - 支持区块引用与嵌套引用
                    > 区块引用
                    >> 嵌套引用
                    - 支持代码显示(注: 代码高亮显示未实现)
                    `行内代码`

                    ```python
                    # 代码块
                    def hello():
                        print("Hello World!")
                    ```
                    - 其它类型未实现, 后继会逐一实现
                    """.trimIndent()
            ))

        override fun addMessage(message: Message) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }

        fun clearMessages() {
            messages.clear()
            messages.addAll(systemMessages)
            notifyDataSetChanged()
        }

        fun addMessageIfNotExists(message: Message) {
            if (messages.none { it.id == message.id }) {
                addMessage(message)
            }
        }

        fun getMessages(): List<Message> = messages.toList()

        private fun detectContentType(content: String): ContentType {
            return when {
                isPrefixedBase64Image(content) -> ContentType.IMAGE_BASE64
                isPureBase64Image(content) -> ContentType.IMAGE_BASE64
                isImageUrl(content) -> ContentType.IMAGE_URL
                else -> ContentType.TEXT
            }
        }

        private fun isPureBase64Image(content: String): Boolean {
            val trimmedContent = content.trim()

            if (trimmedContent.length < 100) return false

            val isJpeg = trimmedContent.startsWith("/9j/")
            val isPng = trimmedContent.startsWith("iVBORw0KGgo")
            val isGif = trimmedContent.startsWith("R0lGOD")
            val isWebP = trimmedContent.startsWith("UklGR")

            return (isJpeg || isPng || isGif || isWebP)
        }

        private fun isPrefixedBase64Image(content: String): Boolean {
            return content.startsWith("data:image/") &&
                    content.contains("base64,") &&
                    content.length > 100
        }

        private fun isImageUrl(content: String): Boolean {
            val trimmedContent = content.trim()
            return (trimmedContent.startsWith("http://") || trimmedContent.startsWith("https://")) &&
                    (trimmedContent.endsWith(".jpg") ||
                            trimmedContent.endsWith(".jpeg") ||
                            trimmedContent.endsWith(".png") ||
                            trimmedContent.endsWith(".gif") ||
                            trimmedContent.endsWith(".webp") ||
                            trimmedContent.contains(".jpg?") ||
                            trimmedContent.contains(".jpeg?") ||
                            trimmedContent.contains(".png?") ||
                            trimmedContent.contains(".gif?"))
        }

        private fun renderBase64Image(container: FrameLayout, content: String) {
            try {
                val bitmap = ImageUtils.base64ToBitmap(content)

                val imageView = ShapeableImageView(container.context).apply {
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(bitmap)
                    adjustViewBounds = true

                    setOnClickListener {
                        // TODO
                    }
                }

                container.addView(imageView)
            } catch (e: Exception) {
                context.ShowToast("图片加载失败")
            }
        }

        private fun renderImageUrl(container: FrameLayout, imageUrl: String) {
            val imageView = ShapeableImageView(container.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true

                // 使用 Glide 加载网络图片
                Glide.with(context)
                    .load(imageUrl)
                    .into(this)

                setOnClickListener {
                    // TODO
                }
            }

            container.addView(imageView)
        }

        private fun renderText(container: FrameLayout, content: String) {
            val textView = TextView(container.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                movementMethod = LinkMovementMethod.getInstance()

                setTextColor(Color.BLACK)
                textSize = 14f
            }

            try {
                // 尝试渲染 Markdown
                MarkdownRenderer.render(textView, content.replace("\n", "  \n"))
            } catch (e: IllegalStateException) {
                // 如果 Markdown 渲染失败, 降级为普通文本
                textView.text = content
            } catch (e: Exception) {
                // 其他异常也降级为普通文本
                textView.text = content
            }

            container.addView(textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // 根布局 横向
            val rootLayout = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(0, 8.dp, 0, 8.dp)
                }
                orientation = HORIZONTAL
            }

            // 头像
            val avatar = ShapeableImageView(parent.context).apply {
                layoutParams = LayoutParams(36.dp, 36.dp).apply {
                    setMargins(0, 0, 4.dp, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                id = R.id.iv_avatar
            }
            rootLayout.addView(avatar) // 添加到根布局

            // 消息内容区域 纵向
            val contentArea = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }

            // 发送者名称
            val senderName = TextView(parent.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                textSize = 12f
                setTextColor(Color.GRAY)
                id = R.id.tv_sender
            }
            contentArea.addView(senderName) // 添加到内容区域

            // 消息内容容器
            val contentContainer = FrameLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 4.dp, 0, 0)
                }
                id = R.id.fl_content
            }
            contentArea.addView(contentContainer) // 添加到内容区域

            // 将内容区域添加到根布局
            rootLayout.addView(contentArea)

            // 返回ViewHolder
            return object : RecyclerView.ViewHolder(rootLayout) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            val rootView = holder.itemView

            // 获取子视图
            val avatar = rootView.findViewById<ImageView>(R.id.iv_avatar)
            val senderName = rootView.findViewById<TextView>(R.id.tv_sender)
            val contentContainer = rootView.findViewById<FrameLayout>(R.id.fl_content)

            // 设置数据
            senderName.text = message.sender
            when {
                message.sender == "系统" && message.id == "1" -> {
                    avatar.setImageResource(android.R.drawable.ic_menu_info_details)
                }
                message.senderImage != "NULL" -> {
                    val image = ImageUtils.base64ToBitmap(message.senderImage)
                    avatar.setImageBitmap(image)
                }
                else -> {
                    avatar.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            // 清空旧内容并渲染新消息
            contentContainer.removeAllViews()

            when (detectContentType(message.content)) {
                ContentType.IMAGE_BASE64 -> {
                    renderBase64Image(contentContainer, message.content)
                }
                ChatRoom.ContentType.IMAGE_URL -> {
                    renderImageUrl(contentContainer, message.content)
                }
                else -> {
                    renderText(contentContainer, message.content)
                }
            }
        }

        override fun getItemCount() = messages.size
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

    private fun processRoomMessage(content: String) {
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

                (roomAdapter as? RoomAdapter)?.addRoomIfNotExists(roomInfo)
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

    private fun parseRoomInfo(json: String): RoomInfo {
        val obj = JSONObject(json)
        return RoomInfo(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            creator = obj.optString("creator", ""),
            roomMessage = obj.optString("roomMessage", ""),
            roomPassword = obj.optString("roomPassword", "")
        )
    }

    private fun showRoomDeleteDialog(room: RoomInfo, callback: (confirmed: Boolean) -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("删除确认")
            .setMessage("确定要删除房间: ${room.title}?\n RoomId: ${room.id}")
            .setPositiveButton("删除") { _, _ ->
                callback(true)
            }
            .setNegativeButton("取消") { _, _ ->
                callback(false)
            }
            .show()
    }

    private fun showHiddenRoomLeaveDialog(room: RoomInfo, callBack: (Boolean) -> Unit = {}) {
        MaterialAlertDialogBuilder(context)
            .setTitle("离开隐藏房间?")
            .setMessage("确定要离开隐藏房间: ${room.title}?\n这将从你的房间列表中移除该房间")
            .setPositiveButton("离开") { _, _ ->
                callBack(true)
            }
            .setNegativeButton("取消") { _, _ ->
                callBack(false)
            }
            .show()
    }

    private fun detectRoomPassword(room: RoomInfo, callback: (Boolean) -> Unit) {
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
        (chatAdapter as? ChatAdapter)?.clearMessages()

        val cachedMessages = userMessageCacheManager.loadCachedMessages(roomId)
        cachedMessages.forEach { message ->
            (chatAdapter as? ChatAdapter)?.addMessageIfNotExists(message)
        }
        loadRoomMessagesll(roomId)
    }

    private fun loadRoomMessagesll(roomId: String) {
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
                                message.content
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (currentRoomId == roomId) {
                                (chatAdapter as? ChatAdapter)?.addMessageIfNotExists(
                                    processedMessage
                                )
                                chatRecyclerView?.scrollToPosition(
                                    (chatAdapter?.itemCount ?: 1) - 1
                                )
                            }
                            (chatAdapter as? ChatAdapter)?.getMessages()?.let { messages ->
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

    private fun startPollingMessages(roomId: String) {
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

    private fun parseIso8601WithTimezone(isoString: String): Long {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoString).toEpochMilli()
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

    private fun processNewMessages(messages: List<SBClient.Message>) {
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
                    (chatAdapter as? ChatAdapter)?.addMessageIfNotExists(message)
                    chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
                }

                (chatAdapter as? ChatAdapter)?.getMessages()?.let { allMessages ->
                    userMessageCacheManager.saveMessagesToCache(currentRoomId ?: return@let, allMessages)
                }
            }
        }
    }

    private suspend fun loadExistingMessages(roomId: String) {
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
                    (chatAdapter as? ChatAdapter)?.addMessageIfNotExists(message)
                }
            }
            withContext(Dispatchers.Main) {
                chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
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