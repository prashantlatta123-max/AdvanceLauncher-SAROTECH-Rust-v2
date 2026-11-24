package com.sarotech.advancelauncher.data

import android.content.Context
import com.sarotech.advancelauncher.core.RustEngine
import org.json.JSONArray
import org.json.JSONObject

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("advance_launcher_store", Context.MODE_PRIVATE)

    fun loadConfig(): LauncherConfig {
        val raw = prefs.getString(KEY_CONFIG, "{}") ?: "{}"
        return LauncherConfig.fromJson(RustEngine.safeNormalizeConfig(raw))
    }

    fun saveConfig(config: LauncherConfig): LauncherConfig {
        val normalized = RustEngine.safeNormalizeConfig(config.toJson().toString())
        prefs.edit().putString(KEY_CONFIG, normalized).apply()
        return LauncherConfig.fromJson(normalized)
    }

    fun loadDockPackages(): List<String> {
        val raw = prefs.getString(KEY_DOCK, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun saveDockPackages(packages: List<String>) {
        prefs.edit().putString(KEY_DOCK, JSONArray(packages).toString()).apply()
    }

    fun loadHomeItems(): List<HomeAppItem> = parseHomeItems(prefs.getString(KEY_HOME_ITEMS, "[]") ?: "[]")

    fun saveHomeItems(items: List<HomeAppItem>) {
        prefs.edit().putString(KEY_HOME_ITEMS, encodeHomeItems(items)).apply()
    }

    fun loadFolders(): List<HomeFolderItem> = parseFolders(prefs.getString(KEY_FOLDERS, "[]") ?: "[]")

    fun saveFolders(folders: List<HomeFolderItem>) {
        prefs.edit().putString(KEY_FOLDERS, encodeFolders(folders)).apply()
    }

    fun loadWidgets(): List<HomeWidgetItem> = parseWidgets(prefs.getString(KEY_WIDGETS, "[]") ?: "[]")

    fun saveWidgets(widgets: List<HomeWidgetItem>) {
        prefs.edit().putString(KEY_WIDGETS, encodeWidgets(widgets)).apply()
    }

    fun loadLaunchCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_USAGE, "{}") ?: "{}"
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, obj.optInt(k, 0))
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun incrementLaunchCount(packageName: String): Map<String, Int> {
        val map = loadLaunchCounts().toMutableMap()
        map[packageName] = (map[packageName] ?: 0) + 1
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_USAGE, obj.toString()).apply()
        return map
    }

    fun buildBackupJson(): String = JSONObject().apply {
        put("brand", "SAROTECH")
        put("app", "Advance Launcher")
        put("developer", "Prashant Latta")
        put("schema_version", 3)
        put("config", loadConfig().toJson())
        put("dock_packages", JSONArray(loadDockPackages()))
        put("home_items_json", encodeHomeItems(loadHomeItems()))
        put("folders_json", encodeFolders(loadFolders()))
    }.toString(2)

    fun importBackupJson(raw: String): Result<Triple<LauncherConfig, List<String>, Pair<List<HomeAppItem>, List<HomeFolderItem>>>> {
        val validatedRaw = RustEngine.safeValidateBackup(raw)
        val resp = runCatching { JSONObject(validatedRaw) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("Could not read backup response"))
        if (!resp.optBoolean("ok", false)) {
            return Result.failure(IllegalArgumentException(resp.optString("error", "Invalid backup file")))
        }
        val env = resp.optJSONObject("normalized_backup")
            ?: return Result.failure(IllegalArgumentException("Missing backup payload"))

        val cfg = saveConfig(LauncherConfig.fromJson(env.optJSONObject("config")?.toString() ?: "{}"))
        val dockArr = env.optJSONArray("dock_packages")
        val dock = buildList {
            if (dockArr != null) {
                for (i in 0 until dockArr.length()) {
                    val s = dockArr.optString(i)
                    if (s.isNotBlank()) add(s)
                }
            }
        }
        saveDockPackages(dock)

        val homeItems = parseHomeItems(env.optString("home_items_json", "[]"))
        saveHomeItems(homeItems)

        val folders = parseFolders(env.optString("folders_json", "[]"))
        saveFolders(folders)

        return Result.success(Triple(cfg, dock, homeItems to folders))
    }

    fun isFirstDockSeeded(): Boolean = prefs.getBoolean(KEY_DOCK_SEEDED, false)
    fun markDockSeeded() { prefs.edit().putBoolean(KEY_DOCK_SEEDED, true).apply() }

    companion object {
        private const val KEY_CONFIG = "launcher_config_json"
        private const val KEY_DOCK = "dock_packages_json"
        private const val KEY_HOME_ITEMS = "home_items_json"
        private const val KEY_FOLDERS = "home_folders_json"
        private const val KEY_WIDGETS = "home_widgets_json"
        private const val KEY_USAGE = "app_usage_counts"
        private const val KEY_DOCK_SEEDED = "dock_seeded"

        fun encodeHomeItems(items: List<HomeAppItem>): String = JSONArray().apply {
            items.forEach {
                put(JSONObject().apply {
                    put("package_name", it.packageName)
                    put("page", it.page)
                    put("col", it.col)
                    put("row", it.row)
                })
            }
        }.toString()

        fun parseHomeItems(raw: String): List<HomeAppItem> = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val pkg = o.optString("package_name")
                    if (pkg.isNotBlank()) {
                        add(HomeAppItem(pkg, o.optInt("page", 0), o.optInt("col", 0), o.optInt("row", 0)))
                    }
                }
            }
        }.getOrDefault(emptyList())

        fun encodeFolders(folders: List<HomeFolderItem>): String = JSONArray().apply {
            folders.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("packages", JSONArray(it.packages))
                    put("page", it.page)
                    put("col", it.col)
                    put("row", it.row)
                })
            }
        }.toString()

        fun parseFolders(raw: String): List<HomeFolderItem> = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val name = o.optString("name", "Folder")
                    val pkgsArr = o.optJSONArray("packages")
                    val pkgs = buildList {
                        if (pkgsArr != null) {
                            for (j in 0 until pkgsArr.length()) {
                                val p = pkgsArr.optString(j)
                                if (p.isNotBlank()) add(p)
                            }
                        }
                    }
                    if (id.isNotBlank()) {
                        add(HomeFolderItem(id, name, pkgs, o.optInt("page", 0), o.optInt("col", 0), o.optInt("row", 0)))
                    }
                }
            }
        }.getOrDefault(emptyList())

        fun encodeWidgets(widgets: List<HomeWidgetItem>): String = JSONArray().apply {
            widgets.forEach {
                put(JSONObject().apply {
                    put("widget_id", it.widgetId)
                    put("provider_pkg", it.providerPackage)
                    put("provider_cls", it.providerClass)
                    put("label", it.label)
                    put("page", it.page)
                    put("col", it.col)
                    put("row", it.row)
                    put("col_span", it.colSpan)
                    put("row_span", it.rowSpan)
                })
            }
        }.toString()

        fun parseWidgets(raw: String): List<HomeWidgetItem> = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val wid = o.optInt("widget_id", -1)
                    if (wid >= 0) {
                        add(
                            HomeWidgetItem(
                                widgetId = wid,
                                providerPackage = o.optString("provider_pkg"),
                                providerClass = o.optString("provider_cls"),
                                label = o.optString("label", "Widget"),
                                page = o.optInt("page", 0),
                                col = o.optInt("col", 0),
                                row = o.optInt("row", 0),
                                colSpan = o.optInt("col_span", 2).coerceAtLeast(1),
                                rowSpan = o.optInt("row_span", 2).coerceAtLeast(1)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
