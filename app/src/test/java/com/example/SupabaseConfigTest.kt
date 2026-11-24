package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.integrations.IntegrationPrefsStore
import com.example.integrations.SupabaseClient
import com.example.integrations.SupabaseConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseConfigTest {

  @Test
  fun supabaseConfig_usesBuildConfigUrlAndIgnoresPlaceholderKey() {
    val config = SupabaseConfig()
    assertEquals("https://pdbpmnnjhzrfezfmvami.supabase.co", config.projectUrl)
    assertEquals("pdbpmnnjhzrfezfmvami.supabase.co", SupabaseClient.extractHost(config.projectUrl))
    if (BuildConfig.VITE_SUPABASE_PUBLISHABLE_KEY == SupabaseConfig.PLACEHOLDER_PUBLISHABLE_KEY) {
      assertEquals("", config.anonKey)
    }
  }

  @Test
  fun integrationPrefsStore_migratesLegacyMockSupabaseValues() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    IntegrationPrefsStore.saveSupabaseConfig(
      context,
      SupabaseConfig(
        projectUrl = "https://kbgujyuxdopx.supabase.co",
        anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.oceantidedrop_mock_anon_key"
      )
    )

    val loaded = IntegrationPrefsStore.loadSupabaseConfig(context)
    assertEquals("https://pdbpmnnjhzrfezfmvami.supabase.co", loaded.projectUrl)
    assertEquals(SupabaseConfig.defaultPublishableKey(), loaded.anonKey)

    IntegrationPrefsStore.saveSupabaseConfig(
      context,
      SupabaseConfig(
        projectUrl = "https://mkgnyarwiscttobnytin.supabase.co",
        anonKey = "custom_key"
      )
    )
    val migrated = IntegrationPrefsStore.loadSupabaseConfig(context)
    assertEquals("https://pdbpmnnjhzrfezfmvami.supabase.co", migrated.projectUrl)
  }

  @Test
  fun supabaseClient_resolveBearerHeaderHandlesPublishableAndJwtKeys() {
    assertNull(SupabaseClient.resolveBearerHeader("sb_publishable_test_key_value"))
    assertEquals("Bearer header.payload.signature", SupabaseClient.resolveBearerHeader("header.payload.signature"))
  }
}
