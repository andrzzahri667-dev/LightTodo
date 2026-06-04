package com.zahri.lighttodo.ui.note

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import android.view.View
import android.util.LruCache
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/** Marker interface for all markdown-related spans. Enables bulk removal. */
interface MarkdownSpan

// ─── Syntax dimming ────────────────────────────────────────────

/** Hides markdown syntax characters on inactive lines while keeping source text editable. */
class MarkdownSyntaxSpan : ReplacementSpan(), MarkdownSpan {
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int = 0

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) = Unit
}

// ─── Headings ──────────────────────────────────────────────────

/**
 * Heading font size. Scales from Xiaomi Notes' RichEditorConfig:
 *   h1=1.25  h2=1.125  h3=1.0625  h4+=1.0
 */
class MarkdownHeadingSpan(val level: Int) : RelativeSizeSpan(
    when (level) {
        1 -> 1.25f
        2 -> 1.125f
        3 -> 1.0625f
        else -> 1.0f
    }
), MarkdownSpan

// ─── Bullet (unordered list) ──────────────────────────────────

/** Draws a small filled circle as bullet marker. */
class MarkdownBulletSpan(
    private val bulletRadius: Float = 8f,
    private val margin: Int = 50
) : LeadingMarginSpan, MarkdownSpan {

    override fun getLeadingMargin(first: Boolean): Int = margin

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: android.text.Layout?
    ) {
        if (!first) return
        val style = paint.style
        val color = paint.color
        val alpha = paint.alpha
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#202124")
        paint.alpha = 255
        val cx = x + dir * (margin * 0.34f)
        val cy = baseline + (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawCircle(cx, cy, bulletRadius, paint)
        paint.style = style
        paint.color = color
        paint.alpha = alpha
    }
}

/** Adds a small leading gutter for ordered list rows while keeping the visible number text. */
class MarkdownOrderedListSpan(
    private val margin: Int = 8
) : LeadingMarginSpan, MarkdownSpan {
    override fun getLeadingMargin(first: Boolean): Int = margin

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: android.text.Layout?
    ) = Unit
}

// ─── Checkbox (task list) ──────────────────────────────────────

/** Draws a rounded-rect checkbox. Checked items get strikethrough + low alpha. */
class MarkdownCheckboxSpan(
    val checked: Boolean,
    private val boxSize: Float = 40f,
    private val margin: Int = 68
) : LeadingMarginSpan, MarkdownSpan {

    override fun getLeadingMargin(first: Boolean): Int = margin

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: android.text.Layout?
    ) {
        val cx = x + dir * (margin * 0.42f)
        val cy = baseline + (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        val half = boxSize / 2f
        val r = half * 0.42f

        val rect = android.graphics.RectF(cx - half, cy - half, cx + half, cy + half)
        val path = Path()
        val style = paint.style
        val color = paint.color
        val alpha = paint.alpha

        if (checked) {
            // Filled checkbox with brand orange
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FF9F0A")
            paint.alpha = 255
            path.addRoundRect(rect, r, r, Path.Direction.CW)
            canvas.drawPath(path, paint)

            // Checkmark
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.WHITE
            val s = half * 0.55f
            canvas.drawLine(cx - s * 0.6f, cy, cx - s * 0.1f, cy + s * 0.6f, paint)
            canvas.drawLine(cx - s * 0.1f, cy + s * 0.6f, cx + s * 0.7f, cy - s * 0.5f, paint)
        } else {
            // Empty checkbox
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.4f
            paint.color = Color.parseColor("#D0D0D0")
            paint.alpha = 255
            path.addRoundRect(rect, r, r, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }

        paint.style = style
        paint.color = color
        paint.alpha = alpha
    }
}

/** Applies alpha=102 (40%) to text of checked items. */
class MarkdownCheckedAlphaSpan : CharacterStyle(), MarkdownSpan {
    override fun updateDrawState(tp: TextPaint) {
        tp.alpha = 102
    }
}

// ─── Blockquote ────────────────────────────────────────────────

/** Indents blockquote text. A separate foreground color span handles the text color. */
class MarkdownQuoteSpan(
    private val barWidth: Float = 3f * 2f,  // 3dp @2x
    private val margin: Int = 40,
    @ColorInt private val barColor: Int = Color.parseColor("#C7C7CC")
) : LeadingMarginSpan, MarkdownSpan {

    override fun getLeadingMargin(first: Boolean): Int = margin

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: android.text.Layout?
    ) {
        val style = paint.style
        val color = paint.color
        paint.style = Paint.Style.FILL
        paint.color = barColor
        val left = (x + 12 * dir).toFloat()
        canvas.drawRect(left, top.toFloat(), left + barWidth, bottom.toFloat(), paint)
        paint.style = style
        paint.color = color
    }
}

// ─── Horizontal rule ───────────────────────────────────────────

/** Draws a full-width thin line. The --- text in the buffer is hidden via MarkdownSyntaxSpan. */
class MarkdownHrSpan(
    @ColorInt private val lineColor: Int = Color.parseColor("#D6D6D6")
) : LeadingMarginSpan, MarkdownSpan {

    override fun getLeadingMargin(first: Boolean): Int = 0

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: android.text.Layout?
    ) {
        if (!first) return
        val style = paint.style
        val color = paint.color
        val alpha = paint.alpha
        paint.style = Paint.Style.FILL
        paint.color = lineColor
        paint.alpha = 255
        val y = (top + bottom) / 2f
        val width = canvas.width.toFloat()
        canvas.drawRect(0f, y - 1f, width, y + 1f, paint)
        paint.style = style
        paint.color = color
        paint.alpha = alpha
    }
}

// ─── Inline code ───────────────────────────────────────────────

/** Monospace font + muted gray text for inline code. */
class MarkdownInlineCodeSpan : CharacterStyle(), MarkdownSpan {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = Typeface.MONOSPACE
        tp.color = Color.parseColor("#636366")
    }
}

// ─── Code block ────────────────────────────────────────────────

/** Monospace font + muted gray text for fenced code blocks. */
class MarkdownCodeBlockSpan : CharacterStyle(), MarkdownSpan {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = Typeface.MONOSPACE
        tp.color = Color.parseColor("#636366")
    }
}

// ─── Link URL hiding ───────────────────────────────────────────

/** Removes the visual width of the hidden ](url) suffix while keeping source text editable. */
class MarkdownLinkUrlSpan : ReplacementSpan(), MarkdownSpan {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int = 0

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) = Unit
}

/** Clickable link span storing a URL for click dispatch. */
class MarkdownLinkSpan(
    val url: String
) : android.text.style.ClickableSpan(), MarkdownSpan {
    override fun onClick(widget: View) {
        // no-op; click handled by MarkdownEditText.onTouchEvent -> findLinkSpanAt
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.color = Color.parseColor("#FF9F0A")
        ds.isUnderlineText = true
    }
}

class MarkdownImageSpan(
    private val context: Context,
    val attachment: NoteAttachmentMarkdown.Attachment
) : ReplacementSpan(), MarkdownSpan {
    private val density = context.resources.displayMetrics.density
    private val maxBoxWidthPx = minOf(
        context.resources.displayMetrics.widthPixels - (56f * density).roundToInt(),
        (360f * density).roundToInt()
    ).coerceAtLeast((180f * density).roundToInt())
    private val maxBoxHeightPx = (300f * density).roundToInt()
    private val fallbackWidthPx = (180f * density).roundToInt()
    private val fallbackHeightPx = (120f * density).roundToInt()
    private val radiusPx = 10f * density
    private val verticalPaddingPx = (2f * density).roundToInt()

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val size = displaySize()
        fm?.let {
            it.ascent = -(size.second + verticalPaddingPx)
            it.descent = verticalPaddingPx
            it.top = it.ascent
            it.bottom = it.descent
        }
        return size.first
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val size = displaySize()
        val rect = RectF(
            x,
            (bottom - size.second - verticalPaddingPx).toFloat(),
            x + size.first,
            (bottom - verticalPaddingPx).toFloat()
        )
        val oldStyle = paint.style
        val oldColor = paint.color
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F1F3")
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

        val bitmap = MarkdownBitmapCache.get(context, attachment.ref, size.first)
        if (bitmap != null) {
            val src = Rect(0, 0, bitmap.width, bitmap.height)
            val path = Path().apply { addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(bitmap, src, rect, paint)
            canvas.restore()
        } else {
            paint.color = Color.parseColor("#8E8E93")
            paint.textSize = 14f * density
            val label = "Image"
            canvas.drawText(label, rect.left + 16f * density, rect.centerY() - (paint.ascent() + paint.descent()) / 2f, paint)
        }
        paint.style = oldStyle
        paint.color = oldColor
    }

    private fun displaySize(): Pair<Int, Int> {
        val sourceSize = MarkdownBitmapCache.size(context, attachment.ref) ?: return fallbackWidthPx to fallbackHeightPx
        val sourceWidth = sourceSize.first.coerceAtLeast(1)
        val sourceHeight = sourceSize.second.coerceAtLeast(1)
        var targetWidth = maxBoxWidthPx
        var targetHeight = (targetWidth * (sourceHeight.toFloat() / sourceWidth.toFloat())).roundToInt()
        if (targetHeight > maxBoxHeightPx) {
            targetHeight = maxBoxHeightPx
            targetWidth = (targetHeight * (sourceWidth.toFloat() / sourceHeight.toFloat())).roundToInt()
        }
        return targetWidth.coerceAtLeast(1) to targetHeight.coerceAtLeast(1)
    }
}

class MarkdownAudioSpan(
    private val context: Context,
    val attachment: NoteAttachmentMarkdown.Attachment
) : ReplacementSpan(), MarkdownSpan {
    private val density = context.resources.displayMetrics.density
    private val widthPx = (220f * density).roundToInt()
    private val heightPx = (54f * density).roundToInt()

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        fm?.let {
            val padding = (8f * density).roundToInt()
            it.ascent = -heightPx - padding
            it.descent = padding
            it.top = it.ascent
            it.bottom = it.descent
        }
        return widthPx
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val padding = 8f * density
        val rect = RectF(x, bottom - heightPx - padding, x + widthPx, bottom - padding)
        val oldStyle = paint.style
        val oldColor = paint.color
        val oldStroke = paint.strokeWidth
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FFF1DA")
        canvas.drawRoundRect(rect, heightPx / 2f, heightPx / 2f, paint)

        val cx = rect.left + 28f * density
        val cy = rect.centerY()
        paint.color = Color.parseColor("#FF9F0A")
        val triangle = Path().apply {
            moveTo(cx - 5f * density, cy - 9f * density)
            lineTo(cx - 5f * density, cy + 9f * density)
            lineTo(cx + 10f * density, cy)
            close()
        }
        canvas.drawPath(triangle, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * density
        paint.color = Color.parseColor("#FFB340")
        var waveX = rect.left + 58f * density
        val waveHeights = intArrayOf(10, 18, 14, 24, 12, 20, 10)
        for (waveHeight in waveHeights) {
            val h = waveHeight * density
            canvas.drawLine(waveX, cy - h / 2f, waveX, cy + h / 2f, paint)
            waveX += 8f * density
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#5C4A26")
        paint.textSize = 15f * density
        canvas.drawText(attachment.label, rect.right - 56f * density, cy - (paint.ascent() + paint.descent()) / 2f, paint)

        paint.style = oldStyle
        paint.color = oldColor
        paint.strokeWidth = oldStroke
    }
}

private object MarkdownBitmapCache {
    private val bitmapCache = LruCache<String, Bitmap>(8)
    private val sizeCache = LruCache<String, Pair<Int, Int>>(32)

    fun size(context: Context, ref: String): Pair<Int, Int>? {
        sizeCache.get(ref)?.let { return it }
        val file = NoteAttachmentStore.resolve(context, ref) ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val size = opts.outWidth.takeIf { it > 0 }?.let { it to opts.outHeight } ?: return null
        sizeCache.put(ref, size)
        return size
    }

    fun get(context: Context, ref: String, targetWidth: Int): Bitmap? {
        bitmapCache.get(ref)?.let { return it }
        val file = NoteAttachmentStore.resolve(context, ref) ?: return null
        val size = size(context, ref) ?: return null
        val sample = calculateInSampleSize(size.first, targetWidth)
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        bitmapCache.put(ref, bitmap)
        return bitmap
    }

    private fun calculateInSampleSize(width: Int, targetWidth: Int): Int {
        var sample = 1
        while (width / sample > targetWidth * 2) sample *= 2
        return sample
    }
}
