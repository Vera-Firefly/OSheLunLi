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
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.client.SupaBase.SBClient
import androidx.core.graphics.toColorInt

class RegistScreen(
    context: Context,
    private val userDataPref: UserDataPref,
    private val onRegisterSuccess: (String, String, String) -> Unit,
    private val onCancelRegister: () -> Unit
) : LinearLayout(context) {

    private lateinit var client: Client
    private lateinit var etUserName: TextInputLayout
    private lateinit var etUserId: TextInputLayout
    private lateinit var etPassword: TextInputLayout

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

        etUserName = createEditText("用户名").apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        etUserId = createEditText("用户ID") {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        }.apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        etPassword = createEditText("设置密码") {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }.apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        createButton("取消") {
            onCancelRegister()
        }.apply {
                layoutParams = LinearLayout.LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        createButton("注册") {
            validateInputs()?.let { (name, id, pwd) ->
                checkUDFromClient(name, id, pwd)
            }
        }.apply {
                layoutParams = LinearLayout.LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        addView(etUserName)
        addView(etUserId)
        addView(etPassword)
        addView(buttonLayout)
    }

    private fun checkUDFromClient(UserName: String, UserId: String, UserPWD: String) {
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
        val progressDialog: AlertDialog = MaterialAlertDialogBuilder(context)
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
                    text = "正在为您创建, 请稍等..."
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
                        val error: String = "账户 " + UserId + " 已存在, 请勿重复创建"
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(error: String) {
                    (context as? Activity)?.runOnUiThread {
                        createUDFromClient(UserName, UserId, UserPWD, message, progressDialog)
                    }
                }
            })
    }

    private fun createUDFromClient(UserName: String, UserId: String, UserPWD: String, message: String?, progressDialog: AlertDialog) {
        client.uploadData("UserData", UserId, message,
            object : Client.ResultCallback {
                override fun onSuccess(content: String) {
                    (context as? Activity)?.runOnUiThread {
                        progressDialog.dismiss()
                        onRegisterSuccess(UserName, UserId, UserPWD)
                        SBClient.createUser(UserId, UserName)
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

    private fun createButton(text: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor("#2196F3".toColorInt())
            rippleColor = ColorStateList.valueOf("#66FFFFFF".toColorInt())
            cornerRadius = 4.dp
            setOnClickListener { onClick() }
        }
    }

    private fun showError(fieldType: Int, message: String): Nothing? {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        when (fieldType) {
            1 -> etUserName.error = message
            2 -> etUserId.error = message
            3 -> etPassword.error = message
        }
        return null
    }
}