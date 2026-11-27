package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.model.AppThemeMode
import com.example.model.UserSettings
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.pressBounce
import kotlin.math.abs
import kotlin.math.roundToInt

private data class TextScalePreset(
    val label: String,
    val scale: Float,
    val testTag: String
)

private val TEXT_SCALE_PRESETS = listOf(
    TextScalePreset("Compact", 0.82f, "text_scale_compact"),
    TextScalePreset("Small", 0.90f, "text_scale_small"),
    TextScalePreset("Standard", 1.00f, "text_scale_standard"),
    TextScalePreset("Large", 1.15f, "text_scale_large")
)

private val DEFAULT_CUP_OPTIONS = listOf(150L, 250L, 350L, 500L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onUpdateSettings: ((UserSettings) -> UserSettings, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderScale by remember(settings.textScale) {
        mutableFloatStateOf(settings.textScale.coerceIn(0.80f, 1.25f))
    }
    var sliderGoal by remember(settings.dailyGoalMl) {
        mutableFloatStateOf(settings.dailyGoalMl.toFloat().coerceIn(1000f, 5000f))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 620.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 14.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StaggeredEntrance(index = 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.pressBounce(backInteraction, pressedScale = 0.90f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            interactionSource = backInteraction,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to history",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "PREFERENCES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        item {
            StaggeredEntrance(index = 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Text size",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(sliderScale * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TEXT_SCALE_PRESETS.forEach { preset ->
                            val isSelected = abs(settings.textScale - preset.scale) < 0.025f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    sliderScale = preset.scale
                                    onUpdateSettings(
                                        { it.copy(textScale = preset.scale) },
                                        "Text size set to ${preset.label}"
                                    )
                                },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag(preset.testTag)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = sliderScale,
                        onValueChange = { sliderScale = it },
                        onValueChangeFinished = {
                            val snapped = ((sliderScale * 20f).roundToInt() / 20f).coerceIn(0.80f, 1.25f)
                            sliderScale = snapped
                            onUpdateSettings(
                                { it.copy(textScale = snapped) },
                                "Text size ${(snapped * 100f).roundToInt()}%"
                            )
                        },
                        valueRange = 0.80f..1.25f,
                        modifier = Modifier.testTag("text_scale_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "250 ml Pure Water • Morning sip",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Adjust scale to fit your preferred reading density across every screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }
        }

        item {
            StaggeredEntrance(index = 2) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentMode = AppThemeMode.safeFromString(settings.themeMode)
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = currentMode == mode,
                                    onClick = {
                                        onUpdateSettings(
                                            { it.copy(themeMode = mode.name) },
                                            "${mode.label} theme applied"
                                        )
                                    },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            StaggeredEntrance(index = 3) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily hydration goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${sliderGoal.roundToInt()} ml",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = sliderGoal,
                            onValueChange = { sliderGoal = it },
                            onValueChangeFinished = {
                                val snapped = ((sliderGoal / 100f).roundToInt() * 100L).coerceIn(1000L, 5000L)
                                onUpdateSettings(
                                    { it.copy(dailyGoalMl = snapped) },
                                    "Daily goal updated to $snapped ml"
                                )
                            },
                            valueRange = 1000f..5000f,
                            modifier = Modifier.testTag("settings_goal_slider")
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Default vessel size",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DEFAULT_CUP_OPTIONS.forEach { cupMl ->
                                FilterChip(
                                    selected = settings.defaultCupMl == cupMl,
                                    onClick = {
                                        onUpdateSettings(
                                            { it.copy(defaultCupMl = cupMl) },
                                            "Default vessel set to $cupMl ml"
                                        )
                                    },
                                    label = { Text("${cupMl}ml") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
