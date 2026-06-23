package com.jing.whaletv.ui.screens

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jing.whaletv.R
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.CountryEntry
import com.jing.whaletv.ui.HomeCountryTabs
import com.jing.whaletv.ui.MAX_HOME_COUNTRY_TABS
import com.jing.whaletv.ui.addHomeCountryTab
import com.jing.whaletv.ui.addableHomeCountryEntries
import com.jing.whaletv.ui.components.tvRemoteClick
import com.jing.whaletv.ui.homeCountryEntries
import com.jing.whaletv.ui.moveHomeCountryTab
import com.jing.whaletv.ui.normalizeHomeCountryTabIds
import com.jing.whaletv.ui.removeHomeCountryTab
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CountryEditorScreen(
    channels: List<TvChannel>,
    visibleCountryIds: List<String>,
    syncSummary: SyncSummary,
    isRefreshing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val allCountries = remember(channels) { homeCountryEntries(channels) }
    val countryById = remember(allCountries) { allCountries.associateBy { it.id } }
    var draftIds by rememberSaveable(visibleCountryIds) {
        mutableStateOf(normalizeHomeCountryTabIds(visibleCountryIds))
    }
    var query by rememberSaveable { mutableStateOf("") }
    var isSearchEditing by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val initialFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val platformDensity = LocalDensity.current

    BackHandler(enabled = isSearchEditing) {
        keyboardController?.hide()
        isSearchEditing = false
        searchFocusRequester.requestFocus()
    }
    BackHandler(enabled = !isSearchEditing, onBack = onBack)

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

    val visibleCountries = draftIds.mapNotNull { countryById[it] }
    val addableCountries = addableHomeCountryEntries(allCountries, draftIds, query)

    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleTokens.Background),
        ) {
            CountryEditorTopBar(
                now = now,
                syncSummary = syncSummary,
                isRefreshing = isRefreshing,
                message = message,
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 48.dp, top = 28.dp, end = 48.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                VisibleCountriesPanel(
                    countries = visibleCountries,
                    initialFocusRequester = initialFocusRequester,
                    onMoveUp = { id -> draftIds = moveHomeCountryTab(draftIds, id, -1) },
                    onMoveDown = { id -> draftIds = moveHomeCountryTab(draftIds, id, 1) },
                    onRemove = { id -> draftIds = removeHomeCountryTab(draftIds, id) },
                    modifier = Modifier.weight(1.12f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.11f)),
                )
                AddableCountriesPanel(
                    countries = addableCountries,
                    totalCountryCount = allCountries.size,
                    query = query,
                    onQueryChange = { query = it },
                    isSearchEditing = isSearchEditing,
                    onSearchEditingChange = { isSearchEditing = it },
                    searchFocusRequester = searchFocusRequester,
                    canAdd = draftIds.size < MAX_HOME_COUNTRY_TABS,
                    onAdd = { id ->
                        if (draftIds.size < MAX_HOME_COUNTRY_TABS) {
                            draftIds = addHomeCountryTab(draftIds, id)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            CountryEditorBottomBar(
                onRestoreDefault = {
                    draftIds = normalizeHomeCountryTabIds(HomeCountryTabs.map { it.id })
                    query = ""
                },
                onCancel = onBack,
                onSave = { onSave(draftIds) },
            )
        }
    }
}

@Composable
private fun CountryEditorTopBar(
    now: Long,
    syncSummary: SyncSummary,
    isRefreshing: Boolean,
    message: String?,
) {
    val statusText = when {
        isRefreshing -> "正在同步"
        message != null -> message
        syncSummary.playlistLastError != null -> "同步失败"
        syncSummary.playlistLastSuccessAt != null -> "已同步"
        else -> "等待同步"
    }
    val statusColor = when {
        isRefreshing -> WhaleTokens.Cyan
        syncSummary.playlistLastError != null -> WhaleTokens.Red
        syncSummary.playlistLastSuccessAt != null -> WhaleTokens.Green
        else -> WhaleTokens.SecondaryText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.whaletv_app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Text(
            text = "鲸电视",
            color = WhaleTokens.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(statusColor),
        )
        Text(statusText, color = WhaleTokens.TertiaryText, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
        Text(
            text = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(now)),
            color = WhaleTokens.PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun VisibleCountriesPanel(
    countries: List<CountryEntry>,
    initialFocusRequester: FocusRequester,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("已显示在首页", color = WhaleTokens.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("最多显示 20 个", color = WhaleTokens.TertiaryText, fontSize = 16.sp)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Surface.copy(alpha = 0.84f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
        ) {
            items(countries, key = { it.id }) { country ->
                val index = countries.indexOf(country)
                VisibleCountryRow(
                    country = country,
                    index = index,
                    initialRemoveFocusRequester = initialFocusRequester.takeIf { countries.indexOf(country) == 1 },
                    canMoveUp = !country.locked && index > 1,
                    canMoveDown = !country.locked && index < countries.lastIndex,
                    onMoveUp = { onMoveUp(country.id) },
                    onMoveDown = { onMoveDown(country.id) },
                    onRemove = { onRemove(country.id) },
                )
            }
        }
    }
}

@Composable
private fun VisibleCountryRow(
    country: CountryEntry,
    index: Int,
    initialRemoveFocusRequester: FocusRequester?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var rowFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(if (rowFocused) WhaleTokens.Cyan.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (rowFocused) WhaleTokens.Cyan.copy(alpha = 0.20f) else Color.Transparent)
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString(),
            color = WhaleTokens.SecondaryText,
            fontSize = 15.sp,
            modifier = Modifier.width(28.dp),
        )
        CountryMark(country = country, modifier = Modifier.padding(start = 2.dp))
        Text(
            text = country.label,
            color = WhaleTokens.PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 24.dp)
                .weight(1f),
        )
        Text(
            text = "${country.channelCount} 个频道",
            color = WhaleTokens.TertiaryText,
            fontSize = 16.sp,
            modifier = Modifier.width(132.dp),
        )
        Row(
            modifier = Modifier.width(198.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (country.locked) {
                Spacer(Modifier.size(48.dp))
                CountryIconButton(
                    icon = Icons.Default.Lock,
                    enabled = false,
                    onClick = {},
                )
                Spacer(Modifier.size(48.dp))
            } else {
                CountryIconButton(icon = Icons.Default.KeyboardArrowUp, enabled = canMoveUp, onClick = onMoveUp)
                CountryIconButton(icon = Icons.Default.KeyboardArrowDown, enabled = canMoveDown, onClick = onMoveDown)
                CountryIconButton(
                    icon = Icons.Default.Remove,
                    onClick = onRemove,
                    onFocusChanged = { rowFocused = it },
                    modifier = initialRemoveFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
            }
        }
    }
    RowDivider()
}

@Composable
private fun AddableCountriesPanel(
    countries: List<CountryEntry>,
    totalCountryCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchEditing: Boolean,
    onSearchEditingChange: (Boolean) -> Unit,
    searchFocusRequester: FocusRequester,
    canAdd: Boolean,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("可添加国家", color = WhaleTokens.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("选择国家添加到顶部显示", color = WhaleTokens.TertiaryText, fontSize = 16.sp)
        }
        CountrySearchField(
            query = query,
            onQueryChange = onQueryChange,
            isEditing = isSearchEditing,
            onEditingChange = onSearchEditingChange,
            searchFocusRequester = searchFocusRequester,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(countries, key = { it.id }) { country ->
                AddableCountryRow(
                    country = country,
                    canAdd = canAdd,
                    onAdd = { onAdd(country.id) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(18.dp))
            Text(
                text = "共 $totalCountryCount 个国家和地区",
                color = WhaleTokens.SecondaryText,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun CountrySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var sawKeyboardWhileEditing by remember { mutableStateOf(false) }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    fun detectKeyboardVisible(): Boolean {
        val windowInsets = ViewCompat.getRootWindowInsets(view)
        if (windowInsets != null) {
            return windowInsets.isVisible(WindowInsetsCompat.Type.ime())
        }
        val visibleBounds = Rect()
        val rootView = view.rootView
        rootView.getWindowVisibleDisplayFrame(visibleBounds)
        val hiddenHeight = rootView.height - visibleBounds.bottom
        return hiddenHeight > rootView.height * 0.15f
    }
    val exitEditing = {
        keyboardController?.hide()
        onEditingChange(false)
        searchFocusRequester.requestFocus()
    }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            isKeyboardVisible = detectKeyboardVisible()
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }
    LaunchedEffect(isEditing) {
        while (isEditing) {
            isKeyboardVisible = detectKeyboardVisible()
            delay(100)
        }
    }
    LaunchedEffect(isEditing, isKeyboardVisible) {
        if (!isEditing) {
            sawKeyboardWhileEditing = false
            return@LaunchedEffect
        }
        if (isKeyboardVisible) {
            sawKeyboardWhileEditing = true
        } else if (sawKeyboardWhileEditing) {
            exitEditing()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WhaleTokens.SurfaceRaised.copy(alpha = 0.86f))
            .border(
                1.dp,
                if (focused) WhaleTokens.Cyan.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(8.dp),
            )
            .onPreviewKeyEvent { event ->
                if (!isEditing || event.key != Key.Back) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        exitEditing()
                        true
                    }
                    KeyEventType.KeyUp -> true
                    else -> false
                }
            }
            .onFocusChanged { focused = it.hasFocus }
            .tvRemoteClick { onEditingChange(true) }
            .focusRequester(searchFocusRequester)
            .focusable()
            .clickable { onEditingChange(true) }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(28.dp))
        if (isEditing) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = WhaleTokens.PrimaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                ),
                singleLine = true,
                cursorBrush = SolidColor(WhaleTokens.Cyan),
                modifier = Modifier
                    .padding(start = 18.dp)
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Back) return@onPreviewKeyEvent false
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                exitEditing()
                                true
                            }
                            KeyEventType.KeyUp -> true
                            else -> false
                        }
                    },
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text("搜索国家名称", color = WhaleTokens.SecondaryText, fontSize = 22.sp)
                    }
                    innerTextField()
                },
            )
        } else {
            Text(
                text = query.ifBlank { "搜索国家名称" },
                color = if (query.isBlank()) WhaleTokens.SecondaryText else WhaleTokens.PrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 18.dp)
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun AddableCountryRow(
    country: CountryEntry,
    canAdd: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountryMark(country = country)
        Text(
            text = country.label,
            color = WhaleTokens.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 20.dp)
                .weight(1f),
        )
        Text(
            text = "${country.channelCount} 个频道",
            color = WhaleTokens.TertiaryText,
            fontSize = 17.sp,
            modifier = Modifier.width(132.dp),
        )
        CountryIconButton(
            icon = Icons.Default.Add,
            enabled = canAdd,
            onClick = onAdd,
        )
    }
    RowDivider()
}

@Composable
private fun CountryEditorBottomBar(
    onRestoreDefault: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(WhaleTokens.Sidebar.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteHint("OK", "选择")
        Spacer(Modifier.width(36.dp))
        RemoteHint("↩", "返回 取消")
        Spacer(Modifier.weight(1f))
        CountryTextButton(text = "恢复默认", icon = Icons.Default.Restore, onClick = onRestoreDefault)
        Spacer(Modifier.width(18.dp))
        CountryTextButton(text = "取消", onClick = onCancel)
        Spacer(Modifier.width(18.dp))
        CountryTextButton(text = "保存更改", primary = true, onClick = onSave)
    }
}

@Composable
private fun CountryIconButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        focused -> WhaleTokens.Cyan.copy(alpha = 0.17f)
        enabled -> WhaleTokens.SurfaceRaised.copy(alpha = 0.82f)
        else -> WhaleTokens.SurfaceRaised.copy(alpha = 0.28f)
    }
    val border = when {
        focused -> WhaleTokens.Cyan
        enabled -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .tvRemoteClick(enabled = enabled, onClick = onClick)
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> WhaleTokens.SecondaryText.copy(alpha = 0.42f)
                focused -> WhaleTokens.Cyan
                else -> WhaleTokens.PrimaryText
            },
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CountryTextButton(
    text: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        primary -> if (focused) WhaleTokens.Cyan else Color(0xFF10B8C6)
        focused -> WhaleTokens.Cyan.copy(alpha = 0.15f)
        else -> WhaleTokens.SurfaceRaised.copy(alpha = 0.78f)
    }
    val border = if (focused && !primary) WhaleTokens.Cyan.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.08f)
    Row(
        modifier = Modifier
            .height(58.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .onFocusChanged { focused = it.isFocused }
            .tvRemoteClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (primary) WhaleTokens.Background else WhaleTokens.PrimaryText,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = text,
            color = if (primary) WhaleTokens.Background else WhaleTokens.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RemoteHint(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, WhaleTokens.SecondaryText.copy(alpha = 0.62f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(key, color = WhaleTokens.TertiaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(label, color = WhaleTokens.TertiaryText, fontSize = 17.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CountryMark(country: CountryEntry, modifier: Modifier = Modifier) {
    val color = remember(country.id) { countryColor(country.id) }
    val context = LocalContext.current
    val flagCode = remember(country.id) { flagResourceCode(country.id) }
    val flagResourceId = remember(flagCode) {
        flagCode?.let { code ->
            context.resources.getIdentifier("flag_$code", "drawable", context.packageName)
        } ?: 0
    }
    Box(
        modifier = modifier
            .width(64.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (flagResourceId == 0) color else Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (flagResourceId != 0) {
            Image(
                painter = painterResource(id = flagResourceId),
                contentDescription = "${country.label}国旗",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = country.id.take(2).uppercase(Locale.ROOT),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.07f)),
    )
}

private fun countryColor(id: String): Color {
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

private fun flagResourceCode(id: String): String? {
    val normalized = id.lowercase(Locale.ROOT)
    if (normalized == "other") return null
    return when (normalized) {
        "uk" -> "gb"
        else -> normalized.takeIf { it.length == 2 && it.all(Char::isLetter) }
    }
}
