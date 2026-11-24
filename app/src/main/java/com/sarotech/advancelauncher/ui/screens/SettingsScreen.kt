package com.sarotech.advancelauncher.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.advancelauncher.BuildConfig
import com.sarotech.advancelauncher.R
import com.sarotech.advancelauncher.core.RustEngine
import com.sarotech.advancelauncher.data.AppInfo
import com.sarotech.advancelauncher.data.IconPackLoader
import com.sarotech.advancelauncher.data.LauncherConfig
import com.sarotech.advancelauncher.system.AdvanceAccessibilityService
import com.sarotech.advancelauncher.system.AdvanceNotificationListener
import com.sarotech.advancelauncher.ui.components.AdvanceIcons
import com.sarotech.advancelauncher.ui.components.iconShapeFor
import com.sarotech.advancelauncher.ui.theme.AdvancePalette
import com.sarotech.advancelauncher.ui.theme.SurfaceStyle
import com.sarotech.advancelauncher.ui.theme.advanceRecessed
import com.sarotech.advancelauncher.ui.theme.advanceSurface
import com.sarotech.advancelauncher.ui.theme.parseHexColor
import kotlin.math.roundToInt

private val THEME_PRESETS = listOf(
    "GlassDark" to "Glass Dark",
    "GlassLight" to "Glass Light",
    "NeomorphDark" to "Neomorph Dark",
    "NeomorphLight" to "Neomorph Light"
)

private val ACCENT_SWATCHES = listOf(
    "#4FC3F7", "#7C4DFF", "#00E676", "#FF5252",
    "#FFB300", "#FF4081", "#26C6DA", "#FFFFFF"
)

private val ICON_SHAPES = listOf("Squircle", "Circle", "RoundedSquare", "Teardrop", "Hexagon")
private val BADGE_STYLES = listOf("Count", "Dot", "Off")
private val CLOCK_STYLES = listOf("DigitalModern", "MinimalPill", "AnalogClean", "Hidden")
private val SEARCH_PLACEMENTS = listOf("Top", "Bottom", "Hidden")
private val DOCK_STYLES = listOf("Floating", "DockedBar", "Transparent")
private val PAGE_TRANSITIONS = listOf("Depth", "Zoom", "Fade", "Slide")
private val GESTURE_ACTIONS = listOf(
    "OpenDrawer", "OpenSearch", "ExpandNotifications", "LockScreen", "OpenSettings", "None"
)

@Composable
fun SettingsScreen(
    config: LauncherConfig,
    palette: AdvancePalette,
    allApps: List<AppInfo>,
    onUpdateConfig: (LauncherConfig) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Result<Unit>,
    onResetDefaults: () -> Unit,
    onOpenDefaultHome: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showHiddenDialog by remember { mutableStateOf(false) }

    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val installedIconPacks = remember(resumeTick) { IconPackLoader.listInstalledPacks(context) }
    val notifAccessGranted = remember(resumeTick) { AdvanceNotificationListener.isEnabled(context) }
    val lockServiceActive = remember(resumeTick) { AdvanceAccessibilityService.isRunning() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onExportBackup().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Backup exported!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { json ->
                onImportBackup(json).onSuccess {
                    Toast.makeText(context, "Backup restored via Rust validator!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Invalid backup", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val screenBg = when (palette.style) {
        SurfaceStyle.NEOMORPH -> palette.backdrop
        SurfaceStyle.GLASS -> if (palette.isLight) Color(0xFFDCE6F7) else Color(0xFF090E18)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stringResource(R.string.app_name)} Settings",
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stringResource(R.string.brand_name)} • Daily Driver Studio",
                    color = palette.accent,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { LiveThemePreviewCard(config = config, palette = palette) }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenDefaultHome,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Default", color = Color(0xFF09101A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenWidgetPicker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(AdvanceIcons.Widgets, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Widgets", color = palette.textPrimary, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenWallpaper,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(AdvanceIcons.Wallpaper, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Wall", color = palette.textPrimary, fontSize = 12.sp)
                    }
                }
            }

            // 1. Theme Studio (Glass & Neomorphism)
            item {
                SectionCard("Theme Style (Glass & Neomorphism)", palette) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(THEME_PRESETS) { (key, label) ->
                            val selected = config.themePreset == key
                            Box(
                                modifier = (if (selected) {
                                    Modifier.advanceRecessed(palette, cornerRadius = 16.dp)
                                } else {
                                    Modifier.advanceSurface(palette, cornerRadius = 16.dp, elevationScale = 0.65f)
                                })
                                    .clickable { onUpdateConfig(config.copy(themePreset = key)) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) palette.accent else palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Accent Color", color = palette.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ACCENT_SWATCHES.forEach { hex ->
                            val swatchColor = parseHexColor(hex)
                            val selected = config.accentHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) palette.textPrimary else palette.border,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateConfig(config.copy(accentHex = hex)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF0A0D14), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (palette.style == SurfaceStyle.GLASS) {
                        SliderRow(
                            title = "Glass Frost Opacity",
                            valueLabel = "${(config.glassOpacity * 100).roundToInt()}%",
                            value = config.glassOpacity,
                            range = 0.18f..0.92f,
                            palette = palette,
                            onValueChange = { onUpdateConfig(config.copy(glassOpacity = it)) }
                        )
                    } else {
                        SliderRow(
                            title = "Neomorphism 3D Shadow Depth",
                            valueLabel = "${(config.neoDepth * 100).roundToInt()}%",
                            value = config.neoDepth,
                            range = 0.4f..1.8f,
                            palette = palette,
                            onValueChange = { onUpdateConfig(config.copy(neoDepth = it)) }
                        )
                    }
                }
            }

            // 2. Icons, Play Store Icon Packs & Notification Badges
            item {
                SectionCard("Icons, Icon Packs & Badges", palette) {
                    ChoiceRow("Icon Shape", ICON_SHAPES, config.iconShape, palette) {
                        onUpdateConfig(config.copy(iconShape = it))
                    }
                    Spacer(Modifier.height(12.dp))

                    // Play Store Icon Pack Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AdvanceIcons.Palette, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play Store Icon Pack", color = palette.textSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 3.dp)
                    ) {
                        item {
                            val sel = config.iconPackPackage.isEmpty()
                            Box(
                                modifier = (if (sel) Modifier.advanceRecessed(palette, cornerRadius = 14.dp)
                                else Modifier.advanceSurface(palette, cornerRadius = 14.dp, elevationScale = 0.55f))
                                    .clickable { onUpdateConfig(config.copy(iconPackPackage = "")) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text("System Default", color = if (sel) palette.accent else palette.textPrimary, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        items(installedIconPacks, key = { it.packageName }) { pack ->
                            val sel = config.iconPackPackage == pack.packageName
                            Box(
                                modifier = (if (sel) Modifier.advanceRecessed(palette, cornerRadius = 14.dp)
                                else Modifier.advanceSurface(palette, cornerRadius = 14.dp, elevationScale = 0.55f))
                                    .clickable { onUpdateConfig(config.copy(iconPackPackage = pack.packageName)) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(pack.label, color = if (sel) palette.accent else palette.textPrimary, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    if (installedIconPacks.isEmpty()) {
                        Text(
                            text = "Install any ADW / Nova / Apex Icon Pack from Play Store to see it here.",
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Notification Badges
                    ChoiceRow("Notification Badges", BADGE_STYLES, config.badgeStyle, palette) {
                        onUpdateConfig(config.copy(badgeStyle = it))
                    }
                    if (config.badgeStyle != "Off" && !notifAccessGranted) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { AdvanceNotificationListener.openSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(AdvanceIcons.Bell, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Notification Access for Badges", color = palette.textPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SliderRow(
                        title = "Icon Size",
                        valueLabel = "${config.iconSizeDp} dp",
                        value = config.iconSizeDp.toFloat(),
                        range = 36f..72f,
                        palette = palette,
                        onValueChange = { onUpdateConfig(config.copy(iconSizeDp = it.roundToInt())) }
                    )
                    SliderRow(
                        title = "Label Text Size",
                        valueLabel = "${config.labelSizeSp} sp",
                        value = config.labelSizeSp.toFloat(),
                        range = 10f..16f,
                        palette = palette,
                        onValueChange = { onUpdateConfig(config.copy(labelSizeSp = it.roundToInt())) }
                    )
                    SwitchRow("Home Labels", config.showDesktopLabels, palette) {
                        onUpdateConfig(config.copy(showDesktopLabels = it))
                    }
                    SwitchRow("Dock Labels", config.showDockLabels, palette) {
                        onUpdateConfig(config.copy(showDockLabels = it))
                    }
                    SwitchRow("Drawer Labels", config.showDrawerLabels, palette) {
                        onUpdateConfig(config.copy(showDrawerLabels = it))
                    }
                }
            }

            // 3. Home Screen & Grid
            item {
                SectionCard("Home Screen & Grid", palette) {
                    SliderRow(
                        title = "Desktop Columns",
                        valueLabel = "${config.desktopCols}",
                        value = config.desktopCols.toFloat(),
                        range = 3f..8f,
                        palette = palette,
                        onValueChange = { onUpdateConfig(config.copy(desktopCols = it.roundToInt())) }
                    )
                    SliderRow(
                        title = "Desktop Rows",
                        valueLabel = "${config.desktopRows}",
                        value = config.desktopRows.toFloat(),
                        range = 4f..10f,
                        palette = palette,
                        onValueChange = { onUpdateConfig(config.copy(desktopRows = it.roundToInt())) }
                    )
                    ChoiceRow("Clock Widget", CLOCK_STYLES, config.clockStyle, palette) {
                        onUpdateConfig(config.copy(clockStyle = it))
                    }
                    Spacer(Modifier.height(10.dp))
                    ChoiceRow("Search Bar", SEARCH_PLACEMENTS, config.searchBarPlacement, palette) {
                        onUpdateConfig(config.copy(searchBarPlacement = it))
                    }
                    Spacer(Modifier.height(10.dp))
                    ChoiceRow("Page Transition", PAGE_TRANSITIONS, config.pageTransition, palette) {
                        onUpdateConfig(config.copy(pageTransition = it))
                    }
                }
            }

            // 4. Dock
            item {
                SectionCard("Dock Customization", palette) {
                    SwitchRow("Enable Bottom Dock", config.dockEnabled, palette) {
                        onUpdateConfig(config.copy(dockEnabled = it))
                    }
                    if (config.dockEnabled) {
                        SliderRow(
                            title = "Dock App Slots",
                            valueLabel = "${config.dockSlots}",
                            value = config.dockSlots.toFloat(),
                            range = 3f..7f,
                            palette = palette,
                            onValueChange = { onUpdateConfig(config.copy(dockSlots = it.roundToInt())) }
                        )
                        ChoiceRow("Dock Style", DOCK_STYLES, config.dockStyle, palette) {
                            onUpdateConfig(config.copy(dockStyle = it))
                        }
                    }
                }
            }

            // 5. Drawer & Privacy
            item {
                SectionCard("App Drawer & Hidden Apps", palette) {
                    SliderRow(
                        title = "Drawer Columns",
                        valueLabel = "${config.drawerCols}",
                        value = config.drawerCols.toFloat(),
                        range = 3f..7f,
                        palette = palette,
                        onValueChange = { onUpdateConfig(config.copy(drawerCols = it.roundToInt())) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showHiddenDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AdvanceIcons.EyeOff, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hidden Apps (${config.hiddenPackages.size})", color = palette.textPrimary)
                    }
                }
            }

            // 6. Gestures & Double-Tap Screen Lock
            item {
                SectionCard("Gestures & Double-Tap Lock", palette) {
                    ChoiceRow("Swipe Up", GESTURE_ACTIONS, config.gestureSwipeUp, palette) {
                        onUpdateConfig(config.copy(gestureSwipeUp = it))
                    }
                    Spacer(Modifier.height(10.dp))
                    ChoiceRow("Swipe Down", GESTURE_ACTIONS, config.gestureSwipeDown, palette) {
                        onUpdateConfig(config.copy(gestureSwipeDown = it))
                    }
                    Spacer(Modifier.height(10.dp))
                    ChoiceRow("Double Tap Home", GESTURE_ACTIONS, config.gestureDoubleTap, palette) {
                        onUpdateConfig(config.copy(gestureDoubleTap = it))
                    }
                    if (!lockServiceActive) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { AdvanceAccessibilityService.openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Accessibility for Screen Lock", color = palette.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 7. Backup & Restore
            item {
                SectionCard("Backup & Restore (Rust Validated)", palette) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportLauncher.launch("AdvanceLauncher_SAROTECH_Backup.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(AdvanceIcons.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export JSON", color = Color(0xFF09101A), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import JSON", color = palette.textPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onResetDefaults) {
                        Text("Reset All Settings", color = Color(0xFFEF5350))
                    }
                }
            }

            // 8. About SAROTECH & Prashant Latta
            item {
                SectionCard("About", palette) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.sarotech_advance_logo),
                            contentDescription = "Advance Launcher Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.app_name), color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Brand: ${stringResource(R.string.brand_name)}", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.developer_credit), color = palette.textSecondary, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = palette.textSecondary, fontSize = 12.sp)
                    Text("Engine: ${RustEngine.safeEngineVersion()}", color = palette.textSecondary, fontSize = 12.sp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showHiddenDialog) {
        val hiddenSet = config.hiddenPackages.toSet()
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            title = { Text("Hidden Apps") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(allApps, key = { it.packageName }) { app ->
                        val isHidden = app.packageName in hiddenSet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = if (isHidden) {
                                        config.hiddenPackages - app.packageName
                                    } else {
                                        config.hiddenPackages + app.packageName
                                    }
                                    onUpdateConfig(config.copy(hiddenPackages = updated))
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.label, fontSize = 14.sp)
                            Icon(
                                imageVector = if (isHidden) AdvanceIcons.EyeOff else AdvanceIcons.EyeOn,
                                contentDescription = null,
                                tint = if (isHidden) Color(0xFFEF5350) else palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun LiveThemePreviewCard(config: LauncherConfig, palette: AdvancePalette) {
    val shape = iconShapeFor(config.iconShape)
    val previewBgMod = if (palette.style == SurfaceStyle.GLASS) {
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF6A11CB))))
            .padding(14.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(palette.backdrop)
            .padding(14.dp)
    }

    Box(modifier = Modifier.fillMaxWidth().then(previewBgMod)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .advanceSurface(palette, cornerRadius = 20.dp, elevationScale = 1.0f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LIVE PREVIEW • ${config.themePreset.uppercase()}",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .advanceRecessed(palette, cornerRadius = 16.dp)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search apps...", color = palette.textSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size((config.iconSizeDp * 0.72f).dp)
                            .advanceSurface(palette, cornerRadius = 14.dp, elevationScale = 0.7f)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(palette.accent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AdvanceIcons.Apps, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, palette: AdvancePalette, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .advanceSurface(palette, cornerRadius = 22.dp, elevationScale = 0.9f)
            .padding(16.dp)
    ) {
        Text(title, color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<String>,
    selected: String,
    palette: AdvancePalette,
    onSelect: (String) -> Unit
) {
    Text(title, color = palette.textSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 3.dp)) {
        items(options) { item ->
            val isSel = item == selected
            Box(
                modifier = (if (isSel) Modifier.advanceRecessed(palette, cornerRadius = 14.dp)
                else Modifier.advanceSurface(palette, cornerRadius = 14.dp, elevationScale = 0.55f))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = item,
                    color = if (isSel) palette.accent else palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: AdvancePalette,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = palette.textPrimary, fontSize = 13.sp)
            Text(valueLabel, color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, palette: AdvancePalette, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = palette.textPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF09101A), checkedTrackColor = palette.accent)
        )
    }
}
