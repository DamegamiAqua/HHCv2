package com.example.cofre.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PxBg = Color(0xFF090D1A)
val PxPanel = Color(0xFF131A30)
val PxInk = Color(0xFF070B16)
val PxGold = Color(0xFFF5C542)
val PxGoldLight = Color(0xFFFFE88A)
val PxGoldDark = Color(0xFFC58F1A)
val PxBlue = Color(0xFF356FE8)
val PxBlueDark = Color(0xFF1A3785)
val PxBlueLight = Color(0xFF9CC9FF)
val PxText = Color(0xFFF5F7FF)
val PxDim = Color(0xFFB2BCD9)
val PxDanger = Color(0xFFFF7777)
val PxSuccess = Color(0xFF71E6A6)

// El monospace anterior reforzaba la sensación de herramienta técnica.
// Un sans-serif del sistema mantiene legibilidad y hace que la UI se sienta más cercana.
private val Friendly = FontFamily.SansSerif

private fun Typography.friendly() = Typography(
    displayLarge = displayLarge.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = displayMedium.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold, letterSpacing = (-0.35).sp),
    displaySmall = displaySmall.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineLarge = headlineLarge.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold),
    headlineMedium = headlineMedium.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold),
    headlineSmall = headlineSmall.copy(fontFamily = Friendly, fontWeight = FontWeight.SemiBold),
    titleLarge = titleLarge.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold),
    titleMedium = titleMedium.copy(fontFamily = Friendly, fontWeight = FontWeight.SemiBold),
    titleSmall = titleSmall.copy(fontFamily = Friendly, fontWeight = FontWeight.SemiBold),
    bodyLarge = bodyLarge.copy(fontFamily = Friendly),
    bodyMedium = bodyMedium.copy(fontFamily = Friendly),
    bodySmall = bodySmall.copy(fontFamily = Friendly),
    labelLarge = labelLarge.copy(fontFamily = Friendly, fontWeight = FontWeight.Bold),
    labelMedium = labelMedium.copy(fontFamily = Friendly, fontWeight = FontWeight.SemiBold),
    labelSmall = labelSmall.copy(fontFamily = Friendly, fontWeight = FontWeight.SemiBold),
)

@Composable
fun CofreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PxGold, onPrimary = PxInk,
            secondary = PxBlue, onSecondary = PxText,
            background = PxBg, onBackground = PxText,
            surface = PxPanel, onSurface = PxText,
            surfaceVariant = PxPanel, onSurfaceVariant = PxDim,
            error = PxDanger, outline = PxBlue,
        ),
        typography = Typography().friendly(),
        content = content,
    )
}
