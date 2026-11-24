package com.sarotech.advancelauncher.data

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(
    val packageName: String,
    val label: String
)

class LoadedIconPack(
    val packageName: String,
    private val packResources: Resources,
    private val componentMap: Map<String, String>,
    private val packageFallbackMap: Map<String, String>
) {
    fun loadIcon(pkg: String, activityName: String): ImageBitmap? {
        val fullKey = "$pkg/$activityName"
        val shortActivity = if (activityName.startsWith(pkg)) {
            activityName.removePrefix(pkg)
        } else activityName
        val shortKey = "$pkg/$shortActivity"

        val drawableName = componentMap[fullKey]
            ?: componentMap[shortKey]
            ?: packageFallbackMap[pkg]
            ?: return null

        val resId = packResources.getIdentifier(drawableName, "drawable", packageName)
        if (resId == 0) return null

        return runCatching {
            ResourcesCompat.getDrawable(packResources, resId, null)
                ?.toBitmap(width = 128, height = 128)
                ?.asImageBitmap()
        }.getOrNull()
    }
}

object IconPackLoader {
    private val PACK_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme"
    )

    fun listInstalledPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, IconPackInfo>()

        for (action in PACK_ACTIONS) {
            val hits = runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList())
            for (ri in hits) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || found.containsKey(pkg)) continue
                val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
                found[pkg] = IconPackInfo(pkg, label)
            }
        }

        val apexIntent = Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME")
        val apexHits = runCatching { pm.queryIntentActivities(apexIntent, 0) }.getOrDefault(emptyList())
        for (ri in apexHits) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || found.containsKey(pkg)) continue
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            found[pkg] = IconPackInfo(pkg, label)
        }

        return found.values.sortedBy { it.label.lowercase() }
    }

    fun loadPack(context: Context, packPackage: String): LoadedIconPack? {
        if (packPackage.isBlank()) return null
        val packRes = runCatching {
            context.packageManager.getResourcesForApplication(packPackage)
        }.getOrNull() ?: return null

        val compMap = HashMap<String, String>()
        val pkgMap = HashMap<String, String>()

        val xmlResId = packRes.getIdentifier("appfilter", "xml", packPackage)
        if (xmlResId != 0) {
            runCatching {
                val parser = packRes.getXml(xmlResId)
                try {
                    parseAppFilter(parser, compMap, pkgMap)
                } finally {
                    parser.close()
                }
            }
        } else {
            runCatching {
                packRes.assets.open("appfilter.xml").use { stream ->
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    parseAppFilter(parser, compMap, pkgMap)
                }
            }
        }

        if (compMap.isEmpty() && pkgMap.isEmpty()) return null
        return LoadedIconPack(packPackage, packRes, compMap, pkgMap)
    }

    private fun parseAppFilter(
        parser: XmlPullParser,
        compMap: MutableMap<String, String>,
        pkgMap: MutableMap<String, String>
    ) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    val inner = component
                        .removePrefix("ComponentInfo{")
                        .removeSuffix("}")
                    if (inner.contains("/")) {
                        compMap[inner] = drawable
                        val pkg = inner.substringBefore("/")
                        if (pkg.isNotBlank() && !pkgMap.containsKey(pkg)) {
                            pkgMap[pkg] = drawable
                        }
                    }
                }
            }
            event = parser.next()
        }
    }
}
