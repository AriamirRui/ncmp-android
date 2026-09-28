package com.ncmp.partner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmp.partner.BuildConfig
import com.ncmp.partner.ui.glass.GlassSurface
import com.ncmp.partner.ui.glass.LiquidState
import com.ncmp.partner.ui.theme.GlassPalette
import com.ncmp.partner.vm.UiState

@Composable
fun AboutScreen(
    state: UiState,
    liquid: LiquidState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))

        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            Text(
                "网易云音乐合伙人",
                color = GlassPalette.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Android 版 v${BuildConfig.VERSION_NAME}",
                color = GlassPalette.TextTertiary,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "基于 ACAne0320/ncmp 移植，核心评分逻辑与桌面版保持一致；" +
                    "界面采用 Jetpack Compose 实现的液态玻璃质感（背景折射模糊 + 流动高光）。",
                color = GlassPalette.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
            )
        }

        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            InfoRow("运行状态", if (state.busy) "任务执行中" else "空闲")
            InfoRow("账号", state.nickname.ifBlank { "未登录" })
            InfoRow("Cookie", if (state.config.hasCookie) "已配置" else "未配置")
            InfoRow("邮件通知", if (state.config.mailConfigured) "已开启" else "未开启")
            InfoRow("每日自动运行", if (state.config.autoDaily) "${state.config.autoHour}:00" else "关闭")
            InfoRow("评分策略", scoreLabel(state.config.score))
            InfoRow(
                "等待时间",
                "${state.config.waitMin.toInt()}-${state.config.waitMax.toInt()} 秒",
            )
        }

        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            Text("说明", color = GlassPalette.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            listOf(
                "· 每日任务：完成 5 个基础评分任务",
                "· 额外任务：最多 15 个额外评分（含听歌上报）",
                "· 任务可随时在「运行」页终止",
                "· 运行日志保存在应用私有目录，可在「历史」中查看",
                "· 安卓端不含 GitHub Secrets 自动更新（桌面版功能）",
                "· 本工具仅供学习交流，使用产生的一切后果由使用者自行承担",
            ).forEach { line ->
                Text(
                    line,
                    color = GlassPalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(label, color = GlassPalette.TextTertiary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = GlassPalette.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}

private fun scoreLabel(score: Int): String = when (score) {
    1 -> "1-2 分"
    2 -> "2-3 分"
    3 -> "3-4 分（默认）"
    else -> "固定 4 分"
}
