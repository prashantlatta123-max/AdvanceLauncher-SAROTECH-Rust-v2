package com.sarotech.advancelauncher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.advancelauncher.core.RustEngine
import com.sarotech.advancelauncher.data.AppInfo
import com.sarotech.advancelauncher.data.LauncherConfig
import com.sarotech.advancelauncher.ui.components.AdvanceIcons
import com.sarotech.advancelauncher.ui.components.AppIconTile
import com.sarotech.advancelauncher.ui.components.NotificationBadgePill
import com.sarotech.advancelauncher.ui.components.iconShapeFor
import com.sarotech.advancelauncher.ui.theme.AdvancePalette
import com.sarotech.advancelauncher.ui.theme.advanceRecessed
import com.sarotech.advancelauncher.ui.theme.advanceSurface
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AppDrawerSheet(
    apps: List<AppInfo>,
    badgeCounts: Map<String, Int>,
    config: LauncherConfig,
    palette: AdvancePalette,
    autoFocusSearch: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onChangeDrawerStyle: (String) -> Unit,
    onChangeDrawerSort: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    var copiedBanner by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val visibleApps = remember(apps, config.hiddenPackages) {
        val hidden = config.hiddenPackages.toHashSet()
        apps.filterNot { it.packageName in hidden }
    }

    val appsPayloadJson = remember(visibleApps) {
        val arr = JSONArray()
        visibleApps.forEach { a ->
            arr.put(JSONObject().apply {
                put("package_name", a.packageName)
                put("label", a.label)
                put("launch_count", a.launchCount)
            })
        }
        arr.toString()
    }

    val filteredApps = remember(visibleApps, appsPayloadJson, query) {
        if (query.isBlank()) {
            visibleApps
        } else {
            val map = visibleApps.associateBy { it.packageName }
            val rustJson = RustEngine.safeFuzzySearch(appsPayloadJson, query)
            val hits = runCatching {
                val arr = JSONArray(rustJson)
                buildList {
                    for (i in 0 until arr.length()) {
                        val pkg = arr.optJSONObject(i)?.optString("package_name") ?: continue
                        map[pkg]?.let { add(it) }
                    }
                }
            }.getOrDefault(emptyList())

            if (hits.isNotEmpty() || RustEngine.isNativeLoaded) {
                hits
            } else {
                val q = query.trim().lowercase()
                visibleApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            }
        }
    }

    val mathResult = remember(query) {
        if (query.isBlank()) "" else RustEngine.safeEvaluateMath(query)
    }

    // Build A-Z letter -> index map for One-Handed Fast Scroller
    val letterIndices = remember(filteredApps) {
        val map = LinkedHashMap<String, Int>()
        filteredApps.forEachIndexed { idx, app ->
            val first = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (first in 'A'..'Z') first.toString() else "#"
            if (!map.containsKey(key)) map[key] = idx
        }
        map
    }
    val showFastRail = query.isBlank() &&
        config.drawerStyle != "Categories" &&
        config.drawerSort != "MostUsed" &&
        letterIndices.size > 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backdrop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Recessed Glass/Neomorph Search Well + Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .advanceRecessed(palette, cornerRadius = 24.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = palette.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search ${visibleApps.size} apps or math...", color = palette.textSecondary, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            copiedBanner = false
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { query = "" }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .advanceSurface(palette, cornerRadius = 22.dp, elevationScale = 0.75f)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .advanceSurface(palette, cornerRadius = 22.dp, elevationScale = 0.75f)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = palette.textPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Instant Rust Calculator Card
        if (mathResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .advanceSurface(palette, cornerRadius = 18.dp, elevationScale = 0.9f)
                    .clickable {
                        clipboard.setText(AnnotatedString(mathResult))
                        copiedBanner = true
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AdvanceIcons.Calc, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("= $mathResult", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (copiedBanner) "Copied to clipboard!" else "Rust Instant Calculator • Tap to copy",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(AdvanceIcons.Copy, contentDescription = "Copy", tint = palette.accent, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item { TactileChip("Grid", config.drawerStyle == "Grid", AdvanceIcons.Grid, palette) { onChangeDrawerStyle("Grid") } }
            item { TactileChip("List", config.drawerStyle == "List", AdvanceIcons.ListView, palette) { onChangeDrawerStyle("List") } }
            item { TactileChip("Categories", config.drawerStyle == "Categories", AdvanceIcons.Category, palette) { onChangeDrawerStyle("Categories") } }
            item { TactileChip("A–Z", config.drawerSort == "AZ", null, palette) { onChangeDrawerSort("AZ") } }
            item { TactileChip("Z–A", config.drawerSort == "ZA", null, palette) { onChangeDrawerSort("ZA") } }
            item { TactileChip("Most Used", config.drawerSort == "MostUsed", null, palette) { onChangeDrawerSort("MostUsed") } }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching apps found", color = palette.textSecondary, fontSize = 15.sp)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                val contentEndPad = if (showFastRail) 26.dp else 0.dp

                when (config.drawerStyle) {
                    "List" -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = contentEndPad),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                DrawerListRow(
                                    app = app,
                                    badgeCount = badgeCounts[app.packageName] ?: 0,
                                    config = config,
                                    palette = palette,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressApp(app) }
                                )
                            }
                        }
                    }
                    "Categories" -> {
                        val grouped = remember(filteredApps) {
                            filteredApps.groupBy { it.category }.toSortedMap()
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            grouped.forEach { (catName, catApps) ->
                                item(key = "header_$catName") {
                                    Text(
                                        text = "$catName (${catApps.size})",
                                        color = palette.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                    )
                                }
                                val chunked = catApps.chunked(config.drawerCols)
                                items(chunked.size, key = { "${catName}_row_$it" }) { rowIdx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val rowApps = chunked[rowIdx]
                                        rowApps.forEach { app ->
                                            AppIconTile(
                                                app = app,
                                                iconSizeDp = config.iconSizeDp,
                                                shapeName = config.iconShape,
                                                showLabel = config.showDrawerLabels,
                                                labelSizeSp = config.labelSizeSp,
                                                labelColor = palette.textPrimary,
                                                palette = palette,
                                                onWallpaper = false,
                                                badgeCount = badgeCounts[app.packageName] ?: 0,
                                                badgeStyle = config.badgeStyle,
                                                onClick = { onLaunchApp(app) },
                                                onLongClick = { onLongPressApp(app) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(config.drawerCols - rowApps.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(config.drawerCols),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = contentEndPad),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppIconTile(
                                    app = app,
                                    iconSizeDp = config.iconSizeDp,
                                    shapeName = config.iconShape,
                                    showLabel = config.showDrawerLabels,
                                    labelSizeSp = config.labelSizeSp,
                                    labelColor = palette.textPrimary,
                                    palette = palette,
                                    onWallpaper = false,
                                    badgeCount = badgeCounts[app.packageName] ?: 0,
                                    badgeStyle = config.badgeStyle,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressApp(app) }
                                )
                            }
                        }
                    }
                }

                // One-Handed A–Z Fast Scroller Rail
                if (showFastRail) {
                    val letters = remember(letterIndices) { letterIndices.keys.toList() }
                    var railHeightPx by remember { mutableIntStateOf(1) }

                    fun jumpToFraction(yPx: Float) {
                        if (letters.isEmpty() || railHeightPx <= 0) return
                        val idx = ((yPx / railHeightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[idx]
                        activeLetter = letter
                        val targetItem = letterIndices[letter] ?: return
                        scope.launch {
                            if (config.drawerStyle == "List") {
                                listState.scrollToItem(targetItem)
                            } else {
                                gridState.scrollToItem(targetItem)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(0.86f)
                            .width(22.dp)
                            .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
                            .pointerInput(letters) {
                                detectVerticalDragGestures(
                                    onDragStart = { off -> jumpToFraction(off.y) },
                                    onVerticalDrag = { change, _ ->
                                        change.consume()
                                        jumpToFraction(change.position.y)
                                    },
                                    onDragEnd = { activeLetter = null },
                                    onDragCancel = { activeLetter = null }
                                )
                            },
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        letters.forEach { letter ->
                            val isActive = activeLetter == letter
                            Text(
                                text = letter,
                                color = if (isActive) palette.accent else palette.textSecondary,
                                fontSize = if (isActive) 12.sp else 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    val target = letterIndices[letter] ?: return@clickable
                                    scope.launch {
                                        if (config.drawerStyle == "List") listState.scrollToItem(target)
                                        else gridState.scrollToItem(target)
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating Letter Bubble while Scrubbing
                val bubble = activeLetter
                if (bubble != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .advanceSurface(palette, cornerRadius = 36.dp, elevationScale = 1.2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bubble,
                            color = palette.accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TactileChip(
    label: String,
    selected: Boolean,
    icon: ImageVector?,
    palette: AdvancePalette,
    onClick: () -> Unit
) {
    val mod = if (selected) {
        Modifier.advanceRecessed(palette, cornerRadius = 18.dp)
    } else {
        Modifier.advanceSurface(palette, cornerRadius = 18.dp, elevationScale = 0.65f)
    }
    Row(
        modifier = mod
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.accent else palette.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (selected) palette.accent else palette.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerListRow(
    app: AppInfo,
    badgeCount: Int,
    config: LauncherConfig,
    palette: AdvancePalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = iconShapeFor(config.iconShape)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .advanceSurface(palette, cornerRadius = 18.dp, elevationScale = 0.72f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(AdvanceIcons.Apps, contentDescription = null, tint = palette.accent)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("${app.category} • Launches: ${app.launchCount}", color = palette.textSecondary, fontSize = 11.sp)
        }
        NotificationBadgePill(count = badgeCount, badgeStyle = config.badgeStyle, accent = palette.accent)
        IconButton(onClick = onLongClick) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = palette.textSecondary, modifier = Modifier.size(19.dp))
        }
    }
}
