package com.sarotech.advancelauncher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.advancelauncher.core.RustEngine
import com.sarotech.advancelauncher.data.AppInfo
import com.sarotech.advancelauncher.data.AppRepository
import com.sarotech.advancelauncher.data.ConfigStore
import com.sarotech.advancelauncher.data.HomeAppItem
import com.sarotech.advancelauncher.data.HomeFolderItem
import com.sarotech.advancelauncher.data.HomeWidgetItem
import com.sarotech.advancelauncher.data.LauncherConfig
import com.sarotech.advancelauncher.system.AdvanceAccessibilityService
import com.sarotech.advancelauncher.system.AdvanceNotificationListener
import com.sarotech.advancelauncher.ui.components.AdvanceIcons
import com.sarotech.advancelauncher.ui.screens.AppDrawerSheet
import com.sarotech.advancelauncher.ui.screens.HomeScreen
import com.sarotech.advancelauncher.ui.screens.SettingsScreen
import com.sarotech.advancelauncher.ui.theme.resolveAdvancePalette
import com.sarotech.advancelauncher.ui.widgets.WidgetChoice
import com.sarotech.advancelauncher.ui.widgets.WidgetPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var store: ConfigStore
    private lateinit var appRepo: AppRepository
    lateinit var appWidgetHost: AppWidgetHost
        private set
    lateinit var appWidgetManager: AppWidgetManager
        private set

    private var pendingWidgetChoice: WidgetChoice? = null
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var onWidgetReadyCallback: ((Int, WidgetChoice) -> Unit)? = null

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val wid = pendingWidgetId
        val choice = pendingWidgetChoice
        if (result.resultCode == RESULT_OK && wid != AppWidgetManager.INVALID_APPWIDGET_ID && choice != null) {
            configureOrCommitWidget(wid, choice)
        } else {
            if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) {
                runCatching { appWidgetHost.deleteAppWidgetId(wid) }
            }
            clearPendingWidget()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        store = ConfigStore(this)
        appRepo = AppRepository(this)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)

        setContent {
            MaterialTheme {
                AdvanceLauncherRoot(
                    store = store,
                    appRepo = appRepo,
                    activity = this
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    fun requestAddWidget(choice: WidgetChoice, onReady: (Int, WidgetChoice) -> Unit) {
        val wid = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = wid
        pendingWidgetChoice = choice
        onWidgetReadyCallback = onReady

        val allowed = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(wid, choice.info.provider)
        }.getOrDefault(false)

        if (allowed) {
            configureOrCommitWidget(wid, choice)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, choice.info.provider)
            }
            runCatching { bindWidgetLauncher.launch(intent) }.onFailure {
                runCatching { appWidgetHost.deleteAppWidgetId(wid) }
                clearPendingWidget()
                Toast.makeText(this, "Could not bind widget", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configureOrCommitWidget(wid: Int, choice: WidgetChoice) {
        if (choice.info.configure != null) {
            val started = runCatching {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this, wid, 0, REQUEST_CONFIGURE_WIDGET, null
                )
                true
            }.getOrDefault(false)
            if (started) return
        }
        onWidgetReadyCallback?.invoke(wid, choice)
        clearPendingWidget()
    }

    @Deprecated("Required by AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val wid = pendingWidgetId
            val choice = pendingWidgetChoice
            if (resultCode == RESULT_OK && wid != AppWidgetManager.INVALID_APPWIDGET_ID && choice != null) {
                onWidgetReadyCallback?.invoke(wid, choice)
            } else if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) {
                runCatching { appWidgetHost.deleteAppWidgetId(wid) }
            }
            clearPendingWidget()
        }
    }

    private fun clearPendingWidget() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetChoice = null
        onWidgetReadyCallback = null
    }

    fun deleteHostWidget(widgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
    }

    fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        runCatching { startActivity(intent) }.onFailure {
            val fallback = packageManager.getLaunchIntentForPackage(app.packageName)
            if (fallback != null) startActivity(fallback)
            else Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("WrongConstant")
    fun expandNotifications() {
        runCatching {
            val statusBarService = getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            clazz.getMethod("expandNotificationsPanel").invoke(statusBarService)
        }
    }

    fun openWallpaperPicker() {
        runCatching { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
    }

    fun openDefaultHomeSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openAppInfo(pkg: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$pkg")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    fun uninstallApp(pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    companion object {
        private const val WIDGET_HOST_ID = 0x5341524F
        private const val REQUEST_CONFIGURE_WIDGET = 9042
    }
}

@Composable
private fun AdvanceLauncherRoot(
    store: ConfigStore,
    appRepo: AppRepository,
    activity: MainActivity
) {
    var config by remember { mutableStateOf(store.loadConfig()) }
    var dockPackages by remember { mutableStateOf(store.loadDockPackages()) }
    var homeItems by remember { mutableStateOf(store.loadHomeItems()) }
    var folders by remember { mutableStateOf(store.loadFolders()) }
    var widgets by remember { mutableStateOf(store.loadWidgets()) }
    var launchCounts by remember { mutableStateOf(store.loadLaunchCounts()) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    val badgeCounts by AdvanceNotificationListener.badgeCounts.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    var autoFocusSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showLockPrompt by remember { mutableStateOf(false) }

    var contextApp by remember { mutableStateOf<AppInfo?>(null) }
    var contextHomeItem by remember { mutableStateOf<HomeAppItem?>(null) }
    var folderPickerApp by remember { mutableStateOf<AppInfo?>(null) }

    DisposableEffect(activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                reloadTrigger++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            activity.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { activity.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(reloadTrigger, config.drawerSort, config.iconPackPackage, launchCounts) {
        val loaded = withContext(Dispatchers.IO) {
            appRepo.loadInstalledApps(launchCounts, config.drawerSort, config.iconPackPackage)
        }
        apps = loaded
        if (!store.isFirstDockSeeded() && dockPackages.isEmpty() && loaded.isNotEmpty()) {
            val defaults = appRepo.pickDefaultDockApps(loaded, (config.dockSlots - 1).coerceAtLeast(3))
            dockPackages = defaults
            store.saveDockPackages(defaults)
            store.markDockSeeded()
        }
    }

    val palette = remember(config) { resolveAdvancePalette(config) }
    val appsByPkg = remember(apps) { apps.associateBy { it.packageName } }

    fun updateConfig(newCfg: LauncherConfig) {
        config = store.saveConfig(newCfg)
    }

    fun handleLaunch(app: AppInfo) {
        launchCounts = store.incrementLaunchCount(app.packageName)
        showDrawer = false
        activity.launchApp(app)
    }

    fun handleGesture(action: String) {
        when (action) {
            "OpenDrawer" -> { autoFocusSearch = false; showDrawer = true }
            "OpenSearch" -> { autoFocusSearch = true; showDrawer = true }
            "ExpandNotifications" -> activity.expandNotifications()
            "LockScreen" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    Toast.makeText(activity, "Screen lock gesture requires Android 9+", Toast.LENGTH_SHORT).show()
                } else if (!AdvanceAccessibilityService.lockScreenNow()) {
                    showLockPrompt = true
                }
            }
            "OpenSettings" -> showSettings = true
        }
    }

    fun findFreeRect(colSpan: Int = 1, rowSpan: Int = 1): Triple<Int, Int, Int> {
        val occArr = JSONArray()
        homeItems.forEach {
            occArr.put(JSONObject().apply {
                put("page", it.page); put("col", it.col); put("row", it.row)
                put("col_span", 1); put("row_span", 1)
            })
        }
        folders.forEach {
            occArr.put(JSONObject().apply {
                put("page", it.page); put("col", it.col); put("row", it.row)
                put("col_span", 1); put("row_span", 1)
            })
        }
        widgets.forEach {
            occArr.put(JSONObject().apply {
                put("page", it.page); put("col", it.col); put("row", it.row)
                put("col_span", it.colSpan); put("row_span", it.rowSpan)
            })
        }
        val freeJson = RustEngine.safeFindNextFreeRect(
            occArr.toString(), config.desktopCols, config.desktopRows, colSpan, rowSpan
        )
        val obj = runCatching { JSONObject(freeJson) }.getOrNull()
        return Triple(
            obj?.optInt("page", 0) ?: 0,
            obj?.optInt("col", 0) ?: 0,
            obj?.optInt("row", 0) ?: 0
        )
    }

    fun pinAppToHome(app: AppInfo) {
        val (page, col, row) = findFreeRect(1, 1)
        val updated = homeItems + HomeAppItem(app.packageName, page, col, row)
        homeItems = updated
        store.saveHomeItems(updated)
        Toast.makeText(activity, "Pinned ${app.label} to Home", Toast.LENGTH_SHORT).show()
    }

    fun toggleDock(app: AppInfo) {
        val updated = if (app.packageName in dockPackages) {
            dockPackages - app.packageName
        } else {
            (dockPackages + app.packageName).takeLast(config.dockSlots)
        }
        dockPackages = updated
        store.saveDockPackages(updated)
    }

    BackHandler(enabled = showSettings || showDrawer) {
        when {
            showSettings -> showSettings = false
            showDrawer -> showDrawer = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            config = config,
            palette = palette,
            appsByPkg = appsByPkg,
            badgeCounts = badgeCounts,
            homeItems = homeItems,
            folders = folders,
            widgets = widgets,
            appWidgetHost = activity.appWidgetHost,
            appWidgetManager = activity.appWidgetManager,
            onLaunchApp = ::handleLaunch,
            onOpenDrawer = { focusSearch ->
                autoFocusSearch = focusSearch
                showDrawer = true
            },
            onOpenSettings = { showSettings = true },
            onOpenWallpaper = { activity.openWallpaperPicker() },
            onOpenWidgetPicker = { showWidgetPicker = true },
            onOpenDefaultHome = { activity.openDefaultHomeSettings() },
            onGestureAction = ::handleGesture,
            onMoveHomeItem = { item, page, col, row ->
                val updated = homeItems.map { if (it == item) it.copy(page = page, col = col, row = row) else it }
                homeItems = updated
                store.saveHomeItems(updated)
            },
            onMoveFolder = { id, page, col, row ->
                val updated = folders.map { if (it.id == id) it.copy(page = page, col = col, row = row) else it }
                folders = updated
                store.saveFolders(updated)
            },
            onMergeAppsIntoFolder = { dragged, target ->
                val remaining = homeItems.filterNot { it == dragged || it == target }
                val newFolder = HomeFolderItem(
                    id = UUID.randomUUID().toString(),
                    name = "Folder",
                    packages = listOf(target.packageName, dragged.packageName).distinct(),
                    page = target.page,
                    col = target.col,
                    row = target.row
                )
                homeItems = remaining
                store.saveHomeItems(remaining)
                val updatedFolders = folders + newFolder
                folders = updatedFolders
                store.saveFolders(updatedFolders)
                Toast.makeText(activity, "Created Folder", Toast.LENGTH_SHORT).show()
            },
            onAddDraggedAppToFolder = { dragged, folderId ->
                val remaining = homeItems.filterNot { it == dragged }
                val updatedFolders = folders.map { f ->
                    if (f.id == folderId && dragged.packageName !in f.packages) {
                        f.copy(packages = f.packages + dragged.packageName)
                    } else f
                }
                homeItems = remaining
                store.saveHomeItems(remaining)
                folders = updatedFolders
                store.saveFolders(updatedFolders)
            },
            onRemoveHomeItem = { item ->
                val updated = homeItems.filterNot { it == item }
                homeItems = updated
                store.saveHomeItems(updated)
                Toast.makeText(activity, "Removed from Home", Toast.LENGTH_SHORT).show()
            },
            onRenameFolder = { id, newName ->
                val updated = folders.map { if (it.id == id) it.copy(name = newName) else it }
                folders = updated
                store.saveFolders(updated)
            },
            onRemoveAppFromFolder = { id, pkg ->
                val updated = folders.map {
                    if (it.id == id) it.copy(packages = it.packages - pkg) else it
                }
                folders = updated
                store.saveFolders(updated)
            },
            onDeleteFolder = { id ->
                val updated = folders.filterNot { it.id == id }
                folders = updated
                store.saveFolders(updated)
            },
            onResizeWidget = { wid, newW, newH ->
                val updated = widgets.map {
                    if (it.widgetId == wid) it.copy(colSpan = newW, rowSpan = newH) else it
                }
                widgets = updated
                store.saveWidgets(updated)
            },
            onDeleteWidget = { wid ->
                activity.deleteHostWidget(wid)
                val updated = widgets.filterNot { it.widgetId == wid }
                widgets = updated
                store.saveWidgets(updated)
                Toast.makeText(activity, "Widget removed", Toast.LENGTH_SHORT).show()
            },
            onAppMenuRequest = { app, homeItem ->
                contextApp = app
                contextHomeItem = homeItem
            },
            dockPackages = dockPackages
        )

        AnimatedVisibility(
            visible = showDrawer && !showSettings,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut()
        ) {
            AppDrawerSheet(
                apps = apps,
                badgeCounts = badgeCounts,
                config = config,
                palette = palette,
                autoFocusSearch = autoFocusSearch,
                onLaunchApp = ::handleLaunch,
                onLongPressApp = { app ->
                    contextApp = app
                    contextHomeItem = null
                },
                onChangeDrawerStyle = { style -> updateConfig(config.copy(drawerStyle = style)) },
                onChangeDrawerSort = { sort -> updateConfig(config.copy(drawerSort = sort)) },
                onOpenSettings = {
                    showDrawer = false
                    showSettings = true
                },
                onClose = { showDrawer = false }
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SettingsScreen(
                config = config,
                palette = palette,
                allApps = apps,
                onUpdateConfig = ::updateConfig,
                onExportBackup = { store.buildBackupJson() },
                onImportBackup = { raw ->
                    store.importBackupJson(raw).map { (cfg, dock, homePair) ->
                        config = cfg
                        dockPackages = dock
                        homeItems = homePair.first
                        folders = homePair.second
                    }
                },
                onResetDefaults = {
                    updateConfig(LauncherConfig())
                    Toast.makeText(activity, "Reset to default settings", Toast.LENGTH_SHORT).show()
                },
                onOpenDefaultHome = { activity.openDefaultHomeSettings() },
                onOpenWallpaper = { activity.openWallpaperPicker() },
                onOpenWidgetPicker = {
                    showSettings = false
                    showWidgetPicker = true
                },
                onClose = { showSettings = false }
            )
        }
    }

    // Real Android Widget Picker Dialog
    if (showWidgetPicker) {
        WidgetPickerDialog(
            maxCols = config.desktopCols,
            maxRows = config.desktopRows,
            palette = palette,
            onPick = { choice ->
                showWidgetPicker = false
                activity.requestAddWidget(choice) { wid, picked ->
                    val (page, col, row) = findFreeRect(picked.colSpan, picked.rowSpan)
                    val placed = HomeWidgetItem(
                        widgetId = wid,
                        providerPackage = picked.info.provider.packageName,
                        providerClass = picked.info.provider.className,
                        label = picked.widgetLabel,
                        page = page,
                        col = col,
                        row = row,
                        colSpan = picked.colSpan,
                        rowSpan = picked.rowSpan
                    )
                    val updated = widgets + placed
                    widgets = updated
                    store.saveWidgets(updated)
                    Toast.makeText(activity, "Added ${picked.widgetLabel} widget", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showWidgetPicker = false }
        )
    }

    // Friendly Double-Tap Lock Accessibility Prompt
    if (showLockPrompt) {
        AlertDialog(
            onDismissRequest = { showLockPrompt = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Enable Double-Tap Screen Lock") },
            text = {
                Text(
                    "To lock the screen with a gesture, enable Advance Launcher in Android Accessibility Settings. " +
                        "Advance Launcher reads zero screen content and stays 100% offline."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLockPrompt = false
                    AdvanceAccessibilityService.openAccessibilitySettings(activity)
                }) { Text("Open Accessibility") }
            },
            dismissButton = {
                TextButton(onClick = { showLockPrompt = false }) { Text("Cancel") }
            }
        )
    }

    // App Context Dialog
    val selectedApp = contextApp
    if (selectedApp != null) {
        val inDock = selectedApp.packageName in dockPackages
        val pinnedHome = contextHomeItem
        AlertDialog(
            onDismissRequest = { contextApp = null; contextHomeItem = null },
            title = { Text(selectedApp.label, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (pinnedHome != null) {
                        ContextRow(AdvanceIcons.Pin, "Remove from Home Screen") {
                            val updated = homeItems.filterNot { it == pinnedHome }
                            homeItems = updated
                            store.saveHomeItems(updated)
                            contextApp = null; contextHomeItem = null
                            Toast.makeText(activity, "Removed from Home", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        ContextRow(AdvanceIcons.Pin, "Pin to Home Screen") {
                            pinAppToHome(selectedApp)
                            contextApp = null
                        }
                    }
                    ContextRow(AdvanceIcons.Pin, if (inDock) "Remove from Dock" else "Add to Dock") {
                        toggleDock(selectedApp)
                        contextApp = null; contextHomeItem = null
                    }
                    ContextRow(AdvanceIcons.Folder, "Add to Home Folder") {
                        folderPickerApp = selectedApp
                        contextApp = null; contextHomeItem = null
                    }
                    ContextRow(AdvanceIcons.EyeOff, "Hide App from Drawer") {
                        updateConfig(config.copy(hiddenPackages = config.hiddenPackages + selectedApp.packageName))
                        contextApp = null; contextHomeItem = null
                    }
                    ContextRow(Icons.Rounded.Info, "App Info") {
                        activity.openAppInfo(selectedApp.packageName)
                        contextApp = null; contextHomeItem = null
                    }
                    ContextRow(Icons.Rounded.Delete, "Uninstall", tint = Color(0xFFEF5350)) {
                        activity.uninstallApp(selectedApp.packageName)
                        contextApp = null; contextHomeItem = null
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextApp = null; contextHomeItem = null }) { Text("Close") }
            }
        )
    }

    // Add App to Folder Dialog
    val appForFolder = folderPickerApp
    if (appForFolder != null) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { folderPickerApp = null },
            title = { Text("Add ${appForFolder.label} to Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val updated = folders.map {
                                        if (it.id == folder.id && appForFolder.packageName !in it.packages) {
                                            it.copy(packages = it.packages + appForFolder.packageName)
                                        } else it
                                    }
                                    folders = updated
                                    store.saveFolders(updated)
                                    folderPickerApp = null
                                    Toast.makeText(activity, "Added to ${folder.name}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AdvanceIcons.Folder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("${folder.name} (${folder.packages.size})")
                        }
                    }
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Or create new folder") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderName.trim().ifEmpty { "Folder ${folders.size + 1}" }
                        val (page, col, row) = findFreeRect(1, 1)
                        val created = HomeFolderItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            packages = listOf(appForFolder.packageName),
                            page = page,
                            col = col,
                            row = row
                        )
                        val updated = folders + created
                        folders = updated
                        store.saveFolders(updated)
                        folderPickerApp = null
                        Toast.makeText(activity, "Created folder $name", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Create Folder") }
            },
            dismissButton = {
                TextButton(onClick = { folderPickerApp = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContextRow(
    icon: ImageVector,
    label: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = tint)
    }
}
