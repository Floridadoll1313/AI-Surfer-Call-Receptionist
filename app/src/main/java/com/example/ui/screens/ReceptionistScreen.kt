package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ReceptionistResult
import com.example.ai.SurferAiReceptionist
import com.example.integrations.EscalationAlertChannel
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.OceanWaveHeader
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.OceanAbyss
import com.example.ui.theme.OceanCard
import com.example.ui.theme.OceanCardSecondary
import com.example.ui.theme.OceanDeepBlue
import com.example.ui.theme.SandTextPrimary
import com.example.ui.theme.SandTextSecondary
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfCyan
import com.example.ui.theme.SurfTeal
import com.example.ui.theme.SurfTurquoise
import kotlinx.coroutines.launch

@Composable
fun ReceptionistScreen(
  viewModel: OceanSurferViewModel,
  onNavigateToCalls: () -> Unit,
  modifier: Modifier = Modifier
) {
  val pendingCalls by viewModel.pendingCalls.collectAsStateWithLifecycle()
  val products by viewModel.products.collectAsStateWithLifecycle()
  val faqs by viewModel.faqs.collectAsStateWithLifecycle()
  val chatGptConfig by viewModel.chatGptConfig.collectAsStateWithLifecycle()
  val escalationTriggerConfig by viewModel.escalationTriggerConfig.collectAsStateWithLifecycle()

  var testQuestion by remember { mutableStateOf("") }
  var testResult by remember { mutableStateOf<ReceptionistResult?>(null) }
  var isAnsweringTest by remember { mutableStateOf(false) }
  var showCustomCallDialog by remember { mutableStateOf(false) }

  var customCallerName by remember { mutableStateOf("") }
  var customCallerPhone by remember { mutableStateOf("") }
  var customCallerQuery by remember { mutableStateOf("") }
  var alertEmailInput by remember(escalationTriggerConfig.targetEmail) {
    mutableStateOf(escalationTriggerConfig.targetEmail)
  }
  var webhookUrlInput by remember(escalationTriggerConfig.webhookUrl) {
    mutableStateOf(escalationTriggerConfig.webhookUrl)
  }

  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OceanAbyss)
      .verticalScroll(scrollState)
  ) {
    OceanWaveHeader(
      title = "Ocean Tide Drop AI Surfer",
      subtitle = "Intelligent AI Receptionist & Cell Call Guard"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Receptionist Status Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("receptionist_status_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfTeal.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = SeaGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Surfing,
                contentDescription = "Active Surfer AI",
                tint = SeaGreen,
                modifier = Modifier.size(28.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(SeaGreen, CircleShape)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "AI RECEPTIONIST ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = SeaGreen,
                fontWeight = FontWeight.ExtraBold
              )
            }
            Text(
              text = "Guarding Incoming Cell Calls",
              style = MaterialTheme.typography.titleMedium,
              color = SandTextPrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Escalates unreviewed calls to oceantidedropservice@gmail.com",
              style = MaterialTheme.typography.bodySmall,
              color = SandTextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      // Pending Calls Alert Notice if any
      if (pendingCalls.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          onClick = onNavigateToCalls,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_alert_banner"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SunsetOrange.copy(alpha = 0.15f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, SunsetOrange)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Warning",
              tint = SunsetOrange,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "⚠️ ${pendingCalls.size} Unreviewed Incoming Call(s)",
                color = GoldenSun,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = "SLA timers running. Tap to review before auto-escalation.",
                color = SandTextPrimary,
                fontSize = 11.sp
              )
            }
            Text(
              text = "View",
              color = GoldenSun,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section: AI Escalation Alert Trigger Controls (Email / Notification)
      Text(
        text = "AI ESCALATION ALERT TRIGGERS",
        style = MaterialTheme.typography.labelMedium,
        color = CoralAlert,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Automatically notify you via Push Notification or Email whenever AI flags a call for escalation.",
        style = MaterialTheme.typography.bodySmall,
        color = SandTextSecondary,
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ai_escalation_trigger_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (escalationTriggerConfig.isEnabled) CoralAlert.copy(alpha = 0.6f) else SurfTeal.copy(alpha = 0.35f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = CoralAlert.copy(alpha = 0.18f),
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = "Escalation Trigger",
                  tint = CoralAlert,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Auto-Trigger on AI Escalation",
                color = SandTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = if (escalationTriggerConfig.isEnabled) "Active: ${escalationTriggerConfig.alertChannel.label}" else "Disabled",
                color = if (escalationTriggerConfig.isEnabled) SeaGreen else SandTextSecondary,
                fontSize = 11.sp
              )
            }
            Switch(
              checked = escalationTriggerConfig.isEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateEscalationTriggerConfig(isEnabled = enabled)
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SeaGreen
              ),
              modifier = Modifier.testTag("escalation_trigger_switch")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Alert Delivery Channel:",
            color = SandTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            EscalationAlertChannel.values().forEach { channel ->
              val selected = escalationTriggerConfig.alertChannel == channel
              FilterChip(
                selected = selected,
                onClick = {
                  viewModel.updateEscalationTriggerConfig(alertChannel = channel)
                },
                label = { Text(channel.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CoralAlert,
                  selectedLabelColor = Color.White,
                  containerColor = OceanCard,
                  labelColor = SandTextPrimary
                ),
                modifier = Modifier.testTag("channel_chip_${channel.name.lowercase()}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = alertEmailInput,
              onValueChange = { alertEmailInput = it },
              label = { Text("Escalation Alert Email", fontSize = 11.sp) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null,
                  tint = SurfCyan,
                  modifier = Modifier.size(16.dp)
                )
              },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("escalation_email_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurfTurquoise,
                unfocusedBorderColor = SurfTeal,
                focusedTextColor = SandTextPrimary,
                unfocusedTextColor = SandTextPrimary
              )
            )
            Button(
              onClick = {
                viewModel.updateEscalationTriggerConfig(targetEmail = alertEmailInput)
              },
              colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("save_escalation_email_button")
            ) {
              Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = webhookUrlInput,
              onValueChange = { webhookUrlInput = it },
              label = { Text("Escalation Webhook / Slack URL (Optional)", fontSize = 11.sp) },
              placeholder = { Text("https://hooks.slack.com/services/...", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("escalation_webhook_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurfTurquoise,
                unfocusedBorderColor = SurfTeal,
                focusedTextColor = SandTextPrimary,
                unfocusedTextColor = SandTextPrimary
              )
            )
            Button(
              onClick = {
                viewModel.updateEscalationTriggerConfig(webhookUrl = webhookUrlInput)
              },
              colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("save_escalation_webhook_button")
            ) {
              Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              viewModel.triggerTestCallAlert()
              onNavigateToCalls()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("trigger_ai_escalation_demo_button")
          ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trigger AI Escalation Alert Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Test Incoming Cell Call Simulation
      Text(
        text = "SIMULATE INCOMING CELL PHONE CALL",
        style = MaterialTheme.typography.labelMedium,
        color = SurfCyan,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Test how the AI Receptionist intercepts, answers customer questions, and logs transcripts.",
        style = MaterialTheme.typography.bodySmall,
        color = SandTextSecondary,
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Preset 1: Sarah Jenkins - Free AI Wave Check
          PresetCallRow(
            caller = "Sarah Jenkins (+1 415-555-WAVE)",
            topic = "Free AI Wave Check™ & Diagnostic Audit",
            onClick = {
              viewModel.simulateIncomingCall(
                callerName = "Sarah Jenkins",
                callerNumber = "+1 (415) 555-WAVE",
                initialQuery = "Aloha! Can I get a free AI Wave Check audit for my business? Where do I take the 5-question diagnostic?"
              )
            }
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Preset 2: Marcus Vance - Wave Starter $497 Sprint
          PresetCallRow(
            caller = "Marcus Vance (+1 206-555-RIDE)",
            topic = "Wave Starter™ ($497) AI Lead Sprint inquiry",
            onClick = {
              viewModel.simulateIncomingCall(
                callerName = "Marcus Vance",
                callerNumber = "+1 (206) 555-RIDE",
                initialQuery = "What is included in the $497 Wave Starter sprint? Can we use Wave Scout for automated outbound lead generation?"
              )
            }
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Preset 3: David Chen - Big Kahuna & Connected Repo
          PresetCallRow(
            caller = "David Chen (+1 212-555-DROP)",
            topic = "Big Kahuna™ Transformation & otdaisurfer/AISurfer-",
            onClick = {
              viewModel.simulateIncomingCall(
                callerName = "David Chen",
                callerNumber = "+1 (212) 555-DROP",
                initialQuery = "Aloha! We are looking for full enterprise AI transformation with the Big Kahuna suite. Also, what repository powers the otdaisurfer.surf platform?"
              )
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { showCustomCallDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_call_button")
          ) {
            Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = "Custom Call")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Custom Incoming Call", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Section: Ask Kai Receptionist Live Test
      Text(
        text = "DIRECT AI RECEPTIONIST TEST CONSOLE",
        style = MaterialTheme.typography.labelMedium,
        color = SurfCyan,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Ask any business or product question to see how the AI receptionist responds.",
        style = MaterialTheme.typography.bodySmall,
        color = SandTextSecondary,
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
      )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          OutlinedTextField(
            value = testQuestion,
            onValueChange = { testQuestion = it },
            placeholder = { Text("e.g. How much is Wave Starter? What is the Free Wave Check? Where is the GitHub repo?", color = SandTextSecondary, fontSize = 12.sp) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("receptionist_test_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            ),
            trailingIcon = {
              IconButton(
                onClick = {
                  if (testQuestion.isNotBlank() && !isAnsweringTest) {
                    isAnsweringTest = true
                    coroutineScope.launch {
                      val result = SurferAiReceptionist.processCallerQuestion(
                        callerName = "Test Inquirer",
                        callerNumber = "+1 (555) 000-TEST",
                        question = testQuestion,
                        products = products,
                        faqs = faqs,
                        openAiApiKey = chatGptConfig.apiKey,
                        modelName = chatGptConfig.model
                      )
                      testResult = result
                      isAnsweringTest = false
                    }
                  }
                },
                modifier = Modifier.testTag("receptionist_test_submit")
              ) {
                if (isAnsweringTest) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SurfTurquoise)
                } else {
                  Icon(imageVector = Icons.Default.Send, contentDescription = "Submit", tint = SurfTurquoise)
                }
              }
            }
          )

          testResult?.let { result ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = OceanCard,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Surfing,
                    contentDescription = null,
                    tint = SurfTurquoise,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Kai's AI Answer:",
                    color = SurfTurquoise,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                  Spacer(modifier = Modifier.weight(1f))
                  if (result.requiresImmediateEscalation) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = CoralAlert
                    ) {
                      Text(
                        text = "AI ESCALATION FLAG",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = result.responseText,
                  color = SandTextPrimary,
                  fontSize = 13.sp,
                  lineHeight = 18.sp
                )

                if (result.requiresImmediateEscalation) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Button(
                    onClick = {
                      viewModel.triggerAiEscalationForQuestion(
                        callerName = "Console Escalation Test",
                        callerNumber = "+1 (555) 000-TEST",
                        question = testQuestion
                      )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("dispatch_console_escalation_button")
                  ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Dispatch ${escalationTriggerConfig.alertChannel.label}",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Custom Call Dialog
  if (showCustomCallDialog) {
    AlertDialog(
      onDismissRequest = { showCustomCallDialog = false },
      containerColor = OceanDeepBlue,
      titleContentColor = SandTextPrimary,
      title = {
        Text("Simulate Custom Phone Call", fontWeight = FontWeight.Bold)
      },
      text = {
        Column {
          OutlinedTextField(
            value = customCallerName,
            onValueChange = { customCallerName = it },
            label = { Text("Caller Name") },
            placeholder = { Text("e.g. John Swell") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customCallerPhone,
            onValueChange = { customCallerPhone = it },
            label = { Text("Caller Phone") },
            placeholder = { Text("+1 (555) 987-6543") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customCallerQuery,
            onValueChange = { customCallerQuery = it },
            label = { Text("Customer Question") },
            placeholder = { Text("What question will they ask the AI receptionist?") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.simulateIncomingCall(
              callerName = customCallerName.ifBlank { "Custom Caller" },
              callerNumber = customCallerPhone.ifBlank { "+1 (555) 012-3456" },
              initialQuery = customCallerQuery.ifBlank { "Aloha! What are your surf packages and prices?" }
            )
            showCustomCallDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SurfTurquoise)
        ) {
          Text("Call Now", color = OceanAbyss, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomCallDialog = false }) {
          Text("Cancel", color = SandTextSecondary)
        }
      }
    )
  }
}

@Composable
fun PresetCallRow(
  caller: String,
  topic: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = OceanCard,
    onClick = onClick,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = SurfCyan.copy(alpha = 0.2f),
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Incoming Call",
            tint = SurfCyan,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = caller,
          color = SandTextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
        Text(
          text = topic,
          color = SandTextSecondary,
          fontSize = 11.sp
        )
      }

      Text(
        text = "Dial In",
        color = SurfTurquoise,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
