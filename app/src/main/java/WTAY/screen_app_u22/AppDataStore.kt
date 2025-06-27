package WTAY.screen_app_u22

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_data")

class AppDataStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_LAST_UPDATE = longPreferencesKey("last_cumulative_update_timestamp")
        val KEY_ALERT_SETTINGS = stringPreferencesKey("alert_settings")
    }

    // すべての利用時間データをMapとして取得する
    suspend fun getAllUsageData(): Map<String, Long> {
        return context.dataStore.data.map { preferences ->
            preferences.asMap().mapNotNull { (key, value) ->
                if (value is Long) key.name to value else null
            }.toMap()
        }.first()
    }

    // 指定したキー（パッケージ名）の利用時間を取得する
    suspend fun getUsageTime(packageName: String): Long {
        val key = longPreferencesKey(packageName)
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0L
        }.first()
    }

    // 複数の利用時間データをまとめて保存する
    suspend fun saveUsageData(data: Map<String, Long>, lastUpdateTime: Long) {
        context.dataStore.edit { preferences ->
            data.forEach { (packageName, time) ->
                val key = longPreferencesKey(packageName)
                preferences[key] = time
            }
            preferences[KEY_LAST_UPDATE] = lastUpdateTime
        }
    }

    // 最終更新時刻を取得する
    suspend fun getLastUpdateTime(): Long {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_LAST_UPDATE] ?: 0L
        }.first()
    }

    // アラート設定のリストをFlowとして取得する
    fun getAlertSettingsFlow(): Flow<List<AlertSetting>> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[KEY_ALERT_SETTINGS] ?: "[]"
            val type = object : TypeToken<List<AlertSetting>>() {}.type
            gson.fromJson(jsonString, type)
        }
    }

    // アラート設定を追加または更新する
    suspend fun addOrUpdateAlertSetting(setting: AlertSetting) {
        context.dataStore.edit { preferences ->
            val currentSettings = getAlertSettingsFlow().first().toMutableList()
            // 既存の設定があれば削除
            currentSettings.removeAll { it.packageName == setting.packageName }
            // 新しい設定を追加
            currentSettings.add(setting)
            // JSONに変換して保存
            preferences[KEY_ALERT_SETTINGS] = gson.toJson(currentSettings)
        }
    }

    // アラート設定を削除する
    suspend fun removeAlertSetting(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentSettings = getAlertSettingsFlow().first().toMutableList()
            currentSettings.removeAll { it.packageName == packageName }
            preferences[KEY_ALERT_SETTINGS] = gson.toJson(currentSettings)
        }
    }
}