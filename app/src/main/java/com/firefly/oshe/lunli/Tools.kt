package com.firefly.oshe.lunli

import android.content.Context
import android.widget.Toast

class Tools() {
    private var previousToast: Toast? = null

    fun ShowToast(context: Context, message: String) {
        previousToast?.cancel()
        previousToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        previousToast?.show()
    }
}