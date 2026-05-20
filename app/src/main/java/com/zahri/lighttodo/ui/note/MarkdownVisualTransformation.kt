package com.zahri.lighttodo.ui.note

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rendered = MarkdownParser.renderForEditing(text.text)
        return TransformedText(
            text = rendered.text,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return rendered.originalToTransformed[offset.coerceIn(rendered.originalToTransformed.indices)]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return rendered.transformedToOriginal[offset.coerceIn(rendered.transformedToOriginal.indices)]
                }
            }
        )
    }
}
