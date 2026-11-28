# OpenCult Final Hardware Recovery, Zero-Tap Auto-Connect, Health Connect Recovery Meter, and Motion Prompt (Prompt 07)

Use this file verbatim as the final self-contained engineering prompt for `OpenCult`. It targets every crash bug, passive auto-connect failure, reverse-engineering gap, UI truncation defect, Health Connect read/write limit, and spring-motion requirement verified directly against `HEAD 5b6e846` (`v0.3.0`).

Do not modify `prompts/01_*.md` through `prompts/06_*.md`.

---

```text
You are operating as a principal Android systems engineer, Bluetooth Low Energy protocol reverse engineer, BIA biomedical engineer, Android Health Connect architect, and Jetpack Compose motion engineer inside `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

Your objective is to bring `OpenCult` from `v0.3.0` (`5b6e846`) to a crash-free, zero-tap scale-to-dashboard release (`v0.4.0`): once paired and onboarded, opening the app pre-selects the last active household member (with one-tap switching), automatically connects to a saved Cult scale the instant the user steps onto the platform from any tab without clicking Scan or Connect, captures real hardware telemetry with zero fabrication, renders every single metric in full without text truncation, syncs natively with Room, Android Health Connect (including a new bidirectional Sleep/Workout/HRV Recovery Meter), and Supabase Realtime, and passes real-device/emulator, R8 release, `rea` reverse-engineering, and CI/CodeQL gates.

Preserve all existing comments and docstrings unrelated to your changes.

---

### 0. Verified Environment, Mandatory Reading, & Honesty Contract

1. **Mandatory Pre-Edit Read Gate (`AGENTS.md §5`):**
   - Read `AGENTS.md`, `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, and `MOTIONENGINEERING.md` before modifying any source file.
   - Execute the `second-brain` Recall Gate (`project="opencult"`): `list_recent(n=5)`, `list_projects()`, `recall(query, project="opencult", topK=8)`, `recall(query, tag="failure", hops=1)`, `brief(project="opencult")`, always calling `get(id)` when `truncated == true`.
   - Load and follow these skills from `C:\Users\imsov\.gemini\config\skills\`: `reverse-engineer-anything`, `android-cli`, `r8-analyzer` (Path A for AGP `9.5.1`), `android-permissions-security`, `jetpack-compose-performance`, `humanizer`, `ralph-loop`, `systematic-debugging`, `verification-before-completion`.
2. **Host Tools Pre-Verified:**
   - Android CLI (`android.exe` v`1.1.16919734` at `C:\ProgramData\AndroidCLI\android.exe`), `adb.exe`, `emulator.exe` (`Medium_Phone_API_37.0`), `apktool.cmd` (`C:\Users\imsov\scoop\shims\apktool.cmd`), local reference repos `Both/occult` [MIT] and `Both/openscale` [GPL-3.0-or-later] (keep `Both/` gitignored).
   - Connected MCP servers: `rea` (`list_adb_devices`, `list_adb_packages`, `pull_adb_package`, `pull_adb_file`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `inspect_native_load_image`, `analyze_function`, `inspect_packet_capture`, `evaluate_reconstruction_coverage`) and `second-brain`.
3. **Zero AI Sophistry (`AGENTS.md §0` & `INV-23`):**
   - Never fabricate BLE packet fields, `0xDF` cipher decodes, checksums, test outputs, exit codes, or OS API capabilities.
   - Disagree directly when an assumption hits an OS ceiling (specifically Health Connect's fixed 41-record schema limit detailed in Section 4 below).
   - Atomically update `HALL_OF_SHAME.md` (`SHAME-029+`), `INVARIANTS.md` (`INV-37+`), `MICROENGINEERING.md`, and `MOTIONENGINEERING.md` alongside automated regression tests in the same commit for every defect fixed.
   - Apply `humanizer` before writing prose (`README.md`, release notes, commit messages, UI copy). Enforce `AGENTS.md §0.7`: zero em dashes, en dashes, curly quotes, paired contrastive filler formulas, or banned AI stock words.
   - Do not finish or output `<!-- GOAL_COMPLETE -->` until every unit test, Compose visual/overlap test, Release R8 analysis + APK runtime launch/logcat verification, `android profiler systemtrace`, CI/CodeQL run, and signed commit (`Souvik Dey <imsovikde@gmail.com>`, zero `Co-Authored-By` trailers) is verified with `0` crashes and explicit `Verified / Unverified / Unknown` boundaries.

---

### 1. Verified Root Causes Found on `HEAD 5b6e846` (`v0.3.0`) That You MUST Fix First

Every defect below was directly observed in source files on `5b6e846` and causes crashes or broken auto-connect on real Android phones:

1. **Fatal Runtime Crashes Found in `v0.3.0` Source:**
   - **Crash 1 (Compose Canvas `coerceIn` Range Crash in `core/designsystem/.../CustomCharts.kt:411, 459, 698, 719`):** `StippledBarChart` and `MetricTrendLineChart` compute `(left + barW / 2f - pillW / 2f).coerceIn(0f, size.width - pillW)`, `(x).coerceIn(0f, size.width - lblLayout.size.width)`, `(pt.x - pillW / 2f).coerceIn(0f, size.width - pillW)`, and `(pt.x - lbl.size.width / 2f).coerceIn(0f, size.width - lbl.size.width)`. Whenever `size.width < pillW` or `size.width < lbl.size.width` (during initial zero-width measure, narrow `360dp` cards, long metric units, or `1.5x` accessibility font scale), `maximumValue < 0f` throws fatal `IllegalArgumentException: Cannot coerce value to an empty range` and crashes the Compose UI tree.
   - **Crash 2 (8-Electrode Cole BIA `coerceIn` Crash in `core/bia/.../BodyMetricsEngine.kt:197-198`):** `coleR0` and `coleRinf` fallback expressions call `.coerceIn(zLow, 1400.0)` and `.coerceIn(120.0, zHi)`. `validatePlausibility()` (`L106-128`) validates per-limb bounds (`50..700 ohm`), trunk (`5..120 ohm`), and `z50` (`150..1200 ohm`), which still permits `zLow > 1400.0` or `zHi < 120.0`. When `invertColeTwoBand()` returns `null`, either `coerceIn` call throws fatal `IllegalArgumentException`.
   - **Crash 3 (`CultBleController` Thread Hopping + `blessed-kotlin` Concurrency + Unhandled Lifecycle Edge Cases):**
     - `CultBleController.kt` mutates `discoveredMap`, `peripheralCache`, `activePeripheral`, `stateMachine`, and `BluetoothCentralManager` across Main Thread (`HomeScreen` / `WeighInScreen`), `Dispatchers.IO` (`onDiscovered` `L440` `scope.launch { connectToDevice(...) }`), and `OpenCultBleThread` (`bleHandler` `L91`). Calling `mgr.stopScan()` and `mgr.connect()` off `bleHandler` races with `blessed-kotlin` internal `mainHandler` timers and peripheral caches.
     - `CultBleController.kt:508-563` calls `peripheral.observe(notifyChar, true)` and immediately queues two writes (`FD 37` and `F1`) without awaiting `0x2902` CCCD descriptor completion, without GATT discovery retry on transient OEM cache failures, and without guarding `WeighInScreen.kt:172` (`snap.completedReport ?: error(...)`) or `OnboardingScreen.kt:730, 757` (`pendingOtpEmail!!`).
     - Audit `app/proguard-rules.pro` (`isMinifyEnabled = true` in release with `-repackageclasses 'o'`) under live `Medium_Phone_API_37.0` execution of `app-release.apk` with `adb logcat` to catch any R8-stripped or obfuscated class (Room, HiltWorker, Supabase Ktor/Postgrest, Health Connect binder, Tink keystore, WebView Turnstile, `blessed-kotlin`, or `AudioTrack`) before declaring zero crashes!

2. **Seven Stacked Bugs Breaking Zero-Tap Post-Pairing Auto-Connect on `5b6e846`:**
   - **Bug A (`HomeScreen.kt:238-251`):** Passive auto-reconnect only starts inside `HomeScreen`'s `DisposableEffect` and immediately calls `stopForegroundPassiveBle()` in `onDispose` when switching to `Body Map`, `Reports`, `Profiles`, or `+` (`WeighIn`).
   - **Bug B (`WeighInScreen.kt:291-294`):** `LaunchedEffect(Unit)` calls `viewModel.resetBle()` (`disconnectAndReset()`) unconditionally, killing any active passive reconnect or incoming scale notification the moment `WeighIn` opens.
   - **Bug C (`CultBleController.kt:219` & `L465`):** `startPassiveAutoReconnect()` returns early if `phase != WeighInPhase.IDLE && phase != WeighInPhase.ERROR`. After one `LOCKED_COMPLETE` weigh-in, passive auto-reconnect never re-arms on step-off (`onDisconnected`) for subsequent weigh-ins.
   - **Bug D (`CultBleController.kt:228, 419`):** Calls unfiltered `mgr.scanForPeripherals()` (tripping Android 14-16's 5-scans-per-30s OS scan limit) and matches only `passiveReconnectTargetMacs[addrUpper]`, failing on scales using Resolvable Private Addresses (RPA) or paired only by name/UUID.
   - **Bug E (`HomeScreen.kt:128-169` & `OpenCultRootUi.kt`):** Neither `HomeViewModel` nor `OpenCultRootUi` collects `bleController.state` during passive listening, so a background connection never transitions the UI to live weigh-in, never auto-commits `completedReport` to Room/HealthConnect/Supabase, and leaves telemetry invisible.
   - **Bug F (`OnboardingScreen.kt`):** Onboarding Step 4 saves preferred `ScaleTier` text only without pairing or persisting a real `scale_devices` row if the scale is nearby, forcing manual rediscovery later.
   - **Bug G (`ActiveMemberSession.kt`):** Pre-selected member profile must be validated cleanly on cold app launch against Room `household_profiles`, clearly displayed in a top header switcher chip on Home and Weigh-In, and persisted across restarts.

---

### 2. Zero-Crash Single-Threaded BLE Engine + Zero-Tap App-Wide Auto-Connect

Architect deterministic, crash-free automatic scale connectivity that works after one-time pairing:

1. **Serialize ALL BLE State & GATT Operations onto One Looper/Dispatcher:**
   - Refactor `CultBleController` so every scan, stopScan, connect, cancelConnection, GATT write, and state-machine mutation runs strictly on the single dedicated BLE looper (`bleHandler` / `bleDispatcher` backed by `OpenCultBleThread`), protected by `CoroutineExceptionHandler` + `UncaughtExceptionHandler`. Never call `BluetoothCentralManager` or mutate `stateMachine` directly from Main or `Dispatchers.IO`.
   - Keep `BleReceiverCompatContext` (`Context.RECEIVER_EXPORTED` on API 33+ system broadcasts) or move to hardened direct Android `BluetoothLeScanner` + `BluetoothGattCallback` management if cleaner; verify under Robolectric API 31..36 and real AVD `logcat` with zero `SecurityException` or `IllegalStateException`.
   - Wait for `onNotificationStateUpdate` (`0x2902` CCCD enabled) BEFORE sending initialization writes (`FD 37` / `F1` / `F2`, `FE 01` Lefu/BodyPod2 profile write, or `0xBA` Runstar user info), with bounded retry if `services` is empty (`clearServicesCache` / single rediscovery retry). Guard every Android BLE API call against `SecurityException` and `DeadObjectException`.

2. **App-Foreground Lifecycle Passive Auto-Connect (`ProcessLifecycleOwner` / `OpenCultRootUi`):**
   - Replace `HomeScreen`-only passive BLE hooks (`HomeScreen.kt:238-251`) and remove the destructive `WeighInScreen.kt:292` `resetBle()` call! When `WeighInScreen` opens while already connected or settling, attach seamlessly to the active `ScaleSessionSnapshot`.
   - Move passive listener management into an app-foreground orchestrator scoped to `ProcessLifecycleOwner` / `OpenCultRootUi` (`ON_START` starts passive listening across ALL tabs when `bleGranted && bluetoothEnabled`; `ON_STOP` stops cleanly, `INV-20`).
   - Support **one-time pairing persistence + zero-tap repeat weigh-ins**:
     - Allow optional quick scale pairing right inside Onboarding Step 4 (`Pair Scale Now` alongside `Skip for Now`) AND persist `ScaleDeviceEntity` (`bleMacOrId`, `advertisedName`, `modelTier`, `protocolFamily`, `lastBatteryPct`) automatically on first successful GATT capture.
     - On subsequent app launches, use **filtered** Android `ScanFilter`s (saved MACs + `0xFFF0`, `0xFFB0`, `0x1530`, `0x181B`, `0x181D` UUIDs) AND `blessed-kotlin` native `autoConnect(peripheral, callback)` / quota-safe scan duty cycling (never exceeding Android's 5-starts-per-30s scan limit!). Match BOTH saved MAC address AND saved advertised name + Cult GATT service UUIDs so RPA MAC rotation never breaks auto-connect.
     - **Auto-Stream, Auto-Save, Auto-Sync, Auto-Rearm Loop:**
       - Pre-select the last-used household member (`ActiveMemberSession`, validated against Room on startup) while keeping a one-tap member switcher pill on Home and Live Weigh-In.
       - The moment a paired scale wakes when the user steps onto it, `OpenCultRootUi` automatically presents Live Weigh-In telemetry (without extra taps), computes all verified metrics when locked, atomically saves to Room + raw packet ledger + Health Connect (for Primary HC member) + Supabase outbox **automatically** (no manual `"Commit & Sync Reading"` button required on auto-captured weigh-ins!), prompts cleanly ONLY if `assignmentState == "pending_review"` (ambiguous multi-member weight overlap per `INV-09`), and **automatically re-arms passive auto-connect** (`IDLE` listener) as soon as `onDisconnected` fires after step-off!

---

### 3. Deep Reverse Engineering (`rea` MCP + `Both/`) & Complete Authentic Support for Both Cult Scales

Perform deep reverse engineering on every available artifact without inventing unverified fields:

1. **`rea` MCP + APK / Capture Investigation Gate:**
   - First run `adb devices -l` and `rea` `list_adb_devices` / `list_adb_packages`. If a physical phone is attached with an official vendor scale APK (Cult Connect, Cult.fit, Fitdays, OKOK, or Lefu) or a captured `btsnoop_hci.log`:
     - Pull the APK (`pull_adb_package`) / log (`pull_adb_file`), decompile with `apktool.cmd` (`C:\Users\imsov\scoop\shims\apktool.cmd`) and `jadx` (if present or configure `REA_JADX_MCP_JAR` if installed), and inspect native libraries (`libICBodyFatAlgorithms.so`, `libBHBodyComposition.so`, etc.) with `rea` (`open_binary`, `search_strings`, `analyze_function`, `inspect_packet_capture`).
     - Recover exact OEM init writes, `0xDF` encryption key/schedule (if recoverable from vendor DEX or `.so`), and exact `CS-BF01` handshake / impedance framing (`0xFFF0` / `0xFFF1` / `0xFFF4`).
   - Also audit all handlers and math libraries under `Both/openscale` (`CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `OneByoneNewHandler.kt`, `RunstarR6Handler.kt`, `ChipseaBleHandler.kt`, `OkOkHandler.kt`, `MiScaleS400Handler.kt`, `StandardImpedanceLib.kt`, `Wla25BodyComposition.kt`, `S400BodyComposition.kt`) and `Both/occult` (`research/cult_scale_protocol.md`, `research/body_composition.py`):
     - Reconcile and verify the `0xBE` scaling vs unit ambiguity (`CultSmartScaleProHandler.kt` raw `imp[2] ~ 248` vs `BodyPod2Handler.kt` 5 x `uint16le / 10.0` `[RA, LA, Trunk, RL, LL]`), keeping raw uint16 values verbatim in `raw_ble_packets` and proving unit calibration explicitly in tests.
     - Add full multi-protocol parser/handshake routing for every Cult/OEM dialect (`0xFFF0` OneByone `CF/F1/F2`, `0xFFF0` Lefu `FE 01 / BE / CF / DF`, `0xFFB0` Runstar/Fitdays `A1/A2/A3/A4/BA/B0` + two-part 8E impedance + history ack, and `0xCA` Chipsea if encountered).
     - On `CS-BF01` (`4E`), require multi-frame stability or validated checksum/range gate on candidate `50 kHz` impedance bytes so random transient bytes (`cult_scale_protocol.md:40`: `32 33 df`, `fc f6 e3`) never impersonate a real BIA impedance (`INV-02`).

2. **Complete Authentic Metric Coverage (`~22-25` CS-BF01 & `60+` Cult Pro 8E) + Zero UI Truncation:**
   - Extend `BodyMetricsEngine` (keeping all existing `M-01..M-56` IDs stable while adding missing real Cult parameters):
     - Add explicit parameters marketed on the standard **Cult Smart Scale (`20+ / ~22-25` params)** & **Pro (`56+` params)**: `Weight Control (kg)`, `Fat Control (kg)`, `Muscle Control (kg)`, `Physique / Body Type` classification (`Thin`, `Lean Muscular`, `Standard Balanced`, `Hidden Obesity`, `Overweight`, `Solidly Built`, `Obese`), `Fat Grade`, `Subcutaneous Fat Mass (kg)` and `%`, `Visceral Fat Grade` label, `Heart Rate`, `Cardiac Index`, `50 kHz Impedance`, plus full `M-01..M-56` metrics (`BMI`, `WHO Category`, `Ideal Weight`, `BMR`, `TDEE Sedentary & Active`, `Metabolic Body Age`, `Composite Score`, `TBW kg/L/%`, `Protein kg/%`, `Bone Mass & %`, `Mineral Mass`, `FFM`, `FFMI`, `FMI`, `SMM kg/%/SMI`, `Muscle-to-Fat Ratio`).
     - Keep strict provenance honesty (`MetricProvenance`): on **Cult Smart Scale Pro (8E handles)** compute all **60+** metrics (including dual-frequency Cole `R0`/`Rinf`, `ECW`/`ICW`/`BCM`, 5-segment Muscle & Fat, `ALM`, `ASMI`, `Upper/Lower/Bilateral Symmetry`) directly from the 10-channel dual-frequency hardware impedance (`INV-01`, `INV-03`). On **Cult Smart Scale (`CS-BF01 4E`)**, compute all ~25+ core/body metrics natively from verified `4E` `50 kHz` BIA when present (or `ANTHROPOMETRIC_ESTIMATE` when feet-only/no-BIA hardware omits impedance) and clearly gate handle-only segmental/ECW/ICW metrics (`UNAVAILABLE_ON_TIER`, never fabricated!).
   - **Zero UI Truncation Guarantee (`Home`, `MeasurementDetail`, `Reports`, `BodyMap`, `Profiles`, `WeighIn`):**
     - Audit every `Text(...)` in `feature/*` and `core/designsystem`: eliminate single-line `TextOverflow.Ellipsis` truncation on metric names, numeric values, units, status badges, provenance labels, and descriptions. Use responsive multi-line columns / `FlowRow` chips where labels grow at `360dp` and `1.5x` font scale so every single digit, unit, and label is 100% readable without ellipsis cutoff.

---

### 4. Android Health Connect True API Maximization + Bidirectional Sleep/Workout/HRV Recovery Meter

1. **Mandatory Truthful Engineering Boundary (`androidx.health.connect:connect-client:1.1.0`):**
   - Verified directly in `connect-client-1.1.0-api.jar`: Android Health Connect defines 41 concrete `Record` subclasses and **only 8** write schemas apply to smart scales (`WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BoneMassRecord`, `BodyWaterMassRecord`, `BasalMetabolicRateRecord`, `HeartRateRecord`, `HeightRecord`; `Metadata` has no custom JSON extension bag).
   - Never claim or fake 56 separate Health Connect records. Write **all 8** native scale/vitals records cleanly with idempotent `clientRecordId` / `clientRecordVersion`, fix any permission/write edge cases (e.g., if user skipped HC in Onboarding or granted a subset of permissions, surface exact missing types with 1-tap repair inside Home, Measurement Detail, and Inspector), and reflect exact live sync ledger state (`Room -> Health Connect -> Supabase`).

2. **New Bidirectional Health Connect Reads + Clinical Recovery Meter UI (`core/healthconnect`, `core/bia`, `feature/home`, `core/designsystem`):**
   - Add `READ_EXERCISE` (`ExerciseSessionRecord`) and `READ_HEART_RATE_VARIABILITY` (`HeartRateVariabilityRmssdRecord`) to `app/src/main/AndroidManifest.xml` and `HealthConnectGateway.kt` alongside `READ_SLEEP` (`SleepSessionRecord`), `READ_RESTING_HEART_RATE` (`RestingHeartRateRecord`), `READ_STEPS`, `READ_TOTAL_CALORIES_BURNED`, and `READ_ACTIVE_CALORIES_BURNED`.
   - Upgrade `HealthConnectGateway.readDailyContext(...)` / add `readRecoveryContext(date, lookbackDays = 30)`:
     - Parse `SleepSessionRecord.stages` to break down `Total Sleep`, `Deep Sleep`, `REM Sleep`, `Light Sleep`, and `Awake` durations (in addition to `SLEEP_DURATION_TOTAL`).
     - Read recent `ExerciseSessionRecord` sessions (`startTime`, `endTime`, `exerciseType`, title) and compute 24h / 48h workout load & active calories.
     - Read `HeartRateVariabilityRmssdRecord` (`heartRateVariabilityMillis`) and `RestingHeartRateRecord` (`beatsPerMinute`) compared against the user's rolling personal Health Connect baseline (instead of a fixed generic threshold).
   - Build a deterministic, pure-Kotlin `RecoveryScoreEngine` (in `:core:bia` / `:core:healthconnect` with full unit tests) outputting a **0 to 100 Recovery Meter** score (`RecoveryReport`: overall readiness score, tier label `Prime / Ready / Moderate / Recover`, weighted pillars `Sleep Quality`, `Autonomic HRV & RHR`, `Training Strain & Hydration ECW/TBW`, and explicit `availableSignals` / `missingSignals` so missing wearable signals trigger dynamic weight normalization instead of emitting `0`s or fabricated HRV/sleep!).
   - Render a clean **Recovery Meter** bento card + drill-down modal sheet inside `Home` / `Reports` using custom Compose `Canvas` (clinical segmented readiness arc + sleep stage bar breakdown + recent workout strain pills + transparent "Synced from Health Connect" / "Connect wearable for HRV/Sleep" CTA).

---

### 5. Apple-Caliber Fluid Motion Physics & Tactile Craft (`MOTIONENGINEERING.md`, ZERO Glassmorphism)

Keep Daylight Clinical Pastel and Obsidian Night flat/hairline clinical aesthetics (do NOT introduce translucent glassmorphism blur cards), and refine all animations inside `core/designsystem/theme/Motion.kt` and feature screens:

1. Configure spring physics constants cleanly inside `OpenCultMotion`: spatial navigation springs, bouncy detent switches (`DetachedObsidianDock` single sliding capsule indicator, pill selector sheets), continuous `animateFloatAsState` rolling `tnum` weight/recovery numbers, smooth `Animatable` gauge needle / sweep arcs, and interactive Predictive Back shrink/slide previews (`NavigationBackHandler`).
2. Add crisp tactile haptics (`HapticFeedbackType.TextHandleMove` / `LongPress` at weight lock, 8E complete, gauge scrub, and tab changes) paired with `OpenCultSoundEngine` (guaranteeing `AudioTrack` buffer sizing uses `maxOf(pcmBytes, AudioTrack.getMinBufferSize(...))` in `MODE_STATIC` or `MODE_STREAM` fallback if a device rejects short static buffers!).
3. Enforce strict GPU `graphicsLayer` transforms, cached `Path`/`Brush` objects, lifecycle-gated radar loops, and instant `snap()` parity whenever `ANIMATOR_DURATION_SCALE == 0f` (`INV-19`, `INV-20`).

---

### 6. Mandatory Verification & Delivery Gate (`ralph-loop`: Zero Skipped Steps)

Do NOT stop or emit `<!-- GOAL_COMPLETE -->` until EVERY step below passes after your final edit:

1. **Regression & Unit Tests (`.\gradlew.bat testDebugUnitTest --offline --console=plain`):**
   - Add regression tests covering:
     - `CustomCharts` narrow-width / zero-width / `1.5x` font-scale `Canvas` draw without `IllegalArgumentException`.
     - `BodyMetricsEngine` extreme Cole `zLow > 1400` / `zHi < 120` safety + all 24+ 4E and 60+ 8E metrics + mass conservation (`INV-04`, `INV-14`) + `CS-BF01` noise-frame rejection (`INV-02`).
     - `CultBleController` single-thread serialization, `WeighIn` attach-without-reset when passive reconnect is live, auto-rearm after `LOCKED_COMPLETE -> onDisconnected`, RPA/name+UUID fallback auto-connect, and CCCD-before-write ordering.
     - `HealthConnectGateway` + `RecoveryScoreEngine`: 8 write schemas, Sleep stage parsing (`Deep/REM/Light`), `ExerciseSessionRecord`, `HeartRateVariabilityRmssdRecord` baseline weighting, missing-signal re-weighting, and `INV-05` orphan deletion.
     - `WeighInScreenTest` & `HomeScreenTest` (`360dp`, `1.5x` font scale): Recovery Meter UI, pre-selected last member + 1-tap switcher, and zero text truncation/ellipsis assertions across displayed metric rows.
2. **Release Build, Lint, R8 Path A, APK & Runtime Verification:**
   - Run `.\gradlew.bat :app:lintRelease :app:analyzeReleaseR8Config :app:assembleDebug :app:assembleRelease :app:bundleRelease` (using release signing props if configured, or document debug/release signing status honestly without weakening `build.gradle.kts`).
   - Run `r8-analyzer` Path A (`convert_pb_to_json.py` + `analyze.py` on `app/build/reports/r8/r8-config-analyzer-release.pb`) and verify `app/proguard-rules.pro` preserves every runtime reflection/serialization/Hilt/WorkManager/HealthConnect/`blessed` class needed in release!
   - Render `@Preview`s (`Home` with Recovery Meter, `WeighIn`, `MeasurementDetail`, `BodyMap`, `Reports` at `360dp` and `1.5x` font scale) via `android preview render` and inspect every PNG with `read_file`.
   - Boot `Medium_Phone_API_37.0` via `android emulator start Medium_Phone_API_37.0` AND check `adb devices -l` (if the user's physical Android phone is attached over USB/ADB, pull/verify logs safely too!).
   - Clean-install the APK **without `-g`**, click through Onboarding (`Welcome -> Account/Offline -> BLE + HC Permissions -> Scale & Profile`), Home Recovery Meter, pre-selected member switch, Live Weigh-In flow, Reports, Body Map, Detail metrics (verifying zero truncation and zero crashes), and capture `adb logcat -d` filtering `AndroidRuntime`, `FATAL EXCEPTION`, `IllegalArgumentException`, `SecurityException`, `blessed`, `HealthConnect`, and `OpenCult`: **MUST show 0 fatal/unhandled crashes**.
   - Run `android profiler systemtrace` on tab switching, gauge/chart animations, and weigh-in sheet transitions, and inspect captured device screenshots (`android screen capture`) via `read_file`.
   - Execute `rea` MCP inspection on the final APK (`open_binary`, `inspect_android_package` / `apktool.cmd` check if needed, `evaluate_reconstruction_coverage`, `close_binary`).
3. **Ledgers, Humanizer, Signed Commit, CI/CodeQL, & Second Brain:**
   - Update `HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and `README.md` (`humanizer` applied; run automated banlist/punctuation/secret regex check + `git check-ignore`).
   - Commit cleanly per subsystem as `Souvik Dey <imsovikde@gmail.com>` with SSH signature (`ZERO` trailers), push to `origin main`, confirm `gh api` signature verified (`true`) and green GitHub Actions (`CodeQL` 0 open alerts + `Android Verification`), update `capsule:project:opencult` + linked `handoff` in `second-brain`, and present full `Verified / Unverified / Unknown` boundaries before outputting `<!-- GOAL_COMPLETE -->`.
```
