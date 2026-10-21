/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import com.baba.callvault.ui.navigation.HomeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which groups each page's Settings shows. One table, so the pages cannot drift apart, and so a setting
 * cannot fall off the "everything" view when it moves between pages.
 */
class SettingsGroupsTest {

    @Test
    fun `recordings shows recording, app calls, reliability, audio and storage in that order`() {
        assertEquals(
            listOf(
                SettingsGroup.RECORDING,
                SettingsGroup.APP_CALLS,
                SettingsGroup.RELIABILITY,
                SettingsGroup.AUDIO,
                SettingsGroup.STORAGE,
            ),
            SettingsGroups.forScope(SettingsScope.RECORDINGS),
        )
    }

    @Test
    fun `transcripts shows transcription only`() {
        assertEquals(listOf(SettingsGroup.TRANSCRIPTION), SettingsGroups.forScope(SettingsScope.TRANSCRIPTS))
    }

    @Test
    fun `summaries shows summaries only, so no second language picker can appear there`() {
        assertEquals(listOf(SettingsGroup.SUMMARIES), SettingsGroups.forScope(SettingsScope.SUMMARIES))
        assertFalse(SettingsGroup.TRANSCRIPTION in SettingsGroups.forScope(SettingsScope.SUMMARIES))
    }

    @Test
    fun `all shows every group, ending with the app-wide ones`() {
        assertEquals(
            listOf(
                SettingsGroup.RECORDING,
                SettingsGroup.APP_CALLS,
                SettingsGroup.RELIABILITY,
                SettingsGroup.AUDIO,
                SettingsGroup.STORAGE,
                SettingsGroup.TRANSCRIPTION,
                SettingsGroup.SUMMARIES,
                SettingsGroup.GENERAL,
                SettingsGroup.DEBUG,
                SettingsGroup.ABOUT,
                SettingsGroup.BACKUP,
            ),
            SettingsGroups.forScope(SettingsScope.ALL),
        )
        assertEquals(SettingsGroup.entries.toSet(), SettingsGroups.forScope(SettingsScope.ALL).toSet())
    }

    @Test
    fun `all is a superset of every other scope`() {
        val all = SettingsGroups.forScope(SettingsScope.ALL).toSet()
        SettingsScope.entries.forEach { scope ->
            val missing = SettingsGroups.forScope(scope).toSet() - all
            assertTrue("$scope has groups that All settings lacks: $missing", missing.isEmpty())
        }
    }

    @Test
    fun `no group appears twice within a scope`() {
        SettingsScope.entries.forEach { scope ->
            val groups = SettingsGroups.forScope(scope)
            assertEquals("duplicate in $scope: $groups", groups.size, groups.toSet().size)
        }
    }

    @Test
    fun `reliability and app calls exist as their own groups`() {
        val recordings = SettingsGroups.forScope(SettingsScope.RECORDINGS)
        assertTrue(SettingsGroup.RELIABILITY in recordings)
        assertTrue(SettingsGroup.APP_CALLS in recordings)
    }

    @Test
    fun `the debug report and the attribution stay reachable from all settings`() {
        // GPLv3 section 7 attribution lives in About; the debug report is what support asks for.
        val all = SettingsGroups.forScope(SettingsScope.ALL)
        assertTrue(SettingsGroup.ABOUT in all)
        assertTrue(SettingsGroup.DEBUG in all)
    }

    @Test
    fun `group keys are unique and do not reuse the classic screen's section keys`() {
        val keys = SettingsGroup.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        val classic = setOf(
            "recording", "storage", "retention", "audio", "transcription", "general",
            "visual", "reliability", "bug_report", "updates", "about",
        )
        assertTrue(keys.none { it in classic })
        SettingsGroup.entries.forEach { assertEquals(it, SettingsGroup.fromKey(it.key)) }
        assertNull(SettingsGroup.fromKey("reliability"))
        assertNull(SettingsGroup.fromKey(null))
    }

    @Test
    fun `each page's settings icon opens its own scope, and the main screen opens everything`() {
        assertEquals(SettingsScope.ALL, SettingsGroups.scopeFor(HomeSection.Hub))
        assertEquals(SettingsScope.RECORDINGS, SettingsGroups.scopeFor(HomeSection.Recordings))
        assertEquals(SettingsScope.TRANSCRIPTS, SettingsGroups.scopeFor(HomeSection.Transcripts))
        assertEquals(SettingsScope.SUMMARIES, SettingsGroups.scopeFor(HomeSection.Summaries))
    }
}
