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

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)

        // ▼▼▼ タイトル設定を修正 ▼▼▼
        supportActionBar?.title = "今週のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        weeklyUsageRecyclerView = findViewById(R.id.weeklyUsageRecyclerView)
        weeklyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayWeeklyUsageDetails()
    }

    private fun displayWeeklyUsageDetails() {
        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()

        // ▼▼▼ 開始時刻を「今週の月曜0時」に変更 ▼▼▼
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

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