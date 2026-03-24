package com.thesis.lumine.data.api

import com.thesis.lumine.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface LumineApiService {

    // ── Profile endpoints — kunin at i-update yung user profile data ───────────

    @GET("api/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<UserProfileResponse>

    // i-update yung profile info ng user sa backend
    @PUT("api/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileResponse>

    // i-upload yung avatar ng user bilang multipart file
    @Multipart
    @POST("api/profile/{userId}/avatar")
    suspend fun uploadAvatar(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<AvatarResponse>

    // kunin yung list ng favorite jewelry IDs ng user
    @GET("api/profile/{userId}/favorites")
    suspend fun getFavorites(@Path("userId") userId: String): Response<List<String>>

    // idagdag yung jewelry sa favorites ng user
    @POST("api/profile/{userId}/favorites/{jewelryId}")
    suspend fun addFavorite(
        @Path("userId") userId: String,
        @Path("jewelryId") jewelryId: String
    ): Response<Unit>

    // tanggalin yung jewelry sa favorites ng user
    @DELETE("api/profile/{userId}/favorites/{jewelryId}")
    suspend fun removeFavorite(
        @Path("userId") userId: String,
        @Path("jewelryId") jewelryId: String
    ): Response<Unit>

    // ── Admin user management — para sa admin na nag-mamanage ng users ─────────

    // kunin lahat ng users para sa admin panel
    @GET("api/admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUserInfo>>

    @GET("api/admin/users/{userId}")
    suspend fun getAdminUser(@Path("userId") userId: String): Response<AdminUserInfo>

    // i-delete yung account ng specific user — pangit pero kailangan
    @DELETE("api/admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<Unit>

    @GET("api/admin/users/{userId}/favorites")
    suspend fun getUserFavorites(@Path("userId") userId: String): Response<List<String>>

    // Auth endpoints — register, login, at OTP flows
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/verify-and-register")
    suspend fun verifyAndRegister(@Body request: VerifyAndRegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // i-send yung OTP sa email ng user para sa verification
    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<Map<String, String>>

    // i-verify yung OTP code tapos i-return ang auth response
    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    // Evaluation endpoints — para sa ratings ng AR try-on experience
    @POST("api/evaluation")
    suspend fun submitEvaluation(@Body request: EvaluationRequest): Response<Unit>

    // kunin yung summary ng lahat ng ratings — para sa admin dashboard
    @GET("api/evaluation/summary")
    suspend fun getEvaluationSummary(): Response<List<EvaluationSummary>>

    // Jewelry endpoints — CRUD operations para sa jewelry catalog
    @GET("api/jewelry")
    suspend fun getAllJewelry(): Response<List<Jewelry>>

    @GET("api/jewelry/{id}")
    suspend fun getJewelryById(@Path("id") id: String): Response<Jewelry>

    // i-create yung bagong jewelry item sa catalog
    @POST("api/jewelry")
    suspend fun createJewelry(@Body jewelry: Jewelry): Response<Jewelry>

    // i-update yung existing jewelry — palitan yung info sa backend
    @PUT("api/jewelry/{id}")
    suspend fun updateJewelry(
        @Path("id") id: String,
        @Body jewelry: Jewelry
    ): Response<Jewelry>

    // i-delete yung jewelry item — permanent, walang undo
    @DELETE("api/jewelry/{id}")
    suspend fun deleteJewelry(@Path("id") id: String): Response<Unit>
}