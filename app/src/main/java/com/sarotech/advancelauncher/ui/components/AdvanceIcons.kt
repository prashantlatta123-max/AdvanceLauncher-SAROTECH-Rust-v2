package com.sarotech.advancelauncher.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AdvanceIcons {

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()

    val Apps: ImageVector by lazy {
        icon("Apps") {
            val fill = SolidColor(Color.Black)
            for (y in listOf(4f, 10f, 16f)) {
                for (x in listOf(4f, 10f, 16f)) {
                    path(fill = fill) {
                        moveTo(x, y); horizontalLineTo(x + 4f); verticalLineTo(y + 4f); horizontalLineTo(x); close()
                    }
                }
            }
        }
    }

    val Folder: ImageVector by lazy {
        icon("Folder") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 4f); horizontalLineTo(4f); verticalLineTo(20f); horizontalLineTo(20f)
                verticalLineTo(6f); horizontalLineTo(12f); lineTo(10f, 4f); close()
            }
        }
    }

    val Wallpaper: ImageVector by lazy {
        icon("Wallpaper") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 5f); horizontalLineTo(21f); verticalLineTo(19f); horizontalLineTo(3f); close()
                moveTo(5f, 17f); horizontalLineTo(19f); lineTo(14.5f, 11f); lineTo(11f, 15.5f); lineTo(8.5f, 12.5f); close()
            }
        }
    }

    val Widgets: ImageVector by lazy {
        icon("Widgets") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) {
                moveTo(3f, 3f); horizontalLineTo(11f); verticalLineTo(11f); horizontalLineTo(3f); close()
            }
            path(fill = fill) {
                moveTo(3f, 13f); horizontalLineTo(11f); verticalLineTo(21f); horizontalLineTo(3f); close()
            }
            path(fill = fill) {
                moveTo(13f, 13f); horizontalLineTo(21f); verticalLineTo(21f); horizontalLineTo(13f); close()
            }
            path(fill = fill) {
                moveTo(16.66f, 2f); lineTo(22f, 7.34f); lineTo(16.66f, 12.68f); lineTo(11.32f, 7.34f); close()
            }
        }
    }

    val Bell: ImageVector by lazy {
        icon("Bell") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) {
                moveTo(12f, 22f); lineTo(10f, 20f); horizontalLineTo(14f); close()
            }
            path(fill = fill) {
                moveTo(18f, 16f); verticalLineTo(11f); lineTo(16f, 6f); lineTo(12f, 4f)
                lineTo(8f, 6f); lineTo(6f, 11f); verticalLineTo(16f); lineTo(4f, 18f)
                verticalLineTo(19f); horizontalLineTo(20f); verticalLineTo(18f); close()
            }
        }
    }

    val Palette: ImageVector by lazy {
        icon("Palette") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f); lineTo(19f, 6f); lineTo(21f, 12f); lineTo(18f, 15f)
                horizontalLineTo(14.5f); lineTo(15.5f, 18.5f); lineTo(13f, 21f)
                lineTo(6f, 19f); lineTo(3f, 12f); lineTo(6f, 5f); close()
            }
        }
    }

    val Grid: ImageVector by lazy {
        icon("Grid") {
            val fill = SolidColor(Color.Black)
            for ((x, y) in listOf(3f to 3f, 13f to 3f, 3f to 13f, 13f to 13f)) {
                path(fill = fill) {
                    moveTo(x, y); horizontalLineTo(x + 8f); verticalLineTo(y + 8f); horizontalLineTo(x); close()
                }
            }
        }
    }

    val ListView: ImageVector by lazy {
        icon("ListView") {
            val fill = SolidColor(Color.Black)
            for (y in listOf(5f, 11f, 17f)) {
                path(fill = fill) { moveTo(3f, y); horizontalLineTo(6f); verticalLineTo(y + 2.5f); horizontalLineTo(3f); close() }
                path(fill = fill) { moveTo(8f, y); horizontalLineTo(21f); verticalLineTo(y + 2.5f); horizontalLineTo(8f); close() }
            }
        }
    }

    val Category: ImageVector by lazy {
        icon("Category") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) { moveTo(12f, 2f); lineTo(6.5f, 11f); horizontalLineTo(17.5f); close() }
            path(fill = fill) { moveTo(3f, 13.5f); horizontalLineTo(11f); verticalLineTo(21.5f); horizontalLineTo(3f); close() }
            path(fill = fill) { moveTo(13f, 13.5f); horizontalLineTo(21f); verticalLineTo(21.5f); horizontalLineTo(13f); close() }
        }
    }

    val Pin: ImageVector by lazy {
        icon("Pin") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 3f); horizontalLineTo(8f); verticalLineTo(5f); horizontalLineTo(9.5f)
                verticalLineTo(11f); lineTo(7f, 13.5f); verticalLineTo(15.5f); horizontalLineTo(11f)
                verticalLineTo(21f); horizontalLineTo(13f); verticalLineTo(15.5f); horizontalLineTo(17f)
                verticalLineTo(13.5f); lineTo(14.5f, 11f); verticalLineTo(5f); horizontalLineTo(16f); close()
            }
        }
    }

    val EyeOn: ImageVector by lazy {
        icon("EyeOn") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f); lineTo(22f, 12f); lineTo(12f, 19f); lineTo(2f, 12f); close()
                moveTo(12f, 9f); lineTo(9f, 12f); lineTo(12f, 15f); lineTo(15f, 12f); close()
            }
        }
    }

    val EyeOff: ImageVector by lazy {
        icon("EyeOff") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) { moveTo(3f, 4.5f); lineTo(4.5f, 3f); lineTo(21f, 19.5f); lineTo(19.5f, 21f); close() }
            path(fill = fill) { moveTo(12f, 6f); lineTo(21f, 12f); lineTo(16.5f, 15f); lineTo(9f, 7.5f); close() }
            path(fill = fill) { moveTo(7.5f, 9f); lineTo(15f, 16.5f); lineTo(12f, 18f); lineTo(3f, 12f); close() }
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) { moveTo(8f, 3f); horizontalLineTo(20f); verticalLineTo(17f); horizontalLineTo(8f); close() }
            path(fill = fill) { moveTo(4f, 7f); horizontalLineTo(6f); verticalLineTo(19f); horizontalLineTo(16f); verticalLineTo(21f); horizontalLineTo(4f); close() }
        }
    }

    val Calc: ImageVector by lazy {
        icon("Calc") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) { moveTo(4f, 6f); horizontalLineTo(10f); verticalLineTo(8f); horizontalLineTo(4f); close() }
            path(fill = fill) { moveTo(6f, 4f); horizontalLineTo(8f); verticalLineTo(10f); horizontalLineTo(6f); close() }
            path(fill = fill) { moveTo(14f, 6f); horizontalLineTo(20f); verticalLineTo(8f); horizontalLineTo(14f); close() }
            path(fill = fill) { moveTo(14f, 14f); horizontalLineTo(20f); verticalLineTo(16f); horizontalLineTo(14f); close() }
            path(fill = fill) { moveTo(14f, 18f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(14f); close() }
            path(fill = fill) { moveTo(4f, 16f); horizontalLineTo(10f); verticalLineTo(18f); horizontalLineTo(4f); close() }
        }
    }

    val Backup: ImageVector by lazy {
        icon("Backup") {
            val fill = SolidColor(Color.Black)
            path(fill = fill) {
                moveTo(11f, 4f); horizontalLineTo(13f); verticalLineTo(14f); horizontalLineTo(16.5f)
                lineTo(12f, 18.5f); lineTo(7.5f, 14f); horizontalLineTo(11f); close()
            }
            path(fill = fill) { moveTo(4f, 19f); horizontalLineTo(20f); verticalLineTo(21f); horizontalLineTo(4f); close() }
        }
    }
}
