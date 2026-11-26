# OpenCult End-to-End Android Build Prompt

Use this prompt after you have reviewed and approved the Open Design prototype (`prompts/01_OPEN_DESIGN_PROTOTYPE_PROMPT.md`) to build the complete native Android application inside this repository.

---

```text
You are building **OpenCult** from scratch in this repository as a production native Android application (Kotlin 2.1+, Jetpack Compose, Material 3 + custom Canvas, Hilt, Room, `blessed-kotlin`, `androidx.health.connect:connect-client`, and Supabase Kotlin SDK).

### 0. Mandatory Pre-Flight Gates
Before modifying or creating any file:
1. Read `AGENTS.md`, `TRD.md`, `PRD.md`, `INVARIANTS.md`, and `DRD.md` in full.
2. Run the Second Brain Recall Gate for `project="opencult"` (`list_recent`, `list_projects`, `recall`, `brief`).
3. Verify `.env.local` exists locally (gitignored) with `OPENCULT_SUPABASE_URL` and `OPENCULT_SUPABASE_ANON_KEY`, and verify `Both/` (`Both/occult` and `Both/openscale`) is present as your local read-only reverse-engineering reference and ignored by `git status`.
4. Read the relevant reference implementations in `Both/openscale` (`CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt`) and `Both/occult` (`research/cult_scale_protocol.md`, `js/ble/parser.js`). Retain GPL-3.0 license headers and upstream attribution on adapted files.

### 1. Build in Vertical Slices (Complete Schema -> Logic -> Tests -> UI Per Slice)
Do NOT build horizontal layers ("all DB first, all UI later"). Complete and verify each vertical slice end to end before moving to the next:

- **Slice S0: Gradle Multi-Module Scaffold and CI**
  - Create root Gradle Kotlin DSL project (`minSdk = 31`, `targetSdk = 36`, Java 21) with modules `:app`, `:core:designsystem`, `:core:ble`, `:core:bia`, `:core:database`, `:core:sync-supabase`, `:core:healthconnect`, `:feature:onboarding`, `:feature:home`, `:feature:weighin`, `:feature:bodymap`, `:feature:reports`, `:feature:profiles`, `:feature:inspector`.
  - Inject `OPENCULT_SUPABASE_URL` and `OPENCULT_SUPABASE_ANON_KEY` safely via `local.properties` / `.env.local` into `BuildConfig` (never hardcode secrets, never commit `.env.local`).
  - Gate: `./gradlew :app:assembleDebug` exits `0`.

- **Slice S1: `:core:designsystem` (`DRD.md` Binding Implementation)**
  - Implement exact Daylight Clinical Pastel & Obsidian Night tokens (`DRD:T-01..T-25`), `Plus Jakarta Sans` (`tnum`) + `JetBrains Mono` typography.
  - Build custom `Canvas` components: `HeroCapsuleSparkBars` (`DRD:C-01`), `OverviewBentoWaveCard` (`DRD:C-02`), `SegmentedArcGauge` (`DRD:C-03`), `StippledDotMatrixBarChart` (`DRD:C-04`), `FloatingObsidianDockWithFab` (`DRD:C-05`), `SegmentalBodyMapCanvas` (`DRD:C-06`).
  - Gate: Compose UI tests verify dark/light token parity, zero hardcoded color/size literals in feature modules (`INV-10`), and `tnum` on numeric styles (`INV-12`).

- **Slice S2: `:core:bia` (Pure-Kotlin 56-Parameter Dual-Frequency + 4E Anthropometric Engine)**
  - Implement all 56 parameters (`PRD:M-01..M-56`) with `MetricProvenance` (`DIRECT_SENSOR`, `BIA_DUAL_FREQ_8E`, `BIA_SINGLE_FREQ`, `ANTHROPOMETRIC_ESTIMATE`) (`INV-02`).
  - Implement physiological plausibility gate (`INV-03`), Sun (2003) FFM/TBW, De Lorenzo (1997) Cole two-band inversion for `ECW`/`ICW`, Wang (2004) `BCM`, Janssen (2000) `SMM`, Katch-McArdle `BMR`, 5-segment normalized muscle/fat decomposition, and `CS-BF01` anthropometric fallback (Deurenberg 1991, Watson 1980, Mifflin-St Jeor 1990).
  - Gate: JVM unit tests covering normal adult male/female, athlete, obese, open-circuit/socks failure (`INV-03`), and segmental mass conservation (`TRD:TEST-03`, `TRD:TEST-04`).

- **Slice S3: `:core:ble` (4E + 8E GATT State Machine and Raw Frame Capture)**
  - Implement `ScaleScanner`, `CultProGattHandler` (`CULT*`, `0xCF` status `0x01/0x00/0x08/0x05`, 2x `0xBE` 10-channel impedance, 4x `0xDF` capture), and `CultBasicGattHandler` (`CS-BF01`, `FD 37` / `F1` / `F2 00` handshake, 11B live vs 18B history vs coalesced duplicate disambiguation).
  - Capture every raw packet verbatim into `RawBlePacket` (`INV-01`).
  - Gate: JVM unit tests replaying captured `0xCF`, `0xBE`, `0xDF`, bad checksums, and coalesced duplicate frames (`TRD:TEST-01`, `TRD:TEST-02`).

- **Slice S4: `:core:database` (Room SQLite + Outbox + 10-Profile Auto-Recognition)**
  - Implement Room entities mirroring `supabase/migrations/20261126000001_opencult_initial_schema.sql` plus `sync_outbox`.
  - Enforce 10-profile limit, soft-delete tombstones (`INV-16`), `< 50 ms` local commit (`INV-06`), and `±2.5 kg` single-match vs `pending_review` ambiguity logic (`INV-09`).
  - Gate: Robolectric Room DAO + migration + profile-matching tests (`TRD:TEST-05`).

- **Slice S5: `:core:healthconnect` (Two-Way Google Health Connect)**
  - Configure AndroidManifest permissions + Android 13/14+ rationale activity aliases.
  - Implement instant auto-write (`WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BoneMassRecord`, `BodyWaterMassRecord`, `BasalMetabolicRateRecord`, `HeartRateRecord`, `HeightRecord`) using deterministic `clientRecordId` (`INV-04`, `INV-05`).
  - Implement `ChangesToken` incremental reader for `StepsRecord`, `SleepSessionRecord`, `TotalCaloriesBurnedRecord`, and external `WeightRecord` (excluding own `packageName` to prevent loops), plus historical backfill and tombstone deletion.
  - Gate: JVM + mocked `HealthConnectClient` tests (`TRD:TEST-06`).

- **Slice S6: `:core:sync-supabase` (Offline Outbox + Zero-Refresh Realtime)**
  - Wire Supabase Auth (Google + Email OTP), PostgREST upsert/tombstone drainer, and `postgresChangeFlow` Realtime subscriber merging remote changes into Room within `<= 2 s` without refresh, filtering self-echoes via `mutation_id` (`INV-07`, `INV-08`).
  - Gate: Sync engine unit tests + `supabase migration list` verification (`TRD:TEST-07`, `TRD:TEST-08`).

- **Slice S7-S10: Feature Screens (`onboarding`, `home`, `weighin`, `bodymap`, `reports`, `profiles`, `inspector`) + Export**
  - Build all 9 screens matching `DRD.md` and your approved Open Design prototype pixel-for-pixel, with `navController`, Daylight/Obsidian theme switcher, lossless JSON/CSV export, and BLE Raw Packet Inspector (`DRD:S-01..S-09`).
  - Gate: Full `./gradlew testDebugUnitTest lintDebug connectedDebugAndroidTest` (or Compose UI screenshot/bounds tests on emulator), verifying floating dock clearance (`INV-13`) and zero UI violations.

### 2. Final Verification Contract
Do not declare work finished until every Gradle build, unit test, lint check, and UI test runs and passes after your final edit, with exact commands, exit codes, and `Verified / Unverified / Unknown` boundaries reported cleanly, and a session-end handoff recorded in Second Brain (`project="opencult"`).
```
