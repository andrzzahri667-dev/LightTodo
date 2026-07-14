package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormattingToolbarSourceTest {

    private val toolbarSource by lazy {
        sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownFormattingToolbar.kt"
        ).readText()
    }

    @Test
    fun toolbarUsesAppAccentAndFamiliarBlockFormatIcons() {
        assertTrue(toolbarSource.contains("AppColors.Brand"))
        assertTrue(toolbarSource.contains("AppColors.BrandForegroundLight"))
        assertTrue(toolbarSource.contains("AppColors.BrandForegroundDark"))
        assertFalse(toolbarSource.contains("Color(0xFF007AFF)"))
        assertTrue(toolbarSource.contains("Icons.Default.CheckBox"))
        assertTrue(toolbarSource.contains("Icons.Default.FormatQuote"))
        assertFalse(toolbarSource.contains("FormatTextButton(\"[]\""))
        assertFalse(toolbarSource.contains("FormatTextButton(\">\""))
    }

    @Test
    fun toolbarButtonsExposeLocalizedActionAndSelectionSemantics() {
        assertTrue(toolbarSource.contains("contentDescription: String"))
        assertTrue(toolbarSource.contains("this.contentDescription = contentDescription"))
        assertTrue(toolbarSource.contains("selected = active"))
        assertTrue(toolbarSource.contains("role = Role.Button"))
        assertTrue(toolbarSource.contains("R.string.note_format_bold"))
        assertTrue(toolbarSource.contains("R.string.note_format_checkbox"))
        assertTrue(toolbarSource.contains("R.string.note_format_quote"))
    }

    @Test
    fun toolbarGroupsInlineHeadingAndBlockActions() {
        val toolbarBody = toolbarSource
            .substringAfter("fun MarkdownFormattingToolbar")
            .substringBefore("private fun FormatActionSlot")
        val inlineOrder = listOf(
            "MarkdownFormatAction.Bold",
            "MarkdownFormatAction.Italic",
            "MarkdownFormatAction.Strikethrough",
            "MarkdownFormatAction.Underline"
        ).map(toolbarBody::indexOf)

        assertTrue(inlineOrder.zipWithNext().all { (left, right) -> left < right })
        assertTrue(toolbarBody.countOccurrences("FormatGroupDivider()") >= 3)
    }

    @Test
    fun toolbarButtonsMeetMinimumTouchTarget() {
        assertTrue(toolbarSource.countOccurrences(".size(48.dp)") >= 2)
        assertFalse(toolbarSource.contains(".size(44.dp)"))
    }

    private fun String.countOccurrences(value: String): Int =
        windowed(value.length).count { it == value }
}
