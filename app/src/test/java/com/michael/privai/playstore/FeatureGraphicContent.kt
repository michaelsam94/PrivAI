package com.michael.privai.playstore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.privai.ui.theme.*

@Composable
fun FeatureGraphicContent() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.horizontalGradient(
          colors = listOf(CosmicMidnight, DarkNavyBlue, Color(0xFF0D2137)),
        ),
      ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 48.dp, vertical = 40.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PrivacyTip,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(36.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "PrivAI",
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Absolute On-Device Privacy",
          fontSize = 24.sp,
          fontWeight = FontWeight.SemiBold,
          color = ElectricTeal,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Secure notes, voice transcription & OCR —\n100% offline. Zero cloud uploads.",
          fontSize = 16.sp,
          color = SteelBlue,
          lineHeight = 24.sp,
        )
      }

      Spacer(modifier = Modifier.width(32.dp))

      Surface(
        modifier = Modifier
          .width(220.dp)
          .height(400.dp)
          .clip(RoundedCornerShape(24.dp))
          .border(2.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        color = DarkNavyBlue,
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "PrivAI",
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
            fontSize = 18.sp,
          )
          Text(
            text = "Absolute On-Device Privacy",
            fontSize = 10.sp,
            color = ElectricTeal,
          )
          Spacer(modifier = Modifier.height(4.dp))
          MiniMetricRow(count = "3", label = "Notes", icon = Icons.Default.Note)
          MiniMetricRow(count = "2", label = "Audios", icon = Icons.Default.Lock)
          Spacer(modifier = Modifier.weight(1f))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(72.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(CyberCyan.copy(alpha = 0.08f))
              .border(0.5.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
              .padding(12.dp),
          ) {
            Text(
              text = "Security Audit Notes",
              fontWeight = FontWeight.Bold,
              color = CyberCyan,
              fontSize = 12.sp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MiniMetricRow(
  count: String,
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = icon, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = label, fontSize = 11.sp, color = SteelBlue)
    }
    Text(text = count, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyberCyan)
  }
}
