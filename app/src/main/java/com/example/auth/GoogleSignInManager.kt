package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.OceanTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

data class GoogleAccountUiState(
  val isSignedIn: Boolean = false,
  val uid: String = "",
  val displayName: String = "",
  val email: String = "",
  val isLoading: Boolean = false,
  val errorMessage: String? = null
)

tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

object GoogleSignInManager {

  fun isFirebaseInitialized(context: Context): Boolean {
    return try {
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context) != null
      } else {
        true
      }
    } catch (_: Exception) {
      false
    }
  }

  fun currentUser(context: Context): FirebaseUser? {
    return try {
      if (!isFirebaseInitialized(context)) null else Firebase.auth.currentUser
    } catch (_: Exception) {
      null
    }
  }

  fun authStateFlow(context: Context): Flow<FirebaseUser?> = callbackFlow {
    if (!isFirebaseInitialized(context)) {
      trySend(null)
      close()
      return@callbackFlow
    }
    val auth = try {
      Firebase.auth
    } catch (_: Exception) {
      trySend(null)
      close()
      return@callbackFlow
    }
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun resolveWebClientId(context: Context): String? {
    return try {
      context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Silent startup sign-in using a single GetGoogleIdOption request.
   */
  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    val existingUser = currentUser(context)
    if (existingUser != null) {
      onAuthSuccess(existingUser)
      return
    }
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
      onUnauthenticated()
      return
    }
    val activity = context.findActivity()
    val credentialContext: Context = activity ?: context

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val cred = credentialManager.getCredential(credentialContext, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          val authResult = Firebase.auth
            .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .await()
          onAuthSuccess(authResult.user ?: currentUser(context))
        } else {
          onUnauthenticated()
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        onUnauthenticated()
      }
    }
  }

  /**
   * Interactive Google Sign-In using a single GetSignInWithGoogleOption request.
   */
  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
      return
    }
    val activity = context.findActivity()
    if (activity == null) {
      onAuthError("Unable to launch Google Sign-In UI without an active screen.")
      return
    }
    if (!isFirebaseInitialized(context)) {
      onAuthError("Firebase is not initialized yet. Restart the app and try again.")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val cred = credentialManager.getCredential(activity, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          val authResult = Firebase.auth
            .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
            .await()
          onAuthSuccess(authResult.user ?: currentUser(context))
        } else {
          onAuthError("Unexpected credential type returned by Google.")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: NoCredentialException) {
        Log.w(AUTH_TAG, "No Google Account credentials available: ${e.message}", e)
        onAuthError("No Google Account found on this device. Sign in using your Gmail address in Android Settings -> Accounts (not phone number), then tap Sign in with Google.")
      } catch (e: GetCredentialProviderConfigurationException) {
        Log.e(AUTH_TAG, "Credential Manager Play Services provider missing or outdated", e)
        onAuthError("Google Play Services Sign-In is unavailable on this device/emulator.")
      } catch (e: GoogleIdTokenParsingException) {
        Log.e(AUTH_TAG, "Invalid Google ID token received", e)
        onAuthError("Received an invalid Google ID token. Please try again.")
      } catch (e: GetCredentialException) {
        Log.e(AUTH_TAG, "Credential Manager sign-in failed", e)
        val raw = e.localizedMessage.orEmpty()
        val hint = if (raw.contains("phone", ignoreCase = true)) {
          " On Google's sign-in page, enter your Gmail address instead of a phone number."
        } else {
          ""
        }
        onAuthError((raw.ifBlank { "Google Sign-In failed. Please try again." }) + hint)
      } catch (e: Exception) {
        Log.e(AUTH_TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Sign in failed")
      }
    }
  }

  /**
   * Clears Credential Manager state and signs out of Firebase inside NonCancellable.
   */
  fun signOut(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context),
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          try {
            if (isFirebaseInitialized(context)) {
              Firebase.auth.signOut()
            }
          } catch (e: Exception) {
            Log.w(AUTH_TAG, "Failed to sign out of Firebase: ${e.message}")
          }
          onSignOutComplete()
        }
      }
    }
  }

  fun toUiState(
    user: FirebaseUser?,
    isLoading: Boolean = false,
    errorMessage: String? = null
  ): GoogleAccountUiState {
    return if (user != null) {
      GoogleAccountUiState(
        isSignedIn = true,
        uid = user.uid,
        displayName = user.displayName.orEmpty().ifBlank { "Google Account" },
        email = user.email.orEmpty(),
        isLoading = isLoading,
        errorMessage = errorMessage
      )
    } else {
      GoogleAccountUiState(
        isSignedIn = false,
        uid = "",
        displayName = "",
        email = "",
        isLoading = isLoading,
        errorMessage = errorMessage
      )
    }
  }
}

@Composable
fun GoogleSignInButton(
  onAuthSuccess: (FirebaseUser?) -> Unit,
  onAuthError: (String) -> Unit,
  onAuthCancelled: () -> Unit = {},
  externalLoading: Boolean = false,
  onLoadingChanged: (Boolean) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activityOrContext = remember(context) { context.findActivity() ?: context }
  val credentialManager = remember(activityOrContext) { CredentialManager.create(activityOrContext) }
  var internalLoading by remember { mutableStateOf(false) }
  val isLoading = externalLoading || internalLoading
  val colors = OceanTheme.colors

  Button(
    onClick = {
      internalLoading = true
      onLoadingChanged(true)
      GoogleSignInManager.onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { user ->
          internalLoading = false
          onLoadingChanged(false)
          onAuthSuccess(user)
        },
        onAuthError = { msg ->
          internalLoading = false
          onLoadingChanged(false)
          onAuthError(msg)
        },
        scope = scope,
        onAuthCancelled = {
          internalLoading = false
          onLoadingChanged(false)
          onAuthCancelled()
        }
      )
    },
    enabled = !isLoading,
    colors = ButtonDefaults.buttonColors(
      containerColor = colors.turquoise,
      contentColor = colors.abyss
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp)
      .testTag("google_sign_in_button")
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = colors.abyss,
        strokeWidth = 2.dp
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Signing in...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    } else {
      Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = null,
        tint = colors.abyss,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
  }
}
