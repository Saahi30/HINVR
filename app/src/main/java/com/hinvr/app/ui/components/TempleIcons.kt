package com.hinvr.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Line icons drawn for HINVR. The Mandirs tab is a nagara shikhara on its plinth,
 * with kalash and flag: a temple, where Material only offers a bank.
 */
object TempleIcons {

    val Shikhara: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shikhara",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Plinth and ground line.
                moveTo(3f, 21f)
                horizontalLineTo(21f)
                moveTo(5f, 21f)
                verticalLineTo(18f)
                horizontalLineTo(19f)
                verticalLineTo(21f)
                // The curved tower rising to the amalaka.
                moveTo(7f, 18f)
                curveTo(7f, 12.5f, 9.4f, 8.4f, 12f, 5.4f)
                curveTo(14.6f, 8.4f, 17f, 12.5f, 17f, 18f)
                moveTo(9.6f, 10f)
                horizontalLineTo(14.4f)
                // Doorway.
                moveTo(10.6f, 18f)
                verticalLineTo(15.4f)
                curveTo(10.6f, 14.6f, 11.2f, 14f, 12f, 14f)
                curveTo(12.8f, 14f, 13.4f, 14.6f, 13.4f, 15.4f)
                verticalLineTo(18f)
                // Kalash and flag.
                moveTo(12f, 5.4f)
                verticalLineTo(2.2f)
                lineTo(14.6f, 3f)
                lineTo(12f, 3.8f)
            }
        }.build()
    }

    val ShikharaFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShikharaFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Tower with the doorway cut through it.
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(6.4f, 18f)
                curveTo(6.4f, 12.3f, 8.9f, 8f, 12f, 4.6f)
                curveTo(15.1f, 8f, 17.6f, 12.3f, 17.6f, 18f)
                close()
                moveTo(10.6f, 18f)
                verticalLineTo(15.4f)
                curveTo(10.6f, 14.6f, 11.2f, 14f, 12f, 14f)
                curveTo(12.8f, 14f, 13.4f, 14.6f, 13.4f, 15.4f)
                verticalLineTo(18f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 21f)
                horizontalLineTo(21f)
                moveTo(5f, 21f)
                verticalLineTo(18.6f)
                horizontalLineTo(19f)
                verticalLineTo(21f)
                moveTo(12f, 4.8f)
                verticalLineTo(2.2f)
                lineTo(14.6f, 3f)
                lineTo(12f, 3.8f)
            }
        }.build()
    }
}
