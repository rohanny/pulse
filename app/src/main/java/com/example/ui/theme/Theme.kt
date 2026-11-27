package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Silver,
    onPrimary = OnSilver,
    primaryContainer = SlateGray,
    onPrimaryContainer = OnSlateGray,
    secondary = SoftSilver,
    onSecondary = InkBlack,
    secondaryContainer = CarbonHighest,
    onSecondaryContainer = PaperWhite,
    tertiary = Platinum,
    onTertiary = InkBlack,
    background = InkBlack,
    onBackground = PaperWhite,
    surface = Carbon,
    onSurface = PaperWhite,
    surfaceVariant = CarbonRaised,
    onSurfaceVariant = SoftSilver,
    surfaceContainerLowest = InkBlack,
    surfaceContainerLow = Carbon,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF202020),
    surfaceContainerHigh = CarbonRaised,
    surfaceContainerHighest = CarbonHighest,
    surfaceBright = CarbonHighest,
    surfaceDim = InkBlack,
    surfaceTint = Silver,
    outline = Pewter,
    outlineVariant = CarbonHighest,
    inverseSurface = PaperWhite,
    inverseOnSurface = InkBlack,
    inversePrimary = SlateGray
)

private val LightColorScheme = lightColorScheme(
    primary = HydroAccent,
    onPrimary = HydroOnAccent,
    primaryContainer = HydroAccentSoft,
    onPrimaryContainer = HydroOnAccentSoft,
    secondary = HydroAccent,
    onSecondary = HydroOnAccent,
    secondaryContainer = MistSurfaceVariant,
    onSecondaryContainer = MistOnSurface,
    tertiary = SeafoamMint,
    background = MistBackground,
    onBackground = MistOnSurface,
    surface = MistSurface,
    onSurface = MistOnSurface,
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = MistOnSurfaceVariant,
    outline = MistOutline,
    error = CoralAlert
)

val HydroPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HydroPulseTheme(
    textScale: Float = 0.90f,
    themeMode: String = AppThemeMode.SYSTEM.name,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (AppThemeMode.safeFromString(themeMode)) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = remember(textScale) { buildHydroPulseTypography(textScale) }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = HydroPulseShapes,
            content = {
                CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colorScheme.onBackground, content = content)
            }
        )
    }
}


