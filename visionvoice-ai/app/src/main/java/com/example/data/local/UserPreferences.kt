package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContrastTheme {
    DARK_CYAN,
    YELLOW_ON_BLACK,
    AMBER_ON_BLACK,
    LIGHT_HIGH_CONTRAST
}

enum class AiDetailLevel {
    CONCISE,   // Fast 1-2 sentence immediate summary (ideal for walking)
    BALANCED,  // Medium detail with main objects and spatial cues
    DETAILED   // Comprehensive analysis of scene, text, colors, and layout
}

data class AppSettings(
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val autoSpeakResults: Boolean = true,
    val contrastTheme: ContrastTheme = ContrastTheme.DARK_CYAN,
    val hapticFeedbackEnabled: Boolean = true,
    val detailLevel: AiDetailLevel = AiDetailLevel.BALANCED,
    val audioCuesEnabled: Boolean = true,
    val cameraFlashAuto: Boolean = false,
    val customApiKey: String = ""
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("drishti_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f),
            autoSpeakResults = prefs.getBoolean(KEY_AUTO_SPEAK, true),
            contrastTheme = try {
                ContrastTheme.valueOf(
                    prefs.getString(KEY_CONTRAST_THEME, ContrastTheme.DARK_CYAN.name) ?: ContrastTheme.DARK_CYAN.name
                )
            } catch (e: Exception) {
                ContrastTheme.DARK_CYAN
            },
            hapticFeedbackEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            detailLevel = try {
                AiDetailLevel.valueOf(
                    prefs.getString(KEY_DETAIL_LEVEL, AiDetailLevel.BALANCED.name) ?: AiDetailLevel.BALANCED.name
                )
            } catch (e: Exception) {
                AiDetailLevel.BALANCED
            },
            audioCuesEnabled = prefs.getBoolean(KEY_AUDIO_CUES, true),
            cameraFlashAuto = prefs.getBoolean(KEY_FLASH_AUTO, false),
            customApiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        )
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()
        _settings.value = _settings.value.copy(speechPitch = pitch)
    }

    fun updateAutoSpeak(auto: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, auto).apply()
        _settings.value = _settings.value.copy(autoSpeakResults = auto)
    }

    fun updateContrastTheme(theme: ContrastTheme) {
        prefs.edit().putString(KEY_CONTRAST_THEME, theme.name).apply()
        _settings.value = _settings.value.copy(contrastTheme = theme)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticFeedbackEnabled = enabled)
    }

    fun updateDetailLevel(level: AiDetailLevel) {
        prefs.edit().putString(KEY_DETAIL_LEVEL, level.name).apply()
        _settings.value = _settings.value.copy(detailLevel = level)
    }

    fun updateAudioCues(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_CUES, enabled).apply()
        _settings.value = _settings.value.copy(audioCuesEnabled = enabled)
    }

    fun updateCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _settings.value = _settings.value.copy(customApiKey = trimmed)
    }

    companion object {
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_CONTRAST_THEME = "contrast_theme"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_DETAIL_LEVEL = "detail_level"
        private const val KEY_AUDIO_CUES = "audio_cues"
        private const val KEY_FLASH_AUTO = "flash_auto"
        private const val KEY_API_KEY = "custom_gemini_api_key"
    }
}
