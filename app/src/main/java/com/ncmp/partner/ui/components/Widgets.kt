package com.ncmp.partner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncmp.partner.ui.theme.GlassPalette

/** 玻璃按钮 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    danger: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val fill: Brush = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color(0x08FFFFFF)))
        accent -> Brush.horizontalGradient(listOf(GlassPalette.Accent, GlassPalette.AccentSoft))
        danger -> Brush.horizontalGradient(listOf(Color(0xFFB03A3A), Color(0xFFD25050)))
        else -> Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF)))
    }
    val stroke: Brush = if (accent || danger) {
        SolidColor(Color(0x33FFFFFF))
    } else {
        Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x12FFFFFF)))
    }
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (enabled) GlassPalette.TextPrimary else GlassPalette.TextTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 玻璃输入框 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    showToggle: Boolean = true,
) {
    var visible by remember { mutableStateOf(!isPassword) }
    val shape = RoundedCornerShape(16.dp)
    Column(modifier) {
        Text(
            text = label,
            color = GlassPalette.TextTertiary,
            fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0x14FFFFFF))
                .border(1.dp, Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF))), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, color = GlassPalette.TextTertiary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(
                        color = GlassPalette.TextPrimary,
                        fontSize = if (mono) 12.sp else 13.5.sp,
                        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    ),
                    cursorBrush = SolidColor(GlassPalette.Accent),
                    visualTransformation = if (isPassword && !visible) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isPassword && showToggle) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "切换可见",
                    tint = GlassPalette.TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { visible = !visible },
                )
            }
        }
    }
}

/** 数值卡片 */
@Composable
fun StatTile(
    label: String,
    value: String,
    hint: String = "",
    modifier: Modifier = Modifier,
    valueColor: Color = GlassPalette.TextPrimary,
) {
    Column(modifier) {
        Text(label, color = GlassPalette.TextTertiary, fontSize = 11.5.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = GlassPalette.TextTertiary, fontSize = 10.5.sp)
        }
    }
}

/** 细进度条 */
@Composable
fun ThinProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = GlassPalette.Accent,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x1FFFFFFF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(color, GlassPalette.AccentSoft)
                    )
                )
        )
    }
}

/** 小标签 */
@Composable
fun GlassChip(text: String, color: Color = GlassPalette.TextSecondary, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/** 玻璃提示条（Toast） */
@Composable
fun GlassToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xE6101018))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(message.orEmpty(), color = GlassPalette.TextPrimary, fontSize = 13.sp)
        }
    }
}
