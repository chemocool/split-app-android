package com.chemocool.split.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Couleurs reprises de la maquette Figma « Split » (Material 3, primaire sarcelle).
private val LightColors = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EF2E3),
    onPrimaryContainer = Color(0xFF00201C),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF051F1C),
    tertiary = Color(0xFF9C4056),
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF3F0016),
    error = Color(0xFFB3261E),
    background = Color(0xFFF4FBF8),
    onBackground = Color(0xFF161D1B),
    surface = Color(0xFFF4FBF8),
    onSurface = Color(0xFF161D1B),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F2),
    surfaceContainer = Color(0xFFE9EFEC),
    surfaceContainerHigh = Color(0xFFE3EAE7),
    surfaceContainerHighest = Color(0xFFDDE4E1),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5C8),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF9EF2E3),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFFFB1C1),
    tertiaryContainer = Color(0xFF7E2940),
    onTertiaryContainer = Color(0xFFFFD9DF),
    error = Color(0xFFF2B8B5),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E1),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1B),
    surfaceContainer = Color(0xFF1A211F),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF2F3634),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
)

/** Couleurs propres à l'app : solde positif (on me doit) / négatif (je dois). */
@Immutable
data class SplitColors(val positive: Color, val negative: Color)

val LocalSplitColors = staticCompositionLocalOf { SplitColors(Color(0xFF006A60), Color(0xFFB3261E)) }

@Composable
fun SplitTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val extra = if (darkTheme) SplitColors(Color(0xFF80D5C8), Color(0xFFF2B8B5))
    else SplitColors(Color(0xFF006A60), Color(0xFFB3261E))
    CompositionLocalProvider(LocalSplitColors provides extra) {
        // Roboto est la police par défaut d'Android : la typographie Material 3 standard suffit.
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
