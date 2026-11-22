package com.example.integrations

import android.content.Context

data class ChatGptConfig(
  val apiKey: String = "",
  val model: String = "gpt-4o-mini",
  val temperature: Double = 0.7,
  val isCustomPromptEnabled: Boolean = true,
  val customPersona: String = "Kai - Ocean Tide Drop AI Surfer Master Receptionist"
)

data class SupabaseConfig(
  val projectUrl: String = "https://kbgujyuxdopx.supabase.co",
  val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.oceantidedrop_mock_anon_key",
  val isConnected: Boolean = true,
  val autoSyncCalls: Boolean = true
)

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

  fun saveSupabaseConfig(context: Context, config: SupabaseConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
      .putString(KEY_SUPABASE_URL, config.projectUrl)
      .putString(KEY_SUPABASE_KEY, config.anonKey)
      .apply()
  }

  fun loadSupabaseConfig(context: Context): SupabaseConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val defaults = SupabaseConfig()
    return SupabaseConfig(
      projectUrl = prefs.getString(KEY_SUPABASE_URL, defaults.projectUrl) ?: defaults.projectUrl,
      anonKey = prefs.getString(KEY_SUPABASE_KEY, defaults.anonKey) ?: defaults.anonKey,
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
