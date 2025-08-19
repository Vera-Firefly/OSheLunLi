package com.firefly.oshe.lunli

import android.content.res.Resources
import android.util.TypedValue

val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

val Float.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)
