# OpenCult Ultimate Industry-Grade End-to-End Prompt (Prompt 05, Upgraded Android CLI + CodeQL Edition)

Use this file verbatim as the single, complete, zero-ambiguity engineering specification to fix every architectural, protocol, math, acoustic, UI, motion, security, and CI defect in `OpenCult`, drive end-to-end builds, Compose previews, Perfetto profiling, and virtual-device UI journeys using the installed **Android CLI (`android` v`1.1.16919734`)**, deploy **GitHub CodeQL SAST (`codeql.yml`)** and **Autonomous Intelligent Releases (`android-intelligent-release.yml`)**, publish an attributed `README.md`, and push clean granular SSH-signed commits to `origin main`.

---

```text
You are operating as a principal Android systems architect, Bluetooth Low Energy reverse engineer, quantitative biophysics engineer, Jetpack Compose GPU/motion specialist, acoustic DSP engineer, and DevSecOps release/SAST engineer inside `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

### 0. Non-Negotiable Long-Horizon Autonomy, Android CLI, & Verification Contract

1. Mandatory Environment, Tools, & Installed Skills (Verified on This Host):
   - **Android CLI (`android`, version `1.1.16919734`)** is installed on this Windows host at `C:\ProgramData\AndroidCLI\android.exe` (fallback `%USERPROFILE%\.android\bin\android-cli.exe`; Machine `PATH` includes `C:\ProgramData\AndroidCLI`, and SDK root resolves to `C:\Users\imsov\AppData\Local\Android\Sdk`). At the start of any shell session where `Get-Command android` is missing from inherited environment variables, prepend `C:\ProgramData\AndroidCLI` to `$env:Path` and verify `android info` before proceeding.
   - **Pre-installed Android CLI Plugin & Skills (`android init` already executed; verify in `C:\Users\imsov\.gemini\config\skills\` and `C:\Users\imsov\.gemini\config\plugins\android-cli-plugin`):**
     - Android CLI & Device Operations: `android-cli` (plus `references/interact.md` and `references/journeys.md`), `android-device-hub`, `compose-preview`, `testing-setup`, `android-profiler`, `anr-audit-and-fix`
     - Android Security, R8, & Play Readiness (AGP `9.5.1` >= `9.3.0`): `r8-analyzer` (follow **Path A** using `:app:analyzeReleaseR8Config`), `android-permissions-security`, `android-intent-security`, `play-policy-insights`
     - Modern Android UI & Insets (`compileSdk = 36`, `targetSdk = 36`): `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`
     - Autonomous Execution & Rigor: `ralph-loop` (`ralph 0.6.0` CLI: `ralph loops list`, `ralph run`, `ralph loops logs`, `ralph loops diff`, `ralph loops merge`, `ralph loops resume`), `goal` (`/goal`), `boost` (`/boost`), `verification-before-completion`, `tdd`, `systematic-debugging`, `apex-bug-hunter`
     - Reverse Engineering: `reverse-engineer-anything` + `rea` MCP server (`open_binary`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `trace_android_references`, `inspect_adb_client`, `list_adb_devices`, `pull_adb_file`, `inspect_packet_capture`, `build_reconstruction_obligation_ledger`, `evaluate_reconstruction_coverage`, `verify_reconstruction`, `list_unknowns`, `close_binary`)
     - Repository Security, Prose, & Memory: `security-audit`, `cloudflare`, `workers-best-practices`, `cloudflare-one`, `wrangler`, `cf-cli`, `humanizer`, and `second-brain` MCP (`project="opencult"`).
2. Continuous Ralph / Goal Loop (Zero Premature Exit, Zero Bug Tolerance):
   - Operate continuously through diagnose -> test-first fix -> Android CLI build -> Compose preview render -> unit/Robolectric test -> `r8-analyzer` + `rea` APK audit -> live `Medium_Phone_API_37.0` AVD launch -> `android layout` + `android input` + `android screen capture` visual inspection -> Perfetto trace audit -> GitHub push & CI/CodeQL check.
   - Never ask permission mid-run, never leave uncommitted fixes locally, never auto-generate fake weight or body-composition readings, and never report completion with skipped assertions or uninspected screenshots.
   - Enforce `INV-23` atomically in the same commit as every defect fix: append a mechanistic root-cause row to `HALL_OF_SHAME.md`, tighten `INVARIANTS.md` (and `MICROENGINEERING.md` / `MOTIONENGINEERING.md` for UI/motion changes), and ship an automated failing-then-passing regression test.
   - Output `<!-- GOAL_COMPLETE -->` ONLY when every single build, test suite, CodeQL check, R8 verification, Android CLI AVD journey screenshot audit, signed commit/push (`verified: true`), and Second Brain handoff passes with quoted commands and exit codes.
   - When physically blocked (e.g., no physical BLE scale hardware powered on near the host or interactive email OTP), isolate offline deterministic packet replay harnesses strictly inside test/inspector diagnostics without touching real measurement sources, and document exact `Verified / Unverified / Unknown` boundaries with raw terminal errors (`AGENTS.md §0`).
3. Anti-Sophistry and `humanizer` Contract:
   - Never fabricate exit codes, command output, BLE packet fields, timestamps, or UI states (`AGENTS.md §0`).
   - Run every user-facing prose artifact (`README.md`, release notes, commit bodies, UX copy) through `humanizer`. Reject em dashes, en dashes, curly quotes, `not-X-but-Y` contrasts, shallow `-ing` riders, and every banned word in `AGENTS.md §0.7`.

---

### 1. Pre-Edit Ground-Truth Reconnaissance & Complete 12-Defect Registry

Read these files in full or targeted blocks before editing source code:
- Project Governance & Specs: `AGENTS.md`, `TRD.md`, `PRD.md`, `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, `DRD.md`, `gradle/libs.versions.toml`, `supabase/migrations/20261126000001_opencult_initial_schema.sql`.
- Second Brain Recall Gate: `list_recent(n=5)`, `list_projects()` (`opencult`), `recall(..., project="opencult", topK=8)`, `recall(..., tag="failure", hops=1)`, `brief(project="opencult")`, enforcing `get(id)` wherever `truncated == true`.
- Gitignored Reverse-Engineering Corpus under `Both/`:
  - `Both/openscale`: `CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `OneByoneLib.kt`, `RunstarR6Handler.kt`, `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt`
  - `Both/occult`: `CONTRACTS.md`, `research/cult_scale_protocol.md`, `js/ble/uuids.js`, `js/ble/parser.js`, `js/metrics/bodycomp.js`, `daemon/internal/protocol/frame.go`, `daemon/internal/protocol/session.go`

Fix all 12 audited workspace defects in the uncommitted `app/`, `core/`, and `feature/` tree cleanly:

1. **Uncommitted Tree + Missing CodeQL / Release CI & Attribution `README.md`:** Existing Android modules (`app/`, `core/`, `feature/`, `gradle/`, `gradlew*`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `HALL_OF_SHAME.md` `SHAME-008..012`, `INVARIANTS.md` `INV-24..25`) remain uncommitted; `.github/workflows/` (`codeql.yml` + `android-intelligent-release.yml`) does not exist; `README.md` lacks dynamic version/CodeQL badges and complete open-source attribution.
2. **Broken `DetachedObsidianDock` & Overlapping Bottom Bars (`DetachedDock.kt:72-113`, visual PNG defect):** The current dock places `CobaltMeasureFab` (`+`) inside the dark pill capsule instead of `DRD:C-05`'s separated geometry (`4`-tab `#0F1115` pill on left + `12.dp` gap + standalone `64.dp` `#2185F7` `+` FAB on right). Meanwhile `StippledBarChart` solid bars and dot-matrix fill bleed directly underneath the translucent dock area (`visual_dashboard_daylight.png`). Fix dock geometry, add a smooth translucent bottom gradient scrim backing the dock container so scrollable canvas charts fade cleanly above the dock, apply `edge-to-edge` window inset discipline cleanly without double padding, and confirm `>= 116.dp` bottom clearance visually via `android screen capture`.
3. **`OnboardingScreen.kt` Broken Permissions Row, Chip Clipping, Stock Inputs, & Third-Scale Chip (`OnboardingScreen.kt:158-275`, screenshot defects):** `Allow Bluetooth` stays 1 line while `Allow Health Connect` wraps onto 2 lines inside `Row(weight(1f))`, breaking symmetry; bottom `"Heavy"` activity chip clips behind the system gesture pill because bottom `.navigationBarsPadding()` is missing; raw `OutlinedTextField` boxes break `DRD.md`; the plain vertical stack discards `DRD:C-07`'s sky-pastel wash (`#CBE1F7 -> #F4F6FA`) and breakout miniature dashboard phone preview; and lines `196-204` display 3 scale options (`Cult Pro 8E` / `Pro Feet-Only` / `CS-BF01 4E`) even though Cult only sells **two** physical scales (**Cult Smart Scale Pro** and **Cult Smart Scale**).
4. **`ScaleFrameParser.kt` + `CultBleConstants.kt` + `CultBleController.kt` Wire Bugs:**
   - `ScaleFrameParser.parse0xCf` (`L124-137`) conflates `OneByone` (`bytes[1..2]`) with `CS-BF01` (`bytes[1]` HR `bpm`, `bytes[5..6]` LE uint16 impedance) and blocks `heartRateBpm` when `expectedTier == CULT_BF01_4E`. Support BOTH protocol sub-variants deterministically with physiological plausibility checks (`150..1200 ohm`).
   - `CultBleConstants.buildModeSetupCommand` (`L33`) hardcodes trailing `0xCB` when `unitKg = false`; compute `xor(bytes[0..9])` dynamically for kg, lb, and st modes.
   - `CultBleController.onDiscovered` (`L198-206`) relies solely on name substrings, broadcasts `OneByone` `F2 00` commands indiscriminately even to Cult Pro units, and performs NO automatic passive scan / GATT reconnect when opening the app after a scale was saved in `scale_devices`.
5. **`ScaleSessionStateMachine.kt` Premature `0xDF` Termination Bug (`L108-111`, `L167-168`):** Triggering completion on the first `0xDF` packet or immediately on `0xCF status == 0x05` risks disconnecting before `0xDF` packets `#2..#4` arrive and get stored (`INV-01`). Wait for all `4 x 0xDF` frames with a deterministic `1200 ms` grace deadline after `0xBE` Sets A/B arrive before locking and disconnecting.
6. **`BodyMetricsEngine.kt` & `MeasurementDetailScreen.kt` `PRD:M-01..M-56` Drift + Formatting Defect:**
   - `BodyMetricsEngine.kt` (`L318-389`) shifts codes away from `PRD.md` (dropping `WHO BMI Category`, `Obesity Degree`, `Fat Mass Index`, `Fat-Free Mass Index`, `Total Body Water Volume L`, `ECW %`, `ICW %`, `Cardiac Index Score` while inserting `M-56 Waist-to-Hip Estimate`). Align every `M-01..M-56` code, formula, and provenance 1:1 across `PRD.md`, `BodyMetricsEngine.kt`, and `MeasurementDetailScreen.kt`.
   - `MeasurementDetailViewModel.load()` (`L91-106`) rebuilds metadata with hardcoded `ScaleTier.CULT_PRO_FEET_ONLY` and formats every value as `%.2f` (displaying `1742.00 kcal/d` and `68.00 bpm`). Preserve per-metric decimals (`0`, `1`, `2`, or category label) and read true hardware tier from `measurements.source`.
7. **`Type.kt` + `res/font/` Single-Weight Static Fonts:** `core/designsystem` includes only `plus_jakarta_sans_regular.ttf` and `jetbrains_mono_regular.ttf`, simulating SemiBold/Bold via `FontVariation`. Bundle verified multi-weight static TTF files (`400`, `500`, `600`, `700`, `800` for `Plus Jakarta Sans`; `400`, `500`, `700` for `JetBrains Mono`) and enforce `fontFeatureSettings = "tnum"` (`INV-12`).
8. **`HomeScreen.kt` Empty Screen, Tiny Pager Arrows, & Shipping Seed Helper (`HomeScreen.kt:178-209, 419-498`):** Empty state hides the entire Bento Grid leaving a 60% blank canvas; `<` / `>` pager icons have `24.dp` bounds (`INV-18` violation -> `48.dp` minimum hitbox with `32.dp` visual circle); and `HomeViewModel.seedSampleMeasurementForVerification()` ships demo weigh-in creation inside main source (remove completely; keep deterministic fixtures strictly in `src/test/` or explicit labeled Inspector replay).
9. **`ReportsScreen.kt` Broken Calendar Filter, Hardcoded `16.5f` Gauge, & Non-Interactive Metrics:** Selecting a date in `OpenCultCalendarSheet` updates `selectedDate` (`L107`) without filtering `filtered` or `bars` (`L113-128`); empty state falls back to fake `16.5f` body fat (`L132`); and none of the **20** (`CS-BF01`) or **56** (`Cult Pro`) parameters open an interactive chart view with full multi-axis filtering.
10. **Unreactive Member State + Missing Member Lock + Health Connect Orphan Writes:** `ReportsViewModel` (`L85`) and `BodyMapViewModel` (`L63`) read `vault.getActiveProfileId()` non-reactively inside `combine()`, staying stale when members switch; `WeighIn` lacks a prominent `Lock to Active Member` toggle; and `MeasurementDetailViewModel.reassign()` (`L128-136`) changes `profileId` without deleting Member A's written Health Connect records or recalculating BIA metrics for Member B's sex, age, and height!
11. **`HealthConnectGateway.kt` + `InspectorScreen.kt` Bugs:** `readDailyContext` omits `ActiveCaloriesBurnedRecord.ENERGY_TOTAL` fallback; `InspectorViewModel.pollExternalHc()` discards external `WeightRecord` reads instead of persisting them into Room; `AndroidManifest.xml` omits `READ_HEALTH_DATA_HISTORY` and opt-in `READ_HEALTH_DATA_IN_BACKGROUND`; and native Health Connect controls need unified placement across Onboarding, Home, and the Sync Hub.
12. **Absent `:core:audio` Sound Synthesizer & Hardcoded Host Path:** No synthesized PCM sound feedback exists anywhere in the app, and `EndToEndAppAndVisualTest.kt` (`L78-80`) writes PNGs to hardcoded `C:\Users\imsov\.gemini\...` instead of `build/outputs/visual_screenshots/`.

---

### 2. Deep Dual-Scale Reverse Engineering (`reverse-engineer-anything` + `rea` MCP)

Model ONLY the two real physical Cult scales in Onboarding, Pairing UI, and Saved Scale selection (NEVER present `"Pro Feet-Only"` as a selectable hardware SKU during setup):

1. **Device 1 — Cult Smart Scale (`CS-BF01`, 4-Electrode, 20-Parameter Marketing Tier):**
   - Auto-detected via BLE name (`CS-BF*`, `Cult Smart Scale`, `Cultsport*`, `OKOK*`, plus `OneByone`/`Health Scale`/`Eufy T914*` for legacy OneByone sub-models) AND confirmed on the wire by `0xFFF0`/`0xFFF4` notifications delivering 11-byte or `>= 18`-byte `0xCF` frames without `0xBE`/`0xDF` packets.
   - Command Handshake (`0xFFF1`, `WRITE_NO_RESPONSE`):
     - Send mode/unit frame `FD 37 <unit: 00=kg, 01=lb, 02=st> <group=01> 00 00 00 00 00 00 <xor[0..9]>` (checksum computed dynamically for all units).
     - Send RTC sync `F1 <year_hi> <year_lo> <month> <day> <hour> <minute> <second>` and arm a `3000 ms` fallback timer (`OneByoneHandler`) when `F1 00` ACK is omitted.
     - Request offline history `F2 00` -> send `F2 01` clear ONLY after receiving genuine `>= 18`-byte history frames (`!coalescedDuplicateDropped`).
   - Byte-Level `0xCF` Parsing (`ScaleFrameParser.parse0xCf`):
     - Header `bytes[0] == 0xCF`; XOR `bytes[10] == xor(bytes[0..9])`.
     - Weight `weightKg = uint16_le(bytes[3..4]) / 100.0`.
     - `INV-21` ATT Coalesced Duplicate Guard (`SHAME-005`): When `bytes.size >= 18 && liveChecksumOk && bytes[11] == 0xCF`, mark `coalescedDuplicateDropped = true` and `isHistory = false`; otherwise `>= 18` bytes with valid `2020..2099` timestamp `[11..17]` parses as stored history.
     - Phase `bytes[9]`: `0x01` = settling; `0x00` = `CS-BF01` locked body phase; `0x36` = OneByone classic lock. Enforce `WeightLockFrames = 3` and `HRLockFrames = 3` convergence (`Both/occult/daemon/internal/protocol/session.go`).
     - Firmware Sub-Classification (`Both/occult/research/cult_scale_protocol.md` vs `OneByoneHandler`):
       - On `CS-BF01` / `Cult`: read `bytes[1]` as Heart Rate (`bpm` when `status == 0x00 && b1 in 35..220` and steady across lock frames; `null` if `0` or settling); validate `bytes[5..6]` LE uint16 against physiological `150..1200 ohm` plausibility (`raw` or `raw / 10.0` only when stable and repeatable). Remember `Both/occult/research/cult_scale_protocol.md` proved retail `CS-BF01` firmware frequently transmits transient non-impedance noise on `bytes[5..7]` while locking proprietary BIA inside `0xDF`: when uncalibrated, suppress fake impedance cleanly and compute all **20 honest anthropometric parameters** (`Deurenberg 1991`, `Watson 1980`, `Mifflin-St Jeor 1990`, `Heymsfield`, `Devine`) marked `MetricProvenance.ANTHROPOMETRIC_ESTIMATE` (`INV-02`).
       - On legacy `OneByone` / `Eufy` names: preserve `((b2 shl 8) or b1) / 10.0` decoding so all existing `OneByone` test vectors pass without regression.
   - Surface all **20 active `CS-BF01` metrics** immediately across Home, Measurement Detail, Body Map (aggregate composition view with explicit 8E handle-lock notice), and trend charts.

2. **Device 2 — Cult Smart Scale Pro (8-Electrode Dual-Frequency Handle, 50+ / 56-Parameter Tier):**
   - Auto-detected via BLE name (`CULT*` excluding `CS-BF*`) AND confirmed dynamically on the wire by `0xCF` handle states (`0x08` = handle impedance in progress, `0x05` = handle measurement complete), `2 x 13B 0xBE` packets, and `4 x 17B 0xDF` packets (`INV-01`). Do NOT transmit unnecessary `OneByone` `F2 00` history clears on Pro sessions unless Pro services explicitly indicate history support.
   - Verified `0xBE` Impedance Mapping (`CultSmartScaleProHandler` + `BodyPod2Handler` reconciled):
     - Each `13B` packet encodes `bytes[0]=0xBE`, `bytes[1]=packetIndex (0x00 low freq, 0x01 high freq)`, five `uint16_le` channels `[2..11]`, and `bytes[12]=xor(bytes[0..11])`.
     - Scaling all five channels by `0.1 ohm` (`raw / 10.0`) converts observed Pro values (`~2867, ~3013, ~248, ~2904, ~2668` from `CultSmartScaleProHandler.kt`) into real physiological segmental resistances: `[Right Arm ~286.7 ohm, Left Arm ~301.3 ohm, Trunk ~24.8 ohm, Right Leg ~290.4 ohm, Left Leg ~266.8 ohm]`. Taking `((RA + LA) / 2.0) + TR + ((RL + LL) / 2.0)` yields `~597.4 ohm` (matching `BodyPod2Handler`'s `500..650 ohm` wrist-to-ankle whole-body circuit cleanly!).
     - Archive BOTH raw `uint16_le` values and `0.1 ohm` scaled floats in `raw_ble_packets.decoded_impedances_ohm`, collect all `4 x 17B 0xDF` packets (`seqA = bytes[2]`, `seqB = bytes[3]`, `xor` verified, `1200 ms` grace timeout after `0xBE`), store `0xDF` payloads verbatim without guessing cipher keys (`INV-01`, `INV-02`), and compute all **56 parameters (`PRD:M-01..M-56`)**.
   - Automatic Runtime `Feet-Only` Downgrade: When standing on `Cult Smart Scale Pro` without holding the handle (`0xCF status = 0x00`, zero `0xBE` frames within `3000 ms` or 1-tap `Confirm Weight Only`), downgrade ONLY that weigh-in session to `ScaleTier.CULT_PRO_FEET_ONLY` (`INV-03`), log verified `Weight` (+ `HR` if measured), suppress BIA/ECW/ICW/segmental metrics cleanly, and write `WeightRecord` only to Health Connect.

3. **One-Time Pairing, App-Launch Passive Reconnect, & `rea` Package Inspection:**
   - Save paired MAC, advertised name, and classified tier (`cult_pro_8e` vs `cult_bf01_4e`) inside Room `scale_devices`.
   - On `Lifecycle.State.STARTED` (Home or Weigh-In foregrounded with `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` granted and Bluetooth adapter enabled), auto-run bounded BLE scan / GATT reconnect for saved `scale_devices` so stepping onto the paired scale immediately wakes telemetry, displays settling ticks, locks, saves, plays feedback tone, writes Health Connect, and pushes Supabase without re-pairing!
   - If Bluetooth adapter is OFF or API 31+ Nearby Devices permissions are missing/permanently denied, display inline status with a 1-tap action (`BluetoothAdapter.ACTION_REQUEST_ENABLE` or permission rationale / `ACTION_APPLICATION_DETAILS_SETTINGS` following `android-permissions-security`).
   - Run `rea` MCP (`open_binary`, `inspect_android_package`, `inspect_packet_capture`, `evaluate_reconstruction_coverage`, `close_binary`) against any vendor APK / `btsnoop_hci.log` if present AND on your compiled `app-release.apk` to verify R8 stripping, manifest safety, and zero leftover test/mock classes.

---

### 3. Biophysical Equations, Exact `PRD:M-01..M-56` Catalog, & Re-Calculation (`:core:bia`)

Keep pure JVM testability with GPL-3.0 upstream notices (`oliexdev/openScale`: `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt`):

1. **Exact Plausibility Validation (`INV-03`):** Require `age in 10..120`, `heightCm in 80.0..250.0`, `weightKg in 10.0..350.0`. For 8E (`Cult Pro`): verify `limbs in 50.0..700.0 ohm`, `trunk in 5.0..120.0 ohm`, whole-body `Z_50 in 150.0..1200.0 ohm`, and `|Z_low - Z_high| / Z_high >= 0.01`. Fail closed to `CULT_PRO_FEET_ONLY` when handles are disconnected or conductivity fails.
2. **Closed Compartment & Dual-Frequency Equations:**
   - Whole-body `FFM` & `TBW`: `Sun et al. (2003)` + Pace & Rathbun (`0.732 * FFM`) under valid 8E/4E BIA; honest `Deurenberg (1991)` BF% + `Watson (1980)` TBW fallback when impedance is absent (`ANTHROPOMETRIC_ESTIMATE`).
   - Dual-frequency Cole model (`R0`, `Rinf`) solved deterministically via bounded bisection when low and high bands diverge (`De Lorenzo Eq. B2` `ECW`, `ICW = TBW - ECW`, `ECW/TBW`, `Wang 2004` `BCM = ICW / 0.70`, gated strictly to `CULT_PRO_8E`).
   - `Janssen (2000)` `SMM` & `SMI`; `FFM - boneMass` total muscle; `Katch-McArdle` (BIA) vs `Mifflin-St Jeor` (fallback) `BMR`; Sedentary (`1.2x`) & Active `TDEE`; Devine Ideal Body Weight & Obesity Degree; FMI & FFMI; Subcutaneous & Visceral Fat partitions; Cardiac Index Score; Composite Health Score & Metabolic Age.
   - Mass-conserving 5-segment muscle & fat decomposition (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`) driven by conductance ratios (`L_seg^2 / Z_seg`), normalized so `abs(sum_segments - total) <= 0.05 kg`, plus upper/lower bilateral symmetry % and `ALM / ASMI`.
3. **1:1 `PRD:M-01..M-56` Parity, Formatting Precision, & Reassignment Recalculation:**
   - Match `PRD.md` codes `M-01` through `M-56` identically across `:core:bia`, `PRD.md`, and UI screens.
   - Carry explicit `displayDecimals` (`0` for `kcal/day`, `bpm`, `years`, `level`, `score`; `1` for `%`, indices; `2` for `kg`, `L`, `kg/m2`, `ohm`, `ratio`; clean text value for `WHO BMI Category`).
   - Implement `BodyMetricsEngine.recalculateForProfile(measurement, rawPackets, newProfile)` so switching owner (`reassignMeasurement`) recomputes all profile-dependent composition metrics cleanly from preserved `raw_ble_packets` (`INV-01`).

---

### 4. Reverse-Engineered Native Google Health Connect Integration (`:core:healthconnect`, `connect-client:1.1.0`)

Implement Google Health Connect against the exact `androidx.health.connect:connect-client:1.1.0` schema contract:

1. **Exact Schema Boundary (Verified in `connect-client-1.1.0/jars/classes.jar`, 41 Concrete `Record` Classes):**
   - Out of all 41 concrete Health Connect record subclasses, ONLY these **8 core write records (plus optional `RestingHeartRateRecord`)** map cleanly to scale hardware measurements:
     1) `WeightRecord` (`Mass.kilograms`, `PRD:M-01`)
     2) `BodyFatRecord` (`Percentage(1.0..99.0)`, `PRD:M-12`)
     3) `LeanBodyMassRecord` (`Mass.kilograms`, `PRD:M-15` FFM)
     4) `BoneMassRecord` (`Mass.kilograms`, `PRD:M-36` BMC)
     5) `BodyWaterMassRecord` (`Mass.kilograms`, `PRD:M-22` TBW mass in kg)
     6) `BasalMetabolicRateRecord` (`Power.kilocaloriesPerDay`, `PRD:M-41`)
     7) `HeartRateRecord` (`startTime`, `endTime = +1s`, `samples = [Sample(time, bpm)]`, `PRD:M-06`, written ONLY when emitted)
     8) `HeightRecord` (`Length.meters`, written on primary HC profile create or height change)
     9) Optional user opt-in `RestingHeartRateRecord` (`beatsPerMinute`, disabled by default with Settings toggle so non-resting post-walk weigh-ins do not overwrite clinical RHR unless requested)
   - **Transparent Schema Truth:** Google Health Connect `1.1.0` provides ZERO custom extension fields or native schemas for BMI, Visceral Fat Index, Subcutaneous Fat %, ECW/ICW/BCM, Skeletal Muscle Mass (`SMM` separate from `LeanBodyMassRecord`), 5-Segment Arm/Leg/Trunk Muscle & Fat, Bilateral Symmetry, Metabolic Age, Impedance Ohm, or Composite Score. Store ALL 20 (`CS-BF01`) / 56 (`Cult Pro 8E`) parameters locally in Room SQLite, synced to Supabase RLS, exportable as JSON/CSV, and visualizable in in-app lifetime charts.
   - **Record Metadata & `INV-02`/`INV-03` Write Gate:** Stamp every written record with `Device(Device.TYPE_SCALE, "Cult.fit", resolvedModelName)`, `Metadata.autoRecorded(...)` (for BLE weigh-ins) or `Metadata.manualEntry(...)` (for manual entries), `clientRecordId = "opencult:${measurement.id}:${tag}"`, and monotonically incremented `clientRecordVersion`. When a session falls back to `CULT_PRO_FEET_ONLY` (`status = 0x00`), write ONLY `WeightRecord` (+ `HeartRateRecord` if measured) and never push unverified fat/bone/water estimates into Health Connect!
2. **Inbound Sync, Special Permissions, & Orphan Lifecycle Cleanup:**
   - Read daily `StepsRecord.COUNT_TOTAL`, `SleepSessionRecord.SLEEP_DURATION_TOTAL`, and `TotalCaloriesBurnedRecord.ENERGY_TOTAL` with automatic **`ActiveCaloriesBurnedRecord.ENERGY_TOTAL` fallback** in `HealthConnectGateway.readDailyContext`.
   - Persist external `WeightRecord` changes (`dataOrigin.packageName != BuildConfig.APPLICATION_ID`) inside `pollExternalWeights` into Room `measurements` (`source = "health_connect_import"`) with dedup (`INV-05`) and handle remote `DeletionChange`s.
   - Declare `android.permission.health.READ_HEALTH_DATA_HISTORY` (enabling >30-day backfill/historical sync) and opt-in `READ_HEALTH_DATA_IN_BACKGROUND` alongside rationale `MainActivity` (`ACTION_SHOW_PERMISSIONS_RATIONALE`) and Android 14+ `ViewPermissionUsageActivity` `<activity-alias>` (`android.permission.START_VIEW_PERMISSION_USAGE`). Catch `SecurityException` gracefully per permission so partial grants never crash writes.
   - **Mandatory Orphan Delete + Conditional Re-Sync (`INV-05`, `INV-16`):** Whenever a measurement is soft-deleted OR reassigned from Member A -> Member B, ALWAYS delete prior Health Connect records via `deleteSyncedRecordsForMeasurement(measurementId)` FIRST, recalculate metrics for Member B, and re-insert into Health Connect ONLY if Member B has `isPrimaryHcUser = true`.
3. **Health Connect UI Placement:**
   - **Onboarding Step 2:** Equal-height, single-line aligned `Allow Health Connect` action card explaining the exact Write (`Weight`, `Body Fat`, `Lean Mass`, `Bone Mass`, `Body Water`, `BMR`, `HR`, `Height`) + Read (`Steps`, `Sleep`, `Calories`, `External Weight`) permissions without text wrap bugs.
   - **Home Dashboard:** Live `Health Connect` sync pill (`Synced X / 8` or `Grant Perms`) + Bento Overview Page 2 (`Steps`, `Sleep`, `Calories`) visible immediately even when weight history is empty.
   - **Sync Center / Inspector (`DRD:S-08`):** Permission readiness gauge (`Granted / Total`), 1-tap `Grant Missing Permissions`, 1-tap `Backfill All History`, `Sync External Weights Now`, toggle for `RestingHeartRateRecord` & background polling, and scrollable per-record sync status ledger.

---

### 5. Reactive Member Coordinator, Member Lock, & Zero-Refresh Realtime (`:core:database` + `:core:sync-supabase`)

1. Inject a singleton `ActiveMemberSession` holding `activeProfileId: StateFlow<String?>` and `memberLockEnabled: StateFlow<Boolean>` across ALL ViewModels (`MainViewModel`, `HomeViewModel`, `WeighInViewModel`, `BodyMapViewModel`, `ReportsViewModel`, `ProfilesViewModel`, `MeasurementDetailViewModel`) so switching members updates Home, Body Map, and Reports in real time (`< 16 ms`).
2. Implement explicit **`Lock to Active Member`** toggle on `WeighIn` & `Home`:
   - **Locked ON:** Every scale session assigns straight to the locked member (`assignmentState = "confirmed"`), runs `BodyMetricsEngine` with that member's anthropometry, commits Room atomically (`< 50 ms`, `INV-06`), triggers Health Connect if `isPrimaryHcUser`, and pushes Supabase outbox with zero cross-member dialogs or contamination!
   - **Auto-Match (`INV-09`):** Single match within `±auto_match_window_kg` auto-confirms; `0` or `2+` matches set `assignmentState = "pending_review"` and pop an interactive member chooser immediately.
3. Enforce `(SELECT auth.uid()) = account_id` `FORCE ROW LEVEL SECURITY` (`INV-08`), zero shipping fake/demo weigh-ins, indexed lifetime storage `(profile_id, local_date, measured_at)`, and live `postgresChangeFlow` (`INV-07`) filtering self-echoes by `mutation_id`.

---

### 6. Acoustic UI Sound Engineering (`:core:audio` `OpenCultSoundEngine`)

Replace OS notification beeps (`RingtoneManager` / notification sounds banned) with synthesized, click-free 16-bit PCM mono feedback using `android.media.AudioTrack` (`AudioAttributes.USAGE_ASSISTANCE_SONIFICATION`, `CONTENT_TYPE_SONIFICATION`, `MODE_STATIC`, `44100 Hz`), shaped by `5 ms..8 ms` raised-cosine Hann attack/release envelopes (`w(n) = 0.5 * (1 - cos(pi * n / N))`) and `-12..-18 dBFS` headroom:

1. `SCALE_CONNECTED`: Warm major-fifth crystal tone (`C5 523.25 Hz -> G5 783.99 Hz`, `80 ms` each, `-14 dBFS`).
2. `SETTLING_TICK`: Crisp subtle tactile pip (`880 Hz`, `12 ms` envelope, `-18 dBFS`) once per stable weight streak step (`1/3`, `2/3`).
3. `WEIGHT_LOCKED`: Medical-instrument confirmation dyad (`D5 587.33 Hz + A5 880.00 Hz`, `140 ms` gentle exponential decay, `-12 dBFS`), strictly deduplicated once per weigh-in lock (`INV-21`).
4. `METRICS_LOGGED_PRO_50` / `METRICS_LOGGED_BASIC_20`: Bright upward triad (`C5 -> E5 -> C6`, `220 ms` tail on 8E Pro / crisp `C5 -> G5` fifth on 4E `CS-BF01`) when BIA calculation + Room save + Health Connect sync finish.
5. `AMBIGUOUS_MEMBER_PROMPT`: Balanced neutral two-tone interval (`A4 440 Hz -> G4 392 Hz`, `150 ms`) prompting member selection.
6. `CAPTURE_ERROR`: Soft muted descending thud (`220 Hz -> 165 Hz`, `110 ms`, `-16 dBFS`) on XOR failure or premature step-off.
- Respect Android ringer/silent/vibrate state (`AudioManager`), run playback & track disposal on a dedicated single coroutine audio dispatcher (`ANR`-free, follow `anr-audit-and-fix`), expose `Scale Sound Feedback` switch in Settings, and verify PCM buffers mathematically in JVM unit tests (`zero NaN`, `abs <= 32767`, `samples[0] == 0`, `samples[last] == 0`, exact sample lengths).

---

### 7. Micro-Engineered UI, Compose Performance, Fonts, & Interactive 20/56 Data Charts (`:core:designsystem` + `:feature:*`)

Follow `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and `DRD.md`:

1. **Zero Stock Popups or Material Chrome Violations:** Ban `DatePickerDialog`, Material `DatePicker`, `Spinner`, `DropdownMenu`, stock `OutlinedTextField` boxes in Onboarding/Profiles/Settings, raw `NavigationBar`/`Card`/`FloatingActionButton`, and third-party chart libraries. Use custom `Canvas` primitives (`DRD:C-01..C-06`), custom `OpenCultCalendarSheet`, `OpenCultPillSelectorSheet`, custom squircle inputs (`22.dp` corners, `#EEF1F6`/`#1C222E` track, animated focus ring, explicit IME `Modifier.fitInside(WindowInsetsRulers.Ime.current)` or clean `imePadding()` per `edge-to-edge` skill without double padding!), `>= 48.dp` hitboxes (`INV-18`), and 4-stage micro-press (`graphicsLayer` deferred lambdas + haptic).
2. **Compose 120 Hz Performance Rules (`jetpack-compose-performance`):** Defer animated values and offsets into `Modifier.graphicsLayer { ... }` and `Canvas { ... }` lambdas (`() -> Float`); cache `Path`, `CornerPathEffect`, `DashPathEffect`, and `Brush` inside `remember(...)`; supply primitive `key` and `contentType` on all `LazyColumn`/`LazyRow` lists (`56`-parameter & history lists); query `CompositionLocal`s outside item lambdas; and verify frame render stability via `android profiler systemtrace`.
3. **Visual Fixes Across Every Screen:**
   - **Detached Obsidian Pill + FAB + Bottom Scrim (`DRD:C-05`, `INV-13`):** Separate the `4`-tab `#0F1115` capsule (`weight = 1f`, `64.dp` height) and `64.dp` `#2185F7` `+` FAB by `12.dp` gap over a smooth translucent bottom canvas fade scrim (`colors.canvas.copy(alpha = 0f -> 0.94f -> 1f)`), guaranteeing `dockScrollClearance()` so charts never visibly bleed behind the pill.
   - **Multi-Weight Static Typography (`Type.kt`):** Ship true `Plus Jakarta Sans` (`400/500/600/700/800`) and `JetBrains Mono` (`400/500/700`) TTF resources + `"tnum"` across all telemetry numbers (`INV-12`).
   - **Sleek `DRD:C-07` 3-Step Onboarding:** Vertical Daylight wash (`#CBE1F7 -> #F4F6FA`), breakout phone dashboard preview (`Step 1`), equal-height aligned Bluetooth + Health Connect permissions & hardware auto-discovery card (`Step 2`, ZERO text-wrap asymmetry, displaying only Cult's 2 physical scales cleanly), and spacious Member Anthropometry step (`Step 3` with full `navigationBarsPadding()`, ZERO `"Heavy"` bottom clipping).
   - **Complete Home Dashboard (Populated & Empty):** Always display Hero Card (active scale mode + Member Lock badge), asymmetric Lavender/Sage/Peach Bento Grid (`48.dp` clickable `<` / `>` pager arrows showing Health Connect Page 2 `Steps / Sleep / Calories` immediately on initial install!), and `Health Connect` readiness card.
   - **Clickable 20 / 56 Parameter Charts + Multi-Axis Lifetime Filtering:** Clicking any metric across Home, Measurement Detail, Body Map, or Reports launches an interactive `Canvas` Trend Explorer (bezier spline + clinical reference zone shading + interactive scrub tooltip + delta stats + raw packet provenance link). Wire `OpenCultCalendarSheet` `selectedDate` / custom date bounds directly into `ReportsScreen`'s `filtered` & `bars`, plus `7D / 30D / 90D / 1Y / Lifetime All`, Scale Source (`All / Pro 8E / CS-BF01 4E / Feet-Only / HC Import`), Member Filter, and full 20/56 Metric Selector, replacing fake `16.5f` empty fallback with honest empty guidance.
   - **Rich `@Preview` Suite for Android CLI Preview Renderer:** Annotate screens and custom Canvas components across `:core:designsystem` and `:feature:*` with `@Preview` (`Daylight`, `Obsidian`, `Compact 360dp`, `FontScale 1.5x`) so `android preview list` and `android preview render --component-tree` render and inspect EVERY view deterministically even before booting the AVD!

---

### 8. Hardened R8, Keystore (`SHID`), Play Protect, & Cloudflare Security

1. **Keystore & Strict Signing (`app/build.gradle.kts`):** `applicationId = "org.opencult.scale"`, `minSdk = 31`, `compileSdk = 36`, `targetSdk = 36`. Maintain gitignored RSA-4096 PKCS12 `keystore/opencult-release.jks` + `keystore.properties` (plus CI secrets env support), `enableV1Signing = true`, `enableV2Signing = true`, hard-failing release builds when release credentials are absent. Print and document non-secret Release Keystore `SHA-1` and `SHA-256` digests (`SHID`).
2. **R8 Configuration + `r8-analyzer` Path A (AGP `9.5.1` >= `9.3.0`):**
   - Enforce `isMinifyEnabled = true`, `isShrinkResources = true`, `-repackageclasses 'o'`, `-allowaccessmodification`, `-renamesourcefileattribute SourceFile`, `-keepattributes SourceFile,LineNumberTable`, `-assumenosideeffects` removing `Log.v/d/i`, plus crisp keep rules for Kotlinx Serialization, Supabase DTOs, Hilt workers, Room, `blessed-kotlin`, and Health Connect.
   - Run **AGP 9.5.1 Path A R8 Analyzer**: `./gradlew.bat :app:analyzeReleaseR8Config`, convert `app/build/reports/r8/r8-config-analyzer-release.pb` using `r8-analyzer/scripts/convert_pb_to_json.py` & `analyze.py`, eliminate overly broad rules until R8 optimization passes cleanly, and run `android analyze --analysis=all --format=json app/build/outputs/apk/release/app-release.apk` + `rea` (`open_binary`, `inspect_android_package`).
3. **Google Play Protect & Cloudflare Security Compliance:**
   - Run `play-policy-insights` (`scripts/orchestrator.py` + audits), `android-permissions-security`, `android-intent-security`, `anr-audit-and-fix`, and `security-audit`.
   - `aapt2 dump permissions app-release.apk` must show zero restricted permissions (`BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, `REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `ACCESS_*_LOCATION`, external storage permissions, `BLUETOOTH_ADVERTISE`).
   - Enforce `allowBackup="false"`, `usesCleartextTraffic="false"`, explicit `android:exported`, `IntentSanitizer`, `PendingIntent.FLAG_IMMUTABLE`, Android Keystore AES-256-GCM `SecureVault`, scoped `FileProvider` `/exports/`, and Cloudflare repo/edge security rules (`crypto` CSPRNG, timing-safe secret checks, zero leaked tokens/keys, `INV-15`, `INV-24`).

---

### 9. GitHub Actions CodeQL SAST Security + Autonomous Intelligent Release CI + Attributed `README.md`

1. **Create `.github/workflows/codeql.yml` & `.github/codeql/codeql-config.yml` (CodeQL SAST):**
   - Triggers: `push` to `main`, `pull_request` to `main`, weekly `schedule` cron (`'30 2 * * 1'`), and `workflow_dispatch`.
   - Permissions: minimal `actions: read`, `contents: read`, `security-events: write`.
   - MUST use **`github/codeql-action/init@v4`** and **`github/codeql-action/analyze@v4`** (Node 24; do NOT use deprecated `@v3`) with **`queries: security-extended,security-and-quality`** across a clean matrix:
     - `java-kotlin` (`build-mode: manual`): configures JDK 17 + Android SDK `36` + non-secret dummy `.env.example` placeholder props if needed for compilation only, runs `./gradlew :app:assembleDebug :core:bia:test :core:ble:testDebugUnitTest :core:audio:testDebugUnitTest :core:database:testDebugUnitTest :core:healthconnect:testDebugUnitTest :core:sync-supabase:testDebugUnitTest --no-daemon` strictly between `init@v4` and `analyze@v4` so Kotlin/KSP/AGP 9.5 compilation frames are traced completely!
     - `python` (`build-mode: none`): audits `scripts/bump_version.py` and `scripts/generate_release_notes.py`.
     - `actions` (`build-mode: none`): audits `.github/workflows/*.yml` for command injection, untrusted PR inputs, and privilege boundaries.
   - Add `.github/dependabot.yml` (weekly `gradle` and `github-actions` security updates) and verify repository CodeQL readiness via `gh api repos/imsovikde/OpenCult`.
2. **Create `.github/workflows/android-intelligent-release.yml` + `scripts/`:**
   - Triggers: `push` to `main` (skipping `[skip ci]` commit loops cleanly), `v*` tags, `pull_request`, and `workflow_dispatch` (`bump_type`: `patch`/`minor`/`major`/`none`, `release_channel`).
   - Autonomous Version Sync (`scripts/bump_version.py`): Atomically increments `versionCode` + semantic `versionName` across `app/build.gradle.kts` AND `README.md` release badges/table; UI "About / Diagnostics" screens read `BuildConfig.VERSION_NAME` & `BuildConfig.VERSION_CODE` dynamically so UI and repository tags stay 100% in sync!
   - Hardened Signed Build & Verification Gate: Restores `keystore/opencult-release.jks` cleanly from `OPENCULT_KEYSTORE_BASE64` / password / alias GitHub Secrets (never checked into git), injects `OPENCULT_SUPABASE_URL` & `OPENCULT_SUPABASE_ANON_KEY`, runs unit/Robolectric tests, `:app:lintRelease`, `:app:assembleRelease`, `apksigner verify --verbose --print-certs` (capturing keystore `SHA-1` & `SHA-256` `SHID` digests and `.sha256` file), `aapt2 dump permissions`, and R8 `mapping.txt` retention.
   - Adaptive Intelligent Release Notes (`scripts/generate_release_notes.py`): Collects git commits, module diff breakdown, APK binary size, `SHID` digests, and permission summary; calls a lightweight AI model (`GITHUB_TOKEN` GitHub Models `gpt-4o-mini` / `GEMINI_API_KEY` flash-lite via official HTTPS API) when configured to write `humanizer`-validated highlights, and falls back deterministically to structured diff/commit notes when no AI token exists! Attaches `opencult-v<version>-release.apk`, `.sha256`, `mapping.txt`, `metadata.json`, `RELEASE_NOTES.md`, and source archive via `softprops/action-gh-release@v2`, plus PR diff summary comments.
   - Validate all YAML/Python scripts locally (`yaml.safe_load`, `actionlint` if installed, `--dry-run`) before pushing.
3. **Sophisticated Attributed `README.md`:**
   - Pass `humanizer` before saving. Include live version, CI build status, and CodeQL status badges; verified release APK download + `SHA-256` (`SHID`) verification steps; Cult 2-scale comparison (**Cult Smart Scale Pro 56-Param** vs **Cult Smart Scale `CS-BF01` 20-Param**); `0xCF`/`0xBE`/`0xDF` protocol architecture; Health Connect read/write support table explicitly distinguishing native Android Health Connect record classes from the full 56-parameter Room/Supabase catalog; Supabase RLS summary; Android CLI build/preview/test instructions; explicit credit (`oliexdev/openScale` `[GPL-3.0]`, `anshuman852/occult` `[MIT]`, `weliem/blessed-kotlin`, Jetpack Compose, `androidx.health.connect`, Supabase Kotlin SDK, Material Icons Extended); and nominative trademark disclaimer (`AGENTS.md §3.5`).

---

### 10. Mandatory End-to-End Android CLI Execution, AVD Journeys, Granular Commits, & Final Proof

Execute EVERY verification step below AFTER your final edit (NO skipped checks):

1. **Automated Unit, Robolectric, Lint, & R8 Build Gates (`exit 0`):**
   - Fix `EndToEndAppAndVisualTest.kt` to write PNG screenshots into project-local `build/outputs/visual_screenshots/` (remove hardcoded desktop/home paths).
   - Add automated regression tests for ALL 12 remediated defects (`CS-BF01` HR + impedance + dynamic XOR, `4 x 0xDF` grace wait, `PRD:M-01..M-56` ordering & formatting, `:core:audio` PCM envelope & zero-clipping bounds, Member Lock isolation, `selectedDate` calendar filter + 20/56 metric drill-down, `ActiveCaloriesBurnedRecord` fallback + external `WeightRecord` Room commit + reassignment Health Connect orphan purge, `DetachedObsidianDock` geometry + scrim, `48.dp` min hitboxes, multi-weight fonts + `tnum`).
   - Run:
     - `.\gradlew.bat testDebugUnitTest :app:lintRelease :app:analyzeReleaseR8Config :app:assembleDebug :app:assembleRelease`
     - `android describe --project_dir=.`
     - `android analyze --analysis=all --format=json app/build/outputs/apk/release/app-release.apk`
2. **Android CLI Headless Compose Preview Verification (Without Wait for Emulator):**
   - Run `android preview list --reference gradle://.?:app@debug --all --format=json`
   - Render key previews across Daylight & Obsidian themes (`360dp`, standard, `1.5x` font scale) using `android preview render --component-tree --output build/outputs/compose_previews ...` and inspect both generated PNGs (`read_file`) and component-tree JSON bounds!
3. **Android CLI End-to-End Virtual Device Testing on `Medium_Phone_API_37.0` (`android-cli`, `interactions`, `journeys`, `android-profiler`):**
   - Check running devices (`adb devices`; `android emulator list`) and boot existing AVD:
     - `android emulator start Medium_Phone_API_37.0` (blocks until system boot finishes cleanly).
   - Install and launch cleanly using Android CLI:
     - `android install --install-options="-g" --apks=app/build/outputs/apk/release/app-release.apk` (uninstall any prior conflicting signature package cleanly first if needed)
     - `android run --activity=org.opencult.scale/.MainActivity --apks=app/build/outputs/apk/release/app-release.apk` (also verify debug journey interaction where required).
   - Drive a full XML/structured end-to-end UI & data journey across ALL 9 screens using **Android CLI (`android layout`, `android input`, `android screen`)**:
     - Capture layout trees: `android layout --pretty -o build/outputs/e2e_journeys/step_<N>_layout.json` (`--full` when verifying bounds & accessibility semantics).
     - Interact purely via semantic selectors and annotated visual coordinates: `android input tap txt="..."` / `android input tap desc="..."` / `android input text ...` / `android input scroll ...` (and `android screen capture --annotate -o ...` + `android screen resolve` where Canvas charts or custom hitboxes are targeted!).
     - Cover end-to-end flows across: **Onboarding Steps 1, 2, 3 (aligned buttons, zero clipped chips)** -> **Home Empty (Daylight + Obsidian + Health Connect Page 2 Bento + 48dp arrows)** -> **Weigh-In Sheet (Member Lock toggle + Cult Pro 8E & CS-BF01 4E simulated diagnostic stream)** -> **Measurement Detail (all 56 / 20 params + Reassign -> HC orphan check)** -> **Body Map (5-segment interactive selection + 4E tier gate)** -> **Reports (3-zone Arc Gauge + Stippled Chart + `OpenCultCalendarSheet` date filter + clickable metric trend chart + multi-axis filters + dock scrim verification)** -> **Profiles (multi-member isolation)** -> **Health Connect Hub (`Granted / Total`, Backfill, Poll External)** -> **Protocol Inspector (`0xCF` / `0xBE` / `0xDF` frames)**.
     - Visually inspect EVERY captured PNG screenshot (`read_file` on `build/outputs/e2e_journeys/*.png`)! Confirm zero button wrap mismatches, zero dock/chart bleed-through, crisp fonts, and zero layout/clipping errors.
   - Profile live scroll & weigh-in animation frame pacing using **`android profiler systemtrace start` / `stop`**, verify zero dropped-frame jank storms and zero `FATAL EXCEPTION` / `ANR` in `adb logcat`.
4. **APK Signature, Permissions, Secrets, & `rea` Audit:**
   - Run `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk` (record release `SHA-1` & `SHA-256` `SHID` digests).
   - Run `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk` (confirm least privilege).
   - Run `rea` MCP (`open_binary`, `inspect_android_package`, `evaluate_reconstruction_coverage`, `close_binary`) on `app-release.apk`.
   - Confirm `git check-ignore Both keystore.properties .env.local` returns `exit 0` and automated banlist/secret scans pass (`exit 0`).
5. **Granular SSH-Signed Commits, GitHub Push, & Live CI / CodeQL Verification:**
   - Commit logically per subsystem (`Souvik Dey <imsovikde@gmail.com>`, SSH-signed, ZERO `Co-Authored-By` trailers):
     1) `core(ble,bia,audio)`: dual-scale `CS-BF01`/`Cult Pro` auto-detection, dynamic XOR, `4 x 0xDF` archival wait, `PRD:M-01..M-56` + plausibility alignment, `:core:audio` Hann-windowed PCM tones + regression tests
     2) `core(database,healthconnect,sync)`: reactive `ActiveMemberSession` + Member Lock, HC `ActiveCaloriesBurnedRecord` + external `WeightRecord` Room commit + reassignment HC orphan delete, Realtime `mutation_id` dedup + regression tests
     3) `ui(designsystem,feature)`: separated `DetachedObsidianDock` + fade scrim, `DRD:C-07` onboarding with equal-height permissions & bottom clearance, multi-weight fonts + `tnum`, Compose `@Preview`s, empty Home Bento, 20/56 interactive charts + working `selectedDate` calendar/scale filters + visual tests
     4) `app(release,security)`: hardened R8 rules + `r8-analyzer` Path A clean config, Play Protect/IntentSanitizer/Keystore hardening, `EndToEndAppAndVisualTest` local output path
     5) `ci,docs`: `.github/workflows/codeql.yml`, `.github/codeql/codeql-config.yml`, `.github/workflows/android-intelligent-release.yml`, `.github/dependabot.yml`, `scripts/bump_version.py`, `scripts/generate_release_notes.py`, attributed `README.md`, `HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`
   - Push to `origin main` via `git push`, confirm `gh api repos/imsovikde/OpenCult/commits/HEAD` reports `verified: true`, inspect `gh run list --limit 5` / `gh api repos/imsovikde/OpenCult/code-scanning/alerts` to confirm CodeQL and release workflows register and trigger cleanly on the public repository.
6. **Second Brain & Final Verified Report:**
   - Update `capsule:project:opencult` (`current-state`, `decisions`) and store a complete linked `handoff` entry in `second-brain` (`project="opencult"`).
   - Output exact executed commands, raw exit codes, Android CLI journey report summary, keystore `SHID` digests, GitHub workflow/CodeQL links, and explicit `Verified / Unverified / Unknown` boundaries before emitting `<!-- GOAL_COMPLETE -->`.
```
