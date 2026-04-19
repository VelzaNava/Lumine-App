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

    private val dotPaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var faceLandmarks: FaceLandmarkerResult? = null
    private var handLandmarks: HandLandmarkerResult? = null
    private var jewelryType: String = ""

    // ring / bracelet smoothing
    private var smoothedX = 0f
    private var smoothedY = 0f

    // earring: separate smooth per ear
    private var smoothedLeftEarX  = 0f
    private var smoothedLeftEarY  = 0f
    private var smoothedRightEarX = 0f
    private var smoothedRightEarY = 0f

    // necklace smoothing
    private var smoothedNeckLX = 0f; private var smoothedNeckLY = 0f
    private var smoothedNeckCX = 0f; private var smoothedNeckCY = 0f
    private var smoothedNeckRX = 0f; private var smoothedNeckRY = 0f

    private val SMOOTH = 0.25f

    fun setJewelryType(type: String) {
        jewelryType = type.lowercase()
        smoothedX = 0f; smoothedY = 0f
        smoothedLeftEarX = 0f; smoothedLeftEarY = 0f
        smoothedRightEarX = 0f; smoothedRightEarY = 0f
        smoothedNeckLX = 0f; smoothedNeckLY = 0f
        smoothedNeckCX = 0f; smoothedNeckCY = 0f
        smoothedNeckRX = 0f; smoothedNeckRY = 0f
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
            "earring"  -> drawEarrings(canvas)
            "ring"     -> drawRing(canvas)
            "bracelet" -> drawBracelet(canvas)
        }
    }

    private fun drawNecklace(canvas: Canvas) {
        faceLandmarks?.let { result ->
            if (result.faceLandmarks().isNotEmpty()) {
                val lm = result.faceLandmarks()[0]

                // face reference points for adaptive scaling
                val forehead = lm[10]
                val chin     = lm[152]
                val leftEar  = lm[234]
                val rightEar = lm[454]

                val faceHeight = (chin.y() - forehead.y()) * height
                val faceWidth  = (rightEar.x() - leftEar.x()) * width

                // necklace sits on the neck/collarbone — below the chin
                // drop = 18% of face height below the chin center
                // side anchors = at ear X, 12% below ear Y
                val neckDrop     = faceHeight * 0.18f
                val sideDropFrac = 0.12f

                val leftX  = leftEar.x()  * width
                val leftY  = leftEar.y()  * height + faceHeight * sideDropFrac
                val centerX = chin.x()     * width
                val centerY = chin.y()     * height + neckDrop
                val rightX = rightEar.x() * width
                val rightY = rightEar.y() * height + faceHeight * sideDropFrac

                // smooth all three anchor points
                smoothedNeckLX = smooth(smoothedNeckLX, leftX)
                smoothedNeckLY = smooth(smoothedNeckLY, leftY)
                smoothedNeckCX = smooth(smoothedNeckCX, centerX)
                smoothedNeckCY = smooth(smoothedNeckCY, centerY)
                smoothedNeckRX = smooth(smoothedNeckRX, rightX)
                smoothedNeckRY = smooth(smoothedNeckRY, rightY)

                // chain stroke proportional to face width — small indicator
                linePaint.strokeWidth = (faceWidth * 0.008f).coerceIn(2f, 5f)
                val pendantR = (faceWidth * 0.015f).coerceIn(4f, 10f)

                canvas.drawLine(smoothedNeckLX, smoothedNeckLY, smoothedNeckCX, smoothedNeckCY, linePaint)
                canvas.drawLine(smoothedNeckCX, smoothedNeckCY, smoothedNeckRX, smoothedNeckRY, linePaint)
                canvas.drawCircle(smoothedNeckCX, smoothedNeckCY + pendantR, pendantR, dotPaint)
            }
        }
    }

    private fun drawEarrings(canvas: Canvas) {
        faceLandmarks?.let { result ->
            if (result.faceLandmarks().isNotEmpty()) {
                val lm = result.faceLandmarks()[0]

                // 234 / 454 = ear-side of face (tragus level)
                // 132 / 361 = jaw angle near ear (below tragus)
                // blend Y between tragus and jaw angle to hit the earlobe
                val leftTragus  = lm[234]
                val rightTragus = lm[454]
                val leftJawEar  = lm[132]
                val rightJawEar = lm[361]
                val forehead    = lm[10]
                val chin        = lm[152]

                val faceWidth  = (rightTragus.x() - leftTragus.x()) * width

                // earlobe X = tragus X (side of face — correct)
                // earlobe Y = 45% tragus + 55% jaw-ear angle — lands right at the lobe
                val leftX  = leftTragus.x() * width
                val leftY  = (leftTragus.y() * 0.45f + leftJawEar.y() * 0.55f) * height
                val rightX = rightTragus.x() * width
                val rightY = (rightTragus.y() * 0.45f + rightJawEar.y() * 0.55f) * height

                // small earring dot — just a tracking indicator
                val earR   = (faceWidth * 0.03f).coerceIn(5f, 16f)
                val hookW  = (faceWidth * 0.008f).coerceIn(1.5f, 4f)
                val hookLen = earR * 1.2f

                // smooth each ear independently
                smoothedLeftEarX  = smooth(smoothedLeftEarX,  leftX)
                smoothedLeftEarY  = smooth(smoothedLeftEarY,  leftY)
                smoothedRightEarX = smooth(smoothedRightEarX, rightX)
                smoothedRightEarY = smooth(smoothedRightEarY, rightY)

                linePaint.strokeWidth = hookW

                // tiny hook above the dot
                canvas.drawLine(
                    smoothedLeftEarX, smoothedLeftEarY - earR,
                    smoothedLeftEarX, smoothedLeftEarY - earR - hookLen, linePaint)
                canvas.drawLine(
                    smoothedRightEarX, smoothedRightEarY - earR,
                    smoothedRightEarX, smoothedRightEarY - earR - hookLen, linePaint)

                // small gem dots
                canvas.drawCircle(smoothedLeftEarX,  smoothedLeftEarY,  earR, dotPaint)
                canvas.drawCircle(smoothedRightEarX, smoothedRightEarY, earR, dotPaint)
            }
        }
    }

    private fun drawRing(canvas: Canvas) {
        handLandmarks?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                val hand = result.landmarks()[0]
                if (hand.size >= 21) {
                    val mcp = hand[13]
                    val pip = hand[14]

                    val mcpX = mcp.x() * width
                    val mcpY = mcp.y() * height
                    val pipX = pip.x() * width
                    val pipY = pip.y() * height

                    val targetX = (mcpX + pipX) / 2f
                    val targetY = (mcpY + pipY) / 2f

                    // small ring indicator scaled to actual finger segment
                    val segPx  = sqrt((pipX - mcpX) * (pipX - mcpX) + (pipY - mcpY) * (pipY - mcpY))
                    val ringR  = (segPx * 0.3f).coerceIn(6f, 28f)

                    val dist = sqrt((targetX - smoothedX) * (targetX - smoothedX) + (targetY - smoothedY) * (targetY - smoothedY))
                    if (dist > 180f || (smoothedX == 0f && smoothedY == 0f)) {
                        smoothedX = targetX; smoothedY = targetY
                    } else {
                        smoothedX += (targetX - smoothedX) * SMOOTH
                        smoothedY += (targetY - smoothedY) * SMOOTH
                    }

                    canvas.drawCircle(smoothedX, smoothedY, ringR, dotPaint)
                }
            }
        }
    }

    private fun drawBracelet(canvas: Canvas) {
        handLandmarks?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                val hand = result.landmarks()[0]
                if (hand.size >= 21) {
                    val wrist    = hand[0]
                    val indexMcp = hand[5]
                    val pinkyMcp = hand[17]

                    val handWidthPx = sqrt(
                        ((indexMcp.x() - pinkyMcp.x()) * width).let { it * it } +
                        ((indexMcp.y() - pinkyMcp.y()) * height).let { it * it }
                    )

                    val braceletR = (handWidthPx * 0.4f).coerceIn(16f, 60f)
                    linePaint.strokeWidth = (handWidthPx * 0.05f).coerceIn(3f, 10f)

                    val targetX = wrist.x() * width
                    val targetY = wrist.y() * height

                    val dist = sqrt((targetX - smoothedX) * (targetX - smoothedX) + (targetY - smoothedY) * (targetY - smoothedY))
                    if (dist > 200f || (smoothedX == 0f && smoothedY == 0f)) {
                        smoothedX = targetX; smoothedY = targetY
                    } else {
                        smoothedX += (targetX - smoothedX) * SMOOTH
                        smoothedY += (targetY - smoothedY) * SMOOTH
                    }

                    canvas.drawCircle(smoothedX, smoothedY, braceletR, linePaint)
                }
            }
        }
    }

    // exponential moving average — init on first frame
    private fun smooth(current: Float, target: Float): Float {
        return if (current == 0f) target
        else current + (target - current) * SMOOTH
    }
}
