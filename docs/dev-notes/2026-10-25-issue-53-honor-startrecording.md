# Issue #53: Honor MagicOS 11 refuses `startRecording()` in the recorder

🧪 VERIFYING — fix merged into integration/next 2026-10-25 (1dc02189, merge 87f49364), shipping in 2.4.7.
Needs the reporter's Honor (Magic 9 Pro Max, MagicOS 11.0.0.111) and one WhatsApp call.

## What happens

Reporters: rik297981 (Magic 8 Pro, MagicOS 11 beta) and jlp9989-sudo (Magic 9 Pro Max WKL-AN20, stable
MagicOS 11.0.0.111, Android 17, 2.4.6, built-in mode). App calls are detected and both captures open, then:

```
E/CV:RecorderServer: startVoipRecording failed: null
java.lang.NullPointerException
  at java.util.Objects.requireNonNull(Objects.java:222)
  at android.media.AudioRecord.startRecording(AudioRecord.java:1573)
  at com.baba.callvault.server.VoipCaptureSession.startInternal(VoipCaptureSession.kt:151)
```

jlp9989-sudo disassembled the ROM's `AudioRecord.startRecording()`. For any source other than CAMCORDER, Honor's
audit block runs `Objects.requireNonNull(ActivityThread.currentApplication())`, then reads the app's label.
Our recorder hosts have no Application: the `app_process` daemon never has one, and `ShellAttribution`
deliberately clears the one Shizuku loads, because our own Application makes AppOps refuse the capture.
Phone calls survived because the direct path fails over to scrcpy, and scrcpy installs a fake Application.

The 2026-10-13 reply on the issue blamed an Android 17 beta framework bug. That was wrong: the ROM is stable,
and the cause is Honor's added code.

## The fix

- `VendorStartCrash` recognises the crash by its shape (an NPE thrown by code `startRecording` called), like
  `VendorConstructorCrash` does for vivo.
- `HostAudioRecord.start()` is now how the recorder starts every capture (VoIP far/near, resume, mic re-take,
  direct). Only on that crash does it run `ShellApplication.install()`, then start the same record again. The
  crash comes before the native start, so the record is still fresh.
- `ShellApplication` is scrcpy's `Workarounds` + `FakeContext`. It makes an ActivityThread if the process has
  none, and sets `mInitialApplication` to an Application over the system context. That Application reports
  package, op package and attribution as `com.android.shell` (uid 2000), so the shell identity is kept.
- Every other phone never hits the crash and keeps exactly the identity it has today.

## Evidence

- Unit tests: `HostAudioRecordStartTest` covers the reported trace, AOSP NPEs, a normal start, repair and
  retry, other errors and a failed repair.
- Emulator, 2026-10-25 (mymemory-test, API 36). A test-only build forced the install at recorder start; it
  was not committed. The daemon was launched by hand as uid 2000:
  - `currentApplication.pkg=com.android.shell` and `currentOpPackageName=com.android.shell`; binder delivery
    still ok;
  - `CaptureSelfTest`: arm=true, sinkRec=RECORDING, micRec=RECORDING, sinkDb=-17.5 (the tone was heard;
    the emulator mic is silent, as always).
- Not run: the Shizuku host. It already has an ActivityThread, so only the `mInitialApplication` swap applies.
- Not run: a real Honor phone.

## Side finding: the mode-switch check script is stale

On 2026-10-25, `tools/device-checks/mode-switch-check.sh` reported 20 failures on the OP9 with 2.4.7. The
script navigates Settings ▸ General ▸ Experimental, which the new default layout no longer has, so every row
read "-". Its VoIP-policy check also reads the event log rather than the live policies, and app-call
recording was off on the OP9 after `pm clear`. Neither is an app regression; the script needs updating for the
new layout.
