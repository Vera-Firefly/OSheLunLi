package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures

import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import androidx.core.view.size
import com.firefly.oshe.lunli.data.UserData
import com.firefly.oshe.lunli.data.UserInformation
import com.firefly.oshe.lunli.dp
import com.google.android.material.textview.MaterialTextView

class Community(
    private val context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val userInformation: UserInformation,

) {
    private lateinit var mainView: LinearLayout

    private var cePage: LinearLayout? = null

    fun createView(): LinearLayout {
        mainView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

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

            MaterialTextView(context).apply {
                text = "敬请期待"
                textSize = 58f
            }.also { addView(it) }

        }
        addView(cePage)
    }

}
