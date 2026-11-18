# Architecture

Status: revision 4 (M3 implemented). Supersedes revision 3.

## Product in one line

Tap a word you just heard in a film, keep the line, and review it the next
morning. Local-first, dark-first, no account, no backend.

## Module graph

```
:app
 ├─ :feature:today ─┐
 ├─ :feature:deck ──┤
 ├─ :feature:library┤──▶ :core:designsystem
 ├─ :feature:settings┘
 ├─ :feature:media
 ├─ :feature:capture
 └─ :core:data ──▶ :core:domain ──▶ :core:common
```

- `:core:common` — coroutine dispatchers, small pure helpers.
- `:core:domain` — models and repository interfaces. No Android, no DI.
- `:core:data` — Room, asset dictionary, repository implementations, fakes.
- `:core:designsystem` — tokens and components only.
- Features never talk to each other; `:app` owns navigation and wiring.
- `:feature:media` — MediaStore scan and the Media3 player.
- `:feature:capture` — OCR, word selection, save sheet, the floating pill
  service and the projection session. Media depends on it for in-app capture.

## Data layer

### App database (Room, `reelvocab.db`)

| Table | Purpose |
| --- | --- |
| `words` | Saved headwords: lemma, normalized form, IPA, POS, difficulty, timestamps |
| `word_senses` | Definitions attached to a saved word (dictionary or AI context) |
| `cards` | Capture cards: word, context line, film source, timestamp, audio state |
| `films` | Film/source titles shown on cards |
| `capture_sessions` | (M3) start/end, consent timestamp, recorded clip path |

Review scheduling (`reviews`, `review_logs`) arrives in M4.

### Bundled dictionary (`assets/dict.db`, read-only)

Built by `tools/build_dict.py` from WordNet 3.1, CMUdict, and the MIT
FrequencyWords list; committed so builds are reproducible offline.

| Table | Contents |
| --- | --- |
| `words` | lemma, POS, frequency rank, US IPA, difficulty 1–5 |
| `senses` | definition and example, ordered by sense number |
| `relations` | `syn` / `ant` edges between lemmas |
| `word_fts` | FTS4 index for prefix search |

`DictionaryAssetInstaller` copies the asset into `databases/dict.db` once per
`dict_version`, then opens it read-only with `SQLiteDatabase`. The thesaurus
reads `relations` for the lemma of a sense; no network.

Difficulty maps frequency rank to 1 (top 2k) through 5 (>20k), so the UI can
show dots without a learner model.

## Thesaurus on the word card

`WordCardModel` carries `synonyms` and `antonyms` resolved at enrichment time
(WordNet `syn`/`ant` relations, max 6 each, self and case-duplicates removed).
The card renders two labelled chip rows under the definition. Tapping a chip
opens the dictionary detail sheet (M5). The debug gallery and the debug
dictionary probe both show the real data.

## Capture pipeline (M3)

1. **Floating pill.** A foreground service draws a 56 dp draggable pill with
   `WindowManager` + `TYPE_APPLICATION_OVERLAY` (PIP-style bubble). It stays
   above other apps, snaps to the nearest edge, and survives app switches.
   The PiP *API* is not used: it cannot host OCR selection UI, and it would
   replace the film surface. The overlay gives the same affordance.
2. **Permission.** First tap asks for `SYSTEM_ALERT_WINDOW` via
   `ACTION_MANAGE_OVERLAY_PERMISSION`; the capture session asks for
   `MediaProjection` consent once per 15-minute session.
3. **Capture.** Tap the pill → `MediaProjection` consent (once per session)
   → `CaptureSessionService` grabs one frame, runs bundled ML Kit OCR off the
   main thread, and `CaptureActivity` shows the word-selection overlay in a
   translucent window. The pill stays draggable and snaps to the nearest edge.
4. **Save.** Selecting a word opens the save sheet (context line, film source,
   timestamp, optional audio). Saving writes a card and marks the session.
5. **End.** The session ends on notification action, after 15 minutes, or when
   the pill is dismissed; media projection is stopped and the notification is
   removed.

Permissions: `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`
(only if audio capture is on), and `USE_FULL_SCREEN_INTENT` for the M6 morning
alarm. Recording audio from other apps is not attempted in v1; the clip is
microphone-only and off by default.

## Media menu (M3)

`Library ▸ Media` (also surfaced from Today when a film is in progress) scans
local media with `MediaStore` (`mp4`, `mkv`, common audio), with a SAF
`ACTION_OPEN_DOCUMENT` fallback for files MediaStore does not index.

- Requires `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` on API 33+ and
  `READ_EXTERNAL_STORAGE` on 29–32, requested when the screen opens.
- Playback uses Media3 ExoPlayer in an app-owned player screen; captions can
  come from a sidecar `.srt`/`.vtt` chosen by the user.
- Capture from media uses the same OCR/selection pipeline, invoked in-app (no
  overlay needed, no projection consent): the player pauses, a frame is
  extracted from the player surface, and the word sheet opens.
- The media menu never uploads filenames or subtitles; nothing leaves the
  device.

## Enrichment and transcription

- `EnrichmentService` and `TranscriptionService` are interfaces in
  `:core:domain`. M2 ships deterministic fakes:
  - dictionary lookup first (IPA, definitions, thesaurus);
  - fallback definition when the word is unknown;
  - transcription returns a canned line keyed by clip name.
- M5 replaces the fakes with on-device implementations; call sites and tests
  do not change.

## Motion, type, colour

Unchanged from revision 2: semantic tokens only, Fraunces for headwords,
Inter for UI, Source Code Pro for IPA, JetBrains Mono for timestamps. Light
theme accents are darkened to hold AA when used as text.

## Milestones

| Milestone | Scope |
| --- | --- |
| M1 ✅ | Skeleton, design system, onboarding, CI |
| M2 | `:core:common/domain/data`, Room, `dict.db` with thesaurus, fakes, WordCard thesaurus UI, debug probe |
| M3 | PIP-style floating pill, capture session + OCR overlay, Media menu player |
| M4 | SRS scheduler, morning review, reminders |
| M5 | Deck/library search and tags, dictionary detail sheet, real enrichment |
| M6 | Settings depth, full-screen alarm |
| M7 | Encryption hardening, export, accessibility pass |
| M8 | Gated billing stub |
| M9 | Release polish, store metadata |

## Risks

- Overlay permission can be denied; the media menu and in-app capture remain
  fully usable, and the pill shows a one-line explanation instead of failing
  silently.
- MediaStore misses some files; SAF fallback covers them.
- WordNet 3.1 lacks modern slang; the fake/AI context path supplies a fallback
  definition and the thesaurus is hidden when empty.
- `dict.db` size: keep under ~35 MB by trimming obscure lemmas below rank
  60k and compressing relations.
