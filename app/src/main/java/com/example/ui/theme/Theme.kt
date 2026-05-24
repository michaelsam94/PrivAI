package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CosmicMidnight,
    primaryContainer = SlateLight,
    onPrimaryContainer = BrightWhite,
    secondary = ElectricTeal,
    onSecondary = CosmicMidnight,
    tertiary = SteelBlue,
    background = CosmicMidnight,
    onBackground = BrightWhite,
    surface = DarkNavyBlue,
    onSurface = BrightWhite,
    error = AlertRed
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = CyberCyan,
    onPrimary = CosmicMidnight,
    primaryContainer = SlateLight,
    onPrimaryContainer = BrightWhite,
    secondary = ElectricTeal,
    onSecondary = CosmicMidnight,
    tertiary = SteelBlue,
    background = CosmicMidnight, // Absolute dark theme focus for security feel!
    onBackground = BrightWhite,
    surface = DarkNavyBlue,
    onSurface = BrightWhite,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable Android dynamic scheme to preserve custom cyber aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
