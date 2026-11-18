# Architecture

Status: revision 6 (M5.5 implemented). Supersedes revision 5.

## Product in one line

Tap a word you just heard in a film, keep the line, and review it the next
morning. Local-first, dark-first, no account, no backend.

## Module graph

```
:app
 ├─ :feature:today ─────┐
 ├─ :feature:deck ──────┤
 ├─ :feature:dictionary ┤──▶ :core:designsystem
 ├─ :feature:library ───┤
 ├─ :feature:settings ──┘
 ├─ :feature:media
 ├─ :feature:capture
 └─ :core:data ──▶ :core:domain ──▶ :core:common
```

- `:core:common` — coroutine dispatchers, small pure helpers.
- `:core:domain` — models and repository interfaces. No Android, no DI.
- `:core:data` — Room, asset dictionary, repository implementations, fakes.
- `:core:designsystem` — tokens and components only.
- Features never talk to each other; `:app` owns navigation and wiring.
- `:feature:media` — MediaStore scan for video/image/audio/documents, thumbnails
  and the Media3 player.
- `:feature:dictionary` — dictionary search, local-first lookup and detail sheet.
- `:feature:capture` — OCR, phrase selection, preview, save sheet, the floating
  pill service and the projection session. Media depends on it for in-app capture.

## Data layer

### App database (Room, `reelvocab.db`)

| Table | Purpose |
| --- | --- |
| `words` | Saved headwords: lemma, normalized form, IPA, POS, difficulty, timestamps |
| `word_senses` | Definitions attached to a saved word (dictionary or AI context) |
| `cards` | Capture cards: word, context line, film source, timestamp, audio state, tags |
| `films` | Film/source titles shown on cards |
| `capture_sessions` | (M3) start/end, consent timestamp, recorded clip path |
| `reviews` | SM-2 state per card: repetitions, ease, interval, due date, lapses |
| `review_logs` | Every grade with the resulting intervals, for history and stats |

Scheduling is SM-2 with the four grades the deck UI already uses. A new card is
due at 08:00 the next morning; `Again` resets the ladder and reappears after ten
minutes; `Hard`, `Good`, `Easy` map to qualities 3, 4, 5. Ease never drops below
1.3. `Sm2Scheduler` is pure Kotlin in `:core:domain` and unit-tested with fixed
clock and zone inputs. The morning reminder is an inexact daily `AlarmManager`
alarm at a user-selected time (08:00 by default), opt-in from Settings, restored
on boot, and gated on `POST_NOTIFICATIONS` on Android 13+.

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
The card renders two labelled chip rows under the definition. Tapping the card
opens the dictionary detail sheet (M5). The debug gallery and the debug
dictionary probe both show the real data.

## Deck and dictionary details (M5)

The Deck observes `cards` from Room and the scheduler's due ids in one view model.
Search is local and case-insensitive across the headword, tags and film title;
filters are All, Due, Words (no captured line) and Sentences (with a line). Tags
are stored on the card row as a normalized comma-separated list, so editing them
is a single update and searching needs no join. The Today tab observes
`reviews.due_at <= now` directly for its due count and refreshes it periodically.

Tapping a card in the Deck or during review opens a bottom sheet backed by
`DictionaryRepository.lookup`: every WordNet sense, example, IPA, part of speech,
difficulty and thesaurus edge. Words missing from the offline dictionary fall
back to the meaning saved with the card. Cards can be deleted from the row after
a destructive confirmation. The tag editor writes through
`CardRepository.updateTags` and updates the visible card immediately.

The dedicated Dictionary tab searches `word_fts` locally with a short debounce.
If the local search is empty, it calls the Free Dictionary API once; offline or
failed requests fall back to a clear empty state. Detail sheets are built from
the same repository entry, so local and online results render identically.

## Capture pipeline (M3)

1. **Floating pill.** A foreground service draws a compact 44 dp draggable pill
   with `WindowManager` + `TYPE_APPLICATION_OVERLAY` (PIP-style bubble). It stays
   above other apps, snaps to the nearest edge, and survives app switches until
   explicitly stopped. The PiP *API* is not used: it cannot host OCR selection UI,
   and it would replace the film surface. The overlay gives the same affordance.
2. **Permission.** First tap asks for `SYSTEM_ALERT_WINDOW` via
   `ACTION_MANAGE_OVERLAY_PERMISSION`; the capture session asks for
   `MediaProjection` consent once per service session.
3. **Capture.** Tap the pill → `MediaProjection` consent (once per session)
   → `CaptureSessionService` grabs one frame, runs bundled ML Kit OCR off the
   main thread, and `CaptureActivity` shows word chips over the frame in its own
   task. Long-pressing a word starts a phrase range; tapping the last word selects
   the phrase.
4. **Preview and save.** Selecting a word opens a save sheet with an editable
   headword, context line, source, timestamp and a dictionary preview. The preview
   is fetched before saving, but an offline lookup never blocks the save.
5. **End.** The session ends on the notification Stop action or by long-pressing
   the pill; media projection is stopped, the overlay is removed, and the
   notification is cleared.

Permissions: `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`
(only if audio capture is on), and `USE_FULL_SCREEN_INTENT` for the M6 morning
alarm. Recording audio from other apps is not attempted in v1; the clip is
microphone-only and off by default.

## Media hub (M5.5)

The Media tab scans local media with `MediaStore` and offers five chips: All,
Videos, Images, Audio and Documents. Documents are ordinary non-media files from
`MediaStore.Files`; the file picker remains the fallback for anything not indexed.

- Requires `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES` and `READ_MEDIA_AUDIO` on API
  33+ and `READ_EXTERNAL_STORAGE` on 29–32, requested when the screen opens.
- Video and image rows load real `ContentResolver` thumbnails; audio uses
  embedded album art. Results are de-duplicated by URI.
- Playback uses Media3 ExoPlayer in an app-owned player screen. Images use a
  fit viewer, documents open in another app, and audio/video share a bounded queue.
- Player gestures: vertical drag on the left changes window brightness; on the
  right it changes volume. The overflow menu holds repeat off/one/all, next, and
  subtitle selection.
- Captions can come from a sidecar `.srt`/`.vtt`/`.ttml` chosen by the user.
- Capture from media uses the same OCR/selection/preview pipeline, invoked in-app
  (no overlay needed, no projection consent): the player pauses, a frame is
  extracted, and the word sheet opens.
- The media hub never uploads filenames or subtitles; nothing leaves the device.

## Enrichment and transcription

- `EnrichmentService` and `TranscriptionService` are interfaces in
  `:core:domain`.
- Enrichment is dictionary first (IPA, definitions, thesaurus). Unknown words
  fall back to "not in the offline dictionary".
- Optional cloud definitions (M5): when enabled in Settings with a
  chat-completions URL and model, the router asks that endpoint once for the
  unknown headword plus film line. Any timeout, HTTP error, or empty answer
  silently falls back offline, so capture never blocks. Off by default; only
  those two fields leave the device.
- Online dictionary fallback: `DictionaryRepositoryImpl` checks the bundled
  `dict.db` first, then calls the Free Dictionary API only when the local entry is
  missing. This fallback is independent of the optional cloud-definition setting;
  failures leave the local result and never block the UI.
- Transcription remains the deterministic fake until the on-device recognizer
  lands.

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
| M5 ✅ | Deck/library search and tags, dictionary detail sheet, due-count wiring, optional cloud definitions |
| M5.5 ✅ | Five-tab shell, persistent small pill, phrase preview, deck delete, dictionary tab, media hub/player, reminder time picker, AI settings |
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
