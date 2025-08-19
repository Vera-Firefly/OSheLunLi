package com.firefly.oshe.lunli

import kotlin.concurrent.thread

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserMData
import com.firefly.oshe.lunli.data.UserMessage
import com.firefly.oshe.lunli.data.UserMessageMData
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.ui.screens.LoginScreen
import com.firefly.oshe.lunli.ui.screens.MainScreen
import com.firefly.oshe.lunli.ui.screens.MessageScreen
import com.firefly.oshe.lunli.ui.screens.RegistScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.json.JSONObject
import org.json.JSONException

public class MainActivity : Activity() {
    private val container by lazy { FrameLayout(this) }
    private var currentScreen: View? = null

    private lateinit var client: Client
    private lateinit var userMData: UserMData
    private lateinit var userMessageMData: UserMessageMData
    private var currentUser = UserData()

    private val REQUEST_CODE = 12
    private val REQUEST_CODE_PERMISSION = 0x00099

    private var hasAllFilesPermission = false
    private var isNoticedAllFilesPermissionMissing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thread {
            MarkdownRenderer.init(applicationContext)
        }

        container.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContentView(container)
        client = Client(this)
        userMData = UserMData(this)
        userMessageMData = UserMessageMData(this)

        val lastUserId = UserMData.getLastUser(this)

        lastUserId?.let { userId ->
            userMData.getUser(userId)?.let { user ->
                currentUser.apply {
                    this.userId = user.userId
                    userName = user.userName
                    password = if (user.hasPasswordError) "" else user.password
                    hasPasswordError = user.hasPasswordError
                }
                if (user.hasPasswordError) {
                    showLoginScreen(0)
                    Toast.makeText(this, "请重新输入密码", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

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
                Toast.makeText(
                    this,
                    "拒绝授权将导致部分功能无法正常工作，请重启软件以授权",
                    Toast.LENGTH_SHORT
                ).show()
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

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesPermission = Environment.isExternalStorageManager()
            if (!hasAllFilesPermission && !isNoticedAllFilesPermissionMissing) {
                Toast.makeText(this, "拒绝授权将导致部分功能无法正常工作", Toast.LENGTH_SHORT).show()
                isNoticedAllFilesPermissionMissing = true
                checkPermission()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun switchScreen(newScreen: View, animationType: Int = 1) {
        val oldScreen = currentScreen

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
        val userList = userMData.getAllUsers().keys.toMutableList()
        val screen = LoginScreen(
            context = this,
            userData = currentUser,
            userList = userList,
            userMData = userMData,
            userMessageMData = userMessageMData,
            onLoginSuccess = { id ->
                UserMData.setLastUser(this, id)
                userMData.getUser(id)?.let { user ->
                    currentUser = user.copy().apply {
                        hasPasswordError = false
                    }
                    userMData.clearPasswordError(id)
                }
                showMainScreen(1)
            },
            onRegisterClick = { showRegisterScreen(3) }
        )
        switchScreen(screen, anim)
    }

    // 注册界面事件处理
    fun showRegisterScreen(anim: Int) {
        val screen = RegistScreen(
            context = this,
            userMData = userMData,
            onRegisterSuccess = { name, id, pwd ->
                currentUser = UserData(
                    userId = id,
                    userName = name,
                    password = pwd
                )
                userMData.saveUser(currentUser)
                UserMData.setLastUser(this, id)
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
            onMessage = { getMessageFromData(currentUser.userId) },
            onLogout = { showLogoutConfirmDialog() }
        )
        switchScreen(screen, anim)
    }

    // 从数据获取用户信息?
    private fun getMessageFromData(userId: String) {
        val progressDialog = MaterialAlertDialogBuilder(this)
                .setView(LinearLayout(this).apply {
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
                    text = "正在加载, 请稍等..."
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.tan))
                })
            })
            .setCancelable(false)
            .create()
        progressDialog.show()
        client.getData("UserMessage", userId,
            object : Client.ResultCallback {
                override fun onSuccess(content: String) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        isGetMessage(content, userId)
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        showMessageScreen(userId, 3)
                        var Err: String = error
                        if (error == "UserFile") Err = "该账户未创建信息"
                        Toast.makeText(this@MainActivity, Err, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    // 获取信息: 处理数据并写入本地
    private fun isGetMessage(IUserMessage: String, IUserId: String) {
        try {
            val rootObject = JSONObject(IUserMessage)
            rootObject.keys().forEach { userId: String ->
                rootObject.getJSONObject(userId)?.let { user ->
                    userMessageMData.deleteMessage(IUserId)
                    val data = UserMessage(
                        userId = user.optString("userId"),
                        userName = user.optString("userName"),
                        biliName = user.optString("biliName"),
                        biliHomePage = user.optString("biliHomePage"),
                        biliUID = user.optString("biliUID"),
                        ksName = user.optString("ksName"),
                        ksHomePage = user.optString("ksHomePage"),
                        ksUID = user.optString("ksUID"),
                        tiktokName = user.optString("tiktokName"),
                        tiktokHomePage = user.optString("tiktokHomePage"),
                        tiktokUID = user.optString("tiktokUID")
                    )
                    userMessageMData.saveMessage(data)
                    showMessageScreen(IUserId, 3)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // 信息界面
    fun showMessageScreen(id: String, anim: Int) {
        val screen = MessageScreen(
            context = this,
            userData = currentUser,
            currentId = id,
            returnMainScreen = { showMainScreen(4) }
        )
        switchScreen(screen, anim)
    }

    // 更新用户配置文件
    fun updateUserProfile(newData: UserData) {
        if (newData.userId == currentUser.userId) {
            currentUser.userName = newData.userName
            userMData.saveUser(currentUser)
        }
    }

    // 登出确认
    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("退出确认")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                currentUser.password = ""
                userMData.isUserExist(currentUser.userId)
                showLoginScreen(2)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
