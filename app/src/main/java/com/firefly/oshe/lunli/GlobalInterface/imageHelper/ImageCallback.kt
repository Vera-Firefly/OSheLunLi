package com.firefly.oshe.lunli.GlobalInterface.imageHelper

import android.net.Uri

interface SimpleImageCallback {
    fun onImageSelected(uri: Uri)
    fun onSelectionCancelled()
    fun onSelectionFailed(error: String)
}

object ImageSelectionManager {
    private var callback: SimpleImageCallback? = null
    private var currentRequester: String = ""

    fun setCallback(requester: String, callback: SimpleImageCallback) {
        this.currentRequester = requester
        this.callback = callback
    }

    fun clearCallback() {
        this.callback = null
        this.currentRequester = ""
    }

    fun notifyImageSelected(uri: Uri) { callback?.onImageSelected(uri) }

    fun notifySelectionCancelled() { callback?.onSelectionCancelled() }

    fun notifySelectionFailed(error: String) { callback?.onSelectionFailed(error) }

    fun getCurrentRequester(): String { return currentRequester }
}