package com.zahri.lighttodo.ui.note

object NoteEditorWindowPolicy {
    fun shouldForceTransparentWindow(mode: NoteEditorLaunchAnimationMode): Boolean =
        mode == NoteEditorLaunchAnimationMode.CustomContainerTransform
}
