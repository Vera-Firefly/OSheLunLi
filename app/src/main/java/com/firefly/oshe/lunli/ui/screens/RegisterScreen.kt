package com.firefly.oshe.lunli.ui.screens

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import androidx.core.graphics.toColorInt
import com.firefly.oshe.lunli.Tools
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterScreen(
    context: Context,
    private val onRegisterSuccess: (String, String, String, String) -> Unit,
    private val onCancelRegister: () -> Unit
) : LinearLayout(context) {

    private lateinit var client: Client
    private lateinit var etUserName: TextInputLayout
    private lateinit var etUserId: TextInputLayout
    private lateinit var etPassword: TextInputLayout

    private val interaction by lazy {
        Interaction(context)
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(16.dp, 8.dp, 16.dp, 0)
        setupViews()
    }

    private fun setupViews() {
        val buttonLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8.dp
            }
        }

        etUserName = interaction.createEditText("用户名").apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etUserId = interaction.createEditText("用户ID (7~8位)") {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        }.apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        etPassword = interaction.createEditText("设置密码") {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }.apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        interaction.createButton("取消", R.color.light_blue) {
            onCancelRegister()
        }.apply {
                layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        interaction.createButton("注册", R.color.light_blue) {
            validateInputs()?.let { (name, id, pwd) ->
                checkUDFromClient(name, id, pwd)
            }
        }.apply {
                layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        addView(etUserName)
        addView(etUserId)
        addView(etPassword)
        addView(buttonLayout)
    }

    private fun checkUDFromClient(UserName: String, UserId: String, UserPWD: String) {
        val progressDialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(32.dp, 32.dp, 32.dp, 32.dp)

                addView(ProgressBar(context).apply {
                    layoutParams = LayoutParams(
                        WRAP_CONTENT,
                        WRAP_CONTENT
                    ).apply {
                        marginEnd = 16.dp
                    }
                })

                addView(TextView(context).apply {
                    text = "正在检查用户ID..."
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.tan))
                })
            })
            .setCancelable(false)
            .create()
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingUser = SBClient.fetchUser(UserId)

                (context as? Activity)?.runOnUiThread {
                    progressDialog.dismiss()

                    if (existingUser != null) {
                        showError(2, "用户ID已存在")
                        val error = "账户 $UserId 已存在, 请勿重复创建"
                        context.ShowToast(error)
                    } else {
                        createUDFromClient(UserName, UserId, UserPWD, progressDialog)
                    }
                }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    progressDialog.dismiss()
                    context.ShowToast("失败, 正在进行降级处理")
                    fallbackToClientCheck(UserName, UserId, UserPWD)
                }
            }
        }
    }

    private fun fallbackToClientCheck(UserName: String, UserId: String, UserPWD: String) {
        client = Client(context)

        val progressDialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(32.dp, 32.dp, 32.dp, 32.dp)

                addView(ProgressBar(context).apply {
                    layoutParams = LayoutParams(
                        WRAP_CONTENT,
                        WRAP_CONTENT
                    ).apply {
                        marginEnd = 16.dp
                    }
                })

                addView(TextView(context).apply {
                    text = "正在检查用户ID..."
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(context, R.color.tan))
                })
            })
            .setCancelable(false)
            .create()
        progressDialog.show()

        client.getData("UserData", UserId,
            object : Client.ResultCallback {
                override fun onSuccess(content: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        showError(2, "用户ID已存在")
                        val error = "账户 $UserId 已存在, 请勿重复创建"
                        context.ShowToast(error)
                    }
                }

                override fun onFailure(error: String) {
                    (context as? Activity)?.runOnUiThread {
                        createUDFromClient(UserName, UserId, UserPWD, progressDialog)
                    }
                }
            })
    }

    private fun createUDFromClient(UserName: String, UserId: String, UserPWD: String, progressDialog: AlertDialog) {
        client = Client(context)
        val userData = UserData(
            userId = UserId,
            userName = UserName,
            password = UserPWD
        )
        val userMap = mapOf(UserId to userData)
        val message = try {
            Gson().toJson(userMap)
        } catch (e: Exception) {
            Log.e("RegistScreen", "JSON 转换失败", e)
            null
        }

        client.uploadData("UserData", UserId, message,
            object : Client.ResultCallback {
                override fun onSuccess(content: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        val drawable = context.getDrawable(R.drawable.user)
                        val image = drawable?.let { ImageUtils.drawableToBase64(it) }
                        if (image != null) {
                            // 同时在 SBClient 创建用户（UID, Name, Image）
                            SBClient.createUser(
                                UserId,
                                UserName,
                                image
                            )
                            onRegisterSuccess(UserName, UserId, UserPWD, image)
                        } else {
                            context.ShowToast("资源创建失败, 请联系管理人员")
                        }
                    }
                }

                override fun onFailure(error: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        context.ShowToast(error)
                    }
                }
            })
    }

    private fun validateInputs(): Triple<String, String, String>? {
        val name = etUserName.editText!!.text.toString()
        val id = etUserId.editText!!.text.toString()
        val pwd = etPassword.editText!!.text.toString()

        return when {
            name.isEmpty() -> showError(1, "用户名不能为空")
            name.length !in 1..20 -> showError(1, "用户名长度1-20位")
            id.isEmpty() -> showError(2, "用户ID不能为空")
            id.length !in 7..8 -> showError(2, "用户ID长度应为7位或8位")
            !id.matches(Regex("^\\d+\$")) -> showError(2, "用户ID只能包含数字")
            pwd.isEmpty() -> showError(3, "密码不能为空")
            pwd.length < 8 -> showError(3, "密码至少8位")
            else -> Triple(name, id, pwd)
        }
    }

    private fun showError(fieldType: Int, message: String): Nothing? {
        context.ShowToast(message)
        when (fieldType) {
            1 -> etUserName.error = message
            2 -> etUserId.error = message
            3 -> etPassword.error = message
        }
        return null
    }
}