# Release notes draft: next version (after 2.4.6)

🧪 VERIFYING — draft, 2026-10-20. Scope: `v2.4.6..integration/next` (e9644840). Full detail and status per line
in `CHANGELOG.md` → Unreleased. Re-check every line against the shipped strings before the release.

- **App calls in Shizuku mode** — app calls such as WhatsApp now record in Shizuku mode too (Android 14+).
- **Shizuku mode catches up** — resilient recording and speaker names work there too, and phone calls start recording faster.
- **Persian transcription** — Persian can be pinned as a transcription language.
- **Hide Recordings** — keeps your recordings out of music players and galleries.
- **Also skip ignored contacts** — your ignored-contacts list can apply to app calls too.
- **Honor / Huawei phone app** — a new file-name format so Honor's phone app can play recordings from the call history.
- **Fixed** — Settings updates straight away when you switch modes.
- **Fixed** — contact- and number-first file names show their arrow, merge, and are found by search again.
- **Fixed** — Shizuku mode: the recorder comes back after Android kills it.
- **Fixed** — Greek and Persian summaries are written in the right language.
- **Fixed** — a recording whose number arrived late kept the call's end time instead of its start.
- **Fixed** — contact names no longer appear in shared debug reports.
- **Security** — other apps can no longer take over the recorder (thanks @kabasik007).

## To do at release

- Tell the #56 reporter to try the release: pick "Honor / Huawei phone app" under the file-name format, set the
  recording folder to `Sounds/CallRecord` and the audio codec to AAC, then check the play button on a saved
  contact, an unsaved number, and a call that rang longer than it lasted.
