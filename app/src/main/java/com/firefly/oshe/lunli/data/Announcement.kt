package com.firefly.oshe.lunli.data

import androidx.annotation.Keep

@Keep
data class Announcement(
    var date: String,
    var body: String,
    var created_at: String
)
