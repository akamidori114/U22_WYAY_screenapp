package WTAY.screen_app_u22

data class AlertSetting(
    val packageName: String,
    val appName: String, // 表示用にアプリ名も保持
    val limitInMinutes: Long
)