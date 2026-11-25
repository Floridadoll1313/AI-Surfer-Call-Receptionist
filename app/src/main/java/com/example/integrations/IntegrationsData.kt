package com.example.integrations

import android.content.Context
import com.example.BuildConfig
import com.example.ui.theme.OceanPalette

data class ChatGptConfig(
  val apiKey: String = "",
  val model: String = "gpt-4o-mini",
  val temperature: Double = 0.7,
  val isCustomPromptEnabled: Boolean = true,
  val customPersona: String = "Kai - Ocean Tide Drop AI Surfer Master Receptionist",
  val isEnabled: Boolean = true
)

data class SupabaseConfig(
  val projectUrl: String = defaultProjectUrl(),
  val anonKey: String = defaultPublishableKey(),
  val isConnected: Boolean = true,
  val autoSyncCalls: Boolean = true
) {
  companion object {
    const val PLACEHOLDER_PUBLISHABLE_KEY = "VITE_SUPABASE_PUBLISHABLE_KEY_DEFAULT_VALUE"
    const val DEFAULT_PROJECT_URL = "https://pdbpmnnjhzrfezfmvami.supabase.co"

    fun defaultProjectUrl(): String {
      return BuildConfig.VITE_SUPABASE_URL.trim().ifBlank { DEFAULT_PROJECT_URL }
    }

    fun defaultPublishableKey(): String {
      val raw = BuildConfig.VITE_SUPABASE_PUBLISHABLE_KEY.trim()
      return if (raw.isBlank() || raw == PLACEHOLDER_PUBLISHABLE_KEY) "" else raw
    }

    fun hasConfiguredBuildConfigKey(): Boolean = defaultPublishableKey().isNotBlank()
  }
}

data class GitHubWebsiteConfig(
  val repoName: String = "AISurfer-",
  val repoOwner: String = "otdaisurfer",
  val repoUrl: String = "https://github.com/otdaisurfer/AISurfer-",
  val websiteUrl: String = "https://otdaisurfer.surf",
  val branch: String = "main",
  val lastDeploymentStatus: String = "Connected: otdaisurfer/AISurfer- (Live on otdaisurfer.surf)",
  val autoDeployOnUpdate: Boolean = true
)

enum class EscalationAlertChannel(val label: String, val shortTag: String) {
  NOTIFICATION("Push Notification", "NOTIFY"),
  EMAIL("Email Alert", "EMAIL"),
  BOTH("Notification + Email", "NOTIFY + EMAIL")
}

data class EscalationTriggerConfig(
  val isEnabled: Boolean = true,
  val alertChannel: EscalationAlertChannel = EscalationAlertChannel.BOTH,
  val targetEmail: String = "oceantidedropservice@gmail.com",
  val webhookUrl: String = "",
  val autoOpenEmailComposer: Boolean = true,
  val escalateOnHighUrgency: Boolean = false
)

enum class BusinessLineId(val id: String, val shortLabel: String) {
  ALL("ALL", "All Phones"),
  PHONE_1("PHONE_1", "Phone 1"),
  PHONE_2("PHONE_2", "Phone 2");

  companion object {
    fun fromId(raw: String?): BusinessLineId {
      return when (raw?.trim()?.uppercase()) {
        "PHONE_2" -> PHONE_2
        "ALL" -> ALL
        else -> PHONE_1
      }
    }
  }
}

data class BusinessPhonesConfig(
  val activeDeviceLine: BusinessLineId = BusinessLineId.PHONE_1,
  val phone1Label: String = "Main Business Cell",
  val phone1Number: String = "",
  val phone2Label: String = "Secondary / Backup Cell",
  val phone2Number: String = "",
  val crossPhoneAlertsEnabled: Boolean = true,
  val phone1Verified: Boolean = false,
  val phone1VerifiedAt: Long = 0L,
  val phone2Verified: Boolean = false,
  val phone2VerifiedAt: Long = 0L,
  val verifiedMethod: String = ""
) {
  fun displayLabelFor(lineId: String): String {
    return when (lineId.trim().uppercase()) {
      BusinessLineId.PHONE_2.id -> "Phone 2 • ${phone2Label.ifBlank { "Secondary Cell" }}"
      BusinessLineId.PHONE_1.id -> "Phone 1 • ${phone1Label.ifBlank { "Main Cell" }}"
      else -> "Phone 1 • ${phone1Label.ifBlank { "Main Cell" }}"
    }
  }

  fun activeLineDisplay(): String = displayLabelFor(activeDeviceLine.id)

  fun activeLineNumber(): String {
    return if (activeDeviceLine == BusinessLineId.PHONE_2) phone2Number else phone1Number
  }

  fun isLineVerified(line: BusinessLineId): Boolean {
    return when (line) {
      BusinessLineId.PHONE_1 -> phone1Verified
      BusinessLineId.PHONE_2 -> phone2Verified
      BusinessLineId.ALL -> phone1Verified && phone2Verified
    }
  }
}

data class TeamSetupBundle(
  val supabaseUrl: String,
  val supabaseKey: String,
  val phone1Label: String,
  val phone1Number: String,
  val phone2Label: String,
  val phone2Number: String,
  val alertEmail: String,
  val webhookUrl: String
)

object TeamSetupCodec {
  private const val PREFIX = "OTDSURFER1:"

  fun encode(
    supabaseConfig: SupabaseConfig,
    phonesConfig: BusinessPhonesConfig,
    escalationConfig: EscalationTriggerConfig
  ): String {
    val json = org.json.JSONObject().apply {
      put("u", supabaseConfig.projectUrl.trim())
      put("k", supabaseConfig.anonKey.trim())
      put("p1l", phonesConfig.phone1Label.trim())
      put("p1n", phonesConfig.phone1Number.trim())
      put("p2l", phonesConfig.phone2Label.trim())
      put("p2n", phonesConfig.phone2Number.trim())
      put("em", escalationConfig.targetEmail.trim())
      put("wh", escalationConfig.webhookUrl.trim())
    }.toString()
    val encoded = android.util.Base64.encodeToString(
      json.toByteArray(Charsets.UTF_8),
      android.util.Base64.NO_WRAP
    )
    return "$PREFIX$encoded"
  }

  fun decode(raw: String): TeamSetupBundle? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith(PREFIX)) return null
    return try {
      val payload = trimmed.removePrefix(PREFIX).trim()
      val decodedBytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
      val json = org.json.JSONObject(String(decodedBytes, Charsets.UTF_8))
      val url = json.optString("u", "").trim()
      if (!url.startsWith("http")) return null
      TeamSetupBundle(
        supabaseUrl = url,
        supabaseKey = json.optString("k", "").trim(),
        phone1Label = json.optString("p1l", "Main Business Cell").trim().ifBlank { "Main Business Cell" },
        phone1Number = json.optString("p1n", "").trim(),
        phone2Label = json.optString("p2l", "Secondary / Backup Cell").trim().ifBlank { "Secondary / Backup Cell" },
        phone2Number = json.optString("p2n", "").trim(),
        alertEmail = json.optString("em", "oceantidedropservice@gmail.com").trim().ifBlank { "oceantidedropservice@gmail.com" },
        webhookUrl = json.optString("wh", "").trim()
      )
    } catch (_: Exception) {
      null
    }
  }
}

data class WaveAuditLead(
  val id: String = java.util.UUID.randomUUID().toString(),
  val businessName: String,
  val contactName: String,
  val contactEmail: String,
  val contactPhone: String,
  val biggestBottleneck: String,
  val recommendedProduct: String,
  val syncedToSupabase: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

object SupabaseSchemaGenerator {
  fun generateSql(): String {
    return """
-- ====================================================================
-- OCEAN TIDE DROP AI SURFER - SUPABASE POSTGRESQL DATABASE SCHEMAS
-- Ocean Surfer Schema for https://otdaisurfer.surf (Repo: otdaisurfer/AISurfer-)
-- ====================================================================

-- 1. Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Incoming Swell Calls Table (caller_id, timestamp, duration, ChatGPT summary)
CREATE TABLE IF NOT EXISTS public.calls (
    id BIGSERIAL PRIMARY KEY,
    caller_id VARCHAR(120) GENERATED ALWAYS AS (caller_name || ' <' || caller_number || '>') STORED,
    caller_number VARCHAR(30) NOT NULL,
    caller_name VARCHAR(100) NOT NULL,
    call_timestamp TIMESTAMPTZ DEFAULT NOW(),
    duration_seconds INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PENDING_REVIEW' CHECK (status IN ('PENDING_REVIEW', 'REVIEWED', 'ESCALATED')),
    urgency VARCHAR(30) DEFAULT 'MEDIUM' CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT_VIP')),
    topic VARCHAR(50) DEFAULT 'GENERAL',
    main_topic VARCHAR(120),
    action_items TEXT,
    conversation_sentiment VARCHAR(40) DEFAULT 'Positive',
    summary TEXT,
    chatgpt_summary TEXT,
    transcription TEXT,
    customer_question TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    escalation_alert_minutes INT DEFAULT 10,
    escalation_deadline TIMESTAMPTZ,
    is_escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMPTZ,
    escalation_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ocean Surfer themed view for PostgREST / Analytics dashboards
CREATE OR REPLACE VIEW public.swell_call_logs AS
SELECT
    id AS swell_id,
    caller_id,
    caller_name AS surfer_name,
    caller_number AS buoy_line,
    call_timestamp,
    duration_seconds AS ride_duration_seconds,
    status AS tide_status,
    urgency AS swell_urgency,
    COALESCE(chatgpt_summary, summary) AS chatgpt_summary,
    main_topic,
    action_items,
    conversation_sentiment,
    is_escalated,
    escalation_notes
FROM public.calls;

-- 3. AI Surfboard & Solutions Catalog (otdaisurfer.surf)
CREATE TABLE IF NOT EXISTS public.surf_products_catalog (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    in_stock BOOLEAN DEFAULT TRUE,
    popular_badge VARCHAR(50),
    surf_level VARCHAR(50) DEFAULT 'All Levels',
    cta_text VARCHAR(100),
    direct_url TEXT,
    slug VARCHAR(100),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Wave Check Diagnostic & Client Leads
CREATE TABLE IF NOT EXISTS public.wave_audit_leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_name VARCHAR(150),
    contact_name VARCHAR(100),
    contact_email VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(30),
    biggest_bottleneck TEXT,
    recommended_product VARCHAR(100),
    status VARCHAR(30) DEFAULT 'NEW_LEAD',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Business Knowledge & FAQs
CREATE TABLE IF NOT EXISTS public.business_faq (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    tags TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Tide Escalation Audit Log
CREATE TABLE IF NOT EXISTS public.tide_escalation_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_id BIGINT REFERENCES public.calls(id) ON DELETE CASCADE,
    triggered_at TIMESTAMPTZ DEFAULT NOW(),
    notification_type VARCHAR(50) DEFAULT 'EMAIL_AND_PUSH',
    recipient_email VARCHAR(100) DEFAULT 'oceantidedropservice@gmail.com',
    status VARCHAR(30) DEFAULT 'DISPATCHED'
);

-- 7. Indexes for High-Speed Swell Lookup
CREATE INDEX IF NOT EXISTS idx_calls_status ON public.calls(status);
CREATE INDEX IF NOT EXISTS idx_calls_caller ON public.calls(caller_number);
CREATE INDEX IF NOT EXISTS idx_calls_timestamp ON public.calls(call_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_calls_deadline ON public.calls(escalation_deadline);
CREATE INDEX IF NOT EXISTS idx_leads_email ON public.wave_audit_leads(contact_email);

-- 8. Row Level Security (RLS) Policies
ALTER TABLE public.calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.surf_products_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wave_audit_leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.business_faq ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tide_escalation_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow read on calls" ON public.calls FOR SELECT USING (true);
CREATE POLICY "Allow insert on calls" ON public.calls FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow update on calls" ON public.calls FOR UPDATE USING (true);
CREATE POLICY "Allow public read on catalog" ON public.surf_products_catalog FOR SELECT USING (true);
CREATE POLICY "Allow public read on business_faq" ON public.business_faq FOR SELECT USING (true);
CREATE POLICY "Allow insert on leads" ON public.wave_audit_leads FOR INSERT WITH CHECK (true);
""".trimIndent()
  }
}

object IntegrationPrefsStore {
  private const val PREFS_NAME = "ocean_surfer_integration_prefs"
  private const val KEY_SUPABASE_URL = "supabase_url"
  private const val KEY_SUPABASE_KEY = "supabase_anon_key"
  private const val KEY_ESCALATION_ENABLED = "escalation_enabled"
  private const val KEY_ESCALATION_CHANNEL = "escalation_channel"
  private const val KEY_ESCALATION_EMAIL = "escalation_email"
  private const val KEY_ESCALATION_AUTO_EMAIL = "escalation_auto_email"
  private const val KEY_ESCALATION_HIGH_URGENCY = "escalation_high_urgency"
  private const val KEY_ESCALATION_WEBHOOK = "escalation_webhook_url"
  private const val KEY_OCEAN_PALETTE = "ocean_theme_palette"
  private const val KEY_CHATGPT_API_KEY = "chatgpt_api_key"
  private const val KEY_CHATGPT_MODEL = "chatgpt_model"
  private const val KEY_CHATGPT_ENABLED = "chatgpt_enabled"
  private const val KEY_ACTIVE_DEVICE_LINE = "active_device_line"
  private const val KEY_PHONE_1_LABEL = "phone_1_label"
  private const val KEY_PHONE_1_NUMBER = "phone_1_number"
  private const val KEY_PHONE_2_LABEL = "phone_2_label"
  private const val KEY_PHONE_2_NUMBER = "phone_2_number"
  private const val KEY_CROSS_PHONE_ALERTS = "cross_phone_alerts_enabled"
  private const val KEY_PHONE_1_VERIFIED = "phone_1_verified"
  private const val KEY_PHONE_1_VERIFIED_AT = "phone_1_verified_at"
  private const val KEY_PHONE_2_VERIFIED = "phone_2_verified"
  private const val KEY_PHONE_2_VERIFIED_AT = "phone_2_verified_at"
  private const val KEY_VERIFIED_METHOD = "phone_verified_method"
  private const val KEY_NOTIFIED_REMOTE_KEYS = "notified_remote_call_keys"

  fun saveBusinessPhonesConfig(context: Context, config: BusinessPhonesConfig) {
    val line = if (config.activeDeviceLine == BusinessLineId.PHONE_2) {
      BusinessLineId.PHONE_2.id
    } else {
      BusinessLineId.PHONE_1.id
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_ACTIVE_DEVICE_LINE, line)
      .putString(KEY_PHONE_1_LABEL, config.phone1Label.trim().ifBlank { "Main Business Cell" })
      .putString(KEY_PHONE_1_NUMBER, config.phone1Number.trim())
      .putString(KEY_PHONE_2_LABEL, config.phone2Label.trim().ifBlank { "Secondary / Backup Cell" })
      .putString(KEY_PHONE_2_NUMBER, config.phone2Number.trim())
      .putBoolean(KEY_CROSS_PHONE_ALERTS, config.crossPhoneAlertsEnabled)
      .putBoolean(KEY_PHONE_1_VERIFIED, config.phone1Verified)
      .putLong(KEY_PHONE_1_VERIFIED_AT, config.phone1VerifiedAt)
      .putBoolean(KEY_PHONE_2_VERIFIED, config.phone2Verified)
      .putLong(KEY_PHONE_2_VERIFIED_AT, config.phone2VerifiedAt)
      .putString(KEY_VERIFIED_METHOD, config.verifiedMethod)
      .apply()
  }

  fun loadBusinessPhonesConfig(context: Context): BusinessPhonesConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val defaults = BusinessPhonesConfig()
    val rawLine = prefs.getString(KEY_ACTIVE_DEVICE_LINE, defaults.activeDeviceLine.id)
    val activeLine = if (rawLine == BusinessLineId.PHONE_2.id) BusinessLineId.PHONE_2 else BusinessLineId.PHONE_1
    return BusinessPhonesConfig(
      activeDeviceLine = activeLine,
      phone1Label = prefs.getString(KEY_PHONE_1_LABEL, defaults.phone1Label) ?: defaults.phone1Label,
      phone1Number = prefs.getString(KEY_PHONE_1_NUMBER, defaults.phone1Number) ?: defaults.phone1Number,
      phone2Label = prefs.getString(KEY_PHONE_2_LABEL, defaults.phone2Label) ?: defaults.phone2Label,
      phone2Number = prefs.getString(KEY_PHONE_2_NUMBER, defaults.phone2Number) ?: defaults.phone2Number,
      crossPhoneAlertsEnabled = prefs.getBoolean(KEY_CROSS_PHONE_ALERTS, defaults.crossPhoneAlertsEnabled),
      phone1Verified = prefs.getBoolean(KEY_PHONE_1_VERIFIED, defaults.phone1Verified),
      phone1VerifiedAt = prefs.getLong(KEY_PHONE_1_VERIFIED_AT, defaults.phone1VerifiedAt),
      phone2Verified = prefs.getBoolean(KEY_PHONE_2_VERIFIED, defaults.phone2Verified),
      phone2VerifiedAt = prefs.getLong(KEY_PHONE_2_VERIFIED_AT, defaults.phone2VerifiedAt),
      verifiedMethod = prefs.getString(KEY_VERIFIED_METHOD, defaults.verifiedMethod) ?: defaults.verifiedMethod
    )
  }

  fun hasNotifiedRemoteCall(context: Context, key: String): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(KEY_NOTIFIED_REMOTE_KEYS, emptySet()) ?: emptySet()
    return set.contains(key)
  }

  fun markRemoteCallNotified(context: Context, key: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = prefs.getStringSet(KEY_NOTIFIED_REMOTE_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
    existing.add(key)
    if (existing.size > 100) {
      val trimmed = existing.toList().takeLast(60).toSet()
      prefs.edit().putStringSet(KEY_NOTIFIED_REMOTE_KEYS, trimmed).apply()
    } else {
      prefs.edit().putStringSet(KEY_NOTIFIED_REMOTE_KEYS, existing).apply()
    }
  }

  fun saveChatGptConfig(context: Context, config: ChatGptConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_CHATGPT_API_KEY, config.apiKey)
      .putString(KEY_CHATGPT_MODEL, config.model)
      .putBoolean(KEY_CHATGPT_ENABLED, config.isEnabled)
      .apply()
  }

  fun loadChatGptConfig(context: Context): ChatGptConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val defaults = ChatGptConfig()
    return defaults.copy(
      apiKey = prefs.getString(KEY_CHATGPT_API_KEY, defaults.apiKey)?.trim() ?: defaults.apiKey,
      model = prefs.getString(KEY_CHATGPT_MODEL, defaults.model) ?: defaults.model,
      isEnabled = prefs.getBoolean(KEY_CHATGPT_ENABLED, defaults.isEnabled)
    )
  }

  fun saveOceanPalette(context: Context, palette: OceanPalette) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_OCEAN_PALETTE, palette.name)
      .apply()
  }

  fun loadOceanPalette(context: Context): OceanPalette {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_OCEAN_PALETTE, OceanPalette.DEEP_SEA.name)
    return OceanPalette.entries.firstOrNull { it.name == stored } ?: OceanPalette.DEEP_SEA
  }

  fun saveSupabaseConfig(context: Context, config: SupabaseConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_SUPABASE_URL, config.projectUrl)
      .putString(KEY_SUPABASE_KEY, config.anonKey)
      .apply()
  }

  fun loadSupabaseConfig(context: Context): SupabaseConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val defaults = SupabaseConfig()
    val storedUrl = prefs.getString(KEY_SUPABASE_URL, null)?.trim()
    val storedKey = prefs.getString(KEY_SUPABASE_KEY, null)?.trim()
    val resolvedUrl = if (
      storedUrl.isNullOrBlank() ||
      storedUrl.contains("kbgujyuxdopx.supabase.co") ||
      storedUrl.contains("mkgnyarwiscttobnytin.supabase.co")
    ) {
      defaults.projectUrl
    } else {
      storedUrl
    }
    val resolvedKey = if (storedKey.isNullOrBlank() || storedKey.contains("oceantidedrop_mock_anon_key")) {
      defaults.anonKey
    } else {
      storedKey
    }
    return SupabaseConfig(
      projectUrl = resolvedUrl,
      anonKey = resolvedKey,
      isConnected = defaults.isConnected,
      autoSyncCalls = defaults.autoSyncCalls
    )
  }

  fun saveEscalationConfig(context: Context, config: EscalationTriggerConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putBoolean(KEY_ESCALATION_ENABLED, config.isEnabled)
      .putString(KEY_ESCALATION_CHANNEL, config.alertChannel.name)
      .putString(KEY_ESCALATION_EMAIL, config.targetEmail)
      .putString(KEY_ESCALATION_WEBHOOK, config.webhookUrl)
      .putBoolean(KEY_ESCALATION_AUTO_EMAIL, config.autoOpenEmailComposer)
      .putBoolean(KEY_ESCALATION_HIGH_URGENCY, config.escalateOnHighUrgency)
      .apply()
  }

  fun loadEscalationConfig(context: Context): EscalationTriggerConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val defaults = EscalationTriggerConfig()
    val channelName = prefs.getString(KEY_ESCALATION_CHANNEL, defaults.alertChannel.name)
    val channel = EscalationAlertChannel.entries.firstOrNull { it.name == channelName } ?: defaults.alertChannel
    return EscalationTriggerConfig(
      isEnabled = prefs.getBoolean(KEY_ESCALATION_ENABLED, defaults.isEnabled),
      alertChannel = channel,
      targetEmail = prefs.getString(KEY_ESCALATION_EMAIL, defaults.targetEmail) ?: defaults.targetEmail,
      webhookUrl = prefs.getString(KEY_ESCALATION_WEBHOOK, defaults.webhookUrl) ?: defaults.webhookUrl,
      autoOpenEmailComposer = prefs.getBoolean(KEY_ESCALATION_AUTO_EMAIL, defaults.autoOpenEmailComposer),
      escalateOnHighUrgency = prefs.getBoolean(KEY_ESCALATION_HIGH_URGENCY, defaults.escalateOnHighUrgency)
    )
  }
}
