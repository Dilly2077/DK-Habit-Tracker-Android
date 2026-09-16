package com.dk.habittracker.data

import java.time.DayOfWeek
import java.time.LocalDate

enum class HabitFrequency(val label: String) {
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY_TARGET("Weekly target")
}

enum class HabitType(val label: String) {
    YES_NO("Yes or No"),
    MEASURABLE("Measurable")
}

data class Habit(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val emoji: String = "✓",
    val category: String = "General",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val weeklyTarget: Int = 5,
    val type: HabitType = HabitType.YES_NO,
    val targetValue: Double = 1.0,
    val unit: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val createdEpochDay: Long = LocalDate.now().toEpochDay(),
    val archived: Boolean = false,
    val accentIndex: Int = 0,
    val sortOrder: Int = 0,
    val completions: Set<Long> = emptySet(),
    val measurements: Map<Long, Double> = emptyMap()
) {
    fun isDue(date: LocalDate): Boolean = when (frequency) {
        HabitFrequency.DAILY -> true
        HabitFrequency.WEEKDAYS -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        HabitFrequency.WEEKLY_TARGET -> weeklyCompletions(date) < weeklyTarget || isCompleted(date)
    }

    fun isCompleted(date: LocalDate): Boolean = when (type) {
        HabitType.YES_NO -> date.toEpochDay() in completions
        HabitType.MEASURABLE -> valueFor(date) >= targetValue.coerceAtLeast(0.000001)
    }

    fun valueFor(date: LocalDate): Double = measurements[date.toEpochDay()] ?: 0.0

    fun weeklyCompletions(date: LocalDate): Int {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        return (0L..6L).count { offset ->
            val d = monday.plusDays(offset)
            !d.isAfter(sunday) && isCompleted(d)
        }
    }

    fun completionScore(today: LocalDate = LocalDate.now(), days: Int = 30): Int {
        var dueCount = 0
        var doneCount = 0
        for (offset in 0 until days) {
            val date = today.minusDays(offset.toLong())
            if (date.toEpochDay() < createdEpochDay) continue
            if (isDue(date) || isCompleted(date)) {
                dueCount++
                if (isCompleted(date)) doneCount++
            }
        }
        return if (dueCount == 0) 0 else ((doneCount * 100.0) / dueCount).toInt().coerceIn(0, 100)
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
        val successfulDays = buildList<Long> {
            completions.forEach { add(it) }
            measurements.forEach { (day, value) ->
                if (value >= targetValue.coerceAtLeast(0.000001)) add(day)
            }
        }.distinct().sorted()
        if (successfulDays.isEmpty()) return 0

        var best = 1
        var run = 1
        for (i in 1 until successfulDays.size) {
            val prev = LocalDate.ofEpochDay(successfulDays[i - 1])
            val cur = LocalDate.ofEpochDay(successfulDays[i])
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
