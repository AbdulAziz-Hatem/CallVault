# Ukrainian localization — progressive rollout
Locale: `uk` (native Android resources, `values-uk`).
This commit translates **1012 of 1012** English `<string>` entries
used by the app. UI strings in the other resources or without translations
continue using the default English strings from `values`.

Priority completed: onboarding, ADB pairing, call recording, cloud folder
selection, notifications, recording controls, filters, storage settings,
transcription controls, and the essential Home hub.

This is a broad Ukrainian pass; a native-speaker proofreading review remains recommended.
The existing `checkTranslations` Gradle gate will still flag the remaining
missing Ukrainian strings on `assembleRelease`. Do not bypass it for a
production release. Debug builds can use native fallback while translating.
Non-translatable app-brand resources remain controlled by upstream/fork branding.

## Optional Ukrainian speech recognition

Whisper now offers `Українська` (`uk`) as an explicitly selectable
transcription language. A phone set to Ukrainian defaults to `uk` instead
of auto-detect. The wrong-script checker classifies `uk` as Cyrillic.
Recognition remains optional and manual by default.

## Follow-up
- Translate all remaining `strings.xml` and `strings_home.xml` keys,
  including the legal disclaimer and sensitive system warnings.
- Ukrainian plural forms and all string arrays have been added.
- Run `:app:checkTranslations` and `:app:lintDebug`.
- Review on an Android 12 Xiaomi and Android 13+ per-app language picker.
- Keep `%1$s`, `%1$d`, and format escapes intact.
