package com.sarotech.advancelauncher.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sarotech.advancelauncher.data.AppInfo
import com.sarotech.advancelauncher.data.HomeAppItem
import com.sarotech.advancelauncher.data.HomeFolderItem
import com.sarotech.advancelauncher.data.HomeWidgetItem
import com.sarotech.advancelauncher.data.LauncherConfig
import com.sarotech.advancelauncher.ui.components.AdvanceIcons
import com.sarotech.advancelauncher.ui.components.AppIconTile
import com.sarotech.advancelauncher.ui.components.FolderIconTile
import com.sarotech.advancelauncher.ui.theme.AdvancePalette
import com.sarotech.advancelauncher.ui.theme.SurfaceStyle
import com.sarotech.advancelauncher.ui.theme.advanceRecessed
import com.sarotech.advancelauncher.ui.theme.advanceSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private sealed class ActiveDrag {
    abstract val originPage: Int
    abstract val originCol: Int
    abstract val originRow: Int

    data class AppDrag(val item: HomeAppItem, val app: AppInfo) : ActiveDrag() {
        override val originPage = item.page
        override val originCol = item.col
        override val originRow = item.row
    }

    data class FolderDrag(val folder: HomeFolderItem) : ActiveDrag() {
        override val originPage = folder.page
        override val originCol = folder.col
        override val originRow = folder.row
    }
}

@Composable
fun HomeScreen(
    config: LauncherConfig,
    palette: AdvancePalette,
    appsByPkg: Map<String, AppInfo>,
    badgeCounts: Map<String, Int>,
    homeItems: List<HomeAppItem>,
    folders: List<HomeFolderItem>,
    widgets: List<HomeWidgetItem>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenDrawer: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onOpenDefaultHome: () -> Unit,
    onGestureAction: (String) -> Unit,
    onMoveHomeItem: (HomeAppItem, Int, Int, Int) -> Unit,
    onMoveFolder: (String, Int, Int, Int) -> Unit,
    onMergeAppsIntoFolder: (HomeAppItem, HomeAppItem) -> Unit,
    onAddDraggedAppToFolder: (HomeAppItem, String) -> Unit,
    onRemoveHomeItem: (HomeAppItem) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onRemoveAppFromFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onDeleteWidget: (Int) -> Unit,
    onAppMenuRequest: (AppInfo, HomeAppItem?) -> Unit,
    dockPackages: List<String>
) {
    val maxPage = maxOf(
        1,
        (homeItems.maxOfOrNull { it.page } ?: 0),
        (folders.maxOfOrNull { it.page } ?: 0),
        (widgets.maxOfOrNull { it.page } ?: 0)
    )
    val pageCount = maxPage + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    var openFolderId by remember { mutableStateOf<String?>(null) }
    var showHomeQuickMenu by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<HomeFolderItem?>(null) }
    var editLayoutMode by remember { mutableStateOf(false) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(config.gestureDoubleTap, editLayoutMode) {
                detectTapGestures(
                    onDoubleTap = { if (!editLayoutMode) onGestureAction(config.gestureDoubleTap) },
                    onLongPress = { showHomeQuickMenu = true }
                )
            }
            .pointerInput(config.gestureSwipeUp, config.gestureSwipeDown, editLayoutMode) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (!editLayoutMode) {
                            if (dragAccum < -95f) onGestureAction(config.gestureSwipeUp)
                            else if (dragAccum > 95f) onGestureAction(config.gestureSwipeDown)
                        }
                        dragAccum = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Layout / Widget Edit Mode Top Toolbar
            if (editLayoutMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .advanceSurface(palette, cornerRadius = 22.dp, elevationScale = 0.9f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Edit Widgets & Layout",
                        color = palette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onOpenWidgetPicker) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Widget", color = palette.accent, fontSize = 12.sp)
                        }
                        TextButton(onClick = { editLayoutMode = false }) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Done", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (config.searchBarPlacement == "Top") {
                SearchPillBar(
                    palette = palette,
                    onClick = { onOpenDrawer(true) },
                    onSettingsClick = onOpenSettings
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Workspace Pager
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !editLayoutMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)

                val pageApps = homeItems.filter { it.page == pageIndex }
                val pageFolders = folders.filter { it.page == pageIndex }
                val pageWidgets = widgets.filter { it.page == pageIndex }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            when (config.pageTransition) {
                                "Depth" -> {
                                    alpha = 1f - absOffset * 0.45f
                                    val s = 1f - absOffset * 0.08f
                                    scaleX = s; scaleY = s
                                }
                                "Zoom" -> {
                                    val s = 1f - absOffset * 0.16f
                                    scaleX = s; scaleY = s
                                    alpha = 1f - absOffset * 0.35f
                                }
                                "Fade" -> alpha = 1f - absOffset * 0.75f
                                else -> alpha = 1f
                            }
                        }
                ) {
                    if (pageIndex == 0 && config.clockStyle != "Hidden" && pageWidgets.isEmpty()) {
                        ClockWidget(
                            style = config.clockStyle,
                            nowMillis = nowMillis,
                            palette = palette,
                            onLongPress = { showHomeQuickMenu = true }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (pageApps.isEmpty() && pageFolders.isEmpty() && pageWidgets.isEmpty() && pageIndex == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 18.dp)
                                    .advanceSurface(palette, cornerRadius = 22.dp, elevationScale = 0.85f)
                                    .clickable { showHomeQuickMenu = true }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Swipe up for apps • Long-press for Widgets & Themes",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        InteractiveWorkspaceGrid(
                            pageIndex = pageIndex,
                            config = config,
                            palette = palette,
                            appsByPkg = appsByPkg,
                            badgeCounts = badgeCounts,
                            pageApps = pageApps,
                            pageFolders = pageFolders,
                            pageWidgets = pageWidgets,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            editLayoutMode = editLayoutMode,
                            onLaunchApp = onLaunchApp,
                            onOpenFolder = { openFolderId = it },
                            onEditFolder = { editingFolder = it },
                            onAppMenuRequest = onAppMenuRequest,
                            onMoveHomeItem = onMoveHomeItem,
                            onMoveFolder = onMoveFolder,
                            onMergeAppsIntoFolder = onMergeAppsIntoFolder,
                            onAddDraggedAppToFolder = onAddDraggedAppToFolder,
                            onRemoveHomeItem = onRemoveHomeItem,
                            onDeleteFolder = onDeleteFolder,
                            onResizeWidget = onResizeWidget,
                            onDeleteWidget = onDeleteWidget,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            // Page Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.accent else Color(0x66FFFFFF))
                    )
                }
            }

            if (!editLayoutMode && config.searchBarPlacement == "Bottom") {
                SearchPillBar(
                    palette = palette,
                    onClick = { onOpenDrawer(true) },
                    onSettingsClick = onOpenSettings
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (config.dockEnabled) {
                DockBar(
                    config = config,
                    palette = palette,
                    dockApps = dockPackages.mapNotNull { appsByPkg[it] }.take(config.dockSlots),
                    badgeCounts = badgeCounts,
                    onLaunchApp = onLaunchApp,
                    onOpenDrawer = { onOpenDrawer(false) },
                    onLongPressApp = { app -> onAppMenuRequest(app, null) }
                )
            }
        }
    }

    val activeFolder = folders.firstOrNull { it.id == openFolderId }
    if (activeFolder != null) {
        FolderPopupDialog(
            folder = activeFolder,
            apps = activeFolder.packages.mapNotNull { appsByPkg[it] },
            badgeCounts = badgeCounts,
            config = config,
            palette = palette,
            onDismiss = { openFolderId = null },
            onLaunchApp = {
                openFolderId = null
                onLaunchApp(it)
            },
            onRemoveApp = { pkg -> onRemoveAppFromFolder(activeFolder.id, pkg) },
            onRename = { newName -> onRenameFolder(activeFolder.id, newName) },
            onDelete = {
                openFolderId = null
                onDeleteFolder(activeFolder.id)
            }
        )
    }

    val folderToEdit = editingFolder
    if (folderToEdit != null) {
        var nameInput by remember(folderToEdit.id) { mutableStateOf(folderToEdit.name) }
        AlertDialog(
            onDismissRequest = { editingFolder = null },
            title = { Text("Edit Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            onDeleteFolder(folderToEdit.id)
                            editingFolder = null
                        }
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF5350))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Folder", color = Color(0xFFEF5350))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) onRenameFolder(folderToEdit.id, nameInput.trim())
                        editingFolder = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingFolder = null }) { Text("Cancel") }
            }
        )
    }

    if (showHomeQuickMenu) {
        AlertDialog(
            onDismissRequest = { showHomeQuickMenu = false },
            title = { Text("Advance Launcher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuickMenuRow(AdvanceIcons.Widgets, "Add Android Widget") {
                        showHomeQuickMenu = false
                        onOpenWidgetPicker()
                    }
                    QuickMenuRow(Icons.Rounded.Edit, if (editLayoutMode) "Finish Editing Layout" else "Resize / Edit Widgets") {
                        showHomeQuickMenu = false
                        editLayoutMode = !editLayoutMode
                    }
                    QuickMenuRow(Icons.Rounded.Settings, "Customize (Glass, Neomorph, Packs)") {
                        showHomeQuickMenu = false
                        onOpenSettings()
                    }
                    QuickMenuRow(AdvanceIcons.Wallpaper, "Change Wallpaper") {
                        showHomeQuickMenu = false
                        onOpenWallpaper()
                    }
                    QuickMenuRow(AdvanceIcons.Apps, "Open App Drawer") {
                        showHomeQuickMenu = false
                        onOpenDrawer(false)
                    }
                    QuickMenuRow(Icons.Rounded.Home, "Set as Default Home") {
                        showHomeQuickMenu = false
                        onOpenDefaultHome()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHomeQuickMenu = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun InteractiveWorkspaceGrid(
    pageIndex: Int,
    config: LauncherConfig,
    palette: AdvancePalette,
    appsByPkg: Map<String, AppInfo>,
    badgeCounts: Map<String, Int>,
    pageApps: List<HomeAppItem>,
    pageFolders: List<HomeFolderItem>,
    pageWidgets: List<HomeWidgetItem>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    editLayoutMode: Boolean,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenFolder: (String) -> Unit,
    onEditFolder: (HomeFolderItem) -> Unit,
    onAppMenuRequest: (AppInfo, HomeAppItem?) -> Unit,
    onMoveHomeItem: (HomeAppItem, Int, Int, Int) -> Unit,
    onMoveFolder: (String, Int, Int, Int) -> Unit,
    onMergeAppsIntoFolder: (HomeAppItem, HomeAppItem) -> Unit,
    onAddDraggedAppToFolder: (HomeAppItem, String) -> Unit,
    onRemoveHomeItem: (HomeAppItem) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onDeleteWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    var dragFingerPos by remember { mutableStateOf(Offset.Zero) }
    var dragTravelPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier) {
        val cols = config.desktopCols
        val rows = config.desktopRows
        val gridWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val gridHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val cellWidthDp = maxWidth / cols
        val cellHeightDp = maxHeight / rows
        val cellWidthPx = gridWidthPx / cols
        val cellHeightPx = gridHeightPx / rows
        val tapSlopPx = with(density) { 18.dp.toPx() }
        val removeZoneHeightPx = with(density) { 46.dp.toPx() }

        val dragging = activeDrag
        val overRemoveZone = dragging != null && dragTravelPx >= tapSlopPx && dragFingerPos.y <= removeZoneHeightPx
        val hoverCol = if (dragging != null) {
            (dragFingerPos.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
        } else -1
        val hoverRow = if (dragging != null) {
            (dragFingerPos.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
        } else -1

        fun isOccupiedByWidget(col: Int, row: Int): Boolean = pageWidgets.any { w ->
            col in w.col until (w.col + w.colSpan) && row in w.row until (w.row + w.rowSpan)
        }

        // 1. Subtle drop target highlight while dragging
        if (dragging != null && dragTravelPx >= tapSlopPx && !overRemoveZone) {
            val targetApp = pageApps.firstOrNull {
                it.col == hoverCol && it.row == hoverRow &&
                    (dragging !is ActiveDrag.AppDrag || dragging.item != it)
            }
            val targetFolder = pageFolders.firstOrNull {
                it.col == hoverCol && it.row == hoverRow &&
                    (dragging !is ActiveDrag.FolderDrag || dragging.folder.id != it.id)
            }
            val isFolderTarget = dragging is ActiveDrag.AppDrag && (targetApp != null || targetFolder != null)
            val isBlocked = isOccupiedByWidget(hoverCol, hoverRow) ||
                (dragging is ActiveDrag.FolderDrag && (targetApp != null || targetFolder != null))

            val borderColor = when {
                isBlocked -> Color(0xCCEF5350)
                isFolderTarget -> Color(0xFF00E676)
                else -> palette.accent
            }
            Box(
                modifier = Modifier
                    .offset(x = cellWidthDp * hoverCol, y = cellHeightDp * hoverRow)
                    .size(width = cellWidthDp, height = cellHeightDp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(if (isFolderTarget) 28.dp else 18.dp))
                    .background(borderColor.copy(alpha = 0.18f))
                    .border(2.dp, borderColor, RoundedCornerShape(if (isFolderTarget) 28.dp else 18.dp))
            )
        }

        // 2. Render Real Android Widgets
        pageWidgets.forEach { widget ->
            val clampedCol = widget.col.coerceIn(0, cols - 1)
            val clampedRow = widget.row.coerceIn(0, rows - 1)
            val spanW = widget.colSpan.coerceIn(1, cols - clampedCol)
            val spanH = widget.rowSpan.coerceIn(1, rows - clampedRow)
            val wDp = cellWidthDp * spanW
            val hDp = cellHeightDp * spanH
            val providerInfo = remember(widget.widgetId) {
                runCatching { appWidgetManager.getAppWidgetInfo(widget.widgetId) }.getOrNull()
            }

            Box(
                modifier = Modifier
                    .offset(x = cellWidthDp * clampedCol, y = cellHeightDp * clampedRow)
                    .size(width = wDp, height = hDp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (providerInfo != null) {
                    val hostView = remember(widget.widgetId) {
                        runCatching {
                            appWidgetHost.createView(context.applicationContext, widget.widgetId, providerInfo)
                        }.getOrNull()
                    }
                    if (hostView != null) {
                        @Suppress("DEPRECATION")
                        AndroidView(
                            factory = {
                                (hostView.parent as? android.view.ViewGroup)?.removeView(hostView)
                                hostView
                            },
                            update = { view ->
                                val wVal = wDp.value.toInt().coerceAtLeast(60)
                                val hVal = hDp.value.toInt().coerceAtLeast(60)
                                runCatching {
                                    view.updateAppWidgetSize(Bundle.EMPTY, wVal, hVal, wVal, hVal)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .advanceSurface(palette, cornerRadius = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Widget unavailable", color = palette.textSecondary, fontSize = 12.sp)
                    }
                }

                // Interactive Resize / Delete Overlay in Edit Mode
                if (editLayoutMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xAA0A0E17))
                            .border(2.dp, palette.accent, RoundedCornerShape(20.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${widget.label} (${spanW}×${spanH})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteWidget(widget.widgetId) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove Widget", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WidgetResizeBtn("W-") { onResizeWidget(widget.widgetId, (spanW - 1).coerceAtLeast(1), spanH) }
                            WidgetResizeBtn("W+") { onResizeWidget(widget.widgetId, (spanW + 1).coerceAtMost(cols - clampedCol), spanH) }
                            WidgetResizeBtn("H-") { onResizeWidget(widget.widgetId, spanW, (spanH - 1).coerceAtLeast(1)) }
                            WidgetResizeBtn("H+") { onResizeWidget(widget.widgetId, spanW, (spanH + 1).coerceAtMost(rows - clampedRow)) }
                        }
                    }
                }
            }
        }

        // 3. Render Draggable Home Apps
        pageApps.forEach { item ->
            val app = appsByPkg[item.packageName] ?: return@forEach
            val c = item.col.coerceIn(0, cols - 1)
            val r = item.row.coerceIn(0, rows - 1)
            val isBeingDragged = dragging is ActiveDrag.AppDrag && dragging.item == item

            Box(
                modifier = Modifier
                    .offset(x = cellWidthDp * c, y = cellHeightDp * r)
                    .size(width = cellWidthDp, height = cellHeightDp)
                    .alpha(if (isBeingDragged) 0.2f else 1f)
                    .pointerInput(item, app) {
                        detectTapGestures(onTap = { onLaunchApp(app) })
                    }
                    .pointerInput(item, app, cols, rows, pageApps, pageFolders, pageWidgets) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeDrag = ActiveDrag.AppDrag(item, app)
                                dragTravelPx = 0f
                                dragFingerPos = Offset(
                                    c * cellWidthPx + localOffset.x,
                                    r * cellHeightPx + localOffset.y
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragFingerPos += dragAmount
                                dragTravelPx += hypot(dragAmount.x, dragAmount.y)
                            },
                            onDragCancel = { activeDrag = null },
                            onDragEnd = {
                                val endPos = dragFingerPos
                                val travel = dragTravelPx
                                activeDrag = null

                                if (travel < tapSlopPx) {
                                    onAppMenuRequest(app, item)
                                    return@detectDragGesturesAfterLongPress
                                }
                                if (endPos.y <= removeZoneHeightPx) {
                                    onRemoveHomeItem(item)
                                    return@detectDragGesturesAfterLongPress
                                }
                                val targetCol = (endPos.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                val targetRow = (endPos.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                if (targetCol == c && targetRow == r) return@detectDragGesturesAfterLongPress
                                if (isOccupiedByWidget(targetCol, targetRow)) return@detectDragGesturesAfterLongPress

                                val hitFolder = pageFolders.firstOrNull { it.col == targetCol && it.row == targetRow }
                                if (hitFolder != null) {
                                    onAddDraggedAppToFolder(item, hitFolder.id)
                                    return@detectDragGesturesAfterLongPress
                                }
                                val hitApp = pageApps.firstOrNull { it.col == targetCol && it.row == targetRow && it != item }
                                if (hitApp != null) {
                                    onMergeAppsIntoFolder(item, hitApp)
                                    return@detectDragGesturesAfterLongPress
                                }
                                onMoveHomeItem(item, pageIndex, targetCol, targetRow)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AppIconTile(
                    app = app,
                    iconSizeDp = config.iconSizeDp,
                    shapeName = config.iconShape,
                    showLabel = config.showDesktopLabels,
                    labelSizeSp = config.labelSizeSp,
                    labelColor = Color.White,
                    palette = palette,
                    onWallpaper = true,
                    badgeCount = badgeCounts[app.packageName] ?: 0,
                    badgeStyle = config.badgeStyle
                )
            }
        }

        // 4. Render Draggable Home Folders
        pageFolders.forEach { folder ->
            val c = folder.col.coerceIn(0, cols - 1)
            val r = folder.row.coerceIn(0, rows - 1)
            val isBeingDragged = dragging is ActiveDrag.FolderDrag && dragging.folder.id == folder.id
            val folderBadgeSum = folder.packages.sumOf { badgeCounts[it] ?: 0 }

            Box(
                modifier = Modifier
                    .offset(x = cellWidthDp * c, y = cellHeightDp * r)
                    .size(width = cellWidthDp, height = cellHeightDp)
                    .alpha(if (isBeingDragged) 0.2f else 1f)
                    .pointerInput(folder.id) {
                        detectTapGestures(onTap = { onOpenFolder(folder.id) })
                    }
                    .pointerInput(folder, cols, rows, pageApps, pageFolders, pageWidgets) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeDrag = ActiveDrag.FolderDrag(folder)
                                dragTravelPx = 0f
                                dragFingerPos = Offset(
                                    c * cellWidthPx + localOffset.x,
                                    r * cellHeightPx + localOffset.y
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragFingerPos += dragAmount
                                dragTravelPx += hypot(dragAmount.x, dragAmount.y)
                            },
                            onDragCancel = { activeDrag = null },
                            onDragEnd = {
                                val endPos = dragFingerPos
                                val travel = dragTravelPx
                                activeDrag = null

                                if (travel < tapSlopPx) {
                                    onEditFolder(folder)
                                    return@detectDragGesturesAfterLongPress
                                }
                                if (endPos.y <= removeZoneHeightPx) {
                                    onDeleteFolder(folder.id)
                                    return@detectDragGesturesAfterLongPress
                                }
                                val targetCol = (endPos.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                val targetRow = (endPos.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                val cellBusy = isOccupiedByWidget(targetCol, targetRow) ||
                                    pageApps.any { it.col == targetCol && it.row == targetRow } ||
                                    pageFolders.any { it.col == targetCol && it.row == targetRow && it.id != folder.id }
                                if (!cellBusy) {
                                    onMoveFolder(folder.id, pageIndex, targetCol, targetRow)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                FolderIconTile(
                    folder = folder,
                    appsByPkg = appsByPkg,
                    iconSizeDp = config.iconSizeDp,
                    shapeName = config.iconShape,
                    showLabel = config.showDesktopLabels,
                    labelSizeSp = config.labelSizeSp,
                    palette = palette,
                    badgeCount = folderBadgeSum,
                    badgeStyle = config.badgeStyle
                )
            }
        }

        // 5. Floating Top Remove Zone + Lifted Icon Overlay while Dragging
        if (dragging != null && dragTravelPx >= tapSlopPx) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (overRemoveZone) Color(0xFFE53935) else Color(0xCC1B1F2A))
                    .border(1.dp, if (overRemoveZone) Color.White else Color(0x55FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (overRemoveZone) "Release to Remove" else "Drop here to Remove • Drop on App for Folder",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragFingerPos.x - cellWidthPx / 2f).roundToInt(),
                            (dragFingerPos.y - cellHeightPx / 2f).roundToInt()
                        )
                    }
                    .size(width = cellWidthDp, height = cellHeightDp)
                    .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
                contentAlignment = Alignment.Center
            ) {
                when (dragging) {
                    is ActiveDrag.AppDrag -> AppIconTile(
                        app = dragging.app,
                        iconSizeDp = config.iconSizeDp,
                        shapeName = config.iconShape,
                        showLabel = false,
                        labelSizeSp = config.labelSizeSp,
                        labelColor = Color.White,
                        palette = palette,
                        onWallpaper = true
                    )
                    is ActiveDrag.FolderDrag -> FolderIconTile(
                        folder = dragging.folder,
                        appsByPkg = appsByPkg,
                        iconSizeDp = config.iconSizeDp,
                        shapeName = config.iconShape,
                        showLabel = false,
                        labelSizeSp = config.labelSizeSp,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetResizeBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchPillBar(
    palette: AdvancePalette,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .advanceSurface(palette, cornerRadius = 28.dp, elevationScale = 0.9f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = palette.accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Search apps or math (25*18%)...",
            color = palette.textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .advanceRecessed(palette, cornerRadius = 16.dp)
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = palette.textPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ClockWidget(
    style: String,
    nowMillis: Long,
    palette: AdvancePalette,
    onLongPress: () -> Unit
) {
    val timeText = remember(nowMillis) { SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date(nowMillis)) }
    val amPmText = remember(nowMillis) { SimpleDateFormat("a", Locale.getDefault()).format(Date(nowMillis)) }
    val dateText = remember(nowMillis) { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(nowMillis)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            "MinimalPill" -> {
                Row(
                    modifier = Modifier
                        .advanceSurface(palette, cornerRadius = 26.dp, elevationScale = 0.95f)
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$timeText $amPmText", color = palette.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(dateText, color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            "AnalogClean" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val hour = cal.get(Calendar.HOUR)
                val minute = cal.get(Calendar.MINUTE)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .advanceSurface(palette, cornerRadius = 56.dp, elevationScale = 1.0f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(96.dp)) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = minOf(cx, cy) - 6f
                            drawCircle(color = palette.accent.copy(alpha = 0.65f), radius = r, style = Stroke(width = 3f))
                            val hourAngle = ((hour + minute / 60f) * 30f - 90f) * (PI / 180.0)
                            val minAngle = (minute * 6f - 90f) * (PI / 180.0)
                            drawLine(
                                color = palette.textPrimary,
                                start = Offset(cx, cy),
                                end = Offset(cx + (r * 0.50f * cos(hourAngle)).toFloat(), cy + (r * 0.50f * sin(hourAngle)).toFloat()),
                                strokeWidth = 6.5f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = palette.accent,
                                start = Offset(cx, cy),
                                end = Offset(cx + (r * 0.74f * cos(minAngle)).toFloat(), cy + (r * 0.74f * sin(minAngle)).toFloat()),
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = palette.accent, radius = 5.5f, center = Offset(cx, cy))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dateText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(shadow = Shadow(Color(0xAA000000), Offset(0f, 1.5f), 4f))
                    )
                }
            }
            else -> {
                val cardMod = if (palette.style == SurfaceStyle.NEOMORPH) {
                    Modifier
                        .advanceSurface(palette, cornerRadius = 28.dp, elevationScale = 1.0f)
                        .padding(horizontal = 26.dp, vertical = 12.dp)
                } else Modifier.padding(vertical = 2.dp)
                val mainColor = if (palette.style == SurfaceStyle.NEOMORPH) palette.textPrimary else Color.White
                val subColor = if (palette.style == SurfaceStyle.NEOMORPH) palette.textSecondary else Color(0xEEFFFFFF)
                val textShadow = if (palette.style == SurfaceStyle.NEOMORPH) null else Shadow(Color(0xAA000000), Offset(0f, 2f), 8f)

                Column(modifier = cardMod, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(timeText, color = mainColor, fontSize = 50.sp, fontWeight = FontWeight.Light, style = TextStyle(shadow = textShadow))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(amPmText, color = palette.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    }
                    Text("$dateText  •  SAROTECH", color = subColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, style = TextStyle(shadow = textShadow))
                }
            }
        }
    }
}

@Composable
private fun DockBar(
    config: LauncherConfig,
    palette: AdvancePalette,
    dockApps: List<AppInfo>,
    badgeCounts: Map<String, Int>,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val isTransparent = config.dockStyle == "Transparent"
    val corner = if (config.dockStyle == "DockedBar") 20.dp else 30.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .advanceSurface(palette = palette, cornerRadius = corner, elevationScale = 1.05f, transparent = isTransparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dockApps.forEach { app ->
            AppIconTile(
                app = app,
                iconSizeDp = (config.iconSizeDp - 4).coerceAtLeast(36),
                shapeName = config.iconShape,
                showLabel = config.showDockLabels,
                labelSizeSp = (config.labelSizeSp - 1).coerceAtLeast(10),
                labelColor = if (isTransparent) Color.White else palette.textPrimary,
                palette = palette,
                onWallpaper = isTransparent,
                badgeCount = badgeCounts[app.packageName] ?: 0,
                badgeStyle = config.badgeStyle,
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenDrawer)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size((config.iconSizeDp - 2).coerceAtLeast(38).dp)
                    .advanceRecessed(palette, cornerRadius = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(AdvanceIcons.Apps, contentDescription = "All Apps", tint = palette.accent, modifier = Modifier.size(22.dp))
            }
            if (config.showDockLabels) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Apps",
                    color = if (isTransparent) Color.White else palette.textPrimary,
                    fontSize = (config.labelSizeSp - 1).coerceAtLeast(10).sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FolderPopupDialog(
    folder: HomeFolderItem,
    apps: List<AppInfo>,
    badgeCounts: Map<String, Int>,
    config: LauncherConfig,
    palette: AdvancePalette,
    onDismiss: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onRemoveApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember(folder.name) { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (editingName) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        if (nameDraft.isNotBlank()) onRename(nameDraft.trim())
                        editingName = false
                    }) { Text("OK", color = palette.accent) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AdvanceIcons.Folder, contentDescription = null, tint = palette.accent)
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        IconButton(onClick = { editingName = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = palette.textSecondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }
        },
        text = {
            if (apps.isEmpty()) {
                Text("Empty folder. Drag any app onto this folder or add from drawer.", color = palette.textSecondary)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppIconTile(
                            app = app,
                            iconSizeDp = 46,
                            shapeName = config.iconShape,
                            showLabel = true,
                            labelSizeSp = 11,
                            labelColor = palette.textPrimary,
                            palette = palette,
                            onWallpaper = false,
                            badgeCount = badgeCounts[app.packageName] ?: 0,
                            badgeStyle = config.badgeStyle,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onRemoveApp(app.packageName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Close", color = palette.accent)
            }
        }
    )
}
