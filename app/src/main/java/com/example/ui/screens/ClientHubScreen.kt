package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiAdvisorUiState
import com.example.ui.OceanSurferViewModel
import com.example.ui.components.OceanWaveHeader
import com.example.ui.theme.OceanTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientHubScreen(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val scrollState = rememberScrollState()
  val advisorState by viewModel.geminiAdvisorState.collectAsStateWithLifecycle()
  val clientLeads by viewModel.clientLeads.collectAsStateWithLifecycle()

  var advisorInput by remember { mutableStateOf("") }
  var selectedBottleneck by remember { mutableStateOf("Slow Lead Follow-Up") }

  // Wave Check Client Intake State
  var businessName by remember { mutableStateOf("") }
  var contactName by remember { mutableStateOf("") }
  var contactEmail by remember { mutableStateOf("") }
  var contactPhone by remember { mutableStateOf("") }

  val bottlenecks = remember {
    listOf(
      "Slow Lead Follow-Up",
      "Missed Sales Calls",
      "Inconsistent Content",
      "AI Search / AEO Visibility",
      "Multi-Agent Enterprise Ops"
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.abyss)
      .verticalScroll(scrollState)
      .testTag("client_hub_screen")
  ) {
    OceanWaveHeader(
      title = "Client AI Portal",
      subtitle = "Gemini AI Wave Advisor • Free Wave Check™ • Supabase Leads"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Live Gemini AI Wave Advisor Card (Streaming Firebase AI Logic)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("client_ai_advisor_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = colors.turquoise.copy(alpha = 0.18f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = "Gemini AI Wave Advisor",
                  tint = colors.turquoise,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Gemini AI Wave Advisor",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
              Text(
                text = "Powered by Firebase AI Logic (gemini-3.8-flash)",
                color = colors.seaGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Primary Growth Bottleneck:",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            bottlenecks.forEach { item ->
              FilterChip(
                selected = selectedBottleneck == item,
                onClick = { selectedBottleneck = item },
                label = { Text(item, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.turquoise,
                  selectedLabelColor = colors.abyss,
                  containerColor = colors.card,
                  labelColor = colors.textPrimary
                ),
                modifier = Modifier.testTag("bottleneck_chip_${item.lowercase().replace(' ', '_')}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = advisorInput,
            onValueChange = { advisorInput = it },
            label = { Text("Describe your business or AI goal") },
            placeholder = { Text("e.g. We miss 40% of inbound service calls and need 24/7 lead qualification") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("client_advisor_input"),
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = { viewModel.askGeminiWaveAdvisor(advisorInput, selectedBottleneck) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("ask_gemini_advisor_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Generate AI Plan",
              tint = colors.abyss,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate AI Strategy Plan", color = colors.abyss, fontWeight = FontWeight.Bold)
          }

          // Advisor Output State
          when (val state = advisorState) {
            GeminiAdvisorUiState.Idle -> Unit
            GeminiAdvisorUiState.Loading -> {
              Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = colors.turquoise,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing with Gemini 3.8 Flash...", color = colors.cyan, fontSize = 12.sp)
              }
            }
            is GeminiAdvisorUiState.Streaming -> {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.card,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("LIVE GEMINI STREAM", color = colors.turquoise, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(state.text, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                }
              }
            }
            is GeminiAdvisorUiState.Success -> {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.seaGreen.copy(alpha = 0.6f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp)
                  .testTag("client_advisor_response")
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.seaGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Recommended: ${state.recommendedProduct}",
                      color = colors.seaGreen,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(state.text, color = colors.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                }
              }
            }
            is GeminiAdvisorUiState.Error -> {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.coralAlert.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, colors.coralAlert),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp)
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Warning, contentDescription = "AI Error", tint = colors.coralAlert, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(state.message, color = colors.textPrimary, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      // 2. Client Wave Check Lead Intake -> Supabase (public.wave_audit_leads)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("client_wave_check_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
        border = BorderStroke(1.dp, colors.teal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Surfing, contentDescription = null, tint = colors.goldenSun, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Free AI Wave Check™ Client Intake",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Syncs directly to Supabase wave_audit_leads & Call Queue",
                color = colors.cyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("client_business_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = contactName,
              onValueChange = { contactName = it },
              label = { Text("Your Name") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("client_name_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            OutlinedTextField(
              value = contactPhone,
              onValueChange = { contactPhone = it },
              label = { Text("Phone") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("client_phone_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
          }
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = contactEmail,
            onValueChange = { contactEmail = it },
            label = { Text("Work Email *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("client_email_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = colors.turquoise,
              unfocusedBorderColor = colors.teal,
              focusedTextColor = colors.textPrimary,
              unfocusedTextColor = colors.textPrimary
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              if (contactEmail.isNotBlank()) {
                viewModel.submitClientWaveCheck(
                  businessName = businessName,
                  contactName = contactName,
                  contactEmail = contactEmail,
                  contactPhone = contactPhone,
                  biggestBottleneck = selectedBottleneck
                )
              } else {
                viewModel.showSnackbar("Please enter your work email to submit Wave Check.")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("submit_wave_check_button")
          ) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save lead on device", color = colors.abyss, fontWeight = FontWeight.Bold)
          }

          AnimatedVisibility(visible = clientLeads.isNotEmpty()) {
            Column(
              modifier = Modifier.padding(top = 12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("Submitted Client Wave Checks (${clientLeads.size})", color = colors.goldenSun, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              clientLeads.forEach { lead ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = colors.card,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("${lead.contactName} • ${lead.businessName}", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Text(
                        text = if (lead.syncedToSupabase) "SUPABASE SYNCED" else "LOCAL",
                        color = if (lead.syncedToSupabase) colors.seaGreen else colors.goldenSun,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    if (!lead.syncedToSupabase) {
                      var confirm by remember(lead.id) { mutableStateOf(false) }
                      OutlinedButton(onClick = { confirm = true }) { Text("Submit live lead") }
                      if (confirm) {
                        var ownerEmail by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        androidx.compose.material3.AlertDialog(
                          onDismissRequest = { confirm = false },
                          title = { Text("Confirm live lead") },
                          text = {
                            Column {
                              Text("Send ${lead.contactEmail} to AI SURFER as a real lead?")
                              OutlinedTextField(ownerEmail, { ownerEmail = it }, singleLine = true,
                                label = { Text("Owner sign-in email") })
                              OutlinedTextField(password, { password = it }, singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                                label = { Text("Owner password") })
                            }
                          },
                          confirmButton = {
                            Button(enabled = ownerEmail.isNotBlank() && password.isNotEmpty(), onClick = {
                              viewModel.submitConfirmedLead(lead, ownerEmail, password)
                              confirm = false
                            }) { Text("Send live lead") }
                          },
                          dismissButton = { OutlinedButton(onClick = { confirm = false }) { Text("Cancel") } }
                        )
                      }
                    }
                    Text("Recommended: ${lead.recommendedProduct}", color = colors.cyan, fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }
      }

      // 3. Direct Client Booking & Website Actions
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSecondary)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Quick Client Actions", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://calendly.com/oceantidedrop/new-meeting")))
                } catch (_: Exception) {}
              },
              border = BorderStroke(1.dp, colors.turquoise),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("client_calendly_button")
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Book Strategy Call", tint = colors.turquoise, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Book Call", color = colors.turquoise, fontSize = 12.sp)
            }
            Button(
              onClick = {
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b")))
                } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.goldenSun),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("client_stripe_button")
            ) {
              Icon(Icons.Default.CreditCard, contentDescription = "Wave Starter Checkout", tint = colors.abyss, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("$497 Sprint", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
          OutlinedButton(
            onClick = {
              try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://otdaisurfer.surf")))
              } catch (_: Exception) {}
            },
            border = BorderStroke(1.dp, colors.cyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("client_open_website_button")
          ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open otdaisurfer.surf", tint = colors.cyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Visit https://otdaisurfer.surf", color = colors.cyan, fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
