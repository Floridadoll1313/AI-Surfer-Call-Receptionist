package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiInstrumentTool
import com.example.ai.CallQueueBriefUiState
import com.example.ai.ImageGenUiState
import com.example.ai.InstrumentTextUiState
import com.example.ai.PitchBriefUiState
import com.example.ui.OceanSurferViewModel
import com.example.ui.theme.OceanTheme

@Composable
fun AiInstrumentsSelectorRow(
  selectedTool: AiInstrumentTool,
  onSelectTool: (AiInstrumentTool) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors
  Column(modifier = modifier.fillMaxWidth().testTag("ai_instruments_selector")) {
    Text(
      text = "AI INSTRUMENTS STUDIO (7 TOOLS • GEMINI + NANO BANANA)",
      color = colors.cyan,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 0.8.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AiInstrumentTool.entries.forEach { tool ->
        val selected = tool == selectedTool
        FilterChip(
          selected = selected,
          onClick = { onSelectTool(tool) },
          leadingIcon = {
            Icon(
              imageVector = toolIcon(tool),
              contentDescription = tool.title,
              tint = if (selected) colors.abyss else colors.turquoise,
              modifier = Modifier.size(16.dp)
            )
          },
          label = {
            Text(
              text = "${tool.title} • ${tool.badge}",
              fontSize = 11.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.turquoise,
            selectedLabelColor = colors.abyss,
            containerColor = colors.card,
            labelColor = colors.textPrimary
          ),
          modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .testTag("ai_tool_chip_${tool.name.lowercase()}")
        )
      }
    }
  }
}

@Composable
fun AiInstrumentsStudio(
  selectedTool: AiInstrumentTool,
  viewModel: OceanSurferViewModel,
  selectedBottleneck: String,
  modifier: Modifier = Modifier
) {
  when (selectedTool) {
    AiInstrumentTool.WAVE_ADVISOR -> Unit
    AiInstrumentTool.COPILOT_CHAT -> CopilotChatInstrumentCard(viewModel, modifier)
    AiInstrumentTool.VISION_SCANNER -> VisionScannerInstrumentCard(viewModel, modifier)
    AiInstrumentTool.IMAGE_STUDIO -> ImageStudioInstrumentCard(viewModel, modifier)
    AiInstrumentTool.PITCH_BUILDER -> PitchBuilderInstrumentCard(viewModel, selectedBottleneck, modifier)
    AiInstrumentTool.CALL_ANALYST -> CallAnalystInstrumentCard(viewModel, modifier)
    AiInstrumentTool.TRANSLATOR_VOICE -> TranslatorVoiceInstrumentCard(viewModel, modifier)
  }
}

@Composable
private fun CopilotChatInstrumentCard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors
  val messages by viewModel.copilotMessages.collectAsStateWithLifecycle()
  val isSending by viewModel.isCopilotSending.collectAsStateWithLifecycle()
  val errorMsg by viewModel.copilotError.collectAsStateWithLifecycle()
  var input by remember { mutableStateOf("") }

  InstrumentCardShell(
    tool = AiInstrumentTool.COPILOT_CHAT,
    tag = "copilot_chat_card",
    modifier = modifier
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      OutlinedButton(
        onClick = { viewModel.resetCopilotChat() },
        border = BorderStroke(1.dp, colors.cyan),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("copilot_reset_button")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "New Chat Session", tint = colors.cyan, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("New Chat", color = colors.cyan, fontSize = 11.sp)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      messages.takeLast(8).forEach { msg ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (msg.isFromUser) colors.teal.copy(alpha = 0.28f) else colors.card,
          border = BorderStroke(1.dp, if (msg.isFromUser) colors.turquoise.copy(alpha = 0.5f) else colors.teal.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = if (msg.isFromUser) "YOU" else "KAI COPILOT",
              color = if (msg.isFromUser) colors.turquoise else colors.seaGreen,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = msg.text, color = colors.textPrimary, fontSize = 12.sp, lineHeight = 18.sp)
          }
        }
      }
    }
    if (!errorMsg.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      InstrumentErrorBanner(message = errorMsg.orEmpty())
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("Ask Kai Copilot...", fontSize = 11.sp) },
        singleLine = false,
        maxLines = 3,
        modifier = Modifier.weight(1f).testTag("copilot_chat_input"),
        colors = instrumentFieldColors()
      )
      Button(
        onClick = {
          val msg = input.trim()
          if (msg.isNotBlank()) {
            viewModel.sendCopilotMessage(msg)
            input = ""
          }
        },
        enabled = !isSending,
        colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("copilot_send_button")
      ) {
        if (isSending) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.abyss, strokeWidth = 2.dp)
        } else {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Copilot Message", tint = colors.abyss, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun VisionScannerInstrumentCard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val state by viewModel.visionResultState.collectAsStateWithLifecycle()
  var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var instruction by remember { mutableStateOf("") }

  val picker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      context.contentResolver.openInputStream(it)?.use { stream ->
        selectedBitmap = BitmapFactory.decodeStream(stream)
      }
    }
  }

  InstrumentCardShell(
    tool = AiInstrumentTool.VISION_SCANNER,
    tag = "vision_scanner_card",
    modifier = modifier
  ) {
    OutlinedButton(
      onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
      border = BorderStroke(1.dp, colors.turquoise),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("pick_vision_image_button")
    ) {
      Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.turquoise, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(if (selectedBitmap == null) "Pick Photo, Flyer, or Card" else "Change Selected Image", color = colors.turquoise, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }

    selectedBitmap?.let { bmp ->
      Spacer(modifier = Modifier.height(10.dp))
      Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "Selected photo for Gemini Vision analysis",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(12.dp))
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = instruction,
      onValueChange = { instruction = it },
      label = { Text("What should Gemini extract? (Optional)", fontSize = 11.sp) },
      placeholder = { Text("e.g. Extract phone, email, offer & AI automation fit", fontSize = 11.sp) },
      modifier = Modifier.fillMaxWidth().testTag("vision_instruction_input"),
      colors = instrumentFieldColors()
    )
    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = {
        val bmp = selectedBitmap
        if (bmp == null) {
          viewModel.showSnackbar("Pick a photo or document first.")
        } else {
          viewModel.analyzePickedImage(bmp, instruction)
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("run_vision_scan_button")
    ) {
      Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Analyze with Gemini Vision", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
    RenderTextUiState(state = state, loadingText = "Inspecting image with Gemini 3.8 Flash...")
  }
}

@Composable
private fun ImageStudioInstrumentCard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val state by viewModel.imageGenState.collectAsStateWithLifecycle()
  var prompt by remember { mutableStateOf("") }
  var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

  val picker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      context.contentResolver.openInputStream(it)?.use { stream ->
        sourceBitmap = BitmapFactory.decodeStream(stream)
      }
    }
  }

  InstrumentCardShell(
    tool = AiInstrumentTool.IMAGE_STUDIO,
    tag = "image_studio_card",
    modifier = modifier
  ) {
    OutlinedTextField(
      value = prompt,
      onValueChange = { prompt = it },
      label = { Text("Describe visual to create or edit", fontSize = 11.sp) },
      placeholder = { Text("e.g. Clean ocean-themed storefront hero banner for local surf shop", fontSize = 11.sp) },
      minLines = 2,
      maxLines = 4,
      modifier = Modifier.fillMaxWidth().testTag("image_studio_prompt_input"),
      colors = instrumentFieldColors()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        border = BorderStroke(1.dp, colors.cyan),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("image_studio_pick_base_button")
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (sourceBitmap == null) "Optional Photo" else "Photo Ready", color = colors.cyan, fontSize = 11.sp)
      }
      Button(
        onClick = { viewModel.generateAiImage(prompt, sourceBitmap) },
        enabled = state !is ImageGenUiState.Loading,
        colors = ButtonDefaults.buttonColors(containerColor = colors.goldenSun),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("generate_ai_image_button")
      ) {
        Icon(Icons.Default.Brush, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Generate Image", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 11.sp)
      }
    }

    if (sourceBitmap != null) {
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedButton(
        onClick = { sourceBitmap = null },
        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
      ) {
        Text("Remove Reference Photo", color = colors.textSecondary, fontSize = 11.sp)
      }
    }

    when (val s = state) {
      ImageGenUiState.Idle -> Unit
      ImageGenUiState.Loading -> {
        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.goldenSun, strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Rendering with Nano Banana 2.1 (usually 30–60s)...", color = colors.goldenSun, fontSize = 11.sp)
        }
      }
      is ImageGenUiState.Success -> {
        Spacer(modifier = Modifier.height(12.dp))
        Image(
          bitmap = s.bitmap.asImageBitmap(),
          contentDescription = s.prompt,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
          onClick = { viewModel.clearAiImage() },
          border = BorderStroke(1.dp, colors.teal),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
        ) {
          Text("Clear Image", color = colors.textSecondary, fontSize = 11.sp)
        }
      }
      is ImageGenUiState.Error -> {
        Spacer(modifier = Modifier.height(10.dp))
        InstrumentErrorBanner(message = s.message)
      }
    }
  }
}

@Composable
private fun PitchBuilderInstrumentCard(
  viewModel: OceanSurferViewModel,
  selectedBottleneck: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val state by viewModel.pitchBriefState.collectAsStateWithLifecycle()
  var prospect by remember { mutableStateOf("") }

  InstrumentCardShell(
    tool = AiInstrumentTool.PITCH_BUILDER,
    tag = "pitch_builder_card",
    modifier = modifier
  ) {
    OutlinedTextField(
      value = prospect,
      onValueChange = { prospect = it },
      label = { Text("Client Business or Industry", fontSize = 11.sp) },
      placeholder = { Text("e.g. Maui Eco Boat Tours", fontSize = 11.sp) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("pitch_prospect_input"),
      colors = instrumentFieldColors()
    )
    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = { viewModel.generateSalesPitch(prospect, selectedBottleneck) },
      colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("generate_pitch_button")
    ) {
      Icon(Icons.Default.Campaign, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Build Structured Pitch & Email", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }

    when (val s = state) {
      PitchBriefUiState.Idle -> Unit
      PitchBriefUiState.Loading -> InstrumentLoadingRow("Generating structured JSON pitch...")
      is PitchBriefUiState.Error -> {
        Spacer(modifier = Modifier.height(10.dp))
        InstrumentErrorBanner(s.message)
      }
      is PitchBriefUiState.Success -> {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.brief.headline, color = colors.turquoise, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Best Fit: ${s.brief.recommendedOffer}", color = colors.seaGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            s.brief.talkingPoints.forEach { pt ->
              Text("• $pt", color = colors.textPrimary, fontSize = 12.sp)
            }
            if (s.brief.objectionReply.isNotBlank()) {
              Text("Objection Reply: ${s.brief.objectionReply}", color = colors.cyan, fontSize = 11.sp)
            }
            Text("Email Subject: ${s.brief.emailSubject}", color = colors.goldenSun, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(s.brief.emailBody, color = colors.textSecondary, fontSize = 11.sp)
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                  putExtra(Intent.EXTRA_SUBJECT, s.brief.emailSubject)
                  putExtra(Intent.EXTRA_TEXT, s.brief.emailBody)
                }
                try { context.startActivity(intent) } catch (_: Exception) {
                  viewModel.showSnackbar("No email app installed on this device.")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
            ) {
              Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open in Email App", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CallAnalystInstrumentCard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val colors = OceanTheme.colors
  val calls by viewModel.calls.collectAsStateWithLifecycle()
  val state by viewModel.callQueueBriefState.collectAsStateWithLifecycle()

  InstrumentCardShell(
    tool = AiInstrumentTool.CALL_ANALYST,
    tag = "call_analyst_card",
    modifier = modifier
  ) {
    Text(
      text = "Real logged calls across Phone 1 & Phone 2: ${calls.size}",
      color = colors.textSecondary,
      fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = { viewModel.analyzeRealCallQueue() },
      colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("analyze_call_queue_button")
    ) {
      Icon(Icons.Default.Analytics, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Analyze Real Call Queue", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }

    when (val s = state) {
      CallQueueBriefUiState.Idle -> Unit
      CallQueueBriefUiState.Loading -> InstrumentLoadingRow("Summarizing real calls with Gemini 3.8 Flash...")
      is CallQueueBriefUiState.Error -> {
        Spacer(modifier = Modifier.height(10.dp))
        InstrumentErrorBanner(s.message)
      }
      is CallQueueBriefUiState.Success -> {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.brief.executiveSummary, color = colors.textPrimary, fontSize = 12.sp)
            if (s.brief.urgentCallers.isNotEmpty()) {
              Text("Priority Callers:", color = colors.coralAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              s.brief.urgentCallers.forEach { Text("• $it", color = colors.textPrimary, fontSize = 11.sp) }
            }
            if (s.brief.nextActions.isNotEmpty()) {
              Text("Crew Actions:", color = colors.turquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              s.brief.nextActions.forEach { Text("• $it", color = colors.textPrimary, fontSize = 11.sp) }
            }
            if (s.brief.draftFollowUpEmail.isNotBlank()) {
              Text("Team Digest Draft:\n${s.brief.draftFollowUpEmail}", color = colors.textSecondary, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslatorVoiceInstrumentCard(
  viewModel: OceanSurferViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val colors = OceanTheme.colors
  val state by viewModel.translatorState.collectAsStateWithLifecycle()
  val modes = remember { listOf("Spanish", "Japanese", "Hawaiian Concierge", "Executive Summary", "Friendly SMS") }
  var selectedMode by remember { mutableStateOf(modes.first()) }
  var sourceText by remember { mutableStateOf("") }

  val micLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      viewModel.startConsoleVoiceInput { spoken -> sourceText = spoken }
    } else {
      viewModel.showSnackbar("Microphone permission required for dictation.")
    }
  }

  InstrumentCardShell(
    tool = AiInstrumentTool.TRANSLATOR_VOICE,
    tag = "translator_voice_card",
    modifier = modifier
  ) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      modes.forEach { mode ->
        FilterChip(
          selected = selectedMode == mode,
          onClick = { selectedMode = mode },
          label = { Text(mode, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.turquoise,
            selectedLabelColor = colors.abyss,
            containerColor = colors.card,
            labelColor = colors.textPrimary
          )
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = sourceText,
      onValueChange = { sourceText = it },
      label = { Text("Message to translate or rewrite", fontSize = 11.sp) },
      trailingIcon = {
        IconButton(
          onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
              viewModel.startConsoleVoiceInput { spoken -> sourceText = spoken }
            } else {
              micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          },
          modifier = Modifier.size(48.dp)
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Dictate text", tint = colors.turquoise)
        }
      },
      minLines = 2,
      maxLines = 4,
      modifier = Modifier.fillMaxWidth().testTag("translator_source_input"),
      colors = instrumentFieldColors()
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = { viewModel.runTranslatorOrRewriter(sourceText, selectedMode) },
        colors = ButtonDefaults.buttonColors(containerColor = colors.turquoise),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("run_translator_button")
      ) {
        Icon(Icons.Default.Translate, contentDescription = null, tint = colors.abyss, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Transform", color = colors.abyss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
      if (state is InstrumentTextUiState.Success) {
        OutlinedButton(
          onClick = { viewModel.speakText((state as InstrumentTextUiState.Success).text) },
          border = BorderStroke(1.dp, colors.seaGreen),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak Aloud", tint = colors.seaGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Speak", color = colors.seaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    RenderTextUiState(state = state, loadingText = "Running Gemini 3.5 Flash-Lite...")
  }
}

@Composable
private fun InstrumentCardShell(
  tool: AiInstrumentTool,
  tag: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val colors = OceanTheme.colors
  Card(
    modifier = modifier.fillMaxWidth().testTag(tag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
    border = BorderStroke(1.dp, colors.turquoise.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = colors.turquoise.copy(alpha = 0.18f), modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Icon(toolIcon(tool), contentDescription = tool.title, tint = colors.turquoise, modifier = Modifier.size(22.dp))
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(tool.title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text(tool.subtitle, color = colors.seaGreen, fontSize = 11.sp)
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun RenderTextUiState(state: InstrumentTextUiState, loadingText: String) {
  val colors = OceanTheme.colors
  when (state) {
    InstrumentTextUiState.Idle -> Unit
    InstrumentTextUiState.Loading -> InstrumentLoadingRow(loadingText)
    is InstrumentTextUiState.Error -> {
      Spacer(modifier = Modifier.height(10.dp))
      InstrumentErrorBanner(state.message)
    }
    is InstrumentTextUiState.Success -> {
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.seaGreen.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          if (state.headline.isNotBlank()) {
            Text(state.headline, color = colors.seaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
          }
          Text(state.text, color = colors.textPrimary, fontSize = 12.sp, lineHeight = 18.sp)
        }
      }
    }
  }
}

@Composable
private fun InstrumentLoadingRow(label: String) {
  val colors = OceanTheme.colors
  Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.turquoise, strokeWidth = 2.dp)
    Spacer(modifier = Modifier.width(8.dp))
    Text(label, color = colors.cyan, fontSize = 12.sp)
  }
}

@Composable
private fun InstrumentErrorBanner(message: String) {
  val colors = OceanTheme.colors
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = colors.coralAlert.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, colors.coralAlert),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Warning, contentDescription = "Error", tint = colors.coralAlert, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(message, color = colors.textPrimary, fontSize = 12.sp)
    }
  }
}

@Composable
private fun instrumentFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = OceanTheme.colors.turquoise,
  unfocusedBorderColor = OceanTheme.colors.teal,
  focusedTextColor = OceanTheme.colors.textPrimary,
  unfocusedTextColor = OceanTheme.colors.textPrimary
)

private fun toolIcon(tool: AiInstrumentTool): ImageVector = when (tool) {
  AiInstrumentTool.WAVE_ADVISOR -> Icons.Default.AutoAwesome
  AiInstrumentTool.COPILOT_CHAT -> Icons.AutoMirrored.Filled.Chat
  AiInstrumentTool.VISION_SCANNER -> Icons.Default.DocumentScanner
  AiInstrumentTool.IMAGE_STUDIO -> Icons.Default.Brush
  AiInstrumentTool.PITCH_BUILDER -> Icons.Default.Campaign
  AiInstrumentTool.CALL_ANALYST -> Icons.Default.Analytics
  AiInstrumentTool.TRANSLATOR_VOICE -> Icons.Default.Translate
}
