package com.thesis.lumine.data.model

data class Jewelry(
    val id: String,
    val createdAt: String,
    val name: String,
    val type: String,
    val material: String,
    val price: Double,
    /** The cover photo - always the first of [imageUrls]. */
    val imageUrl: String?,
    val modelUrl: String?,
    val description: String? = null,
    val isAvailable: Boolean,
    val isArEnabled: Boolean = false,
    // Optional stock count for the admin. null means not tracked, not zero in stock.
    val stockQuantity: Int? = null,
    /** Up to [MAX_PHOTOS] product photos, cover first. Nullable because Gson skips Kotlin defaults. */
    val imageUrls: List<String>? = null
) {
    /** Every photo, cover first; items saved before multi-photo support have just [imageUrl]. */
    val photos: List<String>
        get() = imageUrls.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOfNotNull(imageUrl?.takeIf { it.isNotBlank() }) }

    /** Whether the item can be bought: a tracked count decides, otherwise [isAvailable] does. */
    val inStock: Boolean
        get() = stockQuantity?.let { it > 0 } ?: isAvailable

    /** AR is turned on and there is a 3D model to show. */
    val isArReady: Boolean
        get() = isArEnabled && !modelUrl.isNullOrBlank()

    /**
     * Whether [other] can be added to this item in AR (Try More). The scene holds one
     * model per type, so a second piece of the same type would replace this one.
     */
    fun pairsWith(other: Jewelry): Boolean =
        other.isArReady && !other.type.trim().equals(type.trim(), ignoreCase = true)

    companion object {
        const val MAX_PHOTOS = 3
    }
}
