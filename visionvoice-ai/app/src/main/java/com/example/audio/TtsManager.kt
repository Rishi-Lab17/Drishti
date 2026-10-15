package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        } else {
            Log.e(TAG, "TextToSpeech init failed with status: $status")
        }
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    /**
     * Speaks ALL generated text from beginning to end.
     * Strips markdown symbols for natural audio flow and queues chunks using QUEUE_ADD
     * so that Android TTS never truncates long descriptions or full text transcriptions.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (text.isBlank()) return
        _lastSpokenText.value = text
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        val speechReady = sanitizeForSpeech(text)
        val chunks = splitIntoSpeakableChunks(speechReady, 800)
        if (chunks.isEmpty()) return

        // First chunk uses queueMode (typically QUEUE_FLUSH to cancel prior speech)
        val firstId = UUID.randomUUID().toString()
        tts?.speak(chunks[0], queueMode, null, firstId)

        // Queue all remaining chunks so every single sentence is read
        for (i in 1 until chunks.size) {
            val chunkId = UUID.randomUUID().toString()
            tts?.speak(chunks[i], TextToSpeech.QUEUE_ADD, null, chunkId)
        }
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .replace(Regex("[#*`_~]"), "") // Remove markdown asterisks, hashes, backticks
            .replace(Regex("\n{2,}"), ". ")
            .replace("\n", ". ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun splitIntoSpeakableChunks(text: String, maxLen: Int): List<String> {
        if (text.length <= maxLen) return listOf(text)

        val chunks = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            val s = sentence.trim()
            if (s.isEmpty()) continue

            if (currentChunk.length + s.length + 1 > maxLen) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
            }
            currentChunk.append(s).append(" ")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return if (chunks.isEmpty()) listOf(text) else chunks
    }

    fun repeat() {
        val last = _lastSpokenText.value
        if (last.isNotBlank()) {
            speak(last)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun playStartCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    fun playDoneCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        } catch (_: Exception) {}
    }

    fun playErrorCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "TtsManager"
    }
}
