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

    enum class CropType {
        SQUARE,
        SCREEN_RATIO,
        FULL_SCREEN
    }

    private var currentCropType: CropType = CropType.SQUARE

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        cropPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    fun setCropType(type: CropType) {
        currentCropType = type
        setupInitialRects()
        invalidate()
    }

    fun getCropType(): CropType {
        return currentCropType
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

            when (currentCropType) {
                CropType.SQUARE -> {
                    val cropSize = min(viewWidth, viewHeight) * 0.7f
                    val cropLeft = (viewWidth - cropSize) / 2
                    val cropTop = (viewHeight - cropSize) / 2
                    cropRect.set(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize)
                }
                CropType.SCREEN_RATIO -> {
                    val screenRatio = viewWidth / viewHeight
                    val cropWidth: Float
                    val cropHeight: Float

                    if (screenRatio > 1) {
                        cropHeight = viewHeight * 0.7f
                        cropWidth = cropHeight * screenRatio
                    } else {
                        cropWidth = viewWidth * 0.7f
                        cropHeight = cropWidth / screenRatio
                    }

                    val cropLeft = (viewWidth - cropWidth) / 2
                    val cropTop = (viewHeight - cropHeight) / 2
                    cropRect.set(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)
                }
                CropType.FULL_SCREEN -> {
                    cropRect.set(0f, 0f, viewWidth, viewHeight)
                }
            }

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

            if (currentCropType != CropType.FULL_SCREEN) {
                canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
                canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
                canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
                canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

                canvas.drawRect(cropRect, cropPaint)

                drawCornerMarkers(canvas)
            }

            drawHintText(canvas)
        }
    }

    private fun drawHintText(canvas: Canvas) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val text = when (currentCropType) {
            CropType.FULL_SCREEN -> "全屏预览模式 - 双击恢复到初始状态"
            else -> "请使用双指缩放图片, 滑动裁剪框外部移动图片, 双击任意区域恢复到初始状态"
        }
        canvas.drawText(text, width / 2f, cropRect.top - 50f, textPaint)
    }

    private fun drawCornerMarkers(canvas: Canvas) {
        if (currentCropType == CropType.FULL_SCREEN) return

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

                if (currentCropType == CropType.FULL_SCREEN) {
                    isMovingImage = true
                    isResizing = false
                    isDragging = false
                } else {
                    isResizing = isInCornerArea(event.x, event.y)
                    isDragging = !isResizing && cropRect.contains(event.x, event.y)
                    isMovingImage = !isResizing && !isDragging
                }
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
            MotionEvent.ACTION_UP -> {
                isDragging = false
                isResizing = false
                isMovingImage = false
            }
        }
        return true
    }

    private fun isInCornerArea(x: Float, y: Float): Boolean {
        // 全屏模式下不支持调整大小
        if (currentCropType == CropType.FULL_SCREEN) return false

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

        if (currentCropType == CropType.SCREEN_RATIO) {
            val screenRatio = width.toFloat() / height.toFloat()
            val currentRatio = cropRect.width() / cropRect.height()

            if (abs(currentRatio - screenRatio) > 0.01f) {
                val centerX = cropRect.centerX()
                val centerY = cropRect.centerY()
                val newWidth: Float
                val newHeight: Float

                if (currentRatio > screenRatio) {
                    newWidth = cropRect.width()
                    newHeight = newWidth / screenRatio
                } else {
                    newHeight = cropRect.height()
                    newWidth = newHeight * screenRatio
                }

                cropRect.set(
                    centerX - newWidth / 2,
                    centerY - newHeight / 2,
                    centerX + newWidth / 2,
                    centerY + newHeight / 2
                )
            }
        }

        val size = when (currentCropType) {
            CropType.SQUARE -> cropRect.width()
            else -> min(cropRect.width(), cropRect.height())
        }

        if (size < minSize) {
            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()
            when (currentCropType) {
                CropType.SQUARE -> {
                    val halfSize = minSize / 2
                    cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
                }
                CropType.SCREEN_RATIO -> {
                    val screenRatio = width.toFloat() / height.toFloat()
                    val halfWidth = minSize / 2
                    val halfHeight = halfWidth / screenRatio
                    cropRect.set(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
                }
                else -> {}
            }
        } else if (size > maxSize) {
            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()
            when (currentCropType) {
                CropType.SQUARE -> {
                    val halfSize = maxSize / 2
                    cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
                }
                CropType.SCREEN_RATIO -> {
                    val screenRatio = width.toFloat() / height.toFloat()
                    val halfWidth = maxSize / 2
                    val halfHeight = halfWidth / screenRatio
                    cropRect.set(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
                }
                else -> {}
            }
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
        if (currentCropType == CropType.FULL_SCREEN) {
            return originalBitmap
        }

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

                val cropWidth = transformedCropRect.width()
                val cropHeight = transformedCropRect.height()
                val centerX = transformedCropRect.centerX()
                val centerY = transformedCropRect.centerY()

                val finalCropRect = when (currentCropType) {
                    CropType.SQUARE -> {
                        val cropSize = min(cropWidth, cropHeight)
                        val halfSize = cropSize / 2
                        RectF(
                            centerX - halfSize, centerY - halfSize,
                            centerX + halfSize, centerY + halfSize
                        )
                    }
                    CropType.SCREEN_RATIO -> {
                        RectF(
                            transformedCropRect.left, transformedCropRect.top,
                            transformedCropRect.right, transformedCropRect.bottom
                        )
                    }
                    else -> {
                        RectF(
                            centerX - cropWidth / 2, centerY - cropHeight / 2,
                            centerX + cropWidth / 2, centerY + cropHeight / 2
                        )
                    }
                }

                val scaleX = bitmap.width / imageRect.width()
                val scaleY = bitmap.height / imageRect.height()

                val cropX = ((finalCropRect.left - imageRect.left) * scaleX).toInt()
                val cropY = ((finalCropRect.top - imageRect.top) * scaleY).toInt()
                val cropWidthPixels = (finalCropRect.width() * scaleX).toInt()
                val cropHeightPixels = (finalCropRect.height() * scaleY).toInt()

                val safeX = cropX.coerceIn(0, bitmap.width - 1)
                val safeY = cropY.coerceIn(0, bitmap.height - 1)
                val safeWidth = cropWidthPixels.coerceIn(1, bitmap.width - safeX)
                val safeHeight = cropHeightPixels.coerceIn(1, bitmap.height - safeY)

                return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
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
        if (currentCropType == CropType.FULL_SCREEN) return

        val safeRatio = ratio.coerceIn(0.1f, 0.9f)
        when (currentCropType) {
            CropType.SQUARE -> {
                val cropSize = min(width, height) * safeRatio
                val centerX = width / 2f
                val centerY = height / 2f
                val halfSize = cropSize / 2
                cropRect.set(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)
            }
            CropType.SCREEN_RATIO -> {
                val screenRatio = width.toFloat() / height.toFloat()
                val cropWidth: Float
                val cropHeight: Float

                if (screenRatio > 1) {
                    cropHeight = height * safeRatio
                    cropWidth = cropHeight * screenRatio
                } else {
                    cropWidth = width * safeRatio
                    cropHeight = cropWidth / screenRatio
                }

                val centerX = width / 2f
                val centerY = height / 2f
                cropRect.set(
                    centerX - cropWidth / 2,
                    centerY - cropHeight / 2,
                    centerX + cropWidth / 2,
                    centerY + cropHeight / 2
                )
            }
            else -> {}
        }
        invalidate()
    }
}