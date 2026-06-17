package com.jing.whaletv.ui.components

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
                onClick()
                true
            }
            KeyEventType.KeyUp -> true
            else -> false
        }
    }
}

private val TvRemoteConfirmKeys = setOf(
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
)
