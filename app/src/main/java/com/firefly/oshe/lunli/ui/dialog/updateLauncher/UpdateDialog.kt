package com.firefly.oshe.lunli.ui.dialog.updateLauncher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.popup.PopupManager

class UpdateDialog {

    fun OnUpdateDialog(context: Context, call: (Int) -> Unit = {}) {
        val interaction = Interaction(context)
        val view: View = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 10, 10, 10)
            }

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                this.cornerRadius = 8f
            }
        }
        PopupManager.show(view)
    }

    fun InstallDialog(context: Context, path: String, call: (Int) -> Unit = {}) {
        val interaction = Interaction(context)
        val view: View = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 10, 10, 10)
            }

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                this.cornerRadius = 8f
            }

            TextView(context).apply {
                text = "安装更新"
                textSize = 18f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
            }.also { addView(it) }

            TextView(context).apply {
                text = "有新版本已经下载道${path}, 你要安装它吗?"
                textSize = 14f
                isSingleLine = false
                setTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(24, 10, 24, 10)
                }
            }.also { addView(it) }

            val buttonLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(8.dp, 0, 8.dp, 4.dp)
                }
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            interaction.createButton("取消", R.color.red) {
                PopupManager.dismiss()
                call(0)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }.also { buttonLayout.addView(it) }

            interaction.createButton("重新下载", R.color.red) {
                PopupManager.dismiss()
                call(1)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }.also { buttonLayout.addView(it) }

            interaction.createButton("安装", R.color.light_blue) {
                PopupManager.dismiss()
                call(2)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }.also { buttonLayout.addView(it) }

            addView(buttonLayout)

        }
        PopupManager.show(view, false)
    }

}