package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.client.Client

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserImage
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

// 用户个人主页
class HomePage(
    private val context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val userInformation: UserInformation,
) {

    private lateinit var mainView: LinearLayout
    private lateinit var client: Client

    private final var pageRecyclerView: RecyclerView? = null
    private final var pageAdapter: BaseUserHomeAdapter? = null

    private var homePage: LinearLayout? = null

    private var onItemClickCount = 0
    private var lastResetTime = System.currentTimeMillis()
    private var previousToast: Toast? = null

    fun interface onSignOutListener {
        fun onSignOut()
    }

    private var signOutListener: onSignOutListener? = null

    fun setOnSignOutListener(listener: onSignOutListener) {
        this.signOutListener = listener
    }

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )

            createUserHomePageView()
        }
        return mainView
    }

    private fun LinearLayout.createUserHomePageView() {
        homePage = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL

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
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8.dp, 0, 8.dp)
            }

            createCMLView(
                R.drawable.user,
                userData.userName,
                "NONE",
                userData.userId
            ).also {
                root.addView(it)
            }

            val buttonLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 8.dp
                }
            }

            createButton("退出登录") {
                signOutListener?.onSignOut()
            }.apply {
                layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                    marginStart = 4.dp
                    marginEnd = 4.dp
                }
            }.also { buttonLayout.addView(it) }

            root.addView(buttonLayout)

            return object : RecyclerView.ViewHolder(root) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // val rootView = holder.itemView
        }

        override fun getItemCount() = information.size
    }

    private fun createCMLView(
        iconResId: Int,
        name: String,
        userMessage: String,
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
                    textSize = 24f
                    setTextColor(ContextCompat .getColor(context, R.color.primary_text_color))
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2.dp
                    }
                    addView(this)
                }
                TextView(context).apply {
                    text = "$userMessage"
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
                previousToast?.cancel()

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

                previousToast = Toast.makeText(context, displayText, Toast.LENGTH_SHORT)
                previousToast?.show()
                // TODO:
                /*
                if (userMessage.isNotBlank() && userMessage != "未填写") {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(userMessage))
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
               */
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

    private fun createButton(text: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.red)
            )
            cornerRadius = 8.dp
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(WRAP_CONTENT, 48.dp)
        }
    }

}
