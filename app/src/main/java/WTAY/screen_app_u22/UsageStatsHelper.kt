package WTAY.screen_app_u22

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences // SharedPreferencesをインポート

class UsageStatsHelper(private val context: Context) {

    // SharedPreferencesのインスタンスをクラスレベルで保持
    private val prefs: SharedPreferences = context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)

    fun getAppUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        return stats.filter { it.totalTimeInForeground > 0 }
    }

    fun saveTotalUsageTime(appPackage: String, time: Long) {
        val current = prefs.getLong(appPackage, 0L)
        // 注意: この実装では、displayUsageが呼ばれるたびに「その時点での過去24時間の利用時間」が加算されます。
        // 厳密な「今日の利用時間」を1日1回だけ加算する仕組みにするには、日付の管理が必要です。
        // 今回は、まず表示連携を優先し、このロジックはそのままにします。
        prefs.edit().putLong(appPackage, current + time).apply()
    }

    // 全アプリの累計利用時間の合計を取得する関数
    fun getAllAppsTotalUsageTime(): Long {
        var totalTime = 0L
        prefs.all.forEach { entry ->
            // SharedPreferencesには様々なデータ型が保存される可能性があるため、Long型のみを対象とする
            if (entry.value is Long) {
                totalTime += entry.value as Long
            }
        }
        return totalTime
    }

    // (オプション) 特定のアプリの累計利用時間を取得する関数
    fun getTotalUsageTimeForApp(appPackage: String): Long {
        return prefs.getLong(appPackage, 0L)
    }
}