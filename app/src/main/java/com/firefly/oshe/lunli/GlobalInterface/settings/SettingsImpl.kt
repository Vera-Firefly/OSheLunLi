package com.firefly.oshe.lunli.GlobalInterface.settings

import android.content.Context
import com.firefly.oshe.lunli.data.settings.SettingValue
import com.firefly.oshe.lunli.data.settings.SettingsKey
import com.firefly.oshe.lunli.data.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 设置项实现类
 * 所有设置项的保存逻辑即I/O操作完全封装在此类中
 * 解释一下: 这个类初始化后, 给所有成员变量赋值,
 * 利用初始化之后赋值值存缓存至内存的机制, 直接给
 * 成员变量赋值, 然后提供接口成员变量的重写方案,
 * getter获取缓存中的成员变量的值, setter对缓存成员
 * 变量进行赋值, 并进行异步保存到数据库, 这样只需
 * 要在MainActivity进行初始化, 等全部设置项从数据库
 * 获取完成后再进行UI部分的操作, 虽然这样做会有一个
 * 严重的后果(用户能够感觉到这个过程), 但如果我加个
 * 启动动画就可以了, 反正进不去也是用户的设备性能不
 * 够, 怎么能怪我们开发者呢? 我这种方法确实比较新,
 * 也比较麻烦, 但好处是处理大量的数据时能够高效处理,
 * 不会像Pref那样数据量一多直接炸, 你们说对吧[手动狗头]
 * PS: 就这样吧, 初始化完成后交给缓存得了, I/O操作能
 * 给我整死, 我相信你们也不希望在任何与设置有关的地
 * 方看到一堆的I/O操作, 虽然我已经干过了[再次狗头]
 */
internal class SettingsImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : ISettings {
    private val settingsManager by lazy { SettingsManager.Companion.getInstance(context) }

    // 预加载状态
    private val isPreloaded = AtomicBoolean(false)

    // 缓存成员变量
    private var _lastAppVersion: Int = 100
    private var _cachedAppVersion: Int = 0
    private var _savedIgnoreAppVersion: Int = 0
    private var _savedAnnVersion: Int = 0

    // ISettings接口实现
    override var _LAST_APP_VERSION: Int
        get() = _lastAppVersion
        set(value) {
            _lastAppVersion = value
            saveSync(SettingsKey.LAST_APP_VERSION, value)
        }

    override var _CACHED_APP_VERSION: Int
        get() = _cachedAppVersion
        set(value) {
            _cachedAppVersion = value
            saveSync(SettingsKey.CACHED_APP_VERSION, value)
        }

    override var _SAVED_IGNORE_APP_VERSION: Int
        get() = _savedIgnoreAppVersion
        set(value) {
            _savedIgnoreAppVersion = value
            saveSync(SettingsKey.SAVED_IGNORE_APP_VERSION, value)
        }

    override var _SAVED_ANN_VERSIOON: Int
        get() = _savedAnnVersion
        set(value) {
            _savedAnnVersion = value
            saveSync(SettingsKey.SAVED_ANN_VERSION, value)
        }

    // 懒得给注释, 看不懂别看了, 回去睡觉
    override suspend fun preload() {
        if (isPreloaded.get()) return

        withContext(Dispatchers.IO) {
            _lastAppVersion = settingsManager.getInt(
                SettingsKey.LAST_APP_VERSION,
                100
            )

            _cachedAppVersion = settingsManager.getInt(
                SettingsKey.CACHED_APP_VERSION,
                0
            )

            _savedIgnoreAppVersion = settingsManager.getInt(
                SettingsKey.SAVED_IGNORE_APP_VERSION,
                0
            )

            _savedAnnVersion = settingsManager.getInt(
                SettingsKey.SAVED_ANN_VERSION,
                0
            )

            isPreloaded.set(true)
        }
    }

    override fun isPreloaded(): Boolean = isPreloaded.get()

    // 就用Any, 就用
    private fun saveSync(key: String, value: Any) {
        coroutineScope.launch(Dispatchers.IO) {
            saveValueToDatabase(key, value)
        }
    }

    // 异步保存值到数据库
    private suspend fun saveValueToDatabase(key: String, value: Any) {
        when (value) {
            is Boolean -> settingsManager.saveBoolean(key, value)
            is String -> settingsManager.saveString(key, value)
            is Int -> settingsManager.saveInt(key, value)
            is Long -> settingsManager.saveLong(key, value)
            is Float -> settingsManager.saveSetting(key, SettingValue.FloatValue(value))
            is Double -> settingsManager.saveSetting(key, SettingValue.DoubleValue(value))
            /*
            is List<*> -> {
                when {
                    value.isNotEmpty() && value[0] is String ->
                        settingsManager.saveStringList(key, value as List<String>)

                    value.isNotEmpty() && value[0] is Int ->
                        settingsManager.saveSetting(key, SettingValue.IntListValue(value as List<Int>))

                    value.isEmpty() ->
                        settingsManager.saveStringList(key, emptyList())

                    else -> {
                        try {
                            val json = Json.encodeToString(value)
                            settingsManager.saveSetting(key, SettingValue.StringValue(json))
                        } catch (e: Exception) {
                            settingsManager.saveStringList(key, emptyList())
                        }
                    }
                }
            }

            // 我也不知道会不会用到这个, 先留着吧, 有总比没有好
            else -> {
                try {
                    val json = Json.encodeToString(value.toString())
                    settingsManager.saveSetting(key, SettingValue.StringValue(json))
                } catch (e: Exception) {
                    settingsManager.saveString(key, "")
                }
            }
             */
        }
    }
}