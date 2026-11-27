package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

fun buildHydroPulseTypography(scale: Float = 0.90f): Typography {
    val factor = scale.coerceIn(0.80f, 1.25f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (42f * factor).sp,
            lineHeight = (48f * factor).sp,
            letterSpacing = (-1.0f * factor).sp
        ),
        displayMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (32f * factor).sp,
            lineHeight = (38f * factor).sp,
            letterSpacing = (-0.5f * factor).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * factor).sp,
            lineHeight = (30f * factor).sp,
            letterSpacing = (-0.25f * factor).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * factor).sp,
            lineHeight = (26f * factor).sp
        ),
        titleLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * factor).sp,
            lineHeight = (24f * factor).sp
        ),
        titleMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * factor).sp,
            lineHeight = (21f * factor).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * factor).sp,
            lineHeight = (22f * factor).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13f * factor).sp,
            lineHeight = (19f * factor).sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13f * factor).sp,
            lineHeight = (18f * factor).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * factor).sp,
            lineHeight = (15f * factor).sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5f * factor).sp,
            lineHeight = (13.5f * factor).sp,
            letterSpacing = 0.35.sp
        )
    )
}
