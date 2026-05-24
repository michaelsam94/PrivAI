package com.michael.privai.playstore

import com.michael.privai.data.database.NoteEntity
import com.michael.privai.data.database.OCREntity
import com.michael.privai.data.database.TranscriptionEntity
import com.michael.privai.ui.viewmodel.PrivAIViewModel

object PlayStoreTestFixtures {

  const val NOTE_ID_DETAIL = "playstore-note-detail"
  const val SEARCH_QUERY = "privacy"

  private val now = System.currentTimeMillis()

  val sampleNotes = listOf(
    NoteEntity(
      id = NOTE_ID_DETAIL,
      title = "Security Audit Notes",
      body = "Reviewed on-device encryption for voice transcripts and OCR scans. " +
        "All processing stays local — no cloud uploads. Key findings documented for Q2 compliance.",
      audioUri = null,
      linkedTranscriptionId = "playstore-transcript-1",
      tags = listOf("Security", "Privacy", "Work"),
      createdAt = now - 86_400_000L,
      updatedAt = now - 3_600_000L,
    ),
    NoteEntity(
      id = "playstore-note-2",
      title = "Meeting Summary — Product Sync",
      body = "Discussed offline-first roadmap: local speech-to-text, document OCR, and AI summaries without network access.",
      audioUri = null,
      linkedTranscriptionId = null,
      tags = listOf("Work", "Offline"),
      createdAt = now - 172_800_000L,
      updatedAt = now - 172_800_000L,
    ),
    NoteEntity(
      id = "playstore-note-3",
      title = "Research: Private AI Assistants",
      body = "Compared edge inference options for summarization. PrivAI uses lightweight on-device NLP for bullet points and keyword extraction.",
      audioUri = null,
      linkedTranscriptionId = null,
      tags = listOf("Tech", "Privacy"),
      createdAt = now - 259_200_000L,
      updatedAt = now - 259_200_000L,
    ),
  )

  val sampleTranscriptions = listOf(
    TranscriptionEntity(
      id = "playstore-transcript-1",
      text = "We need absolute privacy guarantees for client meetings. Everything must stay on the device with no external API calls.",
      durationMs = 142_000L,
      createdAt = now - 86_400_000L,
      tags = listOf("Privacy", "Work"),
    ),
    TranscriptionEntity(
      id = "playstore-transcript-2",
      text = "The OCR pipeline handles scanned contracts locally. High accuracy mode uses Tesseract when needed.",
      durationMs = 95_000L,
      createdAt = now - 200_000_000L,
      tags = listOf("Legal", "Offline"),
    ),
  )

  val sampleOcrResults = listOf(
    OCREntity(
      id = "playstore-ocr-1",
      rawText = "CONFIDENTIAL — All data processing occurs on-device. No third-party cloud services are used for transcription or document analysis.",
      sourceImageUri = "content://playstore/fixture/scan1",
      createdAt = now - 50_000_000L,
      tags = listOf("Security", "Legal"),
    ),
    OCREntity(
      id = "playstore-ocr-2",
      rawText = "Invoice #4821 — Services rendered under NDA. Payment terms net 30. Contact legal for amendments.",
      sourceImageUri = "content://playstore/fixture/scan2",
      createdAt = now - 120_000_000L,
      tags = listOf("Finance"),
    ),
  )

  suspend fun seed(viewModel: PrivAIViewModel) {
    sampleNotes.forEach { viewModel.noteRepository.insert(it) }
    sampleTranscriptions.forEach { viewModel.transcriptionRepository.insert(it) }
    sampleOcrResults.forEach { viewModel.ocrRepository.insert(it) }
  }
}
