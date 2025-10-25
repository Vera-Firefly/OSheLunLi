package com.firefly.oshe.lunli.data.ChatRoom

data class Message(
    val id: String,
    val sender: String,
    val senderImage: String,
    val content: String
)