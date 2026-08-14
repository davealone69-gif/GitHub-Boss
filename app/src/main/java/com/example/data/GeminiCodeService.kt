package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Free-tier Gemini via Google AI Studio API key.
 * Get a key (free): https://aistudio.google.com/apikey
 *
 * Falls back gracefully — caller should use KotlinCodeMaker if this fails.
 */
class GeminiCodeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    /**
     * Ask Gemini to produce Kotlin/Compose files as a single markdown response.
     * Returns raw text; parse into files with [parseGeminiOutput].
     */
    suspend fun generateKotlinCode(
        apiKey: String,
        prompt: String,
        packageName: String = "com.example"
    ): ApiResult<String> {
        return try {
            val system = """
You are an expert Android Kotlin engineer.
Generate production-ready Jetpack Compose + ViewModel code for this request.

Rules:
- Package: $packageName
- Use Material 3, StateFlow, viewModelScope, collectAsStateWithLifecycle
- Output ONLY code files in this exact format (repeat for each file):

### FILE: path/to/FileName.kt
```kotlin
// full file content
```

- Include UiState, ViewModel, and @Composable Screen at minimum
- Add Room or Retrofit stubs only if the prompt asks for them
- No explanations outside the FILE blocks
            """.trimIndent()

            val fullPrompt = "$system\n\nUser request:\n$prompt"

            val body = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = fullPrompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.35,
                    maxOutputTokens = 8192
                )
            )

            // Free-tier friendly model id (Flash family)
            val model = "gemini-2.0-flash"
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${apiKey.trim()}"

            val json = requestAdapter.toJson(body)
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = when (response.code) {
                        400 -> "Bad request — check prompt / model"
                        401, 403 -> "Invalid Gemini API key or free quota exceeded"
                        429 -> "Rate limited — wait a bit (free tier limits)"
                        else -> "Gemini error ${response.code}: ${raw.take(200)}"
                    }
                    return ApiResult.Error(msg, response.code)
                }
                val parsed = responseAdapter.fromJson(raw)
                val text = parsed?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                if (text.isNullOrBlank()) {
                    ApiResult.Error("Empty response from Gemini")
                } else {
                    ApiResult.Success(text)
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Gemini network error")
        }
    }

    companion object {
        /**
         * Parse Gemini markdown output into KotlinFile list.
         * Format expected:
         * ### FILE: path/Name.kt
         * ```kotlin
         * ...
         * ```
         */
        fun parseGeminiOutput(raw: String): List<KotlinCodeMaker.KotlinFile> {
            val files = mutableListOf<KotlinCodeMaker.KotlinFile>()
            val regex = Regex(
                """###\s*FILE:\s*([^\n]+)\s*```(?:kotlin)?\s*([\s\S]*?)```""",
                RegexOption.IGNORE_CASE
            )
            regex.findAll(raw).forEach { match ->
                val path = match.groupValues[1].trim().removePrefix("/").trim()
                val content = match.groupValues[2].trim()
                if (path.isNotBlank() && content.isNotBlank()) {
                    val name = path.substringAfterLast('/')
                    files += KotlinCodeMaker.KotlinFile(path = path, name = name, content = content)
                }
            }
            // Fallback: whole response as one file if parse failed
            if (files.isEmpty() && raw.isNotBlank()) {
                files += KotlinCodeMaker.KotlinFile(
                    path = "ui/GeneratedScreen.kt",
                    name = "GeneratedScreen.kt",
                    content = raw
                )
            }
            return files
        }
    }
}

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Double = 0.4,
    val maxOutputTokens: Int = 8192
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)
