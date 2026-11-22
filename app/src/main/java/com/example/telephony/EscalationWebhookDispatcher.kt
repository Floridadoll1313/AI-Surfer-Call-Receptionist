package com.example.telephony

import com.example.data.local.CallEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object EscalationWebhookDispatcher {

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()
  }

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  fun buildPayload(
    call: CallEntity,
    reason: String,
    targetEmail: String
  ): JSONObject {
    val summaryText = call.summary.ifBlank { call.customerQuestion.take(120) }
    val slackText = buildString {
      append("Ocean Tide Drop AI Escalation [").append(call.urgency).append("]\n")
      append("Caller: ").append(call.callerName).append(" (").append(call.callerNumber).append(")\n")
      append("Topic: ").append(call.topic).append('\n')
      append("Reason: ").append(reason).append('\n')
      append("Summary: ").append(summaryText).append('\n')
      append("Crew Email: ").append(targetEmail)
    }
    return JSONObject().apply {
      put("event", "ocean_surfer_escalation")
      put("text", slackText)
      put("callId", call.id)
      put("callerName", call.callerName)
      put("callerNumber", call.callerNumber)
      put("urgency", call.urgency)
      put("topic", call.topic)
      put("summary", call.summary)
      put("customerQuestion", call.customerQuestion)
      put("escalationReason", reason)
      put("targetEmail", targetEmail)
      put("timestamp", call.timestamp)
    }
  }

  suspend fun dispatch(
    webhookUrl: String,
    call: CallEntity,
    reason: String,
    targetEmail: String
  ): Result<Int> = withContext(Dispatchers.IO) {
    val endpoint = webhookUrl.trim()
    if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
      return@withContext Result.failure(IllegalArgumentException("Invalid webhook URL"))
    }
    return@withContext try {
      val payload = buildPayload(call, reason, targetEmail).toString()
      val req = Request.Builder()
        .url(endpoint)
        .post(payload.toRequestBody(jsonMediaType))
        .build()
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) Result.success(resp.code)
        else Result.failure(IllegalStateException("Webhook HTTP ${resp.code}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
