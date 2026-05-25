package com.zahri.lighttodo.ui.note

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppType

@Composable
fun NoteEmptyPlaceholder(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.note_no_title),
        style = AppType.body,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    )
}
