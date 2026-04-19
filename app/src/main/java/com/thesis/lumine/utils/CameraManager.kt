package com.thesis.lumine.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner
) {
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var onFrameAvailable: ((MPImage, Long) -> Unit)? = null
    var onCameraFacingChanged: ((Boolean) -> Unit)? = null  // notifies UI when camera flips

    private var frameCount = 0
    private val FRAME_SKIP = 3

    var useFrontCamera: Boolean = true
        private set

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(context))
    }

    // i-toggle ang front/back camera tapos i-rebind
    fun switchCamera() {
        useFrontCamera = !useFrontCamera
        onCameraFacingChanged?.invoke(useFrontCamera)
        cameraProvider?.let { bindCamera() }
    }

    private fun bindCamera() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(android.util.Size(480, 360))
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy -> processFrame(imageProxy) }
            }

        val cameraSelector = if (useFrontCamera)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // preskip ng frames para mabawasan ang load sa CPU/GPU
            frameCount++
            if (frameCount % (FRAME_SKIP + 1) != 0) {
                imageProxy.close()
                return
            }

            val bitmap = imageProxy.toBitmap()

            // kailangan i-rotate at optionally i-mirror ang bitmap bago i-feed sa MediaPipe
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                // front camera only: mirror para mag-match sa selfie preview ng CameraX
                if (useFrontCamera) {
                    val rotatedWidth = if (rotationDegrees % 180 != 0) bitmap.height.toFloat()
                                       else bitmap.width.toFloat()
                    postScale(-1f, 1f, rotatedWidth / 2f, 0f)
                }
            }

            val correctedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            bitmap.recycle()

            val mpImage   = BitmapImageBuilder(correctedBitmap).build()
            val timestamp = System.currentTimeMillis()

            onFrameAvailable?.invoke(mpImage, timestamp)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
