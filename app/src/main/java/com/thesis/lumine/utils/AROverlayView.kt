package com.thesis.lumine.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

class AROverlayView(context: Context) : View(context) {

    private val necklacePaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val ringPaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        strokeWidth = 6f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var faceLandmarks: FaceLandmarkerResult? = null
    private var handLandmarks: HandLandmarkerResult? = null
    private var jewelryType: String = ""

    // mas mataas na smoothing para sa mid-range phones — less jitter
    private var smoothedX = 0f
    private var smoothedY = 0f
    private val SMOOTHING_FACTOR = 0.25f  // More smoothing (was 0.4f)

    fun setJewelryType(type: String) {
        jewelryType = type.lowercase()
    }

    fun updateFaceLandmarks(result: FaceLandmarkerResult) {
        faceLandmarks = result
        postInvalidate()
    }

    fun updateHandLandmarks(result: HandLandmarkerResult) {
        handLandmarks = result
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (jewelryType) {
            "necklace" -> drawNecklace(canvas)
            "earring" -> drawEarrings(canvas)
            "ring" -> drawRing(canvas)
            "bracelet" -> drawBracelet(canvas)
        }
    }

    private fun drawNecklace(canvas: Canvas) {
        faceLandmarks?.let { result ->
            if (result.faceLandmarks().isNotEmpty()) {
                val landmarks = result.faceLandmarks()[0]

                val chinBottom = landmarks[152]
                val leftJaw = landmarks[234]
                val rightJaw = landmarks[454]

                val x1 = leftJaw.x() * width
                val y1 = leftJaw.y() * height
                val x2 = chinBottom.x() * width
                val y2 = (chinBottom.y() * height) + 30
                val x3 = rightJaw.x() * width
                val y3 = rightJaw.y() * height

                canvas.drawLine(x1, y1, x2, y2, necklacePaint)
                canvas.drawLine(x2, y2, x3, y3, necklacePaint)
                canvas.drawCircle(x2, y2 + 20, 12f, ringPaint)
            }
        }
    }

    private fun drawEarrings(canvas: Canvas) {
        faceLandmarks?.let { result ->
            if (result.faceLandmarks().isNotEmpty()) {
                val landmarks = result.faceLandmarks()[0]

                val leftEar = landmarks[234]
                val rightEar = landmarks[454]

                val leftX = leftEar.x() * width
                val leftY = leftEar.y() * height
                val rightX = rightEar.x() * width
                val rightY = rightEar.y() * height

                canvas.drawCircle(leftX, leftY + 20, 10f, ringPaint)
                canvas.drawCircle(rightX, rightY + 20, 10f, ringPaint)
            }
        }
    }

    private fun drawRing(canvas: Canvas) {
        handLandmarks?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                val hand = result.landmarks()[0]

                if (hand.size >= 21) {
                    // landmark 13 = Ring MCP (knuckle), 14 = Ring PIP (first joint)
                    // midpoint nila = tamang posisyon ng singsing
                    val mcpKnuckle = hand[13]
                    val pipJoint   = hand[14]

                    val targetX = ((mcpKnuckle.x() + pipJoint.x()) / 2) * width
                    val targetY = ((mcpKnuckle.y() + pipJoint.y()) / 2) * height

                    val distance = if (smoothedX != 0f && smoothedY != 0f) {
                        sqrt(
                            (targetX - smoothedX) * (targetX - smoothedX) +
                                    (targetY - smoothedY) * (targetY - smoothedY)
                        )
                    } else {
                        0f
                    }

                    // pag malayo na yung jump, i-teleport na lang — ibig sabihin nawala at nabalik ang tracking
                    if (distance > 180 || (smoothedX == 0f && smoothedY == 0f)) {
                        smoothedX = targetX
                        smoothedY = targetY
                    } else {
                        smoothedX += (targetX - smoothedX) * SMOOTHING_FACTOR
                        smoothedY += (targetY - smoothedY) * SMOOTHING_FACTOR
                    }

                    canvas.drawCircle(smoothedX, smoothedY, 14f, ringPaint)
                }
            }
        }
    }

    private fun drawBracelet(canvas: Canvas) {
        handLandmarks?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                val hand = result.landmarks()[0]

                if (hand.size >= 21) {
                    val wrist = hand[0]

                    val targetX = wrist.x() * width
                    val targetY = wrist.y() * height

                    val distance = if (smoothedX != 0f && smoothedY != 0f) {
                        sqrt(
                            (targetX - smoothedX) * (targetX - smoothedX) +
                                    (targetY - smoothedY) * (targetY - smoothedY)
                        )
                    } else {
                        0f
                    }

                    if (distance > 200 || (smoothedX == 0f && smoothedY == 0f)) {
                        smoothedX = targetX
                        smoothedY = targetY
                    } else {
                        smoothedX += (targetX - smoothedX) * SMOOTHING_FACTOR
                        smoothedY += (targetY - smoothedY) * SMOOTHING_FACTOR
                    }

                    canvas.drawCircle(smoothedX, smoothedY, 30f, necklacePaint)
                }
            }
        }
    }
}