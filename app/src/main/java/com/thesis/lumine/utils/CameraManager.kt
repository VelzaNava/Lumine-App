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

    private var frameCount = 0
    private val FRAME_SKIP = 3  // Process every 4th frame (increased from 2)

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(android.util.Size(480, 360))  // Even lower resolution
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processFrame(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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

            // kailangan i-rotate at i-mirror muna yung bitmap bago i-feed sa MediaPipe
            //
            // yung camera sensor nag-ca-capture ng landscape kahit portrait yung phone.
            // pag hindi natin nai-rotate, makikita ng MediaPipe yung sideways na image
            // at mali yung landmark coordinates — nakapalit yung X at Y sa screen.
            //
            // rotationDegrees (usually 270° sa front cam, portrait) = kung gaano kalayo
            // ang raw frame sa tamang orientation.
            //
            // front camera mirror: ginagawa ng CameraX PreviewView ang selfie mirror automatically,
            // kaya kailangan nating gayahin din sa bitmap para nag-match ang coords sa screen.
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                // i-rotate sa portrait orientation
                postRotate(rotationDegrees.toFloat())
                // i-mirror horizontally para selfie view — after rotation, width = original height
                val rotatedWidth = if (rotationDegrees % 180 != 0) bitmap.height.toFloat()
                                   else bitmap.width.toFloat()
                postScale(-1f, 1f, rotatedWidth / 2f, 0f)
            }

            val correctedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            bitmap.recycle() // i-free yung original — correctedBitmap na ang gagamitin ng MediaPipe

            val mpImage = BitmapImageBuilder(correctedBitmap).build()
            val timestamp = System.currentTimeMillis()

            // huwag i-recycle yung correctedBitmap dito — hawak pa ng mpImage habang nag-po-process ang MediaPipe
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