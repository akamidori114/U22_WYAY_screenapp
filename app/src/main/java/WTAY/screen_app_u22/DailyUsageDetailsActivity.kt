package WTAY.screen_app_u22

import com.google.android.material.appbar.MaterialToolbar
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)

        // ▼▼▼ タイトル設定を修正 ▼▼▼
        supportActionBar?.title = "今日のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        dailyUsageRecyclerView = findViewById(R.id.dailyUsageRecyclerView)
        dailyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayDailyUsageDetails()
    }

    private fun displayDailyUsageDetails() {
        val endTime = System.currentTimeMillis()
        // ▼▼▼ 開始時刻を「今日の0時」に変更 ▼▼▼
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val dailyStatsRaw = usageHelper.getAppUsageStats(startTime, endTime)
        val displayList = dailyStatsRaw.mapNotNull { stat ->
            getAppNameFromPackage(stat.packageName)?.let { appName ->
                AppUsageDisplayItem(stat.packageName, appName, stat.totalTimeInForeground)
            }
        }
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        val adapter = UsageListAdapter(this, displayList)
        dailyUsageRecyclerView.adapter = adapter
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