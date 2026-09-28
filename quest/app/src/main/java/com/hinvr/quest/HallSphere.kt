package com.hinvr.quest

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.Surface

object HallSphere {
    private var appContext: Context? = null

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    fun drawOn(surface: Surface) {
        val context = appContext ?: return
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sanctum_panorama)
            ?: return
        runCatching {
            val canvas = surface.lockCanvas(null)
            try {
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(0, 0, canvas.width, canvas.height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
                )
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        }
        bitmap.recycle()
    }
}
