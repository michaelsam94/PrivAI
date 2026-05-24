package com.michael.privai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE id = :id LIMIT 1")
    suspend fun getTranscriptionById(id: String): TranscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(entity: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteTranscription(id: String)
}

@Dao
interface OCRDao {
    @Query("SELECT * FROM ocr_results ORDER BY createdAt DESC")
    fun getAllOCR(): Flow<List<OCREntity>>

    @Query("SELECT * FROM ocr_results WHERE id = :id LIMIT 1")
    suspend fun getOCRById(id: String): OCREntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOCR(entity: OCREntity)

    @Query("DELETE FROM ocr_results WHERE id = :id")
    suspend fun deleteOCR(id: String)
}

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE sourceId = :sourceId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getSummaryBySourceId(sourceId: String): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(entity: SummaryEntity)

    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteSummary(id: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entity: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
