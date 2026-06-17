package com.zahri.lighttodo.domain.markdown

internal object MarkdownKeyboardDeletePolicy {
    fun shouldRequestPreviousMediaDelete(
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ): Boolean {
        return selectionStart == selectionEnd &&
            selectionStart == 0 &&
            selectionStart in 0..text.length
    }
}
