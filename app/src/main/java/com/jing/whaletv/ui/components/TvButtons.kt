package com.jing.whaletv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens
import com.jing.whaletv.ui.theme.WhaleType

/** 统一图标按钮（顶栏返回 / 刷新等）。 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        enabled = enabled,
        shape = WhaleShapes.Button,
    ) { focused ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> WhaleTokens.TextTertiary
                focused -> WhaleTokens.Accent
                else -> WhaleTokens.TextSecondary
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize),
        )
    }
}

/** 统一文字按钮，可带前置图标；[emphasized] 为主要动作（常态即品牌色）。 */
@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    selected: Boolean = false,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        selected = selected || emphasized,
        enabled = enabled,
        shape = WhaleShapes.Button,
    ) { focused ->
        val contentColor = when {
            !enabled -> WhaleTokens.TextTertiary
            focused || emphasized -> WhaleTokens.Accent
            else -> WhaleTokens.TextSecondary
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                color = if (focused) WhaleTokens.TextPrimary else contentColor,
                fontSize = WhaleType.Label,
                fontWeight = if (focused || emphasized) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
