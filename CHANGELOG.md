# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
semantic versioning once it ships.

## [Unreleased]

### Added

- M6 settings depth: an app-wide System/Dark/Light theme choice that applies
  immediately, a version row in About, and a full-screen morning alarm toggle.
  The alarm channel is now high-importance with a full-screen intent; on Android
  14+ Settings explains when the system still needs to allow full-screen
  notifications and links straight to that screen. Tapping the reminder now opens
  the morning review instead of the home tab.
- M5.5 polish: five-tab shell (Home, Deck, Dictionary, Library, Media) with
  per-tab state, a compact persistent floating pill that no longer times out,
  capture previews and long-press phrase selection, deck-card deletion,
  a dedicated dictionary tab with online fallback, a media hub for video, images,
  audio and documents, player gestures and subtitles, an editable reminder time,
  and a clearer AI dictionary section.
- M5 deck: a real vocabulary listing with local search over headwords, tags and
  film titles; All/Due/Words/Sentences filters; and per-card tag editing stored
  through a Room v2→v3 migration.
- M5 dictionary detail sheet with every WordNet sense, example, IPA, part of
  speech, difficulty and thesaurus edge, opened from the Deck or during review.
- Today now shows the live SM-2 due count from the `reviews` table instead of a
  static placeholder, and the deck's Due filter uses the same scheduler ids.
- M2 data foundation: `:core:common`, `:core:domain`, and `:core:data` modules,
  Room schema v1 (words, senses, films, cards), repository implementations, and
  fake enrichment/transcription services behind Hilt bindings.
- Bundled offline dictionary `assets/dict.db` (12.5 MB): 31,679 headwords, 68,455
  WordNet 3.1 senses, 55,738 synonyms, 3,420 antonyms, CMUdict-derived US IPA and
  frequency ranks. Built by `tools/build_dict.py`, gated by `tools/verify_dict.py`.
- Media hub: MediaStore scan for local videos, images, audio and documents with
  thumbnails, an `ACTION_OPEN_DOCUMENT` fallback, Media3 playback, player
  gestures, subtitle selection, and in-app frame capture through the shared OCR
  sheet.
- Floating word pill (`SYSTEM_ALERT_WINDOW`) plus a persistent
  `mediaProjection` foreground session that captures one frame per tap and stays
  until explicitly stopped.
- Bundled ML Kit Latin OCR (no network, no Play Services model download) and
  a word-selection save sheet that writes a card.
- SM-2 review scheduler in `:core:domain` with Room `reviews`/`review_logs`
  tables and a v1→v2 migration; new cards are scheduled for 08:00 the next
  morning and `Again` re-queues after ten minutes.
- Opt-in morning reminder: inexact daily alarm at a user-selected time (08:00 by
  default), boot restore, and a notification that opens the app; only fires with
  notification permission.
- Morning review screen with the shared word card and the Again/Hard/Good/Easy
  grade bar, entered from Today.
- Optional cloud definitions for words missing from `dict.db`: off by default,
  OpenAI-compatible chat endpoint, dictionary results still win, failures fall
  back offline. Configured in Settings and stored on-device.
- Thesaurus rows on the word card (synonyms in teal, antonyms in rose), plus a
  debug-only dictionary probe screen reachable from Settings.

- Gradle multi-module skeleton: `:app`, `:core:designsystem`, and the feature
  modules, pinned to AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01.
- Design system: semantic dark and light palettes, AA-checked type scale, spacing,
  radius, motion, and haptics tokens.
- 20 shared components: card surface, buttons and chips, word card, context
  sentence, TTS and audio bar, badges, waveform, grade bar, streak ring, progress
  dots, stat tiles, loading/empty/error/offline states, sheets, illustrations.
- Five-tab navigation shell (Today, Deck, Dictionary, Library, Media) with a
  saved back stack, plus Settings, licences, and a debug-only design-system
  gallery.
- Three-page interactive onboarding with a tap-to-learn demo, a quiz demo, and a
  just-in-time permission primer; completion stored in DataStore.
- Bundled Fraunces, Inter, JetBrains Mono, and Source Code Pro with OFL texts shown
  in-app. IPA uses Source Code Pro because JetBrains Mono lacks IPA glyphs.
- CI workflow that bootstraps Gradle 8.11.1, assembles debug, runs unit tests and
  lint, enforces the raw-colour token gate, and uploads the APK.
- Tests for token contrast and navigation route serialization stability.

### Notes

- Light-theme accents are darkened (gold `#8A6A1F`, gold-dim `#7A5E1B`, teal
  `#17727A`, rose `#C0394C`, green `#217A52`) so they hold AA when used as text;
  light-theme on-accent is white. Dark stays the primary theme.
- Capture, dictionary enrichment, scheduling, and review arrive in M2–M4.
- Permissions are requested just in time from their feature screens; nothing is
  requested during onboarding.
