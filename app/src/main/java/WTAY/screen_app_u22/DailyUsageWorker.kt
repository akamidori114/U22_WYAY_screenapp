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
            // Helperクラスを呼び出して、累計時間を更新する
            val usageHelper = UsageStatsHelper(applicationContext)
            usageHelper.updateCumulativeUsage()
            // 処理成功
            Result.success()
        } catch (e: Exception) {
            // 処理失敗
            Result.failure()
        }
    }
}