package com.firefly.oshe.lunli.data.ChatRoom

data class RoomInfo(
    val id: String,
    val title: String,
    val creator: String,
    val roomMessage: String,
    val roomPassword: String
)
