package com.firefly.oshe.lunli.ui.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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

    private var cancelButtonText = "取消"
    private var confirmButtonText = "确认"

    fun setButtonText(cancel: String = "取消", confirm: String = "确认"): CropDialog {
        this.cancelButtonText = cancel
        this.confirmButtonText = confirm
        return this
    }

    fun showCropDialog(bitmap: Bitmap, cropType: Int = -1, onCropResult: (Bitmap?) -> Unit = {}) {
        this.onCropResult = onCropResult
        val rootView = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        cropView = CropUtils(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setImage(bitmap)
            if (cropType != -1) {
                setCropType(getCropTypeFromInt(cropType))
            }
        }

        val buttonLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(16, 16, 16, 16)
            }
        }

        if (cropType == -1) {
            val typeSelectorLayout = createCropTypeSelector()
            rootView.addView(typeSelectorLayout)
        }

        createButton(
            cancelButtonText,
            R.color.red
        ) {
            popupWindow.dismiss()
        }.apply {
            layoutParams = LayoutParams(0, 48.dp, 1f).apply {
                marginEnd = 8
            }
        }.also { buttonLayout.addView(it) }

        createButton(
            confirmButtonText,
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

    private fun createCropTypeSelector(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(16, 16, 16, 8)
            }
            gravity = CENTER

            createTypeButton("正方形", 0).also { addView(it) }

            createTypeButton("屏幕比例", 1).also { addView(it) }

            createTypeButton("全屏预览", 2).also { addView(it) }
        }
    }

    private fun createTypeButton(text: String, cropType: Int): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_blue)))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_blue))
            strokeWidth = 1.dp
            cornerRadius = 6.dp
            elevation = 0.dp.toFloat()
            stateListAnimator = null
            layoutParams = LayoutParams(WRAP_CONTENT, 36.dp).apply {
                marginEnd = 8
            }

            setOnClickListener {
                cropView.setCropType(getCropTypeFromInt(cropType))
                updateTypeButtonStates(this, cropType)
            }
        }
    }

    private fun updateTypeButtonStates(selectedButton: MaterialButton, selectedType: Int) {
        val parent = selectedButton.parent as? LinearLayout
        parent?.let { layout ->
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i) as? MaterialButton
                child?.let { button ->
                    val isSelected = button == selectedButton
                    val colorRes = if (isSelected) R.color.white else R.color.light_blue
                    val bgColorRes = if (isSelected) R.color.light_blue else android.R.color.transparent

                    button.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
                    button.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, bgColorRes)
                    )
                    button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_blue))
                }
            }
        }
    }

    private fun getCropTypeFromInt(cropType: Int): CropUtils.CropType {
        return when (cropType) {
            0 -> CropUtils.CropType.SQUARE
            1 -> CropUtils.CropType.SCREEN_RATIO
            2 -> CropUtils.CropType.FULL_SCREEN
            else -> CropUtils.CropType.SQUARE
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