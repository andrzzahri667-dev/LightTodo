package com.zahri.lighttodo.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.home.dateLabel
import com.zahri.lighttodo.ui.home.displayTitle
import com.zahri.lighttodo.ui.home.isOverdueDate

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodoListFactory(applicationContext)
}

class TodoListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodoEntity> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val app = context.applicationContext as App
        items = app.db.todoDao().listAllUndoneSync(limit = 6)
    }

    override fun onDestroy() { items = emptyList() }
    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item)
        val item = items[position]

        val titleText = SpannableString(item.displayTitle())
        if (item.done) {
            titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.item_title, titleText)

        val deadlineSuffix = if (item.deadlineHour != null && item.deadlineMinute != null)
            " %02d:%02d".format(item.deadlineHour, item.deadlineMinute) else ""
        rv.setTextViewText(R.id.item_subtitle, item.dateLabel() + deadlineSuffix)
        rv.setTextColor(
            R.id.item_subtitle,
            if (item.isOverdueDate()) Color.parseColor("#F26A6A") else Color.parseColor("#B6B6B6")
        )

        // Click the row -> open MainActivity (we don't open EditActivity here to keep widget simple)
        val rowFill = Intent().apply {
            putExtra(TodoWidgetProvider.EXTRA_TODO_ID, item.id)
            putExtra(TodoWidgetProvider.EXTRA_IS_CHECK, false)
        }
        rv.setOnClickFillInIntent(R.id.item_title, rowFill)
        rv.setOnClickFillInIntent(R.id.item_subtitle, rowFill)

        // Click the check -> mark done
        val checkFill = Intent().apply {
            putExtra(TodoWidgetProvider.EXTRA_TODO_ID, item.id)
            putExtra(TodoWidgetProvider.EXTRA_IS_CHECK, true)
        }
        rv.setOnClickFillInIntent(R.id.item_check, checkFill)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
