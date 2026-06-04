package com.zahri.lighttodo.ui.note

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSpanSourceTest {
    @Test
    fun bulletSpanOnlyDrawsBulletOnFirstVisualLine() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/MarkdownSpan.kt").readText()
        val bulletSpan = source.substringAfter("class MarkdownBulletSpan(")
            .substringBefore("/** Adds a small leading gutter for ordered list rows")

        assertTrue(bulletSpan.contains("if (!first) return"))
    }
}
