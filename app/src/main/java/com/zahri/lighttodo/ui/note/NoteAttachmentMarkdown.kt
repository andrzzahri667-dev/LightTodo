package com.zahri.lighttodo.ui.note

object NoteAttachmentMarkdown {
    private val ImageRegex = Regex("^!\\[image]\\(([^)]+)\\)$")
    private val AudioRegex = Regex("^\\[audio ([0-9]{2}:[0-9]{2})]\\(([^)]+)\\)$")

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
        ImageRegex.matchEntire(trimmed)?.let { match ->
            return Attachment(Kind.Image, ref = match.groupValues[1], label = "Image")
        }
        AudioRegex.matchEntire(trimmed)?.let { match ->
            return Attachment(Kind.Audio, ref = match.groupValues[2], label = match.groupValues[1])
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
}
