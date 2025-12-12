package com.firefly.oshe.lunli.settings.interfaces

import android.content.Context

// 设置项注册表, 只在MainActivity初始化一次
object SettingsRegistry {
    private var instance: ISettings? = null

    // 初始化设置项, 只在MaonActivity中调用一次
    fun initialize(context: Context): ISettings {
        synchronized(this) {
            if (instance == null) {
                instance = SettingsImpl(context)
            }
            return instance!!
        }
    }

    /**
     * 获取设置实例
     * PS: 必须先调用initialize(), 不然你等着炸
     */
    internal fun get(): ISettings {
        return instance ?: throw IllegalStateException("芜湖, 炸了! 给爷看代码注释去!")
    }

    fun clear() {
        synchronized(this) {
            instance = null
        }
    }
}