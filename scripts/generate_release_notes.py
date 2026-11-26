#!/usr/bin/env python3
"""Generate verified, human-readable Markdown release notes and SHA-256 digests for OpenCult."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import re
import subprocess
from pathlib import Path

BANNED_PUNCT = ("\u2014", "\u2013", "\u201c", "\u201d", "\u2018", "\u2019")
VCODE_RE = re.compile(r"versionCode\s*=\s*(\d+)")
VNAME_RE = re.compile(r'versionName\s*=\s*"([^"]+)"')
MIN_SDK_RE = re.compile(r"minSdk\s*=\s*(\d+)")
TARGET_SDK_RE = re.compile(r"targetSdk\s*=\s*(\d+)")


def sanitize_prose(text: str) -> str:
    cleaned = text.strip()
    for ch in BANNED_PUNCT:
        replacement = '"' if ch in ("\u201c", "\u201d") else ("'" if ch in ("\u2018", "\u2019") else "-")
        cleaned = cleaned.replace(ch, replacement)
    return cleaned


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
        rows.append((sha.strip(), sanitize_prose(subject)))
    return rows


def read_gradle_meta(cwd: Path) -> dict[str, str]:
    gradle_path = cwd / "app" / "build.gradle.kts"
    if not gradle_path.exists():
        return {}
    text = gradle_path.read_text(encoding="utf-8")
    meta: dict[str, str] = {}
    for key, regex in (
        ("versionCode", VCODE_RE),
        ("versionName", VNAME_RE),
        ("minSdk", MIN_SDK_RE),
        ("targetSdk", TARGET_SDK_RE),
    ):
        match = regex.search(text)
        if match:
            meta[key] = match.group(1)
    return meta


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def bucket_commits(commits: list[tuple[str, str]]) -> list[tuple[str, list[str]]]:
    buckets: dict[str, list[str]] = {
        "BLE protocol and BIA math": [],
        "Health Connect and cloud sync": [],
        "UI screens, charts, and filtering": [],
        "Bug fixes and regression hardening": [],
        "Security, CI, and repository tooling": [],
        "Other changes": [],
    }
    for sha, msg in commits:
        lower = msg.lower()
        entry = f"- {msg} (`{sha}`)"
        if any(k in lower for k in ("ble", "bia", "cole", "compartment", "packet", "0xbe", "0xcf", "cs-bf01")):
            buckets["BLE protocol and BIA math"].append(entry)
        elif any(k in lower for k in ("health connect", "healthconnect", "supabase", "rls", "outbox", "sync")):
            buckets["Health Connect and cloud sync"].append(entry)
        elif any(k in lower for k in ("ui", "screen", "chart", "bodymap", "detail", "report", "filter", "dock")):
            buckets["UI screens, charts, and filtering"].append(entry)
        elif re.match(r"^fix(\([^)]+\))?:", lower) or "shame-" in lower:
            buckets["Bug fixes and regression hardening"].append(entry)
        elif re.match(r"^(sec|security|ci|build|chore|docs|release)(\([^)]+\))?:", lower):
            buckets["Security, CI, and repository tooling"].append(entry)
        else:
            buckets["Other changes"].append(entry)
    return [(title, items) for title, items in buckets.items() if items]


def render_notes(
    version: str,
    commits: list[tuple[str, str]],
    gradle_meta: dict[str, str],
    artifact_rows: list[tuple[str, int, str]],
    since_tag: str | None,
) -> str:
    vcode = gradle_meta.get("versionCode", "unknown")
    vname = gradle_meta.get("versionName", version.lstrip("v"))
    min_sdk = gradle_meta.get("minSdk", "31")
    target_sdk = gradle_meta.get("targetSdk", "36")
    built_utc = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    range_label = f"`{since_tag}..{version}`" if since_tag else f"`initial..{version}` ({len(commits)} commits)"

    lines: list[str] = [
        f"# OpenCult {version}",
        "",
        f"Android open-source companion release for Cult.fit smart scales, version `{vname}` (build `{vcode}`), compiled for Android {min_sdk}+ (target SDK {target_sdk}) at {built_utc}.",
        "",
        "## Hardware and metric coverage",
        "",
        "| Scale Tier | Captured BLE Frames | Biometric Output | Health Connect Writes |",
        "|---|---|---|---|",
        "| Cult Smart Scale Pro (8E handles) | `0xCF`, dual-band `0xBE` Sets A+B (10 ch at 20/100 kHz), `4 x 0xDF` | All 56 parameters (`M-01..M-56`), Cole-Cole bisection `R0`/`Rinf`, De Lorenzo Eq. B2 ECW/ICW, 5-part segmental muscle/fat | Weight, Body Fat %, Lean Mass, Bone Mass, Water Mass, BMR, Heart Rate, Height |",
        "| Cult Smart Body Fat Scale (CS-BF01, 4E) | `FD 37 -> F1 -> F2 00` handshake, `0xCF` HR + 50 kHz LE impedance | Core weight, heart rate, 50 kHz BIA body composition, segmental cards locked | Weight, Body Fat %, Lean Mass, Bone Mass, Water Mass, BMR, Heart Rate, Height |",
        "| Pro Feet-Only (No handles) | `0xCF` weight-only lock | Weight, BMI, and anthropometric estimates only; BIA compartments suppressed | Weight, Height (and Heart Rate when present) |",
        "",
        "## Engineering highlights in this release",
        "",
        "- Two-band Cole-Cole bisection (`20 kHz` / `100 kHz`) and De Lorenzo Eq. B2 water partitioning paired with 4-compartment mineral/protein conservation (`Weight = Fat + TBW + Protein + Mineral`).",
        "- Health Connect alignment matching Android's official Health Connect codelab: `VIEW_PERMISSION_USAGE` and `ACTION_SHOW_PERMISSIONS_RATIONALE` handling, `HealthConnectFeatures` runtime gating for History/Background scopes, and a 30-day `ReadRecordsRequest<WeightRecord>` catch-up on expired changes tokens.",
        "- Full UI visibility and multi-axis filtering: 5-segment Body Map `Muscle` vs `Fat` layer switch, Measurement Detail `All 56` vs `Active Only` parameter filter with direct `Sync Health Connect`, separated `Sim Pro 8E` / `Sim CS-BF01` / `Sim Feet-Only` QA buttons, and scrollable searchable 55-metric Reports selector.",
        "- Multi-member sync reliability: direct primary-key lookup (`RawBlePacketDao.getById`) for all 10 household profiles, RLS-scoped Supabase push+pull sync on sign-in, and least-privilege Android manifest (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` with zero location or advertising permissions).",
        "",
        f"## Commits included ({range_label})",
        "",
    ]

    for title, items in bucket_commits(commits):
        lines.append(f"### {title}")
        lines.extend(items)
        lines.append("")

    if artifact_rows:
        lines.extend([
            "## Release artifacts and SHA-256 verification",
            "",
            "| File | Size | SHA-256 |",
            "|---|---|---|",
        ])
        for name, size_bytes, digest in artifact_rows:
            size_mb = size_bytes / (1024.0 * 1024.0)
            lines.append(f"| `{name}` | {size_mb:.2f} MB ({size_bytes} bytes) | `{digest}` |")
        lines.extend([
            "",
            "Verify the downloaded APK before installing:",
            "",
            "```bash",
            "sha256sum -c SHA256SUMS.txt",
            "```",
            "",
        ])

    lines.extend([
        "## Installation notes",
        "",
        "1. Download `opencult-" + version + "-release.apk` from the Assets section below (recommended for direct Android sideload).",
        "2. Enable installation from unknown sources on Android 12+ and confirm the SHA-256 digest above matches the release signature.",
        "3. `app-release.aab` is included alongside the APK for Play App Bundle verification.",
        "",
        "OpenCult is an independent GPL-3.0-or-later project and is not affiliated with Curefit Healthcare Pvt. Ltd., Cult.fit, or Cultsport.",
    ])

    return sanitize_prose("\n".join(lines)).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate release notes and checksums for OpenCult.")
    parser.add_argument("--version", required=True, help="Release version label (e.g. v0.2.0)")
    parser.add_argument("--since-tag", default=None, help="Previous git tag boundary")
    parser.add_argument("--output", default=None, help="Optional Markdown file output path")
    parser.add_argument("--artifacts", nargs="*", default=[], help="Release artifact files to hash")
    parser.add_argument("--sha256-out", default=None, help="Optional SHA256SUMS.txt output path")
    args = parser.parse_args()

    cwd = Path.cwd()
    since = args.since_tag
    if not since:
        res = subprocess.run(["git", "describe", "--tags", "--abbrev=0"], cwd=cwd, text=True, capture_output=True, check=False)
        if res.returncode == 0:
            candidate = res.stdout.strip()
            if candidate != args.version:
                since = candidate

    artifact_rows: list[tuple[str, int, str]] = []
    sha_lines: list[str] = []
    for raw_path in args.artifacts:
        p = Path(raw_path)
        if p.is_file():
            digest = sha256_file(p)
            artifact_rows.append((p.name, p.stat().st_size, digest))
            sha_lines.append(f"{digest}  {p.name}")

    if args.sha256_out and sha_lines:
        Path(args.sha256_out).write_text("\n".join(sha_lines) + "\n", encoding="utf-8")

    notes = render_notes(
        version=args.version,
        commits=git_commits(since, cwd),
        gradle_meta=read_gradle_meta(cwd),
        artifact_rows=artifact_rows,
        since_tag=since,
    )
    if args.output:
        Path(args.output).write_text(notes, encoding="utf-8")
    else:
        print(notes, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
