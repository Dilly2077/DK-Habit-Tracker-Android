package com.dk.habittracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dk.habittracker.data.HabitStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        HabitStore(context).loadHabits()
            .filter { it.reminderEnabled && !it.archived }
            .forEach { ReminderScheduler.schedule(context, it) }
    }
}
