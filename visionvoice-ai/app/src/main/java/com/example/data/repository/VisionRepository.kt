package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.local.AiDetailLevel
import com.example.data.local.ScanRecord
import com.example.data.local.ScanRecordDao
import com.example.data.local.VisionMode
import com.example.data.remote.GeminiVisionService
import com.example.data.remote.VisionAnalysisResult
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class VisionRepository(
    private val geminiService: GeminiVisionService,
    private val scanRecordDao: ScanRecordDao
) {
    val allScans: Flow<List<ScanRecord>> = scanRecordDao.getAllScans()
    val bookmarkedScans: Flow<List<ScanRecord>> = scanRecordDao.getBookmarkedScans()
    val totalScansCount: Flow<Int> = scanRecordDao.getTotalScansCount()
    val ocrScansCount: Flow<Int> = scanRecordDao.getOcrScansCount()
    val objectScansCount: Flow<Int> = scanRecordDao.getObjectScansCount()

    fun searchScans(query: String): Flow<List<ScanRecord>> {
        return scanRecordDao.searchScans(query)
    }

    suspend fun analyzeAndSave(
        bitmap: Bitmap,
        mode: VisionMode,
        detailLevel: AiDetailLevel,
        customQuestion: String? = null,
        apiKeyOverride: String? = null
    ): Pair<VisionAnalysisResult, Long> {
        val result = geminiService.analyzeImage(bitmap, mode, detailLevel, customQuestion, apiKeyOverride)

        // Convert detected objects to JSON string for storage
        val objectsJson = JSONArray().apply {
            result.detectedObjects.forEach { item ->
                put(JSONObject().apply {
                    put("label", item.label)
                    put("position", item.position)
                    put("distance", item.distance)
                    put("confidence", item.confidence.toDouble())
                    put("isHazard", item.isHazard)
                    put("color", item.color)
                })
            }
        }.toString()

        // Create a small thumbnail base64 for history display
        val thumbBase64 = createThumbnail(bitmap)

        val record = ScanRecord(
            mode = mode,
            title = result.title,
            speechText = result.spokenSummary,
            fullDescription = result.detailedText,
            detectedObjectsJson = objectsJson,
            extractedText = result.extractedText,
            imageBase64Preview = thumbBase64
        )

        val id = scanRecordDao.insertScan(record)
        return Pair(result, id)
    }

    suspend fun toggleBookmark(scan: ScanRecord) {
        scanRecordDao.updateScan(scan.copy(isBookmarked = !scan.isBookmarked))
    }

    suspend fun updateScanNotes(scan: ScanRecord, notes: String) {
        scanRecordDao.updateScan(scan.copy(userNotes = notes))
    }

    suspend fun deleteScan(scan: ScanRecord) {
        scanRecordDao.deleteScan(scan)
    }

    suspend fun clearHistory() {
        scanRecordDao.clearAllScans()
    }

    private fun createThumbnail(bitmap: Bitmap): String {
        return try {
            val thumb = Bitmap.createScaledBitmap(bitmap, 120, 120, true)
            val stream = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}
