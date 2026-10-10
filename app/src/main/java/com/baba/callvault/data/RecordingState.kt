/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.data

/**
 * The user's master recording switch, layered ON TOP of the per-type (carrier / VoIP) settings so that
 * resuming or starting restores those exactly as they were, never clobbers them.
 *
 * - [ACTIVE]  — normal: the recorder is armed and (in standalone) the daemon is kept warm.
 * - [PAUSED]  — a logical gate: no call is recorded, but the daemon stays warm, so resuming is instant
 *               and nothing is torn down. The persistent keep-alive notification stays.
 * - [STOPPED] — fully off: the daemon and the keep-alive service are stopped (so the persistent
 *               notification goes away) and the Shizuku user service is unbound; starting cold-starts
 *               the recorder back up.
 *
 * Both [PAUSED] and [STOPPED] **survive a reboot** — the boot path must honour the stored state and not
 * resurrect the recorder until the user resumes/starts.
 */
enum class RecordingState(val key: String) {
    ACTIVE("active"),
    PAUSED("paused"),
    STOPPED("stopped");

    /** Whether a call should be recorded right now. Only [ACTIVE] records. */
    val isRecordingAllowed: Boolean get() = this == ACTIVE

    /** Whether the recorder may be kept warm / brought up. [STOPPED] alone tears the daemon down. */
    val keepsDaemonWarm: Boolean get() = this != STOPPED

    companion object {
        fun fromKey(k: String?): RecordingState = entries.firstOrNull { it.key == k } ?: ACTIVE
    }
}
