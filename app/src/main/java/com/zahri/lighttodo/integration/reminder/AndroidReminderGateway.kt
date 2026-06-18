package com.zahri.lighttodo.integration.reminder

import android.content.Context
import com.zahri.lighttodo.usecase.todo.ReminderGateway
import com.zahri.lighttodo.usecase.todo.TodoRecord

class AndroidReminderGateway(
    private val context: Context
) : ReminderGateway {
    override fun schedule(todo: TodoRecord, isStart: Boolean) {
        ReminderScheduler.schedule(context, todo, isStart)
    }

    override fun cancel(todoId: Long) {
        ReminderScheduler.cancel(context, todoId)
    }
}
