package com.sarotech.advancelauncher.ui.theme

import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders either:
 * - GLASS: Frosted translucent layered gradient with diagonal specular sheen & luminous rim border.
 * - NEOMORPH: Tactile 3D extruded surface with dual top-left light shadow + bottom-right dark shadow.
 */
fun Modifier.advanceSurface(
    palette: AdvancePalette,
    cornerRadius: Dp = 22.dp,
    elevationScale: Float = 1f,
    transparent: Boolean = false
): Modifier {
    if (transparent) return this
    val shape = RoundedCornerShape(cornerRadius)

    return when (palette.style) {
        SurfaceStyle.GLASS -> {
            val frostBrush = Brush.linearGradient(
                colors = listOf(
                    palette.glassHighlight,
                    palette.surface,
                    palette.surfaceElevated
                )
            )
            val rimBrush = Brush.linearGradient(
                colors = listOf(
                    palette.glassBorderStart,
                    palette.border.copy(alpha = 0.25f),
                    palette.glassBorderEnd
                )
            )
            this
                .clip(shape)
                .background(brush = frostBrush)
                .border(width = 1.15.dp, brush = rimBrush, shape = shape)
        }

        SurfaceStyle.NEOMORPH -> {
            val depth = palette.neoDepth * elevationScale
            val offsetDp = (5.5f * depth).dp
            val blurDp = (10f * depth).dp

            val convexBrush = Brush.linearGradient(
                colors = if (palette.isLight) {
                    listOf(Color(0xFFF0F5FC), palette.neoBase, Color(0xFFD8E1EE))
                } else {
                    listOf(Color(0xFF282F3B), palette.neoBase, Color(0xFF191D25))
                }
            )
            val bevelBrush = Brush.linearGradient(
                colors = listOf(palette.glassBorderStart, Color.Transparent, palette.glassBorderEnd)
            )

            this
                .drawBehind {
                    val r = cornerRadius.toPx()
                    val off = offsetDp.toPx()
                    val blur = blurDp.toPx()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        drawIntoCanvas { canvas ->
                            val darkPaint = Paint().apply {
                                isAntiAlias = true
                                color = palette.neoBase.toArgb()
                                setShadowLayer(blur, off, off, palette.neoDarkShadow.toArgb())
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height, r, r, darkPaint
                            )

                            val lightPaint = Paint().apply {
                                isAntiAlias = true
                                color = palette.neoBase.toArgb()
                                setShadowLayer(blur, -off, -off, palette.neoLightShadow.toArgb())
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height, r, r, lightPaint
                            )
                        }
                    } else {
                        // Soft multi-pass fallback for Android 8.0–8.1
                        for (step in 3 downTo 1) {
                            val d = off * (step / 3f)
                            val a = 0.22f / step
                            drawRoundRect(
                                color = palette.neoDarkShadow.copy(alpha = a),
                                topLeft = Offset(d, d),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(r, r)
                            )
                            drawRoundRect(
                                color = palette.neoLightShadow.copy(alpha = a),
                                topLeft = Offset(-d, -d),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(r, r)
                            )
                        }
                    }
                }
                .clip(shape)
                .background(brush = convexBrush)
                .border(width = 1.dp, brush = bevelBrush, shape = shape)
        }
    }
}

/**
 * Recessed / pressed-in surface for search wells, active indicators, and inner containers.
 */
fun Modifier.advanceRecessed(
    palette: AdvancePalette,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return when (palette.style) {
        SurfaceStyle.GLASS -> this
            .clip(shape)
            .background(palette.surface.copy(alpha = (palette.glassOpacity + 0.14f).coerceAtMost(0.95f)))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(palette.glassBorderEnd, palette.glassBorderStart.copy(alpha = 0.45f))
                ),
                shape = shape
            )

        SurfaceStyle.NEOMORPH -> {
            val concaveBrush = Brush.linearGradient(
                colors = listOf(
                    palette.neoDarkShadow.copy(alpha = if (palette.isLight) 0.42f else 0.72f),
                    palette.neoRecessed,
                    palette.neoLightShadow.copy(alpha = if (palette.isLight) 0.55f else 0.35f)
                )
            )
            val innerRim = Brush.linearGradient(
                colors = listOf(palette.neoDarkShadow, Color.Transparent, palette.neoLightShadow)
            )
            this
                .clip(shape)
                .background(brush = concaveBrush)
                .border(width = 1.2.dp, brush = innerRim, shape = shape)
        }
    }
}
