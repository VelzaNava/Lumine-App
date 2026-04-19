package com.thesis.lumine.data.model

data class UserProfileResponse(
    val userId: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val mobileNumber: String? = null,
    val avatarUrl: String? = null
)

data class AvatarResponse(val avatarUrl: String)
data class ImageUploadResponse(val imageUrl: String)

data class UpdateProfileRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String?
)

data class EvaluationRequest(
    val userId: String,
    val jewelryId: String,
    val jewelryName: String,
    val rating: Int,
    val comment: String?
)

data class EvaluationSummary(
    val jewelryId: String,
    val jewelryName: String,
    val averageRating: Double,
    val totalRatings: Int
)

data class AdminUserInfo(
    val userId: String,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String?
)
