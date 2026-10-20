# Forks review (2026-10-20)

📐 CALCULATED — fork code read through the GitHub API, our code read on integration/next (de3fe50a). Nothing
built, run or adopted. Nothing posted on GitHub.

18 forks. 11 have no commits of their own. `diag/loopback-oneui` and `diag/scrcpy-only`, found on 8 forks,
are our own old branches.

| Fork | Branch | Verdict | Detail |
|---|---|---|---|
| kabasik007 | security/phase1-fork-hardening (12) | **Adopt the security fix**, adopt the SAF-delete fix, adapt Ukrainian | `2026-10-20-fork-kabasik007-hardening.md` |
| slman890 | feature/local-call-assistant (3) | Adopt the decode time limit; SIM label is an idea; skip the dialer | `2026-10-20-fork-slman890-call-assistant.md` |
| lgarvia | feature/speakerphone-route-resilience (1) | Skip; it uncovered a direct-capture gap of ours | `2026-10-20-fork-lgarvia-speakerphone.md` |
| sanju0070 | fix-build (2) | Adapt "import model from file" | `2026-10-20-fork-manikya-sanju0070.md` |
| manikya | samsung-whatsapp-voip-fallback (1) | Skip; we fixed it for every app in v2.2.0 (`898f0690`) | same file |
| alekseykidisyuk/CallMonitor | 5 branches | Unchanged since the 2026-09-11 pass | `2026-09-02-callmonitor-fork-assessment.md` |
| prayagkhandelwal10-hub | main (3) | Unchanged since the 2026-09-27 pass | `2026-09-27-fork-live-translated-captions.md` |
| alirezamsc2-dotcom | main (3) | Skip: a debug-APK CI workflow for themselves | — |
| Sayeed1920 | main (1) | Skip: adds submodule checkout to CI; ours already has it | — |

## Worth acting on, by priority

Update 2026-10-20: #1 fixed (`3907971e`, 🧪). #2 checked and **not needed**: see the backlog row. Android's own `AudioRecord` restores itself, and the resilient path already re-acquires.

1. **Security (medium): any app can replace our recorder.** `RecorderBinderProvider` is exported, and
   `call("sendBinder")` has no caller-uid check, unlike `sendHandoff` and the Track A probes next to it
   (`app/src/main/java/com/baba/callvault/server/RecorderBinderProvider.kt`, `handleSendBinder`). A rogue
   app could hand us its own binder, get the output fd of new recordings, and drop or fake them. It cannot
   read old recordings. Fix: the same shell/system/own-uid check. Found by kabasik007 (`7211c263`).
2. **Direct capture never recovers from a dead AudioRecord.** In `DirectAudioRecorderSession.readLoop`, `read <= 0`
   just `continue`s. On an error like ERROR_DEAD_OBJECT, the loop spins silently, the rest of the call is lost, and
   the file counts as a success. Fix: log the error, reopen, pad the gap with silence, and stop cleanly if
   reopening fails. Found by reviewing lgarvia's fix.
3. **SAF delete that fails still drops the catalog entry.** The file stays on the phone, but the app forgets it
   (`RecordingCopyWorker.kt:85-92`, `SyncSweepWorker.kt:130-133`). From kabasik007 (`6b341fc1`).
4. **Decode time limit.** `AudioDecoder` has no limit, so a stalled decoder leaves transcription "working"
   until the user taps Stop. The fix gives up after 120 s with no output. From slman890 (`8534f237`). No user report.
5. **Import a model from a file**, next to Download. From sanju0070. It needs our per-model writer lock first,
   or it brings back the double-writer bug.
6. **Ukrainian** (UI and transcription). It is fluent but trimmed: 30 strings lose facts and 11 are missing.
   About half a day of work. GPL: keep the author on the commits.
7. **SIM label per call** (dual-SIM), from slman890. It is an idea only, since no user has asked. It is cheap to
   add to our call-log lookup.

Note: three forks fixed "Android SDK setup" in their CI. Our `build-app.yml` is manual-only and has not run
since 2026-07-26, when it failed. We build releases locally, so this does not block anything.
