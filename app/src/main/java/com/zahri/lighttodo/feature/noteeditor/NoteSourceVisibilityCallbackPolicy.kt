package com.zahri.lighttodo.feature.noteeditor

enum class NoteSourceVisibilityCallbackPhase {
    SourceExitStarted,
    SourceExitFinished,
    SourceReenterStarted,
    SourceReenterFinished
}

enum class NoteSourceVisibilityAction {
    Hide,
    Show,
    Keep
}

object NoteSourceVisibilityCallbackPolicy {
    fun actionFor(phase: NoteSourceVisibilityCallbackPhase): NoteSourceVisibilityAction =
        when (phase) {
            NoteSourceVisibilityCallbackPhase.SourceExitStarted -> NoteSourceVisibilityAction.Hide
            NoteSourceVisibilityCallbackPhase.SourceExitFinished -> NoteSourceVisibilityAction.Show
            NoteSourceVisibilityCallbackPhase.SourceReenterStarted,
            NoteSourceVisibilityCallbackPhase.SourceReenterFinished -> NoteSourceVisibilityAction.Keep
        }
}
