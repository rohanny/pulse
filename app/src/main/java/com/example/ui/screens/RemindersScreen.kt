package com.example.ui.screens

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import com.example.ui.components.TimelineHeader
import androidx.compose.foundation.verticalScroll
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_BOTH
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_INTERVAL
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_NONE
import com.example.data.LocalReminderSnapshot.Companion.REMINDER_MODE_SCHEDULED
import com.example.data.firstOverlap
import com.example.data.IntervalWindow
import com.example.data.ReminderPrefsStore
import com.example.model.UserSettings
import com.example.notifications.WaterReminderScheduler
import java.util.Calendar
import java.util.Date

private enum class Editor { Add, Fixed, Window, Options }
private data class AlertRow(val start: Long, val window: IntervalWindow? = null)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    settings: UserSettings,
    nextReminderEpochMs: Long,
    hasNotificationPermission: Boolean,
    bottomContentPadding: Dp,
    onRequestNotificationPermission: () -> Unit,
    onUpdateSettings: ((UserSettings) -> UserSettings, String?) -> Unit,
    onScheduleChanged: () -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember(context) { ReminderPrefsStore(context) }
    val initial = remember(store) { store.readSnapshot() }
    var mode by remember { mutableStateOf(initial.reminderMode) }
    var times by remember { mutableStateOf(initial.scheduledTimesMinutes) }
    var windows by remember { mutableStateOf(initial.windows) }
    val fixedOn = mode == REMINDER_MODE_SCHEDULED || mode == REMINDER_MODE_BOTH
    val activeTimes = if (fixedOn) times else emptyList()
    val rows = buildList {
        activeTimes.forEach { add(AlertRow(it)) }
        windows.forEach { add(AlertRow(it.start, it)) }
    }.sortedBy { it.start }
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var original by rememberSaveable { mutableStateOf<Long?>(null) }
    var windowId by rememberSaveable { mutableStateOf<String?>(null) }
    var start by rememberSaveable { mutableLongStateOf(0) }
    var end by rememberSaveable { mutableLongStateOf(0) }
    var minutes by rememberSaveable { mutableLongStateOf(settings.intervalMinutes) }
    var pickingDuration by rememberSaveable { mutableStateOf(false) }
    val clock = DateFormat.getTimeFormat(context)
    fun label(value: Long): String = clock.format(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, (value / 60).toInt())
        set(Calendar.MINUTE, (value % 60).toInt())
    }.time)

    fun openEditor(kind: Editor, existing: Long? = null, window: IntervalWindow? = null) {
        windowId = window?.id
        original = existing
        start = if (kind == Editor.Window) window?.start ?: settings.startMinutesFromMidnight else existing ?: 480L
        end = window?.end ?: settings.endMinutesFromMidnight
        minutes = window?.minutes ?: settings.intervalMinutes
        editor = kind
    }

    fun pick(title: String, value: Long, save: (Long) -> Unit) {
        TimePickerDialog(context, { _, hour, minute -> save(hour * 60L + minute) },
            (value / 60).toInt(), (value % 60).toInt(), DateFormat.is24HourFormat(context)
        ).apply {
            setTitle(title)
            show()
        }
    }

    fun savePlan(updatedWindows: List<IntervalWindow>, fixed: List<Long>) {
        windows = updatedWindows
        val sorted = fixed.distinct().sorted()
        mode = when {
            windows.isNotEmpty() && sorted.isNotEmpty() -> REMINDER_MODE_BOTH
            windows.isNotEmpty() -> REMINDER_MODE_INTERVAL
            sorted.isNotEmpty() -> REMINDER_MODE_SCHEDULED
            else -> REMINDER_MODE_NONE
        }
        // Keep dormant legacy times when only the interval window is edited.
        if (fixedOn || sorted.isNotEmpty()) times = sorted
        store.savePlan(mode, times, windows)
        WaterReminderScheduler.scheduleFromStoredSnapshot(context)
        onScheduleChanged()
        editor = null
    }

    val scheme = MaterialTheme.colorScheme
    LazyColumn(modifier.widthIn(max = 620.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = bottomContentPadding)) {
        item {
            val status = when {
                !settings.remindersEnabled -> "Reminders paused"
                !hasNotificationPermission -> "Notifications blocked"
                rows.isEmpty() -> "No alerts scheduled"
                nextReminderEpochMs > 0 -> {
                    val day = if (android.text.format.DateUtils.isToday(nextReminderEpochMs)) "today" else DateFormat.getMediumDateFormat(context).format(Date(nextReminderEpochMs))
                    "Next alert $day at ${clock.format(Date(nextReminderEpochMs))}"
                }
                else -> "Daily reminders on"
            }
            TimelineHeader(
                title = "Alerts",
                caption = status,
                image = R.drawable.img_reminders_art,
                imageDescription = null,
                settingsIcon = Icons.Outlined.Tune,
                settingsDescription = "Notification settings",
                onSettings = { editor = Editor.Options }
            )
        }
        if (!hasNotificationPermission || (!settings.remindersEnabled && rows.isNotEmpty())) item {
            TextButton(onClick = {
                if (!hasNotificationPermission) onRequestNotificationPermission()
                else onUpdateSettings({ it.copy(remindersEnabled = true) }, null)
            }) { Text(if (!hasNotificationPermission) "Allow notifications" else "Resume reminders") }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Daily timeline", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, color = scheme.onBackground)
                Button(onClick = { openEditor(Editor.Add) }) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add alert")
                }
            }
        }
        if (rows.isEmpty()) item {
            Surface(color = scheme.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Schedule, null, tint = scheme.primary)
                    Text("Your day is open", style = MaterialTheme.typography.titleLarge)
                    Text("Add a fixed time or a repeating interval to start your daily schedule.",
                        style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                }
            }
        }
        itemsIndexed(rows, key = { _, row -> row.window?.id ?: "fixed-${row.start}" }) { index, row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.width(26.dp).fillMaxHeight()) {
                    val x = 8.dp.toPx()
                    if (index > 0) drawLine(scheme.outline, Offset(x, 0f), Offset(x, center.y), 2.dp.toPx())
                    if (index < rows.lastIndex) drawLine(scheme.outline, Offset(x, center.y), Offset(x, size.height), 2.dp.toPx())
                    drawCircle(scheme.primary, 5.dp.toPx(), Offset(x, center.y))
                }
                Surface(onClick = { openEditor(if (row.window != null) Editor.Window else Editor.Fixed, row.start, row.window) },
                    color = if (row.window != null) scheme.primaryContainer else scheme.surface,
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (row.window != null) "${label(row.start)} – ${label(row.window.end)}" else label(row.start),
                                style = MaterialTheme.typography.headlineMedium)
                            val overnight = row.window != null && row.window.end < row.start
                            Text(if (row.window != null) "Every ${row.window.minutes} min" + (if (overnight) " · Overnight" else "") else "Fixed time",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    editor?.let { sheet ->
        ModalBottomSheet(onDismissRequest = { editor = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when (sheet) {
                    Editor.Add -> "Add alert"
                    Editor.Fixed -> if (original == null) "Add fixed time" else "Edit fixed time"
                    Editor.Window -> if (original == null) "Add interval" else "Edit interval"
                    Editor.Options -> "Notification settings"
                }, style = MaterialTheme.typography.headlineLarge)
                when (sheet) {
                    Editor.Add -> {
                        SheetRow("Fixed time", "Once a day at a time you choose") { openEditor(Editor.Fixed) }
                        SheetRow("Repeating interval", "Repeat between a start and end time") { openEditor(Editor.Window) }
                        if (!fixedOn && times.isNotEmpty()) {
                            TextButton(onClick = { savePlan(windows, times) }) { Text("Restore saved fixed times (${times.size})") }
                        }
                    }
                    Editor.Fixed, Editor.Window -> {
                        if (sheet == Editor.Fixed) {
                            SheetRow("Time", label(start)) { pick("Alert time", start) { start = it } }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SheetRow("From", label(start), Modifier.weight(1f)) { pick("Start time", start) { start = it } }
                                SheetRow("Until", label(end), Modifier.weight(1f)) { pick("End time", end) { end = it } }
                            }
                            if (end < start) Hint("Ends the following day")
                            Text("Repeat every", style = MaterialTheme.typography.titleMedium)
                            val presets = listOf(15L, 30L, 45L, 60L, 90L, 120L)
                            val custom = minutes !in presets
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                presets.forEach { duration ->
                                    FilterChip(selected = minutes == duration,
                                        onClick = { minutes = duration },
                                        label = { Text("$duration min") })
                                }
                                FilterChip(selected = custom, onClick = { pickingDuration = true },
                                    label = { Text(if (custom) "$minutes min · Custom" else "Custom") })
                            }
                            if (pickingDuration) {
                                var draft by rememberSaveable { mutableIntStateOf(minutes.toInt()) }
                                AlertDialog(onDismissRequest = { pickingDuration = false },
                                    title = { Text("Interval in minutes") },
                                    text = {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            AndroidView(factory = { ctx ->
                                                NumberPicker(ctx).apply {
                                                    minValue = 15
                                                    maxValue = 480
                                                    wrapSelectorWheel = false
                                                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                                                    value = draft
                                                    setOnValueChangedListener { _, _, value -> draft = value }
                                                }
                                            })
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            minutes = draft.toLong()
                                            pickingDuration = false
                                        }) { Text("Use interval") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { pickingDuration = false }) { Text("Cancel") }
                                    })
                            }
                        }
                        val duplicate = sheet == Editor.Fixed && start != original && start in activeTimes
                        val conflict = if (sheet == Editor.Window) windows.firstOverlap(start, end, windowId) else null
                        val buttonColors = ButtonDefaults.buttonColors()
                        if (duplicate) Hint("There is already a fixed alert at this time.")
                        Button(onClick = {
                            if (conflict != null) {
                                Toast.makeText(context, "Times overlap", Toast.LENGTH_SHORT).show()
                            } else if (sheet == Editor.Window) {
                                val updated = IntervalWindow(windowId ?: java.util.UUID.randomUUID().toString(), start, end, minutes)
                                savePlan(windows.filter { it.id != windowId } + updated, activeTimes)
                            } else {
                                savePlan(windows, activeTimes.filter { it != original } + start)
                            }
                        }, enabled = !duplicate,
                            colors = if (conflict == null) buttonColors else buttonColors.copy(
                                containerColor = buttonColors.disabledContainerColor,
                                contentColor = buttonColors.disabledContentColor
                            ),
                            modifier = Modifier.fillMaxWidth()) { Text("Save alert") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { editor = null }) { Text("Cancel") }
                            if (original != null) TextButton(onClick = {
                                if (sheet == Editor.Window) savePlan(windows.filter { it.id != windowId }, activeTimes)
                                else savePlan(windows, activeTimes.filter { it != original })
                            }) { Text("Remove alert", color = scheme.error) }
                        }
                    }
                    Editor.Options -> {
                        SettingSwitch("Daily reminders", settings.remindersEnabled) { value -> onUpdateSettings({ it.copy(remindersEnabled = value) }, null) }
                        SettingSwitch("Pause after daily goal", settings.pauseWhenGoalMet) { value -> onUpdateSettings({ it.copy(pauseWhenGoalMet = value) }, null) }
                        Hint("Notification quick-log amount")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(150L, 250L, 350L, 500L).forEach { ml ->
                                FilterChip(settings.defaultCupMl == ml, { onUpdateSettings({ it.copy(defaultCupMl = ml) }, null) }, label = { Text("$ml ml") })
                            }
                        }
                        OutlinedButton(onClick = onSendTestNotification, enabled = hasNotificationPermission) { Text("Send test notification") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, value: String, modifier: Modifier = Modifier, click: () -> Unit) {
    Surface(onClick = click, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { change(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
