package com.thesis.lumine.data.model

/**
 * The 3D models built into the AR scene, by jewelry type. An item's `modelUrl`
 * must be one of these names for AR try-on to show the right piece.
 *
 * Keep in sync with the model lists in the Unity scene (JewelryTracker:
 * ringModels, earringPairs, braceletModels, necklaceModels) - adding a model
 * there means adding its name here.
 */
object ArModels {

    private val byType = mapOf(
        "ring" to listOf("dome", "two toned ring", "wedding ring handmade"),
        "earring" to listOf("ball earrings", "plain earring", "teardrop earrings"),
        "bracelet" to listOf("bangel product", "fendi bangal", "handmade bracelet"),
        "necklace" to listOf("botega", "initial necklace", "paperclip")
    )

    fun forType(type: String): List<String> = byType[type.trim().lowercase()].orEmpty()

    /** "wedding ring handmade" -> "Wedding Ring Handmade". */
    fun displayName(model: String): String =
        model.split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}
