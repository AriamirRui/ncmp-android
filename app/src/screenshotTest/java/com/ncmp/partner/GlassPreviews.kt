package com.ncmp.partner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ncmp.partner.data.Config
import com.ncmp.partner.data.RunRecord
import com.ncmp.partner.ui.glass.LiquidBackground
import com.ncmp.partner.ui.glass.rememberLiquidState
import com.ncmp.partner.ui.screens.AboutScreen
import com.ncmp.partner.ui.screens.ConfigScreen
import com.ncmp.partner.ui.screens.HistoryScreen
import com.ncmp.partner.ui.screens.RunScreen
import com.ncmp.partner.ui.theme.NcmpTheme
import com.ncmp.partner.vm.UiLogLine
import com.ncmp.partner.vm.UiState

/**
 * 界面预览（同时用于 Compose 截图测试，`gradle updateDebugScreenshotTest`）。
 * 液态玻璃效果依赖真实布局与绘制，因此这里用完整页面而非孤立组件。
 */

private fun previewState(busy: Boolean) = UiState(
    config = Config(
        musicU = "MUSIC_U_DEMO_VALUE",
        csrf = "demo-csrf",
        notifyEmail = "demo@example.com",
        autoDaily = true,
    ),
    busy = busy,
    stage = if (busy) "正在进行每日评分任务..." else "就绪",
    accountState = "Cookie 有效",
    accountOk = true,
    nickname = "云村用户",
    dailyDone = 3,
    dailyTotal = 5,
    extraDone = 7,
    extraTotal = 15,
    lastResult = "执行成功",
    lastSuccess = true,
    logs = listOf(
        UiLogLine("SYS", "—— 开始执行每日任务 ——"),
        UiLogLine("INFO", "09:12:01 [INFO] 开始验证用户信息..."),
        UiLogLine("INFO", "09:12:02 [INFO] 用户名: 云村用户"),
        UiLogLine("INFO", "09:12:03 [INFO] 今日任务：未完成[0/5]"),
        UiLogLine("INFO", "09:12:05 [INFO] 开始评分..."),
        UiLogLine("INFO", "09:12:25 [INFO] 晴天「周杰伦」评分完成：4分"),
        UiLogLine("WARNING", "09:12:45 [WARNING] 遇到频率限制，等待 18.2 秒后重试..."),
        UiLogLine("INFO", "09:13:05 [INFO] Lemon「米津玄師」评分完成：4分"),
        UiLogLine("ERROR", "09:13:25 [ERROR] 歌曲「测试」资源状态异常，跳过"),
        UiLogLine("INFO", "09:13:45 [INFO] 成功完成第 7/15 个额外评分任务"),
    ),
)

private val previewHistory = listOf(
    RunRecord("20260101_091201_ab12cd", "2026-01-01 09:12:01", "任务运行", true, "执行成功"),
    RunRecord("20251231_091501_ef34gh", "2025-12-31 09:15:01", "任务运行", false, "任务已被用户终止"),
    RunRecord("20251230_091801_ij56kl", "2025-12-30 09:18:01", "任务运行", false, "Cookie 已失效"),
)

@Composable
private fun PreviewScaffold(content: @Composable () -> Unit) {
    NcmpTheme {
        Box(Modifier.fillMaxSize()) {
            val liquid = rememberLiquidState()
            LiquidBackground(liquid, Modifier.fillMaxSize())
            Box(Modifier.padding(0.dp)) { content() }
        }
    }
}

@Preview(name = "运行页", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun RunScreenPreview() {
    PreviewScaffold {
        val liquid = rememberLiquidState()
        RunScreen(
            state = previewState(busy = false),
            liquid = liquid,
            onStart = {}, onCancel = {}, onValidate = {}, onRefreshCookie = {}, onClearLogs = {},
        )
    }
}

@Preview(name = "运行中", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun RunScreenBusyPreview() {
    PreviewScaffold {
        val liquid = rememberLiquidState()
        RunScreen(
            state = previewState(busy = true).copy(cancelling = false),
            liquid = liquid,
            onStart = {}, onCancel = {}, onValidate = {}, onRefreshCookie = {}, onClearLogs = {},
        )
    }
}

@Preview(name = "配置页", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun ConfigScreenPreview() {
    PreviewScaffold {
        val liquid = rememberLiquidState()
        ConfigScreen(
            state = previewState(busy = false),
            liquid = liquid,
            onSave = {}, onSaveAndValidate = {},
        )
    }
}

@Preview(name = "历史页", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun HistoryScreenPreview() {
    PreviewScaffold {
        val liquid = rememberLiquidState()
        HistoryScreen(
            state = previewState(busy = false).copy(history = previewHistory),
            liquid = liquid,
            onRefresh = {}, onClear = {}, onOpen = {}, onDelete = {}, onCloseDetail = {},
        )
    }
}

@Preview(name = "关于页", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun AboutScreenPreview() {
    PreviewScaffold {
        val liquid = rememberLiquidState()
        AboutScreen(state = previewState(busy = false), liquid = liquid)
    }
}
