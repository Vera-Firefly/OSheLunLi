package com.firefly.oshe.lunli.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.firefly.oshe.lunli.client.SupaBase.SBClient
import com.firefly.oshe.lunli.data.NewVersion

abstract class UpdateLauncherApi {

    private var versionInfo = mutableListOf<NewVersion>()

    fun getInfo(version: String): MutableList<NewVersion> {
        CoroutineScope(Dispatchers.IO).launch {
            val newVersion : List<SBClient.NewVersion> =  SBClient.subscribeNewVersion(version)
            newVersion.forEach { it ->
                val info = NewVersion(
                    it.tag_name,
                    it.name,
                    it.body,
                    it.created_at
                )
                versionInfo.add(info)
            }
        }
        return versionInfo
    }

}