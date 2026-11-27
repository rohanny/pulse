package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.TimelineHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.example.R
import com.example.model.DailyHydrationSummary
import com.example.model.UserSettings
import com.example.model.WaterLog
import com.example.model.todayDateKey
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.WeeklyHydrationBarChart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    settings: UserSettings,
    logs: List<WaterLog>,
    bottomContentPadding: Dp,
    onOpenSettings: () -> Unit,
    onUpdateGoal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val summaries = remember(logs, settings.dailyGoalMl) {
        buildLastSevenDaysSummaries(logs, settings.dailyGoalMl)
    }

    val totalsByDate = remember(logs) {
        logs.groupBy { it.dateKey }.mapValues { entry -> entry.value.sumOf { it.amountMl } }
    }

    val currentStreak = remember(totalsByDate, settings.dailyGoalMl) {
        computeStreakDays(totalsByDate, settings.dailyGoalMl)
    }

    val weeklyAverageMl = remember(summaries) {
        if (summaries.isEmpty()) 0L else summaries.sumOf { it.totalMl } / summaries.size
    }

    val daysMetCount = remember(summaries) {
        summaries.count { it.totalMl >= it.goalMl }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 620.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 18.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            StaggeredEntrance(index = 0) {
                TimelineHeader(
                    title = "History",
                    caption = "OVERVIEW",
                    image = R.drawable.img_hydration_hero,
                    imageDescription = "Serene coastal meadow overlooking morning mist and rolling hills",
                    settingsIcon = Icons.Outlined.Settings,
                    settingsDescription = "Open settings",
                    onSettings = onOpenSettings,
                    settingsModifier = Modifier.testTag("history_settings_button")
                )
            }
        }

        item {
            StaggeredEntrance(index = 1) {
                val animStreak by animateIntAsState(
                    targetValue = currentStreak,
                    animationSpec = tween(550),
                    label = "streak_count"
                )
                val animAvg by animateIntAsState(
                    targetValue = weeklyAverageMl.toInt(),
                    animationSpec = tween(600),
                    label = "weekly_avg"
                )
                val animGoals by animateIntAsState(
                    targetValue = daysMetCount,
                    animationSpec = tween(550),
                    label = "days_met"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MinimalMetricColumn(
                        value = "$animStreak d",
                        label = "Streak",
                        modifier = Modifier.weight(1f)
                    )
                    MinimalMetricColumn(
                        value = "$animAvg ml",
                        label = "7d Average",
                        modifier = Modifier.weight(1f)
                    )
                    MinimalMetricColumn(
                        value = "$animGoals / 7",
                        label = "Goals Met",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            StaggeredEntrance(index = 2) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last 7 days",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Goal ${settings.dailyGoalMl} ml",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        WeeklyHydrationBarChart(summaries = summaries)
                    }
                }
            }
        }

        item {
            StaggeredEntrance(index = 3) {
                var sliderGoal by remember(settings.dailyGoalMl) {
                    mutableFloatStateOf(settings.dailyGoalMl.toFloat().coerceIn(1000f, 5000f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily goal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${sliderGoal.roundToInt()} ml",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderGoal,
                        onValueChange = { sliderGoal = it },
                        onValueChangeFinished = {
                            val snapped = ((sliderGoal / 100f).roundToInt() * 100L).coerceIn(1000L, 5000L)
                            onUpdateGoal(snapped)
                        },
                        valueRange = 1000f..5000f
                    )
                }
            }
        }

        item {
            StaggeredEntrance(index = 4) {
                Text(
                    text = if (settings.remindersEnabled) {
                        "View your daily reminder schedule in Alerts."
                    } else {
                        "Reminders paused. All hydration logs are saved locally on this device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MinimalMetricColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildLastSevenDaysSummaries(
    logs: List<WaterLog>,
    goalMl: Long
): List<DailyHydrationSummary> {
    val totalsByDay = logs.groupBy { it.dateKey }.mapValues { entry -> entry.value.sumOf { it.amountMl } }
    val dayFmt = SimpleDateFormat("EEEEE", Locale.US)
    val result = mutableListOf<DailyHydrationSummary>()
    for (daysAgo in 6 downTo 0) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        val key = todayDateKey(cal.timeInMillis)
        val label = if (daysAgo == 0) "Today" else dayFmt.format(cal.time)
        result.add(
            DailyHydrationSummary(
                dateKey = key,
                shortLabel = label,
                totalMl = totalsByDay[key] ?: 0L,
                goalMl = goalMl
            )
        )
    }
    return result
}

private fun computeStreakDays(
    totalsByDate: Map<String, Long>,
    goalMl: Long
): Int {
    var streak = 0
    val cal = Calendar.getInstance()
    val todayKey = todayDateKey(cal.timeInMillis)
    if ((totalsByDate[todayKey] ?: 0L) >= goalMl) {
        streak++
    }
    for (offset in 1..30) {
        val prevCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val key = todayDateKey(prevCal.timeInMillis)
        if ((totalsByDate[key] ?: 0L) >= goalMl) {
            streak++
        } else {
            break
        }
    }
    return streak
}
