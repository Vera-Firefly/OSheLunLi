package com.firefly.oshe.lunli.ui.screens.components.MainScreenFeatures

import android.content.Context

import com.firefly.oshe.lunli.data.UserData

// 用户个人主页
class UserHomePage(
    context: Context,
    private val currentId: String,
    private val userData: UserData,
    private val returnMainScreen: () -> Unit
) {

}
