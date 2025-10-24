package com.firefly.oshe.lunli.GlobalInterface

import android.app.Activity
import android.content.Intent
import android.net.Uri

class ImagePicker(private val activity: Activity) {
    private var imageCallback: ((Uri?) -> Unit)? = null

    fun pickImage(callback: (Uri?) -> Unit) {
        this.imageCallback = callback

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val chooserIntent = Intent.createChooser(intent, "选择图片")

        try {
            activity.startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE)
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                imageCallback?.invoke(uri)
            } else {
                imageCallback?.invoke(null)
            }
            imageCallback = null
        }
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 1001
    }
}