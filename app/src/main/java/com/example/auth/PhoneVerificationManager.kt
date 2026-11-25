package com.example.auth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.integrations.BusinessLineId
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

private const val PHONE_AUTH_TAG = "PhoneVerification"

data class DeviceTelephonyStatus(
  val hasReadPhoneState: Boolean = false,
  val simReady: Boolean = false,
  val carrierName: String = ""
)

sealed interface PhoneVerificationStage {
  data object Idle : PhoneVerificationStage
  data class SendingCode(val phoneNumber: String) : PhoneVerificationStage
  data class CodeSent(
    val phoneNumber: String,
    val verificationId: String?,
    val isLocalPinMode: Boolean = false,
    val message: String = "Enter the 6-digit verification code."
  ) : PhoneVerificationStage
  data object Verifying : PhoneVerificationStage
  data class Verified(
    val line: BusinessLineId,
    val phoneNumber: String,
    val method: String,
    val verifiedAt: Long
  ) : PhoneVerificationStage
  data class Error(val message: String) : PhoneVerificationStage
}

data class PhoneVerificationUiState(
  val targetLine: BusinessLineId = BusinessLineId.PHONE_1,
  val stage: PhoneVerificationStage = PhoneVerificationStage.Idle,
  val pendingPinHash: String = "",
  val pendingPhoneNumber: String = ""
)

object PhoneVerificationManager {

  private val random = SecureRandom()

  /**
   * Normalizes US/international phone inputs into E.164 format when possible.
   */
  fun normalizeToE164(raw: String, defaultCountryCode: String = "+1"): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val digitsOnly = trimmed.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return ""
    return when {
      trimmed.startsWith("+") -> "+$digitsOnly"
      digitsOnly.length == 10 -> "$defaultCountryCode$digitsOnly"
      digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+$digitsOnly"
      else -> "+$digitsOnly"
    }
  }

  fun isValidE164(e164: String): Boolean {
    return Regex("^\\+[1-9]\\d{7,14}$").matches(e164.trim())
  }

  fun readDeviceTelephonyStatus(context: Context): DeviceTelephonyStatus {
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPerm) {
      return DeviceTelephonyStatus(hasReadPhoneState = false)
    }
    return try {
      val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
      val simReady = tm?.simState == TelephonyManager.SIM_STATE_READY
      val carrier = tm?.networkOperatorName.orEmpty().trim()
        .ifBlank { tm?.simOperatorName.orEmpty().trim() }
      DeviceTelephonyStatus(
        hasReadPhoneState = true,
        simReady = simReady,
        carrierName = carrier
      )
    } catch (_: Exception) {
      DeviceTelephonyStatus(hasReadPhoneState = true)
    }
  }

  fun generateSixDigitPin(): String {
    val code = 100_000 + random.nextInt(900_000)
    return code.toString()
  }

  /**
   * Opens the user's native SMS composer app (zero SEND_SMS permission required)
   * so Phone 1 can send a real 6-digit pairing/verification PIN to Phone 2.
   */
  fun createSmsVerificationIntent(targetNumber: String, pin: String, lineLabel: String): Intent {
    val body = "Ocean Tide Drop AI Surfer verification code for $lineLabel: $pin"
    return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(targetNumber)}")).apply {
      putExtra("sms_body", body)
    }
  }

  /**
   * Starts Firebase Phone Number Verification (Play Services / reCAPTCHA, zero SMS permissions).
   */
  fun startFirebasePhoneVerification(
    context: Context,
    rawPhoneNumber: String,
    onCodeSent: (verificationId: String) -> Unit,
    onAutoVerified: (PhoneAuthCredential, String) -> Unit,
    onFailed: (String) -> Unit
  ) {
    val e164 = normalizeToE164(rawPhoneNumber)
    if (!isValidE164(e164)) {
      onFailed("Enter a valid phone number with area code (e.g. (808) 555-1234 or +18085551234).")
      return
    }
    val activity = context.findActivity()
    if (activity == null) {
      onFailed("Unable to launch phone verification without an active screen.")
      return
    }
    if (!GoogleSignInManager.isFirebaseInitialized(context)) {
      onFailed("Firebase is not initialized. Please restart the app or use 2-Phone PIN verification.")
      return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Log.d(PHONE_AUTH_TAG, "Phone verification auto-completed for $e164")
        onAutoVerified(credential, e164)
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.w(PHONE_AUTH_TAG, "Firebase Phone Verification failed: ${e.message}", e)
        val msg = e.localizedMessage ?: "Phone verification failed."
        val friendly = when {
          msg.contains("operation-not-allowed", ignoreCase = true) ||
            msg.contains("not enabled", ignoreCase = true) ||
            msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
            "Firebase SMS Phone Auth is not enabled in your Firebase Console yet (Authentication -> Sign-in method -> Phone). You can enable it there or use 'Verify via 2-Phone PIN' below right now."
          msg.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
            msg.contains("quota", ignoreCase = true) ->
            "Too many SMS verification attempts right now. Wait a few minutes or use 'Verify via 2-Phone PIN'."
          msg.contains("INVALID_PHONE_NUMBER", ignoreCase = true) ->
            "Invalid phone number format. Include country & area code (e.g. +18085551234)."
          else -> "$msg You can also use 'Verify via 2-Phone PIN' below."
        }
        onFailed(friendly)
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        Log.d(PHONE_AUTH_TAG, "Firebase SMS code sent to $e164")
        onCodeSent(verificationId)
      }
    }

    try {
      val options = PhoneAuthOptions.newBuilder(Firebase.auth)
        .setPhoneNumber(e164)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)
        .build()
      PhoneAuthProvider.verifyPhoneNumber(options)
    } catch (e: Exception) {
      Log.e(PHONE_AUTH_TAG, "Failed to start verifyPhoneNumber", e)
      onFailed(e.localizedMessage ?: "Unable to start SMS verification.")
    }
  }

  suspend fun completeFirebasePhoneCredential(
    context: Context,
    credential: PhoneAuthCredential
  ): Result<String> {
    return try {
      if (!GoogleSignInManager.isFirebaseInitialized(context)) {
        return Result.failure(IllegalStateException("Firebase not initialized"))
      }
      val auth = Firebase.auth
      val currentUser = auth.currentUser
      if (currentUser != null) {
        try {
          val linkResult = currentUser.linkWithCredential(credential).await()
          val linkedPhone = linkResult.user?.phoneNumber ?: currentUser.phoneNumber.orEmpty()
          Result.success(linkedPhone)
        } catch (collision: FirebaseAuthUserCollisionException) {
          Log.w(PHONE_AUTH_TAG, "Phone already linked to another account; verifying credential directly", collision)
          Result.success("")
        }
      } else {
        val signInRes = auth.signInWithCredential(credential).await()
        Result.success(signInRes.user?.phoneNumber.orEmpty())
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Result.failure(IllegalArgumentException("Incorrect 6-digit verification code. Please check and try again."))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
