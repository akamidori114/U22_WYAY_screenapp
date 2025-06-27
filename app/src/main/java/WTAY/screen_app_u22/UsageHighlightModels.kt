package WTAY.screen_app_u22

// 共通で使うアプリ情報
data class AppInfo(
    val packageName: String,
    val appName: String,
    val usageTime: Long = 0,
    val launchCount: Int = 0
)

// 時間帯ごとのトップ利用アプリ
data class TimeSlotUsage(
    val morning: AppInfo?,
    val day: AppInfo?,
    val night: AppInfo?
)

// 今日のハイライトをまとめるクラス
data class TodayHighlight(
    val mostLaunchedApp: AppInfo?,
    val timeSlotUsage: TimeSlotUsage
)