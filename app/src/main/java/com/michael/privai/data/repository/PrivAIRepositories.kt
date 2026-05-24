package com.michael.privai.data.repository

import com.michael.privai.data.database.*
import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(private val dao: TranscriptionDao) {
    val allTranscriptions: Flow<List<TranscriptionEntity>> = dao.getAllTranscriptions()

    suspend fun getById(id: String): TranscriptionEntity? = dao.getTranscriptionById(id)

    suspend fun insert(entity: TranscriptionEntity) = dao.insertTranscription(entity)

    suspend fun delete(id: String) = dao.deleteTranscription(id)
}

class OCRRepository(private val dao: OCRDao) {
    val allOCRResults: Flow<List<OCREntity>> = dao.getAllOCR()

    suspend fun getById(id: String): OCREntity? = dao.getOCRById(id)

    suspend fun insert(entity: OCREntity) = dao.insertOCR(entity)

    suspend fun delete(id: String) = dao.deleteOCR(id)
}

class SummaryRepository(private val dao: SummaryDao) {
    suspend fun getBySourceId(sourceId: String): SummaryEntity? = dao.getSummaryBySourceId(sourceId)

    suspend fun insert(entity: SummaryEntity) = dao.insertSummary(entity)

    suspend fun delete(id: String) = dao.deleteSummary(id)
}

class NoteRepository(private val dao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()

    suspend fun getById(id: String): NoteEntity? = dao.getNoteById(id)

    suspend fun insert(entity: NoteEntity) = dao.insertNote(entity)

    suspend fun delete(id: String) = dao.deleteNote(id)
}
