// このファイルはプロジェクトのトップレベルのビルドファイルです。
// 各モジュールにプラグインを適用するには、モジュールレベルの build.gradle.kts ファイルを
// 使用してください。

plugins {
    // これらのプラグインは :app モジュールで適用されるため、
    // ここでは `apply false` を付けて宣言だけを行います。
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}