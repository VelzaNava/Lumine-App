package com.thesis.lumine.data.api

import android.content.Context
import com.thesis.lumine.BuildConfig
import com.thesis.lumine.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single HTTP stack the app uses. Initialized once from
 * [com.thesis.lumine.LumineApplication]; the backend address comes from
 * `BuildConfig.API_BASE_URL` (see app/build.gradle.kts).
 */
object ApiClient {

    lateinit var service: LumineApiService
        private set

    /** For setting an admin's password only. Null when this build has no Supabase key. */
    var supabaseAuth: SupabaseAuthService? = null
        private set

    fun initialize(context: Context) {
        if (::service.isInitialized) return

        val session = SessionManager(context.applicationContext)
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS) // image uploads
            .apply { if (BuildConfig.DEBUG) addInterceptor(debugLogging()) }
            .build()

        // Token refresh goes through a plain client: it must never trigger itself.
        val refreshApi = retrofit(baseUrl, baseClient).create(AuthRefreshApi::class.java)

        val authenticatedClient = baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session, refreshApi))
            .build()

        service = retrofit(baseUrl, authenticatedClient).create(LumineApiService::class.java)
        supabaseAuth = createSupabaseAuth()
    }

    private fun createSupabaseAuth(): SupabaseAuthService? {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || anonKey.isBlank()) return null

        // Its own client, with no debug logging: these calls carry codes and a password.
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val authorization = request.header("Authorization") ?: "Bearer $anonKey"
                chain.proceed(
                    request.newBuilder()
                        .header("apikey", anonKey)
                        .header("Authorization", authorization)
                        .build()
                )
            }
            .build()

        return retrofit("$url/auth/v1/", client).create(SupabaseAuthService::class.java)
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // Debug builds only: release builds never log traffic (it contains passwords and tokens).
    private fun debugLogging() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }
}
