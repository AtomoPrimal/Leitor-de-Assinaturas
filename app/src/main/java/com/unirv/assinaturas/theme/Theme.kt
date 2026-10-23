package com.unirv.assinaturas.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Obrigatoriamente Light Mode conforme Seção 7 do Enunciado N2
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SlateSurface,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueDark,
    secondary = SlateTextSecondary,
    onSecondary = SlateSurface,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateBackground,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = CoralDanger,
    onError = SlateSurface
)

@Composable
fun LeitorAssinaturasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
