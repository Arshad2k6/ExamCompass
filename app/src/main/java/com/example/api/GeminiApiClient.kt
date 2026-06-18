package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getAiResponse(prompt: String, systemInstruction: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateMockResponse(prompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No text response could be generated."
        } catch (e: Exception) {
            "Error contacting Gemini API: ${e.localizedMessage}. Falling back to helpful simulation mode.\n\n${generateMockResponse(prompt)}"
        }
    }

    private fun generateMockResponse(prompt: String): String {
        val cleaned = prompt.lowercase()
        return when {
            cleaned.contains("concept") || cleaned.contains("explain") -> {
                "📚 **Concept Demystification:**\n\nTo explain this concept, let us break it down into core components:\n\n1. **Core Definition**: The standard implementation focuses on structured decoupling of stateful variables.\n2. **Underlying Architecture**: Designed to support extreme concurrency using Kotlin Coroutines and Flows.\n3. **Practical Analogy**: Think of it as a water pipeline supplying fluid data representations onto your Jetpack Compose screens dynamically.\n\n*This is a custom pre-seeded Compass AI tutor explanation to assist active revision.*"
            }
            cleaned.contains("note") || cleaned.contains("generate note") -> {
                "📝 **Compass AI Smart Notes**\n\n**Topic: Software Design Patterns & MVVM**\n\n- **Model**: Represents physical Room schemas or network payloads.\n- **View**: Jetpack Compose Composables rendering the reactive UI state.\n- **ViewModel**: Preserves view state across config rotations, communicating with repositories.\n\n*💡 Practice Tip: Always decouple business logic from local composable triggers.*"
            }
            cleaned.contains("mcq") || cleaned.contains("quiz") || cleaned.contains("question") -> {
                "❓ **Compass AI Generated Assessment**:\n\n**Q1: Which of the following defines Clean Architecture principles correctly?**\n- A) Coupling components tightly to databases.\n- B) Direct dependencies between UI screens and low-level drivers.\n- C) Business logic sits at the center, isolated from databases or frameworks.\n- D) Deleting models from the view structure.\n\n**Correct Answer**: C\n**Explanation**: Clean architecture structures concentric rings ensuring enterprise code relies strictly on internal entities, not visual UI drivers."
            }
            cleaned.contains("plan") || cleaned.contains("study planner") || cleaned.contains("schedule") -> {
                "🗓️ **Compass AI Optimized Study Schedule**:\n\n- **Week 1-2**: Complete core data structures traversals (Trees, BSTs, and Graphs).\n- **Week 3**: Implement mock assessments & past papers to identify critical performance drop levels.\n- **Week 4 (Sprint)**: Review BCNF rules, indexing operations and relational algebra schemas."
            }
            else -> {
                "🧭 **Compass AI Answer Portal**:\n\nYou inquired about: *\"$prompt\"*\n\nHere is a tailored edtech response:\n- Key Focus: Direct exam success and comprehensive professional preparation.\n- Revision strategy: Spend 45 minutes on active notes recap, then complete the unit-wise quiz modules.\n- Next step: Select one of our dynamic subjects on the home screen to unlock mock test papers."
            }
        }
    }
}
