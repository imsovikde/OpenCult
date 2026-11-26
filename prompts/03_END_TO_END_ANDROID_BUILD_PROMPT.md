# OpenCult Ultimate End-to-End Android Pro Prompt (Remediation, Full Build, Release CI/CD & Verification)

Use this prompt as the single governing instruction for fixing every existing defect in `app/`, `core/`, and `feature/`, completing reverse engineering, native Health Connect, precision sound feedback, full 20-parameter (`Cult Smart Scale`) vs 50+-parameter (`Cult Smart Scale Pro`) UI/math/filtering, R8 release obfuscation, GitHub Actions automated intelligent releases, `README.md` attribution, and granular GitHub commits without stopping until every verification passes.

---

```text
You are operating as a principal Android systems architect, BLE reverse engineer, quantitative body-composition engineer, Jetpack Compose motion/UI specialist, and DevSecOps release engineer inside `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

### 0. Non-Negotiable Long-Horizon Autonomy Contract (`/goal` + `ralph-loop` + `/boost`)

1. **Mandatory Skill & MCP Activation (Load Before Any Edit):**
   - Execute in strict autonomous discipline using these exact local skills and MCP servers:
     - Autonomous loops & rigor: `ralph-loop` (`ralph 0.6.0` CLI), `goal` (`/goal`), `boost` (`/boost`), `verification-before-completion`, `tdd`, `systematic-debugging`, `apex-bug-hunter`, `testing-setup`
     - Reverse engineering: `reverse-engineer-anything` + `rea` MCP server (`open_binary`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `trace_android_references`, `inspect_adb_client`, `list_adb_devices`, `pull_adb_file`, `inspect_packet_capture`, `build_reconstruction_obligation_ledger`, `evaluate_reconstruction_coverage`, `verify_reconstruction`, `list_unknowns`, `close_binary`)
     - Security & Google Play Protect: `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `r8-analyzer`, `anr-audit-and-fix`
     - Cloudflare repository & edge security: `cloudflare`, `workers-best-practices`, `cloudflare-one`, `wrangler`, `cf-cli`
     - Android UI, motion, device checks, & writing: `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`, `android-cli`, `humanizer`
     - Persistent memory: `second-brain` MCP server (`project="opencult"`)
2. **Continuous Self-Repair Loop (Zero Premature Exit):**
   - Do NOT return, ask for permission mid-task, skip an emulator/UI screenshot check, or summarize incomplete work. Keep looping through diagnose -> fix -> build -> test -> inspect rendered PNG screenshots until EVERY requirement in Sections 1 through 9 passes after your final edit.
   - Whenever you fix ANY bug or discover a new defect class, apply `INV-23` in the same commit: append a mechanistic row to `HALL_OF_SHAME.md`, tighten `INVARIANTS.md` (and `MICROENGINEERING.md` / `MOTIONENGINEERING.md` for UI/motion issues), and write a failing-then-passing regression test.
   - Emit `<!-- GOAL_COMPLETE -->` ONLY when every single build, unit/Robolectric test, R8 release APK verification, security audit, visual PNG screenshot audit, GitHub commit/push, CI workflow, and Second Brain sync is 100% verified with exact command outputs and exit codes. If physically blocked (e.g., no physical BLE scale hardware powered on or unconfirmed email credential), keep local simulated/emulator suites separated cleanly and state `Verified / Unverified / Unknown` boundaries with raw errors (`AGENTS.md §0`).

---

### 1. Mandatory Reconnaissance & Verified Defect Ledger Already Found in Workspace

Before touching code:
1. Read `AGENTS.md`, `TRD.md`, `PRD.md`, `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, `DRD.md`, `supabase/migrations/20261126000001_opencult_initial_schema.sql`, and run the Second Brain Recall Gate (`list_recent`, `list_projects`, `recall`, `brief`, `get(id)` truncation guard).
2. Read the local reverse-engineering references under gitignored `Both/` (`Both/openscale`: `CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `RunstarR6Handler.kt`, `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt`; `Both/occult`: `CONTRACTS.md`, `research/cult_scale_protocol.md`, `js/ble/uuids.js`, `js/ble/parser.js`, `js/metrics/bodycomp.js`, `daemon/internal/protocol/frame.go`, `daemon/internal/protocol/session.go`).
3. Confirm `.gitignore` keeps `Both/`, `.env*` (except `.env.example`), `keystore/`, `keystore.properties`, `*.jks`, `*.p12`, and `.scratch/` ignored (`INV-15`). Inspect local Android SDK (`android-34`, `android-36`), JDK (`17` / Android Studio JBR), `adb`, `keytool`, `supabase`, and `gh` CLI auth status.
4. Immediately remediate ALL confirmed defects already present in the current uncommitted workspace code (`app/`, `core/`, `feature/`):
   - **Defect 1 (Uncommitted Workspace + Missing CI & README Attribution):** Current Kotlin code and `SHAME-008..012` / `INV-24..25` edits sit uncommitted; `.github/workflows/` does not exist; `README.md` lacks full release/attribution documentation and build badges.
   - **Defect 2 (Broken `DetachedObsidianDock` & Overlapping Bottom Bars, visual screenshot bug):** `DetachedDock.kt` embeds `CobaltMeasureFab` (`+`) inside the single dark pill row instead of `DRD:C-05`'s separated layout (`4`-tab `#0F1115` capsule on left + `12.dp` gap + detached `64.dp` `#2185F7` `+` FAB on right). Scrolling content overlaps visually underneath the dock (`StippledBarChart` solid bar and stipples bleed across the dock). Fix geometry, add a smooth translucent gradient scrim under the dock, and verify `>= 116.dp` bottom clearance visually.
   - **Defect 3 (`OnboardingScreen.kt` UI Breakage, Cramped Permissions Row, Clipping, & Fake 3-Scale Chips):** `Allow Bluetooth` (1 line) and `Allow Health Connect` (wraps onto 2 lines) inside `OnboardingScreen.kt` create uneven button heights and a broken row; `OnboardingScreen` uses stock `OutlinedTextField` boxes and misses bottom `navigationBarsPadding()` (clipping `"Heavy"` behind the gesture pill); onboarding forces a fake 3-chip selector (`Cult Pro 8E` / `Pro Feet-Only` / `CS-BF01 4E`) instead of **Cult's two real scales** (`Cult Smart Scale Pro` and `Cult Smart Scale`) paired automatically; and the screen discards `DRD:C-07`'s Daylight Pastel sky-wash preview carousel.
   - **Defect 4 (`ScaleFrameParser.kt` `CS-BF01` Heart Rate vs Impedance Byte Swap + Missing Auto-Reconnect):** `ScaleFrameParser.kt` reads `((b2 shl 8) or b1) / 10.0` as `oneByoneImpedanceRaw` and blocks `heartRateBpm` when `expectedTier == CULT_BF01_4E`, directly violating `Both/occult`'s verified `CS-BF01` `0xCF` layout where `bytes[1]` is Heart Rate (`bpm`) and `bytes[5..6]` LE uint16 is Impedance (`ohm`). Also, `CultBleConstants.buildModeSetupCommand` hardcodes XOR `0xCB` even when `unitKg = false`, `CultBleController` detects scale tier from device name string alone rather than live GATT/packet signatures, and saved scales in `scale_devices` do NOT auto-reconnect when the app opens.
   - **Defect 5 (`Type.kt` Static Font Weight Illusion):** `core/designsystem` bundles only a single `plus_jakarta_sans_regular.ttf` and `jetbrains_mono_regular.ttf` while relying on `FontVariation.weight(...)` which fails to produce crisp Bold/ExtraBold letterforms on standard font assets. Bundle true multi-weight static TTF files (`400/500/600/700/800`) or verified variable fonts with calibrated tracking + `fontFeatureSettings = "tnum"`.
   - **Defect 6 (`HomeScreen.kt` Blank Empty Dashboard, Small `24.dp` Pager Buttons, & Seed Helper):** When `measurements` is empty, `HomeScreen` hides the Bento Overview Grid completely and leaves an empty white screen (ignoring Health Connect Page 2 Steps/Sleep/Calories which can populate immediately!). Overview `<` / `>` icons use `size(24.dp)` instead of `48.dp` min hitbox (`INV-18`). `HomeViewModel.seedSampleMeasurementForVerification()` must be removed from production UI code (keep test fixtures inside `src/test/` only; never auto-generate fake body composition data!).
   - **Defect 7 (`ReportsScreen.kt` Broken Calendar Selection, Hardcoded `16.5f` Placeholder, & Missing 20/50 Interactive Drill-Down):** Selecting a date in `OpenCultCalendarSheet` updates `selectedDate` (`ReportsScreen.kt`), yet `filtered` and `bars` ignore `selectedDate` completely! `latestBf` defaults to fake `16.5f` when empty. Users cannot click any of the **20** (`Cult Smart Scale`) or **56** (`Cult Smart Scale Pro`) parameters inside Home / Measurement Detail / Reports to open interactive trend charts, date-range filters (`7D / 30D / 90D / 1Y / ALL / Custom Day`), or scale-tier filters.
   - **Defect 8 (`ReportsViewModel` & `BodyMapViewModel` Stale Member State + Orphan Health Connect Bug):** Reading `vault.getActiveProfileId()` once inside `combine()` means switching members on Home/Profiles fails to reactively update `ReportsScreen` or `BodyMapScreen`! Even worse: reassigning an already-synced measurement from Member A (Primary HC user) to Member B does NOT delete Member A's written records in Health Connect! Plus `WeighIn` lacks an explicit **Member Lock** toggle vs Auto-Match clarity so a user who selects/locks Member B before stepping up onto the scale logs straight to Member B reliably.
   - **Defect 9 (`HealthConnectGateway.kt` + `InspectorScreen.kt` Bugs):** `readDailyContext` omits `ActiveCaloriesBurnedRecord` fallback when `TotalCaloriesBurnedRecord` is empty; `InspectorViewModel.pollExternalHc()` discards fetched external `WeightRecord` items instead of committing them to Room (`INV-05`); and onboarding/home lack crisp, dedicated native Health Connect status & permission UX placement.
   - **Defect 10 (Missing Sound Engine & Hardcoded Machine Path in Tests):** No acoustic feedback exists anywhere in the app. `EndToEndAppAndVisualTest.kt` hardcodes `C:\Users\imsov\...\screenshots` instead of Gradle build/test output directories (`layout.buildDirectory`).

---

### 2. Deep Dual-Model Reverse Engineering (`reverse-engineer-anything` + `rea` MCP)

You MUST model ONLY the two physical scale products Cult sells (no bogus 3rd hardware option during onboarding/setup):

1. **Device 1: Cult Smart Scale (`CS-BF01`, 4-Electrode, 20 Parameters):**
   - Auto-detected via BLE name (`Cult*`, `CS-BF*`, `OKOK*`, `Cultsport*`, `1byone*`) AND wire signature (`0xFFF0` / `0xFFF4`, `FD 37` / `F1` / `F2 00` handshake with `3000 ms` fallback, 11-byte `0xCF` frames without `0xBE`/`0xDF`). Dynamically compute XOR `xor(bytes[0..n-1])` in ALL outbound commands (`FD 37` fixed!).
   - Fix `ScaleFrameParser.parse0xCf`:
     - Distinguish **Cult Smart Scale (`CS-BF01`)** (`bytes[1]` = Heart Rate `bpm`, `bytes[3..4]` LE uint16 `/ 100.0` = `weightKg`, `bytes[5..6]` LE uint16 = `impedanceOhm`, `bytes[9]` `0x01` settling vs `0x00` locked, `bytes[10] = xor(0..9)`) cleanly from **OneByone** history/legacy encodings, while enforcing `INV-21` (`>= 18B` ATT-coalesced duplicate guard `bytes[11] == 0xCF`).
     - Immediately surface and compute all **20 honest parameters** (`Weight`, `BMI`, `WHO Category`, `Body Fat %`, `Body Fat Mass`, `Fat-Free Mass`, `Subcutaneous Fat %`, `Subcutaneous Fat Mass`, `Visceral Fat Index`, `Visceral Fat Mass`, `Total Body Water %`, `Total Body Water Volume/Mass`, `Muscle Mass`, `Skeletal Muscle Mass / %`, `Bone Mass`, `Protein Mass / %`, `BMR`, `TDEE`, `Metabolic Age`, `Ideal Weight / Obesity %`, `Composite Health Score`, plus `Heart Rate` when sensor frame provides `bytes[1] > 0`) using peer-reviewed BIA equations (`Sun 2003`, `Janssen 2000`, `Wang 2004`, `Katch-McArdle`) when valid impedance (`150..1200 ohm`) is emitted or calibrated `Deurenberg / Watson / Mifflin-St Jeor` fallback labeled accurately (`MetricProvenance`).
2. **Device 2: Cult Smart Scale Pro (8-Electrode Dual-Frequency Handle, 50+ / 56 Parameters):**
   - Auto-detected immediately via `0xCF` status transitions (`0x01` settle -> `0x08` handle BIA -> `0x05` complete) and `2 x 13B 0xBE` packets (`Set A[0..4]` low freq + `Set B[0..4]` high freq) + `4 x 17B 0xDF` packets (`INV-01`).
   - Fix `SHAME-004` forever: capture all **10 raw impedance channels** + raw `uint16_le` + ohm scaling (`0.1`), pass plausibility gates (`INV-03`), compute dual-frequency Cole `R0` / `Rinf` -> `ECW` / `ICW` / `ECW-TBW` / `BCM`, `Janssen SMM/SMI`, and **mass-conserving 5-segment (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`) muscle & fat + Bilateral Symmetry + ALM/ASMI**, making all **56 parameters (`PRD:M-01..M-56`)** immediately inspectable and chartable.
   - **Pro Feet-Only Automatic Downgrade:** If a **Cult Smart Scale Pro** emits `0xCF` `status = 0x00` and times out without handle `0xBE` packets (user stepped on without holding the handle), automatically classify that session as **Cult Smart Scale Pro (Feet-Only Mode)** (`INV-03`), suppress 8E segmental/ECW metrics cleanly, and never display a fake 3rd hardware scale during onboarding.
3. **One-Time Pairing + Automatic Background/Foreground Scale Wakeup:**
   - Persist paired scale MAC address, advertised name, and resolved hardware tier (`Cult Smart Scale Pro` vs `Cult Smart Scale`) in Room `scale_devices` (`INV-14`) + DataStore active primary scale reference.
   - Whenever the user opens `OpenCult` (`Lifecycle.State.STARTED` on `Home` / `WeighIn` with `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` granted), automatically run bounded BLE passive recognition / GATT reconnect for saved scales so **stepping onto the scale opens/updates live settling immediately and logs on lock with zero extra pairing clicks**.
4. **Shipped Artifact Verification (`reverse-engineer-anything`):**
   - If a vendor APK (`Cult Fit` / `Cultsport`) or captured `btsnoop_hci.log` is present or supplied on device/workspace, inspect it using `rea` MCP tools (`open_binary`, `inspect_android_package`, `inspect_packet_capture`, `build_reconstruction_obligation_ledger`, `list_unknowns`, `close_binary`). Also run `rea` against your own built `app-release.apk` (`open_binary` + `inspect_android_package` + `evaluate_reconstruction_coverage`) to verify obfuscation, manifest hygiene, and zero secret strings! NEVER fabricate any unverified `0xDF` payload byte (`INV-01`, `INV-02`).

---

### 3. Native Google Health Connect Architecture & UI Placement (`:core:healthconnect`)

Build and polish Google Health Connect end to end as a first-class Android system integration (since `Both/openscale` has ZERO Health Connect support):

1. **Clean Permissions & UI Placement:**
   - Dedicated **Health Connect Readiness & Control Card** placed prominently on:
     1) **Onboarding** (step card with clean equal-height full-width or aligned CTA button, clear explanation of exact 8 write + 5 read data types, zero text-wrap distortion),
     2) **Home Dashboard** (`Health Connect` status pill/card + Page 2 Overview Bento tiles for daily Steps, Sleep Duration, Active/Total Calories Burned),
     3) **Sync Center / Inspector** (`X / 13 permissions granted`, per-record write ledger `Synced / Pending / Failed / Deleted`, 1-tap `Grant Missing Permissions`, 1-tap `Backfill All History`, `Import External Weights`).
2. **Exact Native Record Fidelity (`androidx.health.connect:connect-client`):**
   - On every confirmed lock for a household member flagged `isPrimaryHcUser` (or user-selected Health Connect target), write immediately in background using `Instant` + `ZoneOffset` + `Device(type = Device.TYPE_SCALE, manufacturer = "Cult.fit", model = detectedHardwareModelName)` + `Metadata.autoRecorded(...)` (for BLE weigh-ins) or `Metadata.manualEntry(...)` (for manual entries), with `clientRecordId = "opencult:${measurement.id}:${tag}"` and monotonic `clientRecordVersion`:
     - `WeightRecord` (`Mass.kilograms`)
     - `BodyFatRecord` (`Percentage`)
     - `LeanBodyMassRecord` (`Mass.kilograms`)
     - `BoneMassRecord` (`Mass.kilograms`)
     - `BodyWaterMassRecord` (`Mass.kilograms`)
     - `BasalMetabolicRateRecord` (`Power.kilocaloriesPerDay`)
     - `HeartRateRecord` (when scale emitted HR)
     - `HeightRecord` (when profile height changes)
   - Support partial permission grants gracefully without crashing (`SecurityException` guarded per type, updating `health_connect_sync_ledger` accurately ONLY for records actually inserted/confirmed).
3. **Loop-Free Inbound Sync, Backfill, & Orphan Cleanup:**
   - Read daily `StepsRecord`, `SleepSessionRecord`, and `TotalCaloriesBurnedRecord` **with automatic fallback to `ActiveCaloriesBurnedRecord`** inside `HealthConnectGateway.readDailyContext`.
   - Fix `pollExternalWeights`: actually persist imported external `WeightRecord`s (where `dataOrigin.packageName != BuildConfig.APPLICATION_ID`) into Room `measurements` (`source = "health_connect_import"`) with dedup!
   - When a measurement is soft-deleted OR reassigned from a Health Connect user to another household member, ALWAYS delete the old Health Connect records first via `deleteRecords(clientRecordIds)` so Health Connect never holds phantom data (`INV-04`, `INV-05`, `INV-16`).

---

### 4. Member Isolation, Member Lock, & Lifetime Calendar/Supabase Persistence

1. **Reactive Active Member Selection & Member Lock (`Home` / `Profiles` / `WeighIn`):**
   - Store `activeProfileId` inside a reactive `StateFlow<String?>` in a singleton preference/session coordinator injected into ALL ViewModels (`MainViewModel`, `HomeViewModel`, `BodyMapViewModel`, `ReportsViewModel`, `ProfilesViewModel`, `WeighInViewModel`) so switching members updates every tab instantaneously.
   - Add an explicit **Member Lock** mode (`Lock to Active Member` toggle on `WeighIn` / `Home`):
     - When **Locked ON**: every incoming scale weigh-in immediately logs straight to that exact locked member (even when multiple household members share similar weights or have no prior history) with zero accidental cross-member miswrites.
     - When **Auto-Match (Household Shared)**: enforce `INV-09` (`±2.5 kg` single match -> confirm; 0 or 2+ matches -> `pending_review` + interactive chooser sheet, never blindly dumping Member B onto Member A!).
2. **Lifetime Per-Member, Per-Calendar-Day Persistence & Realtime Sync:**
   - Every weigh-in commits atomically in `< 50 ms` into Room (`measurements` + immutable `raw_ble_packets` + `health_connect_sync_ledger` + `sync_outbox`) indexed by `(profile_id, local_date, measured_at)` (`INV-06`).
   - Re-enable reactive `postgresChangeFlow` across `measurements`, `household_profiles`, `scale_devices`, and `health_connect_sync_ledger` with `mutation_id` self-echo suppression (`INV-07`), scoped strictly by `FORCE ROW LEVEL SECURITY` `(SELECT auth.uid()) = account_id` (`INV-08`).
   - Zero fake/demo seeded measurements anywhere in shipping app paths!

---

### 5. Acoustic Sound Engineering (`:core:audio` `OpenCultSoundEngine`)

Do NOT use harsh Android system notification ringtones (`RingtoneManager` / notification channel bings). Build a lightweight, sample-accurate `:core:audio` synthesizer using `android.media.AudioTrack` (`AudioAttributes.USAGE_ASSISTANCE_SONIFICATION`, `CONTENT_TYPE_SONIFICATION`, `MODE_STATIC`, 44.1 kHz 16-bit PCM mono with raised-cosine Hann attack/release envelopes to eliminate clicks/clipping):

1. `SCALE_CONNECTED`: Gentle warm crystalline major-fifth chime (`C5 523.25 Hz` -> `G5 783.99 Hz`, `80 ms`).
2. `SETTLING_TICK`: Quiet tactile click-pip (`880 Hz`, `12 ms` envelope) when stable weight counter increments (`1/3`, `2/3`).
3. `WEIGHT_LOCKED`: Crisp medical-instrument confirmation dyad (`D5 587.33 Hz` + `A5 880.00 Hz`, `140 ms` decay) synchronized to weigh-in lock (deduplicated once per session).
4. `METRICS_LOGGED_PRO_50` / `METRICS_LOGGED_BASIC_20`: Bright upward triad (`C5 -> E5 -> C6`, `220 ms` tail) confirming full parameters calculated, saved to Room, and Health Connect write triggered.
5. `AMBIGUOUS_MEMBER_PROMPT`: Balanced neutral two-tone interval (`A4 440 Hz -> G4 392 Hz`, `150 ms`) cueing member selection.
6. `CAPTURE_ERROR`: Soft low mute thud (`220 Hz -> 165 Hz`, `110 ms`) on checksum failure or lost contact.
- Respect device ringer/media/silent state and expose a persistent `Scale Sound Feedback` switch inside Profile / Sync Settings (`SecureVault`).

---

### 6. Industry-Grade UI, Typography, Full 20/50 Data Visualization & Filtering (`MICROENGINEERING.md` + `MOTIONENGINEERING.md` + `DRD.md`)

1. **Strict Rejection of Stock Native Controls:**
   - **BANNED:** Stock `DatePickerDialog` / Material `DatePicker` popup, stock `Spinner` / `DropdownMenu` popups, stock `OutlinedTextField` settings lookalikes in Onboarding, raw `Switch` toggles where custom squircle pills apply, stock `NavigationBar`, `Card`, `FloatingActionButton`, or external chart libraries (MPAndroidChart, Vico).
   - **REQUIRED:** Custom Compose `Canvas` (`DRD:C-01`..`C-06`), custom `OpenCultCalendarSheet` (wired directly into Reports & Home filtering!), custom `OpenCultPillSelectorSheet` / animated pills, custom squircle text input containers for member onboarding/editing, all obeying 4-stage micro-press (`0.92f`/`0.94f`/`0.97f` `graphicsLayer` + haptic), `>= 48.dp` touch hitboxes (`INV-18`), and closed `OpenCultMotion` specs (`INV-19`, `INV-20`).
2. **Exact Remediation of Visual Bugs Observed in Screenshots:**
   - **Dock + Scrim Separation (`DRD:C-05`, `INV-13`):** Render `#0F1115` capsule (`4` nav tabs) on left + `12.dp` spacing + detached `64.dp` `#2185F7` `+` FAB on right, layered over a smooth subtle translucent canvas fade scrim so scrolling charts and list cards fade gracefully above the dock without bar overlap (`06844ba6.png` fixed forever!).
   - **Sleek `DRD:C-07` Onboarding + Aligned Permission Row:** Implement the true Daylight Clinical Pastel wash (`#CBE1F7 -> #F4F6FA`), breakout miniature dashboard card preview (`Step 1`), dedicated **Hardware Auto-Discovery + Equal-Height Permissions Page** (`Step 2` with full-width or matched `IntrinsicSize.Max` single-line permission cards + Bluetooth/Health Connect live badges, fixing `d211f02d.png`), and clean **Primary Member Setup Page** (`Step 3` with full `.navigationBarsPadding()`, fixing bottom chip clipping in `a95f495c.png`), using zero hardcoded 3rd-scale selector chips.
   - **True Multi-Weight Fonts (`Type.kt`):** Bundle actual `Plus Jakarta Sans` (`400/500/600/700/800`) and `JetBrains Mono` (`400/500/700`) static font files cleanly in `res/font/`, paired with `fontFeatureSettings = "tnum"` on every numeric metric (`INV-12`).
   - **Calm Empty Home + Full Bento Grid:** Even before a user's first weigh-in, keep the clean Hero card (`Cult Smart Scale Pro` / `Cult Smart Scale` ready badge + quiet spark baseline), the asymmetric Lavender/Sage/Peach Bento Overview Grid (`<`/`>` `48.dp` pager arrows showing real Health Connect Steps/Sleep/Calories on Page 2!), and a clear Google Health Connect card.
   - **Complete 20-Param / 56-Param Data Visualization & Lifetime Filtering:**
     - **Body Map (`DRD:C-06`) & Parameter Explorer:** Automatically shows **20 active panels/metrics** when `Cult Smart Scale (CS-BF01)` is detected/used and **all 56 parameters** (`M-01..M-56` + 5-segment interactive map + Muscle/Fat toggle + bilateral balance bars) when `Cult Smart Scale Pro` is detected/used.
     - **Click Any Metric -> Full Trend Visualizer:** Clicking any parameter across Home Bento, Measurement Detail, Body Map, or Reports opens/updates rich native `Canvas` visualizations (line + gradient area chart with clickable data points + zone bands, 3-zone Arc Gauge `DRD:C-03`, Staggered Stippled Dot-Matrix Bar Chart `DRD:C-04`), with **working multi-axis filtering**: Date Range (`7D`, `30D`, `90D`, `1Y`, `Lifetime All`), **Specific Calendar Date / Date-Range Filter** (`OpenCultCalendarSheet` wired cleanly!), **Scale Source Filter** (`All`, `Cult Pro 8E`, `Cult Scale 4E`, `Feet-Only`), and **Member Filter**.

---

### 7. Security, Persistent Release Keystore, Play Protect, & Cloudflare Rules

1. **Identity, SDKs & Keystore (`app/build.gradle.kts`):**
   - `applicationId = "org.opencult.scale"`, `minSdk = 31`, `compileSdk = 36`, `targetSdk = 36`.
   - Maintain gitignored RSA-4096 `keystore/opencult-release.jks` + `keystore.properties` (plus GitHub Secrets env support) with `enableV1Signing = true` and `enableV2Signing = true`, failing any `Release` build immediately if release credentials are missing (zero debug fallback). Document non-secret Release Keystore `SHA-1` & `SHA-256` digests (`SHID`).
2. **Industry-Grade R8 Obfuscation (`app/proguard-rules.pro`):**
   - Enforce `isMinifyEnabled = true` and `isShrinkResources = true` on `release`.
   - Add hardened R8 directives (`-repackageclasses 'o'`, `-allowaccessmodification`, `-renamesourcefileattribute SourceFile`, `-keepattributes SourceFile,LineNumberTable`, `-assumenosideeffects class android.util.Log { public static boolean isLoggable(java.lang.String, int); public static int v(...); public static int d(...); }`), while keeping exact rules for Kotlinx Serialization, Supabase Auth/PostgREST/Realtime DTOs, Hilt, Room, `blessed-kotlin`, and Health Connect! Audit the resulting mapping and DEX via `r8-analyzer` and `rea` MCP (`open_binary` + `inspect_android_package`).
3. **Google Play Protect & Cloudflare Security Compliance:**
   - Verify `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk` shows ZERO dangerous/restricted permissions (`BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, `REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, location permissions, external storage permissions).
   - Enforce `IntentSanitizer`, `PendingIntent.FLAG_IMMUTABLE`, `RECEIVER_NOT_EXPORTED`, `android:exported="false"` on internal components, scoped `FileProvider` `/exports/`, `usesCleartextTraffic="false"`, Android Keystore AES-256-GCM encryption for tokens (`SecureVault`), and zero secrets in APK assets (`INV-15`, `INV-24`).
   - Run `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, and `anr-audit-and-fix` checks and apply Cloudflare repository/edge security standards across repo & CI (`workers-best-practices` / `cloudflare-one`: zero leaked tokens, `crypto` CSPRNG if edge code added, timing-safe secret comparisons).

---

### 8. GitHub Actions Intelligent Release Workflow, Versioning, & Attribution `README.md`

1. **Create `.github/workflows/android-intelligent-release.yml` + `scripts/`:**
   - Triggers: `push` (`main`, feature branches, `v*` tags), `pull_request`, and `workflow_dispatch`.
   - **Autonomous Version Sync (`scripts/bump_version.py`):** On `main` push / release trigger (avoiding infinite `[skip ci]` recursion cleanly), atomically increments `versionCode` + `versionName` (`patch`/`minor`/`major`) across `app/build.gradle.kts`, UI About display (read dynamically from `BuildConfig.VERSION_NAME` & `BuildConfig.VERSION_CODE` so the UI always displays live synced version info!), and `README.md` release badges/table.
   - **Hardened Release Build & Signing in CI:** Sets up JDK 17 + Android SDK `android-36` + `build-tools;36.0.0`, restores persistent release keystore from GitHub Secrets (`OPENCULT_KEYSTORE_BASE64`, `OPENCULT_KEYSTORE_PASSWORD`, `OPENCULT_KEY_ALIAS`, `OPENCULT_KEY_PASSWORD`) cleanly without committing keys (supports `.env.local`/GitHub Secrets `OPENCULT_SUPABASE_URL` & `OPENCULT_SUPABASE_ANON_KEY`), runs full Gradle test + lint + R8 obfuscated `:app:assembleRelease` + `apksigner verify --print-certs` + `aapt2 dump permissions` gate, uploads signed APK (`opencult-v<version>.apk`), `.sha256`, R8 `mapping.txt` artifact, `metadata.json`, `RELEASE_NOTES.md`, and source archive ZIP.
   - **Adaptive Intelligent Release Notes Generator (`scripts/generate_release_notes.py`):** Inspects commit messages, git diff stats across modules (`:core:ble`, `:core:bia`, `:core:healthconnect`, `:core:designsystem`, `:feature:*`), binary size, `apksigner` SHA-1/SHA-256 keystore digests (`SHID`), and `aapt2` security verification; when an optional lightweight AI model token (`GITHUB_TOKEN` GitHub Models e.g. `gpt-4o-mini` / `GEMINI_API_KEY` flash-lite via official HTTPS API) is present in CI environment secrets, synthesizes concise adaptive human-readable highlights validated cleanly, falling back deterministically to `git-cliff` + diff breakdown when offline/no AI key is configured! Publishes GitHub Release (`softprops/action-gh-release@v2`) with all assets and posts PR diff intelligence comments.
   - Verify workflow YAML syntax locally (Python `yaml.safe_load` / `actionlint` if installed) and dry-run `scripts/bump_version.py` & `scripts/generate_release_notes.py` against sample/real metadata cleanly before committing.
2. **Sophisticated Open-Source `README.md`:**
   - Rewrite root `README.md` cleanly using `humanizer` principles (zero AI buzzwords, zero em/en dashes, zero curly quotes): include live version/build badge hooks synced by CI, release APK + SHA-256 attestation section, hardware support matrix (**Cult Smart Scale Pro 56-param** vs **Cult Smart Scale `CS-BF01` 20-param**), BLE protocol & dual-frequency equations overview, Google Health Connect record table, Supabase RLS architecture, explicit upstream open-source attribution (`oliexdev/openScale` `[GPL-3.0]`, `anshuman852/occult` `[MIT]`, `weliem/blessed-kotlin`, Google Jetpack Compose, `androidx.health.connect`, Supabase Kotlin SDK, Material Icons Extended), build instructions, and trademark disclaimer (`AGENTS.md §3.5`).

---

### 9. Granular GitHub Commits, Emulator Visual Verification, & Final Gate

Before outputting your final report:
1. **Fix Screenshot & Unit Tests (`EndToEndAppAndVisualTest.kt`):** Write visual screenshots into project-local `build/outputs/visual_screenshots/` (NEVER hardcoded user brain folders) and add exhaustive tests for: `CS-BF01` `bytes[1]` HR + `bytes[5..6]` impedance, member lock isolation, Calendar filtering in Reports, `ActiveCaloriesBurnedRecord` + external `WeightRecord` Room import, `:core:audio` PCM waveform envelope generation, and `DetachedObsidianDock` bounds + `48.dp` touch target checks (`INV-13`, `INV-18`).
2. **Boot Emulator / Inspect Every Rendered Screen Visually:**
   - Run `./gradlew.bat testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleRelease`.
   - Install `app-release.apk` on Android Emulator (`adb install -r`; uninstall prior debug/signature conflict cleanly if needed), verify ZERO `FATAL EXCEPTION` / ANR logs in `adb logcat`, capture PNG screenshots (`android screen capture` or `adb exec-out screencap -p`) across **Onboarding**, **Home (empty + populated Daylight + Obsidian)**, **Weigh-In**, **Body Map**, **Reports (Arc Gauge + Stippled Chart + Calendar Filter + Drill-Down)**, **Profiles**, **Health Connect Sync**, and **Inspector**, and visually inspect EVERY PNG image yourself (`read_file`) to confirm zero button wrapping breakage, zero dock/chart collision, crisp fonts, and exact `DRD.md` aesthetic alignment.
   - Run `apksigner verify --print-certs`, `aapt2 dump permissions`, `rea` `open_binary` + `inspect_android_package` on `app-release.apk`, and automated prose/secret/`.gitignore` scans (`Both/`, `keystore/`, `.env.local`).
3. **Granular Signed Commits & Push to GitHub:**
   - Stage and commit logically per concern using repo SSH signing (`Souvik Dey <imsovikde@gmail.com>`, zero `Co-Authored-By` trailers): e.g. `:core:ble` + `:core:bia` + `:core:audio` fixes, `:core:database` + `:core:healthconnect` + `:core:sync-supabase`, `:core:designsystem` + `:feature:*` UI/dock/font/chart fixes, `:app` + R8 + Play Protect hardening, `.github/workflows` + release scripts + `README.md` + regression ledgers (`HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`).
   - Push to `origin main` via `git push` / `gh`, and confirm `gh api repos/imsovikde/OpenCult/commits/HEAD` shows `verified: true`.
4. **Update Second Brain & Output Final Evidence:**
   - Update `capsule:project:opencult` (`current-state`, `decisions`) and store a complete linked `handoff` entry in `second-brain` (`project="opencult"`).
   - Quote every executed command, raw exit code, `Verified / Unverified / Unknown` boundaries, release keystore `SHA-256` (`SHID`), and visual screenshot observations, and append `<!-- GOAL_COMPLETE -->` ONLY when every gate above has passed.
```