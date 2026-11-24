package com.sarotech.advancelauncher.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.sarotech.advancelauncher.core.RustEngine
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AppRepository(private val context: Context) {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    @Volatile private var activePackPackage: String = ""
    @Volatile private var loadedPack: LoadedIconPack? = null

    fun loadInstalledApps(
        launchCounts: Map<String, Int>,
        sortMode: String,
        iconPackPackage: String
    ): List<AppInfo> {
        if (iconPackPackage != activePackPackage) {
            iconCache.clear()
            activePackPackage = iconPackPackage
            loadedPack = if (iconPackPackage.isNotBlank()) {
                IconPackLoader.loadPack(context, iconPackPackage)
            } else null
        }

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveList = pm.queryIntentActivities(intent, 0)
        val pack = loadedPack

        val rawMap = LinkedHashMap<String, AppInfo>()
        for (ri in resolveList) {
            val pkg = ri.activityInfo?.packageName ?: continue
            val act = ri.activityInfo?.name ?: continue
            if (pkg == context.packageName || rawMap.containsKey(pkg)) continue

            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val icon = iconCache[pkg] ?: (
                pack?.loadIcon(pkg, act)
                    ?: runCatching {
                        ri.loadIcon(pm).toBitmap(width = 128, height = 128).asImageBitmap()
                    }.getOrNull()
                )?.also { iconCache[pkg] = it }

            rawMap[pkg] = AppInfo(
                packageName = pkg,
                activityName = act,
                label = label,
                icon = icon,
                launchCount = launchCounts[pkg] ?: 0
            )
        }

        iconCache.keys.retainAll(rawMap.keys)

        val inputArr = JSONArray()
        rawMap.values.forEach { app ->
            inputArr.put(JSONObject().apply {
                put("package_name", app.packageName)
                put("label", app.label)
                put("launch_count", app.launchCount)
            })
        }

        val rustOut = RustEngine.safeSortAndCategorize(inputArr.toString(), sortMode)
        val ordered = mutableListOf<AppInfo>()
        val seen = HashSet<String>()

        runCatching {
            val arr = JSONArray(rustOut)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pkg = obj.optString("package_name")
                val cat = obj.optString("category", "Other")
                val base = rawMap[pkg] ?: continue
                if (seen.add(pkg)) ordered.add(base.copy(category = cat))
            }
        }

        if (ordered.isEmpty()) {
            val fallback = rawMap.values.toList()
            return when (sortMode) {
                "ZA" -> fallback.sortedByDescending { it.label.lowercase() }
                "MostUsed" -> fallback.sortedWith(
                    compareByDescending<AppInfo> { it.launchCount }.thenBy { it.label.lowercase() }
                )
                else -> fallback.sortedBy { it.label.lowercase() }
            }
        }

        rawMap.values.forEach { app ->
            if (seen.add(app.packageName)) ordered.add(app)
        }
        return ordered
    }

    fun pickDefaultDockApps(apps: List<AppInfo>, maxSlots: Int): List<String> {
        val preferred = listOf("dialer", "phone", "messaging", "mms", "chrome", "browser", "camera", "whatsapp")
        val picked = mutableListOf<String>()
        for (kw in preferred) {
            val match = apps.firstOrNull { app ->
                app.packageName !in picked &&
                    (app.packageName.lowercase().contains(kw) || app.label.lowercase().contains(kw))
            }
            if (match != null) picked.add(match.packageName)
            if (picked.size >= maxSlots) break
        }
        for (app in apps) {
            if (picked.size >= maxSlots) break
            if (app.packageName !in picked) picked.add(app.packageName)
        }
        return picked
    }
}
