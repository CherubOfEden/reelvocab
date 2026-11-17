#!/usr/bin/env python3
"""Fail the build if the bundled dictionary is missing or malformed."""

from __future__ import annotations

import os
import sqlite3
import sys

DB = "core/data/src/main/assets/dict.db"
MIN_WORDS = 20000
MIN_SENSES = 40000
MIN_SYNONYMS = 40000
MIN_ANTONYMS = 1500


def main() -> int:
    if not os.path.exists(DB):
        print(f"missing {DB}", file=sys.stderr)
        return 1

    size_mb = os.path.getsize(DB) / (1024 * 1024)
    connection = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)
    errors: list[str] = []

    def scalar(sql: str, *args: object) -> object:
        row = connection.execute(sql, args).fetchone()
        return row[0] if row else None

    tables = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type IN ('table','view')"
        )
    }
    for table in ("meta", "words", "senses", "relations", "word_fts"):
        if table not in tables:
            errors.append(f"missing table {table}")

    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    meta = dict(connection.execute("SELECT key, value FROM meta"))
    if meta.get("schema_version") != "1":
        errors.append(f"unexpected schema_version {meta.get('schema_version')!r}")

    words = scalar("SELECT COUNT(*) FROM words") or 0
    senses = scalar("SELECT COUNT(*) FROM senses") or 0
    synonyms = scalar("SELECT COUNT(*) FROM relations WHERE type = 'syn'") or 0
    antonyms = scalar("SELECT COUNT(*) FROM relations WHERE type = 'ant'") or 0

    if words < MIN_WORDS:
        errors.append(f"words {words} < {MIN_WORDS}")
    if senses < MIN_SENSES:
        errors.append(f"senses {senses} < {MIN_SENSES}")
    if synonyms < MIN_SYNONYMS:
        errors.append(f"synonyms {synonyms} < {MIN_SYNONYMS}")
    if antonyms < MIN_ANTONYMS:
        errors.append(f"antonyms {antonyms} < {MIN_ANTONYMS}")

    ipa = scalar("SELECT ipa FROM words WHERE lemma = 'grit'")
    if not ipa or not ipa.startswith("\u0261"):
        errors.append(f"grit IPA missing or wrong: {ipa!r}")

    grit_senses = scalar(
        "SELECT COUNT(*) FROM senses s JOIN words w ON w.id = s.word_id WHERE w.lemma = 'grit'"
    ) or 0
    if grit_senses < 2:
        errors.append(f"expected at least two senses for grit, got {grit_senses}")

    happy_ants = scalar(
        """
        SELECT COUNT(*) FROM relations r
        JOIN words w ON w.id = r.word_id
        WHERE w.lemma = 'happy' AND r.type = 'ant'
        """
    ) or 0
    if happy_ants < 1:
        errors.append("no antonyms for happy")

    good_ants = {
        row[0]
        for row in connection.execute(
            """
            SELECT r.target FROM relations r
            JOIN words w ON w.id = r.word_id
            WHERE w.lemma = 'good' AND r.type = 'ant'
            """
        )
    }
    if not good_ants:
        errors.append("no antonyms for good")

    fts = {
        row[0]
        for row in connection.execute(
            "SELECT lemma FROM word_fts WHERE lemma MATCH 'gri*'"
        )
    }
    if "grit" not in fts:
        errors.append(f"FTS prefix search missed grit: {sorted(fts)[:5]}")

    connection.close()

    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    print(
        f"dict.db ok ({size_mb:.1f} MB): {words} words, {senses} senses, "
        f"{synonyms} synonyms, {antonyms} antonyms"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
