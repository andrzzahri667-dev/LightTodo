package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEditorIntegrationSourceTest {

    private val editorSource by lazy {
        sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownEditText.kt"
        ).readText()
    }

    @Test
    fun editorOnlyRequestsListContinuationForAnExactInsertedNewline() {
        assertTrue(editorSource.contains("MarkdownEditorPolicy.continuationNewlineIndex("))
        assertTrue(editorSource.contains("isPaste = isPasting"))
        assertTrue(editorSource.contains("isPasting = true"))
        assertFalse(
            editorSource.contains("s.subSequence(start, insertedEnd).indexOf('\\n')")
        )
        val inputConnection = editorSource
            .substringAfter("override fun onCreateInputConnection")
            .substringBefore("fun setContentWithoutTrigger")
        assertFalse(inputConnection.contains("clipboardText()"))
        assertFalse(inputConnection.contains("isPasting ="))
    }

    @Test
    fun editorUsesHalfOpenSpanHitsAndRejectsTouchesBelowTextLayout() {
        assertTrue(editorSource.contains("MarkdownEditorPolicy.containsOffset("))
        assertTrue(editorSource.contains("MarkdownEditorPolicy.containsVertical("))
        assertFalse(editorSource.contains("offset in start..end"))
    }

    @Test
    fun viewModelContentWritebackRestoresBothSelectionEndpoints() {
        assertTrue(editorSource.contains("MarkdownEditorPolicy.clampSelection("))
        assertTrue(editorSource.contains("setSelection(selection.start, selection.end)"))
    }

    @Test
    fun textAndSelectionChangesUseLineScopedSpanRefresh() {
        val afterTextChanged = editorSource
            .substringAfter("override fun afterTextChanged")
            .substringBefore("override fun onSelectionChanged")
        val onSelectionChanged = editorSource
            .substringAfter("override fun onSelectionChanged")
            .substringBefore("override fun onTouchEvent")
        val applierSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownSpanApplier.kt"
        ).readText()

        assertTrue(afterTextChanged.contains("MarkdownEditorPolicy.requiresFullSpanRefresh("))
        assertTrue(afterTextChanged.contains("MarkdownSpanApplier.applyChangedLine("))
        assertTrue(onSelectionChanged.contains("MarkdownSpanApplier.applyActiveLineChange("))
        assertFalse(onSelectionChanged.contains("MarkdownSpanApplier.apply("))
        assertTrue(applierSource.contains("fun applyChangedLine("))
        assertTrue(applierSource.contains("fun applyActiveLineChange("))
    }
}
