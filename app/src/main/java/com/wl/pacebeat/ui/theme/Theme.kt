package com.wl.pacebeat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PaceColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = OrangeSoft,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = Color.White,
    secondaryContainer = Panel,
    onSecondaryContainer = Ink,
    tertiary = Green,
    onTertiary = Color.White,
    background = Background,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Panel,
    onSurfaceVariant = InkSoft,
    surfaceContainer = CardWhite,
    surfaceContainerLow = Color(0xFFFAFAFB),
    outline = Divider,
    outlineVariant = Track,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger
)

private val PaceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PaceBeatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaceColors,
        typography = PaceTypography,
        shapes = PaceShapes,
        content = content
    )
}
