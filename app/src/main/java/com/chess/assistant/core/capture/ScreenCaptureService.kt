package com.chess.assistant.core.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 屏幕截取服务
 * 使用 MediaProjection API 实现屏幕捕获
 * 参考 Android 官方文档和最佳实践
 */
class ScreenCaptureService(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val displayMetrics: DisplayMetrics by lazy {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics
    }

    private val screenWidth: Int get() = displayMetrics.widthPixels
    private val screenHeight: Int get() = displayMetrics.heightPixels

    /**
     * 设置 MediaProjection
     */
    fun setMediaProjection(projection: MediaProjection?) {
        cleanup()
        mediaProjection = projection
    }

    /**
     * 开始屏幕捕获
     */
    suspend fun startCapture(): Boolean = withContext(Dispatchers.Main) {
        try {
            cleanup()

            // 创建 ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            // 创建 VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            isCapturing = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 捕获单个帧
     */
    suspend fun captureFrame(): Bitmap? = withContext(Dispatchers.IO) {
        if (!isCapturing || imageReader == null) {
            return@withContext null
        }

        try {
            val image = suspendCancellableCoroutine { continuation ->
                try {
                    val listener = ImageReader.OnImageAvailableListener { reader ->
                        try {
                            val img = reader.acquireLatestImage()
                            if (img != null) {
                                continuation.resume(img)
                            } else {
                                continuation.resumeWithException(Exception("Failed to acquire image"))
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                    imageReader?.setOnImageAvailableListener(listener, null)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            val bitmap = imageToBitmap(image)
            image.close()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 捕获指定区域
     */
    suspend fun captureRegion(region: Rect): Bitmap? = withContext(Dispatchers.IO) {
        val fullBitmap = captureFrame() ?: return@withContext null

        try {
            // 裁剪指定区域
            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap,
                region.left,
                region.top,
                region.width(),
                region.height()
            )
            fullBitmap.recycle()
            croppedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            fullBitmap
        }
    }

    /**
     * 转换 Image 为 Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )

        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        // 裁剪到实际图像大小
        if (rowPadding > 0) {
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                image.width,
                image.height
            )
            bitmap.recycle()
            return croppedBitmap
        }

        return bitmap
    }

    /**
     * 停止捕获
     */
    fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        isCapturing = false
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            stopCapture()
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 检查是否正在捕获
     */
    fun isCapturing(): Boolean = isCapturing

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(): Pair<Int, Int> {
        return Pair(screenWidth, screenHeight)
    }
}
