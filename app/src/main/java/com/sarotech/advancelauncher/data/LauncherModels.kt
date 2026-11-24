package com.sarotech.advancelauncher.data

import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONArray
import org.json.JSONObject

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    val launchCount: Int = 0,
    val category: String = "Other"
)

data class HomeAppItem(
    val packageName: String,
    val page: Int,
    val col: Int,
    val row: Int
)

data class HomeFolderItem(
    val id: String,
    val name: String,
    val packages: List<String>,
    val page: Int,
    val col: Int,
    val row: Int
)

data class HomeWidgetItem(
    val widgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val label: String,
    val page: Int,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
)

data class LauncherConfig(
    val themePreset: String = "GlassDark",
    val accentHex: String = "#4FC3F7",
    val glassOpacity: Float = 0.52f,
    val neoDepth: Float = 1.0f,
    val desktopCols: Int = 4,
    val desktopRows: Int = 5,
    val drawerCols: Int = 4,
    val drawerStyle: String = "Grid",
    val drawerSort: String = "AZ",
    val dockEnabled: Boolean = true,
    val dockSlots: Int = 5,
    val dockStyle: String = "Floating",
    val iconSizeDp: Int = 52,
    val iconShape: String = "Squircle",
    val iconPackPackage: String = "",
    val badgeStyle: String = "Count",
    val showDesktopLabels: Boolean = true,
    val showDockLabels: Boolean = false,
    val showDrawerLabels: Boolean = true,
    val labelSizeSp: Int = 12,
    val clockStyle: String = "DigitalModern",
    val searchBarPlacement: String = "Top",
    val gestureSwipeUp: String = "OpenDrawer",
    val gestureSwipeDown: String = "ExpandNotifications",
    val gestureDoubleTap: String = "LockScreen",
    val pageTransition: String = "Depth",
    val hiddenPackages: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("theme_preset", themePreset)
        put("accent_hex", accentHex)
        put("glass_opacity", glassOpacity.toDouble())
        put("neo_depth", neoDepth.toDouble())
        put("desktop_cols", desktopCols)
        put("desktop_rows", desktopRows)
        put("drawer_cols", drawerCols)
        put("drawer_style", drawerStyle)
        put("drawer_sort", drawerSort)
        put("dock_enabled", dockEnabled)
        put("dock_slots", dockSlots)
        put("dock_style", dockStyle)
        put("icon_size_dp", iconSizeDp)
        put("icon_shape", iconShape)
        put("icon_pack_package", iconPackPackage)
        put("badge_style", badgeStyle)
        put("show_desktop_labels", showDesktopLabels)
        put("show_dock_labels", showDockLabels)
        put("show_drawer_labels", showDrawerLabels)
        put("label_size_sp", labelSizeSp)
        put("clock_style", clockStyle)
        put("search_bar_placement", searchBarPlacement)
        put("gesture_swipe_up", gestureSwipeUp)
        put("gesture_swipe_down", gestureSwipeDown)
        put("gesture_double_tap", gestureDoubleTap)
        put("page_transition", pageTransition)
        put("hidden_packages", JSONArray(hiddenPackages))
    }

    companion object {
        fun fromJson(raw: String): LauncherConfig {
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return LauncherConfig()
            val hiddenArr = obj.optJSONArray("hidden_packages")
            val hidden = buildList {
                if (hiddenArr != null) {
                    for (i in 0 until hiddenArr.length()) {
                        val s = hiddenArr.optString(i)
                        if (s.isNotBlank()) add(s)
                    }
                }
            }
            return LauncherConfig(
                themePreset = obj.optString("theme_preset", "GlassDark"),
                accentHex = obj.optString("accent_hex", "#4FC3F7"),
                glassOpacity = obj.optDouble("glass_opacity", 0.52).toFloat(),
                neoDepth = obj.optDouble("neo_depth", 1.0).toFloat(),
                desktopCols = obj.optInt("desktop_cols", 4),
                desktopRows = obj.optInt("desktop_rows", 5),
                drawerCols = obj.optInt("drawer_cols", 4),
                drawerStyle = obj.optString("drawer_style", "Grid"),
                drawerSort = obj.optString("drawer_sort", "AZ"),
                dockEnabled = obj.optBoolean("dock_enabled", true),
                dockSlots = obj.optInt("dock_slots", 5),
                dockStyle = obj.optString("dock_style", "Floating"),
                iconSizeDp = obj.optInt("icon_size_dp", 52),
                iconShape = obj.optString("icon_shape", "Squircle"),
                iconPackPackage = obj.optString("icon_pack_package", ""),
                badgeStyle = obj.optString("badge_style", "Count"),
                showDesktopLabels = obj.optBoolean("show_desktop_labels", true),
                showDockLabels = obj.optBoolean("show_dock_labels", false),
                showDrawerLabels = obj.optBoolean("show_drawer_labels", true),
                labelSizeSp = obj.optInt("label_size_sp", 12),
                clockStyle = obj.optString("clock_style", "DigitalModern"),
                searchBarPlacement = obj.optString("search_bar_placement", "Top"),
                gestureSwipeUp = obj.optString("gesture_swipe_up", "OpenDrawer"),
                gestureSwipeDown = obj.optString("gesture_swipe_down", "ExpandNotifications"),
                gestureDoubleTap = obj.optString("gesture_double_tap", "LockScreen"),
                pageTransition = obj.optString("page_transition", "Depth"),
                hiddenPackages = hidden
            )
        }
    }
}
