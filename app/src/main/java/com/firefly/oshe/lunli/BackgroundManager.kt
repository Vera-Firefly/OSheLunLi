package com.firefly.oshe.lunli

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout

class BackgroundManager(private val context: Context) {
    enum class BackgroundType {
        GRADIENT_BEIGE,
        CUSTOM_IMAGE,
        NONE
    }

    private var currentBackgroundView: View? = null
    private var currentType: BackgroundType = BackgroundType.GRADIENT_BEIGE
    private var backgroundChangeListener: OnBackgroundChangeListener? = null

    interface OnBackgroundChangeListener {
        fun onBackgroundChanged(type: BackgroundType)
    }

    fun setBackgroundChangeListener(listener: OnBackgroundChangeListener) {
        this.backgroundChangeListener = listener
    }

    fun setBackground(container: FrameLayout, type: BackgroundType) {
        currentBackgroundView?.let { container.removeView(it) }

        currentType = type

        currentBackgroundView = when (type) {
            BackgroundType.GRADIENT_BEIGE -> createGradientBackground()
            BackgroundType.CUSTOM_IMAGE -> createCustomImageBackground()
            BackgroundType.NONE -> createTransparentBackground()
        }

        container.addView(currentBackgroundView, 0)
        backgroundChangeListener?.onBackgroundChanged(type)
    }

    fun setCustomImageBackground(container: FrameLayout, drawable: Drawable) {
        currentBackgroundView?.let { container.removeView(it) }

        currentType = BackgroundType.CUSTOM_IMAGE

        currentBackgroundView = createCustomImageBackground().apply {
            background = drawable
        }

        container.addView(currentBackgroundView, 0)
        backgroundChangeListener?.onBackgroundChanged(BackgroundType.CUSTOM_IMAGE)
    }

    fun setCustomImageBackground(container: FrameLayout, bitmap: Bitmap) {
        val drawable = BitmapDrawable(context.resources, bitmap)
        setCustomImageBackground(container, drawable)
    }

    fun updateCurrentBackground(drawable: Drawable) {
        currentBackgroundView?.background = drawable
    }

    fun getCurrentType(): BackgroundType = currentType

    fun clearBackground(container: FrameLayout) {
        setBackground(container, BackgroundType.NONE)
    }

    fun restoreDefaultBackground(container: FrameLayout) {
        setBackground(container, BackgroundType.GRADIENT_BEIGE)
    }

    private fun createGradientBackground(): View {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#e8dcbf"),
                    Color.parseColor("#f1e8d5"),
                    Color.parseColor("#f8f4e9")
                )
            )
            gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
            background = gradientDrawable
        }
    }

    private fun createCustomImageBackground(): View {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun createTransparentBackground(): View {
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun release() {
        currentBackgroundView = null
        backgroundChangeListener = null
    }
}