package com.zahri.lighttodo.ui.note

enum class NoteEditExitAction {
    SystemScaleDown,
    FadeFallback
}

object NoteEditExitPolicy {
    fun actionFor(miuiReturnAnimationPrepared: Boolean): NoteEditExitAction =
        if (miuiReturnAnimationPrepared) {
            NoteEditExitAction.SystemScaleDown
        } else {
            NoteEditExitAction.FadeFallback
        }
}
