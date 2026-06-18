package com.zahri.lighttodo

import android.content.Context
import android.content.Intent
import com.zahri.lighttodo.feature.quickadd.QuickAddActivity
import com.zahri.lighttodo.feature.reminder.ReminderActivity

object AppLaunchIntents {
    fun main(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun quickAdd(context: Context): Intent =
        Intent(context, QuickAddActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun reminder(context: Context, todoId: Long): Intent =
        Intent(context, ReminderActivity::class.java).apply {
            putExtra(ReminderActivity.EXTRA_TODO_ID, todoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
}
