package com.dk.habittracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUpdater {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        val todayIds = manager.getAppWidgetIds(
            ComponentName(context, HabitWidgetProvider::class.java)
        )
        if (todayIds.isNotEmpty()) {
            HabitWidgetProvider().onUpdate(context, manager, todayIds)
        }

        val streakIds = manager.getAppWidgetIds(
            ComponentName(context, StreakWidgetProvider::class.java)
        )
        if (streakIds.isNotEmpty()) {
            StreakWidgetProvider().onUpdate(context, manager, streakIds)
        }
    }
}
