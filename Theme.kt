package com.taxivn.dispatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TaxiVnLightScheme = lightColorScheme(
    primary = TaxiTealPrimary,
    onPrimary = Color.White,
    primaryContainer = TaxiTealLight,
    onPrimaryContainer = TaxiTealDark,
    secondary = TaxiAmber,
    onSecondary = SlateInk,
    background = SlateBg,
    onBackground = SlateInk,
    surface = SlateSurface,
    onSurface = SlateInk,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    error = TaxiCrimson,
    onError = Color.White
)

@Composable
fun TaxiVnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TaxiVnLightScheme,
        typography = TaxiVnTypography,
        content = content
    )
}
