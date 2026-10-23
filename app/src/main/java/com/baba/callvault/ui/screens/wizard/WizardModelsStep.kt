/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baba.callvault.R
import com.baba.callvault.data.AppPreferences
import com.baba.callvault.transcription.TranscriptionScheduler
import com.baba.callvault.transcription.model.DownloadableModel
import com.baba.callvault.transcription.model.TranscriptionModel
import com.baba.callvault.ui.common.TranscriptionLabels
import com.baba.callvault.ui.common.CvCard

/**
 * Transcription and summaries: when they run, in which language, and — now — their models.
 *
 * Here at all because a fresh install has no other route to either feature — the What's New dialog
 * needs an *update* to fire, and the wizard cannot be re-run, so a step omitted here leaves the
 * flagship of this release buried in a Settings accordion nobody was told to open.
 *
 * Every control is Settings' own composable, not a wizard copy of it: the mode dropdown carries the
 * "this is slow and heavy" confirmation, the language dropdown carries the auto-detect warning, and
 * the summariser's rows carry the requirements dialog that guards 3.46 GB. A copy that lost any of
 * those would be a copy that lies — the same reason [ExperimentalStep] shares its toggle and
 * [ReliabilityStep] shares the offline switch, both after a hand-rolled version had already dropped
 * the half that mattered.
 *
 * **The downloads start here and nothing waits for them.** The step used to say the button was in
 * Settings, which asked someone who has just been told about the feature to go and find it later.
 * They are WorkManager jobs on an unmetered network, so Next and Finish stay enabled throughout
 * ([canAdvanceFrom] never asks about this step) and the download carries on while the user finishes
 * setup and lands on Home.
 *
 * @param updateTrigger Bumped by the view model after each write, which re-reads the filesystem
 *   under the model rows — deleting a model touches no WorkManager state, so nothing else would.
 */
@Composable
internal fun ModelsStep(
    updateTrigger: Int,
    onDownloadModel: (DownloadableModel) -> Unit,
    onCancelModelDownload: (DownloadableModel) -> Unit,
    onDeleteModel: (DownloadableModel) -> Unit,
    onImportModel: (DownloadableModel, Uri) -> Unit,
    onCancelModelImport: (DownloadableModel) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var mode by remember { mutableStateOf(prefs.getTranscriptionMode()) }
    var language by remember { mutableStateOf(prefs.getTranscriptionLanguage()) }
    var modelId by remember { mutableStateOf(prefs.getTranscriptionModelId()) }
    val selectedModel = TranscriptionModel.fromId(modelId) ?: TranscriptionModel.DEFAULT

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CvCard(contentPadding = PaddingValues(vertical = 8.dp)) {
            TranscriptionModeField(
                mode = mode,
                onModeChange = { chosen ->
                    mode = chosen
                    prefs.setTranscriptionMode(chosen)
                    // Reconcile the sweep immediately, exactly as Settings does — persisting the mode
                    // alone would leave WorkManager disagreeing with it until something else ran.
                    TranscriptionScheduler.apply(context)
                }
            )
            TranscriptionLanguageField(
                language = language,
                onLanguageChange = { chosen ->
                    language = chosen
                    prefs.setTranscriptionLanguage(chosen)
                }
            )

            // Offered here because the download row below fetches whatever this says. Told to pick a
            // model without being told which one is coming, the difference between 190 MB and 874 MB
            // would arrive as a surprise on a metered month.
            TranscriptionModelField(
                modelId = modelId,
                onModelChange = { chosen ->
                    modelId = chosen
                    prefs.setTranscriptionModelId(chosen)
                }
            )

            TranscriptionModelRows(
                model = selectedModel,
                updateTrigger = updateTrigger,
                onDownload = onDownloadModel,
                onCancel = onCancelModelDownload,
                onDelete = onDeleteModel,
                // No backup in setup: a fresh install has nothing to save yet.
                onImport = onImportModel,
                onCancelImport = onCancelModelImport
            )
        }

        // Said even though the field already shows the language: the value arrived without being asked
        // for, and a pre-filled answer nobody explains is one people scroll past rather than check.
        // Only when there IS a pre-filled answer — a phone set to a language the app does not offer
        // lands on auto-detect, and the field's own warning is the right thing to read there.
        if (language != null) {
            NoteCard(stringResource(R.string.wizard_transcription_language_note))
        }

        // Built from the catalogue, not written out. This note used to name SMALL_Q5_1 and
        // LARGE_V3_TURBO_Q5_0 by hand into a four-argument format string, which meant adding a third
        // tier compiled cleanly and left the wizard quietly describing two of three models forever —
        // the one place in the transcription UI with no `when` to fail on. The dropdown right above
        // it already offered the model this note did not mention.
        //
        // `map` rather than `joinToString`, because only the inline one lets `stringResource` be
        // called per entry; the join then runs over plain strings.
        val modelSizes = TranscriptionModel.entries.map { model ->
            stringResource(
                R.string.wizard_transcription_model_size,
                stringResource(TranscriptionLabels.titleOf(model)),
                model.sizeBytes / BYTES_PER_MB
            )
        }
        // A new key rather than the old one narrowed to one argument: the ten values-* files still
        // hold a four-placeholder translation of `wizard_transcription_model_note`, and feeding a
        // one-argument call to a locale's %2$d throws rather than degrading.
        NoteCard(
            stringResource(
                R.string.wizard_transcription_model_sizes_note,
                modelSizes.joinToString(stringResource(R.string.list_separator))
            )
        )

        // A card of its own rather than the single sentence that used to be here. Summaries are half
        // of what this step is for, and a footnote pointing at Settings is how a feature ends up
        // never being found. The rows are the summariser's own, so the requirements dialog — 3.46 GB
        // to download, about 3.5 GB of memory to run — still stands between the tap and the fetch.
        CvCard(contentPadding = PaddingValues(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.wizard_transcription_summary_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.wizard_transcription_summary_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SummaryRows(
                updateTrigger = updateTrigger,
                onDownload = onDownloadModel,
                onCancel = onCancelModelDownload,
                onDelete = onDeleteModel,
                // No backup in setup: a fresh install has nothing to save yet.
                onImport = onImportModel,
                onCancelImport = onCancelModelImport
            )
        }

        // Last, and about both downloads: whatever was started here does not have to finish here.
        NoteCard(stringResource(R.string.wizard_transcription_background_note))
    }
}
