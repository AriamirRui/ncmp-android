package com.ncmp.partner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmp.partner.ui.components.GlassButton
import com.ncmp.partner.ui.components.GlassChip
import com.ncmp.partner.ui.components.StatTile
import com.ncmp.partner.ui.components.ThinProgress
import com.ncmp.partner.ui.glass.GlassSurface
import com.ncmp.partner.ui.glass.LiquidState
import com.ncmp.partner.ui.theme.GlassPalette
import com.ncmp.partner.vm.UiLogLine
import com.ncmp.partner.vm.UiState

private val LOG_FILTERS = listOf("全部" to "ALL", "警告" to "WARN", "错误" to "ERROR")

@Composable
fun RunScreen(
    state: UiState,
    liquid: LiquidState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onValidate: () -> Unit,
    onRefreshCookie: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))

        // ---------------- 账号卡片 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.nickname.take(1).ifBlank { "♪" },
                        color = GlassPalette.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.nickname.ifBlank { "未登录" },
                        color = GlassPalette.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val chipColor = when (state.accountOk) {
                            true -> GlassPalette.Success
                            false -> GlassPalette.Error
                            null -> GlassPalette.TextSecondary
                        }
                        GlassChip(state.accountState, chipColor)
                        if (state.lastResult.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.lastResult,
                                color = if (state.lastSuccess == true) GlassPalette.Success
                                else GlassPalette.TextTertiary,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // ---------------- 进度卡片 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.stage, color = GlassPalette.TextSecondary, fontSize = 12.5.sp)
                Text(
                    text = overallPercent(state).let { "$it%" },
                    color = GlassPalette.TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatTile(
                    label = "每日评分",
                    value = "${state.dailyDone}/${state.dailyTotal}",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "额外评分",
                    value = "${state.extraDone}/${state.extraTotal}",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            ThinProgress(fraction = state.dailyDone.toFloat() / state.dailyTotal.coerceAtLeast(1))
            Spacer(Modifier.height(6.dp))
            ThinProgress(
                fraction = state.extraDone.toFloat() / state.extraTotal.coerceAtLeast(1),
                color = GlassPalette.Violet,
            )
        }

        // ---------------- 操作按钮 ----------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = if (state.busy) "运行中…" else "开始任务",
                onClick = onStart,
                enabled = !state.busy,
                accent = true,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            )
            GlassButton(
                text = if (state.cancelling) "终止中…" else "终止",
                onClick = onCancel,
                enabled = state.busy && !state.cancelling,
                danger = true,
                modifier = Modifier.weight(0.62f),
                icon = { Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = if (state.validating) "验证中…" else "验证 Cookie",
                onClick = onValidate,
                enabled = !state.busy && !state.validating,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.Verified, null, tint = GlassPalette.TextPrimary, modifier = Modifier.size(17.dp)) },
            )
            GlassButton(
                text = "刷新 Cookie",
                onClick = onRefreshCookie,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.Refresh, null, tint = GlassPalette.TextPrimary, modifier = Modifier.size(17.dp)) },
            )
        }

        // ---------------- 日志 ----------------
        GlassSurface(
            state = liquid,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = 14.dp,
        ) {
            var filter by remember { mutableStateOf("ALL") }
            val listState = rememberLazyListState()
            val clipboard = LocalClipboardManager.current

            val visible = remember(state.logs, filter) {
                state.logs.filter { line ->
                    when (filter) {
                        "WARN" -> line.level == "WARNING" || line.level == "ERROR"
                        "ERROR" -> line.level == "ERROR"
                        else -> true
                    }
                }
            }

            LaunchedEffect(visible.size) {
                if (visible.isNotEmpty()) listState.animateScrollToItem(visible.size - 1)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("实时日志", color = GlassPalette.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LOG_FILTERS.forEach { (label, key) ->
                        Box(
                            Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (filter == key) GlassPalette.Accent.copy(alpha = 0.22f)
                                    else Color(0x14FFFFFF)
                                )
                                .clickable { filter = key }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                label,
                                color = if (filter == key) GlassPalette.AccentSoft else GlassPalette.TextTertiary,
                                fontSize = 10.5.sp,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.ContentCopy, "复制日志",
                        tint = GlassPalette.TextTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(state.logs.joinToString("\n") { it.text }))
                            },
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.Delete, "清空日志",
                        tint = GlassPalette.TextTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClearLogs() },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无日志，点击「开始任务」后会实时显示", color = GlassPalette.TextTertiary, fontSize = 12.sp)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(visible) { line -> LogRow(line) }
                }
            }
        }
    }
}

@Composable
private fun LogRow(line: UiLogLine) {
    val color = when (line.level) {
        "ERROR" -> GlassPalette.Error
        "WARNING" -> GlassPalette.Warning
        "SYS" -> GlassPalette.Cyan
        else -> GlassPalette.TextSecondary
    }
    Text(
        text = line.text,
        color = color,
        fontSize = 11.5.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
    )
}

private fun overallPercent(state: UiState): Int {
    val total = state.dailyTotal + state.extraTotal
    if (total <= 0) return 0
    val done = state.dailyDone + state.extraDone
    val percent = (done * 100f / total).toInt()
    return if (state.lastSuccess == true && !state.busy) 100 else percent.coerceIn(0, 100)
}
