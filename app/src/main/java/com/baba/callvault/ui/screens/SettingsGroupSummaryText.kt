/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.baba.callvault.R
import com.baba.callvault.data.AppPreferences
import com.baba.callvault.data.ModeCapability
import com.baba.callvault.data.StorageTarget
import com.baba.callvault.integrations.adb.AdbShell
import com.baba.callvault.integrations.scrcpy.ScrcpyAudioCodec
import com.baba.callvault.summary.SummaryModel
import com.baba.callvault.transcription.model.ModelRepository
import com.baba.callvault.ui.common.TranscriptionLabels
import com.baba.callvault.ui.viewmodels.SettingsActions

/**
 * The one-line summary under a group card's title, read from the same preferences as the group's rows.
 *
 * Re-read when [updateTrigger] moves and whenever the card opens or closes: several rows (app calls,
 * reliability) save straight to preferences without bumping the trigger, and closing the card is the
 * moment the user looks back at its summary.
 */
@Composable
internal fun groupSummaryText(
    group: SettingsGroup,
    preferences: AppPreferences,
    updateTrigger: Int,
    expanded: Boolean,
    actions: SettingsActions,
): String = when (group) {
    SettingsGroup.RECORDING -> recordingSummaryText(preferences, updateTrigger, expanded)
    SettingsGroup.APP_CALLS -> {
        val supported = capabilityAvailable(ModeCapability.VOIP_RECORDING)
        val isOn = remember(updateTrigger, expanded) { preferences.isVoipRecordingEnabled() }
        featureSummaryText(featureSummary(isOn, supported))
    }
    SettingsGroup.RELIABILITY -> reliabilitySummaryText(preferences, updateTrigger, expanded)
    SettingsGroup.AUDIO -> {
        val codecKey = remember(updateTrigger) { preferences.getAudioCodec() }
        val bitRate = remember(updateTrigger) { preferences.getAudioBitRate() }
        val codec = ScrcpyAudioCodec.entries.firstOrNull { it.cliKey == codecKey }
        joinSummary(
            listOf(
                codec?.let { stringResource(it.titleResId) },
                stringResource(R.string.audio_bitrate_kbps, bitRate / BITS_PER_KILOBIT),
            )
        )
    }
    SettingsGroup.STORAGE -> {
        val target = remember(updateTrigger) { preferences.getStorageTarget() }
        stringResource(
            when (target) {
                StorageTarget.LOCAL -> R.string.storage_target_local
                StorageTarget.DRIVE -> R.string.storage_target_drive
                StorageTarget.BOTH -> R.string.storage_target_both
            }
        )
    }
    SettingsGroup.TRANSCRIPTION -> {
        val mode = remember(updateTrigger) { preferences.getTranscriptionMode() }
        joinSummary(
            listOf(
                stringResource(TranscriptionLabels.titleOf(mode)),
                sharedLanguageLabel(preferences, updateTrigger),
            )
        )
    }
    SettingsGroup.SUMMARIES -> {
        val context = LocalContext.current
        val installed = remember(updateTrigger, expanded) {
            ModelRepository.isInstalled(context, SummaryModel.DEFAULT)
        }
        joinSummary(
            listOf(
                stringResource(if (installed) R.string.summary_model_installed else R.string.settings_summary_not_downloaded),
                sharedLanguageLabel(preferences, updateTrigger),
            )
        )
    }
    SettingsGroup.GENERAL -> {
        val theme = remember(updateTrigger) { preferences.getThemeMode() }
        stringResource(
            when (theme) {
                AppPreferences.ThemeMode.SYSTEM -> R.string.settings_theme_mode_system
                AppPreferences.ThemeMode.LIGHT -> R.string.settings_theme_mode_light
                AppPreferences.ThemeMode.DARK -> R.string.settings_theme_mode_dark
            }
        )
    }
    SettingsGroup.DEBUG -> {
        val logging = remember(updateTrigger) { preferences.isLoggingEnabled() }
        featureSummaryText(featureSummary(logging, isAvailable = true))
    }
    SettingsGroup.ABOUT -> actions.getAppVersion()
    SettingsGroup.BACKUP -> ""
}

/** kbps from a stored bit rate in bits per second. */
private const val BITS_PER_KILOBIT = 1000

/** The one language transcripts and summaries share, as the user sees it in the picker. */
@Composable
internal fun sharedLanguageLabel(preferences: AppPreferences, updateTrigger: Int): String {
    val language = remember(updateTrigger) { preferences.getTranscriptionLanguage() }
    return stringResource(TranscriptionLabels.languageOf(language))
}

@Composable
private fun featureSummaryText(summary: FeatureSummary): String = stringResource(
    when (summary) {
        FeatureSummary.ON -> R.string.settings_summary_on
        FeatureSummary.OFF -> R.string.settings_summary_off
        FeatureSummary.UNAVAILABLE -> R.string.settings_summary_unavailable
    }
)

@Composable
private fun recordingSummaryText(preferences: AppPreferences, updateTrigger: Int, expanded: Boolean): String {
    val summary = remember(updateTrigger, expanded) {
        recordingSummary(
            isCarrierOn = preferences.isCarrierRecordingEnabled(),
            isIncomingOn = preferences.isAutoRecordIncomingEnabled(),
            isOutgoingOn = preferences.isAutoRecordOutgoingEnabled(),
        )
    }
    val detail = when (summary) {
        RecordingSummary.OFF -> return stringResource(R.string.settings_summary_off)
        RecordingSummary.BOTH -> R.string.settings_summary_recording_both
        RecordingSummary.INCOMING_ONLY -> R.string.settings_summary_recording_incoming
        RecordingSummary.OUTGOING_ONLY -> R.string.settings_summary_recording_outgoing
        RecordingSummary.MANUAL_ONLY -> R.string.settings_summary_recording_manual
    }
    return joinSummary(listOf(stringResource(R.string.settings_summary_on), stringResource(detail)))
}

/**
 * "N of M on" over the Reliability switches, counting only the ones usable in this mode, as their rows
 * grey out the rest. The mode is read live (never remembered) so the line follows a mode switch.
 */
@Composable
private fun reliabilitySummaryText(preferences: AppPreferences, updateTrigger: Int, expanded: Boolean): String {
    val context = LocalContext.current
    val usesEmbeddedAdb = !currentPrivilegedMode().needsShizuku
    val handoffSupported = capabilityAvailable(ModeCapability.RESILIENT_RECORDING)
    val offlineSupported = capabilityAvailable(ModeCapability.OFFLINE_RECORDING)
    val wdControlSupported = capabilityAvailable(ModeCapability.WIRELESS_DEBUGGING_CONTROL)
    val count = remember(updateTrigger, expanded, usesEmbeddedAdb, handoffSupported, offlineSupported, wdControlSupported) {
        countSummary(
            listOf(
                SwitchSummaryInput(preferences.isHandoffPersistEnabled(), handoffSupported),
                SwitchSummaryInput(preferences.isOfflineRecordingEnabled(), offlineSupported),
                SwitchSummaryInput(AdbShell.isUsbDebuggingEnabled(context), usesEmbeddedAdb),
                SwitchSummaryInput(preferences.isWirelessDebuggingEnforced(), usesEmbeddedAdb),
                SwitchSummaryInput(preferences.isWifiNetworkAutoApproveEnabled(), wdControlSupported),
            )
        )
    }
    if (count.total == 0) return stringResource(R.string.settings_summary_unavailable)
    return stringResource(R.string.settings_summary_count_on, count.on, count.total)
}
