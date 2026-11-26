#!/usr/bin/env python3
"""Generate clean Markdown release notes for OpenCult from git history."""

from __future__ import annotations

import argparse
import re
import subprocess
from pathlib import Path

BANNED_PUNCT = ("\u2014", "\u2013", "\u201c", "\u201d", "\u2018", "\u2019")


def git_commits(since_tag: str | None, cwd: Path) -> list[tuple[str, str]]:
    rev = f"{since_tag}..HEAD" if since_tag else "HEAD"
    res = subprocess.run(
        ["git", "log", rev, "--no-merges", "--format=%h%x09%s"],
        cwd=cwd,
        text=True,
        capture_output=True,
        check=False,
    )
    if res.returncode != 0:
        return []
    rows: list[tuple[str, str]] = []
    for line in res.stdout.splitlines():
        if "\t" not in line:
            continue
        sha, subject = line.split("\t", 1)
        clean = subject.strip()
        for ch in BANNED_PUNCT:
            clean = clean.replace(ch, "-")
        rows.append((sha.strip(), clean))
    return rows


def render_notes(version: str, commits: list[tuple[str, str]]) -> str:
    added: list[str] = []
    fixed: list[str] = []
    hardened: list[str] = []
    other: list[str] = []

    for sha, msg in commits:
        lower = msg.lower()
        entry = f"- {msg} (`{sha}`)"
        if re.match(r"^feat(\([^)]+\))?:", lower):
            added.append(entry)
        elif re.match(r"^fix(\([^)]+\))?:", lower):
            fixed.append(entry)
        elif re.match(r"^(sec|security|ci|build|chore|docs)(\([^)]+\))?:", lower):
            hardened.append(entry)
        else:
            other.append(entry)

    lines = [
        f"## OpenCult {version}",
        "",
        "Release binary built from verified Gradle unit tests, Release Lint checks, and R8 configuration analysis.",
        "",
    ]
    for title, items in (
        ("Capabilities", added),
        ("Fixes", fixed),
        ("Security and Build Hardening", hardened),
        ("Additional Changes", other),
    ):
        if items:
            lines.append(f"### {title}")
            lines.extend(items)
            lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate release notes for OpenCult.")
    parser.add_argument("--version", required=True, help="Release version label (e.g. v0.2.0)")
    parser.add_argument("--since-tag", default=None, help="Previous git tag boundary")
    parser.add_argument("--output", default=None, help="Optional Markdown file output path")
    args = parser.parse_args()

    cwd = Path.cwd()
    since = args.since_tag
    if not since:
        res = subprocess.run(["git", "describe", "--tags", "--abbrev=0"], cwd=cwd, text=True, capture_output=True, check=False)
        if res.returncode == 0:
            since = res.stdout.strip()

    notes = render_notes(args.version, git_commits(since, cwd))
    if args.output:
        Path(args.output).write_text(notes, encoding="utf-8")
    else:
        print(notes, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
