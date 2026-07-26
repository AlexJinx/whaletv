package com.jing.whaletv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 鲸鱼TV 设计令牌：全 app 颜色唯一来源。
 * 禁止在屏幕代码里写 Color(0x...) 字面量或 Accent.copy(alpha = ...) 魔数，
 * 需要新颜色时在这里命名。
 */
object WhaleTokens {
    // ---- 背景层次（深空蓝黑） ----
    val BackgroundTop = Color(0xFF0C1018)
    val BackgroundBottom = Color(0xFF060810)
    val Background = Color(0xFF0A0E15)
    val BackgroundDeep = Color(0xFF04060A)
    val Sidebar = Color(0xFF10141D)
    val Surface = Color(0xFF121722)
    val SurfaceRaised = Color(0xFF1A2130)
    val SurfaceGlass = Color(0xE6111826)
    val Muted = Color(0xFF171E2B)

    // ---- 文字 ----
    val TextPrimary = Color(0xFFE9EEF8)
    val TextSecondary = Color(0xFF8B9CBB)
    val TextTertiary = Color(0xFF64748F)
    val TextOnAccent = Color(0xFF042028)

    // ---- 品牌 / 语义 ----
    val Accent = Color(0xFF25D0DE)
    val AccentBright = Color(0xFF52E5F0)
    val AccentDeep = Color(0xFF10B8C6)
    val Gold = Color(0xFFE0B54F)
    val Green = Color(0xFF34D399)
    val Amber = Color(0xFFE8A94E)
    val Red = Color(0xFFF05555)
    val Teal = Color(0xFF5FC8B8)

    // ---- 边框 ----
    val Border = Color(0x14FFFFFF)
    val BorderStrong = Color(0x22FFFFFF)

    // ---- 焦点语义（focused / selected 四态的唯一来源） ----
    val FocusFill = Color(0x2925D0DE)
    val FocusBorder = Color(0xB325D0DE)
    val FocusGlow = Color(0x5C25D0DE)
    val SelectedFill = Color(0x1425D0DE)
    val SelectedBorder = Color(0x4425D0DE)

    // ---- 弱化元素 ----
    val IconMuted = Color(0xFF7A8EAA)
    val MetaText = Color(0xFF8899BB)
    val CountDim = Color(0xFF5E7090)

    // ---- 频道卡片 ----
    val CardTitle = Color(0xFFD0DCE8)
    val CardTitleActive = Color(0xFFE8F4F5)
    val CardQualityText = Color(0xFF99AEC8)
    val LogoText = Color(0xFFE8F0FF)
    val LogoTextSoft = Color(0xFFC8E0FF)
    val LiveTealBright = Color(0xFF8FE3D8)
    val OkText = Color(0xFF7ACEA0)
    val WarnDot = Color(0xFFE0A04E)
    val WarnText = Color(0xFFE0B875)

    // ---- 播放器 ----
    /** 播放器覆盖层玻璃底色，使用处按需 copy(alpha)。 */
    val PlayerGlass = Color(0xFF111722)
}

private val WhaleColors = darkColorScheme(
    primary = WhaleTokens.Accent,
    onPrimary = WhaleTokens.TextOnAccent,
    secondary = WhaleTokens.Gold,
    onSecondary = Color(0xFF1D1600),
    background = WhaleTokens.Background,
    onBackground = WhaleTokens.TextPrimary,
    surface = WhaleTokens.Surface,
    onSurface = WhaleTokens.TextPrimary,
    surfaceVariant = WhaleTokens.SurfaceRaised,
    onSurfaceVariant = WhaleTokens.TextSecondary,
    error = WhaleTokens.Red,
)

@Composable
fun WhaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhaleColors,
        content = content,
    )
}
