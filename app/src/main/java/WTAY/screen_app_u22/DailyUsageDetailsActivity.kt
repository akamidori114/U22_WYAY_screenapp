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
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyUsageDetailsActivity : AppCompatActivity() {

    // ... (onCreateや他のメソッドは変更なし) ...
    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageRecyclerView: RecyclerView
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "今日のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        dailyUsageRecyclerView = findViewById(R.id.dailyUsageRecyclerView)
        pieChart = findViewById(R.id.pieChart)
        dailyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayDailyUsageDetails()
    }

    private fun displayDailyUsageDetails() {
        val endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val dailyStatsRaw = usageHelper.getAppUsageStats(startTime, endTime)

        setupPieChart(dailyStatsRaw)

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


    // ▼▼▼ このメソッドを丸ごと書き換える ▼▼▼
    private fun setupPieChart(stats: List<android.app.usage.UsageStats>) {
        val TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000L

        // 1. 10分以上のアプリを降順にソート
        val majorApps = stats
            .filter { it.totalTimeInForeground >= TEN_MINUTES_IN_MILLIS }
            .sortedByDescending { it.totalTimeInForeground }

        // 2. 10分未満のアプリの合計時間（その他）を計算
        val otherTime = stats
            .filter { it.totalTimeInForeground < TEN_MINUTES_IN_MILLIS }
            .sumOf { it.totalTimeInForeground }

        // 3. グラフ用のエントリリストを作成
        val entries = ArrayList<PieEntry>()

        // 4. 10分以上のアプリをリストに追加
        majorApps.forEach { stat ->
            val appName = getAppNameFromPackage(stat.packageName) ?: stat.packageName
            val appLabel = "$appName\n(${formatMillisForChart(stat.totalTimeInForeground)})"
            entries.add(PieEntry(stat.totalTimeInForeground.toFloat(), appLabel))
        }

        // 5. 「その他」のエントリを最後に追加
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

        // 6. データセットを作成
        val dataSet = PieDataSet(entries, "App Usage")

        // 7. 色の設定
        val colors = ArrayList<Int>()
        // 10分以上のアプリの色
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        // 「その他」がある場合は、最後に灰色を追加
        if (hasOtherEntry) {
            colors.add(Color.LTGRAY)
        }
        dataSet.colors = colors

        // 8. データをグラフに設定
        val data = PieData(dataSet)
        data.setDrawValues(false) // パーセント表示は不要
        pieChart.data = data

        // 9. グラフの見た目を調整
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(11f)
        pieChart.legend.isEnabled = false
        pieChart.isRotationEnabled = false // 回転無効化

        // 10. グラフの開始位置を調整
        // デフォルトでは3時の位置から描画が始まる
        // 12時の位置から始めるには、-90度（または270度）回転させる
        pieChart.rotationAngle = 270f

        // 11. 更新
        pieChart.invalidate()
    }
    // ▲▲▲ メソッド書き換えここまで ▲▲▲

    // ... (formatMillisForChart, getAppNameFromPackage, onSupportNavigateUp は変更なし) ...
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