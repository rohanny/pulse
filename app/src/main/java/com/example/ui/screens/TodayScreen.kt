package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DrinkType
import com.example.model.UserSettings
import com.example.model.WaterLog
import com.example.model.todayDateKey
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.WaterWaveGauge
import com.example.ui.components.pressBounce
import com.example.ui.theme.AquaGlow
import com.kyant.backdrop.Backdrop
import com.example.ui.theme.MonochromePhotoFilter
import com.example.ui.theme.LocalIsDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class VesselPreset(
    val amountMl: Long,
    val iconRes: Int,
    val label: String
)

private val QUICK_VESSELS = listOf(
    VesselPreset(150L, R.drawable.ic_cup_small, "Cup"),
    VesselPreset(250L, R.drawable.ic_glass_medium, "Glass"),
    VesselPreset(350L, R.drawable.ic_mug_large, "Mug"),
    VesselPreset(500L, R.drawable.ic_bottle_tall, "Bottle")
)

@Composable
fun TodayScreen(
    settings: UserSettings,
    logs: List<WaterLog>,
    nextReminderEpochMs: Long,
    hasNotificationPermission: Boolean,
    bottomContentPadding: Dp,
    onLogWater: (Long, DrinkType) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onOpenRemindersTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = remember { todayDateKey() }
    val todayLogs = remember(logs, todayKey) {
        logs.filter { it.dateKey == todayKey }
    }
    val todayTotalMl = remember(todayLogs) {
        todayLogs.sumOf { it.amountMl }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    var showCustomDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 620.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 18.dp,
            bottom = bottomContentPadding
        )
    ) {
        item {
            StaggeredEntrance(index = 0) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val heroBackdrop = rememberLayerBackdrop()
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(heroBackdrop)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_today_hero),
                                colorFilter = if (LocalIsDarkTheme.current) MonochromePhotoFilter else null,
                                contentDescription = "Misty alpine pine forest and mountain ridges at dawn",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.34f))
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = SimpleDateFormat("EEEE, MMM d", Locale.US)
                                        .format(Date())
                                        .uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hydration",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                            }

                            MinimalReminderStatus(
                                backdrop = heroBackdrop,
                                remindersEnabled = settings.remindersEnabled,
                                hasPermission = hasNotificationPermission,
                                nextReminderEpochMs = nextReminderEpochMs,
                                nowMs = nowMs,
                                onClick = onOpenRemindersTab
                            )
                        }
                    }
                }
            }
        }

        item {
            StaggeredEntrance(index = 1) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WaterWaveGauge(
                            currentMl = todayTotalMl,
                            goalMl = settings.dailyGoalMl
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val remaining = (settings.dailyGoalMl - todayTotalMl).coerceAtLeast(0L)
                        AnimatedContent(
                            targetState = remaining == 0L,
                            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                            label = "goal_remaining_label"
                        ) { reached ->
                            Text(
                                text = if (!reached) {
                                    "$remaining ml left today"
                                } else {
                                    "Daily goal reached"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        item {
            StaggeredEntrance(index = 2) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick add",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = { showCustomDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("custom_drink_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QUICK_VESSELS.forEachIndexed { idx, preset ->
                            StaggeredEntrance(
                                index = 2 + idx,
                                modifier = Modifier.weight(1f)
                            ) {
                                MinimalQuickAddButton(
                                    preset = preset,
                                    onClick = { onLogWater(preset.amountMl, DrinkType.WATER) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        item {
            StaggeredEntrance(index = 3) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        AnimatedContent(
                            targetState = todayLogs.size,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "log_entry_count"
                        ) { count ->
                            if (count > 0) {
                                Text(
                                    text = "$count ${if (count == 1) "entry" else "entries"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.size(1.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (todayLogs.isEmpty()) {
            item(key = "empty_hydration_state") {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_hydration),
                            colorFilter = if (LocalIsDarkTheme.current) MonochromePhotoFilter else null,
                            contentDescription = "Smooth river pebbles stacked beside forest moss in morning light",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Your glass is waiting",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap any vessel above to record your first drink.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(
                items = todayLogs,
                key = { _, item -> item.id }
            ) { index, log ->
                Column(modifier = Modifier.animateItem()) {
                    MinimalWaterLogRow(
                        log = log,
                        onDelete = { if (log.id > 0L) onDeleteLog(log.id) }
                    )
                    if (index < todayLogs.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomDrinkDialog(
            defaultCupMl = settings.defaultCupMl,
            onDismiss = { showCustomDialog = false },
            onConfirm = { amountMl, drinkType ->
                showCustomDialog = false
                onLogWater(amountMl, drinkType)
            }
        )
    }
}

@Composable
private fun MinimalReminderStatus(
    backdrop: Backdrop,
    remindersEnabled: Boolean,
    hasPermission: Boolean,
    nextReminderEpochMs: Long,
    nowMs: Long,
    onClick: () -> Unit
) {
    val active = remindersEnabled && hasPermission
    val label = when {
        !remindersEnabled -> "Paused"
        !hasPermission -> "Allow alerts"
        nextReminderEpochMs > nowMs -> {
            val minsLeft = ((nextReminderEpochMs - nowMs) / 60_000L).coerceAtLeast(1L)
            if (minsLeft < 180) "In ${minsLeft}m" else SimpleDateFormat("h:mm a", Locale.US).format(Date(nextReminderEpochMs))
        }
        else -> "Active"
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressBounce(interactionSource, pressedScale = 0.93f)
            .minimumInteractiveComponentSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(10f.dp.toPx(), 22f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.14f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.48f), CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("reminder_status_chip"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active && com.example.ui.theme.LocalIsDarkTheme.current) MaterialTheme.colorScheme.primary
                        else if (active) AquaGlow
                        else Color.White.copy(alpha = 0.45f)
                    )
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MinimalQuickAddButton(
    preset: VesselPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quick_add_scale_${preset.amountMl}"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .minimumInteractiveComponentSize()
            .testTag("quick_add_${preset.amountMl}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = preset.iconRes),
                contentDescription = preset.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "+${preset.amountMl}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalWaterLogRow(
    log: WaterLog,
    onDelete: () -> Unit
) {
    val type = log.resolvedDrinkType()
    val timeText = remember(log.loggedAtEpochMs) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(log.loggedAtEpochMs))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("water_log_item"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val vesselIcon = when {
                log.amountMl <= 180L -> R.drawable.ic_cup_small
                log.amountMl <= 300L -> R.drawable.ic_glass_medium
                log.amountMl <= 420L -> R.drawable.ic_mug_large
                else -> R.drawable.ic_bottle_tall
            }
            Icon(
                painter = painterResource(id = vesselIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${log.amountMl} ml",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (type != DrinkType.WATER) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove hydration entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomDrinkDialog(
    defaultCupMl: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, DrinkType) -> Unit
) {
    var amountText by remember { mutableStateOf(defaultCupMl.toString()) }
    var selectedType by remember { mutableStateOf(DrinkType.WATER) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom amount",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }.take(4)
                        validationError = null
                    },
                    label = { Text("Milliliters") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_amount_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrinkType.entries.chunked(2).forEach { rowTypes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowTypes.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(type.displayName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.toLongOrNull()
                    if (parsed == null || parsed !in 10L..5000L) {
                        validationError = "Enter 10 to 5000 ml"
                    } else {
                        onConfirm(parsed, selectedType)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_drink_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
