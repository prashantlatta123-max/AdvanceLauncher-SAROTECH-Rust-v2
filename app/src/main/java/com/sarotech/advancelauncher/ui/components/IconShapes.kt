package com.sarotech.advancelauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.advancelauncher.data.AppInfo
import com.sarotech.advancelauncher.data.HomeFolderItem
import com.sarotech.advancelauncher.ui.theme.AdvancePalette
import com.sarotech.advancelauncher.ui.theme.SurfaceStyle
import com.sarotech.advancelauncher.ui.theme.advanceSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun iconShapeFor(shapeName: String): Shape = when (shapeName) {
    "Circle" -> CircleShape
    "RoundedSquare" -> RoundedCornerShape(22)
    "Teardrop" -> RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomStartPercent = 50,
        bottomEndPercent = 14
    )
    "Hexagon" -> GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        for (i in 0 until 6) {
            val angle = (PI / 3.0) * i - (PI / 2.0)
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    else -> RoundedCornerShape(32)
}

@Composable
fun NotificationBadgePill(
    count: Int,
    badgeStyle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (count <= 0 || badgeStyle == "Off") return

    if (badgeStyle == "Dot") {
        Box(
            modifier = modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Color(0xFF0A0F18), CircleShape)
        )
    } else {
        val text = if (count > 99) "99+" else count.toString()
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B5C))
                .border(1.2.dp, Color(0xEEFFFFFF), CircleShape)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconTile(
    app: AppInfo,
    iconSizeDp: Int,
    shapeName: String,
    showLabel: Boolean,
    labelSizeSp: Int,
    labelColor: Color,
    palette: AdvancePalette,
    onWallpaper: Boolean,
    badgeCount: Int = 0,
    badgeStyle: String = "Off",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = iconShapeFor(shapeName)
    val isNeo = palette.style == SurfaceStyle.NEOMORPH
    val podRadius = if (shapeName == "Circle") (iconSizeDp / 2).dp else 16.dp

    val clickMod = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(clickMod)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = if (isNeo) {
                    Modifier
                        .size((iconSizeDp + 6).dp)
                        .advanceSurface(palette, cornerRadius = podRadius, elevationScale = 0.72f)
                        .padding(4.dp)
                } else {
                    Modifier
                        .size(iconSizeDp.dp)
                        .clip(shape)
                        .background(Color(0x22FFFFFF))
                },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
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
                        Icon(
                            imageVector = AdvanceIcons.Apps,
                            contentDescription = app.label,
                            tint = palette.accent,
                            modifier = Modifier.size((iconSizeDp * 0.52f).dp)
                        )
                    }
                }
            }

            NotificationBadgePill(
                count = badgeCount,
                badgeStyle = badgeStyle,
                accent = palette.accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = app.label,
                color = labelColor,
                fontSize = labelSizeSp.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = if (onWallpaper) {
                    TextStyle(
                        shadow = Shadow(
                            color = Color(0xBB000000),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 4f
                        )
                    )
                } else {
                    TextStyle.Default
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconTile(
    folder: HomeFolderItem,
    appsByPkg: Map<String, AppInfo>,
    iconSizeDp: Int,
    shapeName: String,
    showLabel: Boolean,
    labelSizeSp: Int,
    palette: AdvancePalette,
    badgeCount: Int = 0,
    badgeStyle: String = "Off",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val previewApps = folder.packages.mapNotNull { appsByPkg[it] }.take(4)
    val folderCorner = if (shapeName == "Circle") (iconSizeDp / 2).dp else 18.dp

    val clickMod = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(clickMod)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .advanceSurface(palette, cornerRadius = folderCorner, elevationScale = 0.8f)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewApps.isEmpty()) {
                    Icon(
                        imageVector = AdvanceIcons.Folder,
                        contentDescription = folder.name,
                        tint = palette.accent,
                        modifier = Modifier.size((iconSizeDp * 0.45f).dp)
                    )
                } else {
                    val miniSize = (iconSizeDp * 0.34f).dp
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            MiniFolderThumb(previewApps.getOrNull(0), miniSize)
                            MiniFolderThumb(previewApps.getOrNull(1), miniSize)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            MiniFolderThumb(previewApps.getOrNull(2), miniSize)
                            MiniFolderThumb(previewApps.getOrNull(3), miniSize)
                        }
                    }
                }
            }

            NotificationBadgePill(
                count = badgeCount,
                badgeStyle = badgeStyle,
                accent = palette.accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = folder.name,
                color = Color.White,
                fontSize = labelSizeSp.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xBB000000),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

@Composable
private fun MiniFolderThumb(app: AppInfo?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (app != null) Color(0x22FFFFFF) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (app?.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
