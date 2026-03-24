package com.thesis.lumine.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // palitan ng computer IP pag testing locally, 10.0.2.2 pag emulator
    private const val BASE_URL = "https://neaped-lanell-momentous.ngrok-free.dev"

    // i-log lahat ng HTTP requests para madaling mag-debug
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // i-configure yung HTTP client — 30 second timeout para sa connect, read, at write
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // i-build yung Retrofit instance gamit yung base URL at Gson converter
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // dito na makukuha yung actual API service na gagamitin sa buong app
    val apiService: LumineApiService = retrofit.create(LumineApiService::class.java)
}