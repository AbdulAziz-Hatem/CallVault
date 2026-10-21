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

    /** The new screen's source: the screen and its group bodies. */
    private val newScreen get() = source("ScopedSettingsContent.kt") + source("ScopedSettingsGroupBodies.kt")

    /** The text of one composable's body, from its declaration to the next declaration. */
    private fun bodyOf(name: String, text: String): String {
        val start = Regex("""fun $name\(""").find(text)?.range?.first
            ?: throw AssertionError("no composable $name")
        val next = Regex("""\n(@Composable|/\*\*|internal |private )""").find(text, start + 1)?.range?.first
        return text.substring(start, next ?: text.length)
    }

    private fun calls(name: String, text: String) = Regex("""\b$name\(""").containsMatchIn(text)

    private fun bodyName(group: SettingsGroup) =
        group.name.lowercase().split('_').joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Body"

    @Test
    fun `the new screen has no Experimental and no classic General section, while the classic one keeps it`() {
        assertFalse(newScreen.contains("ExperimentalSubSection"))
        assertFalse(calls("GeneralSection", newScreen))
        assertTrue(source("SettingsGeneralSection.kt").contains("{ ExperimentalSubSection() }"))
    }

    @Test
    fun `the new screen offers no second language picker`() {
        // Transcripts and summaries share one language on purpose; two pickers once produced transcripts
        // in the wrong language. The new screen shows it read-only under Summaries and links to it.
        assertFalse(newScreen.contains("TranscriptionLanguageField"))
        assertEquals(1, Regex("""TranscriptionLanguageRows\(""").findAll(newScreen).count())
        val bodies = source("ScopedSettingsGroupBodies.kt")
        assertTrue(calls("TranscriptionLanguageRows", bodyOf("ModelAndLanguageBody", bodies)))
        assertFalse(calls("TranscriptionLanguageRows", bodyOf("SummariesBody", bodies)))
    }

    @Test
    fun `the switch back to the classic screen lives in Debug, which both screens show`() {
        val debug = source("SettingsAboutDebugSection.kt")
        assertTrue(debug.contains("ClassicSettingsScreenToggle(preferences, updateTrigger, actions)"))
        assertTrue("BugReportSection" in SettingsGroups.rowsOf(SettingsGroup.HELP_AND_ABOUT))
        assertEquals(1, Regex("""BugReportSection\(""").findAll(newScreen).count())
    }

    @Test
    fun `each group body calls exactly the rows its table lists`() {
        val bodies = source("ScopedSettingsGroupBodies.kt")
        val everyRow = SettingsGroup.entries.flatMap { SettingsGroups.rowsOf(it) }.toSet()
        SettingsGroup.entries.forEach { group ->
            val body = bodyOf(bodyName(group), bodies)
            val listed = SettingsGroups.rowsOf(group).toSet()
            listed.forEach { assertTrue("${bodyName(group)} does not call $it", calls(it, body)) }
            val unlisted = (everyRow - listed).filter { calls(it, body) }
            assertTrue("${bodyName(group)} calls rows listed elsewhere: $unlisted", unlisted.isEmpty())
        }
    }

    @Test
    fun `connection holds the same switches as the classic resilience rows`() {
        val general = source("SettingsGeneralSection.kt")
        val rows = listOf(
            "UsbDebuggingToggle", "OfflineRecordingToggle", "HandoffPersistToggle", "WirelessDebuggingEnforceToggle",
            "WifiNetworkAutoApproveToggle", "UsbDefaultConfigRow", "PairAgainRow",
        )
        val connection = bodyOf("ConnectionRows", general)
        val resilience = bodyOf("ResilienceRows", general)
        rows.forEach {
            assertTrue("ConnectionRows lacks $it", calls(it, connection))
            assertTrue("ResilienceRows lacks $it", calls(it, resilience))
        }
        // The agreed order (round 2): USB debugging, offline, handoff, keep WD on, approve networks, USB mode, pair.
        assertEquals(rows, rows.sortedBy { connection.indexOf("$it(") })
    }

    @Test
    fun `every settings row the classic screen draws has a home in all settings`() {
        // Every row-like composable declared for Settings. A new one fails here until it is placed in a group
        // (SettingsGroups.rowsOf) or listed below as a container or a part of a listed row.
        val files = screensDir.listFiles { f -> f.name.startsWith("Settings") && f.extension == "kt" }!!.toList() +
            File(screensDir, "SummarySection.kt")
        val declared = files.flatMap { f ->
            Regex("""@Composable\s*\n(?:internal |private )?fun (\w+(?:Row|Rows|Toggle|Switch|Picker|Note|Section|SubSection))\(""")
                .findAll(f.readText()).map { it.groupValues[1] }.toList()
        }.toSet()
        val notRows = setOf(
            // Building blocks and classic containers whose rows are listed one by one.
            "SettingsToggleRow", "NavigationRow", "DropdownRow", "SettingsSection", "SettingsSubSection",
            "RecordingSection", "StorageSection", "TranscriptionSection", "GeneralSection",
            "ExperimentalSubSection", "VisualSubSection", "RetentionSubSection",
            // Composites: the classic screen's bundles of rows that the new screen places one by one.
            "ResilienceRows", "VoipRecordingToggle",
            // Parts of a listed row: drawn inside BugReportSection, SummarySettingsRows, the download rows.
            "ClassicSettingsScreenToggle", "SummaryRows", "TranscriptionModelRows",
            // A dialog over either screen, not a row.
            "SettingsContactPicker",
        )
        val connection = listOf(
            "UsbDebuggingToggle", "OfflineRecordingToggle", "HandoffPersistToggle", "WirelessDebuggingEnforceToggle",
            "WifiNetworkAutoApproveToggle", "UsbDefaultConfigRow", "PairAgainRow",
        )
        val placed = SettingsGroups.forScope(SettingsScope.ALL).flatMap { SettingsGroups.rowsOf(it) }.toSet() + connection
        val missing = declared - notRows - placed
        assertTrue("Settings rows with no group on the new screen: $missing", missing.isEmpty())
        // The classic VoIP bundle is exactly the four rows the new screen places.
        val voip = bodyOf("VoipRecordingToggle", source("SettingsVoipRows.kt"))
        listOf("VoipRecordingSwitch", "VoipAutoStartRow", "VoipAppPicker", "VoipIgnoreContactsRow").forEach {
            assertTrue("VoipRecordingToggle lacks $it", calls(it, voip))
            assertTrue("$it is not placed", it in placed)
        }
    }
}
