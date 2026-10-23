/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baba.callvault.R
import com.baba.callvault.integrations.adb.UsbDefaultConfig
import com.baba.callvault.integrations.adb.UsbDefaultMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.baba.callvault.ui.common.CvCard
import com.baba.callvault.ui.common.CvPrimaryButton

/**
 * Onboarding "Reliability" step — two OPTIONAL opt-ins that make recording robust:
 *  1. **USB "Charging only"** — on many phones, locking the screen mid-call restarts adbd and kills the
 *     recorder; setting the Default USB Configuration to "Charging only" prevents that. Applied over the
 *     embedded shell in one tap.
 *  2. **Offline recording (no Wi-Fi)** — the warned loopback opt-in, rendered by Settings' own toggle.
 * Both are skippable (Next always advances) — nothing here gates setup.
 *
 * Both are also embedded-ADB machinery, which is why [wizardSteps] drops this step whole in Shizuku
 * mode rather than showing two controls that cannot act.
 */
@Composable
internal fun ReliabilityStep() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var usbMode by remember { mutableStateOf(UsbDefaultConfig.cached(context)) }
    var usbApplying by remember { mutableStateOf(false) }
    // Read the live value once (connects+retries internally); falls back to the cached value.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { UsbDefaultConfig.readViaShell(context) }?.let { usbMode = it }
    }
    val chargingOnly = usbMode == UsbDefaultMode.CHARGING

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── USB "Charging only" ──
        CvCard {
            Text(
                text = stringResource(R.string.wizard_reliability_usb_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.wizard_reliability_usb_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            when {
                chargingOnly -> WizardDoneRow(stringResource(R.string.wizard_reliability_usb_done))
                usbApplying -> WizardApplyingRow(stringResource(R.string.settings_usb_default_applying))
                else -> CvPrimaryButton(
                    text = stringResource(R.string.wizard_reliability_usb_button),
                    onClick = {
                        usbApplying = true
                        scope.launch {
                            withContext(Dispatchers.IO) { UsbDefaultConfig.setViaShell(context, UsbDefaultMode.CHARGING) }
                            usbMode = UsbDefaultConfig.cached(context)
                            usbApplying = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Offline recording (no Wi-Fi) ──
        // The switch Settings renders, not a copy of it. The hand-rolled card that used to sit here
        // skipped the ModeCapability gate and offered "Enable" in Shizuku mode, where the opt-in has no
        // embedded ADB to arm and the next mode switch turns it straight back off. Sharing the
        // composable is what stops that drift from happening again — as the experimental step already does.
        CvCard { OfflineRecordingToggle() }
    }
}

/** A small "done" row: a teal check + label, shown once a reliability opt-in has been applied. */
@Composable
private fun WizardDoneRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** A small "applying" row: a spinner + label, shown while a shell change is in flight. */
@Composable
private fun WizardApplyingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
