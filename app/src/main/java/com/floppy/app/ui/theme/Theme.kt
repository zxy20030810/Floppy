package com.floppy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FloppyColors = lightColorScheme(
    primary = Color(0xFF24564A),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5A44),
    onSecondary = Color.White,
    tertiary = Color(0xFF31668A),
    background = Color(0xFFF7F4ED),
    onBackground = Color(0xFF1F2A25),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1F2A25),
    surfaceVariant = Color(0xFFE7E2D5),
    onSurfaceVariant = Color(0xFF4B514A),
    error = Color(0xFFB3261E)
)

@Composable
fun FloppyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FloppyColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
