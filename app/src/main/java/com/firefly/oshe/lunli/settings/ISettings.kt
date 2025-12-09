package com.firefly.oshe.lunli.settings.interfaces

/**
 * 设置项接口
 * 定义所有的设置项
 */
interface ISettings {
    var _ANNOUNCEMENT_DONE: Boolean
    
    suspend fun preload()
    
    fun isPreloaded(): Boolean
}