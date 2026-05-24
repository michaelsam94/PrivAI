package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.database.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PrivAIViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: PrivAIViewModel,
    onNavigateToTranscribe: () -> Unit,
    onNavigateToOCR: (Uri?) -> Unit,
    onNavigateToNoteDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsState()
    val transcriptions by viewModel.allTranscriptions.collectAsState()
    val ocrResults by viewModel.allOCRResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Notes, 1: Transcripts, 2: OCR

    // Image Picker for OCR Scans
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onNavigateToOCR(uri)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PrivAI",
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 32.sp
                        )
                        Text(
                            text = "Absolute On-Device Privacy",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.testTag("action_ocr_scan")
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "OCR Scan Image", tint = CyberCyan)
                    }
                    IconButton(
                        onClick = onNavigateToTranscribe,
                        modifier = Modifier.testTag("action_transcribe_mic")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Live Recording", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                containerColor = CyberCyan,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("fab_add_note")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add secured Note")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search secure files...", color = MaterialTheme.colorScheme.onBackground.copy(0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface
                )
            )

            // 2. Metrics card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkNavyBlue,
                border = BorderStroke(1.dp, CyberCyan.copy(0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricItem(count = notes.size.toString(), label = "Notes", icon = Icons.Default.Note)
                    MetricItem(count = transcriptions.size.toString(), label = "Audios", icon = Icons.Default.Audiotrack)
                    MetricItem(count = ocrResults.size.toString(), label = "OCR Scans", icon = Icons.Default.DocumentScanner)
                }
            }

            // 3. Category selectors list
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Workspace Notes", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Audio Transcripts", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("OCR Extracts", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Observable list rendering
            when (selectedTab) {
                0 -> {
                    val filteredNotes = notes.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.body.contains(searchQuery, ignoreCase = true) ||
                                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                    }
                    if (filteredNotes.isEmpty()) {
                        EmptyWorkspaceState(message = "No matching notes. Tap the '+' button to write a secure note!")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { onNavigateToNoteDetail(note.id) },
                                    onDelete = { viewModel.deleteNote(note.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val filteredTranscripts = transcriptions.filter {
                        it.text.contains(searchQuery, ignoreCase = true) ||
                                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                    }
                    if (filteredTranscripts.isEmpty()) {
                        EmptyWorkspaceState(message = "No audio transcribes. Tap the microphone icon at top right to record local meeting speech!")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTranscripts, key = { it.id }) { transcription ->
                                TranscriptionRowItem(
                                    transcription = transcription,
                                    onClick = {
                                        // Auto find corresponding linked workspace notes or pass contents
                                        val linkedNote = notes.firstOrNull { it.linkedTranscriptionId == transcription.id }
                                        if (linkedNote != null) {
                                            onNavigateToNoteDetail(linkedNote.id)
                                        } else {
                                            // Fallback generate and view
                                            viewModel.addNote(
                                                title = "Audio Scan Log",
                                                body = transcription.text,
                                                tags = transcription.tags
                                            )
                                        }
                                    },
                                    onDelete = { viewModel.deleteTranscription(transcription.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    val filteredOCR = ocrResults.filter {
                        it.rawText.contains(searchQuery, ignoreCase = true) ||
                                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                    }
                    if (filteredOCR.isEmpty()) {
                        EmptyWorkspaceState(message = "No OCR documents. Tap scanner at top right to process and scan image PDFs offline!")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredOCR, key = { it.id }) { ocr ->
                                OCRCardRowItem(
                                    ocr = ocr,
                                    onClick = {
                                        val linkedNote = notes.firstOrNull { it.body.contains(ocr.rawText.take(50)) }
                                        if (linkedNote != null) {
                                            onNavigateToNoteDetail(linkedNote.id)
                                        } else {
                                            // Create wrapper note
                                            viewModel.addNote(
                                                title = "Scanned OCR Document",
                                                body = ocr.rawText,
                                                tags = ocr.tags + "OCR"
                                            )
                                        }
                                    },
                                    onDelete = { viewModel.deleteOCRResult(ocr.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick add dialog handler
        if (showQuickAddDialog) {
            QuickAddNoteDialog(
                onDismiss = { showQuickAddDialog = false },
                onAdd = { title, body, tags ->
                    viewModel.addNote(title, body, tags)
                    showQuickAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MetricItem(count: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ElectricTeal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = count, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = CyberCyan)
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = SteelBlue)
    }
}

@Composable
fun EmptyWorkspaceState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PrivacyTip,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = ElectricTeal.copy(0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Locked Secure Space",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SteelBlue,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        border = BorderStroke(0.5.dp, CyberCyan.copy(0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CyberCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = AlertRed.copy(0.8f), modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = note.body,
                fontSize = 14.sp,
                color = BrightWhite.copy(0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Render Tags Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(note.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CyberCyan.copy(0.08f),
                            border = BorderStroke(0.5.dp, ElectricTeal.copy(0.3f))
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = ElectricTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(note.updatedAt),
                    fontSize = 11.sp,
                    color = SteelBlue
                )
            }
        }
    }
}

@Composable
fun TranscriptionRowItem(
    transcription: TranscriptionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("transcription_row_${transcription.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        border = BorderStroke(0.5.dp, ElectricTeal.copy(0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio Workspace Log",
                        fontWeight = FontWeight.Bold,
                        color = BrightWhite
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed.copy(0.8f), modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = transcription.text,
                fontSize = 14.sp,
                color = BrightWhite.copy(0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val secCount = transcription.durationMs / 1000
                Text(
                    text = "Length: ${secCount / 60}:${(secCount % 60).toString().padStart(2, '0')}",
                    fontSize = 12.sp,
                    color = ElectricTeal,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(transcription.createdAt),
                    fontSize = 11.sp,
                    color = SteelBlue
                )
            }
        }
    }
}

@Composable
fun OCRCardRowItem(
    ocr: OCREntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("ocr_item_${ocr.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBlue),
        border = BorderStroke(0.5.dp, CyberCyan.copy(0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "OCR Scan Extract", fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed.copy(0.8f), modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = ocr.rawText,
                fontSize = 14.sp,
                color = BrightWhite.copy(0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(ocr.createdAt),
                    fontSize = 11.sp,
                    color = SteelBlue
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secured Note", color = CyberCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("add_dialog_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Write content here...") },
                    modifier = Modifier.fillMaxWidth().height(140.dp).testTag("add_dialog_body"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_dialog_tags"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsInput.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onAdd(title, body, tags)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CosmicMidnight),
                modifier = Modifier.testTag("add_dialog_confirm")
            ) {
                Text("Lock Vault")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
            ) {
                Text("Cancel")
            }
        },
        containerColor = DarkNavyBlue
    )
}
