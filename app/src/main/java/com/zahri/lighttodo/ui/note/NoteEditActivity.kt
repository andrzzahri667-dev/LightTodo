package com.zahri.lighttodo.ui.note

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zahri.lighttodo.ui.theme.LightTodoTheme

class NoteEditActivity : ComponentActivity() {
    private val noteEditViewModel: NoteEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editingId = intent.noteIdExtra()
        noteEditViewModel.load(editingId, intent.noteLaunchSeedExtra())

        setContent {
            LightTodoTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    NoteEditScreen(
                        editingId = editingId,
                        onBack = { finish() },
                        vm = noteEditViewModel
                    )
                }
            }
        }
    }

    companion object {
        private const val ExtraNoteId = "com.zahri.lighttodo.extra.NOTE_ID"
        private const val ExtraSeedId = "com.zahri.lighttodo.extra.NOTE_SEED_ID"
        private const val ExtraSeedTitle = "com.zahri.lighttodo.extra.NOTE_SEED_TITLE"
        private const val ExtraSeedContent = "com.zahri.lighttodo.extra.NOTE_SEED_CONTENT"
        private const val ExtraSeedCreatedAt = "com.zahri.lighttodo.extra.NOTE_SEED_CREATED_AT"
        private const val ExtraSeedUpdatedAt = "com.zahri.lighttodo.extra.NOTE_SEED_UPDATED_AT"

        fun intent(context: Context, noteId: Long?, launchSeed: NoteEditLaunchSeed? = null): Intent =
            Intent(context, NoteEditActivity::class.java).apply {
                if (noteId != null) putExtra(ExtraNoteId, noteId)
                if (launchSeed != null) {
                    putExtra(ExtraSeedId, launchSeed.id)
                    putExtra(ExtraSeedTitle, launchSeed.title)
                    putExtra(ExtraSeedContent, launchSeed.content)
                    putExtra(ExtraSeedCreatedAt, launchSeed.createdAtMillis)
                    putExtra(ExtraSeedUpdatedAt, launchSeed.updatedAtMillis)
                }
            }

        private fun Intent.noteIdExtra(): Long? =
            if (hasExtra(ExtraNoteId)) getLongExtra(ExtraNoteId, 0L) else null

        private fun Intent.noteLaunchSeedExtra(): NoteEditLaunchSeed? =
            if (!hasExtra(ExtraSeedId)) {
                null
            } else {
                NoteEditLaunchSeed(
                    id = getLongExtra(ExtraSeedId, 0L),
                    title = getStringExtra(ExtraSeedTitle),
                    content = getStringExtra(ExtraSeedContent).orEmpty(),
                    createdAtMillis = getLongExtra(ExtraSeedCreatedAt, 0L),
                    updatedAtMillis = getLongExtra(ExtraSeedUpdatedAt, 0L)
                )
            }
    }
}
