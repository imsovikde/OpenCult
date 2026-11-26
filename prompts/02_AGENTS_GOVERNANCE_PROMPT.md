# Prompt to Author or Audit OpenCult `AGENTS.md`

Copy and paste this prompt when initializing or auditing governance rules for any coding agent working on **OpenCult**.

---

```text
You are writing or auditing `AGENTS.md` at the root of the **OpenCult** repository, an open-source (GPL-3.0) native Android Kotlin + Jetpack Compose companion for the Cult Smart Scale Pro (8-electrode, 56 parameters) and Cult Smart Body Fat Scale CS-BF01 (4-electrode, 20+ parameters), with two-way Google Health Connect sync and Supabase Realtime sync.

Before writing `AGENTS.md`, read the global user `AGENTS.md` (and `DRD.md`, `PRD.md`, `TRD.md`, `INVARIANTS.md` if present). Your generated `AGENTS.md` must enforce all of the following without ambiguity:

1. **Honesty Contract and Anti-Sophistry Rules (Section 0):**
   - Ban fabricated exit codes, command logs, BLE captures, timestamps, or API outputs.
   - Require every report to state `Verified`, `Unverified`, and `Unknown` boundaries explicitly.
   - Ban confidence laundering ("should work", "probably fixes it") and filler adjectives standing in for test evidence ("robust", "seamless", "comprehensive", "enterprise-grade", "pivotal", "crucial").
   - Enforce Souvik Dey's writing banlist: zero em dashes, zero en dashes, zero curly quotes, zero "not X, but Y" contrasts, zero shallow "-ing" riders, and mandatory `humanizer` skill compliance on all human-facing prose.
   - Require reading back edited files and reporting Target boundary, Prior behavior, Current behavior, Downstream impact, and Unverified scope.

2. **Document Precedence:**
   - Global user `AGENTS.md` > Root `AGENTS.md` > `TRD.md` > `PRD.md` > `INVARIANTS.md` > `DRD.md` > approved Open Design prototype.

3. **Strict UI Governance (Zero UI Violation Policy):**
   - Forbid unstyled Material 3 defaults where `DRD.md` defines custom components.
   - Forbid hardcoded hex colors, font sizes, or corner radii inside feature composables (`INV-10`); require `OpenCultTheme` tokens.
   - Require exact Compose `Canvas` / `DrawScope` implementation of the 9-column capsule spark-bars, Lavender Bezier wave bento card, 3-zone segmented semi-circular arc gauge, stippled dot-matrix bar chart with solid active bar, 8-electrode 5-segment anatomical body map, and detached `#0F1115` floating pill dock + `#2185F7` Measure FAB (`INV-11`).
   - Require `fontFeatureSettings = "tnum"` on all numeric telemetry (`INV-12`), floating dock bottom padding clearance (`INV-13`), and parity across Daylight Clinical Pastel and Obsidian Night themes.

4. **Second Brain Memory Protocol (`project="opencult"`):**
   - Mandate the 5-step Recall Gate (`list_recent`, `list_projects`, `recall`, `recall(tag="failure", hops=1)`, `brief`) and the truncation guard (`get(id)` whenever `truncated` is true) before the first edit or plan.
   - Mandate the Write Gate: recall before write, canonical capsule updates (`capsule:project:opencult`), and one structured `handoff` memory at session end linked via `part_of_project`.
   - Include the plain outage reporting rule if `second-brain` MCP is disconnected or gated by Cloudflare Access `302`.

5. **Reverse-Engineering Reference Hygiene and GPL-3.0 Attribution:**
   - Keep `Both/` (`Both/occult` and `Both/openscale`) local and permanently listed in `.gitignore`. Never commit `Both/`.
   - Preserve GPL-3.0 license headers and upstream credit whenever adapting `openScale` BLE or BIA code.
   - Ban claiming `0xDF` proprietary bytes are decoded without captured `btsnoop_hci.log` evidence and deterministic unit tests.
   - Require nominative trademark disclaimer clarity.

6. **Verification, Security, and Git Discipline:**
   - Require Gradle build, Kotlin unit tests, Compose UI checks, and `supabase db push --dry-run` with quoted exit codes.
   - Ban committing `.env*` (except `.env.example`), database passwords, keystores, or Supabase `service_role` keys.
   - Require all commits to be authored and GPG-signed solely by `Souvik Dey <imsovikde@gmail.com>`, with zero `Co-Authored-By` or AI trailers.
```
