package com.firefly.oshe.lunli.data

import androidx.annotation.Keep

@Keep
data class UserMessage(
    var userId: String = "",
    var userName: String = "",
    var biliName: String = "",
    var biliHomePage: String = "",
    var biliUID: String = "",
    var ksName: String = "",
    var ksHomePage: String = "",
    var ksUID: String = "",
    var tiktokName: String = "",
    var tiktokHomePage: String = "",
    var tiktokUID: String = "",
    var isExpiration: Boolean = false
) {}
