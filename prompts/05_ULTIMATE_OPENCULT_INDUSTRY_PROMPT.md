# OpenCult Ultimate Industry-Grade End-to-End Prompt (Prompt 05)

Use this file verbatim as the final, self-contained engineering directive to remediate every defect in `app/`, `core/`, and `feature/`, align all 56 body-composition parameters (`PRD:M-01..M-56`), reverse-engineer and isolate Cult's two physical scales, maximize native Google Health Connect integration, add `:core:audio` PCM sonification, harden R8/Play Protect/Cloudflare security, deploy autonomous GitHub Actions intelligent releases with attribution `README.md`, inspect live emulator PNG renders visually, and push granular SSH-signed commits to `main`.

---

```text
You are operating as a principal Android systems architect, BLE reverse engineer, quantitative biophysics engineer, Jetpack Compose UI/motion specialist, acoustic UI sound designer, and DevSecOps release engineer inside `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

### 0. Non-Negotiable Long-Horizon Execution Contract (`/goal`, `ralph-loop`, `/boost`)

1. Mandatory Skills and MCP Servers (Load Before Writing Code):
   - Execution rigor: `ralph-loop` (`ralph 0.6.0`), `goal` (`/goal`), `boost` (`/boost`), `verification-before-completion`, `tdd`, `systematic-debugging`, `apex-bug-hunter`, `testing-setup`
   - Reverse engineering: `reverse-engineer-anything` + `rea` MCP server (`open_binary`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `trace_android_references`, `inspect_adb_client`, `list_adb_devices`, `pull_adb_file`, `inspect_packet_capture`, `build_reconstruction_obligation_ledger`, `evaluate_reconstruction_coverage`, `verify_reconstruction`, `list_unknowns`, `close_binary`)
   - Android security and Play Protect: `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `r8-analyzer`, `anr-audit-and-fix`
   - Cloudflare security and tooling: `cloudflare`, `workers-best-practices`, `cloudflare-one`, `wrangler`, `cf-cli`
   - Compose UI, device verification, and prose hygiene: `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`, `android-cli`, `humanizer`
   - Memory continuity: `second-brain` MCP server (`project="opencult"`)
2. Continuous Closed-Loop Verification:
   - Do NOT stop early, ask permission mid-run, commit untested or broken modules, skip emulator PNG screenshot checks, or leave changes uncommitted locally.
   - Follow `INV-23` atomically on every defect fixed: append a factual root-cause row to `HALL_OF_SHAME.md`, tighten or add a binary check in `INVARIANTS.md`, update `MICROENGINEERING.md` / `MOTIONENGINEERING.md` for UI/motion changes, and ship an automated regression test in the same commit.
   - Emit `<!-- GOAL_COMPLETE -->` ONLY after every unit/Robolectric test, R8 release APK verification (`apksigner`, `aapt2`, `rea`), emulator screenshot visual inspection, GitHub Actions YAML/script dry-run, attributed `README.md`, granular SSH-signed git push (`verified: true`), and Second Brain sync passes completely.
   - If hardware or interactive credentials block any single physical step (e.g., no physical BLE scale present on host or interactive email OTP), keep local replay/test harnesses isolated strictly under `src/test/` or explicit debug/inspector controls (never altering real measurements) and report exact `Verified / Unverified / Unknown` boundaries with raw errors (`AGENTS.md §0`).
3. Anti-Sophistry and Zero-Prose-Slop Rule:
   - Never fabricate metrics, BLE payload semantics, exit codes, timestamps, or test output (`AGENTS.md §0`).
   - Apply `humanizer` to every user-facing document (`README.md`, release notes, commit messages, UI copy). Reject em dashes, en dashes, curly quotes, `not-X-but-Y` contrasts, and banned AI buzzwords (`AGENTS.md §0.7`).

---

### 1. Pre-Edit Ground-Truth Audit and Workspace Defect Registry

Read these repository documents and reference files before touching Kotlin code:
- Governance & Specs: `AGENTS.md`, `TRD.md`, `PRD.md`, `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, `DRD.md`, `supabase/migrations/20261126000001_opencult_initial_schema.sql`.
- Second Brain Recall Gate: `list_recent(n=5)`, `list_projects()` (`opencult`), `recall(..., project="opencult", topK=8)`, `recall(..., tag="failure", hops=1)`, `brief(project="opencult")`, calling `get(id)` whenever `truncated == true`.
- Gitignored References under `Both/`:
  - `Both/openscale`: `CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `OneByoneLib.kt`, `RunstarR6Handler.kt`, `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt`
  - `Both/occult`: `CONTRACTS.md`, `research/cult_scale_protocol.md`, `js/ble/parser.js`, `js/metrics/bodycomp.js`, `daemon/internal/protocol/frame.go`, `daemon/internal/protocol/session.go`

Fix all 12 verified defects currently present in the uncommitted workspace (`app/`, `core/`, `feature/`) before building release artifacts:

1. Uncommitted Code + Absent Release CI / Attribution: Local Android modules (`app/`, `core/`, `feature/`, `gradle/`, `gradlew*`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `HALL_OF_SHAME.md` `SHAME-008..012`, `INVARIANTS.md` `INV-24..25`) are uncommitted; `.github/workflows/` is missing; `README.md` omits full attribution and dynamic release badges.
2. `DetachedDock.kt` Layout + Chart Collision: `DetachedDock.kt` (`L72-113`) places `CobaltMeasureFab` (`+`) inside the `#0F1115` capsule (`DRD:C-05` violation), while scrolled bars/cards in `StippledBarChart` overlap beneath the translucent dock region (`visual_dashboard_daylight.png`). Separate the 4-tab `#0F1115` capsule (`weight = 1f`) from the `64.dp` `#2185F7` FAB by `12.dp`, place a smooth translucent canvas fade scrim behind the dock, and verify `dockScrollClearance()` across every scrollable screen.
3. `OnboardingScreen.kt` UI Breakage, Button Wrapping, Chip Clipping, & Third-Scale Option: `Allow Bluetooth` (1 line) and `Allow Health Connect` (wraps to 2 lines at `L176-186`) break button alignment; `"Heavy"` chip (`L263+`) clips at the bottom without `.navigationBarsPadding()`; stock Material `OutlinedTextField` boxes violate `DRD.md`; the plain stacked form discards `DRD:C-07` sky-wash breakout card previews; and `L196-204` displays 3 scale chips (`Cult Pro 8E` / `Pro Feet-Only` / `CS-BF01 4E`) even though Cult only sells **two** physical scales (**Cult Smart Scale Pro** and **Cult Smart Scale**).
4. `ScaleFrameParser.kt` + `CultBleConstants.kt` + `CultBleController.kt` Protocol Bugs:
   - `ScaleFrameParser.parse0xCf` (`L124-137`) swaps `CS-BF01` bytes by reading `((b2 shl 8) or b1) / 10.0` as impedance and suppressing `heartRateBpm` when `expectedTier == CULT_BF01_4E`. Support BOTH `OneByone` classic (`bytes[1..2]` impedance) and `CS-BF01` (`bytes[1]` HR `bpm`, `bytes[5..6]` LE uint16 impedance) via explicit hardware sub-model classification + plausibility checks (`150..1200 ohm`).
   - `CultBleConstants.buildModeSetupCommand` (`L33`) hardcodes `0xCB` XOR even when `unitKg = false`; compute `xor(bytes[0..9])` dynamically across all outbound frames.
   - `CultBleController.onDiscovered` (`L198-206`) guesses scale tier solely by substring (`BF01`/`1byone`) instead of pairing name rules with live GATT/frame classification (`0xBE`/`0xDF`/`0xCF` status `0x05`/`0x08` vs `0xCF`-only `0x00`/`0x01`/`0x36`), sends `OneByone` `F2 00` history requests blindly to Pro units, and performs zero automatic reconnect to saved `scale_devices` on app launch.
5. `ScaleSessionStateMachine.kt` Incomplete `0xDF` Archive + Disconnect Bug: Calling `maybeComplete8ESession()` on the first `0xDF` frame (`L108-111`) or on `status == 0x05` (`L167-168`) can lock and finish before `0xDF` packets `#2..#4` arrive. Track all `4 x 0xDF` frames cleanly (with a bounded `1200 ms` grace timeout after `0xBE` Sets A/B complete) so every raw packet (`0xCF`, `2 x 0xBE`, `4 x 0xDF`) persists verbatim (`INV-01`).
6. `BodyMetricsEngine.kt` & `MeasurementDetailScreen.kt` `PRD:M-01..M-56` Drift + Formatting Bug:
   - `BodyMetricsEngine.kt` (`L318-389`) shifts several `M-01..M-56` codes away from `PRD.md` (omitting `WHO BMI Category`, `Obesity Degree`, `Fat Mass Index`, `Fat-Free Mass Index`, `TBW Volume L`, `ECW %`, `ICW %`, `Cardiac Index Score` while inserting `M-56 Waist-to-Hip Estimate`). Re-align `PRD:M-01..M-56` 1:1 across `BodyMetricsEngine`, `PRD.md`, and `MeasurementDetailScreen`.
   - `MeasurementDetailViewModel.load()` (`MeasurementDetailScreen.kt:91-106`) rebuilds metadata with hardcoded `ScaleTier.CULT_PRO_FEET_ONLY` and formats ALL values as `%.2f` (turning BMR `1742` into `1742.00` and HR `68` into `68.00`). Preserve per-metric precision (`0`, `1`, `2` decimals or category label) and compute provenance cleanly from the stored tier (`measurements.source`).
7. `Type.kt` + `res/font/` Single-Weight Fonts: `core/designsystem` ships only `plus_jakarta_sans_regular.ttf` and `jetbrains_mono_regular.ttf`, simulating Bold/SemiBold via `FontVariation`. Bundle true static TTFs (`400/500/600/700/800` for `Plus Jakarta Sans`; `400/500/700` for `JetBrains Mono`) with mandatory `fontFeatureSettings = "tnum"` (`INV-12`).
8. `HomeScreen.kt` Empty State, Small Pager Targets, & Shipping Seed Helper: When `measurements` is empty, `HomeScreen` hides the Bento Grid completely (leaving 60% blank canvas), renders `24.dp` pager arrows (`INV-18` violation -> `48.dp` min hitbox), and exposes `seedSampleMeasurementForVerification()` inside `HomeViewModel` (`PRD:NG-05` violation -> delete from main source; keep test fixtures inside `src/test/` only).
9. `ReportsScreen.kt` Broken Calendar Filter, Hardcoded `16.5f` Gauge, & Missing 20/56 Drill-Down Charts: `selectedDate` from `OpenCultCalendarSheet` never filters `filtered` or `bars`; empty `latestBf` hardcodes `16.5f`; and users cannot click any of the **20** (`Cult Smart Scale`) or **56** (`Cult Smart Scale Pro`) parameters across Home, Body Map, Measurement Detail, or Reports to open interactive trend charts and multi-axis filters.
10. Non-Reactive Member State + Missing Member Lock + Orphan Health Connect Bug: `ReportsViewModel` and `BodyMapViewModel` read `vault.getActiveProfileId()` once inside `combine()`, failing to update when switching members; `WeighInScreen` lacks a `Lock to Active Member` toggle; and `MeasurementDetailViewModel.reassign()` (`L128-136`) overwrites `profileId` without deleting Member A's prior Health Connect records or recalculating BIA metrics for Member B's sex/age/height!
11. `HealthConnectGateway.kt` + `InspectorScreen.kt` Gaps: `readDailyContext` lacks `ActiveCaloriesBurnedRecord` fallback; `InspectorViewModel.pollExternalHc()` discards imported external `WeightRecord` items instead of writing them into Room; `AndroidManifest.xml` omits `READ_HEALTH_DATA_HISTORY` / background read support where user opts into multi-month backfill/background polling; and `Health Connect` readiness/controls need first-class UI placement across Onboarding, Home, and the Sync Hub.
12. Missing `:core:audio` Sound Synthesizer + Hardcoded Host Screenshot Path: No PCM acoustic feedback exists on connect/settle/lock/log/error, and `EndToEndAppAndVisualTest.kt` (`L78-80`) writes to hardcoded `C:\Users\imsov\.gemini\...` instead of `build/outputs/visual_screenshots/`.

---

### 2. Deep Dual-Scale Reverse Engineering (`reverse-engineer-anything` + `rea` MCP)

Model ONLY the two physical retail Cult scales during Onboarding, Pairing, and Saved Scale settings (NEVER show a third fake scale option in setup):

1. Cult Smart Scale (`CS-BF01`, 4-Electrode, 20-Parameter Model):
   - Auto-recognize by BLE advertised name (`CS-BF*`, `Cult Smart Scale`, `Cultsport*`, `OKOK*`, or `OneByone`/`Health Scale`/`Eufy T914*` for legacy OneByone compatibility) AND confirmed on the wire by `0xFFF0`/`0xFFF4` delivering 11-byte or `>= 18`-byte `0xCF` frames with NO `0xBE`/`0xDF` packets.
   - Handshake on `0xFFF1`:
     - Send mode/unit command `FD 37 <unit: 00=kg, 01=lb, 02=st> <group=01> 00 00 00 00 00 00 <xor[0..9]>` (checksum computed dynamically for EVERY unit, fixing `0xCB`).
     - Send RTC sync `F1 <year_hi> <year_lo> <month> <day> <hour> <minute> <second>`, arming a `3000 ms` fallback timer (`OneByoneHandler` pattern) in case `F1 00` ACK never arrives.
     - Request offline history `F2 00` -> on `F2 00` end marker, clear via `F2 01` ONLY if genuine `>= 18`-byte history frames were received (`!isCoalescedDuplicate`).
   - Exact `0xCF` Decoding (`ScaleFrameParser.parse0xCf`):
     - `bytes[0] == 0xCF`, `bytes[10] == xor(bytes[0..9])`.
     - `weightKg = uint16_le(bytes[3..4]) / 100.0`.
     - Enforced `INV-21` duplicate guard (`SHAME-005`): if `bytes.size >= 18 && liveChecksumOk && bytes[11] == 0xCF`, classify as `coalescedDuplicateDropped = true` and `isHistory = false`; otherwise `>= 18` bytes with valid `2020..2099` timestamp `[11..17]` parses as history.
     - `bytes[9]`: `0x01` = settling; `0x00` = locked (`CS-BF01` body phase); `0x36` = OneByone classic locked. Enforce `WeightLockFrames = 3` and `HRLockFrames = 3` debouncing (`Both/occult/daemon/internal/protocol/session.go`).
     - Handle known firmware split (`Both/occult/research/cult_scale_protocol.md` vs `OneByoneHandler.kt`):
       - On `CS-BF01` / `Cult`: `bytes[1]` carries standing Heart Rate (`bpm` if steady `in 35..220` during locked `0x00` phase; `null` if `0` or settling); `bytes[5..6]` LE uint16 is checked strictly against physiological plausibility (`150..1200 ohm`, scaling `raw` or `raw / 10.0` ONLY when consistent across consecutive frames). Many retail `CS-BF01` firmwares emit transient non-impedance noise in `bytes[5..7]` and keep BIA in an unexposed encrypted app layer; when impedance fails plausibility, NEVER invent or spoof an ohm figure. Compute the **20 anthropometric parameters** honestly from verified weight, optional HR, and member profile (`MetricProvenance.ANTHROPOMETRIC_ESTIMATE`), labeling provenance explicitly (`INV-02`).
       - On `OneByone` / `Eufy` legacy sub-models: retain `bytes[1..2]` `/ 10.0` single-freq ohm extraction so all upstream `OneByone` unit vectors keep passing.
   - Auto-display all **20 active `CS-BF01` parameters** cleanly across Home, Measurement Detail, Body Map (showing aggregate model view with honest 8E segmental lock banner), and Reports trend charts.

2. Cult Smart Scale Pro (8-Electrode Dual-Frequency Handle, 50+ / 56-Parameter Model):
   - Auto-recognize by BLE advertised name (`CULT*` excluding `CS-BF*`) AND confirmed dynamically on the wire by `0xCF` handle states (`0x08` = impedance measuring, `0x05` = handle measurement complete) + `2 x 13B 0xBE` (`0x00` Set A low freq, `0x01` Set B high freq) + `4 x 17B 0xDF` packets (`INV-01`).
   - Exact `0xBE` Physics (`CultSmartScaleProHandler` + `BodyPod2Handler` reconciled):
     - Each `13B` packet carries `bytes[0]=0xBE`, `bytes[1]=index (0x00 or 0x01)`, 5 x `uint16_le` channels at `[2..11]`, and `bytes[12]=xor(bytes[0..11])`.
     - Dividing ALL 5 channels by `10.0` (`0.1 ohm`) yields real physiological segmental resistances `[Right Arm ~280-320 ohm, Left Arm ~280-320 ohm, Trunk ~24-35 ohm, Right Leg ~220-295 ohm, Left Leg ~220-295 ohm]` matching `CultSmartScaleProHandler`'s observed integers (`~2867, ~3013, ~248, ~2904, ~2668`), and forming whole-body wrist-to-ankle impedance `((RA + LA) / 2) + TR + ((RL + LL) / 2)` around `520..620 ohm` low/high band!
     - Store BOTH raw `uint16_le` values and `0.1 ohm`-scaled segmental arrays verbatim inside `raw_ble_packets.decoded_impedances_ohm`, and capture all `4 x 17B 0xDF` frames verbatim (`seqA = bytes[2]`, `seqB = bytes[3]`, `xor` verified, waiting up to `1200 ms` after `0xBE` before finalizing completion).
     - Never fabricate unverified `0xDF` decryption (`INV-01`, `INV-02`). Immediately compute and display all **56 parameters (`PRD:M-01..M-56`)**.
   - Automatic Runtime Downgrade (`Cult Smart Scale Pro Feet-Only Mode`):
     - If a user steps on the `Cult Smart Scale Pro` without holding the handle bars, the scale emits `0xCF status = 0x00` with no `0xBE` handle packets. After a short `3000 ms` grace timeout (or 1-tap `Confirm Weight Only`), classify ONLY that weigh-in session as `ScaleTier.CULT_PRO_FEET_ONLY`, log verified `Weight` (+ `HR` if measured), suppress 8E segmental/ECW/ICW/BIA compartments cleanly (`INV-03`), and write `WeightRecord` only to Health Connect.

3. One-Time Pairing, Foreground Launch Auto-Reconnect, & `rea` Binary Audit:
   - Persist paired scale MAC address, advertised name, and resolved `model_tier` (`cult_pro_8e` vs `cult_bf01_4e`) in Room `scale_devices`.
   - Whenever the user opens OpenCult (`Lifecycle.State.STARTED` on `Home` or `WeighIn` with `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` granted and Bluetooth adapter ON), automatically start bounded passive scan / GATT auto-reconnect to saved `scale_devices`. Stepping onto the paired scale opens/updates the live weigh-in experience and logs automatically with zero re-pairing clicks.
   - Surface an instant non-intrusive card/prompt if Bluetooth permissions (`Never for Location` on API 31+) or the Bluetooth adapter itself are disabled (invoking `BluetoothAdapter.ACTION_REQUEST_ENABLE`).
   - Run `rea` MCP (`open_binary`, `inspect_android_package`, `evaluate_reconstruction_coverage`, `close_binary`) against any user-supplied vendor APK / `btsnoop_hci.log` (if present on device or workspace) AND against your own built `app/build/outputs/apk/release/app-release.apk` to verify obfuscation, manifest safety, and zero leftover test symbols.

---

### 3. Biophysical Equations and Exact 56-Parameter Alignment (`:core:bia`)

Align `BodyMetricsEngine` + `PRD.md` + `MeasurementDetailScreen` 1:1 on all `PRD:M-01..M-56` definitions and units:

1. Plausibility Gates (`INV-03`): Require `age in 10..120`, `heightCm in 80.0..250.0`, `weightKg in 10.0..350.0`. For 8E (`Cult Pro`), require valid limb ranges (`50..700 ohm`), trunk (`5..120 ohm`), whole-body `Z_50` (`150..1200 ohm`), and `|Z_low - Z_high| / Z_high >= 0.01`; on failure, downgrade session to `CULT_PRO_FEET_ONLY` (`contactValid = false`) and suppress BIA compartments cleanly.
2. Exact Dual-Frequency & Anthropometric Equations (retained GPL-3.0 upstream headers citing `openScale`: `StandardImpedanceLib`, `S400BodyComposition`, `Wla25BodyComposition`):
   - Whole-body FFM/TBW (`Sun 2003` + Pace & Rathbun `0.732 * FFM`) on valid 8E/4E BIA; honest `Deurenberg 1991` BF% + `Watson 1980` TBW on anthropometric 4E fallback (`ANTHROPOMETRIC_ESTIMATE`).
   - Dual-frequency Cole inversion via bisection (`R0`, `Rinf`) -> `De Lorenzo Eq. B2` `ECW` -> `ICW = TBW - ECW` -> `ECW / TBW` ratio -> `Wang 2004` `BCM = ICW / 0.70` (gated strictly to 8E handle mode).
   - `Janssen 2000` `SMM` & `SMI`; `FFM - boneMass` total muscle; `Katch-McArdle` (BIA) vs `Mifflin-St Jeor` (fallback) `BMR`; TDEE sedentary & active multipliers; Devine Ideal Weight & Obesity Degree; Composite Health Score & Metabolic Age; FMI & FFMI; Subcutaneous/Visceral Fat; Cardiac Index Score (when HR captured).
   - Mass-conserving 5-segment (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`) muscle and fat split summing cleanly within `<= 0.05 kg` of total SMM and Fat Mass, plus bilateral upper/lower symmetry % and `ALM / ASMI`.
3. Per-Metric Display Formatting & Deterministic Re-Derivation:
   - Store decimal precision on each `BodyMetricItem` (`0` for BMR/TDEE/Metabolic Age/Heart Rate/Score, `1` for %/Visceral/Category idx, `2` for kg/liters/BMI/SMI/ratios, text label for WHO BMI Category).
   - Expose `BodyMetricsEngine.recalculateForProfile(measurement, rawPackets, newProfile)` so reassigning an existing measurement from Member A to Member B recomputes all derived fields cleanly from immutable BLE frames (`INV-01`).

---

### 4. Deep Native Google Health Connect Integration (`:core:healthconnect`, `connect-client:1.1.0`)

Build Google Health Connect end to end grounded in the real AndroidX `connect-client:1.1.0` schema catalog:

1. Exact Record Coverage Truth (41 Concrete Records in `connect-client:1.1.0`):
   - Google Health Connect `1.1.0` provides 8 core scale-compatible native records (plus `RestingHeartRateRecord` = 9 optional) and does NOT expose custom keys or native record classes for BMI, Visceral Fat Index, Subcutaneous Fat %, ECW/ICW/BCM, Skeletal Muscle Mass (`SMM` separate from `LeanBodyMassRecord`), Metabolic Age, 5-Segment Arm/Leg/Trunk Muscle & Fat, Bilateral Symmetry, Impedance Ohm, or Composite Score.
   - Automatically write every supported record type cleanly using `Instant`, `ZoneOffset`, `Device(Device.TYPE_SCALE, "Cult.fit", resolvedModelName)`, `Metadata.autoRecorded(...)` (for BLE weigh-ins) or `Metadata.manualEntry(...)` (for manual logs), deterministic `clientRecordId = "opencult:${measurement.id}:${tag}"`, and incrementing `clientRecordVersion`:
     1) `WeightRecord` (`Mass.kilograms`)
     2) `BodyFatRecord` (`Percentage`, range `1.0..99.0`)
     3) `LeanBodyMassRecord` (`Mass.kilograms`, FFM)
     4) `BoneMassRecord` (`Mass.kilograms`, BMC)
     5) `BodyWaterMassRecord` (`Mass.kilograms`, TBW mass in `kg`)
     6) `BasalMetabolicRateRecord` (`Power.kilocaloriesPerDay`)
     7) `HeartRateRecord` (`startTime`, `endTime = +1s`, `samples = [Sample(time, bpm)]`, written only when scale emits valid HR)
     8) `HeightRecord` (`Length.meters`, written on primary profile create/height edit)
     9) Optional opt-in `RestingHeartRateRecord` (`beatsPerMinute`, disabled by default with explicit Settings toggle so post-activity weigh-ins do not skew clinical resting HR unless requested)
   - `INV-02`/`INV-03` Health Connect Write Guard: On `CULT_PRO_FEET_ONLY` (`status = 0x00` weight-only), write `WeightRecord` (+ `HeartRateRecord` if captured) ONLY; never write invented fat/bone/water records to Health Connect. For all 20 (`CS-BF01`) and 56 (`Cult Pro 8E`) parameters beyond Health Connect's 8/9 native schemas, keep 100% lifetime queryability in Room, Supabase, CSV/JSON exports, and in-app interactive charts.
2. Inbound Sync, Permissions, Historical Read, & Lifecycle Purge:
   - Read daily `StepsRecord.COUNT_TOTAL`, `SleepSessionRecord.SLEEP_DURATION_TOTAL`, and `TotalCaloriesBurnedRecord.ENERGY_TOTAL` with **automatic fallback to `ActiveCaloriesBurnedRecord.ENERGY_TOTAL`** in `HealthConnectGateway.readDailyContext`.
   - Fix `pollExternalWeights`: persist external `WeightRecord` entries (`dataOrigin.packageName != BuildConfig.APPLICATION_ID`) into Room `measurements` (`source = "health_connect_import"`) using deterministic deduplication (`INV-05`), plus propagate external `DeletionChange`s.
   - Declare `android.permission.health.READ_HEALTH_DATA_HISTORY` (for >30-day backfill/history queries) and optional opt-in `READ_HEALTH_DATA_IN_BACKGROUND` alongside rationale `MainActivity` + Android 14+ `ViewPermissionUsageActivity` `<activity-alias>`. Guard partial permission grants (`SecurityException` safe per record type) and keep `health_connect_sync_ledger` (`Synced / Pending / Failed / Deleted / version`) strictly accurate.
   - **Mandatory Orphan Delete:** Both `softDeleteMeasurement()` AND `reassignMeasurement()` (from Member A -> Member B) MUST delete old Health Connect records first (`deleteSyncedRecordsForMeasurement`), recompute metrics for Member B, and re-insert ONLY if Member B has `isPrimaryHcUser = true`.
3. Dedicated Health Connect UI Placement:
   - Onboarding Step 2: Prominent equal-height `Health Connect` readiness card detailing exact Write (`Weight`, `Body Fat`, `Lean Mass`, `Bone Mass`, `Body Water`, `BMR`, `Heart Rate`, `Height`) + Read (`Steps`, `Sleep`, `Calories`, `External Weight`) access with zero wrapped-button clipping.
   - Home Dashboard: Live `Health Connect` sync status chip (`Synced X / 8` or `Grant Perms`) + Bento Overview Page 2 (`Steps`, `Sleep`, `Calories` live tiles).
   - Inspector / Health Connect Hub (`DRD:S-08`): Permission readiness meter (`Granted / Total`), 1-tap `Grant Missing Permissions`, 1-tap `Backfill All History` with live progress, `Sync External Weights Now`, toggle for optional `RestingHeartRateRecord` / background poll, and scrollable per-record write ledger.

---

### 5. Household Isolation, Lock Mode, & Zero-Refresh Realtime Sync (`:core:database` + `:core:sync-supabase`)

1. Single Reactive Session Member Coordinator: Maintain `activeProfileId: StateFlow<String?>` in an injected singleton coordinator consumed reactively by `MainViewModel`, `HomeViewModel`, `WeighInViewModel`, `BodyMapViewModel`, `ReportsViewModel`, `ProfilesViewModel`, and `MeasurementDetailViewModel`. Switching active member updates Home, Body Map, and Reports in `< 16 ms`.
2. Explicit `Lock to Active Member` Toggle (`WeighIn` + `Home`):
   - **Locked ON (Selected Member Priority):** Every weigh-in logs straight to the locked member (`assignmentState = "confirmed"`) with zero ambiguity popups, computing that member's BIA/anthropometric metrics, saving in `< 50 ms`, enqueueing Supabase outbox, and syncing Health Connect if `isPrimaryHcUser`.
   - **Auto-Match Mode (Shared Household, `INV-09`):** Single match within `±auto_match_window_kg` auto-confirms; `0` or `2+` matches set `assignmentState = "pending_review"` and pop the member confirmation sheet immediately (`WeighInScreen` + `Home`).
3. Zero-Refresh Postgres Realtime (`INV-06`, `INV-07`, `INV-08`):
   - Maintain offline-first Room (`< 50 ms` atomic writes indexed by `(profile_id, local_date, measured_at)`), `FORCE ROW LEVEL SECURITY` scoped by `(SELECT auth.uid()) = account_id`, and live `postgresChangeFlow` convergence across `measurements`, `household_profiles`, `scale_devices`, and `health_connect_sync_ledger` with `mutation_id` self-echo filtering. Zero auto-generated/demo weigh-ins anywhere in shipping code.

---

### 6. Acoustic UI Sound Engineering (`:core:audio` `OpenCultSoundEngine`)

Do NOT play harsh system notification ringtones (`RingtoneManager` / notification channel pings). Build a dedicated `:core:audio` synthesizer backed by `android.media.AudioTrack` (`AudioAttributes.USAGE_ASSISTANCE_SONIFICATION`, `CONTENT_TYPE_SONIFICATION`, `MODE_STATIC`, 44.1 kHz 16-bit PCM mono) with `5 ms..8 ms` raised-cosine Hann attack/release envelopes to eliminate DC pops or clipping:

1. `SCALE_CONNECTED`: Gentle warm major-fifth crystalline chime (`C5 523.25 Hz` -> `G5 783.99 Hz`, `80 ms` each, `-14 dBFS`).
2. `SETTLING_TICK`: Quiet crisp wooden/tactile pulse (`880 Hz`, `12 ms` envelope, `-18 dBFS`) once per stable weight increment (`1/3`, `2/3`).
3. `WEIGHT_LOCKED`: Clean medical-instrument dyad (`D5 587.33 Hz` + `A5 880.00 Hz`, `140 ms` soft decay, `-12 dBFS`), strictly deduplicated once per lock event (`INV-21`).
4. `METRICS_LOGGED_PRO_50` / `METRICS_LOGGED_BASIC_20`: Bright warm triad (`C5 -> E5 -> C6`, `220 ms` tail on 8E / crisp double note `C5 -> G5` on 4E) fired when calculation + Room commit + Health Connect dispatch completes.
5. `AMBIGUOUS_MEMBER_PROMPT`: Neutral balanced two-note cue (`A4 440 Hz` -> `G4 392 Hz`, `150 ms`) when user confirmation is needed.
6. `CAPTURE_ERROR`: Soft muted low interval (`220 Hz` -> `165 Hz`, `110 ms`, `-16 dBFS`) on XOR checksum failure or premature step-off.
- Respect Android ringer/media/muted audio routing (`AudioManager.RINGER_MODE_SILENT` / `VIBRATE`), pool/release `AudioTrack` instances cleanly on a single dedicated coroutine scope with zero main-thread blocking (`ANR`-safe), expose a persistent `Scale Sound Feedback` switch in Settings (`SecureVault`), and cover waveform generation (sample count, zero NaN/clipping, Hann envelope endpoints `== 0`) with pure unit tests.

---

### 7. Micro-Engineered UI, Typography, Full 20/56 Charts & Lifetime Filtering (`MICROENGINEERING.md` + `MOTIONENGINEERING.md` + `DRD.md`)

1. Banned vs Required UI Controls:
   - Strictly reject stock `DatePickerDialog`, Material `DatePicker`, `Spinner`, `DropdownMenu`, stock `OutlinedTextField` lookalikes in Onboarding/Settings, raw Material `NavigationBar`/`Card`/`FloatingActionButton`, and third-party chart libraries (MPAndroidChart, Vico).
   - Use custom `Canvas` primitives (`DRD:C-01..C-06`), custom `OpenCultCalendarSheet`, custom `OpenCultPillSelectorSheet`, custom squircle inputs (`22.dp` radius, `#EEF1F6`/`#1C222E` track, hairline focus state), `OpenCultMotion` specs (`INV-19`, `INV-20`), `>= 48.dp` interactive hitboxes (`INV-18`), and 4-stage micro-press feedback.
2. Exact Visual Fixes Across Screens:
   - `DetachedObsidianDock` + Translucent Scrim (`DRD:C-05`, `INV-13`): Draw the `4`-tab `#0F1115` capsule (`height = 64.dp`, `weight = 1f`) on the left, `12.dp` horizontal gap, and detached `64.dp` `#2185F7` `+` FAB on the right, over a smooth translucent bottom background gradient scrim fading canvas content out cleanly so no chart bar or card bleeds visibly across the dock.
   - True Multi-Weight Typography (`Type.kt` + `res/font/`): Include real static TTF weights (`400`, `500`, `600`, `700`, `800` for `Plus Jakarta Sans`; `400`, `500`, `700` for `JetBrains Mono`) and enforce `tnum` on every numeric field (`INV-12`).
   - Sleek `DRD:C-07` Onboarding Carousel: Daylight Pastel wash (`#CBE1F7 -> #F4F6FA`), breakout miniature Home hero card preview (`Step 1`), equal-height aligned Permissions + Hardware Auto-Discovery (`Step 2`, ZERO text-wrap distortion, Health Connect + Bluetooth badges + 2-scale physical recognition explanation), and uncluttered Member Anthropometry setup (`Step 3`, full `.navigationBarsPadding()`, zero bottom chip clipping, zero fake 3rd hardware radio chip).
   - Always-Complete Home Dashboard (`HomeScreen.kt`): Display Hero card (with connected/saved scale pill + Member Lock badge), full asymmetric Bento Overview grid even before the user's first weigh-in (showing Health Connect Page 2 Steps/Sleep/Calories right away, plus calm empty baselines on Page 1), `48.dp` clickable `<` / `>` pager buttons, and dedicated Health Connect readiness/sync summary.
   - Clickable 20 / 56 Parameter Charts + Multi-Axis Filtering (`Home`, `MeasurementDetail`, `BodyMap`, `Reports`):
     - Clicking ANY of the **20 (`CS-BF01`)** or **56 (`Cult Pro 8E`)** parameters opens an interactive custom `Canvas` Trend Explorer screen/sheet displaying full lifetime history, zone-colored target bands, bezier curve + point selection scrubbing, delta vs baseline/prior weigh-in, and raw packet provenance links.
     - Wire `ReportsScreen`'s `OpenCultCalendarSheet` `selectedDate` / custom date window directly into `filtered` and `bars` alongside `7D / 30D / 90D / 1Y / Lifetime All`, **Scale Filter** (`All Scales`, `Cult Pro 8E`, `CS-BF01 4E`, `Feet-Only`, `HC Import`), **Active Member Switcher**, and **Metric Selector (all 20 / 56 params)**, fixing empty gauge fallback (display `-- %` / honest empty prompt instead of `16.5f`).

---

### 8. Security, Persistent Release Keystore (`SHID`), Hardened R8, Play Protect & Cloudflare Rules

1. Release Signing & Keystore (`app/build.gradle.kts`):
   - `applicationId = "org.opencult.scale"`, `minSdk = 31`, `compileSdk = 36`, `targetSdk = 36`.
   - Maintain gitignored RSA-4096 PKCS12 `keystore/opencult-release.jks` + `keystore.properties` (with CI env fallbacks), `enableV1Signing = true`, `enableV2Signing = true`, hard-failing any `:app:assembleRelease` / `:app:bundleRelease` build missing release credentials (zero debug fallback). Record non-secret `SHA-1` and `SHA-256` digests (`SHID`).
2. Hardened R8 (`app/proguard-rules.pro`):
   - Enforce `isMinifyEnabled = true`, `isShrinkResources = true`, `-repackageclasses 'o'`, `-allowaccessmodification`, `-renamesourcefileattribute SourceFile`, `-keepattributes SourceFile,LineNumberTable`, `-assumenosideeffects` stripping `android.util.Log.v/d/i`, plus tested keep rules for Kotlinx Serialization (`$$serializer`), Supabase Auth/PostgREST/Realtime DTOs, Hilt workers, Room entities/DAOs, `blessed-kotlin`, and Health Connect records. Audit R8 output (`mapping.txt` + `app-release.apk`) using `r8-analyzer` and `rea` MCP.
3. Play Protect & Cloudflare Compliance:
   - `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk` must show zero restricted permissions (`BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, `REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, location permissions, external storage permissions, `BLUETOOTH_ADVERTISE`).
   - Enforce `allowBackup="false"`, `usesCleartextTraffic="false"`, `android:exported="false"` on internal components, `IntentSanitizer` on `opencult://auth/callback`, `PendingIntent.FLAG_IMMUTABLE`, Android Keystore AES-256-GCM token storage (`SecureVault`), scoped `FileProvider` `/exports/`, and Cloudflare repo/edge security rules (`crypto` CSPRNG if any worker exists, timing-safe secret comparisons, zero leaked keys/service roles, `INV-15`, `INV-24`). Run `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, and `anr-audit-and-fix`.

---

### 9. GitHub Actions Intelligent Release Pipeline & Open-Source Attribution `README.md`

1. Create `.github/workflows/android-intelligent-release.yml`, `scripts/bump_version.py`, and `scripts/generate_release_notes.py`:
   - Triggers: `push` to `main` (ignoring `[skip ci]` release bump loops cleanly), `v*` tags, `pull_request` to `main`, and `workflow_dispatch` (optional `bump_type`: `patch`/`minor`/`major`/`none` + `release_channel`).
   - Autonomous Version Sync (`scripts/bump_version.py`): Increments `versionCode` + semantic `versionName` across `app/build.gradle.kts` AND `README.md` release badges/table; UI "About / Diagnostics" screens read `BuildConfig.VERSION_NAME` & `BuildConfig.VERSION_CODE` dynamically so app UI and repo metadata always match.
   - CI Build, Sign, & Security Gate: Uses JDK 17, Android SDK `36` + `build-tools;36.0.0`, restores `keystore/opencult-release.jks` from `OPENCULT_KEYSTORE_BASE64` / password / alias GitHub Secrets cleanly (never checked in), injects `OPENCULT_SUPABASE_URL` & `OPENCULT_SUPABASE_ANON_KEY`, runs unit tests, `:app:lintRelease`, `:app:assembleRelease`, `apksigner verify --verbose --print-certs` (capturing `SHA-1`/`SHA-256` `SHID` digests and `.sha256` checksum), and `aapt2 dump permissions` security check.
   - Adaptive Intelligent Release Notes (`scripts/generate_release_notes.py`): Analyzes git log, module diff stats (`:core:ble`, `:core:bia`, `:core:audio`, `:core:healthconnect`, `:core:database`, `:core:sync-supabase`, `:core:designsystem`, `:feature:*`), APK byte size, keystore `SHID` digests, and permission diff. When a lightweight AI key is configured (`GITHUB_TOKEN` GitHub Models e.g. `gpt-4o-mini` or `GEMINI_API_KEY` flash-lite via official HTTPS API), synthesizes clean humanized release highlights validated against `AGENTS.md §0.7`; when unavailable/offline, falls back deterministically to structured commit/diff release notes with zero network failure! Publishes GitHub Release (`softprops/action-gh-release@v2`) attaching `opencult-v<version>-release.apk`, `.sha256`, R8 `mapping.txt`, `metadata.json`, `RELEASE_NOTES.md`, and source archive, and posts PR diff intelligence comments.
   - Validate workflow YAML syntax and dry-run `bump_version.py` & `generate_release_notes.py` locally before committing.
2. Sophisticated Open-Source `README.md`:
   - Apply `humanizer` before finalizing. Include live version/CI badge markers synced by CI, verified release APK download & `SHA-256` (`SHID`) verification steps, Cult two-scale matrix (**Cult Smart Scale Pro 56-Param** vs **Cult Smart Scale `CS-BF01` 20-Param**), BLE protocol & dual-frequency summary, Health Connect read/write compatibility table (explicitly noting the 8/9 native Health Connect record classes vs full 56 in-app/export catalog), Supabase RLS summary, build/signing guide, complete attribution (`oliexdev/openScale` `[GPL-3.0]`, `anshuman852/occult` `[MIT]`, `weliem/blessed-kotlin`, Jetpack Compose, `androidx.health.connect`, Supabase Kotlin SDK, Material Icons Extended), and nominative trademark notice (`AGENTS.md §3.5`).

---

### 10. Visual Emulator Audit, Regression Tests, Granular Commits, & Final Proof

Execute EVERY step below after your final source edit:

1. Automated Verification Suite:
   - Move `EndToEndAppAndVisualTest.kt` screenshot output to project-local `build/outputs/visual_screenshots/` (no hardcoded home paths).
   - Add automated unit/Robolectric/Compose regression tests covering ALL 12 remediated defects (`CS-BF01` HR + impedance + `FD 37` dynamic XOR, `4 x 0xDF` archival wait, `PRD:M-01..M-56` parity & formatting, `:core:audio` PCM envelope & zero-clipping bounds, Member Lock isolation, `selectedDate` calendar filter + 20/56 metric drill-down, `ActiveCaloriesBurnedRecord` fallback + external `WeightRecord` Room commit + reassignment Health Connect orphan purge, `DetachedObsidianDock` separated geometry + scrim clearance, `48.dp` min touch targets, multi-weight fonts + `tnum`).
   - Run `./gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleRelease`. Every task must exit `0` with zero skipped/failing assertions.
2. Live Android Emulator Verification & Visual Inspection:
   - Install release build cleanly (`adb install -r app/build/outputs/apk/release/app-release.apk`; resolve signature mismatch via clean uninstall if prior debug APK exists).
   - Exercise cold launch and navigation across **Onboarding (Steps 1, 2, 3)**, **Home (Empty + Populated, Daylight + Obsidian, Health Connect Page 2)**, **Weigh-In (Member Lock + Live/Simulated Pro 8E & CS-BF01)**, **Measurement Detail (56 / 20 params + Reassign)**, **Body Map**, **Reports (Arc Gauge + Stippled Chart + Calendar Filter + Individual Metric Trend Visualizer)**, **Profiles**, **Health Connect Hub**, and **Inspector**.
   - Capture PNG screenshots (`adb exec-out screencap -p` / `android` CLI) and read EVERY PNG visually with `read_file` to prove zero wrapped permission buttons, zero bottom chip clipping, zero dock/chart bleed-through, crisp typography, and exact `DRD.md` Daylight + Obsidian contrast. Confirm zero `FATAL EXCEPTION` or `ANR` lines in `adb logcat`.
   - Run `apksigner verify --verbose --print-certs`, `aapt2 dump permissions`, `rea` (`open_binary` + `inspect_android_package`) on `app-release.apk`, and automated prose/secret/`git check-ignore Both keystore.properties .env.local` gates (`exit 0`).
3. Granular Signed Commits & GitHub Push:
   - Commit logically by subsystem (`Souvik Dey <imsovikde@gmail.com>`, SSH-signed, zero `Co-Authored-By` trailers):
     1) `core(ble,bia,audio)`: protocol auto-detection, dynamic XOR, `0xDF` archival, `PRD:M-01..M-56` alignment, `:core:audio` PCM engine + tests
     2) `core(database,healthconnect,sync)`: reactive active member + lock, HC `ActiveCaloriesBurnedRecord` + external import + orphan delete, Realtime dedup + tests
     3) `ui(designsystem,feature)`: separated `DetachedObsidianDock` + fade scrim, `DRD:C-07` onboarding, multi-weight fonts, squircle inputs, 20/56 interactive charts + calendar filter + visual tests
     4) `app(release,security)`: R8 rules, Play Protect/Keystore hardening, `EndToEndAppAndVisualTest` clean paths
     5) `ci,docs`: `.github/workflows/android-intelligent-release.yml`, `scripts/*`, attributed `README.md`, `HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`
   - Push to `origin main` via `git push` / `gh`, and confirm `gh api repos/imsovikde/OpenCult/commits/HEAD` reports `verified: true`.
4. Final Memory & Report:
   - Update Second Brain `capsule:project:opencult` (`current-state`, `decisions`) and store a linked session `handoff` entry (`project="opencult"`).
   - Output exact executed commands, raw exit codes, release keystore `SHA-1`/`SHA-256` (`SHID`), screenshot observations, and explicit `Verified / Unverified / Unknown` boundaries before appending `<!-- GOAL_COMPLETE -->`.
```
