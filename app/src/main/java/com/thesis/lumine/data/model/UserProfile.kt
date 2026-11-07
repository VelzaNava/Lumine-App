package com.thesis.lumine.data.model

data class UserProfileResponse(
    val userId: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val mobileNumber: String? = null,
    val avatarUrl: String? = null
)

data class UpdateProfileRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String?
)

data class AvatarResponse(val avatarUrl: String)

data class ImageUploadResponse(val imageUrl: String)

/** A try-on rating. The server records it for the signed-in user. */
data class EvaluationRequest(
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
    val mobileNumber: String?,
    val avatarUrl: String? = null
)
