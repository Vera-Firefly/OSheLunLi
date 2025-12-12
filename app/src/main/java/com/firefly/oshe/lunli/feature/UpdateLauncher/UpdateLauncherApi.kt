package com.firefly.oshe.lunli.feature.UpdateLauncher

import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.NewVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class UpdateLauncherApi {
    fun getInfo(version: String, callback: (List<NewVersion>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedVersions = mutableListOf<NewVersion>()
            val newVersion: List<SBClient.NewVersion> = SBClient.subscribeNewVersion(version)
            newVersion.forEach { it ->
                val info = NewVersion(
                    it.tag_name,
                    it.name,
                    it.body,
                    it.url,
                    it.created_at
                )
                fetchedVersions.add(info)
            }
            withContext(Dispatchers.Main) {
                callback(fetchedVersions)
            }
        }
    }

}