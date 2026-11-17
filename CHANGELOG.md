# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
semantic versioning once it ships.

## [Unreleased]

### Added

- Gradle multi-module skeleton: `:app`, `:core:designsystem`, and four feature
  modules, pinned to AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01.
- Design system: semantic dark and light palettes, AA-checked type scale, spacing,
  radius, motion, and haptics tokens.
- 20 shared components: card surface, buttons and chips, word card, context
  sentence, TTS and audio bar, badges, waveform, grade bar, streak ring, progress
  dots, stat tiles, loading/empty/error/offline states, sheets, illustrations.
- Three-tab navigation shell (Today, Deck, Library) with a saved back stack, plus
  Settings, licences, and a debug-only design-system gallery.
- Three-page interactive onboarding with a tap-to-learn demo, a quiz demo, and a
  just-in-time permission primer; completion stored in DataStore.
- Bundled Fraunces, Inter, JetBrains Mono, and Source Code Pro with OFL texts shown
  in-app. IPA uses Source Code Pro because JetBrains Mono lacks IPA glyphs.
- CI workflow that bootstraps Gradle 8.11.1, assembles debug, runs unit tests and
  lint, enforces the raw-colour token gate, and uploads the APK.
- Tests for token contrast and navigation route serialization stability.

### Notes

- Capture, dictionary enrichment, scheduling, and review arrive in M2–M4.
- No permissions are requested at launch; capture has no entry point yet.
