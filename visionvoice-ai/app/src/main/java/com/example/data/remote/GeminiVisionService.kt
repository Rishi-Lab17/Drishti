package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AiDetailLevel
import com.example.data.local.VisionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiVisionService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun analyzeImage(
        bitmap: Bitmap,
        mode: VisionMode,
        detailLevel: AiDetailLevel,
        customQuestion: String? = null,
        apiKeyOverride: String? = null
    ): VisionAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No valid Gemini API key configured. Performing dynamic on-device frame analysis.")
            return@withContext performOnDeviceFrameAssessment(bitmap, mode, customQuestion)
        }

        val base64Image = bitmapToBase64(bitmap)
        val prompt = buildPrompt(mode, detailLevel, customQuestion)
        val jsonPayload = buildRequestBody(prompt, base64Image, mode)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        // Multiple model endpoints to fallback if one model hits quota/rate limits
        val candidateModels = listOf("gemini-flash-latest", "gemini-3.5-flash", "gemini-3.1-flash-lite-preview")
        var isRateLimited = false
        var lastErrorMsg = "Unable to analyze scene."

        for (candidateModel in candidateModels) {
            val url = "$baseUrl/$candidateModel:generateContent?key=$apiKey"
            for (attempt in 1..2) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val code = response.code
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                        return@withContext parseGeminiResponse(responseBody, mode)
                    }

                    if (code == 429) {
                        isRateLimited = true
                        Log.w(TAG, "Rate limit hit on $candidateModel (attempt $attempt). Switching model...")
                        delay(600)
                        break // Break to next candidate model
                    } else if (code in 500..599) {
                        Log.w(TAG, "Server error $code on $candidateModel. Retrying...")
                        delay(600)
                        continue
                    } else {
                        lastErrorMsg = "Gemini error $code: ${responseBody?.take(150) ?: "Unknown error"}"
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception calling $candidateModel", e)
                    lastErrorMsg = "Network error: ${e.message ?: "Connection failed"}"
                }
            }
        }

        if (isRateLimited) {
            return@withContext VisionAnalysisResult(
                title = "AI Rate Limit (Wait 10s)",
                spokenSummary = "Gemini rate limit reached for this minute. Please wait 10 seconds, or enter your personal Gemini API key in Settings.",
                detailedText = "Gemini AI rate limit reached (HTTP 429: Too Many Requests).\n\nWhat to do:\n1. Pause for 10-15 seconds and tap Scan again.\n2. Or enter your personal free Gemini key in Settings (Settings -> Gemini AI API Key) for dedicated unlimited quota.",
                isFallback = false
            )
        }

        VisionAnalysisResult(
            title = "Analysis Incomplete",
            spokenSummary = lastErrorMsg,
            detailedText = "$lastErrorMsg\n\nPlease check your internet connection or pause briefly and tap Scan again.",
            isFallback = false
        )
    }

    private fun performOnDeviceFrameAssessment(
        bitmap: Bitmap,
        mode: VisionMode,
        customQuestion: String?
    ): VisionAnalysisResult {
        // Sample real pixel data for actual ambient luminance
        val stepX = maxOf(1, bitmap.width / 20)
        val stepY = maxOf(1, bitmap.height / 20)
        var totalLuminance = 0.0
        var samples = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += lum
                samples++
            }
        }

        val avgLum = if (samples > 0) totalLuminance / samples else 128.0
        val lightingCondition = when {
            avgLum < 45 -> "low lighting. Consider turning on the flashlight for better clarity."
            avgLum > 215 -> "bright, high glare lighting."
            else -> "good, balanced illumination."
        }

        val spokenMsg = "Frame captured with $lightingCondition. To activate AI scene description and text reading, enter your Gemini API key in Settings."
        val detailMsg = """
            Drishti Camera Frame Assessment:
            - Frame Size: ${bitmap.width} × ${bitmap.height}
            - Ambient Lighting: $lightingCondition (score: ${avgLum.toInt()}/255)
            
            Status: Live AI Vision requires a Gemini API Key.
            You can enter your key directly inside the app:
            1. Tap 'Settings' in the bottom navigation bar.
            2. Enter or paste your Gemini API key.
            3. Tap 'Save Key' to enable real-time scene and OCR reading.
        """.trimIndent()

        return VisionAnalysisResult(
            title = "Camera Frame Captured",
            spokenSummary = spokenMsg,
            detailedText = detailMsg,
            hazardWarning = if (avgLum < 45) "Low light detected. Tap flashlight button to illuminate view." else null,
            isFallback = true
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        val maxDim = 800
        val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
        } else {
            1.0f
        }
        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildPrompt(
        mode: VisionMode,
        detailLevel: AiDetailLevel,
        customQuestion: String?
    ): String {
        val brevity = when (detailLevel) {
            AiDetailLevel.CONCISE -> "Keep your response concise and focused on immediate navigation and hazards."
            AiDetailLevel.BALANCED -> "Provide clear, structured description with spatial orientation and distances."
            AiDetailLevel.DETAILED -> "Provide a comprehensive breakdown covering colors, text, relative layout, lighting, and peripheral items."
        }

        return when (mode) {
            VisionMode.SCENE -> """
                You are Drishti, an assistive vision guide for blind and low-vision individuals.
                $brevity
                Describe what is visible in front of the camera clearly, truthfully, and completely:
                1. What is straight ahead in the foreground, center, and background.
                2. Key obstacles, people, furniture, or structures with spatial clock positions and estimated distances.
                3. Walking path status: whether the floor/ground path is clear or has obstructions, steps, cords, or curbs.
                4. Environment type and ambient illumination.
                Do not omit visible details.
            """.trimIndent()

            VisionMode.OCR -> """
                You are Drishti in Text Reading mode for a visually impaired user.
                Carefully read and transcribe ALL visible text from the image verbatim (books, documents, signs, menus, labels, medicine packaging, monitors, or handwriting).
                1. Provide a natural spoken summary first.
                2. Transcribe the full text verbatim in natural reading order.
                3. Highlight critical safety numbers, dosage, expiration dates, warnings, or prices clearly.
                Read all visible text without cutting off.
            """.trimIndent()

            VisionMode.OBJECTS -> """
                You are Drishti in Object Detection mode for a visually impaired user.
                Identify all key objects in the scene.
                Return a JSON object with this exact structure:
                {
                  "spokenSummary": "Comprehensive sentence listing all primary objects and their positions",
                  "objects": [
                    {
                      "label": "Name of object",
                      "position": "Center / Left / Right / Upper / Lower",
                      "distance": "Estimated distance, e.g. 1 meter, 2.5 meters",
                      "confidence": 0.95,
                      "hazard": false,
                      "color": "Color description"
                    }
                  ],
                  "hazardWarning": "Any obstacle or hazard directly in user path or null"
                }
                Return ONLY valid JSON.
            """.trimIndent()

            VisionMode.SPATIAL -> """
                You are Drishti in Spatial & Depth Awareness mode.
                $brevity
                Map the 3D space in front of the user:
                1. Immediate zone (within reach / 0-1m).
                2. Mid zone (1 to 3m).
                3. Far zone (3m+).
                4. Walking path assessment: Is the path straight ahead clear to walk? Note any elevation changes, steps, doorways, or floor objects.
            """.trimIndent()

            VisionMode.ASK -> """
                You are Drishti answering a visually impaired user's question:
                Question: "$customQuestion"
                Answer thoroughly, accurately, and politely based directly on what is seen in the camera frame.
            """.trimIndent()
        }
    }

    private fun buildRequestBody(prompt: String, base64Image: String, mode: VisionMode): JSONObject {
        val root = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()

        val textPart = JSONObject().put("text", prompt)
        parts.put(textPart)

        val imagePart = JSONObject()
        val inlineData = JSONObject()
            .put("mimeType", "image/jpeg")
            .put("data", base64Image)
        imagePart.put("inlineData", inlineData)
        parts.put(imagePart)

        content.put("parts", parts)
        contents.put(content)
        root.put("contents", contents)

        val generationConfig = JSONObject()
        generationConfig.put("temperature", 0.3)
        if (mode == VisionMode.OBJECTS) {
            generationConfig.put("responseMimeType", "application/json")
        }
        root.put("generationConfig", generationConfig)

        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put(
            "text",
            "You are Drishti, an assistive vision and speech guide for blind and low-vision individuals. Always be truthful, accurate, descriptive, and prioritize user navigation safety. Never hallucinate objects or text that are not present."
        ))
        systemInstruction.put("parts", sysParts)
        root.put("systemInstruction", systemInstruction)

        return root
    }

    private fun parseGeminiResponse(responseJson: String, mode: VisionMode): VisionAnalysisResult {
        val json = JSONObject(responseJson)
        val candidates = json.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: "No description generated."

        if (mode == VisionMode.OBJECTS) {
            try {
                val cleanJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val objJson = JSONObject(cleanJson)
                val spokenSummary = objJson.optString("spokenSummary", "Objects identified in your surroundings.")
                val hazardWarning = objJson.optString("hazardWarning").takeIf { it.isNotBlank() && it != "null" }

                val items = mutableListOf<DetectedItem>()
                val objectsArray = objJson.optJSONArray("objects")
                if (objectsArray != null) {
                    for (i in 0 until objectsArray.length()) {
                        val itemObj = objectsArray.getJSONObject(i)
                        items.add(
                            DetectedItem(
                                label = itemObj.optString("label", "Object"),
                                position = itemObj.optString("position", "Center"),
                                distance = itemObj.optString("distance", "Ahead"),
                                confidence = itemObj.optDouble("confidence", 0.9).toFloat(),
                                isHazard = itemObj.optBoolean("hazard", false),
                                color = itemObj.optString("color", "")
                            )
                        )
                    }
                }

                val title = if (items.isNotEmpty()) {
                    "${items.size} Objects Detected"
                } else {
                    "Objects Identified"
                }

                val fullText = buildString {
                    append(spokenSummary)
                    if (items.isNotEmpty()) {
                        append("\n\nItemized Objects:\n")
                        items.forEachIndexed { idx, itm ->
                            append("${idx + 1}. ${itm.label} (${itm.position}, ${itm.distance}")
                            if (itm.color.isNotBlank()) append(", ${itm.color}")
                            if (itm.isHazard) append(" - CAUTION")
                            append(")\n")
                        }
                    }
                }

                return VisionAnalysisResult(
                    title = title,
                    spokenSummary = spokenSummary,
                    detailedText = fullText,
                    detectedObjects = items,
                    hazardWarning = hazardWarning,
                    isFallback = false
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falling back to raw text for OBJECTS mode", e)
            }
        }

        val title = when (mode) {
            VisionMode.SCENE -> "Scene Description"
            VisionMode.OCR -> "Recognized Text"
            VisionMode.OBJECTS -> "Detected Objects"
            VisionMode.SPATIAL -> "Spatial & Depth Map"
            VisionMode.ASK -> "Visual Answer"
        }

        val firstSentence = rawText.lines().firstOrNull { it.isNotBlank() } ?: rawText.take(120)
        val spoken = if (mode == VisionMode.OCR) {
            "Text identified: $firstSentence"
        } else {
            firstSentence
        }

        val hasHazard = rawText.contains("hazard", ignoreCase = true) ||
                rawText.contains("trip", ignoreCase = true) ||
                rawText.contains("step down", ignoreCase = true) ||
                rawText.contains("obstacle", ignoreCase = true)

        return VisionAnalysisResult(
            title = title,
            spokenSummary = spoken,
            detailedText = rawText,
            extractedText = if (mode == VisionMode.OCR) rawText else "",
            hazardWarning = if (hasHazard) "Caution: Obstacle or elevation change noted ahead" else null,
            isFallback = false
        )
    }

    companion object {
        private const val TAG = "GeminiVisionService"
    }
}
