package com.example.domain

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// --- Gemini API (REST) ---
@Serializable
data class GenerateContentRequest(val contents: List<Content>)
@Serializable
data class Content(val parts: List<Part>)
@Serializable
data class Part(val text: String? = null)
@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>)
@Serializable
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Hugging Face Inference API ---
@Serializable
data class WhisperRequest(val fileData: String) // Simplified
@Serializable
data class WhisperResponse(val text: String)

interface HuggingFaceApiService {
    @POST("models/{model}")
    suspend fun runInference(
        @Path("model") model: String,
        @Header("Authorization") token: String,
        @Body request: WhisperRequest
    ): WhisperResponse
}
