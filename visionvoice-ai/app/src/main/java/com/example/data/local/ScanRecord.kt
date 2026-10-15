package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VisionMode {
    SCENE,
    OCR,
    OBJECTS,
    SPATIAL,
    ASK
}

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: VisionMode,
    val title: String,
    val speechText: String,
    val fullDescription: String,
    val detectedObjectsJson: String = "[]",
    val extractedText: String = "",
    val isBookmarked: Boolean = false,
    val userNotes: String = "",
    val imageBase64Preview: String? = null
)
