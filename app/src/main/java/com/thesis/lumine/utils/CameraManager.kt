package com.thesis.lumine.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
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

    // Switch between the front and back camera, then rebind.
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
            .analysisSize(android.util.Size(480, 360))
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
            Log.e(CAMERA_TAG, "Couldn't start the camera", e)
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

            // Rotate (and for the front camera, mirror) each frame before handing it to MediaPipe.
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                // Front camera only: mirror to match CameraX's selfie preview.
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
            Log.e(CAMERA_TAG, "Couldn't convert a camera frame", e)
        } finally {
            imageProxy.close()
        }
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}

private const val CAMERA_TAG = "CameraManager"

// The AR placement was tuned against frames picked by this (deprecated) call; the
// newer ResolutionSelector chooses sizes differently, which would shift the jewelry.
@Suppress("DEPRECATION")
private fun ImageAnalysis.Builder.analysisSize(size: android.util.Size): ImageAnalysis.Builder =
    setTargetResolution(size)
