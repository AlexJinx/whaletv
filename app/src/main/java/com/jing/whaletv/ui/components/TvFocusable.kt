package com.jing.whaletv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jing.whaletv.ui.theme.WhaleMotion
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens
import kotlinx.coroutines.delay

/**
 * 可聚焦表面的四态样式：normal / selected / focused（focused 优先于 selected）。
 * [glow] 为 true 时 focused 态附加品牌色光晕阴影。
 */
data class TvFocusStyle(
    val fill: Color = Color.Transparent,
    val fillSelected: Color = WhaleTokens.SelectedFill,
    val fillFocused: Color = WhaleTokens.FocusFill,
    val border: Color = Color.Transparent,
    val borderSelected: Color = WhaleTokens.SelectedBorder,
    val borderFocused: Color = WhaleTokens.FocusBorder,
    val glow: Boolean = false,
) {
    companion object {
        /** 顶栏动作、tab、列表行等常规元素。 */
        val Standard = TvFocusStyle()

        /** 卡片：带底色、常驻描边、焦点光晕。 */
        val Raised = TvFocusStyle(
            fill = WhaleTokens.Surface,
            fillSelected = WhaleTokens.SurfaceRaised,
            border = WhaleTokens.Border,
            glow = true,
        )
    }
}

/**
 * 统一的 TV 可聚焦表面：拥有 focused 状态、缩放动画、填充/描边/光晕，
 * 以及遥控 + 触摸点击接线。取代散落各屏幕的
 * `var focused by remember` + onFocusChanged + when 配色 + tvRemoteClick 样板。
 *
 * [modifier] 由调用方负责 size / focusRequester / focusProperties / onPreviewKeyEvent。
 * [content] 收到当前 focused 状态，供内容自行决定文字/图标颜色。
 */
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = WhaleShapes.Item,
    style: TvFocusStyle = TvFocusStyle.Standard,
    scaleOnFocus: Float = 1f,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) scaleOnFocus else 1f,
        animationSpec = WhaleMotion.FocusSpec,
        label = "tvFocusableScale",
    )
    val fill = when {
        focused -> style.fillFocused
        selected -> style.fillSelected
        else -> style.fill
    }
    val border = when {
        focused -> style.borderFocused
        selected -> style.borderSelected
        else -> style.border
    }
    val glowModifier = if (style.glow && focused) {
        Modifier.shadow(
            elevation = 14.dp,
            shape = shape,
            clip = false,
            ambientColor = WhaleTokens.FocusGlow,
            spotColor = WhaleTokens.FocusGlow,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(glowModifier)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .onFocusChanged { state ->
                if (focused != state.isFocused) {
                    focused = state.isFocused
                    onFocusChanged(state.isFocused)
                }
            }
            .tvClickable(enabled = enabled, onClick = onClick),
    ) {
        content(focused)
    }
}

/**
 * 初始焦点请求：等待一帧组合完成后请求焦点（封装 delay + runCatching 惯用法）。
 * [key] 变化时重新请求；传 Unit 表示只请求一次。
 */
@Composable
fun RequestInitialFocus(
    focusRequester: FocusRequester,
    key: Any? = Unit,
    delayMillis: Long = 120L,
) {
    LaunchedEffect(key) {
        delay(delayMillis)
        runCatching { focusRequester.requestFocus() }
    }
}
