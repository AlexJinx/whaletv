package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.ui.components.TvFocusStyle
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
internal fun SettingsCardStack(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun SettingsCardRow(
    modifier: Modifier = Modifier,
    height: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(SettingsGridGap),
        content = content,
    )
}

@Composable
internal fun SettingsValueCard(
    title: String,
    description: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        SettingsCardTitle(title = title, description = description)
        Text(
            text = value,
            color = WhaleTokens.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Muted)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun SourceStatusCard(
    title: String,
    note: String,
    url: String,
    enabled: Boolean,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier, alpha = if (enabled) 1f else 0.72f) {
        SettingsCardTitle(title = title, description = note)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = url,
                color = if (enabled) WhaleTokens.TextPrimary else WhaleTokens.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WhaleTokens.Muted.copy(alpha = if (enabled) 1f else 0.54f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            SettingsSmallButton(text = actionText, enabled = enabled, onClick = onAction)
        }
    }
}

@Composable
internal fun SettingsTextCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        Text(title, color = WhaleTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Muted.copy(alpha = 0.72f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = description,
                color = WhaleTokens.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    borderColor: Color? = null,
    minHeight: Dp = SettingsCardMinHeight,
    verticalArrangement: Arrangement.Vertical = Arrangement.SpaceBetween,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = WhaleShapes.Button
    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(WhaleTokens.SurfaceRaised.copy(alpha = alpha))
            .border(1.dp, borderColor ?: WhaleTokens.Border.copy(alpha = 0.08f * alpha), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
internal fun SettingsCardTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = WhaleTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            description,
            color = WhaleTokens.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SettingsHintText(text: String) {
    Text(
        text = text,
        color = WhaleTokens.TextSecondary,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
    )
}

@Composable
internal fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
internal fun SettingsStepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    SettingButtonContainer(onClick = onClick, enabled = enabled, iconOnly = true) {
        Text(text, color = settingsButtonTextColor(enabled), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SettingsSmallButton(
    text: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    height: Dp = SettingsControlHeight,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SettingButtonContainer(onClick = onClick, enabled = enabled, highlighted = highlighted, height = height, modifier = modifier) {
        Text(text, color = settingsButtonTextColor(enabled), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun CompactSettingsInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    // 文本输入不是点击表面，无法迁移 TvFocusable，保留自维护 focused 驱动描边
    var focused by remember { mutableStateOf(false) }
    val shape = WhaleShapes.Button
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            color = if (enabled) WhaleTokens.TextPrimary else WhaleTokens.TextSecondary.copy(alpha = 0.48f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = SolidColor(WhaleTokens.Accent),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .height(SettingsControlHeight)
            .clip(shape)
            .background(WhaleTokens.Muted.copy(alpha = if (enabled) 1f else 0.56f))
            .border(
                1.dp,
                if (enabled && focused) WhaleTokens.FocusBorder else WhaleTokens.Border,
                shape,
            )
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isBlank() && placeholder.isNotBlank()) {
                        Text(
                            text = placeholder,
                            color = WhaleTokens.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                suffix?.let {
                    Text(it, color = WhaleTokens.TextSecondary, fontSize = 12.sp, maxLines = 1)
                }
            }
        },
    )
}

@Composable
private fun SettingButtonContainer(
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    iconOnly: Boolean = false,
    height: Dp = SettingsControlHeight,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    TvFocusable(
        onClick = onClick,
        enabled = enabled,
        selected = highlighted,
        shape = WhaleShapes.Item,
        style = TvFocusStyle(
            fill = if (enabled) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.04f),
            border = if (enabled) WhaleTokens.Border else Color.White.copy(alpha = 0.04f),
        ),
        modifier = modifier
            .height(height)
            .then(if (iconOnly) Modifier.width(height) else Modifier),
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (iconOnly) 0.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

private fun settingsButtonTextColor(enabled: Boolean): Color {
    return if (enabled) WhaleTokens.TextPrimary else WhaleTokens.TextSecondary.copy(alpha = 0.38f)
}

internal val SettingsGridGap = 20.dp
private val SettingsCardMinHeight = 0.dp
internal val SettingsCompactRowHeight = 132.dp
internal val SettingsBalancedRowHeight = 156.dp
internal val SettingsUrlRowHeight = 124.dp
internal val SettingsStatusRowHeight = 166.dp
private val SettingsControlHeight = 42.dp
