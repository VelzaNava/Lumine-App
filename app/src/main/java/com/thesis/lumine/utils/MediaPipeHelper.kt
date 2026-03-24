package com.thesis.lumine.utils

import android.content.Context
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
                    error.printStackTrace()
                }
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(applicationContext, options)
        } catch (e: Exception) {
            e.printStackTrace()
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
                    error.printStackTrace()
                }
                // 1 hand lang kasi single-hand ang ring try-on — 2 hands = doble ang CPU cost
                .setNumHands(1)
                // mas mataas = mas kaunting false detections = hindi masyadong mag-jitter yung ring
                .setMinHandDetectionConfidence(0.7f)
                // mag-update lang ng landmarks pag clearly present yung kamay — para hindi matog-tog
                .setMinHandPresenceConfidence(0.7f)
                // hawakan ng MediaPipe yung track nang mas matagal bago sabihing "nawala" — smoother ring
                .setMinTrackingConfidence(0.6f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(applicationContext, options)
        } catch (e: Exception) {
            e.printStackTrace()
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