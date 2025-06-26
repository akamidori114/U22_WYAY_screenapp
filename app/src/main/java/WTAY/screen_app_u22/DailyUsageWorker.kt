package WTAY.screen_app_u22

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyUsageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val usageHelper = UsageStatsHelper(applicationContext)
            // suspend関数をそのまま呼び出す
            usageHelper.updateCumulativeUsage()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}