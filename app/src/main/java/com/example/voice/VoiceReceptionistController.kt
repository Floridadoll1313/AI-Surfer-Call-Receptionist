package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceUiState(
  val isListening: Boolean = false,
  val isSpeaking: Boolean = false,
  val isVoiceOutputEnabled: Boolean = true,
  val isSpeechAvailable: Boolean = true,
  val partialTranscript: String = "",
  val statusMessage: String = ""
)

class VoiceReceptionistController(
  private val context: Context,
  private val onFinalTranscript: (String) -> Unit
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val _uiState = MutableStateFlow(
    VoiceUiState(isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(context))
  )
  val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

  private var speechRecognizer: SpeechRecognizer? = null
  private var tts: TextToSpeech? = null
  private var ttsReady = false

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.US
        ttsReady = true
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _uiState.value = _uiState.value.copy(isSpeaking = true)
          }

          override fun onDone(utteranceId: String?) {
            _uiState.value = _uiState.value.copy(isSpeaking = false)
          }

          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            _uiState.value = _uiState.value.copy(isSpeaking = false)
          }
        })
      }
    }
  }

  fun toggleVoiceOutput() {
    val next = !_uiState.value.isVoiceOutputEnabled
    if (!next) {
      stopSpeaking()
    }
    _uiState.value = _uiState.value.copy(
      isVoiceOutputEnabled = next,
      statusMessage = if (next) "Kai voice reply enabled" else "Kai voice muted"
    )
  }

  fun speakReply(text: String) {
    if (!_uiState.value.isVoiceOutputEnabled || !ttsReady || text.isBlank()) return
    mainHandler.post {
      try {
        val cleanText = text.replace(Regex("https?://\\S+"), "our website link")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "kai_reply_${System.currentTimeMillis()}")
      } catch (_: Exception) {
        _uiState.value = _uiState.value.copy(isSpeaking = false)
      }
    }
  }

  fun stopSpeaking() {
    mainHandler.post {
      try {
        tts?.stop()
      } catch (_: Exception) {}
      _uiState.value = _uiState.value.copy(isSpeaking = false)
    }
  }

  fun startListening() {
    mainHandler.post {
      if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        _uiState.value = _uiState.value.copy(
          isSpeechAvailable = false,
          isListening = false,
          statusMessage = "Speech recognition service unavailable on this device."
        )
        return@post
      }
      stopSpeaking()
      try {
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        recognizer.setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _uiState.value = _uiState.value.copy(
              isListening = true,
              partialTranscript = "",
              statusMessage = "Listening for caller..."
            )
          }

          override fun onBeginningOfSpeech() {}
          override fun onRmsChanged(rmsdB: Float) {}
          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            _uiState.value = _uiState.value.copy(isListening = false)
          }

          override fun onError(error: Int) {
            _uiState.value = _uiState.value.copy(
              isListening = false,
              statusMessage = "Voice capture ended (code $error)"
            )
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull()?.trim().orEmpty()
            _uiState.value = _uiState.value.copy(
              isListening = false,
              partialTranscript = "",
              statusMessage = if (transcript.isNotBlank()) "Captured speech" else ""
            )
            if (transcript.isNotBlank()) {
              onFinalTranscript(transcript)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (partial.isNotBlank()) {
              _uiState.value = _uiState.value.copy(partialTranscript = partial)
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer = recognizer
        recognizer.startListening(intent)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
          isListening = false,
          statusMessage = e.localizedMessage ?: "Unable to start microphone listener."
        )
      }
    }
  }

  fun stopListening() {
    mainHandler.post {
      try {
        speechRecognizer?.stopListening()
      } catch (_: Exception) {}
      _uiState.value = _uiState.value.copy(isListening = false)
    }
  }

  fun release() {
    mainHandler.post {
      try {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
      } catch (_: Exception) {}
    }
  }
}
