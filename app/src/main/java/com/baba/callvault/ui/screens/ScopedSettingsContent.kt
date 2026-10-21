/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baba.callvault.R
import com.baba.callvault.data.AppPreferences
import com.baba.callvault.ui.common.CvScaffold
import com.baba.callvault.ui.common.OemGateNotice
import com.baba.callvault.ui.viewmodels.ContactPickerState
import com.baba.callvault.ui.viewmodels.SettingsActions

/**
 * The new Settings screen (design option C): the groups for [scope] as folding cards, each with a one-line
 * summary, the Shizuku mode row above them, and two links at the foot. It sits BESIDE the classic
 * [SettingsContent], not in place of it; Debug ▸ "Use the classic Settings screen" picks which one opens.
 *
 * Every card's body is made of the classic screen's own rows, so both screens read and write the same
 * preferences and neither can offer a setting the other lacks. See
 * `docs/dev-notes/2026-10-20-settings-per-page-plan.md`.
 *
 * Takes the same callbacks as [SettingsContent], plus the scope and a way to change it (the "All settings"
 * link, and the Summaries language row's link to Transcripts).
 */
@Composable
fun ScopedSettingsContent(
    preferences: AppPreferences,
    updateTrigger: Int,
    actions: SettingsActions,
    contactPickerState: ContactPickerState?,
    onBack: () -> Unit,
    onSelectFolder: () -> Unit,
    onSelectDriveFolder: () -> Unit,
    onOpenContactsIncoming: () -> Unit,
    onOpenContactsOutgoing: () -> Unit,
    onConfirmContacts: (Set<String>) -> Unit,
    onDismissContacts: () -> Unit,
    onShareLogs: () -> Unit,
    onSaveLogs: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
    scope: SettingsScope,
    onScopeChange: (SettingsScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    // The open group, remembered per scope (scope name -> group key), so Recordings does not reopen on
    // whatever was open under Transcripts. New keys: the classic screen's section state is left alone.
    // A HashMap so it saves into the Bundle as it is.
    var openGroups by rememberSaveable { mutableStateOf(HashMap<String, String>()) }
    val openGroup = SettingsGroup.fromKey(openGroups[scope.name])
    fun setOpenGroup(target: SettingsScope, group: SettingsGroup?) {
        openGroups = HashMap(openGroups).apply {
            if (group == null) remove(target.name) else put(target.name, group.key)
        }
    }

    // Bumped by the links, so the list scrolls to what they opened once the new scope has been composed.
    var scrollRequest by remember { mutableIntStateOf(0) }
    fun jumpTo(target: SettingsScope, group: SettingsGroup?) {
        if (group != null) setOpenGroup(target, group)
        if (target != scope) onScopeChange(target)
        scrollRequest++
    }

    val groups = SettingsGroups.forScope(scope)
    val showsModeRow = scope == SettingsScope.RECORDINGS || scope == SettingsScope.ALL
    val listState = rememberLazyListState()
    LaunchedEffect(scrollRequest) {
        if (scrollRequest == 0) return@LaunchedEffect
        val groupIndex = openGroup?.let { groups.indexOf(it) }?.takeIf { it >= 0 }
        val index = if (groupIndex == null) 0 else groupIndex + if (showsModeRow) 1 else 0
        listState.animateScrollToItem(index)
    }

    CvScaffold(
        modifier = modifier.fillMaxSize(),
        title = stringResource(titleOf(scope)),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showsModeRow) {
                item(key = "mode") {
                    // The same row as the classic screen, kept on purpose (maintainer, 2026-10-21): it decides
                    // which recording engine runs, so it stays the first thing on the recording pages.
                    Column {
                        PrivilegedModeSubSection(onModeChanged = { actions.refreshSettings() })
                        OemGateNotice(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            items(groups, key = { it.key }) { group ->
                if (group == SettingsGroup.BACKUP) {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        ExportImportRow(onExport = onExportSettings, onImport = onImportSettings)
                    }
                    return@items
                }
                val expanded = openGroup == group
                SettingsGroupCard(
                    icon = iconOf(group),
                    title = stringResource(titleOf(group)),
                    summary = groupSummaryText(group, preferences, updateTrigger, expanded, actions),
                    expanded = expanded,
                    onToggle = { setOpenGroup(scope, if (expanded) null else group) },
                ) {
                    GroupBody(
                        group = group,
                        preferences = preferences,
                        updateTrigger = updateTrigger,
                        actions = actions,
                        onSelectFolder = onSelectFolder,
                        onSelectDriveFolder = onSelectDriveFolder,
                        onOpenContactsIncoming = onOpenContactsIncoming,
                        onOpenContactsOutgoing = onOpenContactsOutgoing,
                        onShareLogs = onShareLogs,
                        onSaveLogs = onSaveLogs,
                        onShowLicenses = { showLicensesDialog = true },
                        // Transcripts and summaries share ONE language on purpose; Summaries only links to it.
                        onChangeLanguage = {
                            val target = if (scope == SettingsScope.ALL) SettingsScope.ALL else SettingsScope.TRANSCRIPTS
                            jumpTo(target, SettingsGroup.TRANSCRIPTION)
                        },
                    )
                }
            }
            item(key = "footer") {
                Column {
                    SettingsFooterLink(
                        text = stringResource(R.string.settings_link_share_debug_report),
                        onClick = { jumpTo(SettingsScope.ALL, SettingsGroup.DEBUG) },
                    )
                    if (scope != SettingsScope.ALL) {
                        SettingsFooterLink(
                            text = stringResource(R.string.settings_link_all_settings),
                            onClick = { jumpTo(SettingsScope.ALL, null) },
                        )
                    }
                }
            }
        }
    }

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
    SettingsContactPicker(contactPickerState, onConfirmContacts, onDismissContacts)
}

/**
 * A group's rows: the classic screen's sections and rows, reused as they are. Sections draw only their rows
 * here (see [LocalSettingsSectionInGroupCard]), so `expanded` and `onToggle` are unused placeholders.
 */
@Composable
private fun GroupBody(
    group: SettingsGroup,
    preferences: AppPreferences,
    updateTrigger: Int,
    actions: SettingsActions,
    onSelectFolder: () -> Unit,
    onSelectDriveFolder: () -> Unit,
    onOpenContactsIncoming: () -> Unit,
    onOpenContactsOutgoing: () -> Unit,
    onShareLogs: () -> Unit,
    onSaveLogs: () -> Unit,
    onShowLicenses: () -> Unit,
    onChangeLanguage: () -> Unit,
) {
    when (group) {
        SettingsGroup.RECORDING -> RecordingSection(
            preferences, updateTrigger, actions, expanded = true, onToggle = {},
            onOpenContactsIncoming = onOpenContactsIncoming,
            onOpenContactsOutgoing = onOpenContactsOutgoing,
        )
        SettingsGroup.APP_CALLS -> VoipRecordingToggle()
        SettingsGroup.RELIABILITY -> ResilienceRows()
        SettingsGroup.AUDIO -> AudioSection(preferences, updateTrigger, actions, expanded = true, onToggle = {})
        SettingsGroup.STORAGE -> StorageSection(
            preferences, updateTrigger, actions, expanded = true, onToggle = {},
            onSelectFolder = onSelectFolder,
            onSelectDriveFolder = onSelectDriveFolder,
        )
        // No summary rows: they are the Summaries group's.
        SettingsGroup.TRANSCRIPTION -> TranscriptionSection(preferences, updateTrigger, actions, expanded = true, onToggle = {})
        SettingsGroup.SUMMARIES -> {
            NavigationRow(
                icon = Icons.Filled.Translate,
                label = stringResource(R.string.transcription_language_label),
                value = sharedLanguageLabel(preferences, updateTrigger),
                supporting = stringResource(R.string.settings_language_change_in_transcripts),
                onClick = onChangeLanguage,
            )
            SettingsDivider()
            SummarySettingsRows(preferences, updateTrigger, actions)
        }
        // Without Experimental: its rows are the App calls and Reliability groups here.
        SettingsGroup.GENERAL -> GeneralSection(
            preferences, updateTrigger, actions, expanded = true, onToggle = {},
            includeExperimental = false,
        )
        SettingsGroup.DEBUG -> BugReportSection(
            preferences, updateTrigger, actions, onShareLogs, onSaveLogs, expanded = true, onToggle = {},
        )
        SettingsGroup.ABOUT -> AboutSection(
            versionString = actions.getAppVersion(),
            onShowLicenses = onShowLicenses,
            expanded = true,
            onToggle = {},
        )
        SettingsGroup.BACKUP -> Unit // drawn as a plain row by the screen, never inside a card
    }
}

private fun titleOf(scope: SettingsScope): Int = when (scope) {
    SettingsScope.RECORDINGS -> R.string.settings_title_recordings
    SettingsScope.TRANSCRIPTS -> R.string.settings_title_transcripts
    SettingsScope.SUMMARIES -> R.string.settings_title_summaries
    SettingsScope.ALL -> R.string.general_settings
}

private fun titleOf(group: SettingsGroup): Int = when (group) {
    SettingsGroup.RECORDING -> R.string.settings_section_recording
    SettingsGroup.APP_CALLS -> R.string.settings_group_app_calls
    SettingsGroup.RELIABILITY -> R.string.settings_section_reliability
    SettingsGroup.AUDIO -> R.string.settings_section_audio
    SettingsGroup.STORAGE -> R.string.settings_section_storage
    SettingsGroup.TRANSCRIPTION -> R.string.settings_section_transcription
    SettingsGroup.SUMMARIES -> R.string.home_summaries_title
    SettingsGroup.GENERAL -> R.string.settings_section_general
    SettingsGroup.DEBUG -> R.string.settings_section_debug
    SettingsGroup.ABOUT -> R.string.settings_section_about
    SettingsGroup.BACKUP -> R.string.settings_backup_title
}

private fun iconOf(group: SettingsGroup): ImageVector = when (group) {
    SettingsGroup.RECORDING -> Icons.Filled.Call
    SettingsGroup.APP_CALLS -> Icons.Filled.Groups
    SettingsGroup.RELIABILITY -> Icons.Filled.Shield
    SettingsGroup.AUDIO -> Icons.Filled.GraphicEq
    SettingsGroup.STORAGE -> Icons.Filled.Folder
    SettingsGroup.TRANSCRIPTION -> Icons.Filled.Subtitles
    SettingsGroup.SUMMARIES -> Icons.Filled.Summarize
    SettingsGroup.GENERAL -> Icons.Filled.Tune
    SettingsGroup.DEBUG -> Icons.Filled.BugReport
    SettingsGroup.ABOUT -> Icons.Filled.Info
    SettingsGroup.BACKUP -> Icons.Filled.Info
}
