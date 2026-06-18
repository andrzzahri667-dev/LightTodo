package com.zahri.lighttodo.data.note

import android.net.Uri
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.file.FileGateway
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import java.io.File
import java.io.InputStream

class NoteAttachmentStore(
    private val filesDir: File,
    private val fileGateway: FileGateway
) : NoteAttachmentGateway {
    override fun createImageFile(): File =
        uniqueFile(IMAGE_DIR, "photo", "jpg")

    override fun createAudioFile(): File =
        uniqueFile(AUDIO_DIR, "audio", "m4a")

    override fun imageRef(file: File): String = "$REF_PREFIX/$IMAGE_DIR/${file.name}"

    override fun audioRef(file: File): String = "$REF_PREFIX/$AUDIO_DIR/${file.name}"

    override fun fileProviderUri(file: File): Uri =
        fileGateway.providerUri(file)

    override fun copyImageFromUri(uri: Uri): File {
        val extension = fileGateway.imageExtension(uri)
        val target = uniqueFile(IMAGE_DIR, "image", extension)
        fileGateway.copyUriToFile(uri, target)
        return target
    }

    override fun importAttachment(
        kind: NoteAttachmentMarkdown.Kind,
        fileName: String,
        input: InputStream
    ): String {
        val type = when (kind) {
            NoteAttachmentMarkdown.Kind.Image -> IMAGE_DIR
            NoteAttachmentMarkdown.Kind.Audio -> AUDIO_DIR
        }
        val safeName = fileName
            .substringAfterLast('/')
            .substringAfterLast(File.separatorChar)
            .takeIf { it.isNotBlank() && !it.contains("..") }
            ?: "attachment_${System.currentTimeMillis()}.bin"
        val target = uniqueImportedFile(type, safeName)
        target.parentFile?.mkdirs()
        input.use { source ->
            target.outputStream().use { output -> source.copyTo(output) }
        }
        return when (kind) {
            NoteAttachmentMarkdown.Kind.Image -> imageRef(target)
            NoteAttachmentMarkdown.Kind.Audio -> audioRef(target)
        }
    }

    override fun resolveAttachment(ref: String): File? {
        if (!ref.startsWith("$REF_PREFIX/")) return null
        val parts = ref.removePrefix("$REF_PREFIX/").split('/')
        if (parts.size != 2) return null
        val type = parts[0]
        val name = parts[1].takeIf { it.isNotBlank() } ?: return null
        if (name.contains("..") || name.contains(File.separatorChar)) return null
        if (type != IMAGE_DIR && type != AUDIO_DIR) return null
        return File(attachmentDir(type), name).takeIf { it.exists() }
    }

    override fun deleteRefs(refs: Iterable<String>) {
        refs.forEach { delete(it) }
    }

    override fun deleteRemovedRefs(previousContent: String, currentContent: String) {
        deleteRefs(NoteAttachmentMarkdown.removedRefs(previousContent, currentContent))
    }

    override fun deleteUnreferenced(referencedRefs: Set<String>) {
        attachmentFiles().forEach { file ->
            val ref = when (file.parentFile?.name) {
                IMAGE_DIR -> imageRef(file)
                AUDIO_DIR -> audioRef(file)
                else -> null
            }
            if (ref != null && ref !in referencedRefs) {
                file.delete()
            }
        }
    }

    private fun delete(ref: String) {
        resolveAttachment(ref)?.delete()
    }

    private fun uniqueFile(type: String, prefix: String, extension: String): File {
        val dir = attachmentDir(type)
        dir.mkdirs()
        val safeExtension = extension.trimStart('.').ifBlank { "bin" }
        return File(dir, "${prefix}_${System.currentTimeMillis()}.$safeExtension")
    }

    private fun uniqueImportedFile(type: String, fileName: String): File {
        val dir = attachmentDir(type)
        dir.mkdirs()
        val baseName = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        var candidate = File(dir, fileName)
        var suffix = 1
        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) {
                "${baseName}_$suffix"
            } else {
                "${baseName}_$suffix.$extension"
            }
            candidate = File(dir, nextName)
            suffix += 1
        }
        return candidate
    }

    private fun attachmentDir(type: String): File =
        File(File(filesDir, ROOT_DIR), type)

    private fun attachmentFiles(): List<File> =
        listOf(IMAGE_DIR, AUDIO_DIR)
            .flatMap { type -> attachmentDir(type).listFiles()?.toList().orEmpty() }
            .filter { it.isFile }

    private companion object {
        const val ROOT_DIR = "note_attachments"
        const val IMAGE_DIR = "image"
        const val AUDIO_DIR = "audio"
        const val REF_PREFIX = "lighttodo://attachment"
    }
}
