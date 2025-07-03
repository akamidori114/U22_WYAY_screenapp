package WTAY.screen_app_u22

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import java.util.*
import java.util.concurrent.TimeUnit

class WeeklyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var weeklyUsageRecyclerView: RecyclerView
    private lateinit var pieChart: PieChart // グラフの変数を追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_usage_details) // レイアウトも週次用に変更

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "今週のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        weeklyUsageRecyclerView = findViewById(R.id.weeklyUsageRecyclerView)
        pieChart = findViewById(R.id.pieChart) // レイアウトファイルにPieChartを追加する必要あり
        weeklyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayWeeklyUsageDetails()
    }

    private fun displayWeeklyUsageDetails() {
        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()

        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val dailyStatsOverWeek = usageHelper.getAppUsageStats(startTime, endTime)

        // 週次の集計ロジック
        val weeklyAggregatedStats = mutableMapOf<String, Long>()
        dailyStatsOverWeek.forEach { stat ->
            val currentTotal = weeklyAggregatedStats.getOrDefault(stat.packageName, 0L)
            weeklyAggregatedStats[stat.packageName] = currentTotal + stat.totalTimeInForeground
        }

        // 集計データをリストに変換
        val aggregatedList = weeklyAggregatedStats.map { (packageName, usageTime) ->
            SimpleUsageStat(packageName, usageTime)
        }

        // 円グラフをセットアップ
        setupPieChart(aggregatedList)

        // リスト表示のロジック
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

    private fun setupPieChart(stats: List<SimpleUsageStat>) {
        // ▼▼▼ 閾値を1時間に変更 ▼▼▼
        val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L

        val majorApps = stats
            .filter { it.totalTimeInForeground >= ONE_HOUR_IN_MILLIS }
            .sortedByDescending { it.totalTimeInForeground }

        val otherTime = stats
            .filter { it.totalTimeInForeground < ONE_HOUR_IN_MILLIS }
            .sumOf { it.totalTimeInForeground }

        val entries = ArrayList<PieEntry>()

        majorApps.forEach { stat ->
            val appName = getAppNameFromPackage(stat.packageName) ?: stat.packageName
            val appLabel = "$appName\n(${formatMillisForChart(stat.totalTimeInForeground)})"
            entries.add(PieEntry(stat.totalTimeInForeground.toFloat(), appLabel))
        }

        var hasOtherEntry = false
        if (otherTime > 0) {
            val otherLabel = "その他\n(${formatMillisForChart(otherTime)})"
            entries.add(PieEntry(otherTime.toFloat(), otherLabel))
            hasOtherEntry = true
        }

        if (entries.isEmpty()) {
            pieChart.visibility = android.view.View.GONE
            return
        } else {
            pieChart.visibility = android.view.View.VISIBLE
        }

        val dataSet = PieDataSet(entries, "App Usage")

        val colors = ArrayList<Int>()
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        if (hasOtherEntry) {
            colors.add(Color.LTGRAY)
        }
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setDrawValues(false)
        pieChart.data = data

        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(11f)
        pieChart.legend.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.rotationAngle = 270f

        pieChart.invalidate()
    }

    private fun formatMillisForChart(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "${hours}時間${minutes}分"
            minutes > 0 -> "${minutes}分"
            millis > 0 -> "1分"
            else -> "0分"
        }
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