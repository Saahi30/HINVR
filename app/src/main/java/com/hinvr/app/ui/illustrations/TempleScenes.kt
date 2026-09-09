package com.hinvr.app.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.hinvr.app.ui.catalog.TileScene

@Composable
fun TempleScene(scene: TileScene, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        when (scene) {
            TileScene.LiveAarti -> liveAarti()
            TileScene.VrHall -> vrHall()
            TileScene.PassDesk -> passDesk()
            TileScene.PanditDoor -> panditDoor()
            TileScene.ConciergeDesk -> conciergeDesk()
            TileScene.YatraRoad -> yatraRoad()
            TileScene.Tirupati -> tirupati()
            TileScene.Kashi -> kashi()
            TileScene.Shirdi -> shirdi()
            TileScene.Kedarnath -> kedarnath()
            TileScene.Somnath -> somnath()
        }
    }
}

fun TileScene.darkType(): Boolean = when (this) {
    TileScene.LiveAarti, TileScene.VrHall, TileScene.YatraRoad,
    TileScene.Kashi, TileScene.Kedarnath, TileScene.Somnath,
    -> true
    else -> false
}

private fun DrawScope.sky(top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun DrawScope.gopuram(cx: Float, base: Float, w: Float, h: Float, fill: Color, trim: Color) {
    val path = Path().apply {
        moveTo(cx, base - h)
        lineTo(cx + w * 0.12f, base - h * 0.82f)
        lineTo(cx + w * 0.18f, base - h * 0.62f)
        lineTo(cx + w * 0.32f, base - h * 0.38f)
        lineTo(cx + w * 0.46f, base)
        lineTo(cx - w * 0.46f, base)
        lineTo(cx - w * 0.32f, base - h * 0.38f)
        lineTo(cx - w * 0.18f, base - h * 0.62f)
        lineTo(cx - w * 0.12f, base - h * 0.82f)
        close()
    }
    drawPath(path, fill)
    drawCircle(trim, w * 0.035f, Offset(cx, base - h * 0.9f))
}

private fun DrawScope.diya(at: Offset, r: Float) {
    drawCircle(Color(0xFFFFF1C1), r * 0.55f, at)
    drawCircle(Color(0xFFE8A317).copy(alpha = 0.55f), r, at)
    drawOval(
        Color(0xFFC9A227),
        topLeft = Offset(at.x - r * 0.7f, at.y + r * 0.15f),
        size = Size(r * 1.4f, r * 0.55f),
    )
}

private fun DrawScope.liveAarti() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF1B1020), Color(0xFF4A2414))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF2A1A12), Color(0xFF1A120C))),
        topLeft = Offset(0f, h * 0.62f),
        size = Size(w, h * 0.38f),
    )
    gopuram(w * 0.52f, h * 0.68f, w * 0.72f, h * 0.52f, Color(0xFF5A341C), Color(0xFFC9A227))
    drawRect(Color(0xFF3A2418), topLeft = Offset(w * 0.18f, h * 0.66f), size = Size(w * 0.64f, h * 0.12f))
    for (i in 0..6) {
        diya(Offset(w * (0.18f + i * 0.11f), h * 0.78f), w * 0.035f)
    }
    drawCircle(Color(0xFFE8A317).copy(alpha = 0.18f), w * 0.42f, Offset(w * 0.5f, h * 0.72f))
}

private fun DrawScope.vrHall() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF14202E), Color(0xFF3D4A5C))
    drawCircle(Color(0xFF9BB4C9).copy(alpha = 0.25f), w * 0.42f, Offset(w * 0.5f, h * 0.42f))
    drawCircle(Color.White.copy(alpha = 0.18f), w * 0.28f, Offset(w * 0.5f, h * 0.42f), style = Stroke(w * 0.012f))
    drawCircle(Color.White.copy(alpha = 0.12f), w * 0.18f, Offset(w * 0.5f, h * 0.42f), style = Stroke(w * 0.008f))
    gopuram(w * 0.5f, h * 0.78f, w * 0.5f, h * 0.42f, Color(0xFFC9B7A0), Color(0xFF8A7018))
    drawRect(Color(0xFF2C2118).copy(alpha = 0.35f), topLeft = Offset(0f, h * 0.78f), size = Size(w, h * 0.22f))
}

private fun DrawScope.passDesk() {
    val w = size.width
    val h = size.height
    sky(Color(0xFFF3E4D0), Color(0xFFE8D3B5))
    drawRoundRect(
        Color(0xFF2C2118),
        topLeft = Offset(w * 0.18f, h * 0.22f),
        size = Size(w * 0.64f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.06f),
    )
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFFC9A227), Color(0xFF8A7018))),
        topLeft = Offset(w * 0.22f, h * 0.26f),
        size = Size(w * 0.56f, h * 0.44f),
        cornerRadius = CornerRadius(w * 0.045f),
    )
    drawRoundRect(
        Color(0xFFF3E6D0),
        topLeft = Offset(w * 0.32f, h * 0.48f),
        size = Size(w * 0.36f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.02f),
    )
    drawCircle(Color(0xFF1A120C).copy(alpha = 0.35f), w * 0.04f, Offset(w * 0.5f, h * 0.38f))
}

private fun DrawScope.panditDoor() {
    val w = size.width
    val h = size.height
    sky(Color(0xFFEEDCC4), Color(0xFFD9B48A))
    drawRect(Color(0xFF8C5A32), topLeft = Offset(w * 0.12f, h * 0.18f), size = Size(w * 0.76f, h * 0.7f))
    drawRect(Color(0xFF5C3418), topLeft = Offset(w * 0.28f, h * 0.32f), size = Size(w * 0.44f, h * 0.56f))
    drawArc(
        Color(0xFFC45C26).copy(alpha = 0.85f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(w * 0.28f, h * 0.22f),
        size = Size(w * 0.44f, h * 0.22f),
    )
    drawCircle(Color(0xFF2C2118), w * 0.09f, Offset(w * 0.5f, h * 0.58f))
    drawRect(Color(0xFF3D2E22), topLeft = Offset(w * 0.42f, h * 0.64f), size = Size(w * 0.16f, h * 0.24f))
    drawCircle(Color(0xFFC9A227), w * 0.025f, Offset(w * 0.5f, h * 0.28f))
}

private fun DrawScope.conciergeDesk() {
    val w = size.width
    val h = size.height
    sky(Color(0xFFF6EFE4), Color(0xFFE7D5C0))
    drawRoundRect(
        Color(0xFF3D2E22),
        topLeft = Offset(w * 0.08f, h * 0.58f),
        size = Size(w * 0.84f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    drawRoundRect(
        Color(0xFFFFF8F0),
        topLeft = Offset(w * 0.18f, h * 0.36f),
        size = Size(w * 0.38f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.03f),
    )
    drawCircle(Color(0xFFE8A317).copy(alpha = 0.5f), w * 0.12f, Offset(w * 0.72f, h * 0.4f))
    drawRect(Color(0xFFC9A227), topLeft = Offset(w * 0.7f, h * 0.48f), size = Size(w * 0.04f, h * 0.14f))
    drawCircle(Color(0xFF1A120C), w * 0.07f, Offset(w * 0.32f, h * 0.28f))
}

private fun DrawScope.yatraRoad() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF6A8CA8), Color(0xFFF2D3B0))
    val peaks = Path().apply {
        moveTo(0f, h * 0.52f)
        lineTo(w * 0.22f, h * 0.22f)
        lineTo(w * 0.4f, h * 0.42f)
        lineTo(w * 0.62f, h * 0.12f)
        lineTo(w * 0.82f, h * 0.4f)
        lineTo(w, h * 0.28f)
        lineTo(w, h * 0.62f)
        lineTo(0f, h * 0.62f)
        close()
    }
    drawPath(peaks, Color(0xFF5C6B78))
    drawPath(peaks, Color.White.copy(alpha = 0.35f))
    drawRect(Color(0xFFC4A07A), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.4f))
    gopuram(w * 0.72f, h * 0.7f, w * 0.28f, h * 0.22f, Color(0xFFD9C4A0), Color(0xFFC9A227))
    val road = Path().apply {
        moveTo(w * 0.42f, h)
        lineTo(w * 0.58f, h)
        lineTo(w * 0.52f, h * 0.62f)
        lineTo(w * 0.48f, h * 0.62f)
        close()
    }
    drawPath(road, Color(0xFF8A6A4A))
}

private fun DrawScope.tirupati() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF1E3A5F), Color(0xFFF0C48A))
    gopuram(w * 0.5f, h * 0.86f, w * 0.9f, h * 0.78f, Color(0xFFC45C26), Color(0xFFC9A227))
    drawRect(Color(0xFF9A3F16), topLeft = Offset(w * 0.08f, h * 0.82f), size = Size(w * 0.84f, h * 0.18f))
}

private fun DrawScope.kashi() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF2A1C3A), Color(0xFFC45C26))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A4A6A), Color(0xFF0E2A3A))), topLeft = Offset(0f, h * 0.62f), size = Size(w, h * 0.38f))
    for (i in 0..8) {
        val stepY = h * (0.48f + i * 0.03f)
        drawRect(Color(0xFF8A5A3A), topLeft = Offset(w * (0.05f + i * 0.02f), stepY), size = Size(w * 0.7f, h * 0.018f))
    }
    gopuram(w * 0.72f, h * 0.58f, w * 0.38f, h * 0.4f, Color(0xFFD9C4A0), Color(0xFFE8A317))
    for (i in 0..5) diya(Offset(w * (0.12f + i * 0.1f), h * 0.7f), w * 0.028f)
}

private fun DrawScope.shirdi() {
    val w = size.width
    val h = size.height
    sky(Color(0xFFB8D4C8), Color(0xFFF4EDE3))
    drawRoundRect(
        Color(0xFFF7F1E6),
        topLeft = Offset(w * 0.12f, h * 0.28f),
        size = Size(w * 0.76f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    drawArc(
        Color(0xFFC9A227),
        180f,
        180f,
        true,
        topLeft = Offset(w * 0.22f, h * 0.16f),
        size = Size(w * 0.56f, h * 0.28f),
    )
    drawRect(Color(0xFFC45C26), topLeft = Offset(w * 0.42f, h * 0.48f), size = Size(w * 0.16f, h * 0.32f))
    drawCircle(Color(0xFFE8A317), w * 0.04f, Offset(w * 0.5f, h * 0.4f))
}

private fun DrawScope.kedarnath() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF4A6580), Color(0xFFE8EEF4))
    val snow = Path().apply {
        moveTo(0f, h * 0.48f)
        lineTo(w * 0.3f, h * 0.16f)
        lineTo(w * 0.5f, h * 0.38f)
        lineTo(w * 0.78f, h * 0.08f)
        lineTo(w, h * 0.4f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(snow, Color(0xFF8EA4B8))
    drawPath(snow, Color.White.copy(alpha = 0.45f))
    drawRect(Color(0xFF5A4638), topLeft = Offset(w * 0.28f, h * 0.52f), size = Size(w * 0.44f, h * 0.38f))
    gopuram(w * 0.5f, h * 0.58f, w * 0.42f, h * 0.28f, Color(0xFFD9C7A8), Color(0xFFF3E6D0))
}

private fun DrawScope.somnath() {
    val w = size.width
    val h = size.height
    sky(Color(0xFF1B3A4A), Color(0xFFF2B07A))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A6A8A), Color(0xFF0A3040))), topLeft = Offset(0f, h * 0.58f), size = Size(w, h * 0.42f))
    gopuram(w * 0.5f, h * 0.7f, w * 0.7f, h * 0.55f, Color(0xFFE8D4B0), Color(0xFFC9A227))
    drawCircle(Color(0xFFFFF1C1).copy(alpha = 0.5f), w * 0.08f, Offset(w * 0.82f, h * 0.22f))
}
