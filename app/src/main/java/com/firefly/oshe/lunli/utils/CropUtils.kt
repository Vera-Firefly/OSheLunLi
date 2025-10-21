package com.firefly.oshe.lunli.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.isVisible
import kotlin.math.abs

class CropUtils @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var originalBitmap: Bitmap? = null
    private var displayBitmap: Bitmap? = null
    private var cropRect = RectF()
    private var imageRect = RectF()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var isResizing = false

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    var onCropListener: ((Bitmap?) -> Unit)? = null

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        cropPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    fun setImage(bitmap: Bitmap) {
        originalBitmap = bitmap
        displayBitmap = bitmap
        setupInitialRects()
        invalidate()
    }

    private fun setupInitialRects() {
        originalBitmap?.let { bitmap ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val viewRatio = viewWidth / viewHeight

            if (bitmapRatio > viewRatio) {
                val displayHeight = viewWidth / bitmapRatio
                imageRect.set(0f, (viewHeight - displayHeight) / 2, viewWidth, (viewHeight + displayHeight) / 2)
            } else {
                val displayWidth = viewHeight * bitmapRatio
                imageRect.set((viewWidth - displayWidth) / 2, 0f, (viewWidth + displayWidth) / 2, viewHeight)
            }

            val cropWidth = imageRect.width() * 0.8f
            val cropHeight = imageRect.height() * 0.8f
            val cropLeft = imageRect.centerX() - cropWidth / 2
            val cropTop = imageRect.centerY() - cropHeight / 2

            cropRect.set(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        displayBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, imageRect, paint)

            canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
            canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
            canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
            canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

            canvas.drawRect(cropRect, cropPaint)

            drawCornerMarkers(canvas)
        }
    }

    private fun drawCornerMarkers(canvas: Canvas) {
        val cornerSize = 20f
        val strokeWidth = 4f

        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top, cropPaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cornerSize, cropPaint)

        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - cornerSize, cropRect.top, cropPaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cornerSize, cropPaint)

        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cornerSize, cropRect.bottom, cropPaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - cornerSize, cropPaint)

        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - cornerSize, cropRect.bottom, cropPaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - cornerSize, cropPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y

                isResizing = isInCornerArea(event.x, event.y)
                isDragging = !isResizing && cropRect.contains(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    if (isResizing) {
                        adjustCropSize(dx, dy)
                    } else if (isDragging) {
                        moveCropRect(dx, dy)
                    }

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isResizing = false
            }
        }
        return true
    }

    private fun isInCornerArea(x: Float, y: Float): Boolean {
        val cornerThreshold = 50f
        return (abs(x - cropRect.left) < cornerThreshold && abs(y - cropRect.top) < cornerThreshold) ||
                (abs(x - cropRect.right) < cornerThreshold && abs(y - cropRect.top) < cornerThreshold) ||
                (abs(x - cropRect.left) < cornerThreshold && abs(y - cropRect.bottom) < cornerThreshold) ||
                (abs(x - cropRect.right) < cornerThreshold && abs(y - cropRect.bottom) < cornerThreshold)
    }

    private fun adjustCropSize(dx: Float, dy: Float) {
        val minSize = 100f

        if (abs(lastTouchX - cropRect.left) < 50f && abs(lastTouchY - cropRect.top) < 50f) {
            cropRect.left = (cropRect.left + dx).coerceAtLeast(imageRect.left)
            cropRect.top = (cropRect.top + dy).coerceAtLeast(imageRect.top)
        } else if (abs(lastTouchX - cropRect.right) < 50f && abs(lastTouchY - cropRect.top) < 50f) {
            cropRect.right = (cropRect.right + dx).coerceAtMost(imageRect.right)
            cropRect.top = (cropRect.top + dy).coerceAtLeast(imageRect.top)
        } else if (abs(lastTouchX - cropRect.left) < 50f && abs(lastTouchY - cropRect.bottom) < 50f) {
            cropRect.left = (cropRect.left + dx).coerceAtLeast(imageRect.left)
            cropRect.bottom = (cropRect.bottom + dy).coerceAtMost(imageRect.bottom)
        } else if (abs(lastTouchX - cropRect.right) < 50f && abs(lastTouchY - cropRect.bottom) < 50f) {
            cropRect.right = (cropRect.right + dx).coerceAtMost(imageRect.right)
            cropRect.bottom = (cropRect.bottom + dy).coerceAtMost(imageRect.bottom)
        }

        if (cropRect.width() < minSize) {
            if (lastTouchX < cropRect.centerX()) {
                cropRect.left = cropRect.right - minSize
            } else {
                cropRect.right = cropRect.left + minSize
            }
        }
        if (cropRect.height() < minSize) {
            if (lastTouchY < cropRect.centerY()) {
                cropRect.top = cropRect.bottom - minSize
            } else {
                cropRect.bottom = cropRect.top + minSize
            }
        }
    }

    private fun moveCropRect(dx: Float, dy: Float) {
        val newLeft = cropRect.left + dx
        val newTop = cropRect.top + dy
        val newRight = cropRect.right + dx
        val newBottom = cropRect.bottom + dy

        if (newLeft >= imageRect.left && newRight <= imageRect.right) {
            cropRect.left = newLeft
            cropRect.right = newRight
        }
        if (newTop >= imageRect.top && newBottom <= imageRect.bottom) {
            cropRect.top = newTop
            cropRect.bottom = newBottom
        }
    }

    fun cropImage(): Bitmap? {
        originalBitmap?.let { bitmap ->
            val scaleX = bitmap.width / imageRect.width()
            val scaleY = bitmap.height / imageRect.height()

            val cropX = ((cropRect.left - imageRect.left) * scaleX).toInt()
            val cropY = ((cropRect.top - imageRect.top) * scaleY).toInt()
            val cropWidth = (cropRect.width() * scaleX).toInt()
            val cropHeight = (cropRect.height() * scaleY).toInt()

            val safeX = cropX.coerceIn(0, bitmap.width - 1)
            val safeY = cropY.coerceIn(0, bitmap.height - 1)
            val safeWidth = cropWidth.coerceIn(1, bitmap.width - safeX)
            val safeHeight = cropHeight.coerceIn(1, bitmap.height - safeY)

            return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
        }
        return null
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            setupInitialRects()
            invalidate()
            return true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupInitialRects()
    }
}
