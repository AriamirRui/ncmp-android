package com.ncmp.partner.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ncmp.partner.ui.theme.GlassPalette
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 液态玻璃设计系统
 *
 * 组成：
 *  1. [rememberLiquidState] / [LiquidBackground]：会缓慢流动的彩色光斑背景（玻璃折射的内容）
 *  2. [GlassSurface]：把「同一份背景」偏移后模糊，实现真正的背景模糊（backdrop blur）
 *  3. [glassFill] / [glassBorder]：半透明渐变填充 + 渐变描边 + 顶部高光
 *
 * 注意：背景在卡片内外共用同一个 [LiquidState]，两份拷贝才会像素级对齐。
 */
data class LiquidState(val phase: Float)

@Composable
fun rememberLiquidState(): LiquidState {
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    return LiquidState(phase)
}

/** 流动的彩色光斑背景 */
@Composable
fun LiquidBackground(state: LiquidState, modifier: Modifier = Modifier) {
    val phase = state.phase
    Canvas(modifier = modifier) {
        drawRect(color = GlassPalette.Ink)
        val w = size.width
        val h = size.height
        val minSide = minOf(w, h)
        val angle = phase * 2f * Math.PI.toFloat()

        fun blob(cx: Float, cy: Float, radius: Float, core: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(core, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        // 三组缓慢漂移的光斑：红（网易红）、紫罗兰、青
        blob(
            cx = w * (0.28f + 0.14f * cos(angle)),
            cy = h * (0.16f + 0.08f * sin(angle * 1.3f)),
            radius = minSide * 0.95f,
            core = GlassPalette.Accent.copy(alpha = 0.55f),
        )
        blob(
            cx = w * (0.82f + 0.12f * sin(angle * 0.9f)),
            cy = h * (0.34f + 0.10f * cos(angle * 1.1f)),
            radius = minSide * 0.85f,
            core = GlassPalette.Violet.copy(alpha = 0.45f),
        )
        blob(
            cx = w * (0.22f + 0.16f * sin(angle * 1.4f + 1f)),
            cy = h * (0.86f + 0.08f * cos(angle * 0.8f)),
            radius = minSide * 0.90f,
            core = GlassPalette.Cyan.copy(alpha = 0.32f),
        )
        blob(
            cx = w * (0.78f + 0.10f * cos(angle * 1.7f + 2f)),
            cy = h * (0.90f + 0.06f * sin(angle * 1.2f)),
            radius = minSide * 0.70f,
            core = GlassPalette.Amber.copy(alpha = 0.22f),
        )
    }
}

private fun glassFill(): Brush = Brush.verticalGradient(
    colors = listOf(GlassPalette.GlassFillTop, GlassPalette.GlassFillBottom),
)

private fun glassBorder(): Brush = Brush.verticalGradient(
    colors = listOf(GlassPalette.GlassBorderTop, GlassPalette.GlassBorderBottom),
)

/**
 * 玻璃面板：内部会绘制一份「与页面背景对齐并模糊」的副本，从而产生真实的毛玻璃效果。
 */
@Composable
fun GlassSurface(
    state: LiquidState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    blurRadius: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val widthDp = with(density) { configuration.screenWidthDp.dp }
    val heightDp = with(density) { configuration.screenHeightDp.dp }

    Box(
        modifier = modifier
            .onGloballyPositioned { position = it.positionInRoot() }
            .clip(shape)
            .background(glassFill())
            .border(1.dp, glassBorder(), shape)
    ) {
        // 背景副本：偏移到卡片位置后模糊，实现 backdrop blur
        Box(Modifier.matchParentSize().clip(shape)) {
            Box(
                Modifier
                    .offset {
                        IntOffset(-position.x.roundToInt(), -position.y.roundToInt())
                    }
                    .requiredSize(widthDp, heightDp)
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                LiquidBackground(state, Modifier.fillMaxSize())
            }
        }

        // 顶部高光，让玻璃有厚度感
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GlassPalette.GlassHighlight, Color.Transparent),
                        endY = 160f,
                    )
                )
        )

        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** 无列布局的玻璃容器（用于自定义内容，如列表） */
@Composable
fun GlassBox(
    state: LiquidState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    blurRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val widthDp = with(density) { configuration.screenWidthDp.dp }
    val heightDp = with(density) { configuration.screenHeightDp.dp }
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .onGloballyPositioned { position = it.positionInRoot() }
            .clip(shape)
            .background(glassFill())
            .border(1.dp, glassBorder(), shape)
    ) {
        Box(Modifier.matchParentSize().clip(shape)) {
            Box(
                Modifier
                    .offset { IntOffset(-position.x.roundToInt(), -position.y.roundToInt()) }
                    .requiredSize(widthDp, heightDp)
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                LiquidBackground(state, Modifier.fillMaxSize())
            }
        }
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(
                colors = listOf(GlassPalette.GlassHighlight, Color.Transparent),
                endY = 160f,
            )
        ))
        content()
    }
}

/** 轻量玻璃（不做背景模糊，适合列表项等高频组件） */
fun Modifier.lightGlass(cornerRadius: Dp = 20.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x0FFFFFFF))),
        RoundedCornerShape(cornerRadius),
    )

/** 光条：液态玻璃上的流动反光 */
@Composable
fun SpecularSweep(state: LiquidState, modifier: Modifier = Modifier, cornerRadius: Dp = 26.dp) {
    val shape = RoundedCornerShape(cornerRadius)
    val progress = state.phase
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x1AFFFFFF),
                        Color.Transparent,
                    ),
                    start = Offset(x = -600f + progress * 2400f, y = 0f),
                    end = Offset(x = -300f + progress * 2400f, y = 600f),
                )
            )
    )
}

/** 玻璃描边工具（供小部件复用） */
fun glassStrokeBrush(): Brush = Brush.verticalGradient(
    listOf(Color(0x4DFFFFFF), Color(0x12FFFFFF))
)
