package com.firefly.oshe.lunli

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.Color.GRAY
import android.graphics.Color.WHITE
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.Gravity.CENTER
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.GlobalInterface.ImagePicker
import com.firefly.oshe.lunli.GlobalInterface.ImageSelectionManager
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.feature.UpdateLauncher.UpdateLauncher
import com.firefly.oshe.lunli.settings.ANNOUNCEMENT_DONE
import com.firefly.oshe.lunli.settings.interfaces.SettingsRegistry
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.popup.PopupManager
import com.firefly.oshe.lunli.ui.popup.PopupOverlay
import com.firefly.oshe.lunli.ui.screens.LoginScreen
import com.firefly.oshe.lunli.ui.screens.MainScreen
import com.firefly.oshe.lunli.ui.screens.RegisterScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val container by lazy { FrameLayout(this) }
    private var currentScreen: View? = null

    private lateinit var context: Context
    private lateinit var updateLauncher: UpdateLauncher
    private lateinit var client: Client
    private lateinit var userDataPref: UserDataPref
    private lateinit var imagePicker: ImagePicker
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var popupOverlay: PopupOverlay
    private lateinit var interaction: Interaction

    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    private val REQUEST_CODE = 12
    private val REQUEST_CODE_PERMISSION = 0x00099

    private var currentUser = UserData()
    private var colorAnimator: ValueAnimator? = null

    private var isBackgroundAnimated = false
    private var hasAllFilesPermission = false
    private var isNoticedAllFilesPermissionMissing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = this
        container.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContentView(container)

        SettingsRegistry.initialize(this)

        mainScope.launch {
            val settings = SettingsRegistry.get()
            settings.preload()

            // 初始化
            MarkdownRenderer.init(context)

            popupOverlay = PopupOverlay.create(context, container)
            PopupManager.initialize(popupOverlay)

            interaction = Interaction(context)
            backgroundManager = BackgroundManager(context)
            client = Client(context)
            userDataPref = UserDataPref(context)
            imagePicker = ImagePicker(this@MainActivity)
            updateLauncher = UpdateLauncher(context)

            if (settings.isPreloaded()) {
                initMainView()
            }
        }
    }

    private fun announcement(): View {
        return LinearLayout(this).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(10, 10, 10, 10)
            }

            background = GradientDrawable().apply {
                setColor(WHITE)
                this.cornerRadius = 8f
            }

            TextView(context).apply {
                text = "公告"
                textSize = 18f
                setTextColor(BLACK)
                gravity = CENTER
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 10, 0, 10)
                }
            }.also { addView(it) }

            TextView(context).apply {
                text = "由于某些原因, 自动检测更新功能将延迟到下个版本推出\nPS: 哪个傻逼乱开房, 还开了个问题房间, 给我数据库搞炸了, 搞得我以为哪次修改动大动脉了, 害我查了老半天问题\n伤心😭😭😭😭😭😭😭😭😭"
                textSize = 14f
                isSingleLine = false
                setTextColor(GRAY)
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(24, 10, 24, 10)
                }
            }.also { addView(it) }

            val buttonLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(8.dp, 0, 8.dp, 4.dp)
                }
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            interaction.createButton("确认(不再显示)", R.color.light_blue) {
                PopupManager.dismiss()
                ANNOUNCEMENT_DONE = true
            }.apply {
                layoutParams = LayoutParams(WRAP_CONTENT,  WRAP_CONTENT)
            }.also { buttonLayout.addView(it) }

            addView(buttonLayout)
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasAllFilesPermission = true
            } else {
                showPermissionDialog()
            }
        } else {
            requestLegacyPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesPermission = Environment.isExternalStorageManager()
            if (!hasAllFilesPermission && !isNoticedAllFilesPermissionMissing) {
                this.ShowToast("拒绝授权将导致部分功能无法正常工作")
                isNoticedAllFilesPermissionMissing = true
                checkPermission()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imagePicker.handleActivityResult(requestCode, resultCode, data)
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("权限请求")
            .setMessage("程序需要获取访问所有文件权限才能正常使用")
            .setPositiveButton("是") { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE)
                isNoticedAllFilesPermissionMissing = false
            }
            .setNegativeButton("否") { _: DialogInterface?, _: Int ->
                isNoticedAllFilesPermissionMissing = true
                this.ShowToast("拒绝授权将导致部分功能无法正常工作，请重启软件以授权")
            }
            .setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
            .setCancelable(false)
            .show()
    }

    private fun requestLegacyPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSION
            )
        } else {
            hasAllFilesPermission = true
        }
    }

    fun startImageSelection() {
        imagePicker.pickImage { uri ->
            if (uri != null) {
                ImageSelectionManager.notifyImageSelected(uri)
            }
            else
            {
                ImageSelectionManager.notifySelectionCancelled()
            }
        }
    }

    private fun initMainView() {
        // 不要动这里的try-catch/-finally语句, 在你不知道为啥之前
        try {
            initBackgroundManager()

            if (ANNOUNCEMENT_DONE) ANNOUNCEMENT_DONE = false

            val lastUserId = userDataPref.getLastUser()

            lastUserId?.let { userId ->
                userDataPref.getUser(userId)?.let { user ->
                    currentUser.apply {
                        this.userId = user.userId
                        userName = user.userName
                        password = if (user.hasPasswordError) "" else user.password
                        hasPasswordError = user.hasPasswordError
                    }
                    if (user.hasPasswordError) {
                        showLoginScreen(0)
                        context.ShowToast("请重新输入密码")
                        return
                    }
                }
            }

            if (currentUser.isValid()) {
                showMainScreen(0)
            } else {
                showLoginScreen(0)
            }

            if (!ANNOUNCEMENT_DONE) PopupManager.show(announcement())

            updateLauncher.checkForUpdates(true)
        } finally {
            println("All View Done")
        }
    }

    private fun initBackgroundManager() {
        backgroundManager = BackgroundManager(this).apply {
            setBackgroundChangeListener(object : BackgroundManager.OnBackgroundChangeListener {
                override fun onBackgroundChanged(type: BackgroundManager.BackgroundType) {
                    when (type) {
                        BackgroundManager.BackgroundType.GRADIENT_BEIGE -> {
                            startBackgroundAnimation()
                        }
                        BackgroundManager.BackgroundType.CUSTOM_IMAGE -> {
                            stopBackgroundAnimation()
                        }
                        BackgroundManager.BackgroundType.NONE -> {
                            stopBackgroundAnimation()
                        }
                    }
                }
            })

            setBackground(container, BackgroundManager.BackgroundType.GRADIENT_BEIGE)
        }
    }

    // 背景选择, 暂时不启用
    fun switchToCustomBackground(drawable: Drawable) {
        backgroundManager.setCustomImageBackground(container, drawable)
    }

    fun switchToCustomBackground(bitmap: Bitmap) {
        backgroundManager.setCustomImageBackground(container, bitmap)
    }

    fun switchToGradientBackground() {
        backgroundManager.restoreDefaultBackground(container)
    }

    fun clearBackground() {
        backgroundManager.clearBackground(container)
    }

    // 背景动画控制
    private fun startBackgroundAnimation() {
        isBackgroundAnimated = true
        colorAnimator?.cancel()

        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                updateBackgroundColor(progress)
            }

            start()
        }
    }

    private fun stopBackgroundAnimation() {
        isBackgroundAnimated = false
        colorAnimator?.cancel()
    }

    private fun updateBackgroundColor(progress: Float) {
        val nearWhite = Color.parseColor("#f8f6f2")
        val lightWarmBeige = Color.parseColor("#fbfaf8")
        val warmBeige = Color.parseColor("#fefefe")

        val currentColor = ColorUtils.blendColors(nearWhite, warmBeige, progress)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                currentColor,
                ColorUtils.blendColors(nearWhite, lightWarmBeige, progress),
                ColorUtils.blendColors(lightWarmBeige, warmBeige, progress)
            )
        )

        backgroundManager.updateCurrentBackground(gradientDrawable)
    }

    private fun switchScreen(newScreen: View, animationType: Int = 1) {
        val oldScreen = currentScreen

        // 设置主要视图透明背景, 防止覆盖应用背景
        newScreen.setBackgroundColor(Color.TRANSPARENT)

        // 设置新屏幕初始位置（根据动画方向）
        when (animationType) {
            1 -> newScreen.translationY = container.height.toFloat()  // 上：从下方进入
            2 -> newScreen.translationY = -container.height.toFloat() // 下：从上方进入
            3 -> newScreen.translationX = container.width.toFloat()   // 左：从右侧进入
            4 -> newScreen.translationX = -container.width.toFloat()  // 右：从左侧进入
        }
        container.addView(newScreen)

        // 无动画直接切换
        if (animationType == 0) {
            oldScreen?.let { container.removeView(it) }
            currentScreen = newScreen
            return
        }

        // 新屏幕动画：滑入到正常位置
        newScreen.animate()
            .translationX(0f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // 旧屏幕动画: 滑出 + 灰度渐变
        oldScreen?.let { oldView ->
            // 创建灰度遮罩层（初始透明）
            val grayOverlay = View(oldView.context).apply {
                setBackgroundColor(Color.GRAY)
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(oldView.width, oldView.height)
            }
            container.addView(grayOverlay)

            // 遮罩层渐变显示（0 → 0.6）
            grayOverlay.animate()
                .alpha(0.6f)
                .setDuration(300)
                .start()

            // 旧视图滑出动画
            oldView.animate().apply {
                when (animationType) {
                    1 -> translationY(-oldView.height.toFloat())
                    2 -> translationY(oldView.height.toFloat())
                    3 -> translationX(-oldView.width.toFloat())
                    4 -> translationX(oldView.width.toFloat())
                }
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                setUpdateListener { _ ->
                    // 同步遮罩层位置
                    grayOverlay.translationX = oldView.translationX
                    grayOverlay.translationY = oldView.translationY
                }
                withEndAction {
                    container.removeView(oldView)
                    container.removeView(grayOverlay)
                }
                start()
            }
        }

        currentScreen = newScreen
    }

    // 登陆界面事件处理
    fun showLoginScreen(anim: Int) {
        val screen = LoginScreen(
            this,
            { id ->
                userDataPref.setLastUser(id)
                userDataPref.getUser(id)?.let { user ->
                    currentUser = user.copy().apply {
                        hasPasswordError = false
                    }
                    userDataPref.clearPasswordError(id)
                }
                showMainScreen(1)
            },
            { showRegisterScreen(3) }
        )
        switchScreen(screen, anim)
    }

    // 注册界面事件处理
    fun showRegisterScreen(anim: Int) {
        val screen = RegisterScreen(
            this,
            { name, id, pwd, image ->
                currentUser = UserData(id, name, pwd)
                val inf = UserInformation(
                    currentUser.userId,
                    currentUser.userName,
                    image,
                    ""
                )
                userDataPref.saveUser(currentUser)
                userDataPref.setLastUser(id)
                UserInformationPref(this).saveInformation(inf)
                showMainScreen(1)
            },
            { showLoginScreen(4) }
        )
        switchScreen(screen, anim)
    }

    // 主界面事件处理
    fun showMainScreen(anim: Int) {
        val screen = MainScreen(
            this,
            currentUser,
            { showLoginScreen(4) },
            { showLogoutConfirmDialog() }
        )
        switchScreen(screen, anim)
    }

    // 更新用户配置文件
    fun updateUserProfile(newData: UserData) {
        if (newData.userId == currentUser.userId) {
            currentUser.userName = newData.userName
            userDataPref.saveUser(currentUser)
        }
    }

    // 登出确认
    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("退出确认")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                currentUser.password = ""
                userDataPref.isUserExist(currentUser.userId)
                showLoginScreen(2)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    object ColorUtils {
        fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRatio = 1 - ratio
            val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
            val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
            val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
            return Color.rgb(r, g, b)
        }
    }

}
