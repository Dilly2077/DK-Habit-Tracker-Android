package com.dk.habittracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HabitStore(context: Context) {
    private val prefs = context.getSharedPreferences("dk_habit_tracker", Context.MODE_PRIVATE)

    fun loadHabits(): List<Habit> {
        val raw = prefs.getString(KEY_HABITS, null) ?: return emptyList()
        return runCatching { decodeHabits(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun saveHabits(habits: List<Habit>) {
        prefs.edit().putString(KEY_HABITS, encodeHabits(habits).toString()).apply()
    }

    fun loadTheme(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM)

    fun saveTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun exportBackup(habits: List<Habit>, themeMode: ThemeMode): String = JSONObject().apply {
        put("format", "dk-habit-tracker-backup")
        put("version", 1)
        put("theme", themeMode.name)
        put("habits", encodeHabits(habits))
    }.toString(2)

    fun importBackup(raw: String): Pair<List<Habit>, ThemeMode> {
        val root = JSONObject(raw)
        require(root.optString("format") == "dk-habit-tracker-backup") { "Unsupported backup file" }
        val habits = decodeHabits(root.getJSONArray("habits"))
        val theme = runCatching { ThemeMode.valueOf(root.optString("theme", ThemeMode.SYSTEM.name)) }
            .getOrDefault(ThemeMode.SYSTEM)
        return habits to theme
    }

    private fun encodeHabits(habits: List<Habit>): JSONArray = JSONArray().apply {
        habits.forEach { habit ->
            put(JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("emoji", habit.emoji)
                put("category", habit.category)
                put("frequency", habit.frequency.name)
                put("weeklyTarget", habit.weeklyTarget)
                put("reminderEnabled", habit.reminderEnabled)
                put("reminderHour", habit.reminderHour)
                put("reminderMinute", habit.reminderMinute)
                put("createdEpochDay", habit.createdEpochDay)
                put("archived", habit.archived)
                put("accentIndex", habit.accentIndex)
                put("completions", JSONArray().apply { habit.completions.sorted().forEach(::put) })
            })
        }
    }

    private fun decodeHabits(array: JSONArray): List<Habit> = buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val completionArray = o.optJSONArray("completions") ?: JSONArray()
            val completions = buildSet {
                for (j in 0 until completionArray.length()) add(completionArray.getLong(j))
            }
            add(
                Habit(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    emoji = o.optString("emoji", "✓"),
                    category = o.optString("category", "General"),
                    frequency = runCatching { HabitFrequency.valueOf(o.optString("frequency")) }
                        .getOrDefault(HabitFrequency.DAILY),
                    weeklyTarget = o.optInt("weeklyTarget", 5).coerceIn(1, 7),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderHour = o.optInt("reminderHour", 9).coerceIn(0, 23),
                    reminderMinute = o.optInt("reminderMinute", 0).coerceIn(0, 59),
                    createdEpochDay = o.optLong("createdEpochDay", java.time.LocalDate.now().toEpochDay()),
                    archived = o.optBoolean("archived", false),
                    accentIndex = o.optInt("accentIndex", 0),
                    completions = completions
                )
            )
        }
    }

    companion object {
        private const val KEY_HABITS = "habits_json"
        private const val KEY_THEME = "theme_mode"
    }
}
