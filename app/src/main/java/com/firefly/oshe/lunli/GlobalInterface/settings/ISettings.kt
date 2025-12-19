package com.firefly.oshe.lunli.GlobalInterface.settings

/**
 * 设置项接口
 * 定义所有的设置项
 */
interface ISettings {
    var _LAST_APP_VERSION: Int
    var _CACHED_APP_VERSION: Int
    var _SAVED_IGNORE_APP_VERSION: Int
    var _SAVED_ANN_VERSIOON: Int

    suspend fun preload()

    fun isPreloaded(): Boolean
}