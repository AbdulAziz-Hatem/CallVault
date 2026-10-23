/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.baba.callvault.R
import com.baba.callvault.transcription.model.DownloadableModel
import com.baba.callvault.transcription.model.ModelTransferWorker
import com.baba.callvault.ui.common.ModelBackupState
import com.baba.callvault.ui.common.rememberModelBackupState
import com.baba.callvault.utils.AppLogger

/**
 * The Import and Back up rows for a model (GitHub #58), shared by the speech model's and the
 * summariser's rows on both Settings screens and in the setup wizard.
 *
 * **Their explanations are always behind an ⓘ, on every screen.** Both rows sit under a download row
 * that already says what it costs; a paragraph under each would double the height of the model's block
 * for something most people never use. So the rows turn the ⓘ mode on for themselves rather than
 * following the screen's: the classic screen and the wizard show the same ⓘ the new screen does.
 */

private const val TAG = "CV:ModelTransferRows"

/** Any file: providers label a .gguf / .bin inconsistently, and the worker checks it anyway. */
private val ANY_FILE = arrayOf("*/*")

/**
 * The string that says why an import failed, for one of `ModelTransferWorker`'s `ERROR_` values.
 * [R.string.model_import_failed_wrong_file] takes the model's name as its one argument; the others
 * take none. Anything unknown reads as "couldn't read that file", whose remedy — pick it again — is
 * the right one for any failure the worker has not named.
 */
@StringRes
internal fun importFailureRes(reason: String?): Int = when (reason) {
    ModelTransferWorker.ERROR_WRONG_FILE -> R.string.model_import_failed_wrong_file
    ModelTransferWorker.ERROR_OTHER_MODEL -> R.string.model_import_failed_other_model
    ModelTransferWorker.ERROR_UNREADABLE -> R.string.model_import_failed_unreadable
    ModelTransferWorker.ERROR_NO_SPACE -> R.string.model_import_failed_space
    else -> R.string.model_import_failed_unreadable
}

/** [importFailureRes] resolved; [modelName] fills the wrong-file message. */
@Composable
internal fun importFailureText(reason: String?, modelName: String): String {
    val res = importFailureRes(reason)
    return if (res == R.string.model_import_failed_wrong_file) stringResource(res, modelName) else stringResource(res)
}

/**
 * "Import from a file": opens the system file picker and hands back what was picked.
 *
 * @param failureText Replaces the subtitle after a failed import, so the row that failed is the row
 *   that says why — and tapping it again is the remedy for every failure it can report.
 * @param beforePick Called with the picker's launch instead of launching it directly. The summariser
 *   uses it to show its requirements dialog first: an imported 3.46 GB model costs the same memory
 *   to run as a downloaded one.
 */
@Composable
internal fun ModelImportRow(
    model: DownloadableModel,
    onPicked: (Uri) -> Unit,
    beforePick: ((launch: () -> Unit) -> Unit)? = null,
    failureText: String? = null
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(uri)
    }
    val launch = { launchSafely("import") { picker.launch(ANY_FILE) } }
    WithRowInfo {
        NavigationRow(
            icon = Icons.Filled.FileOpen,
            label = stringResource(R.string.model_import),
            value = failureText ?: stringResource(R.string.model_import_subtitle),
            onClick = { beforePick?.invoke(launch) ?: launch() },
            infoExtra = stringResource(R.string.model_import_info, model.fileName, model.sizeBytes / BYTES_PER_MB)
        )
    }
}

/** An import in flight, with its cancel. Replaces both the download and the import rows. */
@Composable
internal fun ModelImportingRow(percent: Int?, onCancel: () -> Unit) {
    ModelDownloadingRow(
        label = percent?.let { stringResource(R.string.model_importing, it) }
            ?: stringResource(R.string.model_import_waiting),
        percent = percent,
        onCancel = onCancel,
        cancelLabel = stringResource(R.string.general_cancel)
    )
}

/**
 * "Back up": saves the installed model to a file the user creates, named as it is on disk so an
 * import later finds the name the info text tells them to look for. Shown only under an installed
 * model, and only in Settings: the wizard runs on a fresh install, with nothing yet to back up.
 */
@Composable
internal fun ModelBackupRow(model: DownloadableModel, onPicked: (Uri) -> Unit, onCancel: () -> Unit) {
    val state by rememberModelBackupState(model)
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) onPicked(uri)
    }
    val launch = { launchSafely("backup") { picker.launch(model.fileName) } }

    when (val current = state) {
        is ModelBackupState.Running -> ModelDownloadingRow(
            // No figure yet reads as 0% over an indeterminate bar: a backup has no network to wait for.
            label = stringResource(R.string.model_backing_up, current.percent ?: 0),
            percent = current.percent,
            onCancel = onCancel,
            cancelLabel = stringResource(R.string.general_cancel)
        )

        else -> WithRowInfo {
            NavigationRow(
                icon = Icons.Filled.Save,
                label = stringResource(R.string.model_backup),
                value = stringResource(
                    when (current) {
                        ModelBackupState.Done -> R.string.model_backup_done
                        is ModelBackupState.Failed -> R.string.model_backup_failed
                        else -> R.string.model_backup_subtitle
                    }
                ),
                onClick = launch,
                infoExtra = stringResource(R.string.model_backup_info)
            )
        }
    }
}

/** Turns the ⓘ mode on for one row, whichever screen it is on (see the file's KDoc). */
@Composable
private fun WithRowInfo(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSettingsDescriptionsAsInfo provides true) { content() }
}

/** A phone with no documents UI throws here; saying nothing beats crashing Settings. */
private inline fun launchSafely(what: String, launch: () -> Unit) {
    try {
        launch()
    } catch (e: ActivityNotFoundException) {
        AppLogger.e(TAG, "No document picker for the model $what", e)
    }
}
