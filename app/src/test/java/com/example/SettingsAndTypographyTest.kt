package com.example

import com.example.model.AppThemeMode
import com.example.model.UserSettings
import com.example.ui.theme.buildHydroPulseTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndTypographyTest {

    @Test
    fun `default user settings use sleek smaller text scale and system theme`() {
        val defaults = UserSettings()
        assertEquals(0.90f, defaults.textScale, 0.001f)
        assertEquals(AppThemeMode.SYSTEM.name, defaults.themeMode)
    }

    @Test
    fun `typography scales monotonically and clamps extreme values`() {
        val compact = buildHydroPulseTypography(0.80f)
        val standard = buildHydroPulseTypography(1.00f)
        val large = buildHydroPulseTypography(1.25f)
        val clampedLow = buildHydroPulseTypography(0.10f)
        val clampedHigh = buildHydroPulseTypography(5.00f)

        assertTrue(compact.bodyMedium.fontSize.value < standard.bodyMedium.fontSize.value)
        assertTrue(standard.bodyMedium.fontSize.value < large.bodyMedium.fontSize.value)
        assertEquals(compact.bodyMedium.fontSize.value, clampedLow.bodyMedium.fontSize.value, 0.01f)
        assertEquals(large.bodyMedium.fontSize.value, clampedHigh.bodyMedium.fontSize.value, 0.01f)
    }

    @Test
    fun `app theme mode safe parser handles invalid strings gracefully`() {
        assertEquals(AppThemeMode.DARK, AppThemeMode.safeFromString("dark"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.safeFromString("LIGHT"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.safeFromString("unknown"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.safeFromString(null))
    }
}
