package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.database.NoteEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkNavyBlue
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.SteelBlue
import com.example.ui.viewmodel.PrivAIViewModel
import com.example.ui.viewmodel.SummaryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: PrivAIViewModel,
    noteId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val notes by viewModel.allNotes.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val scrollState = rememberScrollState()

    // Find note
    val note = remember(noteId, notes) {
        notes.firstOrNull { it.id == noteId }
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CyberCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Locking Vault Session...", color = SteelBlue)
            }
        }
        return
    }

    var isEditMode by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(note.title) }
    var editedBody by remember { mutableStateOf(note.body) }

    // On-Device NLP Local State for this exact note
    var localSummaryBullets by remember { mutableStateOf<List<String>>(emptyList()) }
    var localKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var localSentiment by remember { mutableStateOf("") }
    var isNlpAnalyzed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secured Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyberCyan)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(editedBody))
                            Toast.makeText(context, "Copied note body to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = CyberCyan)
                    }
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.updateNote(
                                    note.copy(
                                        title = editedTitle,
                                        body = editedBody,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                isEditMode = false
                                Toast.makeText(context, "Saved changes locally!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("detail_save_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save changes", tint = ElectricTeal)
                        }
                    } else {
                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier.testTag("detail_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit text", tint = CyberCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            
            // 1. Title Area
            if (isEditMode) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Note Title") },
                    modifier = Modifier.fillMaxWidth().testTag("detail_edit_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
            } else {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = CyberCyan
                )
                Text(
                    text = "Unlocked Local File • Verified Secure",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricTeal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Body Text Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DarkNavyBlue,
                border = BorderStroke(0.5.dp, CyberCyan.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editedBody,
                            onValueChange = { editedBody = it },
                            label = { Text("Note Body") },
                            modifier = Modifier.fillMaxWidth().height(260.dp).testTag("detail_edit_body"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = note.body,
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Tags Chips horizontal view
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Local AI Summaries Trigger Action
            if (!isNlpAnalyzed) {
                Button(
                    onClick = {
                        val result = com.example.domain.summarizer.LocalTextIntelligence.analyzeText(note.body)
                        localSummaryBullets = result.bulletPoints
                        localKeywords = result.keysExtract
                        localSentiment = result.sentiment
                        isNlpAnalyzed = true
                        Toast.makeText(context, "Computed offline on-device summary!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_nlp_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CosmicMidnight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Local AI Analysis", fontWeight = FontWeight.Bold)
                }
            }

            // 5. NLP Analysis Outcomes Render
            AnimatedVisibility(
                visible = isNlpAnalyzed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // Summarized bullet list
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkNavyBlue,
                        border = BorderStroke(0.5.dp, ElectricTeal.copy(0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PRIVAI DOCUMENT SUMMARY",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = ElectricTeal.copy(0.15f), thickness = 0.5.dp)

                            SelectionContainer {
                                Column {
                                    localSummaryBullets.forEach { bullet ->
                                        Text(
                                            text = bullet,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Key concepts extracted
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Extracted Concepts", fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(localKeywords) { kw ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberCyan.copy(0.06f))
                                            .border(0.5.dp, CyberCyan.copy(0.25f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = kw, fontSize = 12.sp, color = CyberCyan)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sentiment Analysis
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SentimentSatisfied,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ElectricTeal
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Sentiment Evaluation:", style = MaterialTheme.typography.labelMedium, color = SteelBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = localSentiment,
                                    color = when (localSentiment) {
                                        "Positive" -> ElectricTeal
                                        "Negative" -> AlertRed
                                        else -> Color.LightGray
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Analysis completed 100% locally underneath isolated sandbox frameworks. Zero logs retained.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

private val CosmicMidnight = Color(0xFF090C15)
private val AlertRed = Color(0xFFFF5252)
