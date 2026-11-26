#!/usr/bin/env python3
"""Deterministic semantic version calculator and Gradle updater for OpenCult."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

SEMVER_RE = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)$")
VCODE_RE = re.compile(r"(versionCode\s*=\s*)(\d+)")
VNAME_RE = re.compile(r'(versionName\s*=\s*")([^"]+)(")')


def run_git(args: list[str], cwd: Path) -> str:
    res = subprocess.run(["git", *args], cwd=cwd, text=True, capture_output=True, check=False)
    if res.returncode != 0:
        return ""
    return res.stdout.strip()


def compute_next_version(current_name: str, current_code: int, commits: list[str]) -> tuple[str, int, str]:
    match = SEMVER_RE.match(current_name.strip())
    if not match:
        raise ValueError(f"Unsupported versionName format: {current_name!r}")
    major, minor, patch = int(match.group(1)), int(match.group(2)), int(match.group(3))

    bump = "patch"
    for msg in commits:
        lower = msg.lower()
        if "breaking change" in lower or re.match(r"^[a-z]+(\([^)]+\))?!:", msg.splitlines()[0].strip().lower()):
            bump = "major"
            break
        if re.match(r"^feat(\([^)]+\))?:", msg.splitlines()[0].strip().lower()):
            if bump != "major":
                bump = "minor"

    if bump == "major":
        next_name = f"{major + 1}.0.0"
    elif bump == "minor":
        next_name = f"{major}.{minor + 1}.0"
    else:
        next_name = f"{major}.{minor}.{patch + 1}"

    return next_name, current_code + 1, bump


def main() -> int:
    parser = argparse.ArgumentParser(description="Compute or apply OpenCult Android version bump.")
    parser.add_argument("--gradle-file", default="app/build.gradle.kts", help="Path to app/build.gradle.kts")
    parser.add_argument("--apply", action="store_true", help="Write updated versionCode/versionName to Gradle file")
    args = parser.parse_args()

    repo_root = Path.cwd()
    gradle_path = repo_root / args.gradle_file
    content = gradle_path.read_text(encoding="utf-8")

    code_match = VCODE_RE.search(content)
    name_match = VNAME_RE.search(content)
    if not code_match or not name_match:
        print("Unable to locate versionCode/versionName in Gradle file", file=sys.stderr)
        return 1

    current_code = int(code_match.group(2))
    current_name = name_match.group(2)

    last_tag = run_git(["describe", "--tags", "--abbrev=0"], repo_root)
    log_range = f"{last_tag}..HEAD" if last_tag else "HEAD"
    raw_log = run_git(["log", log_range, "--format=%B%x1e"], repo_root)
    commits = [c.strip() for c in raw_log.split("\x1e") if c.strip()]

    next_name, next_code, bump = compute_next_version(current_name, current_code, commits)

    if args.apply:
        updated = VCODE_RE.sub(lambda m: f"{m.group(1)}{next_code}", content, count=1)
        updated = VNAME_RE.sub(lambda m: f'{m.group(1)}{next_name}{m.group(3)}', updated, count=1)
        gradle_path.write_text(updated, encoding="utf-8")

    payload = {
        "previous_version_name": current_name,
        "previous_version_code": current_code,
        "next_version_name": next_name,
        "next_version_code": next_code,
        "bump": bump,
        "commit_count": len(commits),
        "applied": args.apply,
    }
    print(json.dumps(payload, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
