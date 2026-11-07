package com.thesis.lumine.data.model

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JewelryTest {

    private fun jewelry(
        type: String = "ring",
        isAvailable: Boolean = true,
        isArEnabled: Boolean = true,
        modelUrl: String? = "dome",
        stockQuantity: Int? = null
    ) = Jewelry(
        id = "id", createdAt = "", name = "Item", type = type, material = "Gold", price = 100.0,
        imageUrl = null, modelUrl = modelUrl, isAvailable = isAvailable, isArEnabled = isArEnabled,
        stockQuantity = stockQuantity
    )

    @Test
    fun untrackedStockFollowsTheSwitch() {
        assertTrue(jewelry(isAvailable = true, stockQuantity = null).inStock)
        assertFalse(jewelry(isAvailable = false, stockQuantity = null).inStock)
    }

    @Test
    fun trackedUnitsOverrideAContradictingSwitch() {
        assertFalse(jewelry(isAvailable = true, stockQuantity = 0).inStock)
        assertTrue(jewelry(isAvailable = false, stockQuantity = 5).inStock)
    }

    @Test
    fun arNeedsBothTheSwitchAndAModel() {
        assertTrue(jewelry(isArEnabled = true, modelUrl = "dome").isArReady)
        assertFalse(jewelry(isArEnabled = false, modelUrl = "dome").isArReady)
        assertFalse(jewelry(isArEnabled = true, modelUrl = null).isArReady)
        assertFalse(jewelry(isArEnabled = true, modelUrl = "  ").isArReady)
    }

    @Test
    fun everyOtherTypeCanBeAdded() {
        val types = listOf("ring", "earring", "bracelet", "necklace")
        for (main in types) {
            for (second in types - main) {
                assertTrue("$main with $second", jewelry(type = main).pairsWith(jewelry(type = second)))
            }
        }
    }

    @Test
    fun aSecondPieceOfTheSameTypeIsNeverOffered() {
        for (type in listOf("ring", "earring", "bracelet", "necklace")) {
            assertFalse(type, jewelry(type = type).pairsWith(jewelry(type = type)))
        }
        assertFalse(jewelry(type = "Necklace").pairsWith(jewelry(type = " necklace ")))
    }

    @Test
    fun itemsThatCantBeShownAreNotOffered() {
        assertFalse(jewelry(type = "ring").pairsWith(jewelry(type = "bracelet", isArEnabled = false)))
        assertFalse(jewelry(type = "ring").pairsWith(jewelry(type = "bracelet", modelUrl = null)))
    }

    @Test
    fun computedStateIsNotSentToTheServer() {
        val json = Gson().toJson(jewelry())
        assertFalse(json.contains("inStock"))
        assertFalse(json.contains("isArReady"))
        assertTrue(json.contains("isAvailable"))
    }
}
