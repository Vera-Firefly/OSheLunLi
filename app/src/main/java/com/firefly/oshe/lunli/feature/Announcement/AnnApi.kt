package com.firefly.oshe.lunli.feature.Announcement

import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.Announcement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnApi {
    fun getInfo(date: String, call: (List<Announcement>) -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedAnns = mutableListOf<Announcement>()
            val newAnn : List<SBClient.Announcement> = SBClient.subscribeAnnouncement(date)
            newAnn.forEach { it ->
                val ann = Announcement(
                    it.date,
                    it.body,
                    it.created_at
                )
                fetchedAnns.add(ann)
            }
            withContext(Dispatchers.Main) {
                call(fetchedAnns)
            }
        }
    }
}