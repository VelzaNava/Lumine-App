package com.thesis.lumine.utils

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class MediaPipeHelper(context: Context) {

    private var faceLandmarker: FaceLandmarker? = null
    private var handLandmarker: HandLandmarker? = null

    private val applicationContext = context.applicationContext

    var onFaceDetected: ((FaceLandmarkerResult) -> Unit)? = null
    var onHandDetected: ((HandLandmarkerResult) -> Unit)? = null

    init {
        setupFaceLandmarker()
        setupHandLandmarker()
    }

    private fun setupFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    onFaceDetected?.invoke(result)
                }
                .setErrorListener { error ->
                    Log.e(MEDIAPIPE_TAG, "Face tracking failed", error)
                }
                .setNumFaces(1)
                // 0.5 rather than 0.3: lower values found "faces" in clothing and wallpaper
                // patterns, which kept the necklace and earrings on screen.
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(applicationContext, options)
        } catch (e: Exception) {
            Log.e(MEDIAPIPE_TAG, "Couldn't load the face tracking model", e)
        }
    }

    private fun setupHandLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    onHandDetected?.invoke(result)
                }
                .setErrorListener { error ->
                    Log.e(MEDIAPIPE_TAG, "Hand tracking failed", error)
                }
                // One hand only: try-on uses a single hand, and tracking two doubles the CPU cost.
                .setNumHands(1)
                // 0.5 for the same reason: at 0.3 the ring and bracelet stayed on screen after
                // the hand left the frame.
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(applicationContext, options)
        } catch (e: Exception) {
            Log.e(MEDIAPIPE_TAG, "Couldn't load the hand tracking model", e)
        }
    }

    fun detectFace(image: MPImage, timestampMs: Long) {
        faceLandmarker?.detectAsync(image, timestampMs)
    }

    fun detectHand(image: MPImage, timestampMs: Long) {
        handLandmarker?.detectAsync(image, timestampMs)
    }

    fun close() {
        faceLandmarker?.close()
        handLandmarker?.close()
    }
}

private const val MEDIAPIPE_TAG = "MediaPipeHelper"
