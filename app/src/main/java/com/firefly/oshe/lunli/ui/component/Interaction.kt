package com.firefly.oshe.lunli.ui.component

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.GlobalInterface.imageHelper.ImageSelectionManager
import com.firefly.oshe.lunli.GlobalInterface.imageHelper.SimpleImageCallback
import com.firefly.oshe.lunli.MainActivity
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.utils.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Interaction(private val context: Context) {

    fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    fun createButton(text: String, color: Int, onClick: () -> Unit): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, color)))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, color))
            strokeWidth = 1.dp
            cornerRadius = 8.dp
            elevation = 0.dp.toFloat()
            stateListAnimator = null

            /* 事实上我也不知道为什么要写这么复杂, 先留着
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        backgroundTintList = ColorStateList.valueOf(Color.argb(30, 128, 128, 128))
                        v.animate()
                            .scaleX(0.99f)
                            .scaleY(0.98f)
                            .setDuration(60)
                            .withStartAction {
                                v.pivotX = v.width / 2f
                                v.pivotY = v.height / 2f
                            }
                            .start()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                        if (isAttachedToWindow) performClick()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                        true
                    }
                    else -> false
                }
            }
            */

            setOnClickListener {
                animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction {
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                onClick()
            }
            layoutParams = LayoutParams(WRAP_CONTENT, 48.dp)
        }
    }

    fun createEditText(
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

    fun ImageRequestCallBack(requester: String, callBack: (Bitmap) -> Unit = {}) {
        ImageSelectionManager.setCallback(requester, object : SimpleImageCallback {
            override fun onImageSelected(uri: Uri) {
                val bitmap = ImageUtils.bitmapFromUri(context, uri)
                bitmap?.let { callBack(it) }
            }

            override fun onSelectionCancelled() {
                onDestroy()
            }

            override fun onSelectionFailed(error: String) {
                context.ShowToast("图片选择失败")
                onDestroy()
            }
        })
        if (context is MainActivity) {
            context.startImageSelection()
        }
    }

    private fun onDestroy() {
        ImageSelectionManager.clearCallback()
    }

}