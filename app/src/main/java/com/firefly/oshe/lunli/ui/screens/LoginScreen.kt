package com.firefly.oshe.lunli.ui.screens

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserDataPref
import com.firefly.oshe.lunli.client.Client
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.R

import org.json.JSONObject
import org.json.JSONException

class LoginScreen(
    context: Context,
    private val userData: UserData,
    private val userList: MutableList<String>,
    private val userDataPref: UserDataPref,
    // private val userMessagePref: UserMessagePref,
    private val onLoginSuccess: (String) -> Unit,
    private val onRegisterClick: () -> Unit
) : LinearLayout(context) {

    private lateinit var client: Client
    private lateinit var spinner: Spinner
    private lateinit var tilUserId: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(16.dp, 0, 16.dp, 0)
        setupViews()
    }

    private fun setupViews() {
        val userSelectLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val buttonLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 8.dp
            }
        }

        tilUserId = createEditText("用户ID") {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }.apply {
            layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
        }

        tilPassword = createEditText("密码") {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }.apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        spinner = Spinner(context).apply {
            adapter = object : ArrayAdapter<String>(
                context, 
                android.R.layout.simple_spinner_item,
                userList
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return View(context).apply {
                        visibility = INVISIBLE
                        layoutParams = ViewGroup.LayoutParams(0, 0)
                    }
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val linearLayout = LinearLayout(context).apply {
                        orientation = HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
                        }
                    }

                    val tvUserId = TextView(context).apply {
                        text = userList[position]
                        layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                        textSize = 16f
                    }
                    tvUserId.setOnClickListener {
                        spinner.setSelection(position)
                        tilPassword.error = null
                    }

                    val btnDelete = ImageButton(context).apply {
                        setImageResource(android.R.drawable.ic_menu_delete)
                        layoutParams = LayoutParams(24.dp, 24.dp)
                        background = context.getDrawable(android.R.drawable.list_selector_background)
                        setOnClickListener {
                            val userId = userList[position]
                            MaterialAlertDialogBuilder(context)
                                .setTitle("删除账户")
                                .setMessage("确定删除本地账户 $userId 吗？")
                                .setPositiveButton("删除") { _, _ ->
                                    userDataPref.deleteUser(userId)
                                    // userMessagePref.deleteMessage(userId)
                                    userList.remove(userId)
                                    notifyDataSetChanged()

                                    if (userId == tilUserId.editText?.text?.toString()) {
                                        tilUserId.editText?.setText("")
                                        tilPassword.editText?.setText("")
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                    }
                    linearLayout.addView(tvUserId)
                    linearLayout.addView(btnDelete)
                    return linearLayout
                }
            }.apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            layoutParams = LayoutParams(32.dp, 32.dp).apply {
                marginStart = 4.dp
                gravity = Gravity.CENTER_VERTICAL
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedUserId = userList[position]
                    tilUserId.editText!!.setText(selectedUserId)
                    userDataPref.getUser(selectedUserId)?.let { user ->
                        if (user.hasPasswordError) {
                            tilPassword.editText!!.setText("")
                            tilPassword.error = "该账户需要重新输入密码"
                        } else {
                            tilPassword.editText!!.setText(user.password)
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        createButton("注册") {
            onRegisterClick()
        }.apply {
            layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        createButton("登陆") {
            val inputUserId = tilUserId.editText!!.text.toString()
            val inputPassword = tilPassword.editText!!.text.toString()

            tilUserId.error = null
            tilPassword.error = null

            userDataPref.getUser(inputUserId)?.let { user ->
                if (user.hasPasswordError && inputPassword.isEmpty()) {
                    tilPassword.error = "请重新输入密码"
                    return@createButton
                }
            }

            when {
                inputUserId.length !in 7..8 -> {
                    tilUserId.error = "用户ID长度应为7位或8位"
                    return@createButton
                }
                else -> {}
            }

            val progressDialog = MaterialAlertDialogBuilder(context)
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
                        text = "正在登陆, 请稍等..."
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(context, R.color.tan))
                    })
                })
                .setCancelable(false)
                .create()
            progressDialog.show()

            client = Client(context)
            client.getData("UserData", inputUserId,
                object : Client.ResultCallback {
                    override fun onSuccess(content: String) {
                        (context as? Activity)?.runOnUiThread {
                            progressDialog.dismiss()
                            isLoginToUser(content, inputUserId, inputPassword)
                        }
                    }

                    override fun onFailure(error: String) {
                        (context as? Activity)?.runOnUiThread {
                            progressDialog.dismiss()
                            var Err: String = error
                            if (error == "UserFile") Err = "未找到该账户, 请检查输入或注册账户"
                            Toast.makeText(context, Err, Toast.LENGTH_SHORT).show()
                        }
                    }
                })

        }.apply {
            layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 4.dp
            }
        }.also { buttonLayout.addView(it) }

        userSelectLayout.addView(tilUserId)
        userSelectLayout.addView(spinner)
        addView(userSelectLayout)
        addView(tilPassword)
        addView(buttonLayout)
    }

    private fun isLoginToUser(IUserData: String, inputUserId:String, inputPassword:String) {
        try {
            val rootObject = JSONObject(IUserData)
            rootObject.keys().forEach { userId: String ->
            rootObject.getJSONObject(userId)?.let { user ->
                    if (inputPassword == user.optString("password")) {
                        userDataPref.deleteUser(inputUserId)
                        // userMessagePref.deleteMessage(inputUserId)
                        val data = UserData(
                            userId = user.optString("userId"),
                            userName = user.optString("userName"),
                            password = user.optString("password")
                        )
                        userDataPref.saveUser(data)
                        UserDataPref.setLastUser(context, inputUserId)
                        onLoginSuccess(inputUserId)
                    } else {
                        tilPassword.error = "验证失败，请检查输入"
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor("#2196F3".toColorInt())
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeColor = ColorStateList.valueOf("#2196F3".toColorInt())
            strokeWidth = 2.dp
            cornerRadius = 8.dp
            elevation = 0.dp.toFloat()
            stateListAnimator = null

            setOnClickListener { onClick() }
            layoutParams = LayoutParams(WRAP_CONTENT, 48.dp)
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
}