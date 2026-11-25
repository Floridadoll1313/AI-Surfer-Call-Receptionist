package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiInstrumentTool
import com.example.ai.CallQueueBriefUiState
import com.example.ai.ImageGenUiState
import com.example.ai.InstrumentTextUiState
import com.example.ai.PitchBriefUiState
import com.example.auth.PhoneVerificationManager
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.integrations.BusinessLineId
import com.example.integrations.BusinessPhonesConfig
import com.example.integrations.IntegrationPrefsStore
import com.example.ui.OceanSurferViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneVerificationAndAiInstrumentsTest {

  @Test
  fun phoneVerificationManager_normalizesE164ValidatesAndGeneratesSixDigitPins() {
    assertEquals("+18085551234", PhoneVerificationManager.normalizeToE164("(808) 555-1234"))
    assertEquals("+18085551234", PhoneVerificationManager.normalizeToE164("1-808-555-1234"))
    assertEquals("+442079460958", PhoneVerificationManager.normalizeToE164("+44 20 7946 0958"))
    assertEquals("", PhoneVerificationManager.normalizeToE164("   "))

    assertTrue(PhoneVerificationManager.isValidE164("+18085551234"))
    assertTrue(PhoneVerificationManager.isValidE164("+442079460958"))
    assertFalse(PhoneVerificationManager.isValidE164("123"))
    assertFalse(PhoneVerificationManager.isValidE164("+0123456789"))

    repeat(10) {
      val pin = PhoneVerificationManager.generateSixDigitPin()
      assertEquals(6, pin.length)
      assertTrue(pin.all { it.isDigit() })
      assertTrue(pin.toInt() in 100_000..999_999)
    }

    val smsIntent = PhoneVerificationManager.createSmsVerificationIntent(
      targetNumber = "+18085559999",
      pin = "482910",
      lineLabel = "Phone 2 • Field Cell"
    )
    assertEquals(Intent.ACTION_SENDTO, smsIntent.action)
    assertTrue(smsIntent.dataString.orEmpty().startsWith("smsto:"))
    assertTrue(smsIntent.getStringExtra("sms_body").orEmpty().contains("482910"))
  }

  @Test
  fun businessPhonesConfig_persistsPerLineVerificationAndResetsOnNumberChange() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val initial = BusinessPhonesConfig(
      phone1Label = "Main Cell",
      phone1Number = "(808) 555-0101",
      phone2Label = "Backup Cell",
      phone2Number = "(808) 555-0202",
      phone1Verified = true,
      phone1VerifiedAt = 1_700_000_000_000L,
      phone2Verified = false,
      verifiedMethod = "Firebase SMS"
    )
    IntegrationPrefsStore.saveBusinessPhonesConfig(context, initial)

    val loaded = IntegrationPrefsStore.loadBusinessPhonesConfig(context)
    assertTrue(loaded.isLineVerified(BusinessLineId.PHONE_1))
    assertFalse(loaded.isLineVerified(BusinessLineId.PHONE_2))
    assertEquals("Firebase SMS", loaded.verifiedMethod)

    val vm = OceanSurferViewModel(AppRepository(AppDatabase.getDatabase(context)), context)
    vm.markPhoneVerified(BusinessLineId.PHONE_2, "+18085550202", "2-Phone SMS PIN")
    assertTrue(vm.businessPhonesConfig.value.isLineVerified(BusinessLineId.PHONE_1))
    assertTrue(vm.businessPhonesConfig.value.isLineVerified(BusinessLineId.PHONE_2))

    // Updating Phone 2 to a different number should invalidate Phone 2 verification while keeping Phone 1 verified
    vm.updateBusinessPhonesConfig(
      phone1Number = "+1 808 555 0101", // same E.164 as (808) 555-0101
      phone2Number = "+18085559999" // changed number
    )
    assertTrue(vm.businessPhonesConfig.value.phone1Verified)
    assertFalse(vm.businessPhonesConfig.value.phone2Verified)
  }

  @Test
  fun aiInstrumentsViewModel_supportsAllSevenToolsAndInputValidation() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = OceanSurferViewModel(AppRepository(AppDatabase.getDatabase(context)), context)

    assertEquals(7, AiInstrumentTool.entries.size)
    AiInstrumentTool.entries.forEach { tool ->
      vm.selectAiInstrument(tool)
      assertEquals(tool, vm.selectedAiInstrument.value)
    }

    vm.generateSalesPitch("", "Missed Sales Calls")
    assertTrue(vm.pitchBriefState.value is PitchBriefUiState.Error)

    vm.generateAiImage("   ", null)
    delay(150)
    assertTrue(vm.imageGenState.value is ImageGenUiState.Error)

    vm.runTranslatorOrRewriter("  ", "Spanish")
    delay(150)
    assertTrue(vm.translatorState.value is InstrumentTextUiState.Error)

    vm.analyzeRealCallQueue()
    delay(150)
    assertTrue(vm.callQueueBriefState.value is CallQueueBriefUiState.Error)
  }
}
