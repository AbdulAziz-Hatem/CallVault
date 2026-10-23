# Onboarding redesign (`feat/onboarding-revamp`)

🧪 VERIFYING — built, 2122 unit tests pass, and the emulator run below looked right in Shizuku mode.
The standalone (paired) flow and the Reliability step have not been seen by anyone. **To settle it, the
maintainer needs to** run setup on the OP9 in standalone mode (see "Maintainer's check" at the end).

## What changed

Same idea as the new Settings layout: one line per option, a control, and an ⓘ for the explanation.

- **Permissions screen.** Each permission is one row: icon, label, pill (Granted / Required), ⓘ. The old
  descriptions sit behind the ⓘ. The hero is a title, a ⓘ beside it and one line. The Wireless-debugging
  card is a title row with a Connected / Not connected pill and a ⓘ (how-to for the current state).
  The Shizuku offer card is a title and a ⓘ; buttons are unchanged.
- **Status lines that need action stay visible** and say what to do: "Developer options off — tap Build
  number 7×", "Wireless debugging is off", "Tap Pair, then enter the pairing code". The longer how-to is
  behind the ⓘ. "Required", the Shizuku "Nothing records until you do" and "setup is done" lines also stay inline.
- **Wizard steps (7–9 steps).** Each step has a header title + ⓘ + one line. Options are one line with a
  control and a ⓘ. Cards were rebuilt as `OptionCard(title, explanation, selected, onClick)`.
- **Models step.** Download **or Import** for both models (transcription and summaries), the #58 import.
  The language note shows only when the stored language really is the phone default.
- **Untouched:** `DisclaimerScreen`, permission requests, pairing, the bottom button, Shizuku logic.
- **Code.** `WizardScreen.kt` is split one file per step (`ui/screens/wizard/`). Moved private
  composables became `internal`. `PermissionsScreen.kt` is 717 lines (cap 800).
- **Guard test `OnboardingInfoModeTest`** (4 tests). It scans the wizard and permissions sources and fails
  if an explanation string is passed inline instead of through ⓘ, if a ⓘ has an empty explanation, or if one
  of the listed status keys is no longer shown as plain `Text` / pill / `DevModeStatusLine`. It
  **cannot catch**: nested parentheses or variables in an empty explanation, a status key that is inline
  but unreachable, or a string returned from a function (`stepInfo()`); `wizard_storage_subtitle` is
  allow-listed for that reason. It cannot judge layout.
- **Strings.** New: `permissions_ui_hero_line`, `permission_status_devmode_off_tap_build`,
  `permission_status_wd_on_enter_code` (15 locales). Deleted: `permission_devmode_on`,
  `permission_devmode_off`, `permission_wd_on`.

## Emulator run, 2026-10-23

AVD `mymemory-test`, **Shizuku mode**, debug build.

- **English:** permissions screen and wizard rows read as one line each; models step shows Download and
  Import for both models; the ADB ⓘ dialog shows the right how-to.
- **Arabic (RTL):** layout mirrors correctly (ⓘ on the left, dropdown ⓘ, models step).
- **Russian at 1.3× font:** first run found broken labels: permission labels split mid-word
  ("Уведомлени/я"), the ADB title clipped ("Wireless debuggi"), the Shizuku buttons split mid-word.
  Fixed in `09f6b7d8` (label + pill now wrap as a unit, Shizuku buttons in a `FlowRow`) and **re-checked**:
  labels whole, pills drop under the label, ADB title full, buttons wrap whole, Arabic still mirrors.
- **NOT seen:** the **standalone (paired) flow** and the **Reliability step** (USB "Charging only" row).
  Pairing on the emulator breaks the emulator's adb, so it was not tried. Their look is unchecked.

## Decisions taken on the maintainer's behalf

- Moved private composables became `internal` so they compile across files. Cost: wider visibility.
- The guard test's first version ignored each test separately, so the models-step test ran at once; the
  ignores were removed once the permissions screen was done.
- Permissions screen stayed in one file (no extra file needed).
- Emulator "before" screenshots came from a build of `cf3a0d70`.
- The final review's fix wave covered: clipped labels, actionable status lines, unused imports, the AAC
  tip (no ⓘ when AAC is already chosen), Shizuku buttons wrapping, the hero line wording.
- The Shizuku card gap and the storage-folder row's double padding were left as cosmetic.
- The "Pair after Shizuku ready" button problem (below) was not fixed here: one concern per branch.
- The merge into `integration/next` happens after the final review.

## Accepted trade-offs

- The conditional Drive-sync and immediate-upload notes are now always in the header ⓘ, not shown only when relevant.
- The Shizuku offer card has a small empty gap between title and buttons (the body moved into the ⓘ). Seen, cosmetic.
- The storage folder row has extra padding inside its card (seen on the emulator, tolerable).
- When connected, the ADB card shows only the Connected pill, not "Developer Options: on / Wireless debugging: on".
- Permission rows are separate cards with their own ⓘ, not one grouped card as in Settings.
- Persian wording of "Pair" in the new status key may not match its `permission_adb_pair` exactly. 📐 CALCULATED (read from code).

## Found, not fixed (not from this branch)

- **After "Use Shizuku" → "Ready — using Shizuku", the permissions screen says setup is done but the
  bottom button still reads "Pair" and opens Developer options.** Back goes to wizard step 1.
  Reproduced on the pre-redesign build `cf3a0d70`, so it predates this work. Belongs in the backlog.
- The "SETUP" eyebrow overlaps the status bar on the permissions screen (also in the "before" screenshots).
- Russian "входящий вызовов" is ungrammatical.
- `permission_adb_hint_wd_on` (and `permission_adb_hint`) still say "Tap **Authorize**" while the button
  says "**Pair**". The fix wave did not change this (checked in the current strings). The new status line
  correctly says "Pair".
- Also noted: English hint copy such as the old hero subtitle is unchanged behind the ⓘ.

## Maintainer's check

On the OP9 (rooted, wiped): clear CallVault's data to re-run onboarding, **standalone mode**.

1. Judge the look of the permissions screen and each wizard step.
2. Developer options off: does the status line read "Developer options off — tap Build number 7×" and is it clear what to do?
3. Wireless debugging off, then on but not paired: do the status lines match what you see?
4. Run the pairing flow end to end.
5. The **Reliability step** (USB "Charging only" row): nobody has seen it yet.

Tell me what you saw and I will update this note.
