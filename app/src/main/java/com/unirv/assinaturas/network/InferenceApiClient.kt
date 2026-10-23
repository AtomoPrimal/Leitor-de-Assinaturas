package com.unirv.assinaturas.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.unirv.assinaturas.model.BoundingBox
import com.unirv.assinaturas.model.InferenceResult
import android.os.Build
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface InferenceApiService {
    @POST("/api/inferencia")
    suspend fun sendInferenceData(@Body payload: InferenceResult): Response<Unit>

    @GET("/api/inferencia/historico")
    suspend fun getInferenceHistory(): List<InferenceResult>

    @GET("/api/inferencia/{id}/caixas")
    suspend fun getSessionBoxes(@Path("id") id: String): List<BoundingBox>
}

class InferenceApiClient(private var baseUrl: String = defaultServerUrl()) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private var retrofit: Retrofit = buildRetrofit()
    var apiService: InferenceApiService = retrofit.create(InferenceApiService::class.java)
        private set

    fun updateBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        this.baseUrl = formatted
        this.retrofit = buildRetrofit()
        this.apiService = retrofit.create(InferenceApiService::class.java)
    }

    fun getBaseUrl(): String = baseUrl

    private fun buildRetrofit(): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private companion object {
        fun defaultServerUrl(): String {
            val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true) ||
                Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
                Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

            return if (isEmulator) "http://10.0.2.2:8081" else "http://127.0.0.1:8081"
        }
    }
}
