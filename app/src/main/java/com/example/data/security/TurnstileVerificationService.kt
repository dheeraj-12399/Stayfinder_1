package com.example.data.security

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class TurnstileValidationResult {
    object Success : TurnstileValidationResult()
    data class Failure(val message: String) : TurnstileValidationResult()
}

class TurnstileVerificationService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TurnstileService"

        // Default local and emulator backend URL candidates
        private const val DEFAULT_EMULATOR_URL = "http://10.0.2.2:3000/api/verify-turnstile"
        private const val DEFAULT_LOCALHOST_URL = "http://127.0.0.1:3000/api/verify-turnstile"

        @Volatile
        private var instance: TurnstileVerificationService? = null

        fun getInstance(context: Context): TurnstileVerificationService {
            return instance ?: synchronized(this) {
                instance ?: TurnstileVerificationService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the candidate backend URLs in order of priority.
     */
    private fun getBackendEndpoints(): List<String> {
        val list = mutableListOf<String>()

        // 1. Configured custom URL from BuildConfig if present
        try {
            val buildConfigUrl = BuildConfig::class.java.getField("TURNSTILE_BACKEND_URL").get(null) as? String
            if (!buildConfigUrl.isNullOrBlank() && buildConfigUrl.startsWith("http")) {
                list.add(buildConfigUrl.trim())
            }
        } catch (_: Exception) {
            // Field not present in BuildConfig, continue
        }

        // 2. Android Emulator loopback host
        list.add(DEFAULT_EMULATOR_URL)

        // 3. Localhost loopback
        list.add(DEFAULT_LOCALHOST_URL)

        return list.distinct()
    }

    /**
     * Sends the Turnstile client-side token to the StayFinder backend service.
     * The backend validates the token with Cloudflare Siteverify using the TURNSTILE_SECRET_KEY.
     *
     * Notice: The secret key NEVER touches Android client code.
     */
    suspend fun verifyTokenWithBackend(token: String): TurnstileValidationResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext TurnstileValidationResult.Failure("Turnstile token is empty.")
        }

        val jsonBody = JSONObject().apply {
            put("token", token.trim())
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val endpoints = getBackendEndpoints()
        var lastErrorMessage = "Failed to connect to StayFinder verification backend."

        for (endpoint in endpoints) {
            try {
                Log.d(TAG, "Attempting Turnstile verification against: $endpoint")
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseString = response.body?.string().orEmpty()
                    Log.d(TAG, "Backend response code=${response.code}: $responseString")

                    if (responseString.isNotBlank()) {
                        val json = JSONObject(responseString)
                        val success = json.optBoolean("success", false)
                        if (success) {
                            return@withContext TurnstileValidationResult.Success
                        } else {
                            val msg = json.optString("message", "Security verification failed")
                            return@withContext TurnstileValidationResult.Failure(msg)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Request failed for endpoint $endpoint: ${e.message}")
                lastErrorMessage = e.localizedMessage ?: "Network error connecting to security service."
            }
        }

        // If local backend endpoints are unreachable (e.g., testing on device without local network routing)
        // verify directly if test tokens are being used or return error
        TurnstileValidationResult.Failure(lastErrorMessage)
    }
}
