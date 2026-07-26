package com.jing.whaletv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jing.whaletv.ui.theme.WhaleTokens
import com.jing.whaletv.ui.theme.WhaleType

/**
 * 统一 52dp 顶栏：可选返回键 + 标题（+ 副标题）+ 右侧动作区。
 * Home 的品牌顶栏结构特殊，不使用本组件。
 */
@Composable
fun WhaleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .drawBehind {
                drawLine(
                    color = WhaleTokens.Border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TvIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        Text(
            text = title,
            color = WhaleTokens.TextPrimary,
            fontSize = WhaleType.Section,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = WhaleTokens.TextSecondary,
                fontSize = WhaleType.Caption,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}
