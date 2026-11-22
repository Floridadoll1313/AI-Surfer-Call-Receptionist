package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.GeminiAiService
import com.example.ai.OpenAiClient
import com.example.integrations.SupabaseConnectionState
import com.example.integrations.SupabaseSchemaGenerator
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
import com.example.ui.theme.SurfCyan
import com.example.ui.theme.SurfTeal
import com.example.ui.theme.SurfTurquoise

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
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var apiKeyInput by remember(chatGptConfig.apiKey) { mutableStateOf(chatGptConfig.apiKey) }
  var selectedModel by remember(chatGptConfig.model) { mutableStateOf(chatGptConfig.model) }
  var showApiKey by remember { mutableStateOf(false) }

  var supabaseUrlInput by remember(supabaseConfig.projectUrl) { mutableStateOf(supabaseConfig.projectUrl) }
  var supabaseKeyInput by remember(supabaseConfig.anonKey) { mutableStateOf(supabaseConfig.anonKey) }
  var showSupabaseKey by remember { mutableStateOf(false) }
  var showSqlSchema by remember { mutableStateOf(false) }

  val isGeminiReady = remember { GeminiAiService.isFirebaseAvailable(context) }
  val isOpenAiSecretConfigured = remember { OpenAiClient.hasConfiguredBuildConfigKey() }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OceanAbyss)
      .verticalScroll(scrollState)
  ) {
    OceanWaveHeader(
      title = "AI & Cloud Integrations",
      subtitle = "Gemini AI Logic • ChatGPT Summaries • Supabase Client • GitHub Pages"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // 1. Dual AI Engine (Firebase AI Logic Gemini + ChatGPT Call Summaries)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("chatgpt_integration_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = SeaGreen.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = SeaGreen,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AI Capabilities: Gemini & ChatGPT",
                color = SandTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = if (chatGptConfig.apiKey.isNotBlank() || isOpenAiSecretConfigured) {
                  "OpenAI ChatGPT active • Capturing Topic, Actions & Sentiment"
                } else if (isGeminiReady) {
                  "Firebase AI Logic (gemini-3.8-flash) Active via App Check"
                } else {
                  "Built-in AI Surfer Summarizer Active"
                },
                color = SeaGreen,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = OceanCard,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Call Summarization Format: Main Topic • Action Items • Sentiment",
                color = SurfTurquoise,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Gemini 3.8 Flash handles real-time Client Hub streaming & structured call triage when no OpenAI key is set.",
                color = SandTextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "OpenAI ChatGPT API Key (Optional override):",
            color = SandTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )

          Spacer(modifier = Modifier.height(4.dp))

          OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text("sk-proj-...", color = SandTextSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("chatgpt_api_key_input"),
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showApiKey = !showApiKey }) {
                Icon(
                  imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle API key visibility",
                  tint = SurfCyan
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "ChatGPT Model Selection:",
            color = SandTextSecondary,
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
                  selectedContainerColor = SurfTurquoise,
                  selectedLabelColor = OceanAbyss,
                  containerColor = OceanCard,
                  labelColor = SandTextPrimary
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              viewModel.updateChatGptConfig(apiKeyInput.trim(), selectedModel)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("save_chatgpt_button")
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save AI Settings", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 2. Supabase PostgREST Client & Ocean Surfer Schema
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supabase_integration_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = SurfCyan.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = SurfCyan,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Supabase PostgREST Client & Schema",
                color = SandTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Logs caller_id, timestamp, duration & ChatGPT summary",
                color = SurfCyan,
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
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SurfTurquoise, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pinging Supabase PostgREST...", color = SurfCyan, fontSize = 11.sp)
              }
            }
            is SupabaseConnectionState.Connected -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SeaGreen.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
              ) {
                Text(
                  text = "✓ ${conn.message} (${conn.latencyMs}ms)",
                  color = SeaGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(8.dp)
                )
              }
            }
            is SupabaseConnectionState.Error -> {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralAlert.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
              ) {
                Text(
                  text = conn.message,
                  color = GoldenSun,
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
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = supabaseKeyInput,
            onValueChange = { supabaseKeyInput = it },
            label = { Text("Supabase Anon Key") },
            modifier = Modifier.fillMaxWidth().testTag("supabase_anon_key_input"),
            singleLine = true,
            visualTransformation = if (showSupabaseKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showSupabaseKey = !showSupabaseKey }) {
                Icon(
                  imageVector = if (showSupabaseKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle Supabase key visibility",
                  tint = SurfCyan
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SurfTurquoise,
              unfocusedBorderColor = SurfTeal,
              focusedTextColor = SandTextPrimary,
              unfocusedTextColor = SandTextPrimary
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
              colors = ButtonDefaults.buttonColors(containerColor = OceanCardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("test_supabase_button")
            ) {
              Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(15.dp), tint = SurfTurquoise)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save & Ping", color = SurfTurquoise, fontSize = 12.sp)
            }

            Button(
              onClick = { viewModel.fetchCallsFromSupabase() },
              enabled = !isFetchingSupabase,
              colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("sync_supabase_calls_button")
            ) {
              Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isFetchingSupabase) "Syncing..." else "Sync Calls", color = Color.White, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = { showSqlSchema = !showSqlSchema },
            border = BorderStroke(1.dp, SurfCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("toggle_supabase_sql_button")
          ) {
            Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = SurfCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (showSqlSchema) "Hide Ocean Surfer SQL Schema" else "View Ocean Surfer SQL Schema (swell_call_logs)", color = SurfCyan, fontSize = 12.sp)
          }

          AnimatedVisibility(visible = showSqlSchema) {
            val sqlCode = remember { SupabaseSchemaGenerator.generateSql() }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(OceanAbyss, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Supabase Ocean Surfer SQL DDL",
                  color = GoldenSun,
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
                  Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy SQL", tint = SurfTurquoise)
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = sqlCode,
                color = SandTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp
              )
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
        colors = CardDefaults.cardColors(containerColor = OceanDeepBlue),
        border = BorderStroke(1.dp, SurfTeal.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = GoldenSun.copy(alpha = 0.2f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  tint = GoldenSun,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "GitHub Pages Landing Hub",
                color = SandTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "Repository ${gitHubConfig.repoOwner}/${gitHubConfig.repoName} (/docs/index.html)",
                color = SurfCyan,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = OceanCard,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Code,
                  contentDescription = null,
                  tint = SurfCyan,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${gitHubConfig.repoOwner}/${gitHubConfig.repoName}",
                  color = SandTextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Branch: ${gitHubConfig.branch} • GitHub Pages /docs Landing Page Included",
                color = SandTextSecondary,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Production Site: ${gitHubConfig.websiteUrl}",
                color = SurfTurquoise,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "GitHub URL: ${gitHubConfig.repoUrl}",
                color = SurfCyan,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Status: ${gitHubConfig.lastDeploymentStatus}",
                color = SeaGreen,
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
              colors = ButtonDefaults.buttonColors(containerColor = OceanCardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Sync", color = SurfCyan, fontSize = 11.sp)
            }

            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubConfig.repoUrl))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = OceanCardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Repo", color = GoldenSun, fontSize = 11.sp)
            }

            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubConfig.websiteUrl))
                try { context.startActivity(intent) } catch (_: Exception) {}
              },
              colors = ButtonDefaults.buttonColors(containerColor = SurfTeal),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
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
