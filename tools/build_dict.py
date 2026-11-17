#!/usr/bin/env python3
"""Build the bundled read-only dictionary (assets/dict.db).

Sources:
  WordNet 3.1 (Princeton)  -> senses, synonyms, antonyms, part of speech
  CMUdict                  -> US IPA (ARPAbet converted, simple syllabification)
  MIT FrequencyWords 50k   -> frequency rank and difficulty

Usage:
  python3 tools/build_dict.py --out core/data/src/main/assets/dict.db

The build is deterministic for a given set of downloads; the cache directory
keeps the tarball/text files between runs.
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import sqlite3
import sys
import tarfile
import urllib.request

WN_URL = "https://wordnetcode.princeton.edu/wn3.1.dict.tar.gz"
CMU_URL = "https://raw.githubusercontent.com/cmusphinx/cmudict/master/cmudict.dict"
FREQ_URL = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_50k.txt"

POS_NAMES = {
    "n": "noun",
    "v": "verb",
    "a": "adjective",
    "s": "adjective",
    "r": "adverb",
}

ARPABET = {
    "AA": "\u0251",
    "AE": "\u00e6",
    "AH": "\u028c",
    "AO": "\u0254",
    "AW": "a\u028a",
    "AY": "a\u026a",
    "EH": "\u025b",
    "ER": "\u025d",
    "EY": "e\u026a",
    "IH": "\u026a",
    "IY": "i",
    "OW": "o\u028a",
    "OY": "\u0254\u026a",
    "UH": "\u028a",
    "UW": "u",
    "B": "b",
    "CH": "t\u0283",
    "D": "d",
    "DH": "\u00f0",
    "F": "f",
    "G": "\u0261",
    "HH": "h",
    "JH": "d\u0292",
    "K": "k",
    "L": "l",
    "M": "m",
    "N": "n",
    "NG": "\u014b",
    "P": "p",
    "R": "\u0279",
    "S": "s",
    "SH": "\u0283",
    "T": "t",
    "TH": "\u03b8",
    "V": "v",
    "W": "w",
    "Y": "j",
    "Z": "z",
    "ZH": "\u0292",
}

VOWELS = {"AA", "AE", "AH", "AO", "AW", "AY", "EH", "ER", "EY", "IH", "IY", "OW", "OY", "UH", "UW"}
ONSETS = {
    "pr", "br", "tr", "dr", "kr", "ɡr", "fr", "vr", "θr", "ʃr", "pl", "bl", "kl", "ɡl", "fl",
    "sl", "sp", "st", "sk", "sm", "sn", "sw", "tw", "kw", "dw", "pj", "kj", "mj", "nj", "lj",
    "str", "spr", "skr", "spl", "skw",
}


def download(url: str, dest: str) -> None:
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        return
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    print(f"download {url}", file=sys.stderr)
    req = urllib.request.Request(url, headers={"User-Agent": "reelvocab-dict-builder"})
    tmp = dest + ".part"
    with urllib.request.urlopen(req, timeout=120) as response, open(tmp, "wb") as out:
        while True:
            chunk = response.read(1 << 20)
            if not chunk:
                break
            out.write(chunk)
    os.replace(tmp, dest)


def load_frequency(path: str, limit_rank: int) -> dict[str, int]:
    ranks: dict[str, int] = {}
    with open(path, encoding="utf-8") as handle:
        rank = 0
        for line in handle:
            parts = line.split()
            if len(parts) < 2:
                continue
            word = parts[0].strip().lower()
            if not re.fullmatch(r"[a-z][a-z'-]*", word):
                continue
            rank += 1
            if rank > limit_rank:
                break
            ranks.setdefault(word, rank)
    return ranks


def syllabify(units: list[tuple[str, str | None]]) -> str:
    vowel_positions = [i for i, (_, stress) in enumerate(units) if stress is not None]
    if not vowel_positions:
        return "".join(phoneme for phoneme, _ in units)

    boundaries: list[int] = []
    start = 0
    for previous, current in zip(vowel_positions, vowel_positions[1:]):
        cluster = units[previous + 1:current]
        size = 0
        for take in (3, 2, 1):
            if take > len(cluster):
                continue
            candidate = "".join(phoneme for phoneme, _ in cluster[len(cluster) - take:])
            if take == 1 or candidate in ONSETS:
                size = take
                break
        boundary = current - size
        boundaries.append(boundary)
        start = boundary

    cuts = [0] + boundaries + [len(units)]
    syllables = []
    for left, right in zip(cuts, cuts[1:]):
        piece = units[left:right]
        stress = next((s for _, s in piece if s), None)
        text = "".join(phoneme for phoneme, _ in piece)
        if len(vowel_positions) > 1:
            if stress == "1":
                text = "\u02c8" + text
            elif stress == "2":
                text = "\u02cc" + text
        syllables.append(text)
    return "".join(syllables)


def arpabet_to_ipa(phones: list[str]) -> str:
    units: list[tuple[str, str | None]] = []
    for token in phones:
        match = re.fullmatch(r"([A-Z]+)([0-2]?)", token)
        if not match:
            continue
        base, stress = match.group(1), match.group(2) or None
        if base not in ARPABET:
            continue
        phoneme = ARPABET[base]
        if base == "AH" and stress == "0":
            phoneme = "\u0259"
        elif base == "ER" and stress == "0":
            phoneme = "\u025a"
        units.append((phoneme, stress if base in VOWELS else None))
    return syllabify(units)


def load_cmudict(path: str) -> dict[str, str]:
    pronunciations: dict[str, str] = {}
    with open(path, encoding="utf-8", errors="ignore") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith(";;;"):
                continue
            head, _, rest = line.partition(" ")
            word = head.strip().lower()
            if word.endswith(")") or "'" in word:
                continue
            if word in pronunciations:
                continue
            ipa = arpabet_to_ipa(rest.split())
            if ipa:
                pronunciations[word] = ipa
    return pronunciations


def difficulty_for(rank: int | None) -> int:
    if rank is None:
        return 5
    if rank <= 2000:
        return 1
    if rank <= 5000:
        return 2
    if rank <= 10000:
        return 3
    if rank <= 20000:
        return 4
    return 5


def load_sense_numbers(path: str) -> dict[tuple[str, str], int]:
    numbers: dict[tuple[str, str], int] = {}
    with open(path, encoding="latin-1") as handle:
        for line in handle:
            parts = line.split()
            if len(parts) < 3:
                continue
            key, offset, number = parts[0], parts[1], parts[2]
            lemma = key.split("%")[0].replace("_", " ").lower()
            try:
                numbers[(lemma, offset)] = int(number)
            except ValueError:
                continue
    return numbers


def split_gloss(gloss: str) -> tuple[str, str | None]:
    definition, _, example = gloss.partition('; "')
    if example:
        example = example.rstrip().rstrip('"').strip()
    else:
        example = None
    return definition.strip(), example


def read_wordnet(extract_dir: str):
    synsets = {}
    ordered = []
    for filename in ("data.noun", "data.verb", "data.adj", "data.adv"):
        path = os.path.join(extract_dir, filename)
        with open(path, encoding="latin-1") as handle:
            for line in handle:
                if not line or line.startswith("  "):
                    continue
                head, _, gloss = line.partition("|")
                parts = head.split()
                if len(parts) < 5:
                    continue
                offset, _lex, kind, count = parts[0], parts[1], parts[2], int(parts[3], 16)
                index = 4
                lemmas = []
                for _ in range(count):
                    lemmas.append((parts[index].replace("_", " ").lower(), int(parts[index + 1], 16)))
                    index += 2
                pointers = []
                pointer_count = int(parts[index])
                index += 1
                for _ in range(pointer_count):
                    symbol = parts[index]
                    target_offset = parts[index + 1]
                    target_pos = parts[index + 2]
                    source_target = parts[index + 3]
                    index += 4
                    try:
                        source = int(source_target[:2], 16)
                        target = int(source_target[2:], 16)
                    except ValueError:
                        source, target = 0, 0
                    pointers.append((symbol, target_offset, target_pos, source, target))
                entry = {
                    "offset": offset,
                    "pos": POS_NAMES.get(kind, "noun"),
                    "lemmas": lemmas,
                    "pointers": pointers,
                    "gloss": gloss.strip(),
                }
                synsets[(offset, kind)] = entry
                ordered.append(entry)
    return synsets, ordered


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="core/data/src/main/assets/dict.db")
    parser.add_argument("--cache", default=os.path.expanduser("~/.cache/reelvocab-dict"))
    parser.add_argument("--limit-rank", type=int, default=50000)
    args = parser.parse_args()

    wn_archive = os.path.join(args.cache, "wn3.1.dict.tar.gz")
    cmu_path = os.path.join(args.cache, "cmudict.dict")
    freq_path = os.path.join(args.cache, "en_50k.txt")
    download(WN_URL, wn_archive)
    download(CMU_URL, cmu_path)
    download(FREQ_URL, freq_path)

    extract_dir = os.path.join(args.cache, "wn3.1")
    if not os.path.isdir(extract_dir):
        os.makedirs(extract_dir, exist_ok=True)
        with tarfile.open(wn_archive, "r:gz") as archive:
            archive.extractall(extract_dir, filter="data")

    frequency = load_frequency(freq_path, args.limit_rank)
    pronunciations = load_cmudict(cmu_path)
    data_dir = os.path.join(extract_dir, "dict")
    sense_numbers = load_sense_numbers(os.path.join(data_dir, "index.sense"))
    if not os.path.isdir(data_dir):
        data_dir = extract_dir
    synsets, ordered = read_wordnet(data_dir)

    kept_lemmas = set(frequency)
    words: dict[tuple[str, str], int] = {}

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    if os.path.exists(args.out):
        os.remove(args.out)

    connection = sqlite3.connect(args.out)
    connection.executescript(
        """
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        CREATE TABLE words(
            id INTEGER PRIMARY KEY,
            lemma TEXT NOT NULL,
            pos TEXT NOT NULL,
            ipa TEXT,
            freq_rank INTEGER,
            difficulty INTEGER NOT NULL,
            UNIQUE(lemma, pos)
        );
        CREATE INDEX words_lemma ON words(lemma);
        CREATE TABLE senses(
            id INTEGER PRIMARY KEY,
            word_id INTEGER NOT NULL REFERENCES words(id),
            definition TEXT NOT NULL,
            example TEXT,
            sense_order INTEGER NOT NULL DEFAULT 99
        );
        CREATE INDEX senses_word ON senses(word_id);
        CREATE TABLE relations(
            id INTEGER PRIMARY KEY,
            word_id INTEGER NOT NULL REFERENCES words(id),
            type TEXT NOT NULL,
            target TEXT NOT NULL
        );
        CREATE INDEX relations_word ON relations(word_id);
        """
    )

    relations: dict[tuple[int, str, str], None] = {}
    word_rows = []
    sense_rows = []
    sense_id = 0
    word_id = 0

    for synset in ordered:
        pos = synset["pos"]
        definition, example = split_gloss(synset["gloss"])
        members = [(lemma, num) for lemma, num in synset["lemmas"] if lemma in kept_lemmas and " " not in lemma]
        if not members:
            continue
        sense_id += 1
        word_ids = []
        for lemma, _num in members:
            key = (lemma, pos)
            existing = words.get(key)
            if existing is None:
                word_id += 1
                existing = word_id
                words[key] = existing
                rank = frequency.get(lemma)
                word_rows.append(
                    (
                        existing,
                        lemma,
                        pos,
                        pronunciations.get(lemma),
                        rank,
                        difficulty_for(rank),
                    )
                )
            word_ids.append(existing)
        for wid, (lemma, _) in zip(word_ids, members):
            sense_id += 1
            sense_rows.append(
                (
                    sense_id,
                    wid,
                    definition,
                    example,
                    sense_numbers.get((lemma, synset["offset"]), 99),
                )
            )

        lemma_set = {lemma for lemma, _ in members}
        for wid, (lemma, _) in zip(word_ids, members):
            for other in lemma_set:
                if other != lemma:
                    relations[(wid, "syn", other)] = None

        for symbol, target_offset, target_pos, _source, target in synset["pointers"]:
            if symbol != "!":
                continue
            target_synset = synsets.get((target_offset, target_pos))
            if target_synset is None and target_pos in ("a", "s"):
                target_synset = synsets.get((target_offset, "s" if target_pos == "a" else "a"))
            if not target_synset:
                continue
            target_lemma = next(
                (lemma for lemma, num in target_synset["lemmas"] if num == target),
                None,
            )
            if target_lemma is None:
                target_lemma = next(
                    (lemma for lemma, _ in target_synset["lemmas"] if lemma in kept_lemmas),
                    None,
                )
            if not target_lemma or target_lemma not in kept_lemmas or " " in target_lemma:
                continue
            for wid, (lemma, _) in zip(word_ids, members):
                if lemma != target_lemma:
                    relations[(wid, "ant", target_lemma)] = None

    connection.executemany(
        "INSERT INTO words(id, lemma, pos, ipa, freq_rank, difficulty) VALUES (?, ?, ?, ?, ?, ?)",
        word_rows,
    )
    connection.executemany(
        "INSERT INTO senses(id, word_id, definition, example, sense_order) VALUES (?, ?, ?, ?, ?)",
        sense_rows,
    )
    connection.executemany(
        "INSERT INTO relations(word_id, type, target) VALUES (?, ?, ?)",
        [(wid, kind, target) for (wid, kind, target) in relations],
    )
    connection.execute("CREATE VIRTUAL TABLE word_fts USING fts4(lemma)")
    connection.execute("INSERT INTO word_fts(lemma) SELECT DISTINCT lemma FROM words")
    connection.executemany(
        "INSERT INTO meta(key, value) VALUES (?, ?)",
        [
            ("schema_version", "1"),
            ("source", "wordnet-3.1+cmudict+frequencywords"),
            ("built", dt.date.today().isoformat()),
            ("words", str(len(word_rows))),
            ("senses", str(len(sense_rows))),
            ("relations", str(len(relations))),
        ],
    )
    connection.commit()
    connection.execute("VACUUM")
    connection.close()

    size_mb = os.path.getsize(args.out) / (1024 * 1024)
    print(
        f"wrote {args.out}: {len(word_rows)} words, {len(sense_rows)} senses, "
        f"{len(relations)} relations, {size_mb:.1f} MB",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
