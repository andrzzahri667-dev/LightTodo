package com.zahri.lighttodo.feature.noteeditor

object NoteAttachmentMarkdown {
    private const val IMAGE_PREFIX = "![image]("
    private const val AUDIO_PREFIX = "[audio "
    private const val MARKER_MIDDLE = "]("

    enum class Kind { Image, Audio }

    data class Attachment(
        val kind: Kind,
        val ref: String,
        val label: String
    )

    fun image(ref: String): String = "![image]($ref)"

    fun audio(ref: String, durationMillis: Long): String =
        "[audio ${formatDuration(durationMillis)}]($ref)"

    fun audio(ref: String, durationLabel: String): String =
        "[audio $durationLabel]($ref)"

    fun parseLine(line: String): Attachment? {
        val trimmed = line.trim()

        if (trimmed.startsWith(IMAGE_PREFIX) && trimmed.endsWith(")")) {
            val ref = trimmed.substring(IMAGE_PREFIX.length, trimmed.lastIndex)
            return ref.takeIf { it.isNotEmpty() }
                ?.let { Attachment(Kind.Image, ref = it, label = "Image") }
        }

        if (trimmed.startsWith(AUDIO_PREFIX) && trimmed.endsWith(")")) {
            val middle = trimmed.indexOf(MARKER_MIDDLE, startIndex = AUDIO_PREFIX.length)
            if (middle != -1) {
                val label = trimmed.substring(AUDIO_PREFIX.length, middle)
                val ref = trimmed.substring(middle + MARKER_MIDDLE.length, trimmed.lastIndex)
                if (label.isValidDurationLabel() && ref.isNotEmpty()) {
                    return Attachment(Kind.Audio, ref = ref, label = label)
                }
            }
        }

        return null
    }

    fun previewLabel(attachment: Attachment): String = when (attachment.kind) {
        Kind.Image -> "Image"
        Kind.Audio -> "Audio ${attachment.label}"
    }

    fun refsIn(markdown: String): List<String> =
        markdown.lineSequence()
            .mapNotNull { parseLine(it)?.ref }
            .toList()

    fun removedRefs(previousMarkdown: String, currentMarkdown: String): List<String> {
        val currentRefs = refsIn(currentMarkdown).toSet()
        return refsIn(previousMarkdown).filterNot { it in currentRefs }
    }

    fun removeAttachment(markdown: String, ref: String): String =
        markdown.lineSequence()
            .filterNot { parseLine(it)?.ref == ref }
            .joinToString("\n")

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun String.isValidDurationLabel(): Boolean {
        val colon = indexOf(':')
        if (colon <= 0 || colon != lastIndexOf(':')) return false
        val minutes = substring(0, colon)
        val seconds = substring(colon + 1)
        if (minutes.any { !it.isDigit() }) return false
        if (seconds.length != 2 || seconds.any { !it.isDigit() }) return false
        return seconds.toInt() in 0..59
    }
}
