package com.michael.privai.playstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michael.privai.ui.screens.HomeScreen
import com.michael.privai.ui.theme.CosmicMidnight
import com.michael.privai.ui.theme.CyberCyan
import com.michael.privai.ui.theme.DarkNavyBlue
import com.michael.privai.ui.theme.MyApplicationTheme
import com.michael.privai.ui.viewmodel.PrivAIViewModel

/** Static add-note dialog over the dashboard — avoids AlertDialog idle loops in Roborazzi. */
@Composable
fun PlayStoreAddNoteOverlay(viewModel: PrivAIViewModel) {
  MyApplicationTheme {
    Box(modifier = Modifier.fillMaxSize()) {
      HomeScreen(
        viewModel = viewModel,
        onNavigateToTranscribe = {},
        onNavigateToOCR = {},
        onNavigateToNoteDetail = {},
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
      ) {
        PlayStoreAddNoteDialog()
      }
    }
  }
}

@Composable
private fun PlayStoreAddNoteDialog() {
  Surface(
    modifier = Modifier
      .padding(24.dp)
      .fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = DarkNavyBlue,
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Secured Note",
        color = CyberCyan,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("add_dialog_title"),
      )
      OutlinedTextField(
        value = "Security Audit Notes",
        onValueChange = {},
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth().testTag("add_dialog_title_field"),
        readOnly = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan),
      )
      OutlinedTextField(
        value = "Document on-device encryption findings for the compliance review.",
        onValueChange = {},
        label = { Text("Write content here...") },
        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("add_dialog_body"),
        readOnly = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan),
      )
      OutlinedTextField(
        value = "Security, Privacy, Work",
        onValueChange = {},
        label = { Text("Tags (comma separated)") },
        modifier = Modifier.fillMaxWidth().testTag("add_dialog_tags"),
        readOnly = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan),
      )
      Button(
        onClick = {},
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CosmicMidnight),
        modifier = Modifier.testTag("add_dialog_confirm"),
      ) {
        Text("Lock Vault")
      }
      TextButton(onClick = {}) {
        Text("Cancel")
      }
    }
  }
}
