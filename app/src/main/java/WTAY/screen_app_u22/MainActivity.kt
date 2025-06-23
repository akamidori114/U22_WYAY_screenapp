package WTAY.screen_app_u22

import com.google.android.material.appbar.MaterialToolbar
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageDetailsButton: Button
    private lateinit var weeklyUsageDetailsButton: Button
    private lateinit var monthlyUsageDetailsButton: Button // ▼▼▼ 追加 ▼▼▼
    private lateinit var totalUsageTextView: TextView
    private lateinit var usageButton: Button
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        usageHelper = UsageStatsHelper(this)

        dailyUsageDetailsButton = findViewById(R.id.dailyUsageDetailsButton)
        weeklyUsageDetailsButton = findViewById(R.id.weeklyUsageDetailsButton)
        monthlyUsageDetailsButton = findViewById(R.id.monthlyUsageDetailsButton) // ▼▼▼ 追加 ▼▼▼
        totalUsageTextView = findViewById(R.id.totalUsage)
        usageButton = findViewById(R.id.usageButton)
        permissionButton = findViewById(R.id.permissionButton)

        dailyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, DailyUsageDetailsActivity::class.java))
            } else {
                requestUsageStatsPermission()
            }
        }

        weeklyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, WeeklyUsageDetailsActivity::class.java))
            } else {
                requestUsageStatsPermission()
            }
        }

        // ▼▼▼ ここから追加 ▼▼▼
        monthlyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, MonthlyUsageDetailsActivity::class.java))
            } else {
                requestUsageStatsPermission()
            }
        }
        // ▲▲▲ ここまで追加 ▲▲▲

        usageButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                displayTotalUsage()
            } else {
                requestUsageStatsPermission()
            }
        }

        permissionButton.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            totalUsageTextView.text = "累計使用時間：更新中..."
            // UIスレッドをブロックしないように、別スレッドで更新処理を実行
            Thread {
                // 累計データを最新の状態に更新
                usageHelper.updateCumulativeUsage()
                // UIの更新はメインスレッドで行う必要がある
                runOnUiThread {
                    // 最新化されたデータを表示
                    displayTotalUsage()
                }
            }.start()
        } else {
            totalUsageTextView.text = "累計使用時間：権限が必要です"
        }
    }

    private fun displayTotalUsage() {
        val totalAccumulatedTime = usageHelper.getAllAppsTotalUsageTime()
        totalUsageTextView.text = "累計使用時間：${formatMillisToHoursMinutes(totalAccumulatedTime)}"
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Binder.getCallingUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun formatMillisToHoursMinutes(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}時間 ${minutes}分"
            minutes > 0 -> "${minutes}分"
            millis > 0 -> "< 1分"
            else -> "0分"
        }
    }
}