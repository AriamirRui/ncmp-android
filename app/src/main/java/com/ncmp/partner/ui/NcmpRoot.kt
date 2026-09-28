package com.ncmp.partner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ncmp.partner.ui.components.GlassChip
import com.ncmp.partner.ui.components.GlassToast
import com.ncmp.partner.ui.glass.GlassBox
import com.ncmp.partner.ui.glass.LiquidBackground
import com.ncmp.partner.ui.glass.LiquidState
import com.ncmp.partner.ui.glass.rememberLiquidState
import com.ncmp.partner.ui.screens.AboutScreen
import com.ncmp.partner.ui.screens.ConfigScreen
import com.ncmp.partner.ui.screens.HistoryScreen
import com.ncmp.partner.ui.screens.RunScreen
import com.ncmp.partner.ui.theme.GlassPalette
import com.ncmp.partner.vm.NcmpViewModel
import com.ncmp.partner.vm.UiState

enum class Page(val title: String, val icon: ImageVector) {
    RUN("运行", Icons.Filled.Home),
    CONFIG("配置", Icons.Filled.Settings),
    HISTORY("历史", Icons.Filled.History),
    ABOUT("关于", Icons.Filled.Info),
}

@Composable
fun NcmpRoot(vm: NcmpViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val liquid = rememberLiquidState()
    var page by remember { mutableStateOf(Page.RUN) }
    val context = LocalContext.current

    // 通知权限（后台任务结果提示）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 流动的彩色背景（玻璃折射的底层内容）
        LiquidBackground(liquid, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            TopBar(state)
            Box(Modifier.weight(1f)) {
                when (page) {
                    Page.RUN -> RunScreen(
                        state = state,
                        liquid = liquid,
                        onStart = { vm.startTask() },
                        onCancel = { vm.cancelTask() },
                        onValidate = { vm.validate() },
                        onRefreshCookie = { vm.refreshCookie() },
                        onClearLogs = { vm.clearLogs() },
                    )
                    Page.CONFIG -> ConfigScreen(
                        state = state,
                        liquid = liquid,
                        onSave = { config ->
                            vm.updateConfig(config)
                            vm.scheduleAutoTask(config)
                        },
                        onSaveAndValidate = { config ->
                            vm.updateConfig(config)
                            vm.scheduleAutoTask(config)
                            vm.validate()
                        },
                    )
                    Page.HISTORY -> HistoryScreen(
                        state = state,
                        liquid = liquid,
                        onRefresh = { vm.refreshHistory() },
                        onClear = { vm.clearHistory() },
                        onOpen = { record -> vm.openRecord(record) },
                        onDelete = { record -> vm.deleteRecord(record) },
                        onCloseDetail = { vm.closeRecord() },
                    )
                    Page.ABOUT -> AboutScreen(state, liquid)
                }
            }
            BottomNav(current = page, liquid = liquid) { page = it }
        }

        GlassToast(
            message = state.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 108.dp),
        )
    }
}

@Composable
private fun TopBar(state: UiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "网易云音乐合伙人",
                color = GlassPalette.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.stage,
                color = GlassPalette.TextTertiary,
                fontSize = 11.5.sp,
                maxLines = 1,
            )
        }
        val (label, color) = when {
            state.busy && state.cancelling -> "终止中" to GlassPalette.Warning
            state.busy -> "运行中" to GlassPalette.AccentSoft
            else -> "空闲" to GlassPalette.Success
        }
        GlassChip(label, color)
    }
}

@Composable
private fun BottomNav(current: Page, liquid: LiquidState, onSelect: (Page) -> Unit) {
    GlassBox(
        state = liquid,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        cornerRadius = 26.dp,
        blurRadius = 30.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Page.entries.forEach { item ->
                val selected = item == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) GlassPalette.Accent.copy(alpha = 0.22f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected) GlassPalette.AccentSoft else GlassPalette.TextTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        color = if (selected) GlassPalette.TextPrimary else GlassPalette.TextTertiary,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
