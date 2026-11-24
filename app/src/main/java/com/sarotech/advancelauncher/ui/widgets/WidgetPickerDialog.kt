package com.sarotech.advancelauncher.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.advancelauncher.ui.components.AdvanceIcons
import com.sarotech.advancelauncher.ui.theme.AdvancePalette
import com.sarotech.advancelauncher.ui.theme.advanceSurface

data class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val colSpan: Int,
    val rowSpan: Int
)

fun estimateSpan(minDp: Int, maxSpan: Int): Int {
    val cells = ((minDp + 30) / 70).coerceAtLeast(1)
    return cells.coerceAtMost(maxSpan)
}

@Composable
fun WidgetPickerDialog(
    maxCols: Int,
    maxRows: Int,
    palette: AdvancePalette,
    onPick: (WidgetChoice) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val choices = remember(maxCols, maxRows) {
        val mgr = AppWidgetManager.getInstance(context)
        val pm = context.packageManager
        val list = runCatching { mgr.installedProviders }.getOrDefault(emptyList())
        list.mapNotNull { info ->
            val pkg = info.provider?.packageName ?: return@mapNotNull null
            val appLabel = runCatching {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            }.getOrDefault(pkg)
            val widgetLabel = runCatching { info.loadLabel(pm) }.getOrNull().orEmpty().ifEmpty { appLabel }
            WidgetChoice(
                info = info,
                appLabel = appLabel,
                widgetLabel = widgetLabel,
                colSpan = estimateSpan(info.minWidth, maxCols),
                rowSpan = estimateSpan(info.minHeight, maxRows)
            )
        }.sortedWith(compareBy<WidgetChoice> { it.appLabel.lowercase() }.thenBy { it.widgetLabel.lowercase() })
    }

    val filtered = remember(choices, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) choices
        else choices.filter {
            it.appLabel.lowercase().contains(q) || it.widgetLabel.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AdvanceIcons.Widgets, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Android Widget", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search ${choices.size} widgets...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filtered.isEmpty()) {
                    Text("No widgets found", color = palette.textSecondary, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { "${it.info.provider.packageName}/${it.info.provider.className}" }
                        ) { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .advanceSurface(palette, cornerRadius = 14.dp, elevationScale = 0.55f)
                                    .clickable { onPick(choice) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = choice.widgetLabel,
                                        color = palette.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = choice.appLabel,
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${choice.colSpan}×${choice.rowSpan}",
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = palette.accent)
            }
        }
    )
}
