package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

// Light Theme — Glacier Mist & Mineral Aqua (soft slate-mist, never pure white)
val HydroAccent = Color(0xFF007EA8)
val HydroOnAccent = Color(0xFFF2F8FC)
val HydroAccentSoft = Color(0xFFC2E5F4)
val HydroOnAccentSoft = Color(0xFF05364A)

val MistBackground = Color(0xFFDCE6EE)
val MistSurface = Color(0xFFEAF1F6)
val MistSurfaceVariant = Color(0xFFCFDDE8)
val MistOnSurface = Color(0xFF142330)
val MistOnSurfaceVariant = Color(0xFF476075)
val MistOutline = Color(0xFFAFC4D6)

// Monochrome dark palette
val InkBlack = Color(0xFF0A0A0A)
val Carbon = Color(0xFF191919)
val CarbonRaised = Color(0xFF292929)
val CarbonHighest = Color(0xFF383838)
val PaperWhite = Color(0xFFF7F7F7)
val SoftSilver = Color(0xFFC6C6C6)
val Pewter = Color(0xFF888888)
val Silver = Color(0xFFE1E1E1)
val DarkWaterBlue = Color(0xFF168BCB)
val OnSilver = Color(0xFF151515)
val SlateGray = Color(0xFF464646)
val OnSlateGray = Color(0xFFF4F4F4)
val Platinum = Color(0xFFF0F0F0)

val AquaGlow = Color(0xFF56CFE1)
val SeafoamMint = Color(0xFF10B981)
val CoralAlert = Color(0xFFE5534B)

internal val MonochromePhotoFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)
