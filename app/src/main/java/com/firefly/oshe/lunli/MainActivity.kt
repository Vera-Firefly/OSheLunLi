package com.firefly.oshe.lunli

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.GlobalInterface.ImagePicker
import com.firefly.oshe.lunli.GlobalInterface.ImageSelectionManager
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.data.UserInformationPref
import com.firefly.oshe.lunli.ui.screens.LoginScreen
import com.firefly.oshe.lunli.ui.screens.MainScreen
import com.firefly.oshe.lunli.ui.screens.RegisterScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val container by lazy { FrameLayout(this) }
    private var currentScreen: View? = null

    private lateinit var client: Client
    private lateinit var userDataPref: UserDataPref
    private lateinit var imagePicker: ImagePicker
    private lateinit var backgroundManager: BackgroundManager


    private val REQUEST_CODE = 12
    private val REQUEST_CODE_PERMISSION = 0x00099


    private var currentUser = UserData()
    private var colorAnimator: ValueAnimator? = null

    private var isBackgroundAnimated = false
    private var hasAllFilesPermission = false
    private var isNoticedAllFilesPermissionMissing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thread {
            MarkdownRenderer.init(applicationContext)
        }

        container.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContentView(container)

        backgroundManager = BackgroundManager(this)
        initBackgroundManager()

        client = Client(this)
        userDataPref = UserDataPref(this)

        val lastUserId = UserDataPref.getLastUser(this)

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
                    Tools().ShowToast(this, "请重新输入密码")
                    return
                }
            }
        }

        imagePicker = ImagePicker(this)

        if (currentUser.isValid()) {
            showMainScreen(0)
        } else {
            showLoginScreen(0)
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
                Tools().ShowToast(this, "拒绝授权将导致部分功能无法正常工作")
                isNoticedAllFilesPermissionMissing = true
                checkPermission()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
                Tools().ShowToast(
                    this,
                    "拒绝授权将导致部分功能无法正常工作，请重启软件以授权",
                )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imagePicker.handleActivityResult(requestCode, resultCode, data)
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

    fun switchToCustomBackground(drawable: android.graphics.drawable.Drawable) {
        backgroundManager.setCustomImageBackground(container, drawable)
    }

    fun switchToCustomBackground(bitmap: android.graphics.Bitmap) {
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
        val nearWhite = Color.parseColor("#e8dcbf")
        val lightWarmBeige = Color.parseColor("#f1e8d5")
        val warmBeige = Color.parseColor("#f8f4e9")

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
            context = this,
            onLoginSuccess = { id ->
                UserDataPref.setLastUser(this, id)
                userDataPref.getUser(id)?.let { user ->
                    currentUser = user.copy().apply {
                        hasPasswordError = false
                    }
                    userDataPref.clearPasswordError(id)
                }
                showMainScreen(1)
            },
            onRegisterClick = { showRegisterScreen(3) }
        )
        switchScreen(screen, anim)
    }

    // 注册界面事件处理
    fun showRegisterScreen(anim: Int) {
        val screen = RegisterScreen(
            context = this,
            onRegisterSuccess = { name, id, pwd, image ->
                currentUser = UserData(id, name, pwd)
                val inf = UserInformation(
                    currentUser.userId,
                    currentUser.userName,
                    image,
                    ""
                )
                userDataPref.saveUser(currentUser)
                UserDataPref.setLastUser(this, id)
                UserInformationPref(this).saveInformation(inf)
                showMainScreen(1)
            },
            onCancelRegister = { showLoginScreen(4) }
        )
        switchScreen(screen, anim)
    }

    // 主界面事件处理
    fun showMainScreen(anim: Int) {
        val screen = MainScreen(
            context = this,
            userData = currentUser,
            onExitToLogin = { showLoginScreen(4) },
            onLogout = { showLogoutConfirmDialog() }
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
