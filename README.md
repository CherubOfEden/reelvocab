# ReelVocab

Learn the English you actually hear in films. Watch with subtitles, tap a word you
missed, and get a card that remembers the line, the film, and the moment. A short
quiz the next morning keeps it.

Local-first: cards, reviews, and audio stay on the device. No account, no backend,
no analytics.

## Status

`0.1.0` — milestone **M1: skeleton and design system**. Navigation shell, onboarding,
theme tokens, component gallery, and CI gates exist. Capture, dictionary enrichment,
scheduling, and review are M2–M4.

## Building

There is no local Android SDK in the development environment (Termux on Android), so
every build runs in GitHub Actions:

1. Push to `main` (or open a pull request).
2. The `build` workflow bootstraps Gradle 8.11.1, then runs `assembleDebug`,
   `testDebugUnitTest`, and `lintDebug`.
3. Download the `reelvocab-debug` artifact from the workflow run and install it.

```bash
adb install -r reelvocab-debug/app-reelvocab-debug.apk
```

Local runs need JDK 17 and an Android SDK (compileSdk 35):

```bash
./gradlew assembleDebug
```

## Modules

| Module | Holds |
| --- | --- |
| `:app` | `MainActivity`, onboarding flow, tab shell, licences, debug gallery |
| `:core:common` | dispatchers and small pure helpers |
| `:core:domain` | models and repository interfaces |
| `:core:data` | Room, bundled dictionary, repositories, fakes |
| `:core:designsystem` | colour, type, spacing, motion tokens; 21 shared components |
| `:feature:today` | daily quiz entry point |
| `:feature:deck` | saved cards, filters |
| `:feature:library` | films and sources, media menu |
| `:feature:settings` | plan, appearance, licences, privacy |
| `:feature:media` | MediaStore scan, in-app local video/audio player |
| `:feature:capture` | OCR, word selection, floating pill, projection session |

Dependency direction: features depend on `:core:designsystem`; `:feature:media`
also consumes `:feature:capture` for in-app frame capture. `:app` wires the
navigation graph and depends on `:core:data`. Nothing depends on `:app`.

## Design system rules

- Dark first. Light is a variant of the same semantic tokens, never a second palette.
- All colours live in `core/designsystem/theme/Palette.kt`. CI fails on `Color(0x`
  literals anywhere else.
- Fraunces is only for headwords, hero words, and quiz prompts. Inter for UI text.
  Source Code Pro for IPA (JetBrains Mono has no IPA glyphs). JetBrains Mono for
  timestamps and counters.
- Motion reads `Settings.Global.ANIMATOR_DURATION_SCALE`; when animations are off,
  transitions collapse to 0 ms instead of disappearing.
- Text meets WCAG AA at the sizes used; `TokenContrastTest` enforces the pairs.

Open the gallery from Settings (debug builds only) to see every component against
the live theme.

## Permissions

Nothing is requested during onboarding. Screen capture, microphone,
notifications, and "display over other apps" are each requested the first time
their feature is used. The floating word pill is a PIP-style overlay drawn with
`SYSTEM_ALERT_WINDOW`, not the PiP API: it must sit above other apps without
replacing the video surface.

The Media menu reads local video and audio through `MediaStore` (or the system
file picker) and plays it inside the app; capture works there without any
screen-projection consent. The floating pill asks for overlay access, then for
media-projection consent once per 15-minute session. OCR uses the bundled ML Kit
Latin model: no network and no Play Services model download. Long-press the
pill, use the notification Stop action, or wait 15 minutes to end a session.

## Verification gates

- `assembleDebug` must succeed.
- Unit tests: token contrast, route serialization stability, headword
  normalization, dictionary shape.
- `lintDebug` with `abortOnError`.
- Raw-colour grep gate.
- `tools/verify_dict.py` checks the bundled `dict.db` schema and row counts.
- Debug APKs come from CI artifacts only.

## Roadmap

| Milestone | Scope |
| --- | --- |
| M1 ✅ | Skeleton, design system, onboarding, CI |
| M2 ✅ | Data foundation: Room, bundled dictionary with thesaurus, enrichment and transcription fakes |
| M3 ✅ | PIP-style floating pill, capture session with OCR overlay, in-app media player |
| M4 | SRS scheduler and morning review |
| M5 | Deck and library content, search, tags, dictionary detail |
| M6 | Settings depth, reminders, full-screen alarm |
| M7 | Encryption hardening, export, accessibility pass |
| M8 | Gated billing stub |
| M9 | Release polish, store metadata |

## Licences

Bundled typefaces (Fraunces, Inter, JetBrains Mono, Source Code Pro) are licensed
under the SIL Open Font Licence. Full texts ship in `app/src/main/assets/licenses/`
and are readable in-app under Settings → Fonts and licences.
