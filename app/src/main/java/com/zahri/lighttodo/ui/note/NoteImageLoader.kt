package com.zahri.lighttodo.ui.note

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class LoadedNoteImage(
    val bitmap: Bitmap,
    val aspectRatio: Float
)

object NoteImageDecodePolicy {
    fun sampleSizeFor(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return 1
        }
        var sample = 1
        while (
            sourceWidth / sample > targetWidth * 2 ||
            sourceHeight / sample > targetHeight * 2
        ) {
            sample *= 2
        }
        return sample
    }
}

fun loadNoteImage(
    context: Context,
    ref: String,
    targetWidthPx: Int,
    targetHeightPx: Int? = null
): LoadedNoteImage? {
    val file = NoteAttachmentStore.resolve(context, ref) ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val sourceWidth = bounds.outWidth.takeIf { it > 0 } ?: return null
    val sourceHeight = bounds.outHeight.takeIf { it > 0 } ?: return null
    val safeTargetWidth = targetWidthPx.coerceAtLeast(1)
    val safeTargetHeight = targetHeightPx?.coerceAtLeast(1)
        ?: ((safeTargetWidth.toLong() * sourceHeight) / sourceWidth)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    val bitmap = BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                targetWidth = safeTargetWidth,
                targetHeight = safeTargetHeight
            )
        }
    ) ?: return null
    return LoadedNoteImage(
        bitmap = bitmap,
        aspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    )
}
