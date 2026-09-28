plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Compose 预览截图测试（在 JVM 上用 Layoutlib 渲染，用于界面回归）
    id("com.android.compose.screenshot") version "0.0.1-alpha01" apply false
}
