package com.example.ai

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatMessage(
  val role: String,
  val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
  val model: String = "gpt-4o-mini",
  val messages: List<ChatMessage>,
  val temperature: Double = 0.7
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
  val index: Int = 0,
  val message: ChatMessage
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
  val id: String?,
  val choices: List<ChatChoice>?
)

interface OpenAiApi {
  @POST("v1/chat/completions")
  suspend fun createChatCompletion(
    @Header("Authorization") authorization: String,
    @Body request: ChatCompletionRequest
  ): ChatCompletionResponse
}

object OpenAiClient {
  private const val PLACEHOLDER_KEY = "OPENAI_API_KEY_DEFAULT_VALUE"

  private val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
    redactHeader("Authorization")
  }

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(logging)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  val api: OpenAiApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.openai.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create())
      .build()
      .create(OpenAiApi::class.java)
  }

  fun configuredBuildConfigKey(): String? {
    val raw = BuildConfig.OPENAI_API_KEY.trim()
    return if (raw.isBlank() || raw == PLACEHOLDER_KEY) null else raw
  }

  fun hasConfiguredBuildConfigKey(): Boolean = configuredBuildConfigKey() != null
}
