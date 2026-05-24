package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val durationMs: Long,
    val createdAt: Long,
    val tags: List<String>
)

@Entity(tableName = "ocr_results")
data class OCREntity(
    @PrimaryKey val id: String,
    val rawText: String,
    val sourceImageUri: String,
    val createdAt: Long,
    val tags: List<String>
)

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val summaryText: String,
    val bulletPoints: List<String>,
    val keysExtract: List<String>,
    val sentiment: String,
    val createdAt: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val audioUri: String?,
    val linkedTranscriptionId: String?,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
