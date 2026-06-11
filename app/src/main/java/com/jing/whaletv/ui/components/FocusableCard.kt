package com.jing.whaletv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    focusedContainer: Color = WhaleTokens.SurfaceRaised,
    container: Color = WhaleTokens.Surface,
    focusedBorder: Color = WhaleTokens.Cyan,
    border: Color = WhaleTokens.Border,
    focusedScale: Float = 1.04f,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (focused) focusedScale else 1f
                scaleY = if (focused) focusedScale else 1f
            }
            .shadow(
                elevation = if (focused) 18.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = focusedBorder.copy(alpha = 0.24f),
                spotColor = focusedBorder.copy(alpha = 0.32f),
            )
            .clip(shape)
            .background(if (focused) focusedContainer else container)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) focusedBorder else border,
                shape = shape,
            )
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .focusable()
            .clickable(onClick = onClick)
            .padding(contentPadding),
    ) {
        content(focused)
    }
}
