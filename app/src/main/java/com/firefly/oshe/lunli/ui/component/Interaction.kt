package com.firefly.oshe.lunli.ui.component

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Interaction(private val context: Context) {

    fun createButton(text: String, color: Int, onClick: () -> Unit): MaterialButton {
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

}