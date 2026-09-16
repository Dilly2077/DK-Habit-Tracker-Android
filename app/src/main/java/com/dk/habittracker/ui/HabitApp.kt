package com.dk.habittracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.HabitStore
import com.dk.habittracker.data.HabitType
import com.dk.habittracker.data.ThemeMode
import com.dk.habittracker.reminder.ReminderScheduler
import com.dk.habittracker.ui.theme.DKHabitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class AppTab(val label: String) {
    TODAY("Today"), HABITS("Habits"), INSIGHTS("Insights"), SETTINGS("Settings")
}

@Composable
fun HabitTrackerApp() {
    val context = LocalContext.current
    val store = remember { HabitStore(context.applicationContext) }
    var habits by remember { mutableStateOf(store.loadHabits()) }
    var themeMode by remember { mutableStateOf(store.loadTheme()) }

    fun persist(next: List<Habit>) {
        habits = next
        store.saveHabits(next)
    }

    DKHabitTheme(themeMode) {
        HabitShell(
            habits = habits,
            themeMode = themeMode,
            onHabitsChange = ::persist,
            onThemeChange = {
                themeMode = it
                store.saveTheme(it)
            },
            exportBackup = { store.exportBackup(habits, themeMode) },
            importBackup = { raw ->
                val (imported, importedTheme) = store.importBackup(raw)
                habits = imported
                themeMode = importedTheme
                store.saveHabits(imported)
                store.saveTheme(importedTheme)
                imported.filter { it.reminderEnabled && !it.archived }
                    .forEach { ReminderScheduler.schedule(context, it) }
            }
        )
    }
}

@Composable
private fun HabitShell(
    habits: List<Habit>,
    themeMode: ThemeMode,
    onHabitsChange: (List<Habit>) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    exportBackup: () -> String,
    importBackup: (String) -> Unit
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var editorHabit by remember { mutableStateOf<Habit?>(null) }
    var editorType by remember { mutableStateOf(HabitType.YES_NO) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Notifications are disabled, so reminders will not appear.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestNotificationsIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun saveHabit(input: Habit) {
        val exists = habits.any { it.id == input.id }
        val habit = if (exists) {
            input
        } else {
            input.copy(sortOrder = (habits.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        }
        val next = if (exists) {
            habits.map { if (it.id == habit.id) habit else it }
        } else {
            habits + habit
        }
        onHabitsChange(next)
        if (habit.reminderEnabled) {
            requestNotificationsIfNeeded()
            ReminderScheduler.schedule(context, habit)
        } else {
            ReminderScheduler.cancel(context, habit.id)
        }
    }

    fun moveHabit(habit: Habit, direction: Int) {
        val active = habits.filter { !it.archived }.sortedBy { it.sortOrder }.toMutableList()
        val index = active.indexOfFirst { it.id == habit.id }
        val target = index + direction
        if (index !in active.indices || target !in active.indices) return
        val other = active[target]
        val next = habits.map {
            when (it.id) {
                habit.id -> it.copy(sortOrder = other.sortOrder)
                other.id -> it.copy(sortOrder = habit.sortOrder)
                else -> it
            }
        }
        onHabitsChange(next)
    }

    fun startNewHabit() {
        editorHabit = null
        showTypePicker = true
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                when (item) {
                                    AppTab.TODAY -> Icons.Default.Home
                                    AppTab.HABITS -> Icons.Default.EventRepeat
                                    AppTab.INSIGHTS -> Icons.Default.Analytics
                                    AppTab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == AppTab.TODAY || tab == AppTab.HABITS) {
                ExtendedFloatingActionButton(
                    onClick = ::startNewHabit,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New habit") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                AppTab.TODAY -> TodayScreen(
                    habits = habits,
                    onSaveHabit = ::saveHabit,
                    onEdit = {
                        editorHabit = it
                        editorType = it.type
                        showEditor = true
                    }
                )

                AppTab.HABITS -> HabitsScreen(
                    habits = habits,
                    onEdit = {
                        editorHabit = it
                        editorType = it.type
                        showEditor = true
                    },
                    onArchive = { saveHabit(it.copy(archived = !it.archived)) },
                    onDelete = {
                        ReminderScheduler.cancel(context, it.id)
                        onHabitsChange(habits.filterNot { h -> h.id == it.id })
                    },
                    onMove = ::moveHabit
                )

                AppTab.INSIGHTS -> InsightsScreen(habits)
                AppTab.SETTINGS -> SettingsScreen(
                    themeMode,
                    onThemeChange,
                    exportBackup,
                    importBackup
                )
            }
        }
    }

    if (showTypePicker) {
        HabitTypePickerDialog(
            onDismiss = { showTypePicker = false },
            onSelected = {
                editorType = it
                showTypePicker = false
                showEditor = true
            }
        )
    }

    if (showEditor) {
        HabitEditorDialog(
            existing = editorHabit,
            initialType = editorType,
            onDismiss = { showEditor = false },
            onSave = {
                saveHabit(it)
                showEditor = false
            }
        )
    }
}

@Composable
private fun TodayScreen(
    habits: List<Habit>,
    onSaveHabit: (Habit) -> Unit,
    onEdit: (Habit) -> Unit
) {
    val today = LocalDate.now()
    val active = habits.filter { !it.archived }.sortedBy { it.sortOrder }
    val due = active.filter { it.isDue(today) || it.isCompleted(today) }
    val completed = due.count { it.isCompleted(today) }
    val progress = if (due.isEmpty()) 0f else completed.toFloat() / due.size
    var measurementHabit by remember { mutableStateOf<Habit?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            WeekStrip(today, habits)
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("$completed of ${due.size} complete", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (due.isEmpty()) {
            item {
                EmptyMessage(
                    "No habits due",
                    "Add a Yes/No or measurable habit to start tracking."
                )
            }
        } else {
            items(due, key = { it.id }) { habit ->
                TodayHabitCard(
                    habit = habit,
                    today = today,
                    onAction = {
                        if (habit.type == HabitType.YES_NO) {
                            val day = today.toEpochDay()
                            onSaveHabit(
                                if (day in habit.completions) {
                                    habit.copy(completions = habit.completions - day)
                                } else {
                                    habit.copy(completions = habit.completions + day)
                                }
                            )
                        } else {
                            measurementHabit = habit
                        }
                    },
                    onEdit = onEdit
                )
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    measurementHabit?.let { habit ->
        MeasurementDialog(
            habit = habit,
            onDismiss = { measurementHabit = null },
            onSave = { value ->
                val day = today.toEpochDay()
                onSaveHabit(
                    habit.copy(
                        measurements = if (value == 0.0) {
                            habit.measurements - day
                        } else {
                            habit.measurements + (day to value)
                        }
                    )
                )
                measurementHabit = null
            }
        )
    }
}

@Composable
private fun WeekStrip(today: LocalDate, habits: List<Habit>) {
    val active = habits.filter { !it.archived }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((6L downTo 0L).toList()) { offset ->
            val day = today.minusDays(offset)
            val due = active.filter {
                it.createdEpochDay <= day.toEpochDay() && (it.isDue(day) || it.isCompleted(day))
            }
            val done = due.count { it.isCompleted(day) }
            val allDone = due.isNotEmpty() && done == due.size
            Column(
                modifier = Modifier
                    .background(
                        if (offset == 0L) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        .uppercase()
                        .take(3),
                    style = MaterialTheme.typography.labelSmall
                )
                Text("${day.dayOfMonth}", fontWeight = FontWeight.Bold)
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(5.dp)
                        .background(
                            if (allDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
private fun TodayHabitCard(
    habit: Habit,
    today: LocalDate,
    onAction: () -> Unit,
    onEdit: (Habit) -> Unit
) {
    val done = habit.isCompleted(today)
    val accent = HabitAccentColors[habit.accentIndex % HabitAccentColors.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(habit.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.SemiBold)
                Text(
                    if (habit.type == HabitType.MEASURABLE) {
                        val value = formatNumber(habit.valueFor(today))
                        val target = formatNumber(habit.targetValue)
                        "$value / $target ${habit.unit}".trim()
                    } else {
                        "${habit.category} • ${habit.frequency.label}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        " ${habit.currentStreak()} day streak",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = { onEdit(habit) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            if (habit.type == HabitType.YES_NO) {
                IconButton(onClick = onAction) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = if (done) "Undo" else "Complete",
                        tint = accent
                    )
                }
            } else {
                OutlinedButton(onClick = onAction) {
                    Text(if (habit.valueFor(today) > 0.0) "Edit value" else "Log")
                }
            }
        }
    }
}

@Composable
private fun MeasurementDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val today = LocalDate.now()
    var value by remember(habit.id) {
        mutableStateOf(
            habit.valueFor(today).takeIf { it > 0.0 }?.let(::formatNumber).orEmpty()
        )
    }
    val parsed = value.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${habit.name}") },
        text = {
            Column {
                Text(
                    "Target: ${formatNumber(habit.targetValue)} ${habit.unit}".trim(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it.filter { c -> c.isDigit() || c == '.' }.take(12)
                    },
                    label = { Text(if (habit.unit.isBlank()) "Value" else habit.unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = parsed != null && parsed >= 0.0,
                onClick = { onSave(parsed ?: 0.0) }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (habit.valueFor(today) > 0.0) {
                    TextButton(onClick = { onSave(0.0) }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

@Composable
internal fun EmptyMessage(title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
