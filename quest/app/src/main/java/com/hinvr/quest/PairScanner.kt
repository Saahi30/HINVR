package com.hinvr.quest

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executor

/**
 * Reads the phone's pairing QR through one of the Quest's passthrough cameras.
 * Needs Horizon OS v74+ and the headset camera permission.
 */
object PairScanner {
    const val PermissionRequest = 4107

    private val permissions = arrayOf(Manifest.permission.CAMERA, HeadsetCamera)
    private val main = Handler(Looper.getMainLooper())
    private var activity: Activity? = null
    private var resumed = false
    private var asked = false
    private var generation = 0
    private var thread: HandlerThread? = null
    private var reader: ImageReader? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var open = false
    private var nextDecodeAt = 0L
    private var luma = ByteArray(0)
    private val qr = QRCodeReader()
    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun onResume() {
        resumed = true
    }

    fun onPause() {
        resumed = false
        close()
    }

    fun detach() {
        close()
        activity = null
    }

    /** Ask again after the member turned the camera down. */
    fun askAgain() {
        asked = false
    }

    fun onPermissionResult() {
        QuestAccount.cameraBlocked(!granted())
    }

    /** Called every frame. Opens the camera only while pairing is on screen. */
    fun sync(wanted: Boolean) {
        val host = activity ?: return
        if (!wanted || !resumed) {
            if (open) close()
            return
        }
        if (!granted()) {
            if (!asked) {
                asked = true
                host.requestPermissions(permissions, PermissionRequest)
            }
            QuestAccount.cameraBlocked(true)
            return
        }
        QuestAccount.cameraBlocked(false)
        if (!open) start(host)
    }

    private fun granted(): Boolean {
        val host = activity ?: return false
        return permissions.all { host.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun start(host: Activity) {
        val manager = host.getSystemService(CameraManager::class.java) ?: return
        val id = runCatching { pickCamera(manager) }.getOrNull() ?: return
        val size = runCatching {
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.YUV_420_888)
                ?.filter { it.width * it.height <= 1920 * 1440 }
                ?.maxByOrNull { it.width * it.height }
        }.getOrNull() ?: return
        open = true
        val token = ++generation
        val worker = HandlerThread("hinvr-pair-scan").also { it.start() }
        val handler = Handler(worker.looper)
        val executor = Executor { handler.post(it) }
        val images = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
        images.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            image.use {
                val now = SystemClock.elapsedRealtime()
                if (now < nextDecodeAt || token != generation) return@use
                nextDecodeAt = now + 120L
                val text = decode(it) ?: return@use
                main.post { if (token == generation) QuestAccount.onScanned(text) }
            }
        }, handler)
        thread = worker
        reader = images
        try {
            manager.openCamera(id, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    if (token != generation) {
                        device.close()
                        return
                    }
                    camera = device
                    val outputs = listOf(OutputConfiguration(images.surface))
                    val config = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        executor,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(capture: CameraCaptureSession) {
                                if (token != generation) {
                                    capture.close()
                                    return
                                }
                                session = capture
                                runCatching {
                                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    request.addTarget(images.surface)
                                    capture.setRepeatingRequest(request.build(), null, handler)
                                }
                            }

                            override fun onConfigureFailed(capture: CameraCaptureSession) {
                                capture.close()
                            }
                        },
                    )
                    runCatching { device.createCaptureSession(config) }
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    main.post { if (token == generation) close() }
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    main.post { if (token == generation) close() }
                }
            })
        } catch (_: SecurityException) {
            close()
        } catch (_: Exception) {
            close()
        }
    }

    private fun close() {
        generation++
        open = false
        runCatching { session?.close() }
        runCatching { camera?.close() }
        runCatching { reader?.close() }
        thread?.quitSafely()
        session = null
        camera = null
        reader = null
        thread = null
    }

    /** Prefers a passthrough camera; Meta tags them with vendor metadata. */
    private fun pickCamera(manager: CameraManager): String? {
        val ids = manager.cameraIdList.toList()
        val source = CameraCharacteristics.Key(MetaCameraSource, Int::class.java)
        return ids.firstOrNull { id ->
            runCatching { manager.getCameraCharacteristics(id).get(source) == 0 }.getOrDefault(false)
        } ?: ids.firstOrNull()
    }

    private fun decode(image: android.media.Image): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val length = buffer.remaining()
        if (luma.size != length) luma = ByteArray(length)
        buffer.get(luma)
        val rowStride = plane.rowStride
        val rows = (length + rowStride - 1) / rowStride
        if (rowStride < image.width || rows < image.height) return null
        val source = PlanarYUVLuminanceSource(
            luma,
            rowStride,
            rows,
            0,
            0,
            image.width,
            image.height,
            false,
        )
        return try {
            qr.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: ReaderException) {
            null
        } catch (_: RuntimeException) {
            null
        } finally {
            qr.reset()
        }
    }

    private const val HeadsetCamera = "horizonos.permission.HEADSET_CAMERA"
    private const val MetaCameraSource = "com.meta.extra_metadata.camera_source"
}
