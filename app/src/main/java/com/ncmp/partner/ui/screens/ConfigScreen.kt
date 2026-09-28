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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmp.partner.data.Config
import com.ncmp.partner.ui.components.GlassButton
import com.ncmp.partner.ui.components.GlassTextField
import com.ncmp.partner.ui.glass.GlassSurface
import com.ncmp.partner.ui.glass.LiquidState
import com.ncmp.partner.ui.theme.GlassPalette
import com.ncmp.partner.vm.NcmpViewModel
import com.ncmp.partner.vm.UiState

@Composable
fun ConfigScreen(
    state: UiState,
    liquid: LiquidState,
    vm: NcmpViewModel,
    modifier: Modifier = Modifier,
) {
    // 本地编辑态，点击保存后写回
    var draft by remember(state.config) { mutableStateOf(state.config) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))

        // ---------------- Cookie ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("网易云账户 Cookie", "必需")
            GlassTextField(
                value = draft.musicU,
                onValueChange = { draft = draft.copy(musicU = it.trim()) },
                label = "MUSIC_U",
                placeholder = "登录 music.163.com 后从 Cookie 复制",
                mono = true,
                isPassword = true,
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = draft.csrf,
                onValueChange = { draft = draft.copy(csrf = it.trim()) },
                label = "__csrf",
                placeholder = "同一 Cookie 中的 __csrf",
                mono = true,
                isPassword = true,
            )
        }

        // ---------------- 任务设置 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("任务设置")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = draft.waitMin.toInt().toString(),
                    onValueChange = { draft = draft.copy(waitMin = it.toDoubleOrNull() ?: draft.waitMin) },
                    label = "最短等待（秒）",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    showToggle = false,
                )
                GlassTextField(
                    value = draft.waitMax.toInt().toString(),
                    onValueChange = { draft = draft.copy(waitMax = it.toDoubleOrNull() ?: draft.waitMax) },
                    label = "最长等待（秒）",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    showToggle = false,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("评分策略", color = GlassPalette.TextTertiary, fontSize = 11.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "1-2分", 2 to "2-3分", 3 to "3-4分", 4 to "固定4分").forEach { (value, label) ->
                    GlassButton(
                        text = label,
                        onClick = { draft = draft.copy(score = value) },
                        accent = draft.score == value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "按歌名/歌手是否含英文决定分数区间，等待时间用于规避风控。",
                color = GlassPalette.TextTertiary,
                fontSize = 10.5.sp,
            )
        }

        // ---------------- 每日自动运行 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("每日自动运行", "可选")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = if (draft.autoDaily) "已开启" else "已关闭",
                    onClick = { draft = draft.copy(autoDaily = !draft.autoDaily) },
                    accent = draft.autoDaily,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = "${draft.autoHour}:00",
                    onClick = { draft = draft.copy(autoHour = (draft.autoHour + 1) % 24) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "开启后每天在指定时间自动执行（需保持 Cookie 有效，系统可能延迟几分钟）。",
                color = GlassPalette.TextTertiary,
                fontSize = 10.5.sp,
            )
        }

        // ---------------- 邮件通知 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("邮件通知", "可选")
            GlassTextField(
                value = draft.notifyEmail,
                onValueChange = { draft = draft.copy(notifyEmail = it.trim()) },
                label = "通知邮箱",
                placeholder = "your.email@gmail.com",
                keyboardType = KeyboardType.Email,
                showToggle = false,
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = draft.emailPassword,
                onValueChange = { draft = draft.copy(emailPassword = it) },
                label = "邮箱密码 / 授权码",
                isPassword = true,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = draft.smtpServer,
                    onValueChange = { draft = draft.copy(smtpServer = it.trim()) },
                    label = "SMTP 服务器",
                    modifier = Modifier.weight(1.4f),
                    showToggle = false,
                )
                GlassTextField(
                    value = draft.smtpPort.toString(),
                    onValueChange = { draft = draft.copy(smtpPort = it.toIntOrNull() ?: draft.smtpPort) },
                    label = "端口",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    showToggle = false,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Cookie 失效或任务失败时会发送提醒邮件。", color = GlassPalette.TextTertiary, fontSize = 10.5.sp)
        }

        // ---------------- 自动登录 ----------------
        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("自动登录（刷新 Cookie）", "可选")
            GlassTextField(
                value = draft.neteasePhone,
                onValueChange = { draft = draft.copy(neteasePhone = it.trim()) },
                label = "网易云手机号",
                keyboardType = KeyboardType.Phone,
                showToggle = false,
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = draft.neteasePassword,
                onValueChange = { draft = draft.copy(neteasePassword = it) },
                label = "密码（明文，二选一）",
                isPassword = true,
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                value = draft.neteaseMd5Password,
                onValueChange = { draft = draft.copy(neteaseMd5Password = it.trim()) },
                label = "密码（MD5，推荐，二选一）",
                mono = true,
                isPassword = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "配置后可在「运行」页点击「刷新 Cookie」，登录成功会更新本机配置（安卓端不上传 GitHub）。",
                color = GlassPalette.TextTertiary,
                fontSize = 10.5.sp,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = "保存配置",
                onClick = {
                    vm.updateConfig(draft)
                    vm.scheduleAutoTask(draft)
                },
                accent = true,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = "保存并验证",
                onClick = {
                    vm.updateConfig(draft)
                    vm.scheduleAutoTask(draft)
                    vm.validate()
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SectionTitle(title: String, tag: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, color = GlassPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (tag != null) {
            Spacer(Modifier.height(0.dp))
            Text(
                text = "  · $tag",
                color = GlassPalette.TextTertiary,
                fontSize = 10.5.sp,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Suppress("unused")
private fun Config.summary(): String = "score=$score wait=$waitMin-$waitMax"
