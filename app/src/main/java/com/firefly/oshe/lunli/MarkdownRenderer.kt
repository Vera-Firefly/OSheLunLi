package com.firefly.oshe.lunli

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.widget.TextView
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import io.noties.markwon.*
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.ImageSize
import io.noties.markwon.image.ImageSizeResolver
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.html.HtmlPlugin
import org.commonmark.node.*

object MarkdownRenderer {

    private var markwon: Markwon? = null

    @WorkerThread
    fun init(context: Context) {
        if (markwon != null) return

        val glide: RequestManager = Glide.with(context)

        markwon = Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(glide))
            .usePlugin(CorePlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.imageSizeResolver(object : ImageSizeResolver() {
                        override fun resolveImageSize(drawable: AsyncDrawable): Rect {
                            val imageSize: ImageSize? = drawable.imageSize
                            val intrinsicWidth = drawable.intrinsicWidth
                            val intrinsicHeight = drawable.intrinsicHeight
                            val displayMetrics = Resources.getSystem().displayMetrics
                            val screenWidth = displayMetrics.widthPixels
                            val margin = 16.dp * 4
                            val maxWidth = screenWidth - margin

                            val ratio = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                                intrinsicHeight.toFloat() / intrinsicWidth
                            } else {
                                9f / 16f
                            }

                            val width = maxWidth
                            val height = (width * ratio).toInt()

                            return Rect(0, 0, width, height)
                        }
                    })
                }

                // 处理 HTML 块和内联 HTML
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    builder
                        .on(HtmlBlock::class.java) { visitor, htmlBlock ->
                            visitor.builder().append(htmlBlock.literal)
                        }
                        .on(HtmlInline::class.java) { visitor, htmlInline ->
                            visitor.builder().append(htmlInline.literal)
                        }
                }
            })
            .build()
    }

    fun render(textView: TextView, markdown: String) {
        checkNotNull(markwon) { "MarkdownRenderer not initialized. Call init() first." }
        markwon?.setMarkdown(textView, markdown)
    }
}
