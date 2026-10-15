package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedScans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE id = :id")
    suspend fun getScanById(id: Long): ScanRecord?

    @Query("SELECT * FROM scan_records WHERE title LIKE '%' || :query || '%' OR fullDescription LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanRecord>>

    @Query("SELECT COUNT(*) FROM scan_records")
    fun getTotalScansCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE mode = 'OCR'")
    fun getOcrScansCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE mode = 'OBJECTS'")
    fun getObjectScansCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanRecord): Long

    @Update
    suspend fun updateScan(scan: ScanRecord)

    @Delete
    suspend fun deleteScan(scan: ScanRecord)

    @Query("DELETE FROM scan_records")
    suspend fun clearAllScans()
}
