package com.zahri.lighttodo.ui.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zahri.lighttodo.ui.theme.LightTodoTheme

class NoteEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editingId = intent.noteIdExtra()

        setContent {
            LightTodoTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    NoteEditScreen(
                        editingId = editingId,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        private const val ExtraNoteId = "com.zahri.lighttodo.extra.NOTE_ID"

        fun intent(context: Context, noteId: Long?): Intent =
            Intent(context, NoteEditActivity::class.java).apply {
                if (noteId != null) putExtra(ExtraNoteId, noteId)
            }

        private fun Intent.noteIdExtra(): Long? =
            if (hasExtra(ExtraNoteId)) getLongExtra(ExtraNoteId, 0L) else null
    }
}
