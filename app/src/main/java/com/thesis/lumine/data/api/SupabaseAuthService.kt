package com.thesis.lumine.data.api

import com.thesis.lumine.data.model.SupabaseCodeRequest
import com.thesis.lumine.data.model.SupabasePasswordRequest
import com.thesis.lumine.data.model.SupabaseSession
import com.thesis.lumine.data.model.SupabaseVerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Supabase Auth, called directly, only to set an admin's password: the server refuses
 * admin accounts on its reset endpoints and has no change-password endpoint. Everything
 * else goes through the server. These are the same requests it makes for customers.
 */
interface SupabaseAuthService {

    /** Emails a one-time code to an existing account. */
    @POST("otp")
    suspend fun sendCode(@Body request: SupabaseCodeRequest): Response<Unit>

    @POST("verify")
    suspend fun verifyCode(@Body request: SupabaseVerifyRequest): Response<SupabaseSession>

    /** Sets the password of the user who owns [bearer]. 422 if the password is rejected. */
    @PUT("user")
    suspend fun setPassword(
        @Header("Authorization") bearer: String,
        @Body request: SupabasePasswordRequest
    ): Response<Unit>
}
