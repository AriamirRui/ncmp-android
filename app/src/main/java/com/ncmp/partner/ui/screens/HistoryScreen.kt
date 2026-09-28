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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmp.partner.data.RunRecord
import com.ncmp.partner.ui.components.GlassButton
import com.ncmp.partner.ui.components.GlassChip
import com.ncmp.partner.ui.glass.GlassSurface
import com.ncmp.partner.ui.glass.LiquidState
import com.ncmp.partner.ui.glass.lightGlass
import com.ncmp.partner.ui.theme.GlassPalette
import com.ncmp.partner.vm.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: UiState,
    liquid: LiquidState,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onOpen: (RunRecord) -> Unit,
    onDelete: (RunRecord) -> Unit,
    onCloseDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))

        GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("运行历史", color = GlassPalette.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("刷新", onRefresh, modifier = Modifier.width(74.dp))
                    GlassButton("清空", onClear, danger = true, modifier = Modifier.width(74.dp))
                }
            }
        }

        if (state.history.isEmpty()) {
            GlassSurface(state = liquid, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("暂无运行记录", color = GlassPalette.TextTertiary, fontSize = 12.5.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.history, key = { it.id }) { record ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .lightGlass(20.dp)
                            .clickable { onOpen(record) }
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(record.time, color = GlassPalette.TextSecondary, fontSize = 12.5.sp)
                            val (label, color) = when {
                                record.cancelled -> "已终止" to GlassPalette.Warning
                                record.success -> "成功" to GlassPalette.Success
                                else -> "失败" to GlassPalette.Error
                            }
                            GlassChip(label, color)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = record.summary.ifBlank { "-" },
                            color = GlassPalette.TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2,
                        )
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }

    // 日志详情
    val detail = state.detailRecord
    if (detail != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseDetail,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xF21A1A22),
            contentColor = GlassPalette.TextPrimary,
            dragHandle = null,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(detail.time, color = GlassPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(detail.summary.ifBlank { "-" }, color = GlassPalette.TextTertiary, fontSize = 11.5.sp)
                    }
                    GlassButton("删除", { onDelete(detail) }, danger = true, modifier = Modifier.width(78.dp))
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC0B0B12))
                        .padding(12.dp)
                ) {
                    if (state.detailLog.isBlank()) {
                        Text("该记录没有日志", color = GlassPalette.TextTertiary, fontSize = 12.sp)
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.detailLog.split("\n")) { line ->
                                Text(
                                    text = line,
                                    color = GlassPalette.TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
