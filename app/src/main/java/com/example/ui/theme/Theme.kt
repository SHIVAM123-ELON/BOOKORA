package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ProfessionalPolishLightColorScheme = lightColorScheme(
    primary = PolishPrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishPrimaryDark,
    secondary = PolishGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = PolishAccentOrange,
    background = PolishBackground,
    onBackground = PolishSlate900,
    surface = PolishSurface,
    onSurface = PolishSlate900,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishSlate600,
    outline = PolishSlate200,
    outlineVariant = PolishSlate100,
    error = PolishError,
    onError = Color.White
)

private val ProfessionalPolishDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = PolishGold,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = PolishAccentOrange,
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),
    error = PolishError,
    onError = Color.White
)

@Composable
fun BookoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ProfessionalPolishDarkColorScheme
        else -> ProfessionalPolishLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = BookoraTheme(darkTheme, dynamicColor, content)
