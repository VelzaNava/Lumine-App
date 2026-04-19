package com.thesis.lumine.data.repository

import com.thesis.lumine.data.api.RetrofitClient
import com.thesis.lumine.data.model.*
import okhttp3.MultipartBody

class JewelryRepository {

    private val api = RetrofitClient.apiService

    // profile operations — kunin at i-update yung data ng kasalukuyang user
    suspend fun getProfile(userId: String) = api.getProfile(userId)
    suspend fun updateProfile(userId: String, request: UpdateProfileRequest) = api.updateProfile(userId, request)
    suspend fun uploadAvatar(userId: String, filePart: MultipartBody.Part) = api.uploadAvatar(userId, filePart)
    suspend fun getFavorites(userId: String) = api.getFavorites(userId)
    suspend fun addFavorite(userId: String, jewelryId: String) = api.addFavorite(userId, jewelryId)
    suspend fun removeFavorite(userId: String, jewelryId: String) = api.removeFavorite(userId, jewelryId)

    // admin operations — para lang sa admin, huwag gamitin ng regular user
    suspend fun getAdminUsers() = api.getAdminUsers()
    suspend fun deleteUser(userId: String) = api.deleteUser(userId)
    suspend fun getUserFavorites(userId: String) = api.getUserFavorites(userId)

    // auth calls — i-wrap yung request objects bago i-send sa API
    suspend fun register(email: String, password: String) =
        api.register(RegisterRequest(email, password))

    // i-verify yung OTP tapos sabay create ng account
    suspend fun verifyAndRegister(email: String, token: String, password: String) =
        api.verifyAndRegister(VerifyAndRegisterRequest(email, token, password))

    suspend fun login(email: String, password: String) =
        api.login(LoginRequest(email, password))

    // i-send yung OTP request gamit yung email ng user
    suspend fun sendOtp(email: String) = api.sendOtp(OtpRequest(email))

    suspend fun verifyOtp(email: String, token: String) =
        api.verifyOtp(VerifyOtpRequest(email, token))

    // evaluation / ratings — i-submit yung rating at kunin yung summaries
    suspend fun submitEvaluation(request: EvaluationRequest) = api.submitEvaluation(request)
    suspend fun getEvaluationSummary() = api.getEvaluationSummary()

    // jewelry CRUD — lahat ng operations para sa jewelry catalog
    suspend fun getAllJewelry() = api.getAllJewelry()

    suspend fun getJewelryById(id: String) = api.getJewelryById(id)

    // i-create yung bagong jewelry entry sa database
    suspend fun createJewelry(jewelry: Jewelry) = api.createJewelry(jewelry)

    suspend fun updateJewelry(id: String, jewelry: Jewelry) =
        api.updateJewelry(id, jewelry)

    // i-delete yung jewelry — walang recovery, burahin na talaga
    suspend fun deleteJewelry(id: String) = api.deleteJewelry(id)

    suspend fun uploadJewelryImage(filePart: MultipartBody.Part) = api.uploadJewelryImage(filePart)
}