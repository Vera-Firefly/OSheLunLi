package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.RECTANGLE
import android.text.TextUtils
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_CLASS_TEXT
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.data.ChatRoom.RoomInfo
import com.firefly.oshe.lunli.data.ChatRoom.cache.RoomPrefManager
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlin.collections.mutableListOf

class RoomAdapterView(
    private val context: Context,
    private val userData: UserData,
    private val roomPrefManager: RoomPrefManager,
    private val client: Client,
    private val onRoomSelected: (RoomInfo) -> Unit,
    private val onRoomDeleted: (RoomInfo) -> Unit,
    private val onHiddenRoomLeft: (RoomInfo) -> Unit
) {

    private val rooms = mutableListOf<RoomInfo>()
    private var adapter: RoomAdapter? = null
    private var isAddNewRoom: Boolean = true

    fun createAdapter(): RoomAdapter {
        adapter = RoomAdapter()
        return adapter!!
    }

    fun addRoom(roomInfo: RoomInfo) {
        rooms.add(roomInfo)
        adapter?.notifyItemInserted(rooms.size - 1)
    }

    fun addRoomIfNotExists(roomInfo: RoomInfo) {
        val existingRoomIndex = rooms.indexOfFirst { it.id == roomInfo.id }
        if (existingRoomIndex == -1) {
            addRoom(roomInfo)
        } else {
            val existingRoom = rooms[existingRoomIndex]
            if (existingRoom.roomPassword != roomInfo.roomPassword) {
                rooms[existingRoomIndex] = roomInfo
                adapter?.notifyItemChanged(existingRoomIndex)
            }
        }
    }

    fun setAddNewRoomMode(enabled: Boolean) {
        isAddNewRoom = enabled
        adapter?.notifyDataSetChanged()
    }

    fun getRooms(): List<RoomInfo> = rooms.toList()

    fun clearRooms() {
        rooms.clear()
        adapter?.notifyDataSetChanged()
    }

    inner class RoomAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        init {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8.dp, 0, 8.dp)
            }

            val titleView = TextView(context).apply {
                id = R.id.room_title
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }
            root.addView(titleView)

            val creatorView = TextView(context).apply {
                id = R.id.room_creator
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4.dp, 0, 0)
            }
            root.addView(creatorView)

            val messageView = TextView(context).apply {
                id = R.id.room_message
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4.dp, 0, 0)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            root.addView(messageView)

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

            rootView.setOnClickListener {
                if (isAddNewRoom) {
                    detectRoomPassword(room) { callback ->
                        if (callback) {
                            onRoomSelected(room)
                        }
                    }
                } else {
                    if (room.id.startsWith("${userData.userId}-")) {
                        showRoomDeleteDialog(room) { callback ->
                            if (callback) {
                                val path = if (isHideRoom) "HideRoomInfo" else "RoomInfo"
                                client.deleteData(
                                    path,
                                    room.id,
                                    object : Client.ResultCallback {
                                        override fun onSuccess(content: String?) {
                                            rooms.remove(room)
                                            notifyItemRemoved(holder.bindingAdapterPosition)
                                            if (isHideRoom) roomPrefManager.removeHiddenRoom(room.id)
                                            context.ShowToast("已删除房间: ${room.id}")
                                            onRoomDeleted(room)
                                        }

                                        override fun onFailure(error: String?) {
                                            context.ShowToast("貌似删除失败了🤔, 请尝试重新删除")
                                        }
                                    })
                            }
                        }
                    } else {
                        if (isHideRoom) {
                            showHiddenRoomLeaveDialog(room) { callback ->
                                if (callback) {
                                    roomPrefManager.removeHiddenRoom(room.id)
                                    rooms.remove(room)
                                    notifyItemRemoved(holder.bindingAdapterPosition)
                                    context.ShowToast("已离开房间: ${room.id}")
                                    onHiddenRoomLeft(room)
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

    private fun detectRoomPassword(room: RoomInfo, callback: (Boolean) -> Unit) {
        if (room.roomPassword.equals("Null")) {
            callback(true)
        } else {
            val savedRoom = roomPrefManager.getRoomById(room.id)
            if (savedRoom != null && savedRoom.roomPassword == room.roomPassword) {
                context.ShowToast("Hello!\n${userData.userName} (${userData.userId})")
                callback(true)
            } else {
                val view = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16.dp, 16.dp, 16.dp, 16.dp)
                }
                val input = TextInputEditText(context).apply {
                    hint = "房间密码"
                    inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                    setTextColor(Color.BLACK)
                    background = GradientDrawable().apply {
                        shape = RECTANGLE
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
            .setTitle("离开房间?")
            .setMessage("确定要离开房间: ${room.title} (${room.id})?\n这将从你的房间列表中移除该房间")
            .setPositiveButton("离开") { _, _ ->
                callBack(true)
            }
            .setNegativeButton("取消") { _, _ ->
                callBack(false)
            }
            .show()
    }
}