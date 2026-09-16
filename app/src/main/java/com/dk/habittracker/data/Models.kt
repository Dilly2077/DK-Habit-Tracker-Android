package com.dk.habittracker.data

import java.time.DayOfWeek
import java.time.LocalDate

enum class HabitFrequency(val label: String) {
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY_TARGET("Weekly target")
}

data class Habit(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val emoji: String = "✓",
    val category: String = "General",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val weeklyTarget: Int = 5,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val createdEpochDay: Long = LocalDate.now().toEpochDay(),
    val archived: Boolean = false,
    val accentIndex: Int = 0,
    val completions: Set<Long> = emptySet()
) {
    fun isDue(date: LocalDate): Boolean = when (frequency) {
        HabitFrequency.DAILY -> true
        HabitFrequency.WEEKDAYS -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        HabitFrequency.WEEKLY_TARGET -> weeklyCompletions(date) < weeklyTarget
    }

    fun isCompleted(date: LocalDate): Boolean = date.toEpochDay() in completions

    fun weeklyCompletions(date: LocalDate): Int {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        return completions.count { day ->
            val d = LocalDate.ofEpochDay(day)
            !d.isBefore(monday) && !d.isAfter(sunday)
        }
    }

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        var cursor = today
        var streak = 0
        var inspected = 0
        while (inspected < 730) {
            inspected++
            if (!isScheduledDayForStreak(cursor)) {
                cursor = cursor.minusDays(1)
                continue
            }
            if (isCompleted(cursor)) {
                streak++
                cursor = cursor.minusDays(1)
            } else if (cursor == today && !isDue(today)) {
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun bestStreak(): Int {
        if (completions.isEmpty()) return 0
        val sorted = completions.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            val prev = LocalDate.ofEpochDay(sorted[i - 1])
            val cur = LocalDate.ofEpochDay(sorted[i])
            var expected = prev.plusDays(1)
            while (!isScheduledDayForStreak(expected) && expected.isBefore(cur)) {
                expected = expected.plusDays(1)
            }
            if (expected == cur) {
                run++
                best = maxOf(best, run)
            } else if (prev != cur) {
                run = 1
            }
        }
        return best
    }

    private fun isScheduledDayForStreak(date: LocalDate): Boolean = when (frequency) {
        HabitFrequency.DAILY, HabitFrequency.WEEKLY_TARGET -> true
        HabitFrequency.WEEKDAYS -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark"), OLED("OLED")
}
