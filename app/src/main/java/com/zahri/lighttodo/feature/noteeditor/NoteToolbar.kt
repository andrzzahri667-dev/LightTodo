package com.zahri.lighttodo.feature.noteeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppColors

@Composable
fun NoteToolbar(
    formatMode: MutableState<Boolean>,
    styleState: MarkdownStyleState,
    recording: Boolean,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onFormatAction: (MarkdownFormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (formatMode.value) {
        MarkdownFormattingToolbar(
            styleState = styleState,
            onAction = onFormatAction,
            onBack = { formatMode.value = false },
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToolbarActionSlot {
            ToolbarIconButton(
                onClick = onPickImage,
                contentDescription = stringResource(R.string.note_insert_image),
                imageVector = Icons.Default.Image
            )
        }
        ToolbarActionSlot {
            ToolbarIconButton(
                onClick = onTakePhoto,
                contentDescription = stringResource(R.string.note_take_photo),
                imageVector = Icons.Default.PhotoCamera
            )
        }
        ToolbarActionSlot {
            ToolbarIconButton(
                onClick = onToggleRecording,
                selected = recording,
                contentDescription = if (recording) {
                    stringResource(R.string.note_stop_recording)
                } else {
                    stringResource(R.string.note_record_audio)
                },
                imageVector = if (recording) Icons.Default.Stop else Icons.Default.Mic
            )
        }
        ToolbarActionSlot {
            ToolbarIconButton(
                onClick = { formatMode.value = true },
                contentDescription = stringResource(R.string.note_format_text),
                imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft
            )
        }
    }
}

@Composable
private fun RowScope.ToolbarActionSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    selected: Boolean = false,
    imageVector: ImageVector
) {
    val bg = if (selected) AppColors.Brand.copy(alpha = 0.16f) else Color.Transparent
    val tint = if (selected) AppColors.Brand else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
