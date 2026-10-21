/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import com.baba.callvault.ui.navigation.HomeSection

/**
 * Which page Settings was opened from, and therefore which groups the new Settings screen shows.
 *
 * The classic screen ignores it and always shows everything. See
 * `docs/dev-notes/2026-10-20-settings-per-page-plan.md`.
 */
enum class SettingsScope { RECORDINGS, TRANSCRIPTS, SUMMARIES, ALL }

/**
 * One card on the new Settings screen. Each one wraps rows that already exist on the classic screen;
 * no group owns a setting of its own, so both screens read and write the same preferences.
 *
 * [key] is what the screen remembers as "the open group". It is new, and does not reuse the classic
 * screen's section keys, so neither screen can disturb the other's state.
 */
enum class SettingsGroup(val key: String) {
    /** The classic Recording section. */
    RECORDING("group_recording"),

    /** App (VoIP) calls: the rows under Experimental ▸ VoIP on the classic screen. */
    APP_CALLS("group_app_calls"),

    /** The rows under Experimental ▸ Resilience on the classic screen, in the same order. */
    RELIABILITY("group_reliability"),
    AUDIO("group_audio"),
    STORAGE("group_storage"),

    /** The classic Transcription & summaries section, without the summary rows. */
    TRANSCRIPTION("group_transcription"),

    /** The summary rows, plus the shared language shown read-only (never a second picker). */
    SUMMARIES("group_summaries"),

    /** The classic General section without its Experimental sub-section. */
    GENERAL("group_general"),
    DEBUG("group_debug"),
    ABOUT("group_about"),

    /** Export / import of settings. Not a folding card: the same plain row as the classic screen. */
    BACKUP("group_backup");

    companion object {
        fun fromKey(key: String?): SettingsGroup? = entries.firstOrNull { it.key == key }
    }
}

/** The one table that says which groups each scope shows, and in what order. */
object SettingsGroups {

    private val RECORDINGS = listOf(
        SettingsGroup.RECORDING,
        SettingsGroup.APP_CALLS,
        SettingsGroup.RELIABILITY,
        SettingsGroup.AUDIO,
        SettingsGroup.STORAGE,
    )

    private val TRANSCRIPTS = listOf(SettingsGroup.TRANSCRIPTION)

    private val SUMMARIES = listOf(SettingsGroup.SUMMARIES)

    /** Everything the classic screen offers, nothing lost: each page's groups, then the app-wide ones. */
    private val ALL = RECORDINGS + TRANSCRIPTS + SUMMARIES + listOf(
        SettingsGroup.GENERAL,
        SettingsGroup.DEBUG,
        SettingsGroup.ABOUT,
        SettingsGroup.BACKUP,
    )

    fun forScope(scope: SettingsScope): List<SettingsGroup> = when (scope) {
        SettingsScope.RECORDINGS -> RECORDINGS
        SettingsScope.TRANSCRIPTS -> TRANSCRIPTS
        SettingsScope.SUMMARIES -> SUMMARIES
        SettingsScope.ALL -> ALL
    }

    /** The scope Settings opens in from each page's settings icon. The main screen shows everything. */
    fun scopeFor(section: HomeSection): SettingsScope = when (section) {
        HomeSection.Hub -> SettingsScope.ALL
        HomeSection.Recordings -> SettingsScope.RECORDINGS
        HomeSection.Transcripts -> SettingsScope.TRANSCRIPTS
        HomeSection.Summaries -> SettingsScope.SUMMARIES
    }
}
