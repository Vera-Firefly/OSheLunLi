package com.firefly.oshe.lunli.ui.screens

import android.content.Context
import android.graphics.Color
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.Tools
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures.ChatRoom
import com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures.Community
import com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures.HomePage
import com.firefly.oshe.lunli.utils.ImageUtils

class MainScreen(
    context: Context,
    private val userData: UserData,
    private val onExitToLogin: () -> Unit,
    private val onLogout: () -> Unit
) : LinearLayout(context) {

    private lateinit var topBar: LinearLayout
    private lateinit var mainView: LinearLayout
    private lateinit var userImage: ShapeableImageView

    private lateinit var chatRoomContent: ChatRoom
    private lateinit var cePageContent: Community
    private lateinit var homePageContent: HomePage
    private var selectedTabIndex = 0
    private var previousTabIndex = 0
    private var currentTabView: View? = null

    private var userInformation = UserInformationPref(context).getInformation(userData.userId)

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
        setupCEPage()
        setupHomePage()
        removeAllViews()
        addView(createTopBar())
        addView(createMainView())
        addView(createDivider())
        createEndBar()
        updateEndBarItems()

        currentTabView = chatRoom
        mainView.addView(currentTabView)
    }

    private fun setupChatRoom() {
        chatRoomContent = ChatRoom(context, userData).apply {
            setOnRoomSelectedListener {
                post {
                    topBar.removeAllViews()
                    getTopBarComponent()?.let { topBar.addView(it) }
                    endBar?.removeAllViews()
                    endBar?.layoutParams?.height = WRAP_CONTENT
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

    private fun setupCEPage() {
        cePageContent = Community(
            context,
            userData.userId,
            userData,
            UserInformation(userData.userId, userData.userName)
        )
    }

    private fun setupHomePage() {
        homePageContent = HomePage(
            context,
            userData,
            UserInformation(userData.userId, userData.userName)
        ).apply {
            setOnSignOutListener {
                post {
                    onLogout()
                }
            }
            setOnExitToLoginListener {
                post {
                    onExitToLogin()
                }
            }
            setOnUserImageChangeListener {
                post {
                    userInformation = UserInformationPref(context).getInformation(userData.userId)
                    val image = userInformation?.let { ImageUtils.base64ToBitmap(it.userImage) }
                    if (image != null) {
                        userImage.setImageBitmap(image)
                    } else {
                        context.ShowToast("无法更新主页图像, 请重启应用或联系管理人员")
                    }
                }
            }
        }
    }

    private fun createTopBar(): LinearLayout {
        topBar = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 40.dp)
            setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 0, 8.dp, 0)
            addUserAvatar()
            addView(CRStatus)
            addBackToMain()
        }
        return topBar
    }

    private fun createDivider(): View {
        return View(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 1.dp)
            setBackgroundColor(Color.LTGRAY)
        }
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
        val base64Image = userInformation?.userImage
        val image = base64Image?.let { ImageUtils.base64ToBitmap(it) }
        userImage = ShapeableImageView(context).apply {
            if (image != null) {
                setImageBitmap(image)
            } else {
                setImageResource(R.drawable.user)
            }
            setPadding(2.dp, 2.dp, 2.dp, 2.dp)
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
            onCommunity()
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
    }

    private fun LinearLayout.onCommunity() {
        cePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = VERTICAL

            addView(cePageContent.createView())
        }
    }

    private fun LinearLayout.onHomePage() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = VERTICAL

            addView(homePageContent.createView())
        }
    }


    private fun LinearLayout.createEndBar() {
        endBar = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 40.dp)
            setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 0, 8.dp, 0)
            createEndBarContainer()
        }
        addView(endBar)
    }
    
    private fun LinearLayout.createEndBarContainer() {
        endBarContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER

            createEndBarItem(R.drawable.comment, "聊天室", 0) {
                if (previousTabIndex != 0) {
                    onUserAvatar = true
                    onBackToMain = false
                    updateEndBarItems()
                    updateBarState()
                }
            }.also { addView(it) }


            createEndBarItem(R.drawable.community, "社区喵", 1) {
                if (previousTabIndex != 1) {
                    onUserAvatar = false
                    onBackToMain = true
                    updateEndBarItems()
                    updateBarState()
                }
            }.also { addView(it) }
            
            createEndBarItem(R.drawable.user, "主页喵", 2) {
                if (previousTabIndex != 2) {
                    onUserAvatar = false
                    onBackToMain = true
                    updateEndBarItems()
                    updateBarState()
                }
            }.also { addView(it) }
        }
        addView(endBarContainer)
    }
    
    private fun createEndBarItem(iconRes: Int, text: String, index: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(0, MATCH_PARENT, 1f)
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener {
                previousTabIndex = selectedTabIndex
                selectedTabIndex = index
                onClick()
            }
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

    private fun performSlideTransition(oldView: View?, newView: View?) {
        if (newView == null) return

        val slideDirection = if (selectedTabIndex > previousTabIndex) 1 else -1

        mainView.removeAllViews()

        newView.translationX = (mainView.width * slideDirection).toFloat()
        mainView.addView(newView)

        newView.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        oldView?.let { old ->
            mainView.addView(old)

            val totalDistance = mainView.width.toFloat()

            old.animate()
                .translationX(-totalDistance * slideDirection)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    mainView.removeView(old)
                }
                .start()
        }

        currentTabView = newView
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
            val selectedColor = ContextCompat.getColor(context, R.color.tan)
            val normalColor = ContextCompat.getColor(context, R.color.gray)
            
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

            userAvatar?.visibility = VISIBLE
            backToMain?.visibility = GONE
        }
        if (!onUserAvatar && onBackToMain) {
            if (userAvatar?.parent != null)
                topBar.removeView(userAvatar)

            if (CRStatus?.parent != null)
                topBar.removeView(CRStatus)

            if (backToMain?.parent == null)
                topBar.addView(backToMain)

            userAvatar?.visibility = GONE
            backToMain?.visibility = VISIBLE
        }

        val oldTabView = currentTabView
        val newTabView = when (selectedTabIndex) {
            0 -> chatRoom
            1 -> cePage
            2 -> homePage
            else -> chatRoom
        }

        performSlideTransition(oldTabView, newTabView)
    }
}
