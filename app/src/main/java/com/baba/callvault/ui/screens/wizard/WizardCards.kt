/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baba.callvault.R
import com.baba.callvault.ui.common.CvCard

/**
 * A branded radio-style option card: title + one-line description, a teal check that appears when
 * selected, and a teal border + faint teal tint in the selected state. Replaces stock RadioButtons.
 */
@Composable
internal fun OptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val container =
        if (selected) primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
        label = "optionBorder"
    )

    CvCard(
        onClick = onClick,
        color = container,
        border = false,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.border(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor,
            shape = MaterialTheme.shapes.large
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            SelectionDot(selected = selected)
        }
    }
}

/** A circular selection indicator: a hollow ring when unselected, a filled teal check when selected. */
@Composable
private fun SelectionDot(selected: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    if (selected) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
}

/**
 * A tappable folder picker row: a leading icon, the label + chosen folder name (or a "Required"
 * pill prompting selection), and a trailing chevron. Required-but-unset rows read with a coral tint.
 */
@Composable
internal fun FolderPickerCard(
    icon: ImageVector,
    label: String,
    chosenName: String?,
    onClick: () -> Unit
) {
    val hasFolder = chosenName != null
    CvCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = chosenName ?: stringResource(R.string.wizard_ui_folder_choose),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasFolder) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** A clear toggle row: title + one-line description on the left, a teal [Switch] on the right. */
@Composable
internal fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    CvCard(onClick = { onCheckedChange(!checked) }, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

/**
 * Like [NoteCard], but coral — for a choice that leaves CallVault doing nothing.
 *
 * Every colour is stated: the scheme's M3 defaults resolve to coral here, so a card that leant on them
 * would be indistinguishable from an ordinary note.
 */
@Composable
internal fun WarningCard(text: String) {
    CvCard(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
        border = false,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** A muted helper/note line set inside a faint surface card — for contextual guidance. */
@Composable
internal fun NoteCard(text: String) {
    CvCard(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = false,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
