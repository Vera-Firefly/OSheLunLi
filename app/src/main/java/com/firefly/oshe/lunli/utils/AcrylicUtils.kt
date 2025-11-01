package com.firefly.oshe.lunli.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.firefly.oshe.lunli.dp

object AcrylicUtils {

    fun createAcrylicBackground(
        context: Context,
        baseColor: Int = Color.argb(180, 128, 128, 128),
        blurRadius: Float = 35f,
        cornerRadius: Float = 12f.dp
    ): Drawable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createModernAcrylicBackground(baseColor, blurRadius, cornerRadius)
        } else {
            createLegacyAcrylicBackground(context, baseColor, cornerRadius)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createModernAcrylicBackground(
        baseColor: Int,
        blurRadius: Float,
        cornerRadius: Float
    ): Drawable {
        val blurMaskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        return object : Drawable() {
            private val paint = Paint().apply {
                color = baseColor
                maskFilter = blurMaskFilter
                isAntiAlias = true
            }

            override fun draw(canvas: Canvas) {
                canvas.drawRoundRect(
                    bounds.left.toFloat(),
                    bounds.top.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    cornerRadius,
                    cornerRadius,
                    paint
                )
            }

            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                paint.colorFilter = colorFilter
            }

            override fun getOpacity(): Int {
                return PixelFormat.TRANSLUCENT
            }
        }
    }

    private fun createLegacyAcrylicBackground(
        context: Context,
        baseColor: Int,
        _cornerRadius: Float
    ): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = _cornerRadius
            setColor(Color.argb(200, 100, 100, 100))
            setStroke(1.dp, Color.argb(120, 200, 200, 200))
        }
    }

    fun setupAcrylicEffect(view: View, blurRadius: Float = 20f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(RenderEffect.createBlurEffect(
                blurRadius, blurRadius, Shader.TileMode.MIRROR
            ))
        }
    }
}