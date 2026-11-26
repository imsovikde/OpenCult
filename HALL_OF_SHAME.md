# OpenCult Agent Regression Ledger (`HALL_OF_SHAME.md`)

> **Living Document.** Every agent MUST read this file before modifying any source, prototype, schema, or documentation file.
> When any bug, regression, UI drift, motion hitch, BLE parse error, Health Connect conflict, or sync failure is introduced or discovered, the fixing agent MUST append an entry here AND tighten `INVARIANTS.md` (plus `MICROENGINEERING.md` or `MOTIONENGINEERING.md` for UI/motion issues) in the SAME commit. Skipping the update is a defect.
> Never delete or rewrite old rows. Append new entries at the bottom.

---

## Update Protocol

1. **When to update:** Every time a bug, hardware edge case, UI violation, motion glitch, or regression occurs, whether found by an agent, by manual testing on real scales, by UI inspection, or by automated checks.
2. **Same-commit atomicity:** The `HALL_OF_SHAME.md` entry, the corresponding update to `INVARIANTS.md` (and `MICROENGINEERING.md` / `MOTIONENGINEERING.md` when UI or motion changed), and the regression test MUST land together in one commit (`INV-23`).
3. **Mechanistic honesty:** State the exact root cause and observed failure without euphemisms. If an agent introduced the regression, record it plainly. If the root cause is still unknown, write `unknown - needs investigation` until verified.
4. **Zero repeat rule:** Before touching any component or parser that appears below, re-read its row and verify its guard remains intact. Repeating a mistake already logged in this table is a critical defect.

---

## Entry Schema

```markdown
| SHAME-NNN | Short defect name | Mechanistic root cause (1 line) | Primary file path | INV-XX / Rule | Concrete fix and verification | YYYY-MM-DD |
```

---

## The Ledger

| ID | Bug / Regression | Root Cause | File | INV / Rule | Fix | Date |
|---|---|---|---|---|---|---|
| `SHAME-001` | v1 prototype used iPhone hardware shell and single-screen dead end | Prototype v1 rendered `.dynamic-island` and `.home-indicator` iOS chrome for an Android-only Health Connect app and booted into a single `onboarding` screen hiding 8 required views | `prototype/index.html` (v1) | `INV-17` | Rebuilt v2 shell with `data-platform="android"`, punch-hole cutout, system gesture pill, desktop Studio Bar (`Gallery` / `1:1 Phone`), and interactive `.screen-rail` across all 9 flows | 2026-11-26 |
| `SHAME-002` | v1 prototype Canvas drift and card over-stuffing | Arc gauge drew flat `stroke-linecap="butt"` ends instead of rounded caps; Hero/Bento cards embedded extra metric rows and Reports stippled bar card included an unrequested 3-column summary footer | `prototype/app.js`, `prototype/styles.css` (v1) | `INV-11`, `INV-18` | Updated v2 arc paths to `stroke-linecap="round"` (`app.js:631`) and trimmed Hero, Bento, and Stippled card clutter to match `DRD.md` whitespace and layout budget | 2026-11-26 |
| `SHAME-003` | v1 prototype hardcoded 8-electrode scale assumptions only | UI assumed Cult Smart Scale Pro (8E) across every card and modal with no switcher, handshake feed, or provenance separation for the user's 4-electrode `CS-BF01` scale | `prototype/index.html`, `prototype/app.js` (v1) | `INV-02`, `INV-14`, `INV-18` | Added live `scaleMode` switcher (`pro`, `bf01`, `proFeet`), 4E `FD 37`/`F1`/`F2 00` handshake log (`app.js:1346-1347`), provenance subtitles, and aggregate-only 4E HC sync labeling | 2026-11-26 |
| `SHAME-004` | Upstream 8E parser discarded 9 of 10 impedance channels | `CultSmartScaleProHandler` read only `imp[2]` from packet `0x00` of `0xBE` and ignored `imp[0..1,3..4]` and packet `0x01`, blocking dual-frequency segmental and ECW/ICW computation | `Both/openscale/.../CultSmartScaleProHandler.kt` (`L206-L236`) | `INV-01`, `INV-02` | Enforced full 10-channel `0xBE` decoding + immutable `raw_ble_packets` storage (`INV-01`) in `TRD.md` and modeled all 10 ohm channels (`CH1..CH10`) in v2 BLE Inspector (`app.js:1251`) | 2026-11-26 |
| `SHAME-005` | Coalesced 20-byte duplicate `0xCF` misparsed as `OneByone` history | 20-byte ATT MTU coalesces an 11-byte final live frame with 9 bytes of its duplicate repeat; length `>= 18` naive check parsed `bytes[11..17]` as history timestamps producing year `53138` and dropped weigh-ins | `Both/openscale/.../OneByoneHandler.kt` (`L466-L484`) | `INV-01`, `INV-14`, `INV-21` | Mandated `hasHistoryTimestamp` frame header + XOR checksum + byte-11 second-frame marker guard (`INV-21`) in `TRD.md` and Android BLE router tests | 2026-11-26 |
| `SHAME-006` | v2 prototype gauge palette, gap angle, and stipple stagger drifted from `DRD.md` | `prototype/app.js:584` and `prototype/index.html:646-648` used `['#3B82F6', '#34C77B', '#F4B428']` (`Low/Normal/High`) with `gap = 8` (`app.js:620`) instead of `DRD.md` `#2185F7`/`#F4B428`/`#EF4444` with `4 deg` gaps; `styles.css:1164` used orthogonal `6px` radial dots instead of odd-row stagger | `prototype/app.js` (`L584, L620`), `prototype/index.html` (`L646-L648`), `prototype/styles.css` (`L1164`) | `INV-10`, `INV-11`, `INV-22`, `MICROENGINEERING.md §5` | Locked `INV-22` binding native Android Compose builds to `DRD.md` canonical zone tokens (`#2185F7`/`#F4B428`/`#EF4444`), `4 deg` gaps, and odd-row staggered Canvas stipples until prototype is aligned | 2026-11-26 |
| `SHAME-007` | Headless PowerShell `&&`, OpenPGP prompt hang, and AI-prose banlist leaks | Command chaining with `&&` failed under PowerShell 5.1; global OpenPGP hung non-interactive git commits on GUI `pinentry-w32`; initial v2 prompt draft emitted two banned buzzwords caught by the automated audit scan | `prompts/04_OPEN_DESIGN_PROTOTYPE_V2_PROMPT.md`, git commit tooling | `INV-15`, `AGENTS.md §0, §4` | Switched PowerShell chaining to `;` with `$LASTEXITCODE`, pinned repo-local SSH signing (`gpg.format=ssh`), and gated every Markdown commit behind automated banned-word/punctuation/secret regex verification | 2026-11-26 |
