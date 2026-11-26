package com.example.integrations

import com.example.data.local.WaveAuditLeadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object KaiOwnerLeadClient {
  const val project = "https://mkgnyarwiscttobnytin.supabase.co"
  const val publicKey = "sb_publishable_Jp0Laxs-KoieNMD5hqLA0w_jCnrxATm"
  private val client = OkHttpClient.Builder().followRedirects(false).build()
  private val json = "application/json".toMediaType()

  private fun post(path: String, body: JSONObject, token: String? = null): String {
    val request = Request.Builder().url(project + path).header("apikey", publicKey)
      .post(body.toString().toRequestBody(json))
    if (token != null) request.header("Authorization", "Bearer " + token)
    return client.newCall(request.build()).execute().use { response ->
      if (!response.isSuccessful) throw IOException("HTTP " + response.code)
      response.body?.string().orEmpty()
    }
  }

  suspend fun signInForCalls(email: String, password: String) = withContext(Dispatchers.IO) {
    val credentials = JSONObject().put("email", email.trim()).put("password", password)
    OwnerCallSession.save(JSONObject(post("/auth/v1/token?grant_type=password", credentials)))
  }

  suspend fun submit(lead: WaveAuditLeadEntity, ownerEmail: String, password: String) = withContext(Dispatchers.IO) {
    val credentials = JSONObject().put("email", ownerEmail.trim()).put("password", password)
    val session = JSONObject(post("/auth/v1/token?grant_type=password", credentials))
    val body = JSONObject().put("lead_id", lead.id).put("contact_email", lead.contactEmail)
      .put("is_demo", false).put("contact_name", lead.contactName).put("business_name", lead.businessName)
      .put("contact_phone", lead.contactPhone).put("biggest_bottleneck", lead.biggestBottleneck)
      .put("recommended_product", lead.recommendedProduct)
    post("/functions/v1/kai-owner-lead", body, session.getString("access_token"))
    Unit
  }
}
