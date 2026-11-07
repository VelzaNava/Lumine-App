package com.thesis.lumine.data.api

import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * The message to show for a failed response: the server's `{ "error": "..." }`
 * text when it sent one (it's always written for users), otherwise [fallback].
 */
fun Response<*>.errorMessage(fallback: String): String {
    val body = try {
        errorBody()?.string()
    } catch (e: IOException) {
        null
    }

    val message = try {
        body?.let { JSONObject(it).optString("error") }
    } catch (e: JSONException) {
        null
    }

    return message?.takeIf { it.isNotBlank() } ?: fallback
}

/** Seconds the server asked the app to wait (its Retry-After header), or [default]. */
fun Response<*>.retryAfterSeconds(default: Int): Int =
    headers()["Retry-After"]?.trim()?.toIntOrNull()?.coerceIn(1, 300) ?: default

/** A readable message for an exception thrown while calling the server. */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException, is ConnectException ->
        "Can't reach the server. Check your internet connection."
    is SocketTimeoutException ->
        "The server took too long to respond. Please try again."
    is SSLException ->
        "A secure connection couldn't be made. Please try again."
    is IOException ->
        "Network error. Please check your connection and try again."
    else ->
        "Something went wrong. Please try again."
}
