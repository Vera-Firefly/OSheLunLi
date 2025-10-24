package com.firefly.oshe.lunli.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class CropUtils @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var originalBitmap: Bitmap? = null
    private var displayBitmap: Bitmap? = null
    private var cropRect = RectF()
    private var imageRect = RectF()

    private var imageScale = 1.0f
    private var imageTranslateX = 0f
    private var imageTranslateY = 0f

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

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var isResizing = false
    private var isMovingImage = false

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

            val cropSize = min(viewWidth, viewHeight) * 0.7f
            val cropLeft = (viewWidth - cropSize) / 2
            val cropTop = (viewHeight - cropSize) / 2

            cropRect.set(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize)

            imageScale = 1.0f
            imageTranslateX = 0f
            imageTranslateY = 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        displayBitmap?.let { bitmap ->
            canvas.save()

            canvas.translate(imageTranslateX, imageTranslateY)
            canvas.scale(imageScale, imageScale, width / 2f, height / 2f)

            canvas.drawBitmap(bitmap, null, imageRect, paint)

            canvas.restore()

            canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
            canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
            canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
            canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

            canvas.drawRect(cropRect, cropPaint)

            drawCornerMarkers(canvas)

            drawHintText(canvas)
        }
    }

    private fun drawHintText(canvas: Canvas) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val text = "请使用双指缩放图片, 滑动裁剪框外部移动图片, 双击任意区域恢复到初始状态"
        canvas.drawText(text, width / 2f, cropRect.top - 50f, textPaint)
    }

    private fun drawCornerMarkers(canvas: Canvas) {
        val cornerSize = 20f

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
                isMovingImage = !isResizing && !isDragging
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    when {
                        isResizing -> adjustCropSize(dx, dy)
                        isDragging -> moveCropRect(dx, dy)
                        isMovingImage -> moveImage(dx, dy)
                    }

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isResizing = false
                isMovingImage = false
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
        val maxSize = min(width, height) * 0.9f

        when {
            abs(lastTouchX - cropRect.left) < 50f && abs(lastTouchY - cropRect.top) < 50f -> {
                val newLeft = (cropRect.left + dx).coerceAtLeast(0f)
                val newTop = (cropRect.top + dy).coerceAtLeast(0f)
                val sizeChange = min(cropRect.left - newLeft, cropRect.top - newTop)

                cropRect.left = cropRect.left - sizeChange
                cropRect.top = cropRect.top - sizeChange
            }
            abs(lastTouchX - cropRect.right) < 50f && abs(lastTouchY - cropRect.top) < 50f -> {
                val newRight = (cropRect.right + dx).coerceAtMost(width.toFloat())
                val newTop = (cropRect.top + dy).coerceAtLeast(0f)
                val sizeChange = min(newRight - cropRect.right, cropRect.top - newTop)

                cropRect.right = cropRect.right + sizeChange
                cropRect.top = cropRect.top - sizeChange
            }
            abs(lastTouchX - cropRect.left) < 50f && abs(lastTouchY - cropRect.bottom) < 50f -> {
                val newLeft = (cropRect.left + dx).coerceAtLeast(0f)
                val newBottom = (cropRect.bottom + dy).coerceAtMost(height.toFloat())
                val sizeChange = min(cropRect.left - newLeft, newBottom - cropRect.bottom)

                cropRect.left = cropRect.left - sizeChange
                cropRect.bottom = cropRect.bottom + sizeChange
            }
            abs(lastTouchX - cropRect.right) < 50f && abs(lastTouchY - cropRect.bottom) < 50f -> {
                val newRight = (cropRect.right + dx).coerceAtMost(width.toFloat())
                val newBottom = (cropRect.bottom + dy).coerceAtMost(height.toFloat())
                val sizeChange = min(newRight - cropRect.right, newBottom - cropRect.bottom)

                cropRect.right = cropRect.right + sizeChange
                cropRect.bottom = cropRect.bottom + sizeChange
            }
        }

        val size = cropRect.width()
        if (size < minSize) {
            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()
            val halfSize = minSize / 2
            cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
        } else if (size > maxSize) {
            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()
            val halfSize = maxSize / 2
            cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
        }
    }

    private fun moveCropRect(dx: Float, dy: Float) {
        val newLeft = cropRect.left + dx
        val newTop = cropRect.top + dy
        val newRight = cropRect.right + dx
        val newBottom = cropRect.bottom + dy

        if (newLeft >= 0 && newRight <= width) {
            cropRect.left = newLeft
            cropRect.right = newRight
        }
        if (newTop >= 0 && newBottom <= height) {
            cropRect.top = newTop
            cropRect.bottom = newBottom
        }
    }

    private fun moveImage(dx: Float, dy: Float) {
        val maxTranslate = 500f * imageScale
        imageTranslateX = (imageTranslateX + dx).coerceIn(-maxTranslate, maxTranslate)
        imageTranslateY = (imageTranslateY + dy).coerceIn(-maxTranslate, maxTranslate)
    }

    fun cropImage(): Bitmap? {
        originalBitmap?.let { bitmap ->
            val matrix = Matrix()
            matrix.postTranslate(imageTranslateX, imageTranslateY)
            matrix.postScale(imageScale, imageScale, width / 2f, height / 2f)

            val inverseMatrix = Matrix()
            if (matrix.invert(inverseMatrix)) {
                val points = floatArrayOf(
                    cropRect.left, cropRect.top,
                    cropRect.right, cropRect.top,
                    cropRect.right, cropRect.bottom,
                    cropRect.left, cropRect.bottom
                )

                inverseMatrix.mapPoints(points)

                val transformedCropRect = RectF(
                    points[0], points[1],
                    points[4], points[5]
                )

                val cropSize = min(transformedCropRect.width(), transformedCropRect.height())
                val centerX = transformedCropRect.centerX()
                val centerY = transformedCropRect.centerY()
                val halfSize = cropSize / 2

                val finalCropRect = RectF(
                    centerX - halfSize, centerY - halfSize,
                    centerX + halfSize, centerY + halfSize
                )

                val scaleX = bitmap.width / imageRect.width()
                val scaleY = bitmap.height / imageRect.height()

                val cropX = ((finalCropRect.left - imageRect.left) * scaleX).toInt()
                val cropY = ((finalCropRect.top - imageRect.top) * scaleY).toInt()
                val cropSizePixels = (cropSize * scaleX).toInt()

                val safeX = cropX.coerceIn(0, bitmap.width - 1)
                val safeY = cropY.coerceIn(0, bitmap.height - 1)
                val safeSize = cropSizePixels.coerceIn(1, min(bitmap.width - safeX, bitmap.height - safeY))

                return Bitmap.createBitmap(bitmap, safeX, safeY, safeSize, safeSize)
            }
        }
        return null
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScale = imageScale
            imageScale *= detector.scaleFactor
            imageScale = imageScale.coerceIn(0.1f, 5.0f)

            val focusX = detector.focusX
            val focusY = detector.focusY
            imageTranslateX += (focusX - imageTranslateX) * (1 - imageScale / previousScale)
            imageTranslateY += (focusY - imageTranslateY) * (1 - imageScale / previousScale)

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

    /**
     * 设置裁剪框大小比例
     * @param ratio 相对于屏幕最小边长的比例 (0.1 - 0.9)
     */
    fun setCropRatio(ratio: Float) {
        val safeRatio = ratio.coerceIn(0.1f, 0.9f)
        val cropSize = min(width, height) * safeRatio
        val centerX = width / 2f
        val centerY = height / 2f
        val halfSize = cropSize / 2

        cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
        invalidate()
    }
}