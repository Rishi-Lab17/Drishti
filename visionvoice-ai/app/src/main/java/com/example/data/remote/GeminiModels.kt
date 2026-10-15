package com.example.data.remote

data class DetectedItem(
    val label: String,
    val position: String, // e.g. "Center", "To your left", "Ahead 2 meters"
    val distance: String = "Unknown",
    val confidence: Float = 0.9f,
    val isHazard: Boolean = false,
    val color: String = ""
)

data class VisionAnalysisResult(
    val title: String,
    val spokenSummary: String,
    val detailedText: String,
    val detectedObjects: List<DetectedItem> = emptyList(),
    val extractedText: String = "",
    val hazardWarning: String? = null,
    val isFallback: Boolean = false
)
