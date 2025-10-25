package com.firefly.oshe.lunli.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import androidx.annotation.DrawableRes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 图像处理工具类
 * 功能：处理Bitmap、Base64、Drawable、File、Uri之间的转换
 */
object ImageUtils {

    // region Bitmap 相关操作

    /**
     * 从资源文件创建Bitmap
     */
    fun bitmapFromResource(context: Context, @DrawableRes resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从资源文件创建优化尺寸的Bitmap
     */
    fun bitmapFromResourceOptimized(
        context: Context,
        @DrawableRes resId: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从文件创建Bitmap
     */
    fun bitmapFromFile(filePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从文件创建优化尺寸的Bitmap
     */
    fun bitmapFromFileOptimized(filePath: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从Uri创建Bitmap
     */
    fun bitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从Drawable创建Bitmap
     */
    fun bitmapFromDrawable(drawable: Drawable): Bitmap {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    /**
     * 缩放Bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 按比例缩放Bitmap
     */
    fun scaleBitmapByRatio(bitmap: Bitmap, ratio: Float): Bitmap {
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 裁剪Bitmap
     */
    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    /**
     * 将Bitmap保存为文件
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 80): Boolean {
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(format, quality, outputStream)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将Bitmap转换为Base64字符串
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 将Base64字符串转换为Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val pureBase64 = extractPureBase64(base64String)
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将Base64转换为优化尺寸的Bitmap
     */
    fun base64ToBitmapOptimized(
        base64String: String,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Bitmap? {
        return try {
            val pureBase64 = extractPureBase64(base64String)
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将文件转换为Base64字符串
     */
    fun fileToBase64(filePath: String): String {
        return try {
            val file = File(filePath)
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 将Base64字符串保存为文件
     */
    fun base64ToFile(base64String: String, outputFile: File): Boolean {
        return try {
            val pureBase64 = extractPureBase64(base64String)
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            FileOutputStream(outputFile).use { outputStream ->
                outputStream.write(imageBytes)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将Drawable转换为Base64
     */
    fun drawableToBase64(
        drawable: Drawable,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): String {
        val bitmap = bitmapFromDrawable(drawable)
        return bitmapToBase64(bitmap, format, quality)
    }

    /**
     * 将Base64转换为Drawable
     */
    fun base64ToDrawable(context: Context, base64String: String): BitmapDrawable? {
        return base64ToBitmap(base64String)?.let { bitmap ->
            BitmapDrawable(context.resources, bitmap)
        }
    }

    /**
     * 将资源图片转换为Base64
     */
    fun resourceToBase64(
        context: Context,
        @DrawableRes resId: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): String {
        return bitmapFromResource(context, resId)?.let { bitmap ->
            bitmapToBase64(bitmap, format, quality)
        } ?: ""
    }

    /**
     * 将Uri转换为Base64
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ): String {
        return bitmapFromUri(context, uri)?.let { bitmap ->
            bitmapToBase64(bitmap, format, quality)
        } ?: ""
    }

    /**
     * 提取纯净的Base64字符串（移除前缀）
     */
    private fun extractPureBase64(base64String: String): String {
        return when {
            base64String.contains("base64,") -> base64String.substringAfter("base64,")
            base64String.contains("data:image") -> base64String.substringAfter(",")
            else -> base64String
        }.trim()
    }

    /**
     * 计算Bitmap采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (width, height) = options.outWidth to options.outHeight
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 获取Bitmap的尺寸信息
     */
    fun getBitmapSizeInfo(bitmap: Bitmap): String {
        return "Width: ${bitmap.width}, Height: ${bitmap.height}, Config: ${bitmap.config}"
    }

    /**
     * 估算Bitmap的内存占用（单位：MB）
     */
    fun estimateBitmapMemory(bitmap: Bitmap): Double {
        val bytesPerPixel = when (bitmap.config) {
            Bitmap.Config.ALPHA_8 -> 1
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ARGB_8888 -> 4
            else -> 4
        }
        return (bitmap.width * bitmap.height * bytesPerPixel).toDouble() / (1024 * 1024)
    }

    /**
     * 安全回收Bitmap
     */
    fun safeRecycleBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

}