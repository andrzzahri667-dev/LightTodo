package com.zahri.lighttodo.ui.note

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

object NoteAttachmentStore {
    private const val ROOT_DIR = "note_attachments"
    private const val IMAGE_DIR = "image"
    private const val AUDIO_DIR = "audio"
    private const val REF_PREFIX = "lighttodo://attachment"

    fun createImageFile(context: Context): File =
        uniqueFile(context, IMAGE_DIR, "photo", "jpg")

    fun createAudioFile(context: Context): File =
        uniqueFile(context, AUDIO_DIR, "audio", "m4a")

    fun imageRef(file: File): String = "$REF_PREFIX/$IMAGE_DIR/${file.name}"

    fun audioRef(file: File): String = "$REF_PREFIX/$AUDIO_DIR/${file.name}"

    fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyImageFromUri(context: Context, uri: Uri): File {
        val extension = imageExtension(context, uri)
        val target = uniqueFile(context, IMAGE_DIR, "image", extension)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open selected image" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    fun importAttachment(
        context: Context,
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
        val target = uniqueImportedFile(context, type, safeName)
        target.parentFile?.mkdirs()
        input.use { source ->
            target.outputStream().use { output -> source.copyTo(output) }
        }
        return when (kind) {
            NoteAttachmentMarkdown.Kind.Image -> imageRef(target)
            NoteAttachmentMarkdown.Kind.Audio -> audioRef(target)
        }
    }

    fun resolve(context: Context, ref: String): File? {
        if (!ref.startsWith("$REF_PREFIX/")) return null
        val parts = ref.removePrefix("$REF_PREFIX/").split('/')
        if (parts.size != 2) return null
        val type = parts[0]
        val name = parts[1].takeIf { it.isNotBlank() } ?: return null
        if (name.contains("..") || name.contains(File.separatorChar)) return null
        if (type != IMAGE_DIR && type != AUDIO_DIR) return null
        return File(attachmentDir(context, type), name).takeIf { it.exists() }
    }

    fun delete(context: Context, ref: String) {
        resolve(context, ref)?.delete()
    }

    fun deleteRefs(context: Context, refs: Iterable<String>) {
        refs.forEach { delete(context, it) }
    }

    fun deleteRemovedRefs(context: Context, previousContent: String, currentContent: String) {
        deleteRefs(context, NoteAttachmentMarkdown.removedRefs(previousContent, currentContent))
    }

    fun deleteUnreferenced(context: Context, referencedRefs: Set<String>) {
        attachmentFiles(context).forEach { file ->
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

    private fun uniqueFile(context: Context, type: String, prefix: String, extension: String): File {
        val dir = attachmentDir(context, type)
        dir.mkdirs()
        val safeExtension = extension.trimStart('.').ifBlank { "bin" }
        return File(dir, "${prefix}_${System.currentTimeMillis()}.$safeExtension")
    }

    private fun uniqueImportedFile(context: Context, type: String, fileName: String): File {
        val dir = attachmentDir(context, type)
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

    private fun attachmentDir(context: Context, type: String): File =
        File(File(context.filesDir, ROOT_DIR), type)

    private fun attachmentFiles(context: Context): List<File> =
        listOf(IMAGE_DIR, AUDIO_DIR)
            .flatMap { type -> attachmentDir(context, type).listFiles()?.toList().orEmpty() }
            .filter { it.isFile }

    private fun imageExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val fromMime = mimeType?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
        return when (fromMime?.lowercase()) {
            "png", "webp", "gif", "jpg", "jpeg" -> fromMime.lowercase()
            else -> "jpg"
        }
    }
}
