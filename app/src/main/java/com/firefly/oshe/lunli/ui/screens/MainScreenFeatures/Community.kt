package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures

import android.content.Context
import android.view.Gravity.CENTER
import android.view.Gravity.CENTER_VERTICAL
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserInformation
import com.google.android.material.textview.MaterialTextView

class Community(
    private val context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val userInformation: UserInformation,

) {
    private lateinit var mainView: LinearLayout

    private var cePage: LinearLayout? = null
    private var onItemClickCount = 0
    private var lastResetTime = System.currentTimeMillis()

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        gravity = CENTER
            createCEView()
        }
        return mainView
    }

    private fun LinearLayout.createCEView() {
        cePage = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 0)
            }
            orientation = VERTICAL
            gravity = CENTER

            MaterialTextView(context).apply {
                text = "\"敬请期待\""
                textSize = 58f
                gravity = CENTER

                setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    val timeZZ = currentTime - lastResetTime
                    if (timeZZ > 5000) {
                        onItemClickCount = 0
                        lastResetTime = currentTime
                    }

                    onItemClickCount++

                    if (onItemClickCount >= 100) {
                        setText("Ciallo～(∠・ω< )⌒☆")
                        setTextSize(36f)
                    }

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

                    context.ShowToast(displayText)
                }
            }.also { addView(it) }

        }
        addView(cePage)
    }

}
