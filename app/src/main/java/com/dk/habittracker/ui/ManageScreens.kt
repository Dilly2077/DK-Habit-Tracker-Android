package com.dk.habittracker.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.ThemeMode
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun HabitsScreen(
    habits: List<Habit>,
    onEdit: (Habit) -> Unit,
    onArchive: (Habit) -> Unit,
    onDelete: (Habit) -> Unit
) {
    val active = habits.filter { !it.archived }
    val archived = habits.filter { it.archived }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Habits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your routines, schedules and reminders", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        if (active.isEmpty()) item { EmptyMessage("No active habits", "Create one to begin.") }
        items(active, key = { it.id }) { habit -> HabitManageCard(habit, onEdit, onArchive, onDelete) }
        if (archived.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("Archived", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(archived, key = { it.id }) { habit -> HabitManageCard(habit, onEdit, onArchive, onDelete) }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HabitManageCard(habit: Habit, onEdit: (Habit) -> Unit, onArchive: (Habit) -> Unit, onDelete: (Habit) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(habit.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${habit.category} • ${habit.frequency.label}${if (habit.reminderEnabled) " • reminder ${habit.reminderHour.toString().padStart(2, '0')}:${habit.reminderMinute.toString().padStart(2, '0')}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onEdit(habit) }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { onArchive(habit) }) {
                Icon(if (habit.archived) Icons.Default.Restore else Icons.Default.Archive, if (habit.archived) "Restore" else "Archive")
            }
            IconButton(onClick = { onDelete(habit) }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@Composable
internal fun InsightsScreen(habits: List<Habit>) {
    val active = habits.filter { !it.archived }
    val today = LocalDate.now()
    val days30 = (0L..29L).map { today.minusDays(it) }
    val due = active.sumOf { h -> days30.count { h.createdEpochDay <= it.toEpochDay() && h.isDue(it) } }
    val done = active.sumOf { h -> days30.count { it.toEpochDay() in h.completions } }
    val rate = if (due == 0) 0 else ((done * 100f) / due).toInt().coerceIn(0, 100)
    val best = active.maxOfOrNull { it.bestStreak() } ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Local statistics from your completion history", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("30-day rate", "$rate%", Modifier.weight(1f))
                MetricCard("Best streak", "$best days", Modifier.weight(1f))
            }
        }
        item {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (6L downTo 0L).forEach { offset ->
                    val day = today.minusDays(offset)
                    val scheduled = active.filter { it.createdEpochDay <= day.toEpochDay() && it.isDue(day) }
                    val count = scheduled.count { it.isCompleted(day) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(36.dp).background(
                                if (scheduled.isNotEmpty() && count == scheduled.size) MaterialTheme.colorScheme.primary
                                else if (count > 0) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Text(if (scheduled.isEmpty()) "–" else "$count") }
                        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Text("Top habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (active.isEmpty()) item { EmptyMessage("No data yet", "Complete habits to build your insights.") }
        items(active.sortedByDescending { it.currentStreak() }, key = { it.id }) { habit ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(habit.emoji)
                    Text(habit.name, Modifier.weight(1f).padding(start = 10.dp), fontWeight = FontWeight.SemiBold)
                    AssistChip(onClick = {}, label = { Text("${habit.currentStreak()} day streak") })
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    exportBackup: () -> String,
    importBackup: (String) -> Unit
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportBackup()) }
        }.onSuccess { Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, "Could not export backup", Toast.LENGTH_LONG).show() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read file")
        }.onSuccess {
            runCatching { importBackup(it) }
                .onSuccess { Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show() }
        }.onFailure { Toast.makeText(context, "Could not read backup", Toast.LENGTH_LONG).show() }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Appearance, privacy and your local data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Appearance", fontWeight = FontWeight.Bold)
            ThemeMode.entries.forEach { mode ->
                FilterChip(selected = themeMode == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.label) })
            }
        }
        item {
            Text("Backup", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { exportLauncher.launch("dk-habit-tracker-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("Export JSON backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore JSON backup")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Private by design", fontWeight = FontWeight.Bold)
                    Text("No account, ads, analytics or internet permission. Habit data stays in this app unless you explicitly export a backup.")
                }
            }
        }
    }
}
