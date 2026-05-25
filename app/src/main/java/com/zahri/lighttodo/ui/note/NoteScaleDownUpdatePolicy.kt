package com.zahri.lighttodo.ui.note

enum class NoteScaleDownUpdateAction {
    None,
    KeepSystemReturn,
    DisableAnimation
}

object NoteScaleDownUpdatePolicy {
    fun actionFor(
        editorWasLaunched: Boolean,
        miuiReturnAnimationPrepared: Boolean
    ): NoteScaleDownUpdateAction =
        if (!editorWasLaunched) {
            NoteScaleDownUpdateAction.None
        } else if (miuiReturnAnimationPrepared) {
            NoteScaleDownUpdateAction.KeepSystemReturn
        } else {
            NoteScaleDownUpdateAction.DisableAnimation
        }
}
