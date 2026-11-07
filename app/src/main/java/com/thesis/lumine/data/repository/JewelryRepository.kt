package com.thesis.lumine.data.repository

import com.thesis.lumine.data.api.ApiClient
import com.thesis.lumine.data.model.DeleteAccountRequest
import com.thesis.lumine.data.model.EvaluationRequest
import com.thesis.lumine.data.model.ForgotPasswordRequest
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.model.LoginRequest
import com.thesis.lumine.data.model.OtpRequest
import com.thesis.lumine.data.model.ResetPasswordRequest
import com.thesis.lumine.data.model.SupabaseCodeRequest
import com.thesis.lumine.data.model.SupabasePasswordRequest
import com.thesis.lumine.data.model.SupabaseVerifyRequest
import com.thesis.lumine.data.model.UpdateProfileRequest
import com.thesis.lumine.data.model.VerifyAndRegisterRequest
import okhttp3.MultipartBody

/** Single entry point the ViewModels use to reach the backend. */
class JewelryRepository {

    private val api = ApiClient.service

    // Auth

    suspend fun login(email: String, password: String) = api.login(LoginRequest(email, password))

    suspend fun sendOtp(email: String) = api.sendOtp(OtpRequest(email))

    suspend fun verifyAndRegister(email: String, token: String, password: String) =
        api.verifyAndRegister(VerifyAndRegisterRequest(email, token, password))

    suspend fun forgotPassword(email: String) = api.forgotPassword(ForgotPasswordRequest(email))

    suspend fun resetPassword(email: String, token: String, newPassword: String) =
        api.resetPassword(ResetPasswordRequest(email, token, newPassword))

    // Admin passwords: the server can't set them, so these go to Supabase directly.

    val canSetAdminPassword: Boolean
        get() = ApiClient.supabaseAuth != null

    private val supabaseAuth
        get() = checkNotNull(ApiClient.supabaseAuth) { "This build has no Supabase key" }

    suspend fun sendAdminResetCode(email: String) = supabaseAuth.sendCode(SupabaseCodeRequest(email))

    suspend fun verifyAdminResetCode(email: String, code: String) =
        supabaseAuth.verifyCode(SupabaseVerifyRequest(email, code))

    suspend fun setAdminPassword(accessToken: String, newPassword: String) =
        supabaseAuth.setPassword("Bearer $accessToken", SupabasePasswordRequest(newPassword))

    // Catalog

    suspend fun getAllJewelry() = api.getAllJewelry()

    suspend fun createJewelry(jewelry: Jewelry) = api.createJewelry(jewelry)

    suspend fun updateJewelry(id: String, jewelry: Jewelry) = api.updateJewelry(id, jewelry)

    suspend fun deleteJewelry(id: String) = api.deleteJewelry(id)

    suspend fun uploadJewelryImage(filePart: MultipartBody.Part) = api.uploadJewelryImage(filePart)

    // Profile and favorites

    suspend fun getProfile(userId: String) = api.getProfile(userId)

    suspend fun updateProfile(userId: String, request: UpdateProfileRequest) =
        api.updateProfile(userId, request)

    suspend fun uploadAvatar(userId: String, filePart: MultipartBody.Part) = api.uploadAvatar(userId, filePart)

    suspend fun getFavorites(userId: String) = api.getFavorites(userId)

    suspend fun addFavorite(userId: String, jewelryId: String) = api.addFavorite(userId, jewelryId)

    suspend fun removeFavorite(userId: String, jewelryId: String) = api.removeFavorite(userId, jewelryId)

    suspend fun deleteAccount(password: String) = api.deleteAccount(DeleteAccountRequest(password))

    // Admin

    suspend fun getAdminUsers() = api.getAdminUsers()

    suspend fun deleteUser(userId: String) = api.deleteUser(userId)

    suspend fun getUserFavorites(userId: String) = api.getUserFavorites(userId)

    // Ratings

    suspend fun submitEvaluation(request: EvaluationRequest) = api.submitEvaluation(request)

    suspend fun getEvaluationSummary() = api.getEvaluationSummary()
}
