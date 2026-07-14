package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MarkdownRenderStyleTest {
    @Test
    fun darkStyleUsesReadableMarkdownColors() {
        val light = MarkdownRenderStyle.forDarkMode(false)
        val dark = MarkdownRenderStyle.forDarkMode(true)

        assertEquals(0xFF202124.toInt(), light.bodyTextColor)
        assertEquals(0xFF636366.toInt(), light.quoteTextColor)
        assertEquals(0xFF8A4B00.toInt(), light.linkColor)
        assertEquals(0xFFFFFFFF.toInt(), dark.bodyTextColor)
        assertEquals(dark.bodyTextColor, dark.bulletColor)
        assertEquals(0xFFE5E5EA.toInt(), dark.codeTextColor)
        assertEquals(0x24FFFFFF, dark.codeBackgroundColor)
        assertNotEquals(light.quoteBarColor, dark.quoteBarColor)
        assertNotEquals(light.dividerColor, dark.dividerColor)
        assertNotEquals(light.imagePlaceholderColor, dark.imagePlaceholderColor)
        assertNotEquals(light.audioBackgroundColor, dark.audioBackgroundColor)
        assertNotEquals(light.audioTextColor, dark.audioTextColor)
        assertNotEquals(dark.audioBackgroundColor, dark.audioActiveBackgroundColor)
    }
}
