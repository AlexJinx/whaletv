package com.jing.whaletv.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 品牌渐变：页面 / 卡片 / 强调条 / 播放器遮罩。 */
object WhaleGradients {
    val Page = Brush.verticalGradient(
        listOf(WhaleTokens.BackgroundTop, WhaleTokens.BackgroundBottom),
    )
    val CardSurface = Brush.linearGradient(
        listOf(Color(0xFF151B28), Color(0xFF0E131D)),
    )
    val LogoPanel = Brush.linearGradient(
        listOf(Color(0xFF111620), Color(0xFF0D1119)),
    )
    val LogoBadge = Brush.linearGradient(
        listOf(Color(0xFF1A2540), Color(0xFF0F1A30)),
    )
    val AccentBar = Brush.horizontalGradient(
        listOf(WhaleTokens.Accent, WhaleTokens.AccentBright),
    )
    val ScrimTop = Brush.verticalGradient(
        listOf(Color(0xB3000000), Color.Transparent),
    )
    val ScrimBottom = Brush.verticalGradient(
        listOf(Color.Transparent, Color(0xCC000000)),
    )
}

/** 圆角形状梯度。 */
object WhaleShapes {
    val Card = RoundedCornerShape(14.dp)
    val Panel = RoundedCornerShape(12.dp)
    val Item = RoundedCornerShape(10.dp)
    val Button = RoundedCornerShape(8.dp)
    val Chip = RoundedCornerShape(6.dp)
    val Pill = RoundedCornerShape(50)
}

/** 焦点动效参数。 */
object WhaleMotion {
    const val FocusScale = 1.03f
    const val CardFocusScale = 1.04f
    val FocusSpec: AnimationSpec<Float> = tween(durationMillis = 160, easing = FastOutSlowInEasing)
}

/** 字号阶梯（TV 在 Density(1f) 下调优）。 */
object WhaleType {
    val Display = 28.sp
    val Title = 24.sp
    val Section = 20.sp
    val Body = 16.sp
    val Label = 14.sp
    val Caption = 13.sp
    val Micro = 12.sp
}
