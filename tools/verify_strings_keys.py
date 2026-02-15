#!/usr/bin/env python3
"""Verify Android string resources parity across locales.

Checks:
1. Every localized strings.xml has the exact same key set as values/strings.xml.
2. Placeholders in localized values match the baseline placeholders for each key.
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

RES_DIR = Path("app/src/main/res")
BASELINE_FILE = RES_DIR / "values" / "strings.xml"
PLACEHOLDER_RE = re.compile(r"%(?:\d+\$)?[ds]")


def load_strings(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    root = tree.getroot()
    values: dict[str, str] = {}
    for node in root.findall("string"):
        key = node.attrib.get("name")
        if not key:
            continue
        values[key] = node.text or ""
    return values


def placeholder_multiset(text: str) -> Counter[str]:
    return Counter(PLACEHOLDER_RE.findall(text))


def main() -> int:
    if not BASELINE_FILE.exists():
        print(f"ERROR: Baseline file not found: {BASELINE_FILE}")
        return 1

    baseline = load_strings(BASELINE_FILE)
    baseline_keys = set(baseline.keys())
    errors: list[str] = []

    locale_dirs = sorted(
        p for p in RES_DIR.glob("values*") if p.is_dir() and p.name != "values"
    )

    for locale_dir in locale_dirs:
        file_path = locale_dir / "strings.xml"
        if not file_path.exists():
            errors.append(f"{locale_dir.name}: missing strings.xml")
            continue

        localized = load_strings(file_path)
        localized_keys = set(localized.keys())

        missing = sorted(baseline_keys - localized_keys)
        extra = sorted(localized_keys - baseline_keys)

        if missing:
            errors.append(
                f"{locale_dir.name}: missing {len(missing)} keys -> {', '.join(missing)}"
            )
        if extra:
            errors.append(
                f"{locale_dir.name}: extra {len(extra)} keys -> {', '.join(extra)}"
            )

        for key in sorted(baseline_keys & localized_keys):
            expected = placeholder_multiset(baseline[key])
            actual = placeholder_multiset(localized[key])
            if expected != actual:
                errors.append(
                    f"{locale_dir.name}: placeholder mismatch for '{key}' "
                    f"(expected {dict(expected)}, got {dict(actual)})"
                )

    if errors:
        print("String parity check failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print(f"String parity check passed for {len(locale_dirs)} locale folders.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
