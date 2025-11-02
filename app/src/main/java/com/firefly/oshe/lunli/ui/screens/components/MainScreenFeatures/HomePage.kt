package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.GlobalInterface.ImageSelectionManager
import com.firefly.oshe.lunli.GlobalInterface.SimpleImageCallback
import com.firefly.oshe.lunli.MainActivity
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.Tools
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.client.Token
import com.firefly.oshe.lunli.data.ChatRoom.cache.MessageCacheManager
import com.firefly.oshe.lunli.data.ChatRoom.cache.SeparateUserCacheManager
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.dialog.CropDialog
import com.firefly.oshe.lunli.utils.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlin.random.Random

// 用户个人主页
class HomePage(
    private val context: Context,
    private val userData: UserData,
    private val userInformation: UserInformation,
) {
    private lateinit var mainView: LinearLayout
    private lateinit var client: Client
    private lateinit var userDataPref: UserDataPref
    private lateinit var cropDialog: CropDialog
    private var pageRecyclerView: RecyclerView? = null
    private var pageAdapter: BaseUserHomeAdapter? = null
    private var homePage: LinearLayout? = null
    private var onItemClickCount = 0
    private var lastResetTime = System.currentTimeMillis()
    private val userCacheManager by lazy {
        SeparateUserCacheManager(context)
    }
    private val userMessageCacheManager by lazy {
        MessageCacheManager(context, userData.userId)
    }

    private val interaction by lazy {
        Interaction(context)
    }

    fun interface onSignOutListener {
        fun onSignOut()
    }

    private var signOutListener: onSignOutListener? = null

    fun setOnSignOutListener(listener: onSignOutListener) {
        this.signOutListener = listener
    }

    fun interface onExitToLoginListener {
        fun onExitToLogin()
    }

    private var exitToLoginListener: onExitToLoginListener? = null

    fun setOnExitToLoginListener(listener: onExitToLoginListener) {
        this.exitToLoginListener = listener
    }

    fun interface onUserImageChangeListener {
        fun onUserImageChange()
    }

    private var userImageChangeListener: onUserImageChangeListener? = null

    fun setOnUserImageChangeListener(listener: onUserImageChangeListener) {
        this.userImageChangeListener = listener
    }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

            createUserHomePageView()
        }
        return mainView
    }

    private fun LinearLayout.createUserHomePageView() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = VERTICAL

            pageRecyclerView = RecyclerView(context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                pageAdapter = UserHomeAdapter()
                adapter = pageAdapter
            }
            addView(pageRecyclerView)
        }

        addView(homePage)
    }

    private abstract inner class BaseUserHomeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // TODO:
    }

    private inner class UserHomeAdapter: BaseUserHomeAdapter() {
        private val information = mutableListOf<UserInformation>()

        init {
            client = Client(context)
            userDataPref = UserDataPref(context)
            information.addAll(listOf(
                UserInformation(
                    userData.userId,
                    userData.userName,
                    "DUMMY",
                    "DUMMY"
                )
            ))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL
                setPadding(0, 8.dp, 0, 8.dp)
            }

            val inf = UserInformationPref(context).getInformation(userData.userId)
            val image: Bitmap? = inf?.let {
                try {
                    ImageUtils.base64ToBitmap(it.userImage)
                } catch (e: Exception) {
                    null
                }
            }
            val defaultIconBase64 = context.getDrawable(R.drawable.user)?.let { drawable ->
                ImageUtils.drawableToBase64(drawable)
            }
            val defaultIcon = defaultIconBase64?.let { ImageUtils.base64ToBitmap(it) }

            createCMLView(
                image ?: defaultIcon ?: createDefaultBitmap(),
                userData.userName,
                userData.userId,
                "NONE"
            ).also {
                root.addView(it)
            }

            val buttonLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(8.dp, 0, 8.dp, 4.dp)
                }
            }

            interaction.createButton("修改密码", R.color.gray) {
                onEditPasswordDialog()
            }.apply {
                layoutParams = LayoutParams(MATCH_PARENT, 48.dp, 1f)
            }.also { buttonLayout.addView(it) }

            interaction.createButton("清除缓存(当前账户)", R.color.gray) {
                userCacheManager.clearUserCache(userData.userId)
                userMessageCacheManager.clearAllMessagesCache()
            }.apply {
                layoutParams = LayoutParams(MATCH_PARENT, 48.dp, 1f)
            }.also { buttonLayout.addView(it) }

            interaction.createButton("清除缓存", R.color.light_blue) {
                userCacheManager.clearAllCache()
                userMessageCacheManager.clearAllMessagesCache()
            }.apply {
                layoutParams = LayoutParams(MATCH_PARENT, 48.dp, 1f)
            }.also { buttonLayout.addView(it) }

            interaction.createButton("注销账户", R.color.red) {
                onLogOutUserDialog()
            }.apply {
                layoutParams = LayoutParams(MATCH_PARENT, 48.dp, 1f)
            }.also { buttonLayout.addView(it) }

            interaction.createButton("退出登录", R.color.red) {
                 signOutListener?.onSignOut()
             }.apply {
                layoutParams = LayoutParams(MATCH_PARENT, 48.dp, 1f)
            }.also { buttonLayout.addView(it) }

            root.addView(buttonLayout)

            return object : RecyclerView.ViewHolder(root) {}
        }

        private fun createDefaultBitmap(): Bitmap {
            val size = 64.dp
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val paint = android.graphics.Paint().apply {
                color = context.getColor(R.color.gray)
                isAntiAlias = true
            }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            val userDrawable = context.getDrawable(R.drawable.user)
            userDrawable?.setBounds(0, 0, size, size)
            userDrawable?.draw(canvas)

            return bitmap
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // TODO:
        }

        override fun getItemCount() = information.size
    }

    private fun createCMLView(
        icon: Bitmap,
        name: String,
        uid: String,
        userMessage: String
    ): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(8.dp, 8.dp, 8.dp, 8.dp)
            }
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)

            val rippleColor = ColorStateList.valueOf("#20000000".toColorInt())
            background = RippleDrawable(
                rippleColor,
                createRoundedBackground().apply {
                    setColor(ContextCompat.getColor(context, R.color.tan))
                    cornerRadius = 16f
                },
                null
            )
            isClickable = true
            isFocusable = true

            ShapeableImageView(context).apply {
                layoutParams = LayoutParams(64.dp, 64.dp).apply {
                    marginEnd = 18.dp
                }
                setImageBitmap(icon)
                setOnClickListener {
                    ImageRequestCallBack { sampleBitmap ->
                        cropDialog = CropDialog(context)
                        sampleBitmap.let {
                            showCropDialog(it) { image ->
                                val currentBase64 = ImageUtils.bitmapToBase64(image)
                                val inf = UserInformation(
                                    userData.userId,
                                    userData.userName,
                                    currentBase64,
                                    ""
                                )
                                updateSBClientUserMessage(inf) {
                                    if (!it) {
                                        context.ShowToast("头像暂时无法更新")
                                    } else {
                                        setImageBitmap(image)
                                        UserInformationPref(context).deleteInformation(userData.userId)
                                        UserInformationPref(context).saveInformation(inf)
                                        userImageChangeListener?.onUserImageChange()
                                    }
                                }
                            }
                        }
                    }
                }
                addView(this)
            }

            LinearLayout(context).apply {
                orientation = VERTICAL
                TextView(context).apply {
                    text = name
                    textSize = 24f
                    setTextColor(ContextCompat .getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
                    }
                    setOnClickListener {
                        editMessageDialog(
                            "编辑用户名",
                            "用户名",
                            userData.userName
                        ) { value ->
                            val data = UserData(
                                userData.userId,
                                value,
                                userData.password,
                                false
                            )
                            val data_1 = UserInformation(
                                data.userId,
                                data.userName,
                                userInformation.userImage,
                                userInformation.userMessage
                            )
                            updateSBClientUserMessage(data_1) {
                                if (!it) {
                                    context.ShowToast("无法连接SBClient, 请稍后尝试更改用户名")
                                } else {
                                    updateClientMessage(data) {
                                        if (!it) {
                                            updateSBClientUserMessage(
                                                UserInformation(
                                                    userData.userId,
                                                    userData.userName,
                                                    userInformation.userImage,
                                                    userInformation.userMessage
                                                )
                                            )
                                            context.ShowToast("无法连接Client, 请稍后尝试更改用户名")
                                        } else {
                                            userDataPref.deleteUser(userData.userId)
                                            userDataPref.saveUser(data)
                                            this.setText(value)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    addView(this)
                }
                TextView(context).apply {
                    text = "UID：$uid"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
                    }
                    setOnClickListener {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("UID", uid)
                        clipboard.setPrimaryClip(clip)
                        context.ShowToast("UID已经复制到剪切板")
                    }
                    addView(this)
                }
                TextView(context).apply {
                    text = userMessage
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
                    }
                    setOnClickListener {
                        editMessageDialog("编辑简介", "简介", userInformation.userMessage)
                    }
                    addView(this)
                }
            }.also { addView(it) }

            setOnClickListener {
                val currentTime = System.currentTimeMillis()
                val timeZZ = currentTime - lastResetTime
                if (timeZZ > 5000) {
                    onItemClickCount = 0
                    lastResetTime = currentTime
                }

                onItemClickCount++

                val displayText = if (onItemClickCount > 6) {
                    lastResetTime = currentTime
                    "Ciallo～(∠・ω< )⌒☆ x ${onItemClickCount}"
                } else if (onItemClickCount > 5) {
                    lastResetTime = currentTime
                    "那你点吧"
                } else if (onItemClickCount > 4) {
                    "您的点击太频繁了（=´口｀=）"
                } else {
                    val emojiArray = arrayOf(
                        "(￣▽￣)", "(～￣▽￣)～", "(´∀｀)", "(°∀°)ﾉ",
                        "（￣︶￣）", "(❤ω❤)", "(≧∇≦)ﾉ", "(´▽｀)",
                        "(づ￣ 3￣)づ", "(*/ω＼*)", "～(∠・ω< )⌒☆"
                    )
                    emojiArray.random()
                }

                context.ShowToast(displayText)
            }

        }
    }

    private fun showCropDialog(bitmap: Bitmap, callBack: (Bitmap) -> Unit = {}) {
        cropDialog.showCropDialog(bitmap, 0) { it ->
            it?.let {
                callBack(it)
                context.ShowToast("DONE")
            }
        }
        cropDialog.showAtLocation(mainView)
    }

    private fun ImageRequestCallBack(callBack: (Bitmap) -> Unit = {}) {
        ImageSelectionManager.setCallback("HomePage", object : SimpleImageCallback {
            override fun onImageSelected(uri: Uri) {
                val bitmap = ImageUtils.bitmapFromUri(context, uri)
                bitmap?.let { callBack(it) }
            }

            override fun onSelectionCancelled() {
                onDestroy()
            }

            override fun onSelectionFailed(error: String) {
                TODO("Not yet implemented")
            }
        })
        if (context is MainActivity) {
            context.startImageSelection()
        }
    }

    private fun editMessageDialog(
        title: String,
        dialogHint: String,
        message: String,
        callBack: (String) -> Unit = {}
    ) {
        val view: LinearLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        val input = TextInputEditText(context).apply {
            hint = dialogHint
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }.also {
            it.setText(message)
            view.addView(
                it,
                LayoutParams(
                    MATCH_PARENT,
                    WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dp
                }
            )
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("确定") { _, _ ->
                callBack(input.text.toString())
            }
            .setNegativeButton("取消") { _, _ ->
                callBack(message)
            }
            .show()
    }

    private fun onEditPasswordDialog() {
        val dialogView = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        val oldPasswordInput =  TextInputEditText(context).apply {
            hint = "旧密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }.also {
            dialogView.addView(
                it,
                LayoutParams(
                    MATCH_PARENT,
                    WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dp
                }
            )
        }

        val newPasswordInput = TextInputEditText(context).apply {
            hint = "新密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 8.dp.toFloat()
                setStroke(1, Color.argb(50, 0, 0, 0))
            }
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        }.also { dialogView.addView(it, LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }

        MaterialAlertDialogBuilder(context)
            .setTitle("修改密码")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认") { _, _ ->
                val oldPassword = oldPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                if (oldPassword == userData.password && oldPassword != newPassword) {
                    val data = UserData(
                        userData.userId,
                        userData.userName,
                        newPassword,
                        false
                    )
                    updateClientMessage(data) {
                        if (!it) {
                            context.ShowToast("无法连接Client, 请稍后再试")
                        } else {
                            userDataPref.deleteUser(userData.userId)
                            userDataPref.saveUser(UserData(
                                userData.userId,
                                userData.userName,
                                "",
                                false
                            ))
                            MaterialAlertDialogBuilder(context)
                                .setTitle("提示:")
                                .setMessage("请退出重新登录, 如遇到问题请联系管理人员")
                                .setPositiveButton("确认") { _, _ ->
                                    exitToLoginListener?.onExitToLogin()
                                }
                                .show()
                        }
                    }
                } else if (oldPassword == userData.password) {
                    context.ShowToast("新密码与旧密码一致")
                } else {
                    context.ShowToast("旧密码输入错误, 请重新输入",)
                }
            }
            .show()
    }

    private fun onLogOutUserDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("注销账户?")
            .setMessage("警告: 此操作将删除你的账户, 且不可逆!!!")
            .setCancelable(false)
            .setNegativeButton("确认") { _, _ ->
                val seed1 = (Random.nextInt(33, 127)).toChar()
                val seed2 = (Random.nextInt(33, 127)).toChar()
                val tokens = Token.getToken("$seed1$seed2")
                val token = if (Random.nextBoolean()) {
                    tokens[0]
                } else {
                    tokens[1]
                }
                val data = UserData(
                    userData.userId,
                    userData.userName,
                    token,
                    false
                )
                updateClientMessage(data) {
                    if (!it) {
                        context.ShowToast("无法连接Client, 请稍后再试")
                    } else {
                        userDataPref.deleteUser(userData.userId)
                        MaterialAlertDialogBuilder(context)
                            .setTitle("提示: 按下确认退出至登录页")
                            .setMessage("您的账户UID已经永久保留且无法被其他人使用\n申请恢复请联系管理人员\n")
                            .setCancelable(false)
                            .setPositiveButton("确认") { _, _ ->
                                exitToLoginListener?.onExitToLogin()
                            }
                            .show()
                    }
                }
            }
            .setPositiveButton("取消", null)
            .show()
    }

    private fun updateClientMessage(data: UserData, callBack: (Boolean) -> Unit = {}) {
        val userMap = mapOf(userData.userId to data)
        val content = try {
            Gson().toJson(userMap)
        } catch (e: Exception) {
            Log.e("RegistScreen", "JSON 转换失败", e)
            null
        }
        client.updateData("UserData", userData.userId, content,
            object : Client.ResultCallback {
                override fun onSuccess(content: String?) {
                    (context as? Activity)?.runOnUiThread {
                        callBack(true)
                    }
                }

                override fun onFailure(error: String?) {
                    (context as? Activity)?.runOnUiThread {
                        callBack(false)
                    }
                }
            }
        )
    }

    private fun updateSBClientUserMessage(data: UserInformation, callBack: (Boolean) -> Unit = {}) {
        SBClient.updateUser(data.userId, data.userName, data.userImage) {
            if (!it) {
                callBack(false)
            } else {
                callBack(true)
            }
        }
    }

    private fun createRoundedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setStroke(2.dp, "#30000000".toColorInt())
        }
    }

    fun onDestroy() {
        ImageSelectionManager.clearCallback()
    }

}
