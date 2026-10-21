/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.test.core.app.ApplicationProvider
import com.baba.callvault.data.AppPreferences
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The new Settings screen beside the classic one (settings per page, step 2): the rollback switch, and the
 * rules the screen's source must keep. Read from source like [com.baba.callvault.ui.ModeAwareUiTest],
 * because the rules are about which composables are called, not about pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScopedSettingsScreenTest {

    private val screensDir = listOf(
        "src/main/java/com/baba/callvault/ui/screens",
        "app/src/main/java/com/baba/callvault/ui/screens",
    ).map(::File).first { it.isDirectory }

    private fun source(name: String) = File(screensDir, name).readText()

    @Test
    fun `the classic screen is the default until the new one is verified`() {
        val prefs = AppPreferences(ApplicationProvider.getApplicationContext())
        assertTrue(prefs.isClassicSettingsScreen())
    }

    @Test
    fun `the classic screen switch is stored and read back`() {
        val prefs = AppPreferences(ApplicationProvider.getApplicationContext())
        prefs.setClassicSettingsScreen(false)
        assertFalse(prefs.isClassicSettingsScreen())
        prefs.setClassicSettingsScreen(true)
        assertTrue(prefs.isClassicSettingsScreen())
    }

    @Test
    fun `the classic screen switch is device-only`() {
        assertFalse(AppPreferences.EXPORTABLE_KEYS.contains("classic_settings_screen"))
    }

    @Test
    fun `the new screen's General has no Experimental, while the classic one keeps it`() {
        val scoped = source("ScopedSettingsContent.kt")
        assertTrue(scoped.contains("includeExperimental = false"))
        assertFalse(scoped.contains("ExperimentalSubSection"))
        assertFalse(source("SettingsScreen.kt").contains("includeExperimental"))
    }

    @Test
    fun `the new screen offers no second language picker`() {
        // Transcripts and summaries share one language on purpose; two pickers once produced transcripts
        // in the wrong language. The new screen shows it read-only under Summaries and links to Transcripts.
        assertFalse(source("ScopedSettingsContent.kt").contains("TranscriptionLanguageField"))
    }

    @Test
    fun `the switch back to the classic screen lives in Debug, which both screens show`() {
        val debug = source("SettingsAboutDebugSection.kt")
        assertTrue(debug.contains("ClassicSettingsScreenToggle(preferences, updateTrigger, actions)"))
        assertTrue(SettingsGroup.DEBUG in SettingsGroups.forScope(SettingsScope.ALL))
        assertEquals(1, Regex("""BugReportSection\(""").findAll(source("ScopedSettingsContent.kt")).count())
    }
}
