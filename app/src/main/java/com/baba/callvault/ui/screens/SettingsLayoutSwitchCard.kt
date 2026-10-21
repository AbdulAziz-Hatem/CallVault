/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baba.callvault.R
import com.baba.callvault.data.AppPreferences
import com.baba.callvault.ui.viewmodels.SettingsActions

/** Corner radius of the layout switch card, matching the new screen's group cards. */
private val CARD_RADIUS = 20.dp

/** The card is a button: never shorter than a comfortable touch target. */
private val MIN_CARD_HEIGHT = 56.dp

/**
 * The one way between the two Settings screens: the same card, in the same place, at the top of both (under
 * the Shizuku mode row where a page has one), with only the wording changing. Tapping it flips the stored
 * choice and refreshes, so the other screen shows at once. Both screens use the same preferences, so
 * switching either way loses nothing. Replaces the Debug switch (2026-10-21).
 */
@Composable
internal fun SettingsLayoutSwitchCard(preferences: AppPreferences, actions: SettingsActions) {
    // Read on each composition: the screen showing this card is the answer, and a refresh redraws it.
    val isClassic = preferences.isClassicSettingsScreen()
    val title = stringResource(
        if (isClassic) R.string.settings_layout_try_new_title else R.string.settings_layout_back_to_classic_title
    )
    val subtitle = stringResource(
        if (isClassic) R.string.settings_layout_try_new_subtitle else R.string.settings_layout_back_to_classic_subtitle
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_CARD_HEIGHT)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClickLabel = title) {
                preferences.setClassicSettingsScreen(!isClassic)
                actions.refreshSettings()
            }
            .semantics(mergeDescendants = true) { contentDescription = title }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ViewAgenda,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(14.dp),
        )
    }
}
