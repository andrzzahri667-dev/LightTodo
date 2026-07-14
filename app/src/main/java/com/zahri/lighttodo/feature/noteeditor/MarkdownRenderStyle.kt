package com.zahri.lighttodo.feature.noteeditor

data class MarkdownRenderStyle(
    val bodyTextColor: Int,
    val hintTextColor: Int,
    val bulletColor: Int,
    val quoteTextColor: Int,
    val quoteBarColor: Int,
    val dividerColor: Int,
    val codeTextColor: Int,
    val codeBackgroundColor: Int,
    val checkboxOutlineColor: Int,
    val checkboxFillColor: Int,
    val checkboxMarkColor: Int,
    val linkColor: Int,
    val imagePlaceholderColor: Int,
    val imagePlaceholderTextColor: Int,
    val audioBackgroundColor: Int,
    val audioActiveBackgroundColor: Int,
    val audioAccentColor: Int,
    val audioWaveColor: Int,
    val audioTextColor: Int
) {
    companion object {
        private val Light = MarkdownRenderStyle(
            bodyTextColor = 0xFF202124.toInt(),
            hintTextColor = 0xFF8E8E93.toInt(),
            bulletColor = 0xFF202124.toInt(),
            quoteTextColor = 0xFF636366.toInt(),
            quoteBarColor = 0xFFC7C7CC.toInt(),
            dividerColor = 0xFFD6D6D6.toInt(),
            codeTextColor = 0xFF636366.toInt(),
            codeBackgroundColor = 0x0F000000,
            checkboxOutlineColor = 0xFFD0D0D0.toInt(),
            checkboxFillColor = 0xFFFF9F0A.toInt(),
            checkboxMarkColor = 0xFFFFFFFF.toInt(),
            linkColor = 0xFF8A4B00.toInt(),
            imagePlaceholderColor = 0xFFF1F1F3.toInt(),
            imagePlaceholderTextColor = 0xFF8E8E93.toInt(),
            audioBackgroundColor = 0xFFFFF1DA.toInt(),
            audioActiveBackgroundColor = 0xFFFFE4B8.toInt(),
            audioAccentColor = 0xFFFF9F0A.toInt(),
            audioWaveColor = 0xFFFFB340.toInt(),
            audioTextColor = 0xFF5C4A26.toInt()
        )

        private val Dark = MarkdownRenderStyle(
            bodyTextColor = 0xFFFFFFFF.toInt(),
            hintTextColor = 0xFF8E8E93.toInt(),
            bulletColor = 0xFFFFFFFF.toInt(),
            quoteTextColor = 0xFFAEAEB2.toInt(),
            quoteBarColor = 0xFF636366.toInt(),
            dividerColor = 0xFF38383A.toInt(),
            codeTextColor = 0xFFE5E5EA.toInt(),
            codeBackgroundColor = 0x24FFFFFF,
            checkboxOutlineColor = 0xFF636366.toInt(),
            checkboxFillColor = 0xFFFF9F0A.toInt(),
            checkboxMarkColor = 0xFF1A1100.toInt(),
            linkColor = 0xFFFFB340.toInt(),
            imagePlaceholderColor = 0xFF2C2C2E.toInt(),
            imagePlaceholderTextColor = 0xFFAEAEB2.toInt(),
            audioBackgroundColor = 0xFF3A2B12.toInt(),
            audioActiveBackgroundColor = 0xFF4A3515.toInt(),
            audioAccentColor = 0xFFFFB340.toInt(),
            audioWaveColor = 0xFFFF9F0A.toInt(),
            audioTextColor = 0xFFFFE4B3.toInt()
        )

        fun forDarkMode(isDark: Boolean): MarkdownRenderStyle =
            if (isDark) Dark else Light
    }
}
