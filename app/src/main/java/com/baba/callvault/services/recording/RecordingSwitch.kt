/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.services.recording

import android.content.Context
import com.baba.callvault.data.AppPreferences
import com.baba.callvault.data.RecordingState
import com.baba.callvault.server.BackendChoice
import com.baba.callvault.server.RecorderBackend
import com.baba.callvault.server.RecorderConnection
import com.baba.callvault.utils.AppLogger

/**
 * Applies the user's master recording switch ([RecordingState]) and carries out the side effects of a
 * transition. The pref itself is what the arming gates read; this is only the lifecycle work around it:
 *
 * - **→ STOPPED:** stop the keep-alive (so its watchdog cannot relaunch what we tear down and its
 *   persistent notification goes away), then tear the daemon down — but never cut a call in progress
 *   (finish-current, apply-to-next). A reboot will not resurrect it, because [DaemonKeepAliveService.start]
 *   itself honours STOPPED.
 * - **leaving STOPPED:** bring the recorder back up (standalone via the keep-alive, Shizuku by binding).
 * - **ACTIVE ⇄ PAUSED:** nothing — the daemon stays warm so resuming is instant; the arming gate in
 *   [com.baba.callvault.services.call.CallSessionManager] / [VoipRecordingCoordinator] does the rest.
 *
 * Teardown and bring-up make blocking calls, so **call this off the main thread.**
 */
object RecordingSwitch {
    private const val TAG = "CV:RecordingSwitch"

    fun apply(context: Context, to: RecordingState) {
        val prefs = AppPreferences(context)
        val from = prefs.getRecordingState()
        if (from == to) return
        prefs.setRecordingState(to)
        AppLogger.i(TAG, "Master recording switch: $from -> $to")
        when {
            to == RecordingState.STOPPED -> tearDown(context)
            from == RecordingState.STOPPED -> bringUp(context)
            // ACTIVE ⇄ PAUSED: the daemon stays warm; nothing to tear down or bring up.
        }
    }

    private fun tearDown(context: Context) {
        // Stop the watchdog FIRST, synchronously, so a binder death from the teardown below cannot be
        // caught and answered with an immediate relaunch (the same ordering the mode switch relies on).
        runCatching { DaemonKeepAliveService.stop(context) }
            .onFailure { AppLogger.w(TAG, "Could not stop the keep-alive: ${it.message}") }
        // Finish-current, apply-to-next: never cut a call being recorded right now. A daemon left warm this
        // way is harmless — the arming gate blocks the next call, the keep-alive notification is already
        // gone, and a reboot will not bring it back (STOPPED is persisted and start() honours it).
        if (runCatching { RecorderConnection.service?.isRecording == true }.getOrDefault(false)) {
            AppLogger.i(TAG, "Stop requested during a recording — leaving the daemon to finish this call")
            return
        }
        when (BackendChoice.of(AppPreferences(context).getPrivilegedMode())) {
            BackendChoice.SHIZUKU -> RecorderBackend.retireShizukuService("the user stopped recording")
            BackendChoice.ADB ->
                runCatching { RecorderConnection.service?.destroy() }
                    .onFailure { AppLogger.w(TAG, "Could not stop the daemon: ${it.message}") }
        }
    }

    private fun bringUp(context: Context) {
        // Standalone: launches the daemon and its watchdog. Shizuku: start() self-skips, so bind directly.
        DaemonKeepAliveService.start(context)
        if (AppPreferences(context).getPrivilegedMode().needsShizuku) {
            Thread({
                runCatching { RecorderBackend.ensureRunning(context) }
                    .onFailure { AppLogger.w(TAG, "Could not bring up the Shizuku recorder: ${it.message}") }
            }, "cv-recorder-start").apply { isDaemon = true }.start()
        }
    }
}
