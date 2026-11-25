package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
import com.example.integrations.EscalationAlertChannel
import com.example.integrations.EscalationTriggerConfig
import com.example.integrations.IntegrationPrefsStore
import com.example.integrations.SupabaseClient
import com.example.integrations.SupabaseConfig
import com.example.integrations.TeamSetupCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TwoPhoneConnectionTest {

  @Test
  fun teamSetupCodec_roundTripsBundleAndRejectsInvalidCodes() {
    val supabase = SupabaseConfig(
      projectUrl = "https://pdbpmnnjhzrfezfmvami.supabase.co",
      anonKey = "sb_publishable_demo_token_123"
    )
    val phones = BusinessPhonesConfig(
      activeDeviceLine = BusinessLineId.PHONE_1,
      phone1Label = "Office Cell",
      phone1Number = "+1 (808) 555-0100",
      phone2Label = "Field Cell",
      phone2Number = "+1 (808) 555-0200",
      crossPhoneAlertsEnabled = true
    )
    val escalation = EscalationTriggerConfig(
      isEnabled = true,
      alertChannel = EscalationAlertChannel.BOTH,
      targetEmail = "oceantidedropservice@gmail.com",
      webhookUrl = "https://hooks.example.com/ocean"
    )

    val code = TeamSetupCodec.encode(supabase, phones, escalation)
    assertTrue(code.startsWith("OTDSURFER1:"))

    val decoded = TeamSetupCodec.decode(code)
    assertNotNull(decoded)
    assertEquals("https://pdbpmnnjhzrfezfmvami.supabase.co", decoded!!.supabaseUrl)
    assertEquals("sb_publishable_demo_token_123", decoded.supabaseKey)
    assertEquals("Office Cell", decoded.phone1Label)
    assertEquals("+1 (808) 555-0100", decoded.phone1Number)
    assertEquals("Field Cell", decoded.phone2Label)
    assertEquals("+1 (808) 555-0200", decoded.phone2Number)
    assertEquals("oceantidedropservice@gmail.com", decoded.alertEmail)
    assertEquals("https://hooks.example.com/ocean", decoded.webhookUrl)

    assertNull(TeamSetupCodec.decode("INVALID_PREFIX:abc"))
    assertNull(TeamSetupCodec.decode("OTDSURFER1:!!!not_base64!!!"))
  }

  @Test
  fun integrationPrefsStore_persistsBusinessPhonesAndDeduplicatesRemoteCalls() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saved = BusinessPhonesConfig(
      activeDeviceLine = BusinessLineId.PHONE_2,
      phone1Label = "Maui Desk",
      phone1Number = "808-555-0111",
      phone2Label = "Kona Mobile",
      phone2Number = "808-555-0222",
      crossPhoneAlertsEnabled = true
    )
    IntegrationPrefsStore.saveBusinessPhonesConfig(context, saved)

    val loaded = IntegrationPrefsStore.loadBusinessPhonesConfig(context)
    assertEquals(BusinessLineId.PHONE_2, loaded.activeDeviceLine)
    assertEquals("Maui Desk", loaded.phone1Label)
    assertEquals("808-555-0111", loaded.phone1Number)
    assertEquals("Kona Mobile", loaded.phone2Label)
    assertEquals("808-555-0222", loaded.phone2Number)
    assertTrue(loaded.crossPhoneAlertsEnabled)
    assertEquals("Phone 2 • Kona Mobile", loaded.activeLineDisplay())

    val callKey = "+18085559876_1700000000000_PHONE_1"
    assertFalse(IntegrationPrefsStore.hasNotifiedRemoteCall(context, callKey))
    IntegrationPrefsStore.markRemoteCallNotified(context, callKey)
    assertTrue(IntegrationPrefsStore.hasNotifiedRemoteCall(context, callKey))
  }

  @Test
  fun supabaseClient_extractReceivingLineParsesPhoneTagsCorrectly() {
    assertEquals("PHONE_2", SupabaseClient.extractReceivingLine("Real Cell Call\n[Line: PHONE_2]"))
    assertEquals("PHONE_1", SupabaseClient.extractReceivingLine("Real Cell Call\n[Line: PHONE_1]"))
    assertEquals("PHONE_1", SupabaseClient.extractReceivingLine(null))
    assertEquals("PHONE_1", SupabaseClient.extractReceivingLine("Untagged call transcription"))
  }
}
