package WTAY.screen_app_u22

import com.google.android.material.appbar.MaterialToolbar
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class DailyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar) // XML側でIDをdetailsToolbarとする
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (this is DailyUsageDetailsActivity) "今日のアプリ利用履歴" else "今週のアプリ利用履歴"

        // アクションバーにタイトルを設定 (オプション)
        supportActionBar?.title = "今日のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 戻るボタン

        usageHelper = UsageStatsHelper(this)
        dailyUsageRecyclerView = findViewById(R.id.dailyUsageRecyclerView)
        dailyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayDailyUsageDetails()
    }

    private fun displayDailyUsageDetails() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1) // 過去24時間

        val dailyStatsRaw = usageHelper.getAppUsageStats(startTime, endTime)
        val displayList = dailyStatsRaw.mapNotNull { stat ->
            getAppNameFromPackage(stat.packageName)?.let { appName ->
                AppUsageDisplayItem(stat.packageName, appName, stat.totalTimeInForeground)
            }
        }
            .filter { it.totalTimeInForeground > 0 } // 利用時間があるもののみ
            .sortedByDescending { it.totalTimeInForeground } // 利用時間が多い順

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

    // アクションバーの戻るボタンの処理
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}