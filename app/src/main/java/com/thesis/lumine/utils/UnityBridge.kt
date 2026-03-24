package com.thesis.lumine.utils

import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.Locale

/**
 * UnityBridge.kt
 *
 * Ito yung connector ng MediaPipe at Unity sa Android side.
 * Kino-convert niya yung landmark coordinates galing sa MediaPipe
 * tapos ini-send sa Unity's JewelryTracker.cs gamit UnitySendMessage.
 *
 * Flow: MediaPipe → UnityBridge → UnitySendMessage → JewelryTracker.cs
 *
 * Bakit UnitySendMessage? Kasi pag na-export ang Unity as Android Library (AAR),
 * yun lang yung built-in na paraan para mag-call ng Unity methods from Android —
 * essentially RPC siya across the Android↔Unity boundary.
 *
 * Data format: "x,y,z" — comma-delimited, normalized 0.0–1.0 values.
 * Locale.US ang gamit para hindi mag-break sa devices na gumagamit ng comma
 * as decimal separator (hal. sa ibang locale settings).
 */
object UnityBridge {

    private const val TAG = "UnityBridge"

    // Must match the GameObject name in the Unity scene exactly (case-sensitive)
    private const val GAME_OBJECT = "JewelryTracker"

    // Must match public method names in JewelryTracker.cs exactly
    private const val METHOD_SHOW_RING     = "ShowRing"
    private const val METHOD_HIDE_RING     = "HideRing"
    private const val METHOD_SHOW_NECKLACE = "ShowNecklace"
    private const val METHOD_HIDE_NECKLACE = "HideNecklace"

    // Set to true once we confirm Unity runtime is available
    private var isUnityAvailable = false

    // i-check kung naka-load yung Unity runtime via reflection
    // para hindi mag-crash pag walang Unity sa build (e.g. emulator o UI-only test)
    fun initialize() {
        isUnityAvailable = try {
            Class.forName("com.unity3d.player.UnityPlayer")
            Log.d(TAG, "Unity runtime detected — bridge is active.")
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Unity runtime NOT found — bridge will be inactive. Running 2D-only mode.")
            false
        }
    }

    // RING

    // i-send yung posisyon ng ring finger papunta sa Unity para sa 3D ring rendering
    // ginagamit landmarks 13 (MCP/knuckle) at 14 (PIP/first joint) — midpoint nila
    // yung tamang posisyon ng singsing anatomically, part ng ALBJOA landmark selection
    fun sendRingPosition(result: HandLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.landmarks().isEmpty()) {
            sendMessage(GAME_OBJECT, METHOD_HIDE_RING, "")
            return
        }

        val hand = result.landmarks()[0]
        if (hand.size < 21) {
            Log.w(TAG, "Unexpected landmark count: ${hand.size}")
            return
        }

        // landmark 13 = Ring Finger MCP (base ng ring finger / knuckle)
        // landmark 14 = Ring Finger PIP (first joint above knuckle)
        // midpoint ng dalawa = tamang posisyon ng singsing sa proximal phalanx
        val mcpKnuckle = hand[13]
        val pipJoint   = hand[14]

        // avgZ = relative depth vs wrist; negative = papalapit sa camera, positive = palayo
        // ginagamit ng Unity para i-scale yung ring depende sa distansya ng kamay
        val avgZ = (mcpKnuckle.z() + pipJoint.z()) / 2f

        // format: "k13x,k13y,k14x,k14y,avgZ" — Locale.US para dot ang decimal, hindi comma
        val message = String.format(
            Locale.US, "%.4f,%.4f,%.4f,%.4f,%.4f",
            mcpKnuckle.x(), mcpKnuckle.y(), pipJoint.x(), pipJoint.y(), avgZ
        )
        sendMessage(GAME_OBJECT, METHOD_SHOW_RING, message)
    }

    // itago yung ring pag nawala na yung kamay sa frame
    fun hideRing() {
        sendMessage(GAME_OBJECT, METHOD_HIDE_RING, "")
    }

    // ── NECKLACE (Face tracking) ─────────────────────────────────────────

    // i-send yung posisyon ng baba/leeg papunta sa Unity para sa necklace rendering
    // landmark 152 = pinakababang punto ng baba — doon naka-anchor yung pendant
    fun sendNecklacePosition(result: FaceLandmarkerResult) {
        if (!isUnityAvailable) return

        if (result.faceLandmarks().isEmpty()) {
            sendMessage(GAME_OBJECT, METHOD_HIDE_NECKLACE, "")
            return
        }

        val face = result.faceLandmarks()[0]
        val chin = face[152]  // landmark 152 = baba ng mukha

        val x = chin.x()
        val y = chin.y()
        val z = chin.z()

        val message = String.format(Locale.US, "%.4f,%.4f,%.4f", x, y, z)
        sendMessage(GAME_OBJECT, METHOD_SHOW_NECKLACE, message)
    }

    // itago yung necklace pag nawala na yung mukha sa frame
    fun hideNecklace() {
        sendMessage(GAME_OBJECT, METHOD_HIDE_NECKLACE, "")
    }

    // ── Core IPC Method ──────────────────────────────────────────────────

    // i-call yung UnitySendMessage via reflection para hindi mag-require ng direct Unity import
    // pag walang Unity sa build, hindi mag-crash — 2D mode na lang ang gagamitin
    private fun sendMessage(gameObject: String, method: String, data: String) {
        if (!isUnityAvailable) return
        try {
            val unityPlayer = Class.forName("com.unity3d.player.UnityPlayer")
            val unitySendMessage = unityPlayer.getMethod(
                "UnitySendMessage",
                String::class.java,
                String::class.java,
                String::class.java
            )
            unitySendMessage.invoke(null, gameObject, method, data)
            Log.v(TAG, "UnitySendMessage → $gameObject.$method($data)")
        } catch (e: Exception) {
            Log.e(TAG, "UnitySendMessage failed: ${e.message}")
        }
    }
}
