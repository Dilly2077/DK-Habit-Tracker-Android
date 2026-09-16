package com.dk.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.dk.habittracker.MainActivity
import com.dk.habittracker.R
import com.dk.habittracker.data.Habit
import com.dk.habittracker.data.HabitStore
import com.dk.habittracker.data.HabitType
import java.time.LocalDate

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, Long.MIN_VALUE)
            if (habitId != Long.MIN_VALUE) {
                val store = HabitStore(context.applicationContext)
                val habits = store.loadHabits()
                val today = LocalDate.now().toEpochDay()
                val next = habits.map { habit ->
                    if (habit.id == habitId && habit.type == HabitType.YES_NO) {
                        if (today in habit.completions) {
                            habit.copy(completions = habit.completions - today)
                        } else {
                            habit.copy(completions = habit.completions + today)
                        }
                    } else {
                        habit
                    }
                }
                store.saveHabits(next)
            }
            return
        }
        super.onReceive(context, intent)
    }

    private fun buildViews(context: Context): RemoteViews {
        val store = HabitStore(context.applicationContext)
        val today = LocalDate.now()
        val due = store.loadHabits()
            .filter { !it.archived && (it.isDue(today) || it.isCompleted(today)) }
            .sortedBy { it.sortOrder }

        val completed = due.count { it.isCompleted(today) }
        val percent = if (due.isEmpty()) 0 else ((completed * 100f) / due.size).toInt()

        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(R.id.widget_summary, "$completed/${due.size} complete")
        views.setProgressBar(R.id.widget_progress, 100, percent, false)

        val openIntent = openAppIntent(context)
        views.setOnClickPendingIntent(R.id.widget_header, openIntent)
        views.setOnClickPendingIntent(R.id.widget_empty, openIntent)

        val rowIds = intArrayOf(
            R.id.habit_row_1,
            R.id.habit_row_2,
            R.id.habit_row_3,
            R.id.habit_row_4
        )
        val nameIds = intArrayOf(
            R.id.habit_name_1,
            R.id.habit_name_2,
            R.id.habit_name_3,
            R.id.habit_name_4
        )
        val actionIds = intArrayOf(
            R.id.habit_action_1,
            R.id.habit_action_2,
            R.id.habit_action_3,
            R.id.habit_action_4
        )

        views.setViewVisibility(
            R.id.widget_empty,
            if (due.isEmpty()) View.VISIBLE else View.GONE
        )

        rowIds.indices.forEach { index ->
            val habit = due.getOrNull(index)
            if (habit == null) {
                views.setViewVisibility(rowIds[index], View.GONE)
            } else {
                views.setViewVisibility(rowIds[index], View.VISIBLE)
                bindHabitRow(
                    context = context,
                    views = views,
                    habit = habit,
                    rowId = rowIds[index],
                    nameId = nameIds[index],
                    actionId = actionIds[index],
                    today = today
                )
            }
        }

        return views
    }

    private fun bindHabitRow(
        context: Context,
        views: RemoteViews,
        habit: Habit,
        rowId: Int,
        nameId: Int,
        actionId: Int,
        today: LocalDate
    ) {
        val valueText = if (habit.type == HabitType.MEASURABLE) {
            val current = formatNumber(habit.valueFor(today))
            val target = formatNumber(habit.targetValue)
            "  $current/$target ${habit.unit}".trimEnd()
        } else {
            ""
        }

        views.setTextViewText(nameId, "${habit.emoji}  ${habit.name}$valueText")
        views.setOnClickPendingIntent(rowId, openAppIntent(context))

        if (habit.type == HabitType.YES_NO) {
            views.setTextViewText(actionId, if (habit.isCompleted(today)) "✓" else "○")
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(EXTRA_HABIT_ID, habit.id)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                habit.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(actionId, pending)
        } else {
            views.setTextViewText(actionId, "Log")
            views.setOnClickPendingIntent(actionId, openAppIntent(context))
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            6001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else "%.1f".format(value).trimEnd('0').trimEnd('.')

    companion object {
        private const val ACTION_TOGGLE = "com.dk.habittracker.widget.TOGGLE"
        private const val EXTRA_HABIT_ID = "habit_id"
    }
}
