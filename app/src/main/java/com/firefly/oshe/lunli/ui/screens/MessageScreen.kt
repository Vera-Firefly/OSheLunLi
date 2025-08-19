package com.firefly.oshe.lunli.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.text.InputType
import android.text.InputFilter
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserMessage
import com.firefly.oshe.lunli.data.UserMessageMData
import com.firefly.oshe.lunli.client.Client

// 用户信息界面
// 该类已经弃用
class MessageScreen(
    context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val returnMainScreen: () -> Unit
): LinearLayout(context) {

    private var currentUserMessage = UserMessage()

    private lateinit var etBliName: TextInputLayout
    private lateinit var etBliHP: TextInputLayout
    private lateinit var etBliUID: TextInputLayout
    private lateinit var etKsName: TextInputLayout
    private lateinit var etKsHP: TextInputLayout
    private lateinit var etKsUID: TextInputLayout
    private lateinit var etTTName: TextInputLayout
    private lateinit var etTTHP: TextInputLayout
    private lateinit var etTTUID: TextInputLayout

    private lateinit var biliView: LinearLayout
    private lateinit var ksView: LinearLayout
    private lateinit var tiktokView: LinearLayout

    private lateinit var client: Client
    private lateinit var userMessageMData: UserMessageMData

    private var onCreateMessage: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    private var isMessageFill: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    private var isEditMessage: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    private lateinit var topBar: LinearLayout
    private lateinit var messageViewCL: LinearLayout
    private var cancelCreateMS: LinearLayout? = null
    private var editMessageL: LinearLayout? = null
    private var isMessageNF: MaterialTextView? = null
    private var onWriteMSLayout: LinearLayout? = null
    private var currentMessage: LinearLayout? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setupViews()
    }

    private fun setupViews() {
        getMessageFromData()
        removeAllViews()
        addView(createTopBar())
        addView(createMessageView())
        if (isMessageFill) {
            showCurrentMessage()
            updateMsState()
        }
    }

    private fun getMessageFromData() {
        userMessageMData = UserMessageMData(context)
        userMessageMData.getMessage(currentId)?.let { UM ->
            currentUserMessage.apply {
                userId = UM.userId
                userName = UM.userName
                biliName = UM.biliName.takeIf { !it.isNullOrBlank() } ?: ""
                biliHomePage = UM.biliHomePage.takeIf { !it.isNullOrBlank() } ?: ""
                biliUID = UM.biliUID.takeIf { !it.isNullOrBlank() } ?: ""
                ksName = UM.ksName.takeIf { !it.isNullOrBlank() } ?: ""
                ksHomePage = UM.ksHomePage.takeIf { !it.isNullOrBlank() } ?: ""
                ksUID = UM.ksUID.takeIf { !it.isNullOrBlank() } ?: ""
                tiktokName = UM.tiktokName.takeIf { !it.isNullOrBlank() } ?: ""
                tiktokHomePage = UM.tiktokHomePage.takeIf { !it.isNullOrBlank() } ?: ""
                tiktokUID = UM.tiktokUID.takeIf { !it.isNullOrBlank() } ?: ""
            }
            if (!UM.isExpiration) {
                isMessageFill = true
                CreateCurrentMView()
            }
        }
    }

    private fun createTopBar(): LinearLayout {
        topBar = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 40.dp)
            setBackgroundColor(ContextCompat.getColor(context, R.color.tan))
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 0, 8.dp, 0)
            addView(createBackView())
            onCancelCreateMS()
            onEditMessageL()
        }
        return topBar
    }

    private fun createBackView(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            orientation = HORIZONTAL
            gravity = Gravity.START
            addBackButton()
            addBackMessage()
        }
    }

    private fun LinearLayout.onCancelCreateMS() {
        cancelCreateMS = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            orientation = HORIZONTAL
            gravity = Gravity.END
            addCancelButton()
            addConfirmButton()
        }
        if (onCreateMessage) addView(cancelCreateMS)
    }

    private fun LinearLayout.onEditMessageL() {
        editMessageL = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            orientation = HORIZONTAL
            gravity = Gravity.END
            addEditButton()
        }
        if (isMessageFill) addView(editMessageL)
    }

    private fun LinearLayout.addBackButton() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.arrow_left_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                returnMainScreen()
            }
            setPadding(6.dp, 6.dp, 6.dp, 6.dp)
            layoutParams = LayoutParams(
                WRAP_CONTENT, 
                WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(this, 36.dp, 36.dp)
        }
    }

    private fun LinearLayout.addBackMessage() {
        MaterialTextView(context).apply {
            text = "返回到主屏幕"
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(this)
        }
    }

    private fun LinearLayout.addCancelButton() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.close_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                if (!isEditMessage) {
                    onCreateMessage = false
                } else {
                    isMessageFill = true
                    isEditMessage = false
                    CreateCurrentMView()
                    showCurrentMessage()
                }
                updateMsState()
            }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)
            layoutParams = LayoutParams(
                WRAP_CONTENT, 
                WRAP_CONTENT
            ).apply {
                marginEnd = 4.dp
            }
            addView(this, 36.dp, 36.dp)
        }
    }

    private fun LinearLayout.addConfirmButton() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.select_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener { checkWriteMsaage() }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)
            layoutParams = LayoutParams(
                WRAP_CONTENT, 
                WRAP_CONTENT
            )
            addView(this, 36.dp, 36.dp)
        }
    }

    private fun LinearLayout.addEditButton() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.edit)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                onCreateMessage = true
                isEditMessage = true
                isMessageFill = false
                updateMsState()
                etBliName.editText!!.setText(currentUserMessage.biliName)
                etBliHP.editText!!.setText(currentUserMessage.biliHomePage)
                etBliUID.editText!!.setText(currentUserMessage.biliUID)
                etKsName.editText!!.setText(currentUserMessage.ksName)
                etKsHP.editText!!.setText(currentUserMessage.ksHomePage)
                etKsUID.editText!!.setText(currentUserMessage.ksUID)
                etTTName.editText!!.setText(currentUserMessage.tiktokName)
                etTTHP.editText!!.setText(currentUserMessage.tiktokHomePage)
                etTTUID.editText!!.setText(currentUserMessage.tiktokUID)
            }
            setPadding(8.dp, 2.dp, 8.dp, 2.dp)
            layoutParams = LayoutParams(
                WRAP_CONTENT, 
                WRAP_CONTENT
            )
            addView(this, 36.dp, 36.dp)
        }
    }

    private fun createMessageView(): LinearLayout {
        messageViewCL = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 0, 1f)
            orientation = VERTICAL
            gravity = Gravity.TOP
            setPadding(8.dp, 0, 8.dp, 0)
            isMessageNotFilled()
            onWriteMessage()
        }
        return messageViewCL
    }

    private fun LinearLayout.isMessageNotFilled() {
        isMessageNF = MaterialTextView(context).apply {
            text = "当前账户暂未填写信息\n点击屏幕任意位置创建"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        isMessageNF?.setOnClickListener {
            onCreateMessage = true
            updateMsState()
        }
        if (!onCreateMessage && !isMessageFill) {
            addView(isMessageNF, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    private fun LinearLayout.onWriteMessage() {
        onWriteMSLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.TOP
            visibility = View.GONE
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8.dp
            }

            etBliName = createEditText("BiliBili 用户名").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etBliHP = createEditText("BiliBili 主页链接").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etBliUID = createEditText("BiliBili UID") {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
            }.apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etKsName = createEditText("快手 用户名").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etKsHP = createEditText("快手 主页链接").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etKsUID = createEditText("快手 UID") {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                val allowedCharacters = Regex("[a-zA-Z0-9_-]*")
                filters = arrayOf<InputFilter>(InputFilter { source, _, _, _, _, _ ->
                    if (source.matches(allowedCharacters)) null else ""
                })
            }.apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etTTName = createEditText("抖音 用户名").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etTTHP = createEditText("抖音 主页链接").apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }

            etTTUID = createEditText("抖音 UID") {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
            }.apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }.also { addView(it) }
        }
        if (onCreateMessage && !isMessageFill) {
            addView(onWriteMSLayout, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    private fun showCurrentMessage() {
        currentMessage = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.TOP
            visibility = View.GONE
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8.dp
            }
            addView(biliView)
            addView(ksView)
            addView(tiktokView)
        }
    }

    private fun createEditText(
        hint: String,
        editTextConfig: (TextInputEditText.() -> Unit)? = null
    ): TextInputLayout {
        return TextInputLayout(context).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxBackgroundColorResource(R.color.light_grey)
            isHintEnabled = true
            setHintTextAppearance(R.style.InputHintStyle)
        }.also { til ->
            val et = TextInputEditText(context).apply {
                editTextConfig?.invoke(this)
            }
            til.addView(et)
        }
    }

    private fun createCMLView(
        iconResId: Int,
        name: String,
        homePage: String,
        uid: String
    ): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(8.dp, 8.dp, 8.dp, 8.dp)
            }
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)

            val rippleColor = ColorStateList.valueOf(Color.parseColor("#20000000"))
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
                layoutParams = LayoutParams(52.dp, 52.dp).apply {
                    marginEnd = 18.dp
                }
                setImageResource(iconResId)
                addView(this)
            }

            LinearLayout(context).apply {
                orientation = VERTICAL
                TextView(context).apply {
                    text = "$name"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
                    }
                    addView(this)
                }
                TextView(context).apply {
                    text = "$homePage"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
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
                    addView(this)
                }
            }.also { subLayout ->
                addView(subLayout)
            }

            setOnClickListener {
                if (homePage.isNotBlank() && homePage != "未填写") {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(homePage))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "链接格式错误或浏览器未安装",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "未设置主页链接",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }

    private fun createRoundedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setStroke(2.dp, Color.parseColor("#30000000"))
        }
    }

    private fun checkWriteMsaage() {
        val BName = etBliName.editText!!.text.toString()
        val BHomePage = etBliHP.editText!!.text.toString()
        val BUID = etBliUID.editText!!.text.toString()
        val KName = etKsName.editText!!.text.toString()
        val KHomePage = etKsHP.editText!!.text.toString()
        val KUID = etKsUID.editText!!.text.toString()
        val TName = etTTName.editText!!.text.toString()
        val THomePage = etTTHP.editText!!.text.toString()
        val TUID = etTTUID.editText!!.text.toString()

        etBliHP.error = null
        etBliUID.error = null
        etKsHP.error = null
        etKsUID.error = null
        etTTHP.error = null
        etTTUID.error = null

        if (BName.isEmpty() && KName.isEmpty() && TName.isEmpty()) {
            Toast.makeText(context, "请至少填写一项平台信息!", Toast.LENGTH_SHORT).show()
            return
        }

        if (BName.isNotEmpty()) {
            if (BHomePage.isEmpty()) {
                etBliHP.error = "链接不能为空"
                return
            }
            if (BUID.isEmpty()) {
                etBliUID.error = "UID不能为空"
                return
            }
        }

        if (KName.isNotEmpty()) {
            if (KHomePage.isEmpty()) {
                etKsHP.error = "链接不能为空"
                return
            }
            if (KUID.isEmpty()) {
                etKsUID.error = "UID不能为空"
                return
            }
        }

        if (TName.isNotEmpty()) {
            if (THomePage.isEmpty()) {
                etTTHP.error = "链接不能为空"
                return
            }
            if (TUID.isEmpty()) {
                etTTUID.error = "UID不能为空"
                return
            }
        }

        currentUserMessage = UserMessage(
            userId = userData.userId,
            userName = userData.userName,
            biliName = BName.takeIf { !it.isNullOrBlank() } ?: "",
            biliHomePage = BHomePage.takeIf { !it.isNullOrBlank() } ?: "",
            biliUID = BUID.takeIf { !it.isNullOrBlank() } ?: "",
            ksName = KName.takeIf { !it.isNullOrBlank() } ?: "",
            ksHomePage = KHomePage.takeIf { !it.isNullOrBlank() } ?: "",
            ksUID = KUID.takeIf { !it.isNullOrBlank() } ?: "",
            tiktokName = TName.takeIf { !it.isNullOrBlank() } ?: "",
            tiktokHomePage = THomePage.takeIf { !it.isNullOrBlank() } ?: "",
            tiktokUID = TUID.takeIf { !it.isNullOrBlank() } ?: ""
        )

        checkMDataFromClient(currentUserMessage)
    }

    private fun checkMDataFromClient(IUserMessageData: UserMessage) {
        client = Client(context)
        val progressDialog = MaterialAlertDialogBuilder(context)
                .setView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(32.dp, 32.dp, 32.dp, 32.dp)

                addView(ProgressBar(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        WRAP_CONTENT, 
                        WRAP_CONTENT
                    ).apply {
                        marginEnd = 16.dp
                    }
                })

                addView(TextView(context).apply {
                    text = "正在上传, 请稍等..."
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.tan))
                })
            })
            .setCancelable(false)
            .create()
        progressDialog.show()
        val userMap = mapOf(userData.userId to IUserMessageData)
        val content = try {
            Gson().toJson(userMap)
        } catch (e: Exception) {
            Log.e("RegistScreen", "JSON 转换失败", e)
            null
        }
        val operation = if (isEditMessage) {
            client::updateData
        } else {
            client::uploadData
        }

        operation.invoke("UserMessage", userData.userId, content,
            object : Client.ResultCallback {
                override fun onSuccess(content: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        userMessageMData.deleteMessage(userData.userId)
                        userMessageMData.saveMessage(IUserMessageData)
                        CreateCurrentMView()
                        isMessageFill = true
                        isEditMessage = false
                        showCurrentMessage()
                        updateMsState()
                    }
                }

                override fun onFailure(error: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun CreateCurrentMView() {
        biliView = createCMLView(
            R.drawable.ic_bilibili,
            currentUserMessage.biliName.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.biliHomePage.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.biliUID.takeIf { !it.isNullOrBlank() } ?: "未填写"
        )
        ksView = createCMLView(
            R.drawable.ic_ks,
            currentUserMessage.ksName.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.ksHomePage.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.ksUID.takeIf { !it.isNullOrBlank() } ?: "未填写"
        )
        tiktokView = createCMLView(
            R.drawable.ic_tiktok,
            currentUserMessage.tiktokName.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.tiktokHomePage.takeIf { !it.isNullOrBlank() } ?: "未填写",
            currentUserMessage.tiktokUID.takeIf { !it.isNullOrBlank() } ?: "未填写"
        )
    }

    private fun updateMsState() {
        if (onCreateMessage && !isMessageFill) {
            if (cancelCreateMS?.parent == null)
                topBar.addView(cancelCreateMS)

            if (editMessageL?.parent != null)
                topBar.removeView(editMessageL)

            if (onWriteMSLayout?.parent == null)
                messageViewCL.addView(onWriteMSLayout, LayoutParams(MATCH_PARENT, MATCH_PARENT))

            if (isMessageNF?.parent != null)
                messageViewCL.removeView(isMessageNF)

            if (currentMessage?.parent != null)
                messageViewCL.removeView(currentMessage)

            cancelCreateMS?.visibility = View.VISIBLE
            editMessageL?.visibility = View.GONE
            isMessageNF?.visibility = View.GONE
            onWriteMSLayout?.visibility = View.VISIBLE
            currentMessage?.visibility = View.GONE
        }
        if (!onCreateMessage && !isMessageFill) {
            if (cancelCreateMS?.parent != null)
                topBar.removeView(cancelCreateMS)

            if (editMessageL?.parent != null)
                topBar.removeView(editMessageL)

            if (isMessageNF?.parent == null)
                messageViewCL.addView(isMessageNF, LayoutParams(MATCH_PARENT, MATCH_PARENT))

            if (onWriteMSLayout?.parent != null)
                messageViewCL.removeView(onWriteMSLayout)

            if (currentMessage?.parent != null)
                messageViewCL.removeView(currentMessage)

            cancelCreateMS?.visibility = View.GONE
            editMessageL?.visibility = View.GONE
            isMessageNF?.visibility = View.VISIBLE
            onWriteMSLayout?.visibility = View.GONE
            currentMessage?.visibility = View.GONE
        }
        if (isMessageFill) {
            if (cancelCreateMS?.parent != null)
                topBar.removeView(cancelCreateMS)

            if (editMessageL?.parent == null)
                topBar.addView(editMessageL)

            if (onWriteMSLayout?.parent != null)
                messageViewCL.removeView(onWriteMSLayout)

            if (currentMessage?.parent == null) {
                messageViewCL.addView(currentMessage)
            }

            cancelCreateMS?.visibility = View.GONE
            editMessageL?.visibility = View.VISIBLE
            onWriteMSLayout?.visibility = View.GONE
            currentMessage?.visibility = View.VISIBLE
        }
    }

}
