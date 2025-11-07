package com.firefly.oshe.lunli.ui.screens.MainScreenFeatures.ChatRoomFeatures

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_CROP
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firefly.oshe.lunli.R
import com.firefly.oshe.lunli.data.ChatRoom.Message
import com.firefly.oshe.lunli.dp
import com.firefly.oshe.lunli.MarkdownRenderer
import com.firefly.oshe.lunli.utils.Ciallo
import com.firefly.oshe.lunli.utils.ImageUtils
import com.google.android.material.imageview.ShapeableImageView
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.VERTICAL
import com.firefly.oshe.lunli.Tools.ShowToast
import com.firefly.oshe.lunli.ui.dialog.CropDialog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapterView {

    private val messages = mutableListOf<Message>()
    private var adapter: ChatAdapter? = null
    private val base64 = Ciallo().ciallo // 注意: 禁止尝试打开这个类, 除非你想你的IDE爆炸!!!
    private val systemMessages = listOf(
        Message(
            "1",
            "系统",
            "NULL",
            content = """
                ## 欢迎使用Markdown聊天室
                - 支持层级显示
                - 支持**粗体**和*斜体*
                - 支持[链接](https://example.com)
                - 支持图片显示(需要图床)
                ![]($base64)
                - 支持表格
                ### 表格示例:
                | A | B |
                |---------|---------|
                | 1 | ○ |
                | 2 | ● |
                | 3 | ♥ |
                - 支持区块引用与嵌套引用
                > 区块引用
                >> 嵌套引用
                - 支持代码显示(注: 代码高亮显示未实现)
                `行内代码`

                ```python
                # 代码块
                def hello():
                    print("Hello World!")
                ```
                - 其它类型未实现, 后继会逐一实现
                """.trimIndent()
        ))

    fun createAdapter(): ChatAdapter {
        adapter = ChatAdapter()
        return adapter!!
    }

    fun addMessage(message: Message) {
        adapter?.addMessage(message)
    }

    fun getMessages(): List<Message> = messages.toList()

    private fun ImagePreview(context: Context, bitmap: Bitmap, view: View) {
        val cropDialog = CropDialog(context)
        cropDialog.setButtonText(confirm = "保存")
        cropDialog.showCropDialog(bitmap, 2) {
            saveImageToPictures(context, bitmap)
        }
        cropDialog.showAtLocation(view)
    }

    private fun saveImageToPictures(context: Context, bitmap: Bitmap) {
        Thread {
            try {
                val picturesDir = File("/sdcard/Pictures/OSheLunLi")
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "OSheLunLi.${timeStamp}.jpg"
                val imageFile = File(picturesDir, fileName)

                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                }

                Handler(Looper.getMainLooper()).post {
                    context.ShowToast("图片已保存到: ${imageFile.absolutePath}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    context.ShowToast("保存失败: ${e.message}")
                }
            }
        }.start()
    }

    private enum class ContentType {
        IMAGE_BASE64,
        IMAGE_URL,
        TEXT
    }

    abstract inner class BaseChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        abstract fun addMessage(message: Message)
    }

    inner class ChatAdapter : BaseChatAdapter() {

        override fun addMessage(message: Message) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }

        fun clearMessages() {
            messages.clear()
            messages.addAll(systemMessages)
            notifyDataSetChanged()
        }

        fun addMessageIfNotExists(message: Message) {
            if (messages.none { it.id == message.id} ) {
                addMessage(message)
            }
        }

        fun getMessages(): List<Message> = messages.toList()

        private fun detectContentType(content: String): ContentType {
            return when {
                isPrefixedBase64Image(content) -> ContentType.IMAGE_BASE64
                isPureBase64Image(content) -> ContentType.IMAGE_BASE64
                isImageUrl(content) -> ContentType.IMAGE_URL
                else -> ContentType.TEXT
            }
        }

        private fun isPureBase64Image(content: String): Boolean {
            val trimmedContent = content.trim()

            if (trimmedContent.length < 100) return false

            val isJpeg = trimmedContent.startsWith("/9j/")
            val isPng = trimmedContent.startsWith("iVBORw0KGgo")
            val isGif = trimmedContent.startsWith("R0lGOD")
            val isWebP = trimmedContent.startsWith("UklGR")

            return (isJpeg || isPng || isGif || isWebP)
        }

        private fun isPrefixedBase64Image(content: String): Boolean {
            return content.startsWith("data:image/") &&
                    content.contains("base64,") &&
                    content.length > 100
        }

        private fun isImageUrl(content: String): Boolean {
            val trimmedContent = content.trim()
            return (trimmedContent.startsWith("http://") || trimmedContent.startsWith("https://")) &&
                    (trimmedContent.endsWith(".jpg") ||
                            trimmedContent.endsWith(".jpeg") ||
                            trimmedContent.endsWith(".png") ||
                            trimmedContent.endsWith(".gif") ||
                            trimmedContent.endsWith(".webp") ||
                            trimmedContent.contains(".jpg?") ||
                            trimmedContent.contains(".jpeg?") ||
                            trimmedContent.contains(".png?") ||
                            trimmedContent.contains(".gif?"))
        }

        private fun renderBase64Image(container: FrameLayout, content: String) {
            try {
                val bitmap = ImageUtils.base64ToBitmap(content)

                val imageView = ShapeableImageView(container.context).apply {
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    scaleType = CENTER_CROP
                    setImageBitmap(bitmap)
                    adjustViewBounds = true

                    setOnClickListener {
                        ImagePreview(context, bitmap!!, this)
                    }
                }

                container.addView(imageView)
            } catch (e: Exception) {
                // 图片加载失败时不显示任何内容?
            }
        }

        private fun renderImageUrl(container: FrameLayout, imageUrl: String) {
            val imageView = ShapeableImageView(container.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                scaleType = CENTER_CROP
                adjustViewBounds = true

                Glide.with(context)
                    .load(imageUrl)
                    .into(this)

                setOnClickListener {
                    // TODO
                }
            }

            container.addView(imageView)
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val rootLayout = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setPadding(0, 8.dp, 0, 8.dp)
                }
                orientation = HORIZONTAL
            }

            val avatar = ShapeableImageView(parent.context).apply {
                layoutParams = LayoutParams(36.dp, 36.dp).apply {
                    setMargins(0, 0, 4.dp, 0)
                }
                scaleType = CENTER_CROP
                id = R.id.iv_avatar
            }
            rootLayout.addView(avatar)

            val contentArea = LinearLayout(parent.context).apply {
                layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
                orientation = VERTICAL
            }

            val senderName = TextView(parent.context).apply {
                layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                textSize = 12f
                setTextColor(Color.GRAY)
                id = R.id.tv_sender
            }
            contentArea.addView(senderName)

            val contentContainer = FrameLayout(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 4.dp, 0, 0)
                }
                id = R.id.fl_content
            }
            contentArea.addView(contentContainer)

            rootLayout.addView(contentArea)

            return object : RecyclerView.ViewHolder(rootLayout) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            val rootView = holder.itemView

            val avatar = rootView.findViewById<ImageView>(R.id.iv_avatar)
            val senderName = rootView.findViewById<TextView>(R.id.tv_sender)
            val contentContainer = rootView.findViewById<FrameLayout>(R.id.fl_content)

            senderName.text = message.sender
            when {
                message.sender == "系统" && message.id == "1" -> {
                    avatar.setImageResource(android.R.drawable.ic_menu_info_details)
                }
                message.senderImage != "NULL" -> {
                    val image = ImageUtils.base64ToBitmap(message.senderImage)
                    avatar.setImageBitmap(image)
                    avatar.setOnClickListener {
                        ImagePreview(contentContainer.context, image!!, rootView)
                    }
                }
                else -> {
                    avatar.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            contentContainer.removeAllViews()

            when (detectContentType(message.content)) {
                ContentType.IMAGE_BASE64 -> {
                    renderBase64Image(contentContainer, message.content)
                }
                ContentType.IMAGE_URL -> {
                    renderImageUrl(contentContainer, message.content)
                }
                else -> {
                    renderText(contentContainer, message.content)
                }
            }
        }

        override fun getItemCount() = messages.size
    }
}