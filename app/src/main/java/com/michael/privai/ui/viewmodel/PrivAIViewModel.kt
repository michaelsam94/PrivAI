package com.michael.privai.ui.viewmodel

import android.content.Context
import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michael.privai.data.database.*
import com.michael.privai.data.repository.*
import com.michael.privai.domain.scanner.AndroidSpeechTranscriber
import com.michael.privai.domain.scanner.SpeechTranscriptionEvent
import com.michael.privai.data.preferences.OcrPreferences
import com.michael.privai.domain.scanner.DocumentOCREngine
import com.michael.privai.domain.scanner.OcrImageCache
import com.michael.privai.domain.scanner.TesseractSessionHolder
import com.michael.privai.domain.summarizer.LocalTextIntelligence
import com.michael.privai.domain.summarizer.SummaryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface RecordingUiState {
    object Idle : RecordingUiState
    data class Recording(
        val partialText: String,
        val activeLanguages: String? = null
    ) : RecordingUiState
    data class Success(val finalResult: TranscriptionEntity) : RecordingUiState
    data class Error(val message: String) : RecordingUiState
}

sealed interface OCRUiState {
    object Idle : OCRUiState
    object Scanning : OCRUiState
    data class Success(val extractedResult: OCREntity) : OCRUiState
    data class Error(val message: String) : OCRUiState
}

sealed interface SummaryUiState {
    object Idle : SummaryUiState
    object Summarizing : SummaryUiState
    data class Success(val summary: SummaryEntity) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

class PrivAIViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PrivAIDatabase.getDatabase(application)
    
    val noteRepository = NoteRepository(database.noteDao())
    val transcriptionRepository = TranscriptionRepository(database.transcriptionDao())
    val ocrRepository = OCRRepository(database.ocrDao())
    val summaryRepository = SummaryRepository(database.summaryDao())

    private val ocrEngine by lazy { DocumentOCREngine(application) }
    private val ocrPreferences = OcrPreferences(application)
    private var ocrJob: Job? = null

    val ocrHighAccuracy: StateFlow<Boolean> = ocrPreferences.highAccuracyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Flow lists
    val allNotes: StateFlow<List<NoteEntity>> = noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTranscriptions: StateFlow<List<TranscriptionEntity>> = transcriptionRepository.allTranscriptions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOCRResults: StateFlow<List<OCREntity>> = ocrRepository.allOCRResults
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // UI Interactive States
    private val _recordingState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val recordingState: StateFlow<RecordingUiState> = _recordingState.asStateFlow()

    private val _ocrState = MutableStateFlow<OCRUiState>(OCRUiState.Idle)
    val ocrState: StateFlow<OCRUiState> = _ocrState.asStateFlow()

    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    // Search results or queries
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Live listening controls
    private var listeningJob: Job? = null
    private var recordingStartTime = 0L

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun reportRecordingPermissionDenied() {
        _recordingState.value = RecordingUiState.Error(
            "Microphone permission is required for voice capture. Grant access when prompted, or enable it in Settings."
        )
    }

    /**
     * Start live speech transcribing with microphone amplitude simulation
     */
    fun startLiveRecording(speechContext: Context) {
        if (!hasMicrophonePermission()) {
            reportRecordingPermissionDenied()
            return
        }

        listeningJob?.cancel()
        _recordingState.value = RecordingUiState.Recording("")
        recordingStartTime = System.currentTimeMillis()

        listeningJob = viewModelScope.launch {
            try {
                var committedText = ""
                var livePartial = ""
                var activeLanguages: String? = null

                AndroidSpeechTranscriber(speechContext).startContinuousListeningFlow()
                    .collect { event ->
                        when (event) {
                            is SpeechTranscriptionEvent.LanguagesReady -> {
                                activeLanguages = event.locales.displayLabel
                                _recordingState.value = RecordingUiState.Recording(
                                    partialText = buildLiveTranscript(committedText, livePartial),
                                    activeLanguages = activeLanguages
                                )
                            }
                            is SpeechTranscriptionEvent.Partial -> {
                                livePartial = event.text
                                _recordingState.value = RecordingUiState.Recording(
                                    partialText = buildLiveTranscript(committedText, livePartial),
                                    activeLanguages = activeLanguages
                                )
                            }
                            is SpeechTranscriptionEvent.FinalSegment -> {
                                committedText = appendTranscriptSegment(committedText, event.text)
                                livePartial = ""
                                _recordingState.value = RecordingUiState.Recording(
                                    partialText = committedText,
                                    activeLanguages = activeLanguages
                                )
                            }
                        }
                    }
            } catch (_: CancellationException) {
                // User tapped stop — stopLiveRecording handles persistence.
            } catch (e: Exception) {
                _recordingState.value = RecordingUiState.Error(e.message ?: "Failed during transcription")
            }
        }
    }

    private fun buildLiveTranscript(committed: String, partial: String): String {
        val trimmedPartial = partial.trim()
        return when {
            committed.isBlank() -> trimmedPartial
            trimmedPartial.isBlank() -> committed
            else -> "$committed $trimmedPartial"
        }
    }

    private fun appendTranscriptSegment(existing: String, segment: String): String {
        val trimmed = segment.trim()
        if (trimmed.isEmpty()) return existing
        return if (existing.isBlank()) trimmed else "$existing $trimmed"
    }

    fun stopLiveRecording() {
        val current = _recordingState.value
        listeningJob?.cancel()
        listeningJob = null

        if (current is RecordingUiState.Recording) {
            val text = current.partialText
            if (text.isNotBlank()) {
                viewModelScope.launch {
                    val finalId = UUID.randomUUID().toString()
                    val duration = System.currentTimeMillis() - recordingStartTime
                    val entity = TranscriptionEntity(
                        id = finalId,
                        text = text.trim(),
                        durationMs = duration,
                        createdAt = System.currentTimeMillis(),
                        tags = extractTagsFromText(text)
                    )
                    transcriptionRepository.insert(entity)
                    _recordingState.value = RecordingUiState.Success(entity)
                    createNoteFromTranscription(entity)
                }
            } else {
                _recordingState.value = RecordingUiState.Idle
            }
        }
    }

    fun resetRecordingState() {
        _recordingState.value = RecordingUiState.Idle
    }

    /**
     * Process Image file for OCR
     */
    fun performOCR(uri: Uri) {
        ocrJob?.cancel()
        ocrJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _ocrState.value = OCRUiState.Scanning
            }
            try {
                ensureActive()
                val localUri = withContext(Dispatchers.IO) {
                    OcrImageCache.ensureLocalCopy(getApplication(), uri)
                }
                val highAccuracy = ocrHighAccuracy.value
                withContext(Dispatchers.IO) {
                    runCatching {
                        TesseractSessionHolder.warmUp(getApplication(), highAccuracy = highAccuracy)
                    }
                }
                val extractedText = ocrEngine.extractText(localUri, highAccuracy)
                withContext(Dispatchers.Main) {
                    if (extractedText.isNotBlank()) {
                        val entity = OCREntity(
                            id = UUID.randomUUID().toString(),
                            rawText = extractedText.trim(),
                            sourceImageUri = uri.toString(),
                            createdAt = System.currentTimeMillis(),
                            tags = extractTagsFromText(extractedText)
                        )
                        ocrRepository.insert(entity)
                        _ocrState.value = OCRUiState.Success(entity)
                    } else {
                        _ocrState.value = OCRUiState.Error("No readable words found in this image.")
                    }
                }
            } catch (_: CancellationException) {
                // New scan started or screen closed — ignore.
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _ocrState.value = OCRUiState.Error(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: "OCR failed. Try a clearer photo with good lighting."
                    )
                }
            }
        }
    }

    fun setOcrHighAccuracy(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ocrPreferences.setHighAccuracyEnabled(enabled)
            TesseractSessionHolder.release()
            runCatching {
                TesseractSessionHolder.warmUp(getApplication(), highAccuracy = enabled)
            }
        }
    }

    fun resetOCRState() {
        ocrJob?.cancel()
        ocrJob = null
        _ocrState.value = OCRUiState.Idle
    }

    /**
     * Perform local AI NLP summarization
     */
    fun generateLocalSummary(sourceId: String, textToSummarize: String) {
        _summaryState.value = SummaryUiState.Summarizing
        viewModelScope.launch {
            try {
                val result: SummaryResult = LocalTextIntelligence.analyzeText(textToSummarize)
                val summaryEntity = SummaryEntity(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    summaryText = result.summaryText,
                    bulletPoints = result.bulletPoints,
                    keysExtract = result.keysExtract,
                    sentiment = result.sentiment,
                    createdAt = System.currentTimeMillis()
                )
                summaryRepository.insert(summaryEntity)
                _summaryState.value = SummaryUiState.Success(summaryEntity)

                // Also update any notes linking to this source context with the generated summaries!
                val notes = allNotes.value
                val existingNote = notes.firstOrNull { it.linkedTranscriptionId == sourceId }
                if (existingNote != null) {
                    val updatedNote = existingNote.copy(
                        body = existingNote.body + "\n\n📄 LOCAL AI SUMMARY:\n" + summaryEntity.summaryText + "\n\n" + summaryEntity.bulletPoints.joinToString("\n"),
                        updatedAt = System.currentTimeMillis()
                    )
                    noteRepository.insert(updatedNote)
                }
            } catch (e: Exception) {
                _summaryState.value = SummaryUiState.Error(e.message ?: "Failed to compute summaries")
            }
        }
    }

    fun resetSummaryState() {
        _summaryState.value = SummaryUiState.Idle
    }

    // --- Note Management ---
    fun addNote(title: String, body: String, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Untitled Scan" },
                body = body,
                audioUri = null,
                linkedTranscriptionId = null,
                tags = tags,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.insert(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.insert(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteRepository.delete(id)
        }
    }

    fun deleteTranscription(id: String) {
        viewModelScope.launch {
            transcriptionRepository.delete(id)
        }
    }

    fun deleteOCRResult(id: String) {
        viewModelScope.launch {
            ocrRepository.delete(id)
        }
    }

    // --- Helpers ---
    private fun createNoteFromTranscription(transcription: TranscriptionEntity) {
        viewModelScope.launch {
            val title = "Transcription - ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(transcription.createdAt)}"
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                body = transcription.text,
                audioUri = null,
                linkedTranscriptionId = transcription.id,
                tags = transcription.tags + "Voice",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.insert(note)
        }
    }

    private fun extractTagsFromText(text: String): List<String> {
        val words = text.lowercase().split(Regex("[^a-zA-Z]+")).filter { it.length > 4 }
        val commonTags = mapOf(
            "security" to "Security", "privacy" to "Privacy", "local" to "Offline", 
            "meeting" to "Work", "developer" to "Code", "research" to "Tech",
            "medical" to "Health", "legal" to "Legal", "financial" to "Finance"
        )
        val extracted = mutableSetOf<String>()
        for (word in words) {
            val tag = commonTags[word]
            if (tag != null) {
                extracted.add(tag)
            }
        }
        if (extracted.isEmpty()) {
            extracted.add("General")
        }
        return extracted.toList()
    }
}
