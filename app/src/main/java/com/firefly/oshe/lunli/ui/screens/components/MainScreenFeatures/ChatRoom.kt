package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

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
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
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
    private val sharedPref by lazy {
        context.getSharedPreferences("ChatRoomPrefs_${userData.userId}", Context.MODE_PRIVATE)
    }
    private val userCache = mutableMapOf<String, String>()
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
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
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
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
                layoutParams = LinearLayout.LayoutParams(
                    30.dp,
                    30.dp
                ).apply {
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
                    val markdownText = inputEditText.text.toString()
                    val currentId = UUID.randomUUID().toString()
                    if (markdownText.isNotEmpty()) {
                        addMessage(
                            Message(
                                id = currentId,
                                sender = userData.userName + " (" + userData.userId + ")",
                                content = markdownText
                            )
                        )
                        currentRoomId?.let { roomId ->
                            SBClient.sendMessage(currentId, roomId, userData.userId, markdownText) {
                                if (!it) {
                                    Toast.makeText(context, "$currentId: 发送失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        inputEditText.text?.clear()
                    }
                }
            }

            addView(inputEditText)
            addView(sendButton)
        }
    }

    fun setRoomStatus(): LinearLayout? {
        onRoomStatus()
        return roomStatus
    }

    private fun onRoomStatus() {
        roomStatus = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
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
                        loadRoomsFromClient()
                    } else {
                        // TODO:
                    }
                }
            }
        }

        roomStatus_add = ShapeableImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
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
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
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
            menu.add("新建Room").setOnMenuItemClickListener {
                addRoomDialog()
                true
            }
            menu.add("删除Room").setOnMenuItemClickListener {
                Toast.makeText(context, "单击房间以删除, 注意: 只能删除自己创建的房间, 否则你将被骗(PS: 这是AS推荐我加的一句)", Toast.LENGTH_SHORT).show()
                isAddNewRoom = false
                roomStatus?.removeAllViews()
                roomStatus_done?.let {
                    roomStatus?.addView(it, 36.dp, 36.dp)
                }
                true
            }
            show()
        }
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
        dialogView.addView(titleInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = 12.dp
        })
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
        dialogView.addView(messageInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = 12.dp
        })

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
        dialogView.addView(passwordInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setTitle("创建新房间")
            .setPositiveButton("确定") { _, _ ->
                val title = titleInput.text.toString()
                val roomMessage = messageInput.text.toString()
                val roomPassword = passwordInput.text.toString()

                if (title.isEmpty()) {
                    Toast.makeText(context, "请输入房间标题", Toast.LENGTH_SHORT).show()
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

                uploadRoomToClient(newRoom, object : Client.ResultCallback {
                    override fun onSuccess(content: String?) {
                        addRoom(newRoom)
                        SBClient.createRoom(newRoom.id)
                        Toast.makeText(context, "房间创建成功", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(error: String?) {
                        Toast.makeText(context, "创建失败, 请重新创建", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addExitRoom(roomInfo: RoomInfo) {
        exitRoom = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
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
                        saveMessagesToCache(roomInfo.id, messages)
                    }
                    backClickListener?.onBackClicked()
                    mainView.removeAllViews()
                    roomSelection?.let { mainView.addView(it) }
                }
                setPadding(6.dp, 6.dp, 6.dp, 6.dp)
                layoutParams = LinearLayout.LayoutParams(
                    WRAP_CONTENT, 
                    WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            val title = MaterialTextView(context).apply {
                text = roomInfo.title
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            addView(exitButton, 36.dp, 36.dp)
            addView(title)
        }
    }

    private fun LinearLayout.createRoomSelectionView() {
        roomSelection = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL

            roomRecyclerView = RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL

            chatRecyclerView = RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
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
                    loadRoomsFromClient()
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
        }

        override fun addRoom(roomInfo: RoomInfo) {
            rooms.add(roomInfo)
            notifyItemInserted(rooms.size - 1)
        }

        fun addRoomIfNotExists(roomInfo: RoomInfo) {
            if (rooms.none { it.id == roomInfo.id }) {
                addRoom(roomInfo)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
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
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dp
                ).apply {
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
                    if (room.id.startsWith(userData.userId)) {
                        showRoomDeleteDialog(room) { callback ->
                            if (callback) {
                                client.deleteData(
                                    "RoomInfo",
                                    room.id,
                                    object : Client.ResultCallback {
                                        override fun onSuccess(content: String?) {
                                            rooms.remove(room)
                                            notifyItemRemoved(holder.bindingAdapterPosition)
                                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT)
                                                .show()
                                        }

                                        override fun onFailure(error: String?) {
                                            Toast.makeText(
                                                context,
                                                "貌似删除失败了🤔, 请尝试重新删除",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            } else {
                                // TODO:
                            }
                        }
                    } else {
                        Toast.makeText(context, "别人的房间你删逆🐎呢", Toast.LENGTH_SHORT).show()
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

    private inner class ChatAdapter : BaseChatAdapter() {
        private val messages = mutableListOf<Message>()
        private val systemMessages = listOf(
            Message(
                id = "1",
                sender = "系统",
                content = """
                    ## 欢迎使用Markdown聊天室
                    - 支持层级显示
                    - 支持**粗体**和*斜体*
                    - 支持[链接](https://example.com)
                    - 支持图片显示(需要图床)
                    ![](https://youke1.picui.cn/s1/2025/08/06/6893621c91508.png)
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

        init {
            // TODO:
        }

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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // 根布局 横向
            val rootLayout = LinearLayout(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(0, 8.dp, 0, 8.dp)
                }
                orientation = LinearLayout.HORIZONTAL
            }

            // 头像
            val avatar = ImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(36.dp, 36.dp).apply {
                    setMargins(0, 0, 4.dp, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(android.R.drawable.sym_def_app_icon)
                id = R.id.iv_avatar
            }
            rootLayout.addView(avatar) // 添加到根布局

            // 消息内容区域 纵向
            val contentArea = LinearLayout(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                orientation = LinearLayout.VERTICAL
            }

            // 发送者名称
            val senderName = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 12f
                setTextColor(Color.GRAY)
                id = R.id.tv_sender
            }
            contentArea.addView(senderName) // 添加到内容区域

            // 消息内容容器
            val contentContainer = FrameLayout(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
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
            avatar.setImageResource(
                if (message.sender == "系统") android.R.drawable.ic_menu_info_details
                else android.R.drawable.sym_def_app_icon
            )

            // 清空旧内容并渲染新消息
            contentContainer.removeAllViews()
            val textView = TextView(rootView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                movementMethod = LinkMovementMethod.getInstance() // 支持链接点击
            }

            try {
                MarkdownRenderer.render(textView, (message.content).replace("\n", "  \n"))
            } catch (e: IllegalStateException) {
                textView.text = message.content // 降级为普通文本
            }

            contentContainer.addView(textView)
        }

        override fun getItemCount() = messages.size
    }

    private fun loadRooms(callback: (Boolean) -> Unit = {}) {
        client = Client(context)
        if (isLoading) {
            Toast.makeText(context, "正在加载房间列表, 请稍后...", Toast.LENGTH_SHORT).show()
            callback(false)
        } else {
            isLoading = true
            callback(true)
        }
    }

    private fun loadRoomsFromClient() {
        client.getDir("RoomInfo", object : Client.ResultCallback {
            override fun onSuccess(content: String) {
                processRoomDirectory(content)
            }

            override fun onFailure(error: String) {
                isLoading = false
            }
        })
    }

    private fun uploadRoomToClient(roomInfo: RoomInfo, callback: Client.ResultCallback) {
        try {
            val room = JSONObject().apply {
                put("id", roomInfo.id)
                put("title", roomInfo.title)
                put("creator", roomInfo.creator)
                put("roomMessage", roomInfo.roomMessage)
                put("roomPassword", roomInfo.roomPassword)
            }.toString()

            client.uploadData("RoomInfo", roomInfo.id, room, callback)
        } catch (e: Exception) {
            callback.onFailure("Failed To Create Room: ${e.message}")
        }
    }

    private fun processRoomDirectory(content: String) {
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
                client.getMultipleFiles("RoomInfo", rooms, object : Client.ResultCallback {
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

            while (keys.hasNext()) {
                val key = keys.next()
                val roomJson = result.getString(key)
                val roomInfo = parseRoomInfo(roomJson)

                (roomAdapter as? RoomAdapter)?.addRoomIfNotExists(roomInfo)
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

    private fun detectRoomPassword(room: RoomInfo, callback: (Boolean) -> Unit) {
        if (room.roomPassword.equals("Null")) {
            currentRoomId = room.id
            loadRoomMessages(room.id)
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
            view.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            MaterialAlertDialogBuilder(context)
                .setTitle("请输入房间密码")
                .setView(view)
                .setPositiveButton("确认") { _,_ ->
                    val password = input.text.toString()
                    if (password.equals(room.roomPassword)) {
                        currentRoomId = room.id
                        loadRoomMessages(room.id)
                        Toast.makeText(context, "Hello!\n${userData.userName} (${userData.userId})",
                            Toast.LENGTH_SHORT).show()
                        callback(true)
                    } else {
                        Toast.makeText(context, "错误的密码", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }
                .setNegativeButton("取消") { _,_ ->
                    callback(false)
                }
                .show()
        }
    }

    private suspend fun getUserName(userId: String): String {
        return userCache[userId] ?: run {
            val user = SBClient.fetchUser(userId)
            user?.name?.also { name ->
                userCache[userId] = name
            } ?: "未知用户"
        }
    }

    private fun loadRoomMessages(roomId: String) {
        unsubscribeFromMessages()
        currentRoomId = roomId
        (chatAdapter as? ChatAdapter)?.clearMessages()

        val cachedMessages = loadCachedMessages(roomId)
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
                            val senderName = getUserName(message.user_id)
                            Message(
                                id = message.id,
                                sender = "$senderName (${message.user_id})",
                                content = message.content
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
                                saveMessagesToCache(roomId, messages)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (currentRoomId == roomId) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "订阅消息失败: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } finally {
                    Toast.makeText(context, "订阅消息结束", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startPollingMessages(roomId: String) {
        unsubscribeFromMessages()
        // Toast.makeText(context, "开始轮询消息", Toast.LENGTH_SHORT).show()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentRoomId == roomId) {
                try {
                    val messages = SBClient.fetchMessages(roomId)

                    val newMessages = if (lastPollTime > 0) {
                        // Toast.makeText(context, "轮询到${messages.size}条新消息", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "轮询消息失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    delay(5000L)
                }/* finally {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "轮询结束", Toast.LENGTH_SHORT).show()
                    }
                }*/
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
        // Toast.makeText(context, "轮询到${messages.size}条新消息", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            messages.forEach { dbMessage ->
                val senderName = getUserName(dbMessage.user_id)
                val message = Message(
                    id = dbMessage.id,
                    sender = "$senderName (${dbMessage.user_id})",
                    content = dbMessage.content
                )
                withContext(Dispatchers.Main) {
                    (chatAdapter as? ChatAdapter)?.addMessageIfNotExists(message)
                    chatRecyclerView?.scrollToPosition((chatAdapter?.itemCount ?: 1) - 1)
                }

                (chatAdapter as? ChatAdapter)?.getMessages()?.let { allMessages ->
                    saveMessagesToCache(currentRoomId ?: return@let, allMessages)
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
                val senderName = getUserName(dbMessage.user_id)
                val message = Message(
                    id = dbMessage.id,
                    sender = "$senderName (${dbMessage.user_id})",
                    content = dbMessage.content
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
                Toast.makeText(context, "加载消息失败: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun loadCachedMessages(roomId: String): List<Message> {
        val json = sharedPref.getString("room_$roomId", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Message(
                    id = obj.getString("id"),
                    sender = obj.getString("sender"),
                    content = obj.getString("content")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveMessagesToCache(roomId: String, messages: List<Message>) {
        val jsonArray = JSONArray()
        messages.forEach { message ->
            val obj = JSONObject().apply {
                put("id", message.id)
                put("sender", message.sender)
                put("content", message.content)
            }
            jsonArray.put(obj)
        }
        sharedPref.edit().putString("room_$roomId", jsonArray.toString()).apply()
    }
}