package com.firefly.oshe.lunli.ui.screens

import android.content.Context
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserImage
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures.ChatRoom
import com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures.HomePage

class MainScreen(
    context: Context,
    private val userData: UserData,
    private val onMessage: () -> Unit,
    private val onLogout: () -> Unit
) : LinearLayout(context) {

    private lateinit var topBar: LinearLayout
    private lateinit var mainView: LinearLayout

    private lateinit var chatRoomContent: ChatRoom
    private lateinit var homePageContent: HomePage
    private var selectedTabIndex = 0

    private var onUserAvatar: Boolean = true
        set(value) {
            if (field == value) return
            field = value
        }

    private var onBackToMain: Boolean = false
        set(value) {
            if (field == value) return
            field = value
        }

    private var endBar: LinearLayout? = null
    private var userAvatar: LinearLayout? = null
    private var backToMain: LinearLayout? = null
    private var chatRoom: LinearLayout? = null
    private var cePage: LinearLayout? = null
    private var homePage: LinearLayout? = null
    private var endBarContainer: LinearLayout? = null

    private var CRStatus: LinearLayout? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setupViews()
    }

    private fun setupViews() {
        setupChatRoom()
        setupHomePage()
        removeAllViews()
        addView(createTopBar())
        addView(createMainView())
        createEndBar()
        updateEndBarItems()
    }

    private fun setupChatRoom() {
        chatRoomContent = ChatRoom(context, userData).apply {
            setOnRoomSelectedListener {
                post {
                    topBar.removeAllViews()
                    getTopBarComponent()?.let { topBar.addView(it) }
                    endBar?.removeAllViews()
                    endBar?.layoutParams?.height = LayoutParams.WRAP_CONTENT
                    endBar?.addView(createInputContainer())
                }
            }
            setOnBackClickListener {
                post {
                    topBar.removeAllViews()
                    userAvatar?.let { topBar.addView(it) }
                    CRStatus?.let { topBar.addView(it) }
                    endBar?.removeAllViews()
                    endBar?.layoutParams?.height = 40.dp
                    endBarContainer?.let { endBar?.addView(it) }
                }
            }
        }
        CRStatus = chatRoomContent.setRoomStatus()
    }

    private fun setupHomePage() {
        homePageContent = HomePage(
            context,
            userData.userId,
            userData,
            UserInformation("1"),
            UserImage("1", "DUMMY")
        ).apply {
            setOnSignOutListener {
                post {
                    onLogout()
                }
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
            addUserAvatar()
            addView(CRStatus)
            addBackToMain()
        }
        return topBar
    }

    private fun LinearLayout.addUserAvatar() {
        userAvatar = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            orientation = HORIZONTAL
            gravity = Gravity.START
            addUserImage()
            addUserMessage()
        }
        if (onUserAvatar) addView(userAvatar)
    }

    private fun LinearLayout.addUserImage() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.user)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                selectedTabIndex = 2
                onUserAvatar = false
                onBackToMain = true
                updateEndBarItems()
                updateBarState()
            }
            layoutParams = LayoutParams(
                WRAP_CONTENT, 
                WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(this, 40.dp, 40.dp)
        }
    }

    private fun LinearLayout.addUserMessage() {
         MaterialTextView(context).apply {
            text = userData.userName + " (" + userData.userId + ")"
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(this)
        }
    }

    private fun LinearLayout.addBackToMain() {
        backToMain = LinearLayout(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            orientation = HORIZONTAL
            gravity = Gravity.START
            addBackButton()
            addBackMessage()
        }
        if (onBackToMain) addView(backToMain)
    }

    private fun LinearLayout.addBackButton() {
        ShapeableImageView(context).apply {
            setImageResource(R.drawable.arrow_left_bold)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(8f.dp)
                .build()
            setOnClickListener {
                selectedTabIndex = 0
                onUserAvatar = true
                onBackToMain = false
                updateEndBarItems()
                updateBarState()
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

    private fun createMainView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 0, 1f)
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(8.dp, 0, 8.dp, 0)
            onChatRoom()
            // onCommunity()
            onHomePage()
        }
        return mainView
    }

    private fun LinearLayout.onChatRoom() {
        chatRoom = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = VERTICAL

            addView(chatRoomContent.createView())
        }
        if (selectedTabIndex == 0) addView(chatRoom)
    }

    private fun LinearLayout.onCommunity() {
        cePage = LinearLayout(context).apply {
            // TODO: 
        }
        if (selectedTabIndex == 1) addView(cePage)
    }

    private fun LinearLayout.onHomePage() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = VERTICAL

            addView(homePageContent.createView())
        }
        if (selectedTabIndex == 2) addView(homePage)
    }


    private fun LinearLayout.createEndBar() {
        endBar = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 40.dp)
            setBackgroundColor(ContextCompat.getColor(context, R.color.tan))
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 0, 8.dp, 0)
            createEndBarContainer()
        }
        // 隐藏底部栏
        // if (selectedTabIndex == 0)
        addView(endBar)
    }
    
    private fun LinearLayout.createEndBarContainer() {
        endBarContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER

            addView(createEndBarItem(
                iconRes = R.drawable.comment,
                text = "聊天室",
                index = 0,
                onClick = { 
                    selectedTabIndex = 0
                    onUserAvatar = true
                    onBackToMain = false
                    updateEndBarItems()
                    updateBarState()
                }
            ))

            addView(createEndBarItem(
                iconRes = R.drawable.community,
                text = "社区喵",
                index = 1,
                onClick = { 
                    selectedTabIndex = 1
                    onUserAvatar = false
                    onBackToMain = true
                    updateEndBarItems()
                    updateBarState()
                }
            ))
            
            addView(createEndBarItem(
                iconRes = R.drawable.user,
                text = "主页喵",
                index = 2,
                onClick = { 
                    selectedTabIndex = 2
                    onUserAvatar = false
                    onBackToMain = true
                    updateEndBarItems()
                    updateBarState()
                }
            ))
        }
        addView(endBarContainer)
    }
    
    private fun createEndBarItem(iconRes: Int, text: String, index: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(0, MATCH_PARENT, 1f)
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
            tag = "tab_$index" // 为每个项设置唯一tag
            
            // 图标
            ShapeableImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LayoutParams(20.dp, 20.dp)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(4f.dp)
                    .build()
                tag = "icon_$index"
                addView(this)
            }
        
            // 文字
            MaterialTextView(context).apply {
                this.text = text
                textSize = 10f
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 2.dp
                }
                tag = "text_$index"
                addView(this)
            }
        }
    }
    
    private fun updateEndBarItems() {
        val container = endBar?.getChildAt(0) as? LinearLayout ?: return
    
        for (i in 0 until container.childCount) {
            val item = container.getChildAt(i) as? LinearLayout ?: continue
            val isSelected = i == selectedTabIndex
        
            // 获取图标和文字视图
            val icon = item.findViewWithTag<ShapeableImageView>("icon_$i")
            val text = item.findViewWithTag<MaterialTextView>("text_$i")
        
            // 更新选中状态
            val selectedColor = ContextCompat.getColor(context, R.color.white)
            val normalColor = ContextCompat.getColor(context, R.color.black)
            
            icon?.setColorFilter(if (isSelected) selectedColor else normalColor)
            text?.setTextColor(if (isSelected) selectedColor else normalColor)
        }
    }

    private fun updateBarState() {
        if (onUserAvatar && !onBackToMain) {
            if (userAvatar?.parent == null)
                topBar.addView(userAvatar)

            if (CRStatus?.parent == null)
                topBar.addView(CRStatus)

            if (backToMain?.parent != null)
                topBar.removeView(backToMain)

            userAvatar?.visibility = View.VISIBLE
            backToMain?.visibility = View.GONE
        }
        if (!onUserAvatar && onBackToMain) {
            if (userAvatar?.parent != null)
                topBar.removeView(userAvatar)

            if (CRStatus?.parent != null)
                topBar.removeView(CRStatus)

            if (backToMain?.parent == null)
                topBar.addView(backToMain)

            userAvatar?.visibility = View.GONE
            backToMain?.visibility = View.VISIBLE
        }
        // 未实现, 暂时不启用

        when (selectedTabIndex) {
            // 聊天室
            0 -> {
                homePage?.let { if (it.parent != null) mainView.removeView(it) }
                cePage?.let { if (it.parent != null) mainView.removeView(it) }
                
                if (chatRoom?.parent == null) {
                    chatRoom?.let { mainView.addView(it) }
                }
                chatRoom?.visibility = View.VISIBLE
            }

            // 社区
            1 -> {
                homePage?.let { if (it.parent != null) mainView.removeView(it) }
                chatRoom?.let { if (it.parent != null) mainView.removeView(it) }
            
                // 添加或显示设置页
                if (cePage?.parent == null) {
                    onCommunity()
                    cePage?.let { mainView.addView(it) }
                }
                cePage?.visibility = View.VISIBLE
            }

            // 主页
            2 -> {
                chatRoom?.let { if (it.parent != null) mainView.removeView(it) }
                cePage?.let { if (it.parent != null) mainView.removeView(it) }

                // 添加或显示用户页面
                if (homePage?.parent == null) {
                    homePage?.let { mainView.addView(it) }
                }
                homePage?.visibility = View.VISIBLE
            }
        }

    }
}
