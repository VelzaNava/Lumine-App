package com.thesis.lumine.utils

import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.Locale
import kotlin.math.hypot

/**
 * Sends MediaPipe landmarks to JewelryTracker.cs in Unity.
 *
 * UnitySendMessage is called through reflection so the app still runs when the Unity
 * library isn't in the build. Numbers use Locale.US so the decimal separator is always "."
 */
object UnityBridge {

    private const val TAG         = "UnityBridge"
    private const val GAME_OBJECT = "JewelryTracker"   // GameObject name in the Unity scene

    // must match the public methods in JewelryTracker.cs
    private const val METHOD_SET_ACTIVE_MODEL = "SetActiveModel"
    private const val METHOD_ADD_MODEL        = "AddModel"
    private const val METHOD_REMOVE_MODEL     = "RemoveModel"
    private const val METHOD_SHOW_RING        = "ShowRing"
    private const val METHOD_HIDE_RING        = "HideRing"
    private const val METHOD_SHOW_EARRINGS    = "ShowEarrings"
    private const val METHOD_HIDE_EARRINGS    = "HideEarrings"
    private const val METHOD_SHOW_BRACELET    = "ShowBracelet"
    private const val METHOD_HIDE_BRACELET    = "HideBracelet"
    private const val METHOD_SHOW_NECKLACE    = "ShowNecklace"
    private const val METHOD_HIDE_NECKLACE    = "HideNecklace"

    private var isUnityAvailable = false

    // Frames without landmarks before a model is hidden; 0 hides it right away.
    private const val HIDE_GRACE_FRAMES = 0
    private var emptyRingFrames     = 0
    private var emptyEarringFrames  = 0
    private var emptyBraceletFrames = 0
    private var emptyNecklaceFrames = 0

    // Size multipliers, sent as the last value of each message. Unity applies them after
    // placing the model, so they change the size without moving it, on top of the scale
    // multipliers set in the JewelryTracker Inspector. The necklace has none: Unity sizes
    // it from the face width sent by sendNecklacePosition().
    private const val RING_SCALE_OVERRIDE     = 0.7f
    private const val EARRING_SCALE_OVERRIDE  = 3.0f
    private const val BRACELET_SCALE_OVERRIDE = 3.0f

    // tragus landmarks, used to measure face width for the necklace
    private const val LM_LEFT_TRAGUS  = 234
    private const val LM_RIGHT_TRAGUS = 454

    fun initialize() {
        isUnityAvailable = try {
            Class.forName("com.unity3d.player.UnityPlayer")
            Log.d(TAG, "Unity runtime found, bridge active")
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Unity runtime not found, running 2D only")
            false
        }
    }

    /**
     * Tells Unity which model to show, as "type|identifier", e.g. "ring|wedding ring handmade".
     * Uses the item's modelUrl when it has one, since display names can repeat.
     */
    fun setActiveModel(type: String, name: String, modelUrl: String? = null) {
        if (!isUnityAvailable) return
        val identifier = (modelUrl?.takeIf { it.isNotBlank() } ?: name).trim().lowercase()
        val message    = "${type.trim().lowercase()}|$identifier"
        sendMessage(GAME_OBJECT, METHOD_SET_ACTIVE_MODEL, message)
    }

    /** Adds a second item (Try More) without removing the first. */
    fun addModel(type: String, name: String, modelUrl: String? = null) {
        if (!isUnityAvailable) return
        val identifier = (modelUrl?.takeIf { it.isNotBlank() } ?: name).trim().lowercase()
        sendMessage(GAME_OBJECT, METHOD_ADD_MODEL, "${type.trim().lowercase()}|$identifier")
    }

    fun removeModel(type: String) {
        if (!isUnityAvailable) return
        sendMessage(GAME_OBJECT, METHOD_REMOVE_MODEL, type.trim().lowercase())
    }

    /**
     * Ring position from landmarks 13 (ring finger knuckle) and 14 (first joint). Landmark 9,
     * the middle finger knuckle next to it, lets Unity estimate the finger's width.
     *
     * Format: "k13x,k13y,k14x,k14y,avgZ,m9x,m9y,size"
     */
    fun sendRingPosition(result: HandLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.landmarks().isEmpty()) {
            emptyRingFrames++
            if (emptyRingFrames >= HIDE_GRACE_FRAMES) {
                sendMessage(GAME_OBJECT, METHOD_HIDE_RING, "")
            }
            return
        }
        emptyRingFrames = 0

        val hand = result.landmarks()[0]
        if (hand.size < 21) {
            Log.w(TAG, "Unexpected hand landmark count: ${hand.size}")
            return
        }

        val mcp         = hand[13] // ring finger base knuckle
        val pip         = hand[14] // ring finger first joint
        val neighborMcp = hand[9]  // middle finger base knuckle
        val avgZ        = (mcp.z() + pip.z()) / 2f

        val message = String.format(
            Locale.US, "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
            mcp.x(),         mcp.y(),
            pip.x(),         pip.y(),
            avgZ,
            neighborMcp.x(), neighborMcp.y(),
            RING_SCALE_OVERRIDE
        )
        sendMessage(GAME_OBJECT, METHOD_SHOW_RING, message)
    }

    fun hideRing() = sendMessage(GAME_OBJECT, METHOD_HIDE_RING, "")

    /**
     * Earlobe positions. X is the tragus (234 left, 454 right); Y is blended 45/55 with the
     * jaw landmark below it so the earring hangs from the lobe, not the cartilage above.
     * The Z values let Unity hide the earring on the side turned away from the camera.
     *
     * Format: "lx,ly,rx,ry,lz,rz,size"
     */
    fun sendEarringPositions(result: FaceLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.faceLandmarks().isEmpty()) {
            emptyEarringFrames++
            if (emptyEarringFrames >= HIDE_GRACE_FRAMES) {
                sendMessage(GAME_OBJECT, METHOD_HIDE_EARRINGS, "")
            }
            return
        }
        emptyEarringFrames = 0

        val face = result.faceLandmarks()[0]

        val leftTragus  = face[234]
        val rightTragus = face[454]

        // jaw angle just below each ear
        val leftJawEar  = face[132]
        val rightJawEar = face[361]

        val lx = leftTragus.x()
        val ly = leftTragus.y()  * 0.45f + leftJawEar.y()  * 0.55f
        val rx = rightTragus.x()
        val ry = rightTragus.y() * 0.45f + rightJawEar.y() * 0.55f
        val lz = leftTragus.z()
        val rz = rightTragus.z()

        val message = String.format(
            Locale.US, "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
            lx, ly, rx, ry, lz, rz, EARRING_SCALE_OVERRIDE
        )
        sendMessage(GAME_OBJECT, METHOD_SHOW_EARRINGS, message)
    }

    fun hideEarrings() = sendMessage(GAME_OBJECT, METHOD_HIDE_EARRINGS, "")

    /**
     * Wrist (0) and the index (5), pinky (17) and middle (9) knuckles, with depth, so Unity
     * can size the bracelet to the hand and turn it with the wrist.
     *
     * Format: "wx,wy,i5x,i5y,p17x,p17y,wz,i5z,p17z,m9x,m9y,m9z,size"
     */
    fun sendBraceletPosition(result: HandLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.landmarks().isEmpty()) {
            emptyBraceletFrames++
            if (emptyBraceletFrames >= HIDE_GRACE_FRAMES) {
                sendMessage(GAME_OBJECT, METHOD_HIDE_BRACELET, "")
            }
            return
        }
        emptyBraceletFrames = 0

        val hand = result.landmarks()[0]
        if (hand.size < 21) return

        val wrist     = hand[0]
        val indexMcp  = hand[5]
        val middleMcp = hand[9]
        val pinkyMcp  = hand[17]

        val message = String.format(
            Locale.US,
            "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
            wrist.x(),     wrist.y(),
            indexMcp.x(),  indexMcp.y(),
            pinkyMcp.x(),  pinkyMcp.y(),
            wrist.z(),     indexMcp.z(),  pinkyMcp.z(),
            middleMcp.x(), middleMcp.y(), middleMcp.z(),
            BRACELET_SCALE_OVERRIDE
        )
        sendMessage(GAME_OBJECT, METHOD_SHOW_BRACELET, message)
    }

    fun hideBracelet() = sendMessage(GAME_OBJECT, METHOD_HIDE_BRACELET, "")

    /**
     * Chin (152) plus face width. Unity hangs the necklace below the chin by a fraction of the
     * face width, so it stays on the collarbone at any distance from the camera.
     *
     * Format: "x,y,z,faceWidth"
     */
    fun sendNecklacePosition(result: FaceLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.faceLandmarks().isEmpty()) {
            emptyNecklaceFrames++
            if (emptyNecklaceFrames >= HIDE_GRACE_FRAMES) {
                sendMessage(GAME_OBJECT, METHOD_HIDE_NECKLACE, "")
            }
            return
        }
        emptyNecklaceFrames = 0

        val face = result.faceLandmarks()[0]
        val chin = face[152]

        // tragus to tragus; hypot so a tilted head doesn't read as a narrower face
        val lTragus = face[LM_LEFT_TRAGUS]
        val rTragus = face[LM_RIGHT_TRAGUS]
        val faceWidthNorm = hypot(
            (rTragus.x() - lTragus.x()).toDouble(),
            (rTragus.y() - lTragus.y()).toDouble()
        ).toFloat()

        val message = String.format(
            Locale.US, "%.4f,%.4f,%.4f,%.4f",
            chin.x(), chin.y(), chin.z(), faceWidthNorm
        )
        sendMessage(GAME_OBJECT, METHOD_SHOW_NECKLACE, message)
    }

    fun hideNecklace() = sendMessage(GAME_OBJECT, METHOD_HIDE_NECKLACE, "")

    // Through reflection, so the app has no compile-time dependency on Unity.
    private fun sendMessage(gameObject: String, method: String, data: String) {
        if (!isUnityAvailable) return
        try {
            val unityPlayer = Class.forName("com.unity3d.player.UnityPlayer")
            val send = unityPlayer.getMethod(
                "UnitySendMessage",
                String::class.java,
                String::class.java,
                String::class.java
            )
            send.invoke(null, gameObject, method, data)
            Log.v(TAG, "$gameObject.$method($data)")
        } catch (e: Exception) {
            Log.e(TAG, "UnitySendMessage failed: ${e.message}")
        }
    }
}
