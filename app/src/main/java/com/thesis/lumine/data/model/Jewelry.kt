package com.thesis.lumine.data.model

data class Jewelry(
    val id: String,
    val createdAt: String,
    val name: String,
    val type: String,
    val material: String,
    val price: Double,
    val imageUrl: String?,
    val modelUrl: String?,
    val description: String? = null,
    val isAvailable: Boolean,
    val isArEnabled: Boolean = false
)