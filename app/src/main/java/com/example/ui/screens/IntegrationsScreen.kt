package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiAiService
import com.example.ai.OpenAiClient
import com.example.auth.GoogleSignInButton
import com.example.auth.GoogleSignInManager
import com.example.integrations.BusinessLineId
import com.example.integrations.SupabaseConnectionState
import com.example.integrations.SupabaseSchemaGenerator
import com.example.integrations.TeamSetupCodec
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.OceanWaveHeader
import com.example.ui.components.PhoneVerificationCard
import com.example.ui.theme.OceanPalette
import com.example.ui.theme.OceanTheme

@Composable
fun IntegrationsScreen(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val chatGptConfig by viewModel.chatGptConfig.collectAsStateWithLifecycle()
  val supabaseConfig by viewModel.supabaseConfig.collectAsStateWithLifecycle()
  val supabaseConnState by viewModel.supabaseConnectionState.collectAsStateWithLifecycle()
  val isFetchingSupabase by viewModel.isFetchingFromSupabase.collectAsStateWithLifecycle()
  val gitHubConfig by viewModel.gitHubConfig.collectAsStateWithLifecycle()
  val businessPhonesConfig by viewModel.businessPhonesConfig.collectAsStateWithLifecycle()
  val phoneVerificationState by viewModel.phoneVerificationState.collectAsStateWithLifecycle()
  val escalationConfig by viewModel.escalationTriggerConfig.collectAsStateWithLifecycle()
  val googleAccountState by viewModel.googleAccountState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val scrollState = rememberScrollState()
  val colors = OceanTheme.colors
  val themeController = OceanTheme.controller

  var phone1LabelInput by remember(businessPhonesConfig.phone1Label) { mutableStateOf(businessPhonesConfig.phone1Label) }
  var phone1NumberInput by remember(businessPhonesConfig.phone1Number) { mutableStateOf(businessPhonesConfig.phone1Number) }
  var phone2LabelInput by remember(businessPhonesConfig.phone2Label) { mutableStateOf(businessPhonesConfig.phone2Label) }
  var phone2NumberInput by remember(businessPhonesConfig.phone2Number) { mutableStateOf(businessPhonesConfig.phone2Number) }
  var teamCodeInput by remember { mutableStateOf("") }

  var hasPhoneStatePermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasNotificationPermission by remember {
    mutableStateOf(
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    hasPhoneStatePermission = grants[Manifest.permission.READ_PHONE_STATE] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    if (hasPhoneStatePermission && hasNotificationPermission) {
      viewModel.showSnackbar("All real call & alert permissions are active!")
    }
  }

  var apiKeyInput by remember(chatGptConfig.apiKey) { mutableStateOf(chatGptConfig.apiKey) }
  var selectedModel by remember(chatGptConfig.model) { mutableStateOf(chatGptConfig.model) }
  var showApiKey by remember { mutableStateOf(false) }

  var supabaseUrlInput by remember(supabaseConfig.projectUrl) { mutableStateOf(supabaseConfig.projectUrl) }
  var supabaseKeyInput by remember(supabaseConfig.anonKey) { mutableStateOf(supabaseConfig.anonKey) }
  var showSupabaseKey by remember { mutableStateOf(false) }
  var showSqlSchema by remember { mutableStateOf(false) }

  val isGeminiReady = remember { GeminiAiService.isFirebaseAvailable(context) }
  val isOpenAiSecretConfigured = remember { OpenAiClient.hasConfiguredBuildConfigKey() }

  fun readClipboardText(): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount <= 0) return null
    return clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
  }

  fun writeClipboardText(label: String, value: String) {
    if (value.isBlank()) {
      viewModel.showSnackbar("Nothing to copy yet.")
      return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    viewModel.showSnackbar("$label copied to clipboard!")
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.abyss)
      .verticalScroll(scrollState)
  ) {
    OceanWaveHeader(
      title = "2-Phone Setup & Settings",
      subtitle = "Connect Both Business Phones • Cloud Sync • Publish"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // GOOGLE ACCOUNT SIGN-IN (Credential Manager + Firebase Auth)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("google_account_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(
          1.dp,
          if (googleAccountState.isSignedIn) colors.seaGreen.copy(alpha = 0.65f) else colors.cyan.copy(alpha = 0.55f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = (if (googleAccountState.isSignedIn) colors.seaGreen else colors.cyan).copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = "Google Account",
                  tint = if (googleAccountState.isSignedIn) colors.seaGreen else colors.cyan,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Google Account Sign-In",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = if (googleAccountState.isSignedIn) {
                  "Verified: ${googleAccountState.email.ifBlank { googleAccountState.displayName }}"
                } else {
                  "Sign in with Google Credential Manager for account & email linking"
                },
                color = if (googleAccountState.isSignedIn) colors.seaGreen else colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (!googleAccountState.errorMessage.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = colors.coralAlert.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, colors.coralAlert.copy(alpha = 0.6f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("google_auth_error_banner")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = googleAccountState.errorMessage.orEmpty(),
                  color = colors.textPrimary,
                  fontSize = 11.sp,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { viewModel.clearGoogleAuthError() },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Dismiss Sign-In Error",
                    tint = colors.coralAlert,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }

          if (googleAccountState.isSignedIn) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = colors.card,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = googleAccountState.displayName,
                  color = colors.textPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                if (googleAccountState.email.isNotBlank()) {
                  Text(
                    text = googleAccountState.email,
                    color = colors.turquoise,
                    fontSize = 12.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (googleAccountState.email.isNotBlank() &&
                !googleAccountState.email.equals(escalationConfig.targetEmail, ignoreCase = true)
              ) {
                Button(
                  onClick = { viewModel.useSignedInGoogleEmailForAlerts() },
                  colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("use_google_alert_email_button")
                ) {
                  Text("Use as Alert Email", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              OutlinedButton(
                onClick = {
                  GoogleSignInManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                    onSignOutComplete = { viewModel.onGoogleSignedOut() }
                  )
                },
                border = BorderStroke(1.dp, colors.coralAlert.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("google_sign_out_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Logout,
                  contentDescription = null,
                  tint = colors.coralAlert,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", color = colors.coralAlert, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          } else {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = colors.card,
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
            ) {
              Text(
                text = "Tip: On Google's login page, enter your Gmail address (not a phone number). Emulators & Wi-Fi devices cannot look up accounts by phone number.",
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(10.dp)
              )
            }
            GoogleSignInButton(
              onAuthSuccess = { user -> viewModel.onGoogleAuthSuccess(user) },
              onAuthError = { msg -> viewModel.onGoogleAuthError(msg) },
              onAuthCancelled = { viewModel.onGoogleAuthCancelled() },
              externalLoading = googleAccountState.isLoading,
              onLoadingChanged = { loading -> viewModel.onGoogleAuthLoading(loading) },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = {
                val addGoogleIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                  putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                }
                try {
                  context.startActivity(addGoogleIntent)
                } catch (_: Exception) {
                  try {
                    context.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                  } catch (_: Exception) {
                    viewModel.showSnackbar("Open Android Settings -> Accounts to add your Google account with email.")
                  }
                }
              },
              border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.65f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .testTag("open_android_google_account_settings_button")
            ) {
              Text("Add Google Account in Device Settings", color = colors.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // STEP 1: Connect Two Business Cell Phones
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("two_phone_setup_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.6f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = colors.turquoise.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PhoneAndroid,
                  contentDescription = null,
                  tint = colors.turquoise,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Step 1: Connect 2 Business Phones",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Install this app on Phone 1 & Phone 2 to share real calls & alerts.",
                color = colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Which phone are you holding right now?",
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(BusinessLineId.PHONE_1, BusinessLineId.PHONE_2).forEach { line ->
              val isSelected = businessPhonesConfig.activeDeviceLine == line
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.setActiveDeviceLine(line) },
                label = {
                  Text(
                    text = if (line == BusinessLineId.PHONE_1) "This is Phone 1" else "This is Phone 2",
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.turquoise,
                  selectedLabelColor = colors.abyss,
                  containerColor = colors.card,
                  labelColor = colors.textPrimary
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 44.dp)
                  .testTag("device_line_chip_${line.id.lowercase()}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = phone1LabelInput,
              onValueChange = { phone1LabelInput = it },
              label = { Text("Phone 1 Name", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("phone1_label_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            OutlinedTextField(
              value = phone1NumberInput,
              onValueChange = { phone1NumberInput = it },
              label = { Text("Phone 1 Number", fontSize = 11.sp) },
              placeholder = { Text("(808) 555-...", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("phone1_number_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = phone2LabelInput,
              onValueChange = { phone2LabelInput = it },
              label = { Text("Phone 2 Name", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("phone2_label_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            OutlinedTextField(
              value = phone2NumberInput,
              onValueChange = { phone2NumberInput = it },
              label = { Text("Phone 2 Number", fontSize = 11.sp) },
              placeholder = { Text("(808) 555-...", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("phone2_number_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Real Cross-Phone Call Alerts",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Alert this phone when a real call is logged on the other business phone",
                color = colors.textSecondary,
                fontSize = 10.sp
              )
            }
            Switch(
              checked = businessPhonesConfig.crossPhoneAlertsEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateBusinessPhonesConfig(
                  activeDeviceLine = businessPhonesConfig.activeDeviceLine,
                  phone1Label = phone1LabelInput,
                  phone1Number = phone1NumberInput,
                  phone2Label = phone2LabelInput,
                  phone2Number = phone2NumberInput,
                  crossPhoneAlertsEnabled = enabled
                )
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = colors.abyss,
                checkedTrackColor = colors.turquoise
              ),
              modifier = Modifier.testTag("cross_phone_alerts_switch")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              viewModel.updateBusinessPhonesConfig(
                activeDeviceLine = businessPhonesConfig.activeDeviceLine,
                phone1Label = phone1LabelInput,
                phone1Number = phone1NumberInput,
                phone2Label = phone2LabelInput,
                phone2Number = phone2NumberInput,
                crossPhoneAlertsEnabled = businessPhonesConfig.crossPhoneAlertsEnabled
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 44.dp)
              .testTag("save_two_phone_button")
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save 2-Phone Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 1-Tap Phone-to-Phone Pairing Code
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "1-Tap Pair With Your 2nd Phone",
                color = colors.turquoise,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "On Phone 1, tap Share/Copy Team Code and send it to Phone 2. On Phone 2, tap Paste & Connect.",
                color = colors.textSecondary,
                fontSize = 10.sp
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    val code = TeamSetupCodec.encode(
                      supabaseConfig = supabaseConfig,
                      phonesConfig = businessPhonesConfig.copy(
                        phone1Label = phone1LabelInput,
                        phone1Number = phone1NumberInput,
                        phone2Label = phone2LabelInput,
                        phone2Number = phone2NumberInput
                      ),
                      escalationConfig = escalationConfig
                    )
                    writeClipboardText("Team Connect Code", code)
                  },
                  border = BorderStroke(1.dp, colors.cyan),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("copy_team_code_button")
                ) {
                  Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Copy Code", color = colors.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    val code = TeamSetupCodec.encode(
                      supabaseConfig = supabaseConfig,
                      phonesConfig = businessPhonesConfig.copy(
                        phone1Label = phone1LabelInput,
                        phone1Number = phone1NumberInput,
                        phone2Label = phone2LabelInput,
                        phone2Number = phone2NumberInput
                      ),
                      escalationConfig = escalationConfig
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, "Ocean Tide Drop AI Surfer - 2nd Phone Setup Code")
                      putExtra(
                        Intent.EXTRA_TEXT,
                        "Open Ocean Tide Drop AI Surfer -> Settings -> Paste this Team Connect Code:\n\n$code"
                      )
                    }
                    try {
                      context.startActivity(Intent.createChooser(sendIntent, "Send Setup Code to Phone 2"))
                    } catch (_: Exception) {
                      writeClipboardText("Team Connect Code", code)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("share_team_code_button")
                ) {
                  Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Send to Phone 2", color = colors.abyss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = teamCodeInput,
                  onValueChange = { teamCodeInput = it },
                  placeholder = { Text("Paste OTDSURFER1:... code here", fontSize = 11.sp) },
                  singleLine = true,
                  trailingIcon = {
                    IconButton(
                      onClick = {
                        val pasted = readClipboardText()
                        if (pasted != null) {
                          teamCodeInput = pasted
                          viewModel.applyTeamSetupCode(pasted)
                        } else {
                          viewModel.showSnackbar("Clipboard is empty. Copy the Team Code from Phone 1 first.")
                        }
                      },
                      modifier = Modifier.testTag("paste_team_code_button")
                    ) {
                      Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste Team Code", tint = colors.turquoise)
                    }
                  },
                  modifier = Modifier.weight(1f).testTag("team_code_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.turquoise,
                    unfocusedBorderColor = colors.teal,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                  )
                )
                Button(
                  onClick = { viewModel.applyTeamSetupCode(teamCodeInput) },
                  colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("apply_team_code_button")
                ) {
                  Text("Link", color = colors.abyss, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // STEP 2: Phone Number Verification (Firebase SMS OTP & 2-Phone SMS PIN)
      PhoneVerificationCard(
        businessPhonesConfig = businessPhonesConfig,
        verificationState = phoneVerificationState,
        onSelectLine = { line -> viewModel.selectVerificationLine(line) },
        onSendFirebaseSms = { line, rawNumber ->
          viewModel.startFirebaseSmsVerification(context, line, rawNumber)
        },
        onSendTwoPhonePin = { line, rawNumber ->
          viewModel.startTwoPhonePinVerification(context, line, rawNumber)
        },
        onVerifyCode = { code ->
          viewModel.submitPhoneVerificationCode(context, code)
        },
        onResetVerification = { viewModel.resetPhoneVerification() }
      )

      // STEP 3 CARD SUMMARY: Real Call Permissions & Phone / Google Play Publishing
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("permissions_and_publishing_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = colors.seaGreen.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = colors.seaGreen,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Step 3: Real Call Alerts & Publishing",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Permissions for real incoming calls + Phone APK / Play Store guide",
                color = colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.card,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Phone Call Detection: ${if (hasPhoneStatePermission) "✓ Ready" else "Needs Permission"}",
                color = if (hasPhoneStatePermission) colors.seaGreen else colors.sunsetOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Real Alert Notifications: ${if (hasNotificationPermission) "✓ Ready" else "Needs Permission"}",
                color = if (hasNotificationPermission) colors.seaGreen else colors.sunsetOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (!hasPhoneStatePermission || !hasNotificationPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                val needed = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  needed.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionsLauncher.launch(needed.toTypedArray())
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.sunsetOrange),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .testTag("grant_call_permissions_button")
            ) {
              Text("Grant Real Call & Alert Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.abyss.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Install on Phones & Google Play Guide",
                color = colors.turquoise,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "• Install on Phone 1 & Phone 2: Use AI Studio's Export/Build APK option and open the APK on both phones.",
                color = colors.textSecondary,
                fontSize = 11.sp
              )
              Text(
                text = "• Google Play Console: Package com.aistudio.oceansurfer.kxmvqp (v1.0, code 2) meets Play permission policies. Play Store requires an App Bundle (.aab) signed with your Play upload key (not a debug keystore).",
                color = colors.textSecondary,
                fontSize = 11.sp
              )
            }
          }
        }
      }

      // 0. Dynamic Ocean Theme Switcher (ThemeProvider)
      OceanThemeSwitcherCard(
        activePalette = themeController.currentPalette,
        onSelectPalette = { palette -> themeController.selectPalette(palette) },
        onCyclePalette = { themeController.cyclePalette() }
      )

      // 1. Dual AI Engine (Firebase AI Logic Gemini + ChatGPT Call Summaries)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("chatgpt_integration_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = colors.seaGreen.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = colors.seaGreen,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AI Capabilities: Gemini & ChatGPT",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              val hasAnyOpenAiKey = chatGptConfig.apiKey.isNotBlank() || isOpenAiSecretConfigured
              val isOpenAiActive = chatGptConfig.isEnabled && hasAnyOpenAiKey
              Text(
                text = if (isOpenAiActive) {
                  "OpenAI ChatGPT active • Capturing Topic, Actions & Sentiment"
                } else if (isGeminiReady) {
                  "Firebase AI Logic (gemini-3.8-flash) Active via App Check"
                } else {
                  "Built-in AI Surfer Summarizer Active"
                },
                color = colors.seaGreen,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.card,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Call Summarization Format: Main Topic • Action Items • Sentiment",
                color = colors.turquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Gemini 3.8 Flash handles real-time Client Hub streaming & structured call triage when OpenAI is cleared or disabled.",
                color = colors.textSecondary,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Use OpenAI ChatGPT Key",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = when {
                  !chatGptConfig.isEnabled -> "Released — using Gemini / built-in AI"
                  chatGptConfig.apiKey.isNotBlank() -> "Using saved custom OpenAI key"
                  isOpenAiSecretConfigured -> "Using AI Studio Secrets key (BuildConfig)"
                  else -> "No key active — add in Secrets or paste below"
                },
                color = colors.textSecondary,
                fontSize = 11.sp
              )
            }
            Switch(
              checked = chatGptConfig.isEnabled,
              onCheckedChange = { enabled -> viewModel.setOpenAiEnabled(enabled) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = colors.abyss,
                checkedTrackColor = colors.turquoise,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.card
              ),
              modifier = Modifier.testTag("openai_enabled_switch")
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "OpenAI ChatGPT API Key (Optional override):",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(4.dp))

          OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text("sk-proj-...", color = colors.textSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("chatgpt_api_key_input"),
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    val pasted = readClipboardText()
                    if (pasted != null) {
                      apiKeyInput = pasted
                      viewModel.showSnackbar("OpenAI key pasted from clipboard!")
                    } else {
                      viewModel.showSnackbar("Clipboard is empty. Copy your key first, or use AI Studio Secrets.")
                    }
                  },
                  modifier = Modifier.testTag("paste_chatgpt_api_key_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste OpenAI API key",
                    tint = colors.turquoise
                  )
                }
                if (apiKeyInput.isNotEmpty()) {
                  IconButton(
                    onClick = { apiKeyInput = "" },
                    modifier = Modifier.testTag("clear_chatgpt_key_input_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Clear,
                      contentDescription = "Clear key field",
                      tint = colors.textSecondary
                    )
                  }
                }
                IconButton(onClick = { showApiKey = !showApiKey }) {
                  Icon(
                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle API key visibility",
                    tint = colors.cyan
                  )
                }
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "ChatGPT Model Selection:",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo").forEach { model ->
              FilterChip(
                selected = selectedModel == model,
                onClick = { selectedModel = model },
                label = { Text(model, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.turquoise,
                  selectedLabelColor = colors.abyss,
                  containerColor = colors.card,
                  labelColor = colors.textPrimary
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val trimmed = apiKeyInput.trim()
                if (trimmed.isEmpty()) {
                  viewModel.clearOpenAiKey()
                } else {
                  viewModel.updateChatGptConfig(trimmed, selectedModel, isEnabled = true)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("save_chatgpt_button")
            ) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save AI Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                apiKeyInput = ""
                viewModel.clearOpenAiKey()
              },
              border = BorderStroke(1.dp, colors.coralAlert.copy(alpha = 0.7f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("clear_chatgpt_key_button")
            ) {
              Icon(
                imageVector = Icons.Default.LinkOff,
                contentDescription = "Release OpenAI key",
                tint = colors.coralAlert,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Release Key", color = colors.coralAlert, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      // 2. Supabase PostgREST Client & Ocean Surfer Schema
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supabase_integration_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = colors.cyan.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = colors.cyan,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Step 2: Shared Cloud Sync (Supabase)",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Keeps Phone 1 and Phone 2 in sync with real call logs & summaries",
                color = colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Live connection status banner
          when (val conn = supabaseConnState) {
            SupabaseConnectionState.Idle -> Unit
            SupabaseConnectionState.Testing -> {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = colors.turquoise, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pinging Supabase PostgREST...", color = colors.cyan, fontSize = 11.sp)
              }
            }
            is SupabaseConnectionState.Connected -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.seaGreen.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
              ) {
                Text(
                  text = "✓ ${conn.message} (${conn.latencyMs}ms)",
                  color = colors.seaGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
            is SupabaseConnectionState.Error -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.coralAlert.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
              ) {
                Text(
                  text = conn.message,
                  color = colors.goldenSun,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = supabaseUrlInput,
            onValueChange = { supabaseUrlInput = it },
            label = { Text("Supabase Project URL") },
            modifier = Modifier.fillMaxWidth().testTag("supabase_url_input"),
            singleLine = true,
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    val pasted = readClipboardText()
                    if (pasted != null) {
                      supabaseUrlInput = pasted
                      viewModel.showSnackbar("Supabase URL pasted from clipboard!")
                    } else {
                      viewModel.showSnackbar("Clipboard is empty.")
                    }
                  },
                  modifier = Modifier.testTag("paste_supabase_url_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Supabase URL",
                    tint = colors.turquoise
                  )
                }
                IconButton(
                  onClick = { writeClipboardText("Supabase URL", supabaseUrlInput) },
                  modifier = Modifier.testTag("copy_supabase_url_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Supabase URL",
                    tint = colors.cyan
                  )
                }
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = supabaseKeyInput,
            onValueChange = { supabaseKeyInput = it },
            label = { Text("Supabase Publishable / Anon Key") },
            placeholder = { Text("sb_publishable_...", color = colors.textSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier.fillMaxWidth().testTag("supabase_anon_key_input"),
            singleLine = true,
            visualTransformation = if (showSupabaseKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    val pasted = readClipboardText()
                    if (pasted != null) {
                      supabaseKeyInput = pasted
                      viewModel.showSnackbar("Supabase key pasted from clipboard!")
                    } else {
                      viewModel.showSnackbar("Clipboard is empty.")
                    }
                  },
                  modifier = Modifier.testTag("paste_supabase_key_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Supabase key",
                    tint = colors.turquoise
                  )
                }
                IconButton(onClick = { showSupabaseKey = !showSupabaseKey }) {
                  Icon(
                    imageVector = if (showSupabaseKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Supabase key visibility",
                    tint = colors.cyan
                  )
                }
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                viewModel.updateSupabaseConfig(supabaseUrlInput.trim(), supabaseKeyInput.trim())
                viewModel.testSupabaseConnection()
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("test_supabase_button")
            ) {
              Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.turquoise)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save & Ping", color = colors.turquoise, fontSize = 12.sp)
            }

            Button(
              onClick = { viewModel.fetchCallsFromSupabase() },
              enabled = !isFetchingSupabase,
              colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("sync_supabase_calls_button")
            ) {
              Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isFetchingSupabase) "Syncing..." else "Sync Calls", color = Color.White, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = { showSqlSchema = !showSqlSchema },
            border = BorderStroke(1.dp, colors.cyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("toggle_supabase_sql_button")
          ) {
            Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (showSqlSchema) "Hide Ocean Surfer SQL Schema" else "View Ocean Surfer SQL Schema (swell_call_logs)", color = colors.cyan, fontSize = 12.sp)
          }

          AnimatedVisibility(visible = showSqlSchema) {
            val sqlCode = remember { SupabaseSchemaGenerator.generateSql() }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(colors.abyss, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Supabase Ocean Surfer SQL DDL",
                  color = colors.goldenSun,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )

                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Supabase Schemas SQL", sqlCode)
                    clipboard.setPrimaryClip(clip)
                    viewModel.showSnackbar("Supabase SQL copied to clipboard!")
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy SQL", tint = colors.turquoise)
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              SelectionContainer {
                Text(
                  text = sqlCode,
                  color = colors.textSecondary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  lineHeight = 14.sp
                )
              }
            }
          }
        }
      }

      // 3. GitHub Pages Landing Page & Repository Section
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("github_integration_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = colors.goldenSun.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  tint = colors.goldenSun,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "GitHub Pages Landing Hub",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Repository ${gitHubConfig.repoOwner}/${gitHubConfig.repoName} (/docs/index.html)",
                color = colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.card,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Code,
                  contentDescription = null,
                  tint = colors.cyan,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${gitHubConfig.repoOwner}/${gitHubConfig.repoName}",
                  color = colors.textPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Branch: ${gitHubConfig.branch} • GitHub Pages /docs Landing Page Included",
                color = colors.textSecondary,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Production Site: ${gitHubConfig.websiteUrl}",
                color = colors.turquoise,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "GitHub URL: ${gitHubConfig.repoUrl}",
                color = colors.cyan,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Status: ${gitHubConfig.lastDeploymentStatus}",
                color = colors.seaGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = { viewModel.triggerGitHubWebsiteDeploy() },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
            ) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Sync", color = colors.cyan, fontSize = 11.sp)
            }

            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubConfig.repoUrl))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
            ) {
              Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Repo", color = colors.goldenSun, fontSize = 11.sp)
            }

            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubConfig.websiteUrl))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
            ) {
              Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Live Site", color = Color.White, fontSize = 11.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
fun OceanThemeSwitcherCard(
  activePalette: OceanPalette,
  onSelectPalette: (OceanPalette) -> Unit,
  onCyclePalette: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("theme_switcher_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
    border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.55f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = colors.turquoise.copy(alpha = 0.2f),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = "Ocean Theme Palette",
              tint = colors.turquoise,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Ocean Theme Switcher",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "Active: ${activePalette.displayName} • ${activePalette.tagline}",
            color = colors.turquoise,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2x2 Grid of Ocean Palette Cards with Color Swatches
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OceanPalette.entries.chunked(2).forEach { rowPalettes ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            rowPalettes.forEach { palette ->
              val isSelected = palette == activePalette
              val preview = palette.colors
              Card(
                onClick = { onSelectPalette(palette) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = preview.card),
                border = BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) preview.turquoise else preview.teal.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                  .weight(1f)
                  .defaultMinSize(minHeight = 84.dp)
                  .testTag("theme_option_${palette.name.lowercase()}")
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = palette.displayName,
                      color = preview.textPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "${palette.displayName} selected",
                        tint = preview.turquoise,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = palette.tagline,
                    color = preview.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  // Multi-color ocean swatch strip
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    listOf(
                      preview.abyss,
                      preview.turquoise,
                      preview.cyan,
                      preview.goldenSun,
                      preview.seaGreen
                    ).forEach { swatchColor ->
                      Box(
                        modifier = Modifier
                          .size(14.dp)
                          .clip(CircleShape)
                          .background(swatchColor)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = onCyclePalette,
        colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("cycle_theme_button")
      ) {
        Icon(
          imageVector = Icons.Default.Waves,
          contentDescription = null,
          tint = colors.abyss,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Toggle Next Ocean Wave (${activePalette.next().displayName})",
          color = colors.abyss,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  }
}
