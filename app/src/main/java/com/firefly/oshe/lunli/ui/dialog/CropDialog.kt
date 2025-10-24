package com.firefly.oshe.lunli.ui.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.*
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.utils.CropUtils
import com.google.android.material.button.MaterialButton

class CropDialog(
    private val context: Context
) {
    private lateinit var popupWindow: PopupWindow
    private lateinit var cropView: CropUtils
    private var onCropResult: ((Bitmap?) -> Unit)? = null

    fun showCropDialog(bitmap: Bitmap, onCropResult: (Bitmap?) -> Unit = {}) {
        this.onCropResult = onCropResult
        val rootView = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        cropView = CropUtils(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setImage(bitmap)
        }

        val buttonLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(16, 16, 16, 16)
            }
        }

        createButton(
            "取消",
            R.color.red
        ) {
            popupWindow.dismiss()
        }.apply {
            layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 8
            }
        }.also { buttonLayout.addView(it) }

        createButton(
            "确定",
            R.color.light_blue
        ) {
            val croppedBitmap = cropView.cropImage()
            onCropResult(croppedBitmap)
            popupWindow.dismiss()
        }.apply {
            layoutParams = LayoutParams(0, 48.dp, 1f)
        }.also {
            buttonLayout.addView(it)
        }


        rootView.addView(
            cropView,
            LayoutParams(
                MATCH_PARENT,
                0,
                1f
            )
        )
        rootView.addView(buttonLayout)

        popupWindow = PopupWindow(
            rootView,
            MATCH_PARENT,
            MATCH_PARENT,
            true
        ).apply {
            isOutsideTouchable = false
            elevation = 20f
        }
    }

    fun showAtLocation(view: View) {
        if (::popupWindow.isInitialized) {
            popupWindow.showAtLocation(view, CENTER, 0, 0)
        }
    }

    fun dismiss() {
        if (::popupWindow.isInitialized) {
            popupWindow.dismiss()
        }
    }

    private fun createButton(text: String, color: Int, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, color)))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, color))
            strokeWidth = 2.dp
            cornerRadius = 8.dp
            elevation = 0.dp.toFloat()
            stateListAnimator = null

            setOnClickListener { onClick() }
            layoutParams = LayoutParams(WRAP_CONTENT, 48.dp)
        }
    }
}