# OpenCult Agent Governance (`AGENTS.md`)

> Precedence: Global user `AGENTS.md` is highest. Inside this repository, document order is:
> `AGENTS.md` > `TRD.md` > `PRD.md` > `INVARIANTS.md` > `DRD.md` > approved Open Design prototype.

---

## 0. Honesty Contract and Anti-Sophistry Rules

AI sophistry is banned in code reviews, status updates, commits, documentation, and handoffs. Sophistry includes:

1. Adjectives standing in for evidence (`robust`, `seamless`, `comprehensive`, `elegant`, `enterprise-grade`, `production-ready`, `meticulous`, `holistic`, `pivotal`, `crucial`).
2. Confidence laundering (`should work`, `likely fine`, `probably resolves it`, `this ensures`, `handles all edge cases`).
3. Claiming a command, Gradle task, BLE test, Health Connect write, or Supabase check ran when it did not.
4. Reporting a task complete when any part failed, was skipped, or was not observed.
5. Describing what code "should do" instead of quoting what the execution output showed.
6. Agreeing with a wrong premise to avoid friction.

Mandatory rules for every agent working in OpenCult:

1. **Never fabricate.** Zero invented exit codes, terminal output, file paths, BLE packet captures, timestamps, or API responses.
2. **Evidence before completion.** Quote the exact command, its exit code, and its observed output.
3. **Always state all three boundaries:** `Verified: ...`, `Unverified: ...`, `Unknown: ...`. Omitting `Unverified` or `Unknown` is a lie by omission.
4. **Say "I don't know"** when a root cause or BLE byte meaning is undetermined, then inspect or name the blocker.
5. **Read back every edit.** A successful file write is not proof of content; inspect the written lines or run a verification check.
6. **Report every change clearly:** Target boundary (path + lines), Prior behavior, Current behavior, Downstream impact, Unverified scope.
7. **Banned prose words and punctuation:** Never emit em dashes, en dashes, curly quotes, `forensic`, `delve`, `tapestry`, `testament`, `underscore`, `pivotal`, `crucial`, `vital`, `paramount`, `meticulous`, `intricate`, `multifaceted`, `comprehensive`, `holistic`, `synergy`, `paradigm`, `landscape`, `realm`, `beacon`, `catalyst`, `nuanced`, `robust`, `seamless`, `bespoke`, `unwavering`, `unleash`, `unlock`, `harness`, `showcase`, `garner`, `bolster`, `foster`, `cultivate`, `elevate`, `embark`, `navigate`, `resonate`, `vibrant`, `profound`, `enduring`, `indelible`, `utilize`, `leverage`, `facilitate`, `elucidate`, `spearhead`, `moreover`, `furthermore`, `additionally`, `notably`, `importantly`, `arguably`, `fundamentally`, `essentially`, `verdict`, `stands as`, `serves as`, `functions as`, `operates as`, `boasts`, `features` (as verb or filler noun), `plays a key role`, "not X, but Y" contrasts, or shallow `-ing` riders. Load the `humanizer` skill before drafting human-facing prose.

---

## 1. Strict UI Governance (Zero UI Violation Policy)

Every screen, card, chart, bottom dock, and animation in OpenCult must match `DRD.md` and the approved Open Design prototype. Visual drift is a build defect.

1. **No unstyled Material 3 defaults.** Never drop default Material `NavigationBar`, default `Card`, or stock `FloatingActionButton` where `DRD.md` defines the Detached Obsidian Pill Dock (`#0F1115`), Cobalt Action Button (`#2185F7`), or soft squircle bento cards (`24dp` / `28dp`).
2. **Token exclusivity (`INV-10`).** Never hardcode `Color(0x...)`, font sizes, or corner radii inside screen composables. Every visual value must come from `OpenCultTheme` tokens defined in `core/designsystem`.
3. **Custom Canvas fidelity (`INV-11`).** Do not replace custom `Canvas` / `DrawScope` charts with generic library bars or lines:
   - `DRD:C-01` Hero Card: 9-column vertical rounded capsule spark-bars (`#263238` normal, `#EF4444` spike).
   - `DRD:C-02` Overview Bento Grid: tall Lavender wave card + Sage top-right + Peach bottom-right cards with `<` / `>` pager controls.
   - `DRD:C-03` Reports Arc Gauge: 180-degree 3-zone segmented arc (`#2185F7`, `#F4B428`, `#EF4444`) with 4-degree gaps and ringed circular thumb.
   - `DRD:C-04` Stippled Dot-Matrix Bar Chart: unselected bars drawn with deterministic dot-matrix pattern; active bar solid `#111111` (Light) / `#F4F6FA` (Dark) with top white dot and floating mint status pill.
   - `DRD:C-06` 8-Electrode 5-Segment Anatomical Body Map: interactive Left Arm, Right Arm, Trunk, Left Leg, Right Leg regions.
4. **Tabular numerals (`INV-12`).** All weight, impedance, heart rate, and body metric numbers must use `fontFeatureSettings = "tnum"`.
5. **Dual theme parity.** Daylight Clinical Pastel (default) and Obsidian Night Mode must both pass Compose screenshot/layout checks with zero clipped labels and proper dock inset clearance (`INV-13`).

---

## 2. Second Brain Memory Protocol (`project="opencult"`)

Whenever `second-brain` MCP tools are connected, follow this protocol on every work session:

1. **Recall Gate (before first edit or plan):**
   - `list_recent(n=5)`
   - `list_projects()` (use exact slug `opencult`)
   - `recall(query, project="opencult", topK=8)`
   - `recall(query, tag="failure", hops=1)` for the subsystem being modified
   - `brief(project="opencult")`
   - **Truncation guard:** If any result has `truncated: true`, call `get(id)` before quoting or acting on it.
2. **Write Gate (during and end of work):**
   - Always `recall` before writing to avoid duplicates. Use `append` for ongoing threads, `update` for corrections, and `remember` for genuinely new facts or decisions.
   - Maintain `capsule:project:opencult` (`current-state`, `decisions`, `open-questions`) with `status:canonical` only after verification.
   - End every session with one structured `handoff` memory tagged `["handoff", "session", "opencult"]` and linked via `part_of_project` to the canonical project entry, listing files touched, commands with exit codes, failures, Verified/Unverified/Unknown boundaries, and the next concrete action.
3. **Outage rule:** If `second-brain` is disconnected or returns a Cloudflare Access `302`, report it plainly once and continue working without fabricating recall or handoff claims.

---

## 3. Reverse-Engineering Reference Rules (`Both/`) and GPL-3.0 Compliance

1. Local reference repositories live under `Both/` (`Both/occult` [MIT] and `Both/openscale` [GPL-3.0-or-later]).
2. `Both/` MUST remain listed in `.gitignore` and MUST NEVER be committed to the `OpenCult` repository.
3. OpenCult is licensed under `GPL-3.0-or-later` (see `LICENSE`). Whenever adapting protocol logic or BIA algorithms from `Both/openscale` (`CultSmartScaleProHandler`, `BodyPod2Handler`, `OneByoneHandler`, `StandardImpedanceLib`, `S400BodyComposition`, `Wla25BodyComposition`), retain GPL-3.0 headers and explicit upstream attribution.
4. Never fabricate BLE packet layouts or claim a `0xDF` byte field is decoded unless backed by an actual captured `btsnoop_hci.log` and unit test fixture (`INV-01`, `INV-02`).
5. Nominative trademark notice: OpenCult is an independent open-source interoperability project and is not affiliated with, endorsed by, or sponsored by Curefit Healthcare Pvt. Ltd., Cult.fit, or Cultsport.

---

## 4. Verification, Security, and Git Discipline

1. **Build and test checks:** After any Android code change, run Gradle assemble, Kotlin unit tests (BLE parser + BIA math + Health Connect mapper), and Compose UI tests; after any schema edit, run `supabase db push --dry-run` or `supabase migration list`. Quote exact exit codes.
2. **Secret hygiene:** Never commit `.env`, `.env.local`, `local.properties`, keystores, database passwords, or `service_role` keys. The Android client uses only `OPENCULT_SUPABASE_URL` and the public `anon` key paired with user JWTs under strict PostgreSQL RLS (`INV-08`).
3. **Git attribution:** All commits belong exclusively to `Souvik Dey <imsovikde@gmail.com>`. Zero `Co-Authored-By`, zero `Generated with`, zero agent trailers. Never pass `--no-gpg-sign` (`commit.gpgsign=true`). Never force-push `main`.
