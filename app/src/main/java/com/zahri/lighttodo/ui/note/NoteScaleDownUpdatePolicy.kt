package com.zahri.lighttodo.ui.note

enum class NoteScaleDownUpdateAction {
    None,
    UpdateData,
    DisableAnimation
}

object NoteScaleDownUpdatePolicy {
    fun actionFor(editorWasLaunched: Boolean, hasScaleDownData: Boolean): NoteScaleDownUpdateAction =
        if (!editorWasLaunched) {
            NoteScaleDownUpdateAction.None
        } else if (hasScaleDownData) {
            NoteScaleDownUpdateAction.UpdateData
        } else {
            NoteScaleDownUpdateAction.DisableAnimation
        }
}
