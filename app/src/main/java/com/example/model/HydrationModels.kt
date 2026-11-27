package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DrinkType(
    val displayName: String,
    val defaultCupMl: Long
) {
    WATER("Pure Water", 250L),
    SPARKLING("Sparkling", 250L),
    TEA("Herbal Tea", 300L),
    ELECTROLYTE("Electrolytes", 500L);

    companion object {
        fun safeFromString(raw: String?): DrinkType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: WATER
    }
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun safeFromString(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val dailyGoalMl: Long = 2500L,
    val remindersEnabled: Boolean = true,
    val intervalMinutes: Long = 60L,
    val startMinutesFromMidnight: Long = 8 * 60L, // 08:00 AM
    val endMinutesFromMidnight: Long = 22 * 60L,  // 10:00 PM
    val pauseWhenGoalMet: Boolean = false,
    val defaultCupMl: Long = 250L,
    val textScale: Float = 0.90f,
    val themeMode: String = AppThemeMode.SYSTEM.name
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountMl: Long = 250L,
    val drinkType: String = DrinkType.WATER.name,
    val dateKey: String = todayDateKey(),
    val loggedAtEpochMs: Long = 0L
) {
    fun resolvedDrinkType(): DrinkType = DrinkType.safeFromString(drinkType)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DailyHydrationSummary(
    val dateKey: String,
    val shortLabel: String,
    val totalMl: Long,
    val goalMl: Long
)

fun todayDateKey(epochMs: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(epochMs))
}

fun formatMinutesAsClock(minutesFromMidnight: Int): String {
    val clamped = minutesFromMidnight.coerceIn(0, 1439)
    val hour24 = clamped / 60
    val minute = clamped % 60
    val period = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
}
