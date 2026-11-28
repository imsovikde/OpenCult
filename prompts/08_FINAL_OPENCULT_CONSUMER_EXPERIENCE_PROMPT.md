# OpenCult Consumer Experience, Automatic Hardware Adaptation, Full-Duration Capture, and Health Recovery Prompt (Prompt 08)

Use this file verbatim as the self-contained engineering prompt for `OpenCult`. It builds on verified code state `HEAD` (`v0.3.0` app code plus Prompt 07 audit findings) and targets `v0.4.0` as an everyday consumer wellness app with zero developer jargon, full-duration measurement completion, automatic scale capability adaptation, zero manual sync buttons, complete plain-English parameter visualization, and full device verification.

Do not modify `prompts/01_*.md` through `prompts/07_*.md`.

```text
You are acting as a principal Android product engineer, Bluetooth Low Energy reverse engineer, biomedical composition engineer, Android Health Connect architect, and Jetpack Compose motion designer inside `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

Your objective is to transform `OpenCult` (`v0.3.0` codebase) into a consumer-first, zero-jargon, crash-free Android release (`v0.4.0`) that feels as effortless as the best commercial health companions without paywalls or missing integrations:

1. Zero technical strings or developer internals anywhere in user-facing screens (no BLE, hex, GATT, UUID, MAC, dBm, XOR, CS-BF01, 4E, 8E, M-16 codes, raw packet counters, or manual fixture simulators exposed to end users). Only plain model names appear: **Cult Smart Scale** and **Cult Smart Scale Pro**.
2. Guided two-permission onboarding (Bluetooth & Nearby Devices, then Google Health Connect) followed immediately by live scale scanning and one-tap pairing inside Onboarding, which permanently configures the app for the detected hardware model.
3. App-wide zero-tap automatic measurement from Home (or any active tab): step onto the paired scale, the measurement sheet opens immediately, stays live until weight, body composition, and heart rate pulse all finish cleanly without early cutoff, automatically saves locally, automatically syncs with Google Health Connect and Supabase cloud, and automatically resets for the next weigh-in.
4. Dynamic hardware capability switching: when **Cult Smart Scale** is paired, Pro-exclusive capabilities such as the Segmental Body Map tab, left/right arm and leg compartments, and dual-frequency intracellular/extracellular water stay automatically hidden and turned off, while 100% of the scale's complete ~24 to 25 parameters appear in full without locked placeholders. When **Cult Smart Scale Pro** is paired, the full 56+ / 60+ suite and Segmental Body Map turn on automatically.
5. Minimal buttons paired with rich, immediate data visualization: show ALL active parameters right inside Home and intuitive visual charts using full-form plain English names (never cryptic abbreviations or multi-hop menus) with zero text ellipsis cutoff.

Preserve all existing comments and docstrings unrelated to your edits.

### 0. Verified Environment, Mandatory Reading, and Honesty Contract

1. **Pre-Edit Read Gate (`AGENTS.md §5`):**
   - Read `AGENTS.md`, `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, and `MOTIONENGINEERING.md` before changing source code.
   - Run the `second-brain` Recall Gate (`project="opencult"`): `list_recent(n=5)`, `list_projects()`, `recall(query, project="opencult", topK=8)`, `recall(query, tag="failure", hops=1)`, `brief(project="opencult")`, and `get(id)` for every truncated result.
   - Load and apply these skills from `C:\Users\imsov\.gemini\config\skills\`: `reverse-engineer-anything`, `android-cli`, `r8-analyzer` (Path A for AGP `9.5.1`), `android-permissions-security`, `jetpack-compose-performance`, `humanizer`, `ralph-loop`, `systematic-debugging`, `verification-before-completion`.
2. **Verified Host Tools:**
   - Android CLI (`android.exe` at `C:\ProgramData\AndroidCLI\android.exe`), `adb.exe`, `emulator.exe` (`Medium_Phone_API_37.0`), `apktool.cmd` (`C:\Users\imsov\scoop\shims\apktool.cmd`), reference repositories `Both/occult` [MIT] and `Both/openscale` [GPL-3.0-or-later] (keep `Both/` gitignored).
   - Connected MCP servers: `rea` (`list_adb_devices`, `list_adb_packages`, `pull_adb_package`, `pull_adb_file`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `open_binary`, `search_strings`, `analyze_function`, `inspect_packet_capture`, `evaluate_reconstruction_coverage`) and `second-brain`.
3. **Zero Fabrication and Honest Boundaries (`AGENTS.md §0` and `INV-23`):**
   - Never invent Bluetooth packet decodes, cipher keys, blood pressure cuff values, test outputs, exit codes, or OS API capabilities.
   - Be transparent about physical hardware facts: smart floor scales without an arm cuff cannot measure arterial blood pressure in mmHg; they measure **Heart Rate (Pulse in bpm)** during Phase 2 right after weight locks. Fix the premature Phase 2 cutoff documented in Section 1 so Heart Rate and full body composition log reliably, and check any pulled vendor APK / capture with `rea` if a device is attached.
   - Enforce Google Health Connect's verified `androidx.health.connect:connect-client:1.1.0` ceiling (41 concrete `Record` subclasses, 8 scale write schemas maximum, no custom Metadata JSON bag) while keeping auto-sync completely invisible to the user.
   - Record every bug fixed in `HALL_OF_SHAME.md` (`SHAME-029+`), `INVARIANTS.md` (`INV-37+`), `MICROENGINEERING.md`, and `MOTIONENGINEERING.md` in the same commit with automated regression tests.
   - Run `humanizer` before writing any prose (`README.md`, release notes, commit messages, UI text). Enforce `AGENTS.md §0.7`: zero em dashes, en dashes, curly quotes, banned stock words, paired contrast filler phrases, or shallow participial tails.

### 1. Verified Crashes, Premature Stop Bugs, and Auto-Connect Defects to Remediate

Every defect below exists on `v0.3.0` (`5b6e846`) and must be eradicated cleanly:

1. **Fatal Runtime Crashes:**
   - `CustomCharts.kt` (`L411, L459, L698, L719`): `StippledBarChart` and `MetricTrendLineChart` compute `(x).coerceIn(0f, size.width - pillW)` and `(x).coerceIn(0f, size.width - lblW)`. Whenever `size.width` is smaller than `pillW` or label width (zero-width first measure, `360dp` narrow cards, long labels, or `1.5x` font scale), `maximumValue < 0f` throws `IllegalArgumentException`. Clamp upper bounds with `maxOf(0f, ...)`.
   - `BodyMetricsEngine.kt` (`L197-L198`): Fallback Cole expressions invoke `.coerceIn(zLow, 1400.0)` and `.coerceIn(120.0, zHi)`. Because `validatePlausibility()` allows whole-body ranges where `zLow` can exceed `1400.0` or `zHi` can fall below `120.0`, when `invertColeTwoBand()` returns `null`, `coerceIn` crashes with `IllegalArgumentException`. Order bounds safely with `minOf`/`maxOf`.
   - `CultBleController.kt`: Mutates `discoveredMap`, `peripheralCache`, `activePeripheral`, `stateMachine`, and `BluetoothCentralManager` across Main thread, `Dispatchers.IO` (`L440`), and `OpenCultBleThread` (`L91`), and queues initialization writes (`L508-L563`) before `0x2902` CCCD notification confirmation finishes. Guard `WeighInScreen.kt:172` and `OnboardingScreen.kt:730,757` against null state assertions, and validate `app/proguard-rules.pro` against R8 Path A release builds.

2. **Root Cause of Premature Measurement Cutoff on Cult Smart Scale (`ScaleSessionStateMachine.kt:168-189` vs `Both/occult/daemon/internal/protocol/session.go`):**
   - Look directly at `ScaleSessionStateMachine.kt:169-184`: on `ScaleTier.CULT_BF01_4E` (standard **Cult Smart Scale**), the moment 3 identical weight frames arrive (`WEIGHT_LOCK_FRAMES = 3`), `readyByStreak` becomes `true` and immediately sets `phase = WeighInPhase.LOCKED_COMPLETE`.
   - On real Cult Smart Scale hardware (documented in `Both/occult/daemon/internal/protocol/session.go`, `client.js`, and `listener.go`), weigh-ins happen in **two sequential wire phases**:
     1. **Phase 1 (Weight Settling):** Status `0x01` settles until `3` matching weight frames lock the weight. Wait here ONLY for weight lock; **do NOT complete or end the session at weight lock!**
     2. **Phase 2 (Body Composition and Heart Rate Pulse Detection):** The scale emits Phase 2 frames (`status 0x00` or `0x36`) while calculating BIA impedance and waiting for **Heart Rate (`bpm`)** to converge before flashing the result and powering off.
   - Refactor `ScaleSessionStateMachine` and `CultBleController` to match this full lifecycle faithfully:
     - Phase 1 lock transitions into active Phase 2 (`MEASURING_BODY_AND_PULSE`, keeping the scale stream open!).
     - Live UI displays locked weight with calming pulse/composition progress indicators while listening for updated impedance AND stable Heart Rate (`HRLockFrames = 3` matching non-zero HR frames + settled BIA impedance when emitted by firmware).
     - Never hang forever and never lose a partial measurement: finalize with captured data ONLY when Phase 2 completes (HR + impedance settled), when post-lock stream goes silent for `IdleAfterLock` (`6 seconds` after the last frame post-lock), on `onDisconnected` (scale powered off after flashing HR), or after an overall safety ceiling (`45 seconds`). Keep improving the stored reading in place if additional HR/BIA frames arrive right before step-off!
   - Similarly on **Cult Smart Scale Pro** (`0xBE` / `0xDF` / Runstar two-part `0xFFB0`), hold Phase 2 cleanly for handlebar contact, dual-frequency channels, Heart Rate, and `4 x 0xDF` frames before finalizing, falling back cleanly to Foot Sensors Only only after true user timeout or step-off.

3. **Seven Auto-Connect Blockers Preventing Zero-Tap Home Weigh-Ins:**
   - Remove Home-only `DisposableEffect` teardown (`HomeScreen.kt:238-251`) and move passive hardware monitoring into an app-foreground orchestrator scoped to `ProcessLifecycleOwner` / `OpenCultRootUi` (`ON_START` arms listener on ALL tabs; `ON_STOP` pauses cleanly, `INV-20`).
   - Remove `viewModel.resetBle()` from `WeighInScreen.kt:291-294` so opening or auto-presenting the measurement screen attaches directly to the active incoming scale stream without dropping notifications.
   - Serialize 100% of scan/stopScan/connect/CCCD/GATT writes/state-machine calls onto `bleHandler`/`bleDispatcher` (`OpenCultBleThread`).
   - Use filtered `ScanFilter`s (`0xFFF0`, `0xFFB0`, `0x1530`, `0x181B`, `0x181D`, plus saved MACs), `blessed-kotlin` `autoConnect` and quota-safe scan duty cycling (staying safely under Android's 5-scans-per-30s OS limit), matching saved device MAC OR advertised Cult device names/UUIDs so Resolvable Private Address (RPA) rotation never breaks auto-connect.
   - Automatically open the Live Measurement overlay the instant any saved scale connects from Home or any tab, display real-time stages until complete, save atomically to Room + Health Connect + Supabase outbox **with zero extra button taps**, return smoothly to the refreshed Home dashboard showing the full updated readings, prompt ONLY when household weights overlap (`pending_review`), and **automatically re-arm passive listening** as soon as the scale powers off (`onDisconnected`).
   - Pre-select the last active member (`ActiveMemberSession`, validated against Room at launch) with a subtle one-tap header member pill.

### 2. Consumer-First UI Purity: Zero Technical Details, Minimal Controls, Guided Onboarding

Eliminate EVERY trace of developer jargon and unnecessary buttons from normal user flows:

1. **Global Consumer Terminology Rule (Zero Developer Text Anywhere):**
   - Device names anywhere in Onboarding, Home, Live Weigh-In, Measurement Detail, Reports, Profiles/Settings, and notifications MUST be strictly:
     - **Cult Smart Scale** (replacing `CS-BF01`, `CS-BF01 (4E)`, `4E / Feet`, `ble_4e_feet`, `0xFFF0`)
     - **Cult Smart Scale Pro** (replacing `Cult Pro (8E)`, `Cult Pro 8E`, `ble_8e_handles`)
     - When a **Cult Smart Scale Pro** user stands with feet only (no handle contact), show **"Cult Smart Scale Pro (Foot Sensors)"** and one helpful tip (`"Hold the handlebar next time for arms, legs, and cellular hydration"`), never `"8E Locked"` or `"UNAVAILABLE_ON_TIER"`.
   - Remove ALL occurrences of `BLE`, `hex`, `GATT`, `UUID`, `0xCF`, `0xBE`, `0xDF`, `0xFFF0`, `0xFFB0`, `MAC address`, `RSSI`, `dBm`, `XOR`, `4E`, `8E`, `PRD:M-01..M-56` codes, `DERIVED_4E_BIA` / `ANTHROPOMETRIC_ESTIMATE` enum strings, `clientRecordVersion`, `HC Connected (8)`, `"Sim Pro 8E"`, and `"Protocol & Sync Hub"` from consumer screens!
   - Relocate developer Raw Packet / QA Fixture diagnostics completely out of Home (`Tune` icon), Measurement Detail (`Packets` button), and Settings buttons. Keep them accessible strictly via a hidden developer gesture (tapping the OpenCult app version text 7 times inside Settings/About) ONLY for maintainer diagnostics.

2. **Onboarding Rebuild: Guided Permissions, Live Scan & Pair, and Instant Scale Adaptation:**
   - **Step 1 (Welcome):** Clean visual introduction in Daylight Clinical Pastel explaining automatic weigh-ins, full body composition metrics, sleep/workout recovery insights, and automatic Health Connect sync.
   - **Step 2 (Cloud Sync Account or Private Offline):** Keep existing email validation, Cloudflare Turnstile check, anti-temp-mail check, and verification flow under clean copy, plus `"Use Privately on This Phone"` option. Remove developer backend-config disclaimers for normal builds cleanly.
   - **Step 3 (Guided Permissions):**
     - Clear interactive cards with plain icons and zero jargon:
       1. **Bluetooth & Nearby Devices**: *"Connect your Cult scale automatically whenever you step on it."* Button: **Allow Bluetooth** (handling permanently denied App Settings fallback and Bluetooth-off enable prompt cleanly).
       2. **Google Health Connect**: *"Automatically sync your weigh-ins and read your sleep, workouts, and heart recovery."* Button: **Connect Health Connect** (handling `UPDATE_REQUIRED` Play Store fallback cleanly).
     - Remove the `"8 Health Connect Schemas vs Full Scale Dataset"` technical box from Onboarding.
   - **Step 4 (Your Profile + Live Scale Discovery & Pairing):**
     - Collect member profile (Name, Sex, Birth Date via custom calendar sheet, Height, Goal Weight, Activity Level).
     - Include a clean **Find My Scale** card right inside Step 4 that starts scanning immediately when Bluetooth is allowed: shows animated radar + *"Step briefly onto your scale so it lights up"*, lists nearby scales purely as **Cult Smart Scale** or **Cult Smart Scale Pro** with a clean signal bar icon (NO MAC address, NO dBm!), lets the user tap **Pair Scale** once, shows a crisp confirmation (`"Paired: Cult Smart Scale"` or `"Paired: Cult Smart Scale Pro"`), stores the paired `ScaleDeviceEntity`, **determines the hardware capability profile immediately**, and transitions cleanly into Home! (Provide a quiet `"I'll Pair Later"` text link below if scale is not nearby.)

3. **Zero Manual Sync Buttons & Minimal Controls Across Every Tab:**
   - **Home:** Remove the `"HC Connected / HC Needs Perms"` chip bar, remove the top-right `Tune/Inspector` icon, replace the large empty-state `"Start Weigh-In"` button with a gentle `"Ready · Step onto your Cult Smart Scale"` status banner (passive auto-connect is live!), and keep the top header ultra-clean: **Member Switcher Pill** + **Calendar button**. (Automatic member recognition handles multi-member households; keep optional `"Only me"` switch quietly inside Member Switcher sheet instead of cluttering Home icons!)
   - **Live Measurement Window (`WeighInScreen`):** When triggered automatically on step-on (or via the Cobalt `+` button), remove `"Scan Cult Scale"`, `"Perms"`, `"GATT 0xFFF0 / 0xFFB0"`, `"Cult Pro (8E) / CS-BF01 (4E)"` switcher pills, `"Lock Feet-Only Now"`, and manual `"Commit & Sync Reading"` buttons! Instead show clean, fluid progressive stages:
     - Stage 1: Finding / Connecting to **Cult Smart Scale** (or Pro)
     - Stage 2: Measuring Weight (`72.40 kg` animated rolling ticker)
     - Stage 3: Measuring Body Composition & Pulse (calm rhythmic wave ring + live bpm heart icon, staying active until the scale finishes!)
     - Stage 4: Complete (weight, body fat %, heart rate, and subtle checkmark `"Saved & Synced to Health Connect"`, auto-returning smoothly to the refreshed Home dashboard after ~1.5s or via single `Done` tap!)
   - **Measurement Detail:** Remove `"Packets"`, manual `"Sync Health Connect"`, and raw `Health Connect Sync Ledger` rows. Show clean header (`"Cult Smart Scale · Today, 7:42 AM · Auto-Synced"`), subtle overflow `...` action menu for `Switch Member` or `Delete Measurement`, and interactive full-form parameter groups with trend sparklines on tap.
   - **Profiles & Settings:** Keep cleanly organized cards for Household Members (add/edit up to 10 members), **Connected Scale** (showing **Cult Smart Scale** or **Cult Smart Scale Pro**, battery %, and 1-tap `Change / Pair Scale`), **Health Connect** simple auto-sync toggle/status (shows `Reconnect` only if system permission was revoked), **Appearance** (Daylight / Obsidian / System), **Sound Cues**, and **Export Data Backup**.

### 3. Deep Hardware Reverse Engineering and Automatic Scale Capability Switching

1. **Hardware Verification (`rea` MCP + `apktool.cmd` + `Both/`):**
   - Check `adb devices -l` and `rea` `list_adb_devices` / `list_adb_packages`. If a handset with an official app (Cult Connect, Cult.fit, Fitdays, OKOK, Lefu) or `btsnoop_hci.log` is connected, pull and inspect it with `rea` + `apktool.cmd` (`open_binary`, `search_strings`, `analyze_function`, `inspect_packet_capture`) to cross-verify initialization writes, phase bytes (`0x01`/`0x00`/`0x36`), heart-rate offset, and impedance scaling.
   - Cross-check all handlers under `Both/openscale` (`CultSmartScaleProHandler.kt`, `BodyPod2Handler.kt`, `OneByoneHandler.kt`, `OneByoneNewHandler.kt`, `RunstarR6Handler.kt`, `StandardImpedanceLib.kt`, `Wla25BodyComposition.kt`, `S400BodyComposition.kt`) and `Both/occult` (`research/cult_scale_protocol.md`, `daemon/internal/protocol/session.go`). Reconcile the `0xBE` scaling (`BodyPod2Handler.kt` 5 x `uint16le / 10.0` `[RA, LA, Trunk, RL, LL]`), support `0xFFF0` (`OneByone` & `Lefu`) and `0xFFB0` (`Runstar` two-part), keep raw packets stored silently in `raw_ble_packets` for auditability, and gate `CS-BF01` candidate impedance bytes against frame stability + checksum + physiological bounds (`INV-02`) so noisy startup bytes never corrupt body composition!

2. **Automatic Capability Manager (`ScaleCapabilityProfile`) Driven by Paired / Detected Scale:**
   - Observe paired `scale_devices` and recent hardware sessions to derive an app-wide reactive capability state:
     - **Cult Smart Scale Mode** (active whenever paired with **Cult Smart Scale** and no Pro hardware is paired):
       - **Turn OFF & Hide Pro-Only Capabilities Automatically:**
         1. Dynamically remove the **Body Map** tab (`OpenCultDestination.BODY_MAP`) from `DetachedObsidianDock`, leaving a clean 3-tab dock (**Home**, **Reports**, **Settings** plus `+`), centered with GPU sliding pill spacing adjusted cleanly (`slotWidth = maxWidth / visibleDestinations.size`).
         2. Filter Pro-only metrics (`Cole R0`, `Cole Rinf`, `ECW`, `ICW`, `ECW/TBW`, `Body Cell Mass`, all 5-segment Arm/Leg/Trunk muscle and fat values, `Appendicular Lean Mass`, `Bilateral Symmetry`) completely out of Home grids, Measurement Detail, and the Reports parameter selector!
         3. Hide `Pro 8E` / `4E / Feet` scale-type filter rows inside `ReportsScreen`.
       - **Expose 100% of Cult Smart Scale Parameters (~24 to 25 Authentic Metrics, Zero Empty Rows):**
         Weight, Body Mass Index, Weight Category, Healthy Ideal Weight, Weight Control Goal, Fat Control Goal, Muscle Control Goal, Body Type / Physique (`Thin`, `Lean Muscular`, `Standard Balanced`, `Hidden Obesity`, `Overweight`, `Solidly Built`, `Obese`), Body Fat Percentage, Body Fat Mass, Fat Mass Index, Subcutaneous Fat Percentage, Subcutaneous Fat Mass, Visceral Fat Level, Obesity Degree, Fat-Free Body Mass, Fat-Free Mass Index, Skeletal Muscle Mass, Skeletal Muscle Percentage, Skeletal Muscle Index, Total Muscle Mass, Total Muscle Percentage, Muscle-to-Fat Ratio, Total Body Water Percentage, Total Body Water Volume, Protein Mass, Protein Percentage, Bone Mass, Mineral Mass, Basal Metabolic Rate, Resting Daily Energy Burn, Active Daily Energy Burn, Metabolic Body Age, Heart Rate (Pulse, when captured), Cardiac Health Score, and Overall Body Score.
     - **Cult Smart Scale Pro Mode** (active whenever paired with **Cult Smart Scale Pro**):
       - Automatically enable the **Body Map** tab (`4` dock tabs: **Home**, **Body Map**, **Reports**, **Settings** plus `+`), dual-frequency Intracellular/Extracellular hydration (`ICW`, `ECW`, `Fluid Balance`), `Body Cell Mass`, all 5 anatomical limb/trunk Muscle and Fat breakdowns, `Limb Muscle Mass`, `Symmetry`, and all 60+ metrics.
     - If a user later pairs a different scale in Settings or steps onto a household's other Cult scale, auto-detect hardware tier on connection and dynamically adapt UI capabilities immediately.

### 4. Plain Full-Form Parameter Names & Upgraded Home/Reports Data Visualization

Fix the two biggest UX pain points directly: cryptic clinical abbreviations and only seeing 3 to 4 metrics on Home!

1. **Full-Form Plain English Naming Everywhere (Zero Cryptic Shorthand):**
   - Audit every parameter display label in `BodyMetricsEngine` and UI screens:
     - Write out full, clear human names: **Body Mass Index** (never bare `BMI`), **Basal Metabolic Rate** (never bare `BMR`), **Active Daily Calories** (never `TDEE Active`), **Total Body Water** (never `TBW`), **Extracellular Water** / **Intracellular Water** (never bare `ECW`/`ICW`), **Fat-Free Body Mass**, **Fat-Free Mass Index**, **Fat Mass Index**, **Skeletal Muscle Mass**, **Skeletal Muscle Index**, **Body Cell Mass**, **Appendicular Limb Muscle**, **Whole-Body Impedance**.
     - Strip all `M-01..M-56` prefixes from user-facing cards and lists, and replace internal provenance enums (`DERIVED_4E_BIA`) with concise consumer badges only where helpful (e.g., `Heart Sensor`, `Body Composition`, or subtle info icon sheet explaining how each value is computed).

2. **All Active Parameters Visualized Directly on Home (Zero Buried Menus) + Enhanced Canvas Charts:**
   - Keep `OpenCultTheme` Daylight Clinical Pastel and Obsidian Night palettes, `24dp`/`28dp` soft squircle bento cards, `OpenCultMotion`, and `tnum` tabular numerals (**no glassmorphism blur**).
   - Design Home from top to bottom as an effortless visual dashboard:
     1. **Weight & Trend Hero Card:** Current weight, weight delta chip, plain scale badge (`Cult Smart Scale` or `Cult Smart Scale Pro`), 9-column vertical capsule spark chart.
     2. **Daily Overview Trio (`Steps`, `Recovery Meter`, `Body Fat`):**
        - **Lavender Wave Card:** Body Fat Percentage + smooth wave sparkline + left/right pager cycling key highlights (`Body Fat %`, `Skeletal Muscle Mass`, `Total Body Water`, `Visceral Fat Level`).
        - **Sage Card (Automatic Recovery Meter):** `0 to 100` clinical readiness score (`Prime`, `Ready`, `Moderate`, `Recover`) derived automatically from Health Connect Sleep Stages (`Deep`, `REM`, `Light`, `Awake`), 24h/48h Exercise Strain, Resting Heart Rate, and HRV against personal rolling baseline (dynamically re-weighted if a user has no wearable HRV/sleep synced, never emitting fake zeros!). Tap opens an interactive Recovery Breakdown sheet (`Canvas` segmented readiness arc + Sleep Stage bar + recent workout load pills + automatic Health Connect source tag).
        - **Peach Activity Card:** Today's Steps, Active Calories, Total Sleep duration, and resting/measured Heart Rate (`bpm`).
     3. **Body Composition Stacked Bar Card (Custom Compose `Canvas`):** Visual horizontal rounded segmented bar showing exact percentage split of Total Weight into **Muscle**, **Body Fat**, **Water**, and **Bone & Minerals** with clean color key pills.
     4. **Weight & Body Goals Card:** Target Weight progress bar, **Weight Control**, **Fat Control**, **Muscle Control**, and **Physique / Body Type** classification pill rendered as clear visual cards (no math table hunting!).
     5. **Complete All-Parameter Interactive Visual Matrix (Right Below on Home!):**
        - Display **every single active parameter for the detected scale** (~25 cards for Cult Smart Scale, 56+ for Pro) grouped into clean, collapsible or scrollable visual category grids (2-column compact bento tiles on normal font scale, automatic 1-column full-width stack at `1.5x` font scale so zero words ever truncate!).
        - Every metric card shows: Full-Form Name, Big `tnum` Value + Unit, plain Status Pill (`Healthy`, `Optimal`, `Watch`, `High`), and a **Custom `Canvas` Spectrum Range Gauge Bar** plotting exactly where the user sits between `Low`, `Optimal Target Zone`, and `High`!
        - Tapping ANY metric card anywhere on Home opens an instant slide-up **Metric Insight & Trend Sheet** (`MetricTrendLineChart`, Min/Avg/Max, plain-English 1-sentence health meaning, and healthy reference range).
   - **Upgraded Reports Screen:** Keep 3-zone segmented `BodyFatArcGauge`, clean `7D / 30D / 90D / 1Y / All` period selector, full-form metric quick chips (`Weight`, `Body Fat Percentage`, `Skeletal Muscle Mass`, `Visceral Fat Level`, `Heart Rate`, plus `"All 25 Metrics"` or `"All 56+ Metrics"` sheet filtered strictly to the detected scale), **Stippled Dot-Matrix Bar Chart (`DRD:C-04`)**, and full interactive trend explorer!

### 5. Android Health Connect Bidirectional Automatic Architecture

1. Declare and use both outbound writes (`8` native scale records: `WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BoneMassRecord`, `BodyWaterMassRecord`, `BasalMetabolicRateRecord`, `HeartRateRecord`, `HeightRecord` with deterministic `clientRecordId` and orphan cleanup on delete/reassign, `INV-04`, `INV-05`) AND rich inbound reads (`READ_SLEEP`, `READ_EXERCISE`, `READ_HEART_RATE_VARIABILITY`, `READ_RESTING_HEART_RATE`, `READ_STEPS`, `READ_TOTAL_CALORIES_BURNED`, `READ_ACTIVE_CALORIES_BURNED`, `READ_WEIGHT` excluding own package).
2. Parse `SleepSessionRecord.stages`, `ExerciseSessionRecord`, `HeartRateVariabilityRmssdRecord`, and `RestingHeartRateRecord` inside `HealthConnectGateway` + `RecoveryScoreEngine` to populate the Recovery Meter cleanly.
3. Sync runs automatically on every completed weigh-in and foreground resume (`WorkManager` retry on temporary failure) with **zero manual sync buttons** in daily UI.

### 6. Fluid Apple-Caliber Motion, Haptics, and Audio Craft (`MOTIONENGINEERING.md`)

1. Tune `OpenCultMotion` spring specs for card expansion, dock active pill sliding across 3 or 4 adaptive tabs, rolling numeric `tnum` weight/recovery tickers, gauge arc sweeps, and measurement overlay stage transitions.
2. Add light crisp tactile haptics (`HapticFeedbackType`) on weight lock, measurement completion, range chart scrub, and tab changes alongside `OpenCultSoundEngine` using safe `maxOf(pcmBytes, AudioTrack.getMinBufferSize(...))` sizing.
3. Enforce GPU `graphicsLayer` transforms, lifecycle-gated radar animations, zero overlapping text or buttons, and full `ANIMATOR_DURATION_SCALE == 0f` instant parity (`INV-19`, `INV-20`).

### 7. Mandatory Verification and Delivery Gate (`ralph-loop`, Zero Skipped Steps)

Do NOT finish or emit `<!-- GOAL_COMPLETE -->` until EVERY verification item below runs and passes after your final edit:

1. **Automated Unit and UI Regression Suite (`.\gradlew.bat testDebugUnitTest --offline --console=plain`):**
   - `CustomCharts` narrow/zero-width/`1.5x` safety tests (0 `IllegalArgumentException`).
   - `BodyMetricsEngine` extreme Cole safety, ~25 Cult Smart Scale parameters + 56+ Pro parameters, Physique/Control metrics, and `CS-BF01` noise-frame rejection (`INV-02`).
   - `ScaleSessionStateMachine` & `CultBleController` Phase 1 into Phase 2 full-duration capture test proving `WEIGHT_LOCK_FRAMES = 3` NEVER stops before Heart Rate and BIA impedance settle (plus timeout/disconnect completion), single-thread serialization, `WeighIn` attach-without-reset, automatic auto-connect + re-arm on step-off, and RPA name/UUID matching.
   - `ScaleCapabilityProfile` tests verifying Pro-only Body Map tab, filters, and segmental/ECW/ICW parameters stay hidden when Cult Smart Scale is paired and turn on automatically when Cult Smart Scale Pro is paired.
   - Consumer-purity UI tests (`OnboardingScreenTest`, `HomeScreenTest`, `WeighInScreenTest`, `MeasurementDetailScreenTest`, `ReportsScreenTest`, `ProfilesScreenTest`) at `360dp` AND `1.5x` font scale asserting: ZERO technical jargon (`BLE`, `0x`, `CS-BF01`, `4E`, `8E`, `dBm`, `XOR`, `Inspector`, `Sync Health Connect`, `Sim `), ALL ~25 Cult Smart Scale parameters rendered with full-form names on Home, Body Map hidden for Cult Smart Scale and shown for Pro, and zero truncated ellipsis rows!
2. **Release Lint, R8 Path A, APK Build, Preview Renders, Emulator/Device Runtime Check, and Profiling:**
   - Run `.\gradlew.bat :app:lintRelease :app:analyzeReleaseR8Config :app:assembleDebug :app:assembleRelease :app:bundleRelease`.
   - Run `r8-analyzer` Path A (`convert_pb_to_json.py` + `analyze.py`) verifying `app/proguard-rules.pro` keeps every Hilt, Room, Health Connect, Supabase/Ktor, Tink, and Bluetooth class intact in release.
   - Render `@Preview` images (Onboarding Scale Pair, Home Cult Smart Scale Full Matrix + Recovery Meter, Live Weigh-In Phase 2 Pulse, Measurement Detail, Reports at `360dp` and `1.5x` font scale, Obsidian Dark Mode) via `android preview render` and inspect EVERY PNG with `read_file`.
   - Start `Medium_Phone_API_37.0` (`android emulator start`) AND inspect `adb devices -l` (check connected physical handset too if attached). Clean-install release/debug APK without `-g`, exercise full onboarding (`Bluetooth` + `Health Connect` + `Scale Scan`), Home all-metric grid, auto-connect/weigh-in flow, dynamic switching (`Cult Smart Scale` 3-tab dock vs `Cult Smart Scale Pro` 4-tab dock), capture screenshots via `android screen capture` + inspect with `read_file`, run `android profiler systemtrace`, and verify `adb logcat -d` has **0** crashes/exceptions!
   - Run `rea` MCP inspection on the final APK (`open_binary`, `inspect_artifact` / `apktool.cmd`, `evaluate_reconstruction_coverage`, `close_binary`).
3. **Ledgers, Humanizer, Signed Commit, CI/CodeQL, and Second Brain:**
   - Update `HALL_OF_SHAME.md`, `INVARIANTS.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and `README.md`; run automated banlist, dash/quote, contrast-phrase, secret, and `git check-ignore` checks (`exit 0`).
   - Commit cleanly per logical unit signed solely by `Souvik Dey <imsovikde@gmail.com>` with zero trailers, push to `origin main`, verify GitHub Actions (`CodeQL` 0 open alerts + `Android Verification`) and signature verification via `gh api`, update `second-brain` (`capsule:project:opencult` + linked canonical `handoff`), and output complete `Verified / Unverified / Unknown` boundaries before `<!-- GOAL_COMPLETE -->`.
```
