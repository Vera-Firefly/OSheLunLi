package com.firefly.oshe.lunli.ui.dialog.announcement

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.data.Announcement
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.ui.component.Interaction
import com.firefly.oshe.lunli.ui.popup.PopupManager
import com.firefly.oshe.lunli.ui.screens.components.AnnAdapterView

class AnnDialog(private val context: Context) {

    fun onAnnDialog(
        anns: List<Announcement>,
        call: (Int) -> Unit = {}
    ) {
        val interaction = Interaction(context)
        val view: View = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(10.dp, 10.dp, 10.dp, 10.dp)
            }

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                this.cornerRadius = 8f
            }

            TextView(context).apply {
                text = "公告"
                textSize = 18f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 8.dp, 0, 8.dp)
                }
            }.also { addView(it) }

            val recyclerView = RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 450.dp).apply {
                    setMargins(8.dp, 0, 8.dp, 0)
                }

                layoutManager = LinearLayoutManager(context)

                addItemDecoration(
                    DividerItemDecoration(
                        context,
                        LinearLayoutManager.VERTICAL
                    )
                )
            }

            val adapterView = AnnAdapterView()
            val adapter = adapterView.createAdapter()
            recyclerView.adapter = adapter

            anns.forEach { ann ->
                adapterView.addAnn(ann)
            }

            addView(recyclerView)

            val buttonLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                }
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            interaction.createButton("关闭", R.color.red_500) {
                PopupManager.dismiss()
                call(0)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginEnd = 4.dp
                }
            }.also { buttonLayout.addView(it) }

            interaction.createButton("确认", R.color.gray) {
                PopupManager.dismiss()
                call(1)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }.also { buttonLayout.addView(it) }

            addView(buttonLayout)
        }

        PopupManager.show(view)
    }

}