package WTAY.screen_app_u22

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class UsageStatsHelper(private val context: Context) {

    private val dataStore = AppDataStore(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager: PackageManager = context.packageManager

    fun getAppUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ).filter { it.totalTimeInForeground > 0 }
    }

    suspend fun updateCumulativeUsage() {
        val lastUpdateTime = dataStore.getLastUpdateTime()
        val currentTime = System.currentTimeMillis()

        val startTime = if (lastUpdateTime == 0L) {
            currentTime - TimeUnit.DAYS.toMillis(1)
        } else {
            lastUpdateTime
        }

        if (currentTime - startTime < TimeUnit.MINUTES.toMillis(1)) {
            return
        }

        val stats = getAppUsageStats(startTime, currentTime)
        if (stats.isEmpty()) {
            dataStore.saveUsageData(emptyMap(), currentTime)
            return
        }

        val currentData = dataStore.getAllUsageData().toMutableMap()
        currentData.remove(AppDataStore.KEY_LAST_UPDATE.name)


        stats.forEach { stat ->
            val currentTotal = currentData.getOrDefault(stat.packageName, 0L)
            currentData[stat.packageName] = currentTotal + stat.totalTimeInForeground
        }

        dataStore.saveUsageData(currentData, currentTime)
    }

    suspend fun getAllAppsTotalUsageTime(): Long {
        val allData = dataStore.getAllUsageData()
        var totalTime = 0L
        allData.forEach { (key, value) ->
            if (key != AppDataStore.KEY_LAST_UPDATE.name) {
                totalTime += value
            }
        }
        return totalTime
    }

    /**
     * 今日のハイライト（最多起動アプリ、時間帯別最長利用アプリ）を分析する
     */
    suspend fun analyzeTodayHighlights(): TodayHighlight {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

            val launchCounts = mutableMapOf<String, Int>()
            val usageTimeBySlot = mapOf(
                "morning" to mutableMapOf<String, Long>(),
                "day" to mutableMapOf<String, Long>(),
                "night" to mutableMapOf<String, Long>()
            )
            val appForegroundTimestamps = mutableMapOf<String, Long>()

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    val count = launchCounts.getOrDefault(event.packageName, 0)
                    launchCounts[event.packageName] = count + 1
                }

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        appForegroundTimestamps[event.packageName] = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val foregroundTime = appForegroundTimestamps[event.packageName]
                        if (foregroundTime != null) {
                            val duration = event.timeStamp - foregroundTime
                            if (duration > 0) {
                                val slot = getTimeSlot(foregroundTime)
                                val currentDuration = usageTimeBySlot[slot]!!.getOrDefault(event.packageName, 0L)
                                usageTimeBySlot[slot]!![event.packageName] = currentDuration + duration
                            }
                            appForegroundTimestamps.remove(event.packageName)
                        }
                    }
                }
            }

            val mostLaunchedEntry = launchCounts.maxByOrNull { it.value }
            val mostLaunchedApp = mostLaunchedEntry?.let {
                AppInfo(
                    packageName = it.key,
                    appName = getAppName(it.key),
                    launchCount = it.value
                )
            }

            val morningTop = getTopAppForSlot(usageTimeBySlot["morning"])
            val dayTop = getTopAppForSlot(usageTimeBySlot["day"])
            val nightTop = getTopAppForSlot(usageTimeBySlot["night"])
            val timeSlotUsage = TimeSlotUsage(morningTop, dayTop, nightTop)

            TodayHighlight(mostLaunchedApp, timeSlotUsage)
        }
    }

    private fun getTimeSlot(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "morning"
            in 12..17 -> "day"
            else -> "night"
        }
    }

    private fun getTopAppForSlot(usageMap: Map<String, Long>?): AppInfo? {
        val topEntry = usageMap?.maxByOrNull { it.value }
        return topEntry?.let {
            AppInfo(
                packageName = it.key,
                appName = getAppName(it.key),
                usageTime = it.value
            )
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}