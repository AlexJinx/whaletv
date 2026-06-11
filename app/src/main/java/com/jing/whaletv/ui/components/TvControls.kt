package com.jing.whaletv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WhaleTokens.SecondaryText,
    focusedTint: Color = WhaleTokens.Cyan,
) {
    FocusableButtonSurface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        focusedBorder = focusedTint,
    ) { focused ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (focused) focusedTint else tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun TvTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    focusedBorder: Color = if (primary) WhaleTokens.Cyan else WhaleTokens.Cyan,
) {
    FocusableButtonSurface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        focusedBorder = focusedBorder,
        container = if (primary) WhaleTokens.Cyan else Color.White.copy(alpha = 0.06f),
        focusedContainer = if (primary) WhaleTokens.Cyan else WhaleTokens.SurfaceRaised,
        focusedScale = 1.03f,
    ) { focused ->
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (primary) WhaleTokens.Background else if (focused) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = text,
                color = if (primary) WhaleTokens.Background else if (focused) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                fontSize = 13.sp,
                fontWeight = if (primary || focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FocusableButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.Transparent,
    focusedContainer: Color = Color(0x1F00C8D4),
    focusedBorder: Color = WhaleTokens.Cyan,
    focusedScale: Float = 1.06f,
    content: @Composable (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (focused) focusedScale else 1f
                scaleY = if (focused) focusedScale else 1f
            }
            .shadow(
                elevation = if (focused) 10.dp else 0.dp,
                shape = shape,
                ambientColor = focusedBorder.copy(alpha = 0.2f),
                spotColor = focusedBorder.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(if (focused) focusedContainer else container)
            .border(
                width = 1.dp,
                color = if (focused) focusedBorder else Color.White.copy(alpha = 0.10f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        content(focused)
    }
}
