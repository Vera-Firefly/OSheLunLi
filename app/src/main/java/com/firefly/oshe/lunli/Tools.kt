package com.firefly.oshe.lunli

import android.content.Context
import android.widget.Toast

object Tools {
    private var previousToast: Toast? = null

    fun Context.ShowToast(message: String) {
        previousToast?.cancel()
        previousToast = Toast.makeText(this, message, Toast.LENGTH_SHORT).apply {
            show()
        }
    }

    @JvmStatic
    fun Toast(context: Context, message: String) {
        context.ShowToast(message)
    }
}