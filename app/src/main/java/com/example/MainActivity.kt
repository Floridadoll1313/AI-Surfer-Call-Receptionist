package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.auth.GoogleSignInManager
import com.example.data.AppRepository
import com.example.telephony.CallEscalationManager
import com.example.ui.OceanSurferViewModel
import com.example.ui.OceanSurferViewModelFactory
import com.example.ui.components.CallAlertBanner
import com.example.ui.components.CallStatusFilter
import com.example.ui.components.OceanStatusBar
import com.example.ui.input.DuplicateKeyEventFilter
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ClientHubScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReceptionistScreen
import com.example.ui.theme.OceanTheme
import com.example.ui.theme.ThemeProvider

class MainActivity : ComponentActivity() {

  private val notificationTabState = mutableStateOf<Int?>(null)
  private val keyEventFilter = DuplicateKeyEventFilter()

  private val viewModel: OceanSurferViewModel by viewModels {
    val app = application as OceanSurferApp
    val repository = AppRepository(app.database)
    OceanSurferViewModelFactory(repository, applicationContext)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()

    val initialTab = if (intent?.getStringExtra("NAVIGATE_TO") == "calls") 1 else 0

    setContent {
      val selectedPalette by viewModel.selectedPalette.collectAsStateWithLifecycle()
      ThemeProvider(
        palette = selectedPalette,
        onSelectPalette = { palette -> viewModel.selectOceanPalette(palette) }
      ) {
        OceanSurferMainContent(
          viewModel = viewModel,
          initialTab = initialTab,
          requestedTab = notificationTabState.value,
          onConsumedRequestedTab = { notificationTabState.value = null }
        )
      }
    }
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (keyEventFilter.shouldSuppressDuplicateKeyEvent(event, SystemClock.uptimeMillis())) {
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getStringExtra("NAVIGATE_TO") == "calls") {
      notificationTabState.value = 1
    }
  }
}

@Composable
fun OceanSurferMainContent(
  viewModel: OceanSurferViewModel,
  initialTab: Int = 0,
  requestedTab: Int? = null,
  onConsumedRequestedTab: () -> Unit = {}
) {
  val colors = OceanTheme.colors
  var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
  LaunchedEffect(requestedTab) {
    requestedTab?.let {
      selectedTab = it
      onConsumedRequestedTab()
    }
  }
  val allCalls by viewModel.calls.collectAsStateWithLifecycle()
  val pendingCalls by viewModel.pendingCalls.collectAsStateWithLifecycle()
  val activeCallAlert by viewModel.activeCallAlert.collectAsStateWithLifecycle()
  val escalationTriggerConfig by viewModel.escalationTriggerConfig.collectAsStateWithLifecycle()
  val businessPhonesConfig by viewModel.businessPhonesConfig.collectAsStateWithLifecycle()
  val googleAccountState by viewModel.googleAccountState.collectAsStateWithLifecycle()
  val pendingEmailEscalation by viewModel.pendingEmailEscalation.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  val escalatedCount = remember(allCalls) { allCalls.count { it.status == "ESCALATED" || it.isEscalated } }
  val reviewedCount = remember(allCalls) { allCalls.count { it.status == "REVIEWED" } }

  // Permissions launcher for Android 13+ Notifications & Telephony
  val permissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ -> }

  LaunchedEffect(Unit) {
    GoogleSignInManager.attemptAutoSignIn(
      context = context,
      onAuthSuccess = { user -> viewModel.onGoogleAuthSuccess(user, showSnackbar = false) },
      onUnauthenticated = { },
      scope = this
    )

    val permissionsToRequest = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
    }

    if (permissionsToRequest.isNotEmpty()) {
      permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }
  }

  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearSnackbar()
    }
  }

  LaunchedEffect(pendingEmailEscalation) {
    pendingEmailEscalation?.let { call ->
      val emailIntent = CallEscalationManager.createEmailEscalationIntent(
        call = call,
        targetEmail = escalationTriggerConfig.targetEmail
      )
      try {
        context.startActivity(Intent.createChooser(emailIntent, "Send AI Escalation Email"))
      } catch (_: Exception) {
        viewModel.showSnackbar("No email app found. Alert email prepared for ${escalationTriggerConfig.targetEmail}.")
      }
      viewModel.consumePendingEmailEscalation()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.abyss),
      containerColor = colors.abyss,
      topBar = {
        OceanStatusBar(
          isOnline = true,
          pendingCount = pendingCalls.size,
          escalatedCount = escalatedCount,
          reviewedCount = reviewedCount,
          activeDeviceLineLabel = businessPhonesConfig.activeDeviceLine.shortLabel,
          triggerChannelLabel = if (escalationTriggerConfig.isEnabled) escalationTriggerConfig.alertChannel.shortTag else "PAUSED",
          targetEmail = escalationTriggerConfig.targetEmail,
          signedInGoogleEmail = googleAccountState.email.takeIf { googleAccountState.isSignedIn },
          onOpenAlerts = {
            selectedTab = 1
            viewModel.setStatusFilter(CallStatusFilter.ESCALATED)
          },
          onOpenPhoneSetup = {
            selectedTab = 4
          }
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        NavigationBar(
          containerColor = colors.deepBlue,
          contentColor = colors.cyan
        ) {
          val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.abyss,
            selectedTextColor = colors.turquoise,
            indicatorColor = colors.turquoise,
            unselectedIconColor = colors.textSecondary,
            unselectedTextColor = colors.textSecondary
          )

          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = {
              Icon(
                imageVector = Icons.Default.Surfing,
                contentDescription = "Receptionist",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("Receptionist", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_receptionist")
          )

          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = {
              BadgedBox(
                badge = {
                  val alertBadgeCount = escalatedCount + pendingCalls.size
                  if (alertBadgeCount > 0) {
                    Badge(containerColor = colors.coralAlert, contentColor = Color.White) {
                      Text(alertBadgeCount.toString())
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.PhoneInTalk,
                  contentDescription = "Call Dashboard",
                  modifier = Modifier.size(24.dp)
                )
              }
            },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_calls")
          )

          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Client AI",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("Client AI", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_client_hub")
          )

          NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            icon = {
              Icon(
                imageVector = Icons.Default.Sell,
                contentDescription = "Products",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("Products", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_products")
          )

          NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { selectedTab = 4 },
            icon = {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings & Integrations",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_integrations")
          )
        }
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        CallAlertBanner(
          callAlert = activeCallAlert,
          targetAlertEmail = escalationTriggerConfig.targetEmail,
          onDismiss = { viewModel.dismissCallAlert() },
          onMarkReviewed = { callId -> viewModel.markCallReviewed(callId) }
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          when (selectedTab) {
            0 -> ReceptionistScreen(
              viewModel = viewModel,
              onNavigateToCalls = { selectedTab = 1 },
              onOpenPhoneSetup = { selectedTab = 4 },
              onOpenAiStudio = { selectedTab = 2 }
            )
            1 -> CallsScreen(viewModel = viewModel)
            2 -> ClientHubScreen(viewModel = viewModel)
            3 -> ProductsScreen(viewModel = viewModel)
            4 -> IntegrationsScreen(viewModel = viewModel)
          }
        }
      }
    }
  }
}
