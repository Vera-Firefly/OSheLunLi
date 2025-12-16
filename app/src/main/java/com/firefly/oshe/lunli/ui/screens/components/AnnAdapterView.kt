package com.firefly.oshe.lunli.ui.screens.components

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firefly.oshe.lunli.MarkdownRenderer
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.data.Announcement
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.utils.Iso8601Converter

class AnnAdapterView {
    private val anns = mutableListOf<Announcement>()
    private var adapter: AnnAdapter? = null

    fun createAdapter(): AnnAdapter {
        adapter = AnnAdapter()
        return adapter!!
    }

    fun addAnn(ann: Announcement) {
        adapter?.addAnn(ann)
    }

    abstract inner class BaseAnnAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        abstract fun addAnn(ann : Announcement)
    }

    inner class AnnAdapter : BaseAnnAdapter() {
        override fun addAnn(ann: Announcement) {
            anns.add(ann)
            notifyItemInserted(anns.size - 1)
        }

        private fun renderText(container: FrameLayout, content: String) {
            val textView = TextView(container.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                movementMethod = LinkMovementMethod.getInstance()

                setTextColor(Color.BLACK)
                textSize = 14f
            }

            try {
                MarkdownRenderer.render(textView, content.replace("\n", "  \n"))
            } catch (e: IllegalStateException) {
                textView.text = content
            } catch (e: Exception) {
                textView.text = content
            }

            container.addView(textView)
        }

        private fun formatMessageTime(isoTime: String): String {
            return try {
                val timestamp = Iso8601Converter.parseUtcZeroOffsetFormat(isoTime)

                val instant = java.time.Instant.ofEpochMilli(timestamp)
                val dateTime = instant.atZone(java.time.ZoneId.of("Asia/Shanghai"))

                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                dateTime.format(formatter)
            } catch (e: Exception) {
                " "
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val rootLayout = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(0, 8.dp, 0, 8.dp)
                }
                orientation = HORIZONTAL
            }

            val contentArea = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                orientation = VERTICAL
            }

            val contentContainer = FrameLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 4.dp, 0, 0)
                }
                id = R.id.ann_body
            }
            contentArea.addView(contentContainer)

            val timeText = TextView(parent.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(0, 2.dp, 0, 0)
                }
                textSize = 10f
                setTextColor(Color.argb(128, 128, 128, 128))
                id = R.id.ann_time
            }
            contentArea.addView(timeText)

            rootLayout.addView(contentArea)

            return object : RecyclerView.ViewHolder(rootLayout) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val ann = anns[position]
            val rootView = holder.itemView

            val contentContainer = rootView.findViewById<FrameLayout>(R.id.ann_body)
            val timeText = rootView.findViewById<TextView>(R.id.ann_time)

            timeText.text = formatMessageTime(ann.created_at)

            contentContainer.removeAllViews()

            renderText(contentContainer, ann.body)
        }

        override fun getItemCount() = anns.size
    }
}