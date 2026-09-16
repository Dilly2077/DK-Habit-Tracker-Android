package com.dk.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dk.habittracker.MainActivity
import com.dk.habittracker.R
import com.dk.habittracker.data.HabitStore
import java.time.LocalDate

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        val habits = HabitStore(context.applicationContext)
            .loadHabits()
            .filter { !it.archived }
        val today = LocalDate.now()
        val due = habits.filter { it.isDue(today) || it.isCompleted(today) }
        val done = due.count { it.isCompleted(today) }
        val best = habits.maxByOrNull { it.currentStreak(today) }

        return RemoteViews(context.packageName, R.layout.widget_streak).apply {
            if (best == null) {
                setTextViewText(R.id.streak_emoji, "☆")
                setTextViewText(R.id.streak_title, "Start a habit")
                setTextViewText(R.id.streak_value, "0 day streak")
            } else {
                val streak = best.currentStreak(today)
                setTextViewText(R.id.streak_emoji, best.emoji)
                setTextViewText(R.id.streak_title, best.name)
                setTextViewText(
                    R.id.streak_value,
                    "$streak day${if (streak == 1) "" else "s"} streak"
                )
            }
            setTextViewText(R.id.streak_today, "Today  $done/${due.size}")
            setOnClickPendingIntent(R.id.streak_root, openAppIntent(context))
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            6002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
