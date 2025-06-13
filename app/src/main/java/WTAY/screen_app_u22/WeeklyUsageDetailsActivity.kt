package WTAY.screen_app_u22

import com.google.android.material.appbar.MaterialToolbar
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.util.concurrent.TimeUnit

class WeeklyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var weeklyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar) // XML側でIDをdetailsToolbarとする
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (this is WeeklyUsageDetailsActivity) "今日のアプリ利用履歴" else "今週のアプリ利用履歴"

        supportActionBar?.title = "今週のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        weeklyUsageRecyclerView = findViewById(R.id.weeklyUsageRecyclerView)
        weeklyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayWeeklyUsageDetails()
    }

    private fun displayWeeklyUsageDetails() {
        val calendar = Calendar.getInstance()
        // 週の終わりを現在時刻とする
        val endTime = System.currentTimeMillis()
        // 週の始まりを計算 (例: 今日の日付から過去6日前の0時0分0秒)
        // または、カレンダーの週の始まり（通常は日曜日）に設定
        calendar.add(Calendar.DAY_OF_YEAR, -6) // 過去7日間（今日を含む）
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // INTERVAL_DAILYで過去7日分を取得し、アプリごとに集計
        val dailyStatsOverWeek = usageHelper.getAppUsageStats(startTime, endTime)

        val weeklyAggregatedStats = mutableMapOf<String, Long>()
        dailyStatsOverWeek.forEach { stat ->
            val currentTotal = weeklyAggregatedStats.getOrDefault(stat.packageName, 0L)
            weeklyAggregatedStats[stat.packageName] = currentTotal + stat.totalTimeInForeground
        }

        val displayList = weeklyAggregatedStats.mapNotNull { entry ->
            getAppNameFromPackage(entry.key)?.let { appName ->
                AppUsageDisplayItem(entry.key, appName, entry.value)
            }
        }
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        val adapter = UsageListAdapter(this, displayList)
        weeklyUsageRecyclerView.adapter = adapter
    }

    private fun getAppNameFromPackage(packageName: String): String? {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}