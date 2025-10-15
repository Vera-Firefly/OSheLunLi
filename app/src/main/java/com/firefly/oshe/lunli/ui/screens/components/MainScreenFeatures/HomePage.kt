package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.Context
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import android.widget.TextView

import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserImage
import com.firefly.oshe.lunli.data.UserInformation

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

            val text = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    WRAP_CONTENT,
                    WRAP_CONTENT
                )
                text = "测试"
            }

            addView(text)
        }

        addView(homePage)
    }
}
