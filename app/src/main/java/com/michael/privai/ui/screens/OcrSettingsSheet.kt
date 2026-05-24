package com.michael.privai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michael.privai.ui.theme.CyberCyan
import com.michael.privai.ui.theme.ElectricTeal
import com.michael.privai.ui.theme.SteelBlue
import com.michael.privai.ui.viewmodel.PrivAIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSettingsSheet(
    viewModel: PrivAIViewModel,
    onDismiss: () -> Unit
) {
    val highAccuracy by viewModel.ocrHighAccuracy.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "OCR Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = ElectricTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "High accuracy (slower)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Uses a larger model, deskew, and multiple passes. Best for difficult photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = highAccuracy,
                    onCheckedChange = viewModel::setOcrHighAccuracy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = ElectricTeal.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (highAccuracy) {
                    "Enabled — scans usually finish within 45 seconds."
                } else {
                    "Disabled — fast mode, usually 2–5 seconds."
                },
                style = MaterialTheme.typography.labelMedium,
                color = ElectricTeal
            )
        }
    }
}
