package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.R

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserImage
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.dp
import com.google.android.material.button.MaterialButton

// 用户个人主页
class HomePage(
    private val context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val userInformation: UserInformation,
    private val userImage: UserImage
) {

    private lateinit var mainView: LinearLayout

    private var homePage: LinearLayout? = null

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
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = LinearLayout.VERTICAL

            val buttonLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 8.dp
                }
            }

            val exitButton = createButton("退出登录") {
                signOutListener?.onSignOut()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, 48.dp, 1f).apply {
                    marginEnd = 4.dp
                }
            }.also { buttonLayout.addView(it) }

            addView(buttonLayout)
        }

        addView(homePage)
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
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, 48.dp)
        }
    }

}
