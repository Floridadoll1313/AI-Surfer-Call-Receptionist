package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.ActiveCallUiState
import com.example.ui.theme.OceanTheme
import com.example.voice.VoiceUiState

@Composable
fun ActiveCallOverlay(
  activeCall: ActiveCallUiState,
  onAnswer: () -> Unit,
  onEndCall: () -> Unit,
  onSendQuery: (String) -> Unit,
  voiceUiState: VoiceUiState = VoiceUiState(),
  onStartVoiceListening: () -> Unit = {},
  onStopVoiceListening: () -> Unit = {},
  onToggleVoiceOutput: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (!activeCall.isRinging && !activeCall.isInCall) return

  val context = LocalContext.current
  val colors = OceanTheme.colors
  var customQueryInput by remember { mutableStateOf("") }
  var micPermissionDenied by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      micPermissionDenied = false
      onStartVoiceListening()
    } else {
      micPermissionDenied = true
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.abyss.copy(alpha = 0.94f))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .testTag("active_call_card"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = colors.deepBlue),
      border = androidx.compose.foundation.BorderStroke(2.dp, colors.turquoise.copy(alpha = 0.6f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Surfer Avatar & Status Icon
        Surface(
          shape = CircleShape,
          color = colors.teal.copy(alpha = 0.3f),
          modifier = Modifier.size(80.dp),
          border = androidx.compose.foundation.BorderStroke(2.dp, colors.turquoise)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Surfing,
              contentDescription = "AI Receptionist Surfer",
              tint = colors.turquoise,
              modifier = Modifier.size(44.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = if (activeCall.isRinging) "🌊 Incoming Cell Call..." else "🌊 AI Receptionist Active",
          style = MaterialTheme.typography.labelLarge,
          color = colors.goldenSun,
          fontWeight = FontWeight.Bold
        )

        Text(
          text = activeCall.callerName.ifBlank { "Ocean Swell Caller" },
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = colors.textPrimary
        )

        Text(
          text = activeCall.callerNumber.ifBlank { "+1 (555) 789-WAVE" },
          style = MaterialTheme.typography.bodyMedium,
          color = colors.textSecondary
        )

        if (activeCall.returningContextBadge.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.goldenSun.copy(alpha = 0.18f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.goldenSun),
            modifier = Modifier.testTag("returning_caller_badge")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Returning Caller Profile",
                tint = colors.goldenSun,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = activeCall.returningContextBadge,
                color = colors.goldenSun,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        if (activeCall.isInCall) {
          val minutes = activeCall.callDurationSeconds / 60
          val seconds = activeCall.callDurationSeconds % 60
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Text(
              text = "Duration: %02d:%02d".format(minutes, seconds),
              style = MaterialTheme.typography.bodySmall,
              color = colors.cyan,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = onToggleVoiceOutput,
              modifier = Modifier
                .size(48.dp)
                .testTag("toggle_tts_button")
            ) {
              Icon(
                imageVector = if (voiceUiState.isVoiceOutputEnabled) {
                  Icons.AutoMirrored.Filled.VolumeUp
                } else {
                  Icons.AutoMirrored.Filled.VolumeOff
                },
                contentDescription = if (voiceUiState.isVoiceOutputEnabled) {
                  "Mute Kai voice reply"
                } else {
                  "Unmute Kai voice reply"
                },
                tint = if (voiceUiState.isSpeaking) colors.goldenSun else if (voiceUiState.isVoiceOutputEnabled) colors.seaGreen else colors.textSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          AudioWaveformIndicator(
            waveHeights = activeCall.audioWaveHeights,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ringing State Controls
        if (activeCall.isRinging) {
          Text(
            text = "Ocean Tide Drop AI Receptionist is ready to intercept and answer caller questions automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            Button(
              onClick = onEndCall,
              colors = ButtonDefaults.buttonColors(containerColor = colors.coralAlert),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("decline_call_button")
            ) {
              Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Decline")
              Spacer(modifier = Modifier.width(6.dp))
              Text("Decline")
            }

            Button(
              onClick = onAnswer,
              colors = ButtonDefaults.buttonColors(containerColor = colors.seaGreen),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag("answer_call_button")
            ) {
              Icon(imageVector = Icons.Default.Call, contentDescription = "Answer")
              Spacer(modifier = Modifier.width(6.dp))
              Text("Answer with AI", color = colors.abyss, fontWeight = FontWeight.Bold)
            }
          }
        } else if (activeCall.isInCall) {
          // Active Conversation Bubbles
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(colors.card, RoundedCornerShape(16.dp))
              .padding(14.dp)
          ) {
            Text(
              text = "🗣️ Caller Inquired:",
              style = MaterialTheme.typography.labelMedium,
              color = colors.goldenSun,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = activeCall.currentCustomerQuery.ifBlank { "Connecting..." },
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textPrimary,
              modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            androidx.compose.material3.HorizontalDivider(
              color = colors.teal.copy(alpha = 0.3f),
              modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "🏄‍♂️ Kai (AI Receptionist):",
                style = MaterialTheme.typography.labelMedium,
                color = colors.turquoise,
                fontWeight = FontWeight.Bold
              )
              if (activeCall.isAiThinking) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                  modifier = Modifier.size(14.dp),
                  color = colors.turquoise,
                  strokeWidth = 2.dp
                )
              }
            }

            Text(
              text = if (activeCall.isAiThinking) "Formulating ocean response..." else activeCall.currentAiResponse,
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textPrimary,
              modifier = Modifier.padding(top = 4.dp)
            )

            if (activeCall.isFlaggedForEscalation && !activeCall.isAiThinking) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.coralAlert.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.coralAlert),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("active_call_escalation_flag")
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Escalation Flagged",
                    tint = colors.coralAlert,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "🚨 AI FLAGGED FOR ESCALATION",
                      color = colors.coralAlert,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                      text = activeCall.escalationReason.ifBlank { "Alert will dispatch automatically via configured Email/Notification trigger on hang-up." },
                      color = colors.textPrimary,
                      fontSize = 10.sp
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Quick Topic Simulation Chips
          Text(
            text = "Ask Receptionist Live on Call:",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.Start)
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = { onSendQuery("What is included in the Free AI Wave Check audit?") },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Free Wave Check", fontSize = 11.sp, color = colors.cyan)
            }
            Button(
              onClick = { onSendQuery("How do I buy the $497 Wave Starter Sprint via Stripe?") },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Wave Starter", fontSize = 11.sp, color = colors.cyan)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = { onSendQuery("Tell me about Big Kahuna multi-agent enterprise AI transformation.") },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Big Kahuna VIP", fontSize = 11.sp, color = colors.goldenSun)
            }
            Button(
              onClick = { onSendQuery("How can I book a strategy call on Calendly?") },
              colors = ButtonDefaults.buttonColors(containerColor = colors.cardSecondary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Strategy Call", fontSize = 11.sp, color = colors.cyan)
            }
          }

          if (voiceUiState.isListening || voiceUiState.partialTranscript.isNotBlank() || micPermissionDenied) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (micPermissionDenied) colors.coralAlert.copy(alpha = 0.16f) else colors.teal.copy(alpha = 0.2f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = when {
                  micPermissionDenied -> "Microphone permission needed for live voice input. You can also type below."
                  voiceUiState.partialTranscript.isNotBlank() -> "Listening: \"${voiceUiState.partialTranscript}\""
                  else -> "Listening for live caller speech..."
                },
                color = if (micPermissionDenied) colors.coralAlert else colors.cyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Voice Mic + Custom Input during call
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                if (voiceUiState.isListening) {
                  onStopVoiceListening()
                } else {
                  val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                  ) == PackageManager.PERMISSION_GRANTED
                  if (granted) {
                    micPermissionDenied = false
                    onStartVoiceListening()
                  } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("voice_mic_button")
            ) {
              Icon(
                imageVector = if (voiceUiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (voiceUiState.isListening) "Stop voice input" else "Speak to AI Receptionist",
                tint = if (voiceUiState.isListening) colors.coralAlert else colors.turquoise
              )
            }

            OutlinedTextField(
              value = customQueryInput,
              onValueChange = { customQueryInput = it },
              placeholder = { Text("Ask any custom question...", fontSize = 13.sp, color = colors.textSecondary) },
              modifier = Modifier
                .weight(1f)
                .testTag("call_query_input"),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.turquoise,
                unfocusedBorderColor = colors.teal,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            IconButton(
              onClick = {
                if (customQueryInput.isNotBlank()) {
                  onSendQuery(customQueryInput)
                  customQueryInput = ""
                }
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("call_send_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send to Receptionist",
                tint = colors.turquoise
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onEndCall,
            colors = ButtonDefaults.buttonColors(containerColor = colors.coralAlert),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("end_call_button")
          ) {
            Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call")
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Call & Save Transcript", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
