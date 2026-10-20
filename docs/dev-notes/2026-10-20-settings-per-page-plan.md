# Settings per page: plan (2026-10-20)

📐 PLANNED. Nothing built, and no date set (maintainer, 2026-10-20: "plan for this properly, I'm not sure
when we are going to do this"). This plan replaces the section-grouping question on the earlier Settings
options canvas (https://claude.ai/artifact/2xGqeTEG3aUU1rqrn2VyHN). Re-check the code references below
before starting. They were read on integration/next at 19e722a0.

## Decisions (confirmed by the maintainer, 2026-10-20)

1. **Settings opened from a page shows only that page's settings.** Recordings shows recording settings,
   Transcripts shows transcription settings, Summaries shows summary settings. Opening it from the main
   screen shows everything.
2. **The Experimental section goes.** Its 7 switches move to where they belong. **Their defaults do not
   change**: they all stay off unless the user turns them on. Only the "Experimental" label and its wording
   go. Reasons to keep the defaults:
   - **Recording away from Wi-Fi** opens a debugging port on the phone. Shipping it as a warned opt-in was a
     decision ([[loopback-tcpip-offwifi]]).
   - **App calls**: the "far side can't hear me" report is still open, waiting on logs
     ([[voip-mic-capture-mutes-user]]).
   - **Resilient recording** could become on by default later, but as its own change with its own testing.
3. **The Telegram pill shows only on the main screen,** so each page's title row has room for its settings
   icon. **The ♥ Support pill stays as it is**, on every page.

## What each page shows

Today's sections: the mode switch (at the top, outside every section), then Recording, Storage (with Retention
and Merge), Audio, Transcription & summaries, General (Visual, Privacy, Experimental, Updates), Debug, About,
and export/import at the very bottom (`SettingsScreen.kt:530-665`).

| Setting (today's place) | Recordings | Transcripts | Summaries | Main screen (all) |
|---|---|---|---|---|
| Recording mode: built-in or Shizuku (top) | ✔ first | | | ✔ first |
| Recording: phone calls, incoming/outgoing, ignored contacts, record-only-selected, start when they answer, file-name template | ✔ | | | ✔ |
| **App calls** (from Experimental ▸ VoIP): record app calls, which apps, start automatically, skip ignored contacts | ✔ new subsection "App calls" | | | ✔ |
| **Reliability** (from Experimental ▸ Resilience): resilient recording, recording away from Wi-Fi, USB debugging, keep Wireless debugging on, USB mode on unlock, pair again | ✔ new subsection "Reliability" | | | ✔ |
| Audio: source, codec, bit rate | ✔ | | | ✔ |
| Storage: where to save, folders, Drive and its schedule, details file, Hide Recordings, retention / storage cap, merge | ✔ | | | ✔ |
| "Save transcript and notes beside the recording" (Storage today) | ✔ (it is a file in the folder) | link ▸ | | ✔ |
| Transcription: model, language, run at, only while charging, ask language, ask before transcribing | | ✔ | | ✔ |
| Summaries: model, the summaries dialog switch | | | ✔ | ✔ |
| **Language** (shared on purpose, see below) | | ✔ editable | shown read-only, with a link ▸ | ✔ |
| General: look, toasts, vibration | | | | ✔ |
| Privacy: app lock, screenshots | | | | ✔ |
| Updates | | | | ✔ |
| Debug (logging, share log) | footer link | footer link | footer link | ✔ |
| About, support, licences, fork attribution | | | | ✔ (§7 attribution must stay reachable) |
| Export / import settings | | | | ✔ |

Every page view ends with **"All settings ›"**, which opens the full list. That is the way out for someone
looking in the wrong place.

## Rules the split must keep

- **One home per setting.** No setting is duplicated across pages. Where another page needs it, show a
  one-line link to it ("Backed up with the recording: Drive settings ›").
- **Transcription and summaries share one language setting.** Today's code says why: two separate language
  settings once produced transcripts in the wrong language (`SettingsScreen.kt:591-595`). The Summaries page
  must not get a language picker of its own.
- **The §7 fork attribution stays reachable** (GPLv3 terms, [[upstream-license-section7-attribution]]).
- **The wizard cannot be re-run** ([[new-features-consider-onboarding]]). Every setting the wizard sets
  still needs a home. `SettingDecisionsTest`, `ModeAwareUiTest` and `tools/device-checks/mode-switch-check.sh`
  enforce this. Keep them green and update them; don't weaken them.
- **Settings that change with the mode** (greyed out in Shizuku mode, and so on) must keep updating live
  on every page view. This was fixed in `bca782f5`.

## How to build it

- **One list, tagged by page; not four copies.** Each section or row gets a page tag:
  `SettingsScope { RECORDINGS, TRANSCRIPTS, SUMMARIES, ALL }`. Opening Settings passes the scope, and the list
  filters on it. Filtering one list keeps the guard tests meaningful and stops the pages drifting apart.
- **Entry points.** Every page's settings icon opens the same drawer today (`AppNavigationScreen.kt:291`;
  `onOpenSettings` in `HomeScreen`, `TranscriptsScreen`, `SummariesScreen`, `HubScreen`). Each will pass its
  own scope; the Hub passes ALL.
- **The open section is remembered per scope**, so Recordings doesn't reopen on a Transcription section.
  Saved section keys from today still need to map somewhere: today `SUB_EXPERIMENTAL` opens General, and
  it should open Recordings ▸ Reliability.
- **Split `SettingsScreen.kt` first** (3,461 lines; the coding rule is 800 at most) into one file per area:
  recording, storage, audio, transcription, summaries, general, about/debug. Do this as a **separate commit
  with no change in behaviour**, so the scope change lands on readable code and can be reverted on its own.

## Experimental removal: what has to change

- **The section:** `ExperimentalSubSection` (`SettingsScreen.kt:2274`) and its entry in `GeneralSection`.
  The rows move as listed above.
- **Text that points users to "Settings ▸ Experimental"**, in all 15 languages:
  - the notifications `notif_usb_lock_warning` and `notif_usb_check_unavailable`, and `notif_wd_required`
  - `offline_failed_needs_usb`
  - `settings_voip_recording_description`, which calls the feature experimental
  - `voip_recording_warning_message`, `settings_persistent_server_description` and
    `settings_handoff_persist_description`: check each one's wording
  - the old `whatsnew_146/147/150_where` strings. They are no longer shown, so there's no need to retranslate them.
- **The wizard step** `wizard_experimental_title` / `_subtitle` ("A feature still being proven"), and
  `WizardStep.EXPERIMENTAL`. That is the internal name, used in `SettingDecisionsTest:107`.
- **`tools/device-checks/mode-switch-check.sh`** navigates Settings ▸ GENERAL ▸ Experimental (lines 6, 26-29).
  It needs the new path.
- **The README** says "Opt-in, experimental" (line 30) and points to *Settings ▸ Experimental* (line 177). The
  README is held, so log these in `readme-impact-log` ([[readme-impact-log]]).

## Telegram pill

- Today the title row on every page shows the Support pill, and the Telegram pill too once the card has been tucked
  away (`HomeScreen.kt:900-925`, `CommunityPill`). Show the Telegram pill only on the Hub. Long-press to
  restore keeps working there.
- The transcription-progress pill keeps its place on every page. It takes the slot while a transcription is running.

## Order of work

0. **Design canvas.** Two or three looks for a page's settings view: a header naming the page, the
   "All settings" footer, the Reliability and App-calls subsections, and the cleaner title row. The maintainer
   picks before any code ([[offer-design-options]]).
1. Split `SettingsScreen.kt` into files, with no change in behaviour. Run all tests, and compare before/after
   screenshots on the OP9.
2. Add the scope, the per-page entry points and the "All settings" footer.
3. Remove Experimental: move the rows, change the strings in 15 languages, change the wizard text, update the
   tests and mode-switch-check.
4. Make the Telegram pill Hub-only.
5. On the phone:
   - Open Settings from each of the four places, in both built-in and Shizuku mode.
   - Run mode-switch-check.
   - Check that a saved old section key lands on the right page.
   - Write the release-note lines.

Each step is its own branch and commit ([[always-keep-rollback-available]]).

## Open questions (for when we start)

- **Debug footer:** should "Share a debug report" sit in every page's footer? I suggest yes. It is what
  support asks every reporter for, and it is easy to miss under All settings.
- **Search:** a search box over every setting. Probably later; "All settings" covers it at first.
- **Resilient recording on by default:** a separate decision, not part of this.
