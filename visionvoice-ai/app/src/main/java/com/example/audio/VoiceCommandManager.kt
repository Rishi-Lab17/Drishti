package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.local.VisionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceAction {
    data class SwitchMode(val mode: VisionMode) : VoiceAction()
    data class AskQuestion(val question: String) : VoiceAction()
    data object ScanNow : VoiceAction()
    data object ToggleTorch : VoiceAction()
    data object RepeatSpeech : VoiceAction()
    data object StopSpeech : VoiceAction()
    data class Unknown(val rawText: String) : VoiceAction()
}

class VoiceCommandManager(private val context: Context) : RecognitionListener {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastSpokenPrompt = MutableStateFlow("")
    val lastSpokenPrompt: StateFlow<String> = _lastSpokenPrompt.asStateFlow()

    private val _lastAction = MutableStateFlow<VoiceAction?>(null)
    val lastAction: StateFlow<VoiceAction?> = _lastAction.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceCommandManager)
            }
        } else {
            Log.w(TAG, "Speech recognition not available on this device")
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun clearAction() {
        _lastAction.value = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
        Log.w(TAG, "Speech recognition error code: $error")
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull()?.trim() ?: return
        _lastSpokenPrompt.value = bestText
        _lastAction.value = parseCommand(bestText)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let { _lastSpokenPrompt.value = it }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun parseCommand(text: String): VoiceAction {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("describe") || lower.contains("scene") || lower.contains("look around") ->
                VoiceAction.SwitchMode(VisionMode.SCENE)

            lower.contains("read") || lower.contains("text") || lower.contains("words") || lower.contains("label") ->
                VoiceAction.SwitchMode(VisionMode.OCR)

            lower.contains("object") || lower.contains("detect") || lower.contains("find") || lower.contains("what is here") ->
                VoiceAction.SwitchMode(VisionMode.OBJECTS)

            lower.contains("depth") || lower.contains("distance") || lower.contains("hazard") || lower.contains("obstacle") || lower.contains("spatial") ->
                VoiceAction.SwitchMode(VisionMode.SPATIAL)

            lower.contains("scan") || lower.contains("capture") || lower.contains("take photo") ->
                VoiceAction.ScanNow

            lower.contains("torch") || lower.contains("flash") || lower.contains("light") ->
                VoiceAction.ToggleTorch

            lower.contains("repeat") || lower.contains("again") || lower.contains("say that again") ->
                VoiceAction.RepeatSpeech

            lower.contains("stop") || lower.contains("quiet") || lower.contains("silence") || lower.contains("pause") ->
                VoiceAction.StopSpeech

            lower.startsWith("what") || lower.startsWith("is there") || lower.startsWith("where") || lower.startsWith("how many") || lower.startsWith("can you") ->
                VoiceAction.AskQuestion(text)

            else -> VoiceAction.AskQuestion(text)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "VoiceCommandManager"
    }
}
