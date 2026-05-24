package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import com.example.ui.viewmodel.OCRUiState
import com.example.ui.viewmodel.PrivAIViewModel
import com.example.ui.viewmodel.SummaryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRScreen(
    viewModel: PrivAIViewModel,
    imageUriString: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val ocrState by viewModel.ocrState.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val scrollState = rememberScrollState()

    val imageUri = remember(imageUriString) {
        if (!imageUriString.isNullOrBlank()) Uri.parse(imageUriString) else null
    }

    // Process OCR once URI is parsed
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            viewModel.performOCR(imageUri)
        }
    }

    // Reset state on entry or exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetOCRState()
            viewModel.resetSummaryState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secured Document OCR Scans", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ocr_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = CyberCyan)
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Doc Image thumbnail view (if available)
            if (imageUri != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = DarkNavyBlue,
                    border = BorderStroke(1.dp, CyberCyan.copy(0.2f))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Scanned document",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                EmptyOCRStatePrompt()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. OCR analysis status / results card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DarkNavyBlue,
                border = BorderStroke(0.5.dp, CyberCyan.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EXTRACTED RAW CONTENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = CyberCyan.copy(0.15f), thickness = 0.5.dp)

                    when (val state = ocrState) {
                        is OCRUiState.Idle -> {
                            Text(
                                "No document currently queued. Return to workspace and tap the scanner icon to choose a document image.",
                                color = SteelBlue,
                                fontSize = 14.sp
                            )
                        }
                        is OCRUiState.Scanning -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = CyberCyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Processing Optical Characters...",
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Locked sandbox computing • 100% offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SteelBlue,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        is OCRUiState.Success -> {
                            val scannedData = state.extractedResult
                            Column {
                                SelectionContainer {
                                    Text(
                                        text = scannedData.rawText,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                // Actions row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(scannedData.rawText))
                                            Toast.makeText(context, "Copied securely to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CosmicMidnight),
                                        modifier = Modifier.weight(1f).testTag("ocr_copy_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Text", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.generateLocalSummary(scannedData.id, scannedData.rawText)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal, contentColor = CosmicMidnight),
                                        modifier = Modifier.weight(1f).testTag("ocr_summarize_btn")
                                    ) {
                                        Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Summarize", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        is OCRUiState.Error -> {
                            Text(
                                text = "Fatal OCR scan failure: ${state.message}",
                                color = AlertRed,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 3. Summarizer result display (if requested)
            AnimatedVisibility(
                visible = summaryState !is SummaryUiState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkNavyBlue,
                    border = BorderStroke(0.5.dp, ElectricTeal.copy(0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LOCAL AI SUMMARY ANALYSIS",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = ElectricTeal.copy(0.15f), thickness = 0.5.dp)

                        when (val sumState = summaryState) {
                            is SummaryUiState.Summarizing -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ElectricTeal)
                                }
                            }
                            is SummaryUiState.Success -> {
                                val s = sumState.summary
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = ElectricTeal
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Tone Mood: ${s.sentiment}", color = ElectricTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    SelectionContainer {
                                        Column {
                                            s.bulletPoints.forEach { pt ->
                                                Text(
                                                    text = pt,
                                                    color = BrightWhite,
                                                    fontSize = 14.sp,
                                                    lineHeight = 20.sp,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Processed completely locally inside your private database instance. No metadata transferred.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SteelBlue
                                    )
                                }
                            }
                            is SummaryUiState.Error -> {
                                Text("Parsing failure: ${sumState.message}", color = AlertRed, fontSize = 14.sp)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyOCRStatePrompt() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkNavyBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(48.dp), tint = CyberCyan.copy(0.4f))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Document Not Loaded", color = SteelBlue, fontSize = 14.sp)
    }
}

// Visual layout helper objects to avoid compiler warning issues on missing simple dependencies
private val CosmicMidnight = Color(0xFF090C15)
private val AlertRed = Color(0xFFFF5252)

@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}
