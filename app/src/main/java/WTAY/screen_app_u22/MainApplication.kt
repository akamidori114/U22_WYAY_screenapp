package WTAY.screen_app_u22

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        // 24時間に1回実行する定期的なリクエストを作成
        val repeatingRequest = PeriodicWorkRequestBuilder<DailyUsageWorker>(1, TimeUnit.DAYS)
            .build()

        // WorkManagerにタスクを登録する
        // enqueueUniquePeriodicWorkを使うと、同じタスクが複数登録されるのを防げる
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyUsageWorker", // タスクのユニークな名前
            ExistingPeriodicWorkPolicy.KEEP, // 既にタスクがあれば何もしない
            repeatingRequest
        )
    }
}