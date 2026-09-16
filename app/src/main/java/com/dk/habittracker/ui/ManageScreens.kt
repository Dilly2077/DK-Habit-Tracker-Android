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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.HabitType
import com.dk.habittracker.data.ThemeMode
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class HabitSort(val label: String) {
    MANUAL("Manually"),
    NAME("By name"),
    COLOR("By color"),
    SCORE("By score"),
    STATUS("By status")
}

@Composable
internal fun HabitsScreen(
    habits: List<Habit>,
    onEdit: (Habit) -> Unit,
    onArchive: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    onMove: (Habit, Int) -> Unit
) {
    val today = LocalDate.now()
    var hideArchived by rememberSaveable { mutableStateOf(true) }
    var hideCompleted by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(HabitSort.MANUAL) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val visible = habits
        .filter { !(hideArchived && it.archived) }
        .filter { !(hideCompleted && it.isCompleted(today)) }
        .let { source ->
            when (sort) {
                HabitSort.MANUAL -> source.sortedWith(compareBy<Habit> { it.archived }.thenBy { it.sortOrder })
                HabitSort.NAME -> source.sortedBy { it.name.lowercase() }
                HabitSort.COLOR -> source.sortedWith(compareBy<Habit> { it.accentIndex }.thenBy { it.name.lowercase() })
                HabitSort.SCORE -> source.sortedByDescending { it.completionScore(today) }
                HabitSort.STATUS -> source.sortedWith(
                    compareBy<Habit> { it.archived }
                        .thenBy { it.isCompleted(today) }
                        .thenBy { it.name.lowercase() }
                )
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Habits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${habits.count { !it.archived }} active • ${habits.count { it.archived }} archived",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter and sort")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = {
                            menuOpen = false
                            sortMenuOpen = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Hide archived", Modifier.weight(1f))
                                    Checkbox(
                                        checked = hideArchived,
                                        onCheckedChange = null
                                    )
                                }
                            },
                            onClick = { hideArchived = !hideArchived }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Hide completed", Modifier.weight(1f))
                                    Checkbox(
                                        checked = hideCompleted,
                                        onCheckedChange = null
                                    )
                                }
                            },
                            onClick = { hideCompleted = !hideCompleted }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sort", Modifier.weight(1f))
                                    Text(sort.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                sortMenuOpen = true
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }
                    ) {
                        HabitSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (sort == option) "✓  ${option.label}" else option.label,
                                        fontWeight = if (sort == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sort = option
                                    sortMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { menuOpen = true },
                    label = { Text(sort.label) }
                )
                if (!hideArchived) {
                    AssistChip(
                        onClick = { hideArchived = true },
                        label = { Text("Archived shown") }
                    )
                }
                if (hideCompleted) {
                    AssistChip(
                        onClick = { hideCompleted = false },
                        label = { Text("Completed hidden") }
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                EmptyMessage(
                    if (habits.isEmpty()) "No habits yet" else "Nothing matches these filters",
                    if (habits.isEmpty()) "Create a habit to begin." else "Change the filter options to show more habits."
                )
            }
        } else {
            items(visible, key = { it.id }) { habit ->
                HabitManageCard(
                    habit = habit,
                    sort = sort,
                    onEdit = onEdit,
                    onArchive = onArchive,
                    onDelete = onDelete,
                    onMove = onMove
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HabitManageCard(
    habit: Habit,
    sort: HabitSort,
    onEdit: (Habit) -> Unit,
    onArchive: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    onMove: (Habit, Int) -> Unit
) {
    val accent = HabitAccentColors[habit.accentIndex % HabitAccentColors.size]
    val today = LocalDate.now()

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
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

            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(habit.name, fontWeight = FontWeight.SemiBold)
                    if (habit.isCompleted(today)) {
                        Text(
                            "  ✓",
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val typeLabel = if (habit.type == HabitType.YES_NO) {
                    "Yes/No"
                } else {
                    "${formatManageNumber(habit.targetValue)} ${habit.unit}".trim()
                }

                Text(
                    "$typeLabel • ${habit.frequency.label} • ${habit.completionScore(today)}% score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (habit.reminderEnabled) {
                    Text(
                        "Reminder ${habit.reminderHour.toString().padStart(2, '0')}:${habit.reminderMinute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sort == HabitSort.MANUAL && !habit.archived) {
                Column {
                    IconButton(onClick = { onMove(habit, -1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowUpward, "Move up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onMove(habit, 1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ArrowDownward, "Move down", modifier = Modifier.size(18.dp))
                    }
                }
            }

            IconButton(onClick = { onEdit(habit) }) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = { onArchive(habit) }) {
                Icon(
                    if (habit.archived) Icons.Default.Restore else Icons.Default.Archive,
                    if (habit.archived) "Restore" else "Archive"
                )
            }
            IconButton(onClick = { onDelete(habit) }) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

private fun formatManageNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')

@Composable
internal fun InsightsScreen(habits: List<Habit>) {
    val active = habits.filter { !it.archived }
    val today = LocalDate.now()
    val days30 = (0L..29L).map { today.minusDays(it) }
    val due = active.sumOf { h ->
        days30.count {
            h.createdEpochDay <= it.toEpochDay() && (h.isDue(it) || h.isCompleted(it))
        }
    }
    val done = active.sumOf { h -> days30.count { h.isCompleted(it) } }
    val rate = if (due == 0) 0 else ((done * 100f) / due).toInt().coerceIn(0, 100)
    val best = active.maxOfOrNull { it.bestStreak() } ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Local statistics from your completion history",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    val scheduled = active.filter {
                        it.createdEpochDay <= day.toEpochDay() && (it.isDue(day) || it.isCompleted(day))
                    }
                    val count = scheduled.count { it.isCompleted(day) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(36.dp).background(
                                if (scheduled.isNotEmpty() && count == scheduled.size) {
                                    MaterialTheme.colorScheme.primary
                                } else if (count > 0) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(10.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (scheduled.isEmpty()) "–" else "$count")
                        }
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        item {
            Text("Top habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (active.isEmpty()) {
            item { EmptyMessage("No data yet", "Complete habits to build your insights.") }
        }

        items(
            active.sortedByDescending { it.completionScore(today) },
            key = { it.id }
        ) { habit ->
            val accent = HabitAccentColors[habit.accentIndex % HabitAccentColors.size]
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(34.dp).background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(habit.emoji) }

                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(habit.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${habit.completionScore(today)}% 30-day score",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("${habit.currentStreak()} day streak") }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(exportBackup())
                }
            }.onSuccess {
                Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not export backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read file")
            }.onSuccess {
                runCatching { importBackup(it) }
                    .onSuccess {
                        Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show()
                    }
            }.onFailure {
                Toast.makeText(context, "Could not read backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Appearance, widgets, privacy and your local data",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text("Appearance", fontWeight = FontWeight.Bold)
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Home-screen widgets", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Two Android widgets are included: Today Habits and Streak Spotlight. Add them from your launcher's Widgets menu."
                    )
                }
            }
        }

        item {
            Text("Backup", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { exportLauncher.launch("dk-habit-tracker-backup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export JSON backup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore JSON backup")
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Private by design", fontWeight = FontWeight.Bold)
                    Text(
                        "No account, ads, analytics or internet permission. Habit data stays in this app unless you explicitly export a backup."
                    )
                }
            }
        }
    }
}
