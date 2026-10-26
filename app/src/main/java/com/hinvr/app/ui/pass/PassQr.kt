package com.hinvr.app.ui.pass

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.hinvr.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Near-black on white: the contrast desk scanners are tuned for. The ivory card frames it. */
private const val QrDark = 0xFF14100C.toInt()
private const val QrLight = 0xFFFFFFFF.toInt()

/** The standard four-module quiet zone. */
private const val QuietZoneModules = 4

/** Built once per payload off the main thread, at one pixel per module, then scaled without smoothing. */
@Composable
fun PassQr(payload: String, modifier: Modifier = Modifier) {
    val bitmap by produceState(initialValue = cachedQr(payload), payload) {
        value = cachedQr(payload) ?: withContext(Dispatchers.Default) { qrFor(payload) }
    }
    val ready = bitmap
    if (ready == null) {
        Box(modifier)
        return
    }
    Image(
        bitmap = ready,
        contentDescription = stringResource(R.string.qr_cd),
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

// The card and the enlarged view show the same code; draw it once.
@Volatile
private var lastQr: Pair<String, ImageBitmap>? = null

private fun cachedQr(payload: String): ImageBitmap? = lastQr?.takeIf { it.first == payload }?.second

private fun qrFor(payload: String): ImageBitmap =
    cachedQr(payload) ?: createQrBitmap(payload).asImageBitmap().also { lastQr = payload to it }

private fun createQrBitmap(payload: String): Bitmap {
    // Size 0 asks ZXing for the bare module grid; the quiet zone is added here.
    val matrix = QRCodeWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.MARGIN to QuietZoneModules,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        ),
    )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            pixels[row + x] = if (matrix[x, y]) QrDark else QrLight
        }
    }
    return createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
