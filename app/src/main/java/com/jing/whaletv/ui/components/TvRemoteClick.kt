package com.jing.whaletv.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.tvRemoteClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    return onPreviewKeyEvent { event ->
        if (event.key !in TvRemoteConfirmKeys) {
            return@onPreviewKeyEvent false
        }
        when (event.type) {
            KeyEventType.KeyDown -> {
                // 长按确认键会产生自动重复事件，只在首次按下时触发
                if (event.nativeKeyEvent.repeatCount == 0) {
                    onClick()
                }
                true
            }
            KeyEventType.KeyUp -> true
            else -> false
        }
    }
}

/**
 * 遥控 + 触摸统一点击接线，顺序固定：tvRemoteClick → focusable → clickable。
 * enabled=false 时保持可聚焦（避免遥控焦点断链）但不可点击。
 */
fun Modifier.tvClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    return tvRemoteClick(enabled = enabled, onClick = onClick)
        .focusable()
        .clickable(enabled = enabled, onClick = onClick)
}

private val TvRemoteConfirmKeys = setOf(
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
)
