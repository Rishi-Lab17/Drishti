package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticManager
import com.example.audio.TtsManager
import com.example.audio.VoiceAction
import com.example.audio.VoiceCommandManager
import com.example.camera.CameraController
import com.example.data.local.AppSettings
import com.example.data.local.ContrastTheme
import com.example.data.local.PreferencesManager
import com.example.data.local.ScanRecord
import com.example.data.local.VisionMode
import com.example.data.local.VisionVoiceDatabase
import com.example.data.remote.GeminiVisionService
import com.example.data.remote.VisionAnalysisResult
import com.example.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    VISION,
    HISTORY,
    DASHBOARD,
    SETTINGS
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VisionVoiceDatabase.getDatabase(application)
    private val repository = VisionRepository(GeminiVisionService(), database.scanRecordDao())
    val preferencesManager = PreferencesManager(application)
    val ttsManager = TtsManager(application)
    val voiceCommandManager = VoiceCommandManager(application)
    val hapticManager = HapticManager(application)

    val settings: StateFlow<AppSettings> = preferencesManager.settings

    private val _currentTab = MutableStateFlow(AppTab.VISION)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentMode = MutableStateFlow(VisionMode.SCENE)
    val currentMode: StateFlow<VisionMode> = _currentMode.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<VisionAnalysisResult?>(null)
    val analysisResult: StateFlow<VisionAnalysisResult?> = _analysisResult.asStateFlow()

    private val _activeScanId = MutableStateFlow<Long?>(null)
    val activeScanId: StateFlow<Long?> = _activeScanId.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _askDialogVisible = MutableStateFlow(false)
    val askDialogVisible: StateFlow<Boolean> = _askDialogVisible.asStateFlow()

    private val _customQuestion = MutableStateFlow("")
    val customQuestion: StateFlow<String> = _customQuestion.asStateFlow()

    var cameraController: CameraController? = null

    // Scans flow
    val allScans: StateFlow<List<ScanRecord>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.allScans else repository.searchScans(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedScans: StateFlow<List<ScanRecord>> = repository.bookmarkedScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalScansCount: StateFlow<Int> = repository.totalScansCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val ocrScansCount: StateFlow<Int> = repository.ocrScansCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val objectScansCount: StateFlow<Int> = repository.objectScansCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Sync TTS settings
        viewModelScope.launch {
            settings.collect { s ->
                ttsManager.setSpeechRate(s.speechRate)
                ttsManager.setPitch(s.speechPitch)
            }
        }

        // Listen for voice commands
        viewModelScope.launch {
            voiceCommandManager.lastAction.collect { action ->
                action?.let { handleVoiceAction(it) }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        _currentTab.value = tab
        val announcement = when (tab) {
            AppTab.VISION -> "Drishti Vision screen"
            AppTab.HISTORY -> "Scan History screen"
            AppTab.DASHBOARD -> "Dashboard statistics"
            AppTab.SETTINGS -> "Accessibility settings"
        }
        ttsManager.speak(announcement)
    }

    fun updateCustomApiKey(key: String) {
        preferencesManager.updateCustomApiKey(key)
        val msg = if (key.isBlank()) "API key cleared. Using local assessment." else "Gemini API key saved."
        ttsManager.speak(msg)
    }

    fun switchMode(mode: VisionMode) {
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        _currentMode.value = mode
        val modeAnnouncement = when (mode) {
            VisionMode.SCENE -> "Scene Description mode. Tap screen to analyze surroundings."
            VisionMode.OCR -> "Read Text mode. Align signs, packaging or documents."
            VisionMode.OBJECTS -> "Object Detection mode. Scanning for items and hazards."
            VisionMode.SPATIAL -> "Spatial and Depth mode. Gauging distances and obstacles."
            VisionMode.ASK -> "Ask AI mode. Ask any question about your surroundings."
        }
        ttsManager.speak(modeAnnouncement)
    }

    fun captureAndAnalyze(question: String? = null) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true

        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateCapture()
        if (settings.value.audioCuesEnabled) ttsManager.playStartCue()

        ttsManager.speak("Analyzing visual surroundings...", android.speech.tts.TextToSpeech.QUEUE_FLUSH)

        val controller = cameraController
        if (controller != null) {
            controller.capturePhoto(
                onSuccess = { bitmap ->
                    processCapturedBitmap(bitmap, question)
                },
                onError = {
                    val fallbackBitmap = createSyntheticViewBitmap()
                    processCapturedBitmap(fallbackBitmap, question)
                }
            )
        } else {
            val fallbackBitmap = createSyntheticViewBitmap()
            processCapturedBitmap(fallbackBitmap, question)
        }
    }

    fun processCapturedBitmap(bitmap: Bitmap, question: String? = null) {
        viewModelScope.launch {
            try {
                val (result, scanId) = repository.analyzeAndSave(
                    bitmap = bitmap,
                    mode = _currentMode.value,
                    detailLevel = settings.value.detailLevel,
                    customQuestion = question ?: _customQuestion.value.takeIf { it.isNotBlank() },
                    apiKeyOverride = settings.value.customApiKey
                )

                _analysisResult.value = result
                _activeScanId.value = scanId
                _isAnalyzing.value = false

                if (settings.value.hapticFeedbackEnabled) {
                    if (result.hazardWarning != null) {
                        hapticManager.vibrateWarning()
                    } else {
                        hapticManager.vibrateSuccess()
                    }
                }
                if (settings.value.audioCuesEnabled) ttsManager.playDoneCue()

                if (settings.value.autoSpeakResults) {
                    val fullSpeech = buildCompleteSpokenText(result)
                    ttsManager.speak(fullSpeech)
                }
            } catch (e: Exception) {
                _isAnalyzing.value = false
                if (settings.value.audioCuesEnabled) ttsManager.playErrorCue()
                ttsManager.speak("Unable to analyze image. Please try again.")
            }
        }
    }

    private fun buildCompleteSpokenText(result: VisionAnalysisResult): String {
        return buildString {
            if (!result.hazardWarning.isNullOrBlank()) {
                append(result.hazardWarning)
                append(". ")
            }
            if (result.detailedText.isNotBlank()) {
                append(result.detailedText)
            } else {
                append(result.spokenSummary)
            }
        }.trim()
    }

    private fun handleVoiceAction(action: VoiceAction) {
        voiceCommandManager.clearAction()
        when (action) {
            is VoiceAction.SwitchMode -> {
                switchMode(action.mode)
            }
            is VoiceAction.ScanNow -> {
                captureAndAnalyze()
            }
            is VoiceAction.ToggleTorch -> {
                toggleTorch()
            }
            is VoiceAction.RepeatSpeech -> {
                repeatSpeech()
            }
            is VoiceAction.StopSpeech -> {
                stopSpeaking()
            }
            is VoiceAction.AskQuestion -> {
                _currentMode.value = VisionMode.ASK
                _customQuestion.value = action.question
                captureAndAnalyze(action.question)
            }
            is VoiceAction.Unknown -> {
                ttsManager.speak("Heard: ${action.rawText}. Tap the center button to scan.")
            }
        }
    }

    fun toggleTorch() {
        cameraController?.toggleTorch { isOn ->
            _isTorchOn.value = isOn
            val stateText = if (isOn) "Flashlight turned on" else "Flashlight turned off"
            ttsManager.speak(stateText)
            if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        }
    }

    fun switchCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        cameraController?.toggleCamera(lifecycleOwner)
        _isFrontCamera.value = cameraController?.isFrontCamera ?: false
        val camText = if (_isFrontCamera.value) "Front selfie camera active" else "Back main camera active"
        ttsManager.speak(camText)
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
    }

    fun startListeningForVoiceCommand() {
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        ttsManager.speak("Listening for command...")
        voiceCommandManager.startListening()
    }

    fun stopListeningVoiceCommand() {
        voiceCommandManager.stopListening()
    }

    fun repeatSpeech() {
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        ttsManager.repeat()
    }

    fun stopSpeaking() {
        if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        ttsManager.stop()
    }

    fun speakCustomText(text: String) {
        ttsManager.speak(text)
    }

    fun toggleBookmark(scan: ScanRecord) {
        viewModelScope.launch {
            repository.toggleBookmark(scan)
            val msg = if (!scan.isBookmarked) "Saved to bookmarks" else "Removed from bookmarks"
            ttsManager.speak(msg)
            if (settings.value.hapticFeedbackEnabled) hapticManager.vibrateClick()
        }
    }

    fun deleteScan(scan: ScanRecord) {
        viewModelScope.launch {
            repository.deleteScan(scan)
            ttsManager.speak("Scan removed from history")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            ttsManager.speak("Scan history cleared")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAskDialogVisible(visible: Boolean) {
        _askDialogVisible.value = visible
    }

    fun setCustomQuestion(q: String) {
        _customQuestion.value = q
    }

    private fun createSyntheticViewBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(18, 24, 38))
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("Drishti AI Lens", 80f, 200f, paint)
        canvas.drawText("Assistive Camera Viewfinder", 80f, 260f, paint)
        return bitmap
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        voiceCommandManager.destroy()
        cameraController?.release()
    }
}
