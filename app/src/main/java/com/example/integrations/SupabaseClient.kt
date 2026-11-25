package com.example.integrations

import com.example.data.local.CallEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

sealed interface SupabaseConnectionState {
  data object Idle : SupabaseConnectionState
  data object Testing : SupabaseConnectionState
  data class Connected(val latencyMs: Long, val message: String) : SupabaseConnectionState
  data class Error(val message: String) : SupabaseConnectionState
}

@JsonClass(generateAdapter = true)
data class SupabaseCallRecordDto(
  val id: Long? = null,
  @Json(name = "caller_number") val callerNumber: String = "",
  @Json(name = "caller_name") val callerName: String = "Ocean Caller",
  @Json(name = "call_timestamp") val callTimestamp: String? = null,
  @Json(name = "duration_seconds") val durationSeconds: Int = 0,
  val status: String = "PENDING_REVIEW",
  val urgency: String = "MEDIUM",
  val topic: String = "GENERAL",
  val summary: String? = null,
  @Json(name = "chatgpt_summary") val chatgptSummary: String? = null,
  val transcription: String? = null,
  @Json(name = "customer_question") val customerQuestion: String = "",
  @Json(name = "ai_response") val aiResponse: String = "",
  @Json(name = "escalation_alert_minutes") val escalationAlertMinutes: Int = 10,
  @Json(name = "is_escalated") val isEscalated: Boolean = false,
  @Json(name = "escalation_notes") val escalationNotes: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCallPatchDto(
  val status: String,
  @Json(name = "is_escalated") val isEscalated: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseWaveAuditLeadDto(
  @Json(name = "business_name") val businessName: String,
  @Json(name = "contact_name") val contactName: String,
  @Json(name = "contact_email") val contactEmail: String,
  @Json(name = "contact_phone") val contactPhone: String,
  @Json(name = "biggest_bottleneck") val biggestBottleneck: String,
  @Json(name = "recommended_product") val recommendedProduct: String,
  val status: String = "NEW_LEAD"
)

interface SupabaseRestApi {
  @GET("rest/v1/calls?select=*&order=id.desc&limit=50")
  suspend fun fetchCalls(
    @Header("apikey") apiKey: String,
    @Header("Authorization") bearer: String?
  ): Response<List<SupabaseCallRecordDto>>

  @GET("rest/v1/calls?select=id&limit=1")
  suspend fun pingCallsTable(
    @Header("apikey") apiKey: String,
    @Header("Authorization") bearer: String?
  ): Response<List<SupabaseCallRecordDto>>

  @POST("rest/v1/calls")
  suspend fun insertCall(
    @Header("apikey") apiKey: String,
    @Header("Authorization") bearer: String?,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: SupabaseCallRecordDto
  ): Response<List<SupabaseCallRecordDto>>

  @PATCH("rest/v1/calls")
  suspend fun patchCallStatus(
    @Header("apikey") apiKey: String,
    @Header("Authorization") bearer: String?,
    @Header("Prefer") prefer: String = "return=representation",
    @Query("id") idEq: String,
    @Body body: SupabaseCallPatchDto
  ): Response<List<SupabaseCallRecordDto>>

  @POST("rest/v1/wave_audit_leads")
  suspend fun submitWaveAuditLead(
    @Header("apikey") apiKey: String,
    @Header("Authorization") bearer: String?,
    @Header("Prefer") prefer: String = "return=representation",
    @Body body: SupabaseWaveAuditLeadDto
  ): Response<List<SupabaseWaveAuditLeadDto>>
}

object SupabaseClient {

  private val okHttpClient: OkHttpClient by lazy {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
      redactHeader("apikey")
      redactHeader("Authorization")
    }
    OkHttpClient.Builder()
      .addInterceptor(logging)
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()
  }

  fun resolveBearerHeader(apiKey: String): String? {
    val trimmed = apiKey.trim()
    return if (trimmed.count { it == '.' } == 2 && !trimmed.startsWith("sb_publishable_")) {
      "Bearer $trimmed"
    } else {
      null
    }
  }

  fun extractHost(rawUrl: String): String {
    return rawUrl.trim()
      .removePrefix("https://")
      .removePrefix("http://")
      .substringBefore("/")
      .ifBlank { "pdbpmnnjhzrfezfmvami.supabase.co" }
  }

  private fun normalizeBaseUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
  }

  private fun buildApi(baseUrl: String): SupabaseRestApi {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create())
      .build()
      .create(SupabaseRestApi::class.java)
  }

  suspend fun testConnection(config: SupabaseConfig): SupabaseConnectionState {
    val baseUrl = normalizeBaseUrl(config.projectUrl)
      ?: return SupabaseConnectionState.Error("Invalid Supabase URL. Expected https://<project>.supabase.co")
    if (config.anonKey.isBlank()) {
      return SupabaseConnectionState.Error("Supabase Publishable / Anon Key is missing. Set VITE_SUPABASE_PUBLISHABLE_KEY in the AI Studio Secrets panel or Settings.")
    }

    val start = System.currentTimeMillis()
    return try {
      val api = buildApi(baseUrl)
      val key = config.anonKey.trim()
      val response = api.pingCallsTable(
        apiKey = key,
        bearer = resolveBearerHeader(key)
      )
      val elapsed = System.currentTimeMillis() - start
      if (response.isSuccessful) {
        SupabaseConnectionState.Connected(
          latencyMs = elapsed,
          message = "PostgREST HTTP ${response.code()} connected to ${baseUrl.removeSuffix("/")}"
        )
      } else {
        SupabaseConnectionState.Error(
          "Supabase responded HTTP ${response.code()} (${response.message()}). Verify publishable key & RLS policies on public.calls."
        )
      }
    } catch (e: Exception) {
      SupabaseConnectionState.Error(
        "Supabase network check: ${e.localizedMessage ?: "Endpoint unreachable"}"
      )
    }
  }

  suspend fun fetchCalls(config: SupabaseConfig): Result<List<CallEntity>> {
    val baseUrl = normalizeBaseUrl(config.projectUrl)
      ?: return Result.failure(IllegalArgumentException("Invalid Supabase project URL"))
    if (config.anonKey.isBlank()) {
      return Result.failure(IllegalArgumentException("Missing Supabase publishable/anon key"))
    }
    return try {
      val api = buildApi(baseUrl)
      val key = config.anonKey.trim()
      val response = api.fetchCalls(
        apiKey = key,
        bearer = resolveBearerHeader(key)
      )
      if (!response.isSuccessful) {
        return Result.failure(IllegalStateException("HTTP ${response.code()} from Supabase PostgREST"))
      }
      val rows = response.body().orEmpty().map { dto ->
        val parsedTs = parseIsoTimestamp(dto.callTimestamp) ?: System.currentTimeMillis()
        val deadline = parsedTs + (dto.escalationAlertMinutes.coerceAtLeast(1) * 60_000L)
        val rawTranscript = dto.transcription ?: ""
        val parsedLine = extractReceivingLine(rawTranscript)
        CallEntity(
          id = dto.id ?: 0L,
          callerNumber = dto.callerNumber.ifBlank { "Private / Hidden Number" },
          callerName = dto.callerName.ifBlank { "Incoming Caller" },
          timestamp = parsedTs,
          durationSeconds = dto.durationSeconds,
          status = dto.status,
          urgency = dto.urgency,
          topic = dto.topic,
          summary = dto.chatgptSummary ?: dto.summary ?: "Synced from Supabase public.calls",
          transcription = rawTranscript,
          customerQuestion = dto.customerQuestion,
          aiResponse = dto.aiResponse,
          escalationAlertMinutes = dto.escalationAlertMinutes,
          escalationDeadline = deadline,
          isEscalated = dto.isEscalated,
          escalationNotes = dto.escalationNotes ?: "",
          receivingLine = parsedLine
        )
      }
      Result.success(rows)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun extractReceivingLine(transcription: String?): String {
    if (transcription.isNullOrBlank()) return BusinessLineId.PHONE_1.id
    val match = Regex("\\[Line:\\s*(PHONE_1|PHONE_2)\\b[^\\]]*\\]", RegexOption.IGNORE_CASE)
      .find(transcription)
    return when (match?.groupValues?.getOrNull(1)?.uppercase()) {
      BusinessLineId.PHONE_2.id -> BusinessLineId.PHONE_2.id
      else -> BusinessLineId.PHONE_1.id
    }
  }

  private fun appendLineTag(transcription: String, lineId: String): String {
    val cleanLine = if (lineId.equals(BusinessLineId.PHONE_2.id, ignoreCase = true)) {
      BusinessLineId.PHONE_2.id
    } else {
      BusinessLineId.PHONE_1.id
    }
    if (Regex("\\[Line:\\s*(PHONE_1|PHONE_2)\\b", RegexOption.IGNORE_CASE).containsMatchIn(transcription)) {
      return transcription
    }
    val tag = "[Line: $cleanLine]"
    return if (transcription.isBlank()) tag else "$transcription\n$tag"
  }

  suspend fun syncCallRecord(config: SupabaseConfig, call: CallEntity): Result<Unit> {
    val baseUrl = normalizeBaseUrl(config.projectUrl)
      ?: return Result.failure(IllegalArgumentException("Invalid Supabase URL"))
    if (config.anonKey.isBlank()) {
      return Result.failure(IllegalArgumentException("Missing Supabase publishable/anon key"))
    }
    return try {
      val api = buildApi(baseUrl)
      val key = config.anonKey.trim()
      val dto = SupabaseCallRecordDto(
        callerNumber = call.callerNumber,
        callerName = call.callerName,
        callTimestamp = formatIsoTimestamp(call.timestamp),
        durationSeconds = call.durationSeconds,
        status = call.status,
        urgency = call.urgency,
        topic = call.topic,
        summary = call.summary,
        chatgptSummary = call.summary,
        transcription = appendLineTag(call.transcription, call.receivingLine),
        customerQuestion = call.customerQuestion,
        aiResponse = call.aiResponse,
        escalationAlertMinutes = call.escalationAlertMinutes,
        isEscalated = call.isEscalated,
        escalationNotes = call.escalationNotes
      )
      val resp = api.insertCall(
        apiKey = key,
        bearer = resolveBearerHeader(key),
        body = dto
      )
      if (resp.isSuccessful) Result.success(Unit)
      else Result.failure(IllegalStateException("HTTP ${resp.code()} syncing call"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun submitWaveAuditLead(
    config: SupabaseConfig,
    lead: SupabaseWaveAuditLeadDto
  ): Result<Unit> {
    val baseUrl = normalizeBaseUrl(config.projectUrl)
      ?: return Result.failure(IllegalArgumentException("Invalid Supabase URL"))
    if (config.anonKey.isBlank()) {
      return Result.failure(IllegalArgumentException("Missing Supabase publishable/anon key"))
    }
    return try {
      val api = buildApi(baseUrl)
      val key = config.anonKey.trim()
      val resp = api.submitWaveAuditLead(
        apiKey = key,
        bearer = resolveBearerHeader(key),
        body = lead
      )
      if (resp.isSuccessful) Result.success(Unit)
      else Result.failure(IllegalStateException("HTTP ${resp.code()} submitting lead"))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun formatIsoTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
  }

  private fun parseIsoTimestamp(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return try {
      val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
      sdf.timeZone = TimeZone.getTimeZone("UTC")
      sdf.parse(raw.take(19))?.time
    } catch (_: Exception) {
      null
    }
  }
}
