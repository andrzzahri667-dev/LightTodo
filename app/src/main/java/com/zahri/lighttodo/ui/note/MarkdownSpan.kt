package com.zahri.lighttodo.ui.note

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import androidx.annotation.ColorInt

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
