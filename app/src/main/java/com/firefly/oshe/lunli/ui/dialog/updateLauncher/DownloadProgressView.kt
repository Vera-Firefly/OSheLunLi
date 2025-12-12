package com.firefly.oshe.lunli.ui.dialog.updateLauncher

import android.R
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.popup.PopupManager

class DownloadProgressView(context: Context) : LinearLayout(context) {

    interface OnDownloadListener {
        fun onCancel()
        fun onProgress(progress: Int)
    }

    private var listener: OnDownloadListener? = null
    private var isCancelled = false

    private lateinit var titleTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var interaction: Interaction

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams( dpToPx(300), LayoutParams.WRAP_CONTENT)

        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            this.cornerRadius = 8f
        }

        setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        initializeViews()
    }

    private fun initializeViews() {
        interaction = Interaction(context)

        titleTextView = TextView(context).apply {
            text = "下载中..."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dpToPx(12))
        }
        addView(titleTextView)

        progressBar = ProgressBar(context, null, R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(8)
            ).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(8))
            }
            max = 100
            progress = 0
        }
        addView(progressBar)

        progressTextView = TextView(context).apply {
            text = "0%"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }
        addView(progressTextView)

        val buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            gravity = Gravity.END
        }

        interaction.createButton("取消", com.firefly.oshe.lunli.R.color.red) {
            PopupManager.dismiss()
            isCancelled = true
            listener?.onCancel()
        }.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f)
        }.also { buttonContainer.addView(it) }

        addView(buttonContainer)
    }

    fun setTitle(title: String) {
        uiHandler.post {
            titleTextView.text = title
        }
    }

    fun setProgress(progress: Int) {
        uiHandler.post {
            progressBar.progress = progress
            progressTextView.text = "$progress%"
        }
        listener?.onProgress(progress)
    }

    fun setOnDownloadListener(listener: OnDownloadListener) {
        this.listener = listener
    }

    fun onDismiss() {
        PopupManager.dismiss()
    }

    fun isCancelled(): Boolean = isCancelled

    fun reset() {
        isCancelled = false
        uiHandler.post {
            progressBar.progress = 0
            progressTextView.text = "0%"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}