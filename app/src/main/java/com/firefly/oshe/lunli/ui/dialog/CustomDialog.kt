package com.firefly.oshe.lunli.ui.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.View
import android.widget.*

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomDialog private constructor(
    private val context: Context,
    private val title: String?,
    private val message: String?,
    private val view: View?,
    private val cancelable: Boolean,
    private val cancelListener: () -> Unit,
    private val confirmListener: ((View) -> Boolean)?
) {
    private lateinit var builder: MaterialAlertDialogBuilder

    init {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val buttonParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)

        val builder = MaterialAlertDialogBuilder(context)

        title?.let { builder.setTitle(it) }

        message?.let { builder.setMessage(it) }

        view?.let { layout.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)) }

        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val cancelButton = Button(context).apply {
            text = "取消"
        }
        val confirmButton = Button(context).apply {
            text = "确认"
        }

        buttonLayout.addView(cancelButton, buttonParams)
        buttonLayout.addView(confirmButton, buttonParams)
        layout.addView(buttonLayout)

        builder.setView(layout)
        builder.setCancelable(cancelable)

        cancelButton.setOnClickListener { builder.create().dismiss() }
        confirmButton.setOnClickListener {
            view?.let {
                val isValid = confirmListener?.invoke(it) ?: true
                if (isValid) builder.create().dismiss()
            } ?: builder.create().dismiss()
        }
    }

    fun show() {
        builder.show()
    }

    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var view: View? = null
        private var cancelable: Boolean = true
        private var cancelListener: () -> Unit = {}
        private var confirmListener: ((View) -> Boolean)? = null

        fun setTitle(title: String) = apply { this.title = title }
        fun setMessage(message: String) = apply { this.message = message }
        fun setView(view: View) = apply { this.view = view }
        fun setCancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }
        fun setCancelListener(listener: () -> Unit) = apply { this.cancelListener = listener }
        fun setConfirmListener(listener: (View) -> Boolean) = apply { this.confirmListener = listener }

        fun build(): CustomDialog {
            return CustomDialog(context, title, message, view, cancelable, cancelListener, confirmListener)
        }
    }
}