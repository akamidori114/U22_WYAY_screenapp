package WTAY.screen_app_u22

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import WTAY.screen_app_u22.UsageStatsHelper

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageDetailsButton: Button
    private lateinit var weeklyUsageDetailsButton: Button
    private lateinit var monthlyUsageDetailsButton: Button
    private lateinit var totalUsageTextView: TextView
    private lateinit var usageButton: Button
    private lateinit var permissionButton: Button
    private lateinit var alertSettingsButton: Button

    // ハイライト表示用のUIプロパティ
    private lateinit var highlightCard: MaterialCardView
    private lateinit var mostLaunchedLayout: LinearLayout
    private lateinit var mostLaunchedAppName: TextView
    private lateinit var timeSlotMorning: TextView
    private lateinit var timeSlotDay: TextView
    private lateinit var timeSlotNight: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        usageHelper = UsageStatsHelper(this)

        // findViewById
        dailyUsageDetailsButton = findViewById(R.id.dailyUsageDetailsButton)
        weeklyUsageDetailsButton = findViewById(R.id.weeklyUsageDetailsButton)
        monthlyUsageDetailsButton = findViewById(R.id.monthlyUsageDetailsButton)
        totalUsageTextView = findViewById(R.id.totalUsage)
        usageButton = findViewById(R.id.usageButton)
        permissionButton = findViewById(R.id.permissionButton)
        alertSettingsButton = findViewById(R.id.alertSettingsButton)

        // ハイライトUIのfindViewById
        highlightCard = findViewById(R.id.highlightCard)
        mostLaunchedLayout = findViewById(R.id.mostLaunchedLayout)
        mostLaunchedAppName = findViewById(R.id.mostLaunchedAppName)
        timeSlotMorning = findViewById(R.id.timeSlotMorning)
        timeSlotDay = findViewById(R.id.timeSlotDay)
        timeSlotNight = findViewById(R.id.timeSlotNight)

        // setOnClickListener
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

        monthlyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, MonthlyUsageDetailsActivity::class.java))
            } else {
                requestUsageStatsPermission()
            }
        }

        usageButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                updateAndDisplayData()
            } else {
                requestUsageStatsPermission()
            }
        }

        permissionButton.setOnClickListener {
            requestUsageStatsPermission()
        }

        alertSettingsButton.setOnClickListener {
            startActivity(Intent(this, AlertSettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            updateAndDisplayData()
        } else {
            totalUsageTextView.text = "累計使用時間：権限が必要です"
            highlightCard.visibility = View.GONE
        }
    }

    private fun updateAndDisplayData() {
        lifecycleScope.launch {
            totalUsageTextView.text = "累計使用時間：更新中..."
            highlightCard.visibility = View.GONE

            // 累計時間の更新と表示
            usageHelper.updateCumulativeUsage()
            val totalTime = usageHelper.getAllAppsTotalUsageTime()
            totalUsageTextView.text = "累計使用時間：${formatMillisToHoursMinutes(totalTime)}"

            // ハイライトの分析と表示
            val highlights = usageHelper.analyzeTodayHighlights()
            displayHighlights(highlights)
        }
    }

    private fun displayHighlights(highlights: TodayHighlight) {
        var isHighlightAvailable = false

        // 最多起動アプリの表示
        highlights.mostLaunchedApp?.let {
            if (it.launchCount > 0) {
                mostLaunchedAppName.text = "${it.appName} (${it.launchCount}回)"
                mostLaunchedLayout.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else {
                mostLaunchedLayout.visibility = View.GONE
            }
        } ?: run { mostLaunchedLayout.visibility = View.GONE }

        // 時間帯別利用の表示
        val ts = highlights.timeSlotUsage
        ts.morning?.let {
            if(it.usageTime > 0) {
                timeSlotMorning.text = "朝：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotMorning.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else {
                timeSlotMorning.visibility = View.GONE
            }
        } ?: run { timeSlotMorning.visibility = View.GONE }

        ts.day?.let {
            if(it.usageTime > 0) {
                timeSlotDay.text = "昼：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotDay.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else {
                timeSlotDay.visibility = View.GONE
            }
        } ?: run { timeSlotDay.visibility = View.GONE }

        ts.night?.let {
            if(it.usageTime > 0) {
                timeSlotNight.text = "夜：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotNight.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else {
                timeSlotNight.visibility = View.GONE
            }
        } ?: run { timeSlotNight.visibility = View.GONE }

        // 何か一つでも表示する情報があればカード全体を表示
        if (isHighlightAvailable) {
            highlightCard.visibility = View.VISIBLE
        }
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