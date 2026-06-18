package com.zahri.lighttodo.feature.noteeditor

object NoteEditorWindowPolicy {
    fun shouldForceTransparentWindow(mode: NoteEditorLaunchAnimationMode): Boolean =
        mode == NoteEditorLaunchAnimationMode.CustomContainerTransform

    fun shouldUseEdgeToEdgeWindow(mode: NoteEditorLaunchAnimationMode): Boolean =
        when (mode) {
            NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown,
            NoteEditorLaunchAnimationMode.CustomContainerTransform,
            NoteEditorLaunchAnimationMode.Plain -> true
        }
}
