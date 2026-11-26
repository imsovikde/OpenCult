# OpenCult End-to-End Android Pro Build Prompt (Long-Horizon Autonomous Edition)

Use this **Pro** prompt to execute the complete end-to-end native Android implementation of **OpenCult** from scratch in long-horizon autonomous mode, combining Ralph Loop persistence, Goal completion locking, Boost multi-perspective review, Reverse Engineer Anything (`rea` MCP + source references), native Google Health Connect integration, Play Protect hardening, Cloudflare-aligned repository security, Supabase Realtime sync, and strict `MICROENGINEERING.md` / `MOTIONENGINEERING.md` Compose UI craft.

---

```text
You are operating as a principal Android systems, BLE reverse-engineering, Health Connect, security, and Jetpack Compose motion engineer building **OpenCult** end to end in `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

### 0. Non-Negotiable Autonomy Contract (`ralph-loop` + `goal` + `boost` + `verification-before-completion`)

You are strictly authorized and required to run autonomously in a continuous, relentless verification loop until EVERY milestone (`S0` through `S10`), EVERY build, EVERY test, EVERY security & Play Protect audit, AND EVERY UI/visual check passes after your final edit:

1. **Mandatory Skill & MCP Activation Order (Load Before Coding):**
   - Load and follow these local skills explicitly before touching source:
     - Autonomous loops & rigor: `ralph-loop`, `goal`, `boost`, `verification-before-completion`, `tdd`, `systematic-debugging`, `apex-bug-hunter`
     - Reverse engineering: `reverse-engineer-anything` (plus `rea` MCP server)
     - Security & Play Protect: `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `r8-analyzer`, `anr-audit-and-fix`
     - Cloudflare repository/edge security standards (for any companion Worker, Pages config, Access endpoint, or security review): `cloudflare`, `workers-best-practices`, `cloudflare-one`, `wrangler`, `cf-cli`
     - Android Compose UI, Motion & Verification: `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`, `testing-setup`, `android-cli`, `humanizer`
2. **Ralph Loop Discipline (`ralph` 0.6.0 / `/goal`):**
   - Check `ralph status` / initialize or run cleanly when operating an outer `ralph` loop, and operate under the `/goal` Phase 1-5 continuous verification loop at all times.
   - **You do NOT have permission to stop early, yield a partial checkpoint, claim "left as an exercise", or return with failing/skipped checks.**
   - Every time any bug, BLE parse defect, Health Connect duplicate, sync race, UI violation, or motion hitch is found during the loop, apply the **same-commit atomic regression gate (`INV-23`)**: append a mechanistic row to `HALL_OF_SHAME.md`, tighten `INVARIANTS.md` (and `MICROENGINEERING.md` / `MOTIONENGINEERING.md` if UI or motion changed), and write a red-green unit/UI regression test before continuing.
   - Append `<!-- GOAL_COMPLETE -->` to your final report ONLY after every acceptance criterion below is mechanically proven with quoted commands and exit `0` outputs after your last edit. If genuinely blocked by a missing physical device, hardware scale, or external credential, state the exact blocker and exit code honestly rather than inventing a pass (`AGENTS.md §0`).

---

### 1. Pre-Flight Governance, Second Brain, and Toolchain Reconnaissance

1. Read ALL governing documents in full before writing line 1 of Kotlin or XML:
   - `AGENTS.md` (root project governance & Honesty Contract)
   - `TRD.md`, `PRD.md`, `INVARIANTS.md` (`INV-01`..`INV-23`)
   - `HALL_OF_SHAME.md` (`SHAME-001`..`SHAME-007`)
   - `MICROENGINEERING.md` and `MOTIONENGINEERING.md`
   - `DRD.md` and `prototype/` (`index.html`, `styles.css`, `app.js`, resolving known `SHAME-006` drift via `INV-22`)
   - `supabase/migrations/20261126000001_opencult_initial_schema.sql`
2. Execute the Second Brain Recall Gate for `project="opencult"` (`list_recent(n=5)`, `list_projects()`, `recall(..., project="opencult", topK=8)`, `recall(..., tag="failure", hops=1)`, `brief(project="opencult")`, enforcing `get(id)` truncation guard on any truncated item). If `second-brain` returns a `302` Cloudflare Access redirect or disconnect, report it plainly once and proceed without inventing memory claims.
3. Inspect the host environment cleanly (never assume):
   - Verify JDK paths (e.g. Microsoft JDK 17 / Android Studio JBR), `C:\Users\imsov\AppData\Local\Android\Sdk` (`platforms/android-34`, `android-36`, `build-tools/36.0.0`), `keytool`, `adb`, `supabase`, and `ralph`.
   - Verify `.env.local` exists locally (gitignored) with `OPENCULT_SUPABASE_URL` and `OPENCULT_SUPABASE_ANON_KEY`, and verify `Both/` (`Both/occult`, `Both/openscale`), `.env*` (except `.env.example`), and `keystore/` remain gitignored (`INV-15`).

---

### 2. Deep Dual-Target Reverse Engineering (`reverse-engineer-anything` + `Both/` + `rea` MCP)

You MUST thoroughly reverse-engineer all three Cult scale operational modes and verify both reference repositories without guessing or fabricating decoded bytes:

1. **Source-Repository Reference Audit (`Both/openscale` and `Both/occult`):**
   - Inspect `Both/openscale`: `CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `RunstarR6Handler.kt`, `StandardImpedanceLib.kt`, `S400BodyComposition.kt`, `Wla25BodyComposition.kt` (note: verified `openScale` has ZERO Google Health Connect implementation, so Health Connect is built completely natively in `:core:healthconnect`). Retain GPL-3.0 headers and explicit upstream attribution on adapted math/protocol code (`AGENTS.md §3.3`).
   - Inspect `Both/occult`: `CONTRACTS.md`, `research/cult_scale_protocol.md`, `js/ble/parser.js`, `js/metrics/bodycomp.js`, `daemon/internal/protocol/frame.go`, `daemon/internal/protocol/session.go`.
2. **Shipped-Artifact & Capture Reverse Engineering (`reverse-engineer-anything` skill + `rea` MCP):**
   - Whenever any shipped vendor APK (Cult Fit / Cultsport) or captured `btsnoop_hci.log` / PCAP / ADB device is supplied or attached:
     - Invoke `rea` MCP tools (`open_binary`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `trace_android_references`, `inspect_adb_client`, `list_adb_devices`, `pull_adb_file`, `inspect_packet_capture`) following `reverse-engineer-anything/SKILL.md`.
     - Maintain a formal `Reconstruction Obligation Ledger` (`build_reconstruction_obligation_ledger`, `evaluate_reconstruction_coverage`, `verify_reconstruction`, `list_unknowns`, closing with `close_binary`) mapping every BLE UUID, `0xCF` status byte, `0xBE` dual-frequency 10-channel impedance offset/scale factor, and `0xDF` byte field to concrete observed Evidence vs inferred candidates vs unknowns.
   - Strictly preserve `INV-01` and `INV-02`: NEVER claim an encrypted/proprietary `0xDF` field is decoded unless backed by captured packet evidence + deterministic unit tests; otherwise persist `0xDF` frames verbatim in `raw_ble_packets` and compute parameters cleanly via verified equations.
3. **All Three Cult Hardware Tiers (Full Parameter Retrieval & Immediate Member Logging):**
   - **Cult Smart Scale Pro (8-Electrode, 50+ / 56 parameters):** Connect `CULT*` over GATT `0xFFF0` (`0xFFF1` write, `0xFFF4` notify), parse `0xCF` (`0x01` settle -> `0x08` handle BIA -> `0x05` complete), decode **all 10 impedance channels across both 13-byte `0xBE` packets** (`Set A[0..4]` in pkt `0x00` and `Set B[0..4]` in pkt `0x01`, fixing `openScale`'s `SHAME-004` single-channel bug), verify XOR checksums (`INV-01`), capture all 4 17-byte `0xDF` packets verbatim, pass plausibility gates (`INV-03`: whole-body `50 kHz` in `150..1200 ohm`, non-zero limb channels), and derive all 56 parameters (`PRD:M-01..M-56`: Weight, BMI, Body Fat, Fat Mass, FFM, Subq Fat, Visceral Fat, TBW, **Cole dual-frequency ECW / ICW / ECW-TBW**, BCM, **Janssen SMM / SMI**, Bone Mass, Protein, Katch-McArdle BMR, TDEE, Metabolic Age, Composite Score, **5-segment LA/RA/TR/LL/RL Muscle & Fat with mass-sum conservation**, Bilateral Symmetry, ALM).
   - **Cult Smart Scale Pro (Feet-Only, handle unheld, `0xCF` status `0x00`):** Suppress 8E segmental/ECW claims cleanly (`INV-03`) and fall back transparently to weight + explicit `ANTHROPOMETRIC_ESTIMATE`.
   - **Cult Smart Body Fat Scale (`CS-BF01`, 4-Electrode, 20+ parameters):** Execute `0xFFF1` handshake (`FD 37` mode/unit, `F1` RTC sync with `3000 ms` fallback timer, `F2 00` history / `F2 01` clear), enforce `WeightLockFrames = 3` / `HRLockFrames = 3` and `OneByone` 20-byte ATT coalesced-duplicate vs 18-byte+ history disambiguation (`INV-21`, `SHAME-005`), retrieve weight + heart rate / impedance where emitted by firmware, compute 20+ parameters labeled explicitly with `MetricProvenance`.
   - **Immediate Member-Scoped Logging:** Auto-assign weigh-ins ONLY on unique rolling-weight match within `auto_match_window_kg` (default `2.5 kg`); 0 or 2+ matches mark `assignment_state = 'pending_review'` and open the interactive member chooser (`INV-09`) before assigning. Every locked reading commits permanently per `profile_id` and calendar day (`LocalDate`) in Room in `< 50 ms` (`INV-06`).

---

### 3. Android Identity, Keystore Signing, Play Protect, and Cloudflare-Aligned Security

Design `:app` and all modules under `security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `r8-analyzer`, and Cloudflare security discipline:

1. **Application ID, Namespace, and Non-Infringing Branding:**
   - Pin canonical `applicationId = "org.opencult.scale"` (clean reverse-DNS namespace matching `org.opencult.scale`; NEVER use `com.example.*` and NEVER infringe `com.cultfit.*` / `com.curefit.*` per `AGENTS.md §3.5`).
   - Pin `minSdk = 31`, `compileSdk = 36`, `targetSdk = 36` (verified against installed `android-36`; NEVER target unreleased preview API levels like `37` that cause `'App not installed'` rejections). Align Gradle/JDK toolchain cleanly with host JDK/Studio JBR (`Java 17` or `21` bytecode target).
2. **Release Keystore & Zero-Fallback Signing Config:**
   - Configure release signing in `app/build.gradle.kts` from gitignored `keystore.properties` + `keystore/opencult-release.jks` (or CI env secrets `OPENCULT_KEYSTORE_PATH`, `OPENCULT_KEYSTORE_PASSWORD`, `OPENCULT_KEY_ALIAS`, `OPENCULT_KEY_PASSWORD`) using 4096-bit RSA, `enableV1Signing = true`, and `enableV2Signing = true`.
   - Generate a local gitignored release keystore if missing (via `keytool` using cryptographically random passwords stored in gitignored `keystore.properties`, printing ONLY non-secret `SHA-1` and `SHA-256` fingerprints for Google OAuth setup). **Release builds MUST fail immediately if release keystore properties are missing; silent fallback to the debug keystore is permanently banned.**
3. **Google Play Protect & Android Component Hardening (`play-policy-insights`, `android-permissions-security`, `android-intent-security`):**
   - **Zero dangerous/restricted permissions that trigger Play Protect or Android 13+ Restricted Settings flags:** Permanently ban `BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, `REQUEST_INSTALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `READ_MEDIA_*`.
   - Use minimal BLE permissions (`BLUETOOTH_SCAN` with `android:usesPermissionFlags="neverForLocation"`, `BLUETOOTH_CONNECT`), Health Connect permissions (`PRD:HC-01`, `TRD:HC-01..HC-10`), `INTERNET`, and `POST_NOTIFICATIONS` only where justified. Implement the 3-state runtime permission flow (`checkSelfPermission` -> rationale UI -> `ActivityResultContract` -> `ACTION_APPLICATION_DETAILS_SETTINGS`), never caching permission booleans (`android-permissions-security`).
   - Explicit `android:exported="false"` on all internal services/receivers/providers; `PendingIntent.FLAG_IMMUTABLE` everywhere; `ContextCompat.RECEIVER_NOT_EXPORTED` for dynamic receivers; `IntentSanitizer` on external/OAuth redirect Intents; `FileProvider` scoped to explicit export subdirectory (`path="/exports/"`, never root `/`); `android:allowBackup="false"` with explicit `data_extraction_rules.xml` excluding auth/DB secrets; `android:usesCleartextTraffic="false"` with explicit `network_security_config.xml` enforcing HTTPS TLS 1.2+.
   - Store Supabase session tokens inside Android Keystore AES-256-GCM encrypted storage, NEVER plain `SharedPreferences` or unencrypted DataStore. Never ship `service_role`, DB passwords, or private signing keys inside APK assets/BuildConfig (`INV-08`, `INV-15`).
   - Enable `isMinifyEnabled = true` and `isShrinkResources = true` on `release` with explicit Kotlinx Serialization / Supabase / Health Connect / `blessed-kotlin` ProGuard rules audited cleanly (`r8-analyzer`).
4. **Cloudflare Security & Repository Discipline (`cloudflare`, `workers-best-practices`, `cloudflare-one`):**
   - Apply Cloudflare security hygiene to the entire repository AND to any optional companion Cloudflare Worker / Pages privacy-policy / OAuth callback / Access proxy if configured: zero secrets in repo or `wrangler.toml`, secrets injected via `wrangler secret` / `.env.local` only, `crypto.randomUUID()` / `crypto.getRandomValues()` instead of `Math.random()`, constant-time secret comparison, streamed/bounded bodies, structured observability, explicit fail-closed HTTP errors, and never bypassing Cloudflare Access headers.

---

### 4. Native Google Health Connect Integration (`androidx.health.connect:connect-client`)

Implement production-grade native two-way Health Connect synchronization in `:core:healthconnect`:

1. **Manifest & Lifecycle Contract:**
   - Declare every Health Connect permission in `TRD.md §5`.
   - Register `<activity-alias>` for `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13) and `ViewPermissionUsageActivity` with `android.intent.action.VIEW_PERMISSION_USAGE` + `android.intent.category.HEALTH_PERMISSIONS` (Android 14+).
   - Handle `HealthConnectClient.getSdkStatus(context)`: `SDK_AVAILABLE`, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` (prompting official Health Connect Play Store update cleanly), and `SDK_UNAVAILABLE`.
2. **Idempotent Instant Auto-Write (`INV-04`, `INV-05`):**
   - On every locked scale weigh-in assigned to the active Health Connect household profile (when permissions are granted), automatically map and write using `Instant` + `ZoneOffset`, typed units (`Mass.kilograms`, `Percentage`, `Power.kilocaloriesPerDay`, `Length.meters`), and `Metadata.automaticallyRecorded(device = Device(manufacturer = "Cult", model = modelTier, type = Device.TYPE_SCALE), clientRecordId = "opencult:${measurement.id}:${recordTag}", clientRecordVersion = measurement.version)`:
     - `WeightRecord`
     - `BodyFatRecord`
     - `LeanBodyMassRecord` (FFM)
     - `BoneMassRecord`
     - `BodyWaterMassRecord` (`TBW_kg`)
     - `BasalMetabolicRateRecord`
     - `HeartRateRecord` (when captured)
     - `HeightRecord` (when profile height changes)
   - For `4E CS-BF01` / `Pro Feet-Only`, write verified weight + honestly labeled aggregate body-comp permissions cleanly without claiming 8E hardware fidelity; tolerate partial permission subsets (write what the user granted, record exact per-type status in `health_connect_sync_ledger`).
3. **Incremental Inbound Read (`ChangesToken`), Echo Prevention, Backfill & Delete Sync (`INV-05`, `INV-16`):**
   - Read daily `StepsRecord`, `SleepSessionRecord`, `TotalCaloriesBurnedRecord` (with `ActiveCaloriesBurnedRecord` fallback) for Home Bento Page 2, and optional external `WeightRecord` strictly excluding `record.metadata.dataOrigin.packageName == BuildConfig.APPLICATION_ID` (`INV-05`).
   - Handle expired `ChangesToken` (`ChangesResponse.changesTokenExpired`) cleanly by resetting and re-syncing window.
   - Implement batch historical backfill with progress indicator (`Sync Center`) and immediate `HealthConnectClient.deleteRecords` on tombstone deletion (`INV-16`).

---

### 5. Supabase Backend, Offline-First Room, and Permanent Calendar-Day Persistence

1. **Permanent Local-First Ledger (`< 50 ms`, `INV-06`):**
   - Room SQLite (`2.7+` KSP) mirrors `household_profiles` (enforcing `<= 10` active members), `scale_devices`, `measurements` (indexed by `(profile_id, local_date, measured_at DESC)` where `deleted_at IS NULL` so every member's readings for every specific calendar day stay permanently stored and queryable instantaneously), immutable `raw_ble_packets` (`INV-01`), `health_connect_sync_ledger`, and `sync_outbox`.
   - Soft deletes (`deleted_at`) propagate cleanly across Room, Health Connect, and Supabase before compaction (`INV-16`).
2. **Zero-Refresh Supabase Realtime Sync (`INV-07`, `INV-08`):**
   - Configure `supabase-kt` (`Auth`, `Postgrest`, `Realtime` with Ktor CIO/OkHttp TLS) against the live project schema (`supabase/migrations/20261126000001_opencult_initial_schema.sql`).
   - Run WorkManager `OutboxDrainer` (battery-aware, exponential backoff) and authenticated `supabase.channel("opencult-sync").postgresChangeFlow<PostgresAction>(schema = "public")` converging remote changes across devices into Room/Kotlin `StateFlow` in `<= 2 s` without manual refresh, ignoring self-echoes by `mutation_id`.
   - Enforce `(SELECT auth.uid()) = account_id` RLS (`ENABLE` + `FORCE`) with zero `service_role` on client (`INV-08`).

---

### 6. Visual Reverse Engineering, Motion/Micro-Engineered UI, and Rejection of Generic Native Widgets

Reverse-engineer every layout proportion, spacing, pastel surface, elevation, typography rule, chart, micro-interaction, and spring curve directly from `DRD.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and `prototype/` (following `INV-22` whenever `prototype/` differs from `DRD.md`, such as `SHAME-006`):

1. **Strict Ban on Stock Native Popups & Generic Material Defaults:**
   - **NEVER use `android.app.DatePickerDialog` or stock Material `DatePicker` dialogs.** Build a custom OpenCult **Daylight/Obsidian Squircle Calendar Sheet** (`28.dp` radius, `Plus Jakarta Sans` `tnum` grid, colored activity dot indicators showing saved weigh-ins per member per day, horizontal swipe/pager month transitions driven by `OpenCultMotion.SpatialDefault` and `SheetEnter`).
   - **NEVER use stock Android `Spinner`, stock unstyled `DropdownMenu` popups, stock `Material 3 NavigationBar`, stock `Card`, stock `FloatingActionButton`, or external charting libraries (MPAndroidChart / Vico / Compose-Charts).** Replace every selector (`Weight v`, `Weekly v`, profile switch, scale switch) with custom animated pills and squircle modal sheets obeying `MICROENGINEERING.md §2` (`4-stage Touchdown / Tracking / Payoff / Consequence`).
2. **Binding Custom Compose `Canvas` & Motion Specifications:**
   - Implement `:core:designsystem` (`OpenCultTheme` + `OpenCultMotion` + `DRD:T-01..T-25`) first with ZERO hardcoded `Color(0x...)` or `sp`/`dp` or `spring()` literals in `:feature:*` (`INV-10`, `INV-19`).
   - Implement all custom `Canvas` components via `DrawScope` with cached `Path`/`PathEffect` inside `remember(size)` and `Modifier.graphicsLayer { ... }` lambda deferral (`jetpack-compose-performance`):
     - `DRD:C-01` Hero 9-column vertical rounded capsule spark-bars (`4.dp` width, `6.dp` gap, `#EF4444` spike).
     - `DRD:C-02` Asymmetric 2-column Bento Overview Grid (`212.dp` Lavender card beside stacked `100.dp` Sage/Peach cards, `<` / `>` pager, cubic Bezier wave + hollow terminal dot).
     - `DRD:C-03` 3-Zone Semi-Circular Arc Gauge (`180 deg`, `18.dp`, `StrokeCap.Round`, `4 deg` gaps, `#2185F7` Optimal / `#F4B428` Elevated / `#EF4444` High per `DRD.md` & `INV-22`, polar angular interpolation for `24.dp` ringed thumb, never Cartesian chord shortcut).
     - `DRD:C-04` Stippled Dot-Matrix Bar Chart (clipped rounded-top columns, deterministic `2.2.dp` dots on `6.dp` pitch with `3.dp` odd-row horizontal stagger, solid `inkPrimary` active bar + `6.dp` inner dot + `1.dp` dashed connector + non-clipping floating status pill, zero extra footer rows inside chart card).
     - `DRD:C-05` Detached `#0F1115` Obsidian Pill Dock (`64.dp`, `32.dp` radius, `46.dp` active white circle) + `64.dp` `#2185F7` Measure FAB (`>= 116.dp` scroll inset clearance on every screen, `INV-13`).
     - `DRD:C-06` Interactive 5-Segment Anatomical Body Map (honestly gated on `4E` / `Pro Feet-Only`, clamped floating callouts, balance bars).
     - `DRD:C-07..C-10` Onboarding Breakout Device-in-Device Preview, Live 8-Pad/4-Pad Weigh-In Sheet (`tnum`, deduplicated lock haptic, lifecycle-gated Stage 1 `AmbientRadar`), 56-Param Searchable Categorized Accordion (`160 ms` max stagger cap), BLE Wire Inspector (`JetBrains Mono`), Custom Calendar Day Ledger, Household Manager (10 profiles), and Sync Center.
   - Honor `enableEdgeToEdge()`, `ANIMATOR_DURATION_SCALE == 0f` (`INV-20`), `>= 48.dp` touch hitboxes (`INV-18`), and both **Daylight Clinical Pastel** + **Obsidian Night** themes on compact `360.dp` width viewports (`INV-13`).

---

### 7. Mandatory Vertical-Slice Execution Plan & Exit Verification Gates

Execute and verify each slice completely before advancing (all code -> tests -> lint -> UI verification):

- **Slice S0:** Multi-module Gradle Kotlin DSL scaffold (`:app`, `:core:*`, `:feature:*`), `org.opencult.scale` applicationId, gitignored release keystore setup with zero debug-fallback, `network_security_config.xml`, `:app:assembleDebug` + `:app:assembleRelease` compile checks.
- **Slice S1:** `:core:designsystem` tokens (`OpenCultTheme`, `OpenCultMotion`), custom Canvas primitives (`DRD:C-01`..`C-06`), custom Calendar & Pill Selector sheets, Compose UI bounds/theme/reduced-motion tests.
- **Slice S2:** `:core:bia` pure-Kotlin 56-param dual-frequency Cole + Sun + Janssen + Wang + `Wla25` segmental engine + `CS-BF01` 20+ param anthropometric engine + plausibility gate (`INV-02`, `INV-03`). Run JVM parameterized unit tests.
- **Slice S3:** `:core:ble` (`blessed-kotlin`) GATT state machines for Cult Pro `8E` (`0xCF` + 10-ch `0xBE` + 4x `0xDF`), Pro Feet-Only (`0x00`), and `CS-BF01` `4E` (`FD 37`/`F1`/`F2` + coalesced duplicate guard `INV-21`), plus `rea` reconstruction ledger & captured frame replay unit tests.
- **Slice S4:** `:core:database` Room SQLite + `sync_outbox` + calendar-day measurement index + 10-profile limit + `±2.5 kg` auto-match / ambiguity flow (`INV-06`, `INV-09`, `INV-16`). Run Robolectric DAO + migration tests.
- **Slice S5:** `:core:healthconnect` native AndroidManifest aliases + `HealthConnectClient` idempotent auto-write (`Metadata.automaticallyRecorded`) + `ChangesToken` read (`dataOrigin` exclusion) + backfill + tombstone delete tests (`INV-04`, `INV-05`).
- **Slice S6:** `:core:sync-supabase` Encrypted Keystore session storage + Supabase Auth + WorkManager `OutboxDrainer` + Realtime `postgresChangeFlow` (`mutation_id` dedup, `<= 2 s` convergence) + `supabase migration list` / RLS check (`INV-07`, `INV-08`).
- **Slice S7-S10:** All 9 feature modules (`DRD:S-01`..`S-09`) + Lossless JSON/CSV export + R8 Release verification + Security & Play Policy Audits (`security-audit`, `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `anr-audit-and-fix`) + Visual UI Verification (`connectedDebugAndroidTest` / Robolectric Compose UI bounds + Daylight/Obsidian screenshot verification via `android screen capture` / Compose Preview Screenshot Testing).

### 8. Final Non-Bypassable Release Gate

Before returning control to the user:
1. Run full Gradle build, debug + release signing check, unit tests, Robolectric tests, Android Lint, Compose UI bounds/screenshot tests, `supabase migration list`, secret/`.gitignore` scan, and Play Policy / Intent / Permissions security checks **after your final edit**.
2. Inspect every generated UI screenshot directly (`android screen capture` or test screenshot artifacts) across Light & Dark mode at `360.dp` width; verify zero clipping over the dock (`INV-13`), zero generic native calendar/dropdown popups, and exact `DRD.md` Canvas fidelity.
3. Reconcile `HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and Second Brain (`project="opencult"` capsule + linked `handoff`).
4. Report exact commands, raw exit codes, `Verified / Unverified / Unknown` boundaries, SHA-256 fingerprint of the release signing config (never secrets), and only append `<!-- GOAL_COMPLETE -->` once 100% of verification checks pass.
```