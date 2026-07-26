package com.jing.whaletv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * 国旗渲染：按 flag_<iso2> 命名约定查找 drawable（'uk' 别名 'gb'），
 * 缺失时回退为国家色块 + 两位代码。
 * 注意：资源名通过 getIdentifier 动态解析，flag_ 前缀契约不可改。
 */
@Composable
fun FlagImage(
    countryId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 4.dp,
    fallbackFontSize: TextUnit = 15.sp,
) {
    val context = LocalContext.current
    val flagCode = remember(countryId) { flagResourceCode(countryId) }
    val flagResourceId = remember(flagCode) {
        flagCode?.let { code ->
            context.resources.getIdentifier("flag_$code", "drawable", context.packageName)
        } ?: 0
    }
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (flagResourceId == 0) countryFallbackColor(countryId) else Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (flagResourceId != 0) {
            Image(
                painter = painterResource(id = flagResourceId),
                contentDescription = contentDescription,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = countryId.take(2).uppercase(Locale.ROOT),
                color = Color.White,
                fontSize = fallbackFontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

fun flagResourceCode(id: String): String? {
    val normalized = id.lowercase(Locale.ROOT)
    if (normalized == "other") return null
    return when (normalized) {
        "uk" -> "gb"
        else -> normalized.takeIf { it.length == 2 && it.all(Char::isLetter) }
    }
}

fun countryFallbackColor(id: String): Color {
    return when (id.lowercase(Locale.ROOT)) {
        "cn" -> Color(0xFFE1192D)
        "us" -> Color(0xFF2D5BBA)
        "jp" -> Color(0xFFF3F5F8)
        "uk" -> Color(0xFF21468B)
        "kr" -> Color(0xFFF4F5F7)
        "de" -> Color(0xFF101010)
        "fr" -> Color(0xFF2E5AAC)
        "ca" -> Color(0xFFE03535)
        "au" -> Color(0xFF234B9B)
        "sg" -> Color(0xFFD92D2D)
        "in" -> Color(0xFFE58A24)
        "br" -> Color(0xFF229C45)
        else -> Color(0xFF27415F)
    }
}
