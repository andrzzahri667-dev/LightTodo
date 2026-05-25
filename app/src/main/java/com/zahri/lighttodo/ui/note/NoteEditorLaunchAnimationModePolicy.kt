package com.zahri.lighttodo.ui.note

enum class NoteEditorLaunchAnimationMode {
    MiuiSystemScaleUpDown,
    CustomContainerTransform,
    Plain
}

object NoteEditorLaunchAnimationModePolicy {
    fun modeFor(
        miuiOptionsAvailable: Boolean,
        sourceBoundsAvailable: Boolean
    ): NoteEditorLaunchAnimationMode =
        if (miuiOptionsAvailable) {
            NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown
        } else if (sourceBoundsAvailable) {
            NoteEditorLaunchAnimationMode.CustomContainerTransform
        } else {
            NoteEditorLaunchAnimationMode.Plain
        }
}
