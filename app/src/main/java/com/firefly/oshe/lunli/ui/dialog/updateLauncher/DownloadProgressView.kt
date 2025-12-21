package com.firefly.oshe.lunli.ui.dialog.updateLauncher

import android.R
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.firefly.oshe.lunli.dp
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
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(10.dp, 10.dp, 10.dp, 10.dp)
        }

        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            this.cornerRadius = 8f
        }

        initializeViews()
    }

    private fun initializeViews() {
        interaction = Interaction(context)

        titleTextView = TextView(context).apply {
            text = "下载中..."
            textSize = 18f
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8.dp, 0, 8.dp)
            }
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
        }
        addView(titleTextView)

        val progressView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(8.dp, 0, 8.dp, 0)
            }
        }

        progressBar = ProgressBar(context, null, R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginEnd = 8.dp
            }
            max = 100
            progress = 0
        }
        progressView.addView(progressBar)

        progressTextView = TextView(context).apply {
            text = "0%"
            textSize = 14f
            setTextColor(Color.GRAY)
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        progressView.addView(progressTextView)

        addView(progressView)

        val buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            }
            gravity = Gravity.END
        }

        interaction.createButton("取消", com.firefly.oshe.lunli.R.color.red) {
            PopupManager.dismiss()
            isCancelled = true
            listener?.onCancel()
        }.apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
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
}