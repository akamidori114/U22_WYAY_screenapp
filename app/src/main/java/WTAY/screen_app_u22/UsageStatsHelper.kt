package WTAY.screen_app_u22

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

class UsageStatsHelper(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)

    companion object {
        // 最後に累計を更新した時刻を保存するためのキー
        const val KEY_LAST_CUMULATIVE_UPDATE_TIMESTAMP = "last_cumulative_update_timestamp"
    }

    fun getAppUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        return stats.filter { it.totalTimeInForeground > 0 }
    }

    // 累計利用時間を更新するメソッド (このメソッドがメインのロジック)
    fun updateCumulativeUsage() {
        val lastUpdateTime = prefs.getLong(KEY_LAST_CUMULATIVE_UPDATE_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        // 初回起動時（lastUpdateTimeが0）は、過去24時間分を初期データとする
        val startTime = if (lastUpdateTime == 0L) {
            currentTime - TimeUnit.DAYS.toMillis(1)
        } else {
            lastUpdateTime
        }

        // 最後に更新してから1分も経っていなければ処理をスキップ (頻繁な更新を防ぐため)
        if (currentTime - startTime < TimeUnit.MINUTES.toMillis(1)) {
            return
        }

        val stats = getAppUsageStats(startTime, currentTime)
        if (stats.isEmpty()) {
            // 更新するデータがない場合でも、タイムスタンプは更新して終了
            prefs.edit().putLong(KEY_LAST_CUMULATIVE_UPDATE_TIMESTAMP, currentTime).apply()
            return
        }

        val editor = prefs.edit()
        stats.forEach { stat ->
            if (stat.totalTimeInForeground > 0) {
                // SharedPreferencesに保存されている累計時間に、新しい利用時間を加算
                val currentTotal = prefs.getLong(stat.packageName, 0L)
                editor.putLong(stat.packageName, currentTotal + stat.totalTimeInForeground)
            }
        }

        // 最終更新時刻を現在時刻で保存
        editor.putLong(KEY_LAST_CUMULATIVE_UPDATE_TIMESTAMP, currentTime)
        editor.apply()
    }


    // 全アプリの累計利用時間の合計を取得する関数
    fun getAllAppsTotalUsageTime(): Long {
        var totalTime = 0L
        prefs.all.forEach { (key, value) ->
            // 値がLong型で、かつタイムスタンプのキーでなければ加算対象とする
            if (value is Long && key != KEY_LAST_CUMULATIVE_UPDATE_TIMESTAMP) {
                totalTime += value
            }
        }
        return totalTime
    }

    // 特定のアプリの累計利用時間を取得する関数
    fun getTotalUsageTimeForApp(appPackage: String): Long {
        return prefs.getLong(appPackage, 0L)
    }
}