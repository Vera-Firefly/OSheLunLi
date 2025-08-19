package com.firefly.oshe.lunli.data

import androidx.annotation.Keep

@Keep
data class UserData(
    var userId: String = "",
    var userName: String = "",
    var password: String = "",
    var hasPasswordError: Boolean = false
) {
    fun isValid(): Boolean {
        return userId.isNotBlank() && password.isNotBlank()
    }
}