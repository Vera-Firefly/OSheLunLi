package com.firefly.oshe.lunli.data.settings

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import com.firefly.oshe.lunli.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors

sealed class SettingValue {
    data class StringValue(val value: String) : SettingValue()
    data class IntValue(val value: Int) : SettingValue()
    data class LongValue(val value: Long) : SettingValue()
    data class BooleanValue(val value: Boolean) : SettingValue()
    data class FloatValue(val value: Float) : SettingValue()
    data class DoubleValue(val value: Double) : SettingValue()
    data class StringListValue(val value: List<String>) : SettingValue()
    data class IntListValue(val value: List<Int>) : SettingValue()

    fun toJson(): String = when (this) {
        is StringValue -> Json.encodeToString(SerializedValue("string", value))
        is IntValue -> Json.encodeToString(SerializedValue("int", value.toString()))
        is LongValue -> Json.encodeToString(SerializedValue("long", value.toString()))
        is BooleanValue -> Json.encodeToString(SerializedValue("boolean", value.toString()))
        is FloatValue -> Json.encodeToString(SerializedValue("float", value.toString()))
        is DoubleValue -> Json.encodeToString(SerializedValue("double", value.toString()))
        is StringListValue -> Json.encodeToString(SerializedValue("string_list", Json.encodeToString(value)))
        is IntListValue -> Json.encodeToString(SerializedValue("int_list", Json.encodeToString(value)))
    }
}

@Serializable
private data class SerializedValue(val type: String, val value: String)

class SettingConverters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun fromJsonToSettingValue(json: String): SettingValue {
            val serialized = Json.decodeFromString<SerializedValue>(json)
            return when (serialized.type) {
                "string" -> SettingValue.StringValue(serialized.value)
                "int" -> SettingValue.IntValue(serialized.value.toInt())
                "long" -> SettingValue.LongValue(serialized.value.toLong())
                "boolean" -> SettingValue.BooleanValue(serialized.value.toBoolean())
                "float" -> SettingValue.FloatValue(serialized.value.toFloat())
                "double" -> SettingValue.DoubleValue(serialized.value.toDouble())
                "string_list" -> SettingValue.StringListValue(Json.decodeFromString(serialized.value))
                "int_list" -> SettingValue.IntListValue(Json.decodeFromString(serialized.value))
                else -> SettingValue.StringValue("")
            }
        }

        @TypeConverter
        @JvmStatic
        fun fromSettingValueToJson(value: SettingValue): String {
            return value.toJson()
        }
    }
}

object SettingsKey {
    const val LAST_APP_VERSION = "last_app_version"
    const val CACHED_APP_VERSION = "cached_app_version"
    const val SAVED_IGNORE_APP_VERSION = "saved_ignore_app_version"
    const val ANNOUNCEMENT_DONE = "announcement_done"
}

@Entity(tableName = "app_settings")
@TypeConverters(SettingConverters::class)
data class SettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val key: String,

    @ColumnInfo(name = "setting_value")
    val value: SettingValue,

    @ColumnInfo(name = "category")
    val category: String = "general",

    @ColumnInfo(name = "is_user_specific")
    val isUserSpecific: Boolean = false,

    @ColumnInfo(name = "last_modified")
    val lastModified: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = "description")
    val description: String = ""
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE setting_key = :key")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("SELECT * FROM app_settings WHERE category = :category")
    suspend fun getSettingsByCategory(category: String): List<SettingEntity>

    @Query("SELECT * FROM app_settings WHERE is_user_specific = :isUserSpecific")
    suspend fun getSettingsByUserScope(isUserSpecific: Boolean): List<SettingEntity>

    @Query("SELECT * FROM app_settings WHERE setting_key IN (:keys)")
    suspend fun getSettings(keys: List<String>): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingEntity>)

    @Query("DELETE FROM app_settings WHERE setting_key = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM app_settings WHERE setting_key IN (:keys)")
    suspend fun deleteSettings(keys: List<String>)

    @Query("DELETE FROM app_settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)

    @Query("DELETE FROM app_settings WHERE is_user_specific = :isUserSpecific")
    suspend fun deleteSettingsByUserScope(isUserSpecific: Boolean)

    @Query("UPDATE app_settings SET setting_value = :value, last_modified = :timestamp WHERE setting_key = :key")
    suspend fun updateSetting(key: String, value: SettingValue, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM app_settings")
    suspend fun getSettingsCount(): Int

    @Query("SELECT setting_key FROM app_settings")
    suspend fun getAllKeys(): List<String>
}

@Database(
    entities = [SettingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SettingConverters::class)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null

        fun getInstance(context: Context): SettingsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createInstance(context).also { instance ->
                    INSTANCE = instance
                }
            }
        }

        private fun createInstance(context: Context): SettingsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SettingsDatabase::class.java,
                "app_settings.db"
            )
                .setQueryExecutor(Executors.newSingleThreadExecutor())
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(connection: SQLiteConnection) {
                            super.onCreate(connection)
                        }
                    }
                )
                .build()
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

class SettingsManager(private val context: Context) {
    private val database by lazy {
        SettingsDatabase.getInstance(context)
    }

    private val settingsDao by lazy { database.settingsDao() }

    companion object {
        private const val DEFAULT_USER_ID = "global"
        private var managers: MutableMap<String, SettingsManager> = mutableMapOf()

        fun getInstance(context: Context, userId: String = DEFAULT_USER_ID): SettingsManager {
            return managers[userId] ?: synchronized(this) {
                managers[userId] ?: SettingsManager(context).also { manager ->
                    managers[userId] = manager
                }
            }
        }

        fun removeInstance(userId: String = DEFAULT_USER_ID) {
            managers.remove(userId)
        }
    }

    suspend fun saveSetting(
        key: String,
        value: SettingValue,
        category: String = "general",
        isUserSpecific: Boolean = false,
        description: String = ""
    ) {
        withContext(Dispatchers.IO) {
            try {
                val entity = SettingEntity(
                    key,
                    value,
                    category,
                    isUserSpecific,
                    description
                )
                settingsDao.insertSetting(entity)
            } catch (e: Exception) {
                fallbackSave(key, value)
            }
        }
    }

    suspend fun getSetting(key: String): SettingValue? {
        return withContext(Dispatchers.IO) {
            try {
                settingsDao.getSetting(key)?.value
            } catch (e: Exception) {
                fallbackGet(key)
            }
        }
    }

    suspend fun deleteSetting(key: String) {
        withContext(Dispatchers.IO) {
            try {
                settingsDao.deleteSetting(key)
            } catch (e: Exception) {
                fallbackDelete(key)
            }
        }
    }

    suspend fun saveString(key: String, value: String, category: String = "general") {
        saveSetting(key, SettingValue.StringValue(value), category)
    }

    suspend fun getString(key: String, defaultValue: String = ""): String {
        return (getSetting(key) as? SettingValue.StringValue)?.value ?: defaultValue
    }

    suspend fun saveBoolean(key: String, value: Boolean, category: String = "general") {
        saveSetting(key, SettingValue.BooleanValue(value), category)
    }

    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return (getSetting(key) as? SettingValue.BooleanValue)?.value ?: defaultValue
    }

    suspend fun saveInt(key: String, value: Int, category: String = "general") {
        saveSetting(key, SettingValue.IntValue(value), category)
    }

    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return (getSetting(key) as? SettingValue.IntValue)?.value ?: defaultValue
    }

    suspend fun saveLong(key: String, value: Long, category: String = "general") {
        saveSetting(key, SettingValue.LongValue(value), category)
    }

    suspend fun getLong(key: String, defaultValue: Long = 0): Long {
        return (getSetting(key) as? SettingValue.LongValue)?.value ?: defaultValue
    }

    suspend fun saveStringList(key: String, value: List<String>, category: String = "general") {
        saveSetting(key, SettingValue.StringListValue(value), category)
    }

    suspend fun getStringList(key: String, defaultValue: List<String> = emptyList()): List<String> {
        return (getSetting(key) as? SettingValue.StringListValue)?.value ?: defaultValue
    }

    suspend fun saveSettings(settings: Map<String, SettingValue>, category: String = "general") {
        withContext(Dispatchers.IO) {
            try {
                val entities = settings.map { (key, value) ->
                    SettingEntity(key = key, value = value, category = category)
                }
                settingsDao.insertSettings(entities)
            } catch (e: Exception) {
                settings.forEach { (key, value) ->
                    fallbackSave(key, value)
                }
            }
        }
    }

    suspend fun getSettings(keys: List<String>): Map<String, SettingValue?> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = settingsDao.getSettings(keys)
                entities.associate { it.key to it.value }
            } catch (e: Exception) {
                keys.associateWith { fallbackGet(it) }
            }
        }
    }

    suspend fun getSettingsByCategory(category: String): Map<String, SettingValue> {
        return withContext(Dispatchers.IO) {
            try {
                settingsDao.getSettingsByCategory(category)
                    .associate { it.key to it.value }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun deleteSettingsByCategory(category: String) {
        withContext(Dispatchers.IO) {
            settingsDao.deleteSettingsByCategory(category)
        }
    }

    suspend fun migrateToUser(userId: String) {
        withContext(Dispatchers.IO) {
        }
    }

    suspend fun getStatistics(): SettingsStatistics {
        return withContext(Dispatchers.IO) {
            try {
                val count = settingsDao.getSettingsCount()
                val keys = settingsDao.getAllKeys()

                SettingsStatistics(
                    totalSettings = count,
                    categories = keys.groupBy { it.split("_").firstOrNull() ?: "other" }
                )
            } catch (e: Exception) {
                SettingsStatistics(0, emptyMap())
            }
        }
    }

    suspend fun exportSettings(): String {
        return withContext(Dispatchers.IO) {
            try {
                val allEntities = settingsDao.getAllKeys().mapNotNull { key ->
                    settingsDao.getSetting(key)
                }
                Json.encodeToString(allEntities)
            } catch (e: Exception) {
                "{}"
            }
        }
    }

    suspend fun importSettings(json: String) {
        withContext(Dispatchers.IO) {
            try {
                val entities = Json.decodeFromString<List<SettingEntity>>(json)
                settingsDao.insertSettings(entities)
            } catch (e: Exception) {
            }
        }
    }

    suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            settingsDao.getAllKeys().forEach { key ->
                settingsDao.deleteSetting(key)
            }

            setDefaultSettings()
        }
    }

    private suspend fun setDefaultSettings() {
        val defaults = mapOf(
            SettingsKey.ANNOUNCEMENT_DONE to SettingValue.BooleanValue(false)
        )

        saveSettings(defaults, "defaults")
    }

    private fun fallbackSave(key: String, value: SettingValue) {
        val prefs = context.getSharedPreferences("app_settings_fallback", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        when (value) {
            is SettingValue.StringValue -> editor.putString(key, value.value)
            is SettingValue.IntValue -> editor.putInt(key, value.value)
            is SettingValue.LongValue -> editor.putLong(key, value.value)
            is SettingValue.BooleanValue -> editor.putBoolean(key, value.value)
            is SettingValue.FloatValue -> editor.putFloat(key, value.value)
            else -> editor.putString(key, value.toJson())
        }

        editor.apply()
    }

    private fun fallbackGet(key: String): SettingValue? {
        val prefs = context.getSharedPreferences("app_settings_fallback", Context.MODE_PRIVATE)

        return if (prefs.contains(key)) {
            val all = prefs.all[key]
            when (all) {
                is String -> SettingValue.StringValue(all)
                is Int -> SettingValue.IntValue(all)
                is Long -> SettingValue.LongValue(all)
                is Boolean -> SettingValue.BooleanValue(all)
                is Float -> SettingValue.FloatValue(all)
                else -> null
            }
        } else {
            null
        }
    }

    private fun fallbackDelete(key: String) {
        val prefs = context.getSharedPreferences("app_settings_fallback", Context.MODE_PRIVATE)
        prefs.edit().remove(key).apply()
    }

    data class SettingsStatistics(
        val totalSettings: Int,
        val categories: Map<String, List<String>>
    )
}

fun MainActivity.getSettings(): SettingsManager {
    return SettingsManager.getInstance(this)
}