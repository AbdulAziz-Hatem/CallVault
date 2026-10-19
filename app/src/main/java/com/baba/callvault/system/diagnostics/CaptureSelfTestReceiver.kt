/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.system.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.baba.callvault.server.RecorderConnection
import java.io.File

/**
 * SPIKE ONLY (branch spike/shizuku-voip): runs the recorder host's capture self-test on demand.
 * Guarded by android.permission.DUMP in the manifest, so only the shell can send it:
 * `adb shell am broadcast -n com.baba.callvault/.system.diagnostics.CaptureSelfTestReceiver`.
 * The result goes to logcat (tag CV:SelfTest) and files/selftest.txt.
 */
class CaptureSelfTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            val result = runCatching { RecorderConnection.service?.captureSelfTest() ?: "no recorder binder" }
                .getOrElse { "binder error: ${it.message}" }
            Log.i("CV:SelfTest", result)
            runCatching { File(app.filesDir, "selftest.txt").appendText("${System.currentTimeMillis()} $result\n") }
            pending.finish()
        }.start()
    }
}
