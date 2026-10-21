/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** The one-line summaries on the new Settings screen's group cards. */
class SettingsGroupSummariesTest {

    @Test
    fun `phone calls off reads off whatever the direction switches say`() {
        assertEquals(RecordingSummary.OFF, recordingSummary(isCarrierOn = false, isIncomingOn = true, isOutgoingOn = true))
    }

    @Test
    fun `recording summary names the directions that record automatically`() {
        assertEquals(RecordingSummary.BOTH, recordingSummary(true, isIncomingOn = true, isOutgoingOn = true))
        assertEquals(RecordingSummary.INCOMING_ONLY, recordingSummary(true, isIncomingOn = true, isOutgoingOn = false))
        assertEquals(RecordingSummary.OUTGOING_ONLY, recordingSummary(true, isIncomingOn = false, isOutgoingOn = true))
        assertEquals(RecordingSummary.MANUAL_ONLY, recordingSummary(true, isIncomingOn = false, isOutgoingOn = false))
    }

    @Test
    fun `a feature that cannot work here is unavailable, not off`() {
        assertEquals(FeatureSummary.UNAVAILABLE, featureSummary(isOn = true, isAvailable = false))
        assertEquals(FeatureSummary.UNAVAILABLE, featureSummary(isOn = false, isAvailable = false))
        assertEquals(FeatureSummary.ON, featureSummary(isOn = true, isAvailable = true))
        assertEquals(FeatureSummary.OFF, featureSummary(isOn = false, isAvailable = true))
    }

    @Test
    fun `count summary counts only switches the user can use`() {
        val switches = listOf(
            SwitchSummaryInput(isOn = true, isAvailable = true),
            SwitchSummaryInput(isOn = true, isAvailable = false), // stored on, drawn off and greyed
            SwitchSummaryInput(isOn = false, isAvailable = true),
            SwitchSummaryInput(isOn = true, isAvailable = true),
            SwitchSummaryInput(isOn = false, isAvailable = true),
        )
        assertEquals(CountSummary(on = 2, total = 4), countSummary(switches))
    }

    @Test
    fun `count summary of nothing usable has a zero total`() {
        assertEquals(CountSummary(0, 0), countSummary(listOf(SwitchSummaryInput(true, false))))
        assertEquals(CountSummary(0, 0), countSummary(emptyList()))
    }

    @Test
    fun `join skips blank parts and uses one separator`() {
        assertEquals("On · incoming only", joinSummary(listOf("On", null, "", "incoming only")))
        assertEquals("Opus", joinSummary(listOf("Opus")))
        assertEquals("", joinSummary(listOf(null, " ")))
    }
}
