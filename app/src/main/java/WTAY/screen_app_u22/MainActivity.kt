package WTAY.screen_app_u22

import com.google.android.material.appbar.MaterialToolbar
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.provider.Settings
import android.widget.Button // Buttonをインポート
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    // private lateinit var dailyUsageTextView: TextView // 削除
    // private lateinit var weeklyUsageTextView: TextView // 削除
    private lateinit var dailyUsageDetailsButton: Button // 追加
    private lateinit var weeklyUsageDetailsButton: Button // 追加
    private lateinit var totalUsageTextView: TextView
    private lateinit var usageButton: Button
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar) // NoActionBarテーマのため手動でセット

        usageHelper = UsageStatsHelper(this)

        // TextViewとButtonをfindViewByIdで取得
        dailyUsageDetailsButton = findViewById(R.id.dailyUsageDetailsButton) // 修正
        weeklyUsageDetailsButton = findViewById(R.id.weeklyUsageDetailsButton) // 修正
        totalUsageTextView = findViewById(R.id.totalUsage)
        usageButton = findViewById(R.id.usageButton)
        permissionButton = findViewById(R.id.permissionButton)

        // ボタンのクリックリスナーを設定
        dailyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, DailyUsageDetailsActivity::class.java))
            } else {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        weeklyUsageDetailsButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, WeeklyUsageDetailsActivity::class.java))
            } else {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        usageButton.setOnClickListener {
            if (hasUsageStatsPermission()) {
                displayTotalUsage() // メイン画面の累計は表示更新
            } else {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        permissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            displayTotalUsage() // 累計利用時間のみをメイン画面で更新
            // 累計保存ロジックは詳細画面表示時ではなく、
            // 例えばバックグラウンドサービスで定期的に行うか、
            // このonResumeで直近24時間の利用分を加算するなどの工夫が必要。
            // 今回は、表示更新のみに絞ります。
            // 以前のsaveTotalUsageTimeの呼び出しは、詳細画面への遷移時に重複して
            // 呼ばれる可能性を避けるため、ここでは一旦コメントアウトします。
            // 必要に応じて、より適切なタイミングでの保存処理を検討してください。
            // updateTodaysUsageForCumulative(); // 例: 今日の利用分を累計に加算する関数
        } else {
            totalUsageTextView.text = "累計使用時間：権限が必要です"
        }
    }

    // メイン画面では累計利用時間のみを表示更新する
    private fun displayTotalUsage() {
        val totalAccumulatedTime = usageHelper.getAllAppsTotalUsageTime()
        totalUsageTextView.text = "累計使用時間：${formatMillisToHoursMinutes(totalAccumulatedTime)}"

        // (オプション) 累計保存ロジックをここに移動する場合
        // 1日に1回だけ実行するような制御が必要
        // updateTodaysUsageForCumulative();
    }

    // 今日の利用時間を取得し、累計に加算する（呼び出しタイミングの検討が必要）
    // この関数は、例えば1日の終わりに1回だけ呼び出されるようにするか、
    // 最後に累計を更新した日付と比較して、日付が変わっていたら更新するなどの工夫が必要
    private fun updateTodaysUsageForCumulative() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)
        val dailyStats = usageHelper.getAppUsageStats(startTime, endTime)

        for (stat in dailyStats) {
            // ここで `saveTotalUsageTime` を呼び出すと、onResumeのたびに
            // 直近24時間が加算されてしまう。
            // 正確な「今日の利用時間」を1回だけ加算するロジックが別途必要。
            // 今回のスコープでは、この関数の呼び出しは保留。
            // usageHelper.saveTotalUsageTime(stat.packageName, stat.totalTimeInForeground)
        }
    }


    // showAlertIfOverLimit は詳細画面で表示するか、通知機能として実装する方が適切かもしれません。
    // MainActivityからは一旦削除またはコメントアウトします。
    /*
    private fun showAlertIfOverLimit(appName: String, usageTime: Long, limit: Long) {
        if (usageTime > limit) {
            AlertDialog.Builder(this)
                .setTitle("利用時間超過")
                .setMessage("$appName の利用時間が ${formatMillisToMinutes(usageTime)} 分で、制限の${formatMillisToMinutes(limit)}分を超えました。")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    */

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Binder.getCallingUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppNameFromPackage(packageName: String): String? {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun formatMillisToMinutes(millis: Long): Long {
        return TimeUnit.MILLISECONDS.toMinutes(millis)
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