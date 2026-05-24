package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import com.example.ui.viewmodel.PrivAIViewModel
import com.example.ui.viewmodel.RecordingUiState
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    viewModel: PrivAIViewModel,
    onNavigateBack: () -> Unit
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val scrollState = rememberScrollState()

    // Sound wave pulse animation values
    val infiniteTransition = rememberInfiniteTransition(label = "sound_waves")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Automatically scroll to bottom as transcript text populates
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingUiState.Recording) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secured Voice Capturer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. Live Visualizer View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkNavyBlue)
                    .border(0.5.dp, CyberCyan.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (recordingState is RecordingUiState.Recording) {
                    // Pulsing animated Soundwave bars
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        val barCount = 14
                        val barWidth = 12.dp.toPx()
                        val spacing = 8.dp.toPx()
                        val startOffset = (size.width - (barCount * barWidth + (barCount - 1) * spacing)) / 2f
                        
                        for (i in 0 until barCount) {
                            // Sinusoidal math creates elegant fluid dancing waveform bars centered in the container
                            val heightOffset = sin(pulsePhase + i * 0.5f) * 0.4f + 0.5f
                            val amplitude = if (i % 2 == 0) 140.dp.toPx() else 90.dp.toPx()
                            val barHeight = (heightOffset * amplitude).coerceIn(10.dp.toPx(), size.height)

                            drawRoundRect(
                                color = if (i % 3 == 0) ElectricTeal else CyberCyan,
                                topLeft = Offset(
                                    x = startOffset + i * (barWidth + spacing),
                                    y = (size.height - barHeight) / 2f
                                ),
                                size = Size(width = barWidth, height = barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = CyberCyan.copy(0.06f),
                            border = BorderStroke(1.dp, CyberCyan.copy(0.2f)),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = CyberCyan
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Awaiting Audio Stream",
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            text = "Tap Record below to stream local speech",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            modifier = Modifier.padding(top = 4.txt)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Transcription text stream panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                shape = RoundedCornerShape(16.dp),
                color = DarkNavyBlue,
                border = BorderStroke(0.5.dp, CyberCyan.copy(0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LIVE SPEECH STREAM",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = CyberCyan.copy(0.15f),
                        thickness = 0.5.dp
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        when (val state = recordingState) {
                            is RecordingUiState.Idle -> {
                                Text(
                                    text = "Ready to start live local speech recognition. Your mic stream is processed 100% on device with absolute hardware sandboxing. No connections in manifest.",
                                    color = SteelBlue,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                            is RecordingUiState.Recording -> {
                                Text(
                                    text = if (state.partialText.isBlank()) "Listening..." else state.partialText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                            }
                            is RecordingUiState.Success -> {
                                Text(
                                    text = state.finalResult.text,
                                    color = CyberCyan,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                            }
                            is RecordingUiState.Error -> {
                                Text(
                                    text = "Permission error or hardware block occurred: ${state.message}\n\nFalling back to high-fidelity simulated streaming tests so your development experience is never a dead end!",
                                    color = AlertRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Audio Recorder Control Trigger button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRecording = recordingState is RecordingUiState.Recording
                
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) AlertRed else CyberCyan,
                    modifier = Modifier
                        .size(76.dp)
                        .clickable {
                            if (isRecording) {
                                viewModel.stopLiveRecording()
                            } else {
                                viewModel.startLiveRecording()
                            }
                        }
                        .testTag("record_trigger_btn"),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            modifier = Modifier.size(32.dp),
                            tint = if (isRecording) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

val Int.txt: androidx.compose.ui.unit.Dp get() = this.dp
