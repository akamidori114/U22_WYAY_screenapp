package WTAY.screen_app_u22

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.util.*

class MonthlyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var monthlyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "今月のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        monthlyUsageRecyclerView = findViewById(R.id.monthlyUsageRecyclerView)
        monthlyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayMonthlyUsageDetails()
    }

    private fun displayMonthlyUsageDetails() {
        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()

        // 開始時刻を「今月の1日0時」に設定
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val dailyStatsOverMonth = usageHelper.getAppUsageStats(startTime, endTime)

        val monthlyAggregatedStats = mutableMapOf<String, Long>()
        dailyStatsOverMonth.forEach { stat ->
            val currentTotal = monthlyAggregatedStats.getOrDefault(stat.packageName, 0L)
            monthlyAggregatedStats[stat.packageName] = currentTotal + stat.totalTimeInForeground
        }

        val displayList = monthlyAggregatedStats.mapNotNull { entry ->
            getAppNameFromPackage(entry.key)?.let { appName ->
                AppUsageDisplayItem(entry.key, appName, entry.value)
            }
        }
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        val adapter = UsageListAdapter(this, displayList)
        monthlyUsageRecyclerView.adapter = adapter
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