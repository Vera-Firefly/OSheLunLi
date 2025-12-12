package com.firefly.oshe.lunli.data

import androidx.annotation.Keep

@Keep
data class NewVersion(
    var tag_name: String,
    var name: String,
    var body: String,
    var url: String,
    var created_at: String
) {

}
