package com.hinvr.app.ui.pass

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun PassQr(payload: String, modifier: Modifier = Modifier) {
    val bitmap = remember(payload) { createQrBitmap(payload) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Membership QR credential",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private fun createQrBitmap(payload: String, size: Int = 640): Bitmap {
    val matrix = MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        ),
    )
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) 0xFF1A120C.toInt() else 0xFFFFF8F0.toInt())
            }
        }
    }
}
