package com.dk.habittracker.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.HabitFrequency

@Composable
internal fun HabitEditorDialog(existing: Habit?, onDismiss: () -> Unit, onSave: (Habit) -> Unit) {
    val context = LocalContext.current
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var emoji by remember(existing?.id) { mutableStateOf(existing?.emoji ?: "✓") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "General") }
    var frequency by remember(existing?.id) { mutableStateOf(existing?.frequency ?: HabitFrequency.DAILY) }
    var weeklyTarget by remember(existing?.id) { mutableIntStateOf(existing?.weeklyTarget ?: 5) }
    var reminderEnabled by remember(existing?.id) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderHour by remember(existing?.id) { mutableIntStateOf(existing?.reminderHour ?: 9) }
    var reminderMinute by remember(existing?.id) { mutableIntStateOf(existing?.reminderMinute ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New habit" else "Edit habit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(2) },
                        label = { Text("Icon") },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        label = { Text("Habit name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it.take(30) },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                Text("Schedule", fontWeight = FontWeight.SemiBold)
                HabitFrequency.entries.forEach { option ->
                    FilterChip(
                        selected = frequency == option,
                        onClick = { frequency = option },
                        label = { Text(option.label) }
                    )
                }
                AnimatedVisibility(frequency == HabitFrequency.WEEKLY_TARGET) {
                    Column {
                        Text("Target: $weeklyTarget times per week")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..7).forEach { n ->
                                AssistChip(
                                    onClick = { weeklyTarget = n },
                                    label = { Text("$n") },
                                    colors = if (weeklyTarget == n) AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else AssistChipDefaults.assistChipColors()
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("Reminder", fontWeight = FontWeight.SemiBold)
                        Text("Generated locally on this device", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                AnimatedVisibility(reminderEnabled) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> reminderHour = hour; reminderMinute = minute },
                                reminderHour,
                                reminderMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reminder at ${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    val base = existing ?: Habit(name = name.trim())
                    onSave(
                        base.copy(
                            name = name.trim(),
                            emoji = emoji.ifBlank { "✓" },
                            category = category.ifBlank { "General" }.trim(),
                            frequency = frequency,
                            weeklyTarget = weeklyTarget,
                            reminderEnabled = reminderEnabled,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
