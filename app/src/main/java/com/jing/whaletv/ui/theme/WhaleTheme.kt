package com.jing.whaletv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WhaleTokens {
    val Background = Color(0xFF0D0F12)
    val BackgroundDeep = Color(0xFF050709)
    val Sidebar = Color(0xFF0F1218)
    val Surface = Color(0xFF13171F)
    val SurfaceRaised = Color(0xFF1C2233)
    val Muted = Color(0xFF1A2030)
    val PrimaryText = Color(0xFFDDE4F0)
    val SecondaryText = Color(0xFF6B7FA3)
    val TertiaryText = Color(0xFF8899B4)
    val Cyan = Color(0xFF00C8D4)
    val Gold = Color(0xFFD4A017)
    val Red = Color(0xFFE53535)
    val Green = Color(0xFF22C55E)
    val Border = Color(0x12FFFFFF)
    val BorderStrong = Color(0x1FFFFFFF)
}

private val WhaleColors = darkColorScheme(
    primary = WhaleTokens.Cyan,
    onPrimary = WhaleTokens.Background,
    secondary = WhaleTokens.Gold,
    onSecondary = Color(0xFF1D1600),
    background = WhaleTokens.Background,
    onBackground = WhaleTokens.PrimaryText,
    surface = WhaleTokens.Surface,
    onSurface = WhaleTokens.PrimaryText,
    surfaceVariant = WhaleTokens.SurfaceRaised,
    onSurfaceVariant = WhaleTokens.SecondaryText,
    error = WhaleTokens.Red,
)

@Composable
fun WhaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhaleColors,
        content = content,
    )
}
