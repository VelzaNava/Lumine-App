package com.thesis.lumine.data.api

import com.thesis.lumine.data.model.AdminUserInfo
import com.thesis.lumine.data.model.AuthResponse
import com.thesis.lumine.data.model.AvatarResponse
import com.thesis.lumine.data.model.DeleteAccountRequest
import com.thesis.lumine.data.model.EvaluationRequest
import com.thesis.lumine.data.model.EvaluationSummary
import com.thesis.lumine.data.model.ForgotPasswordRequest
import com.thesis.lumine.data.model.ImageUploadResponse
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.model.LoginRequest
import com.thesis.lumine.data.model.OtpRequest
import com.thesis.lumine.data.model.ResetPasswordRequest
import com.thesis.lumine.data.model.UpdateProfileRequest
import com.thesis.lumine.data.model.UserProfileResponse
import com.thesis.lumine.data.model.VerifyAndRegisterRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * The Lumine backend API. Every call except the auth endpoints carries the
 * signed-in user's access token (see [AuthInterceptor]); the server only lets
 * users touch their own account, and admins everything.
 */
interface LumineApiService {

    // Auth

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /** Sign-up step 1: emails a verification code. 409 if the email is taken. */
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<Map<String, String>>

    /** Sign-up step 2: verifies the code, sets the password and signs in. */
    @POST("api/auth/verify-and-register")
    suspend fun verifyAndRegister(@Body request: VerifyAndRegisterRequest): Response<AuthResponse>

    /** Password reset step 1. 403 for admin accounts, 404 if there is no account. */
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Map<String, String>>

    /** Password reset step 2: verifies the code, sets the new password and signs in. */
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<AuthResponse>

    // Catalog

    @GET("api/jewelry")
    suspend fun getAllJewelry(): Response<List<Jewelry>>

    @GET("api/jewelry/{id}")
    suspend fun getJewelryById(@Path("id") id: String): Response<Jewelry>

    /** Admin only. */
    @POST("api/jewelry")
    suspend fun createJewelry(@Body jewelry: Jewelry): Response<Jewelry>

    /** Admin only. */
    @PUT("api/jewelry/{id}")
    suspend fun updateJewelry(@Path("id") id: String, @Body jewelry: Jewelry): Response<Jewelry>

    /** Admin only. Permanent - there is no undo. */
    @DELETE("api/jewelry/{id}")
    suspend fun deleteJewelry(@Path("id") id: String): Response<Unit>

    /** Admin only. Returns the uploaded photo's public URL. */
    @Multipart
    @POST("api/jewelry/upload-image")
    suspend fun uploadJewelryImage(@Part file: MultipartBody.Part): Response<ImageUploadResponse>

    // Profile and favorites (own account)

    @GET("api/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<UserProfileResponse>

    @PUT("api/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileResponse>

    @Multipart
    @POST("api/profile/{userId}/avatar")
    suspend fun uploadAvatar(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<AvatarResponse>

    /** Ids of the jewelry the user saved. */
    @GET("api/profile/{userId}/favorites")
    suspend fun getFavorites(@Path("userId") userId: String): Response<List<String>>

    @POST("api/profile/{userId}/favorites/{jewelryId}")
    suspend fun addFavorite(
        @Path("userId") userId: String,
        @Path("jewelryId") jewelryId: String
    ): Response<Unit>

    @DELETE("api/profile/{userId}/favorites/{jewelryId}")
    suspend fun removeFavorite(
        @Path("userId") userId: String,
        @Path("jewelryId") jewelryId: String
    ): Response<Unit>

    /**
     * Permanently deletes the signed-in account (not available to admins).
     * A wrong password is 400, not 401, so it never ends the session.
     */
    @POST("api/account/delete")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>

    // Admin: accounts

    @GET("api/admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUserInfo>>

    /** Removes the account with its profile, favorites and photo; its ratings stay, unlinked. */
    @DELETE("api/admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<Unit>

    @GET("api/admin/users/{userId}/favorites")
    suspend fun getUserFavorites(@Path("userId") userId: String): Response<List<String>>

    // Try-on ratings

    /** Rated as the signed-in user. */
    @POST("api/evaluation")
    suspend fun submitEvaluation(@Body request: EvaluationRequest): Response<Unit>

    /** Admin only: average rating per item. */
    @GET("api/evaluation/summary")
    suspend fun getEvaluationSummary(): Response<List<EvaluationSummary>>
}
