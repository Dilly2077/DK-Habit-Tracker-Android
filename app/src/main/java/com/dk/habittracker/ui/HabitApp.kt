package com.dk.habittracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.HabitStore
import com.dk.habittracker.data.ThemeMode
import com.dk.habittracker.reminder.ReminderScheduler
import com.dk.habittracker.ui.theme.DKHabitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var showEditor by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "Notifications are disabled, so reminders will not appear.", Toast.LENGTH_LONG).show()
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun saveHabit(habit: Habit) {
        val next = if (habits.any { it.id == habit.id }) {
            habits.map { if (it.id == habit.id) habit else it }
        } else habits + habit
        onHabitsChange(next)
        if (habit.reminderEnabled) {
            requestNotificationsIfNeeded()
            ReminderScheduler.schedule(context, habit)
        } else ReminderScheduler.cancel(context, habit.id)
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
                    onClick = { editorHabit = null; showEditor = true },
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
                    onToggle = { habit ->
                        val day = LocalDate.now().toEpochDay()
                        saveHabit(
                            if (day in habit.completions) habit.copy(completions = habit.completions - day)
                            else habit.copy(completions = habit.completions + day)
                        )
                    },
                    onEdit = { editorHabit = it; showEditor = true }
                )
                AppTab.HABITS -> HabitsScreen(
                    habits = habits,
                    onEdit = { editorHabit = it; showEditor = true },
                    onArchive = { saveHabit(it.copy(archived = !it.archived)) },
                    onDelete = {
                        ReminderScheduler.cancel(context, it.id)
                        onHabitsChange(habits.filterNot { h -> h.id == it.id })
                    }
                )
                AppTab.INSIGHTS -> InsightsScreen(habits)
                AppTab.SETTINGS -> SettingsScreen(themeMode, onThemeChange, exportBackup, importBackup)
            }
        }
    }

    if (showEditor) {
        HabitEditorDialog(
            existing = editorHabit,
            onDismiss = { showEditor = false },
            onSave = { saveHabit(it); showEditor = false }
        )
    }
}

@Composable
private fun TodayScreen(habits: List<Habit>, onToggle: (Habit) -> Unit, onEdit: (Habit) -> Unit) {
    val today = LocalDate.now()
    val active = habits.filter { !it.archived }
    val due = active.filter { it.isDue(today) || it.isCompleted(today) }
    val completed = due.count { it.isCompleted(today) }
    val progress = if (due.isEmpty()) 0f else completed.toFloat() / due.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("$completed of ${due.size} complete", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (due.isEmpty()) {
            item { EmptyMessage("No habits due", "Add a habit with the button below to start tracking.") }
        } else {
            items(due, key = { it.id }) { habit ->
                TodayHabitCard(habit, today, onToggle, onEdit)
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun TodayHabitCard(habit: Habit, today: LocalDate, onToggle: (Habit) -> Unit, onEdit: (Habit) -> Unit) {
    val done = habit.isCompleted(today)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${habit.category} • ${habit.frequency.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(" ${habit.currentStreak()} day streak", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { onEdit(habit) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = { onToggle(habit) }) {
                Icon(Icons.Default.Check, contentDescription = if (done) "Undo" else "Complete", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun EmptyMessage(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
