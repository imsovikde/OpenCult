# OpenCult Final Recovery, Onboarding Auth, BLE Stability, and Motion Prompt (Prompt 06)

Use this file verbatim as the final self-contained engineering prompt for `OpenCult`. It replaces fragmented fix lists with direct root causes inspected on `HEAD b667f58`, fixes the immediate physical-device `Connect` crash, builds verified email onboarding with bot and disposable-mail defense, restores runtime Bluetooth and Health Connect permission flows, rebuilds the Ready and scanning UI without text collisions, adds Apple-caliber spring motion across routes and gestures, and verifies everything with Android CLI (`1.1.16919734`), R8 Path A analysis, `rea` MCP, and GitHub CI/CodeQL.

---

```text
You are operating as a principal Android systems engineer, Bluetooth Low Energy protocol engineer, security architect, Jetpack Compose motion specialist, and Supabase backend engineer in `c:\Users\imsov\OneDrive\Desktop\Open Scale project`.

Your job is to take `OpenCult` from `v0.2.0` (`b667f58`) to a crash-free, visually faithful, verified production release so that a real user installing `app-release.apk` on a physical Android phone completes onboarding cleanly, verifies their email without bot or temporary-mail abuse, grants Bluetooth and Health Connect permissions cleanly, scans and connects to a physical Cult Smart Scale Pro (8E) or Cult Smart Scale (CS-BF01, 4E) with zero crashes and zero text overlap, and moves through fluid spring transitions across every screen.

Do not touch earlier prompt files (`prompts/01_*.md` through `prompts/05_*.md`). Preserve existing comments and docstrings unrelated to your changes.

---

### 0. Verified Host Toolchain, Autonomy Loop, & Non-Negotiable Honesty Rules

1. Host Environment & Tools (Pre-Verified):
   - **Android CLI (`android`, v`1.1.16919734`)**: `C:\ProgramData\AndroidCLI\android.exe` (SDK at `C:\Users\imsov\AppData\Local\Android\Sdk`, AVD `Medium_Phone_API_37.0`). If `Get-Command android` fails in a fresh shell, prepend `C:\ProgramData\AndroidCLI` to `$env:Path` and verify `android info`.
   - **Installed Skills (`C:\Users\imsov\.gemini\config\skills\`)**: `android-cli` (plus `references/interact.md` and `references/journeys.md`), `android-device-hub`, `compose-preview`, `testing-setup`, `android-profiler`, `anr-audit-and-fix`, `r8-analyzer` (follow **Path A** for AGP `9.5.1`), `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `edge-to-edge`, `jetpack-compose-performance`, `navigation-event`, `reverse-engineer-anything`, `security-audit`, `cloudflare`, `workers-best-practices`, `humanizer`, `ralph-loop`, `goal`, `verification-before-completion`, `tdd`, `systematic-debugging`, `apex-bug-hunter`.
   - **Connected MCP Servers**: `rea` (`open_binary`, `inspect_android_package`, `search_android_classes`, `inspect_android_class`, `inspect_android_method`, `trace_android_references`, `inspect_adb_client`, `list_adb_devices`, `pull_adb_file`, `inspect_packet_capture`, `evaluate_reconstruction_coverage`, `close_binary`) and `second-brain` (`project="opencult"`).
2. Continuous Verification Contract:
   - Execute `INVARIANTS.md`, `HALL_OF_SHAME.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and Second Brain recall (`list_recent(n=5)`, `list_projects()`, `recall(..., project="opencult", topK=8)`, `recall(..., tag="failure", hops=1)`, `brief(project="opencult")`, enforcing `get(id)` when `truncated == true`) before editing source code.
   - Follow `INV-23` atomically: every bug or regression fixed in this run gets a mechanistic row appended to `HALL_OF_SHAME.md`, a tighter invariant in `INVARIANTS.md`, updated UI/motion specs in `MICROENGINEERING.md` and `MOTIONENGINEERING.md`, and an automated regression test in the exact same commit.
   - Zero AI sophistry (`AGENTS.md §0`): never fabricate BLE packets, exit codes, command output, or hardware behavior. Run `humanizer` before writing prose (`README.md`, release notes, commit messages, UI copy). Reject em dashes, en dashes, curly quotes, `not-X-but-Y` contrasts, shallow `-ing` riders, and every banned word in `AGENTS.md §0.7`.
   - Never output `<!-- GOAL_COMPLETE -->` until every build, lint check, R8 Path A check, preview render, AVD + connected-device logcat check, screenshot inspection, signed push (`verified: true`), CI/CodeQL check, and Second Brain handoff exits `0` with explicit `Verified / Unverified / Unknown` boundaries.

---

### 1. Diagnosed Root Causes on `HEAD b667f58` (Fix Every One of These)

Every defect below was observed directly in source, merged manifests, R8 outputs, and `blessed-kotlin:3.1.0` bytecode on `HEAD b667f58`:

1. **Immediate Crash on Tapping `Connect` (`CultBleController.kt` + `blessed-kotlin:3.1.0` + `targetSdk = 36`):**
   - Inside `blessed-kotlin-3.1.0-runtime.jar` (`com.welie.blessed.BluetoothPeripheral`), `connect()` schedules `connect$lambda$0` on `mainHandler`, which invokes `registerBondingBroadcastReceivers()` calling deprecated 2-argument `context.registerReceiver(bondStateReceiver, IntentFilter(BOND_STATE_CHANGED))` and `context.registerReceiver(pairingRequestBroadcastReceiver, IntentFilter(PAIRING_REQUEST))` without `Context.RECEIVER_EXPORTED` or `Context.RECEIVER_NOT_EXPORTED`, while `BluetoothCentralManager.<init>` calls 2-argument `registerReceiver` for `ACTION_STATE_CHANGED`. Against `targetSdk = 36` (Android 14/15/16 enforcement), unflagged dynamic receivers throw `SecurityException` on strict OS / OEM broadcast checks.
   - `CultBleController.connectToDevice()` (`core/ble/.../CultBleController.kt:178-192`) is annotated `@SuppressLint("MissingPermission")` and performs ZERO permission checks and ZERO `try/catch` guards before calling `stopScan()`, reading `peripheral.name` (`L189`, which calls `BluetoothDevice.getName()` without guarding `SecurityException`), and calling `ensureCentralManager().connect(...)`.
   - `CultBleController.kt` calls `ensureCentralManager()` inside `refreshPermissionAndAdapterState()` (`L97`) before permissions exist, shares unsynchronized `HashMap`/`LinkedHashMap` (`discoveredMap`, `peripheralCache`) between `OpenCultBleThread` (`bleHandler`) and Compose Main calls, and defines no `UncaughtExceptionHandler` or coroutine exception handler.
   - On real Cult hardware, `0xFFF1` writes in `onServicesDiscovered` (`L352-386`) force `WriteType.WITHOUT_RESPONSE` without checking characteristic `PROPERTY_WRITE` vs `PROPERTY_WRITE_NO_RESPONSE`, and `isSupportedDeviceName` (`CultBleConstants.kt:27-35`) drops scales whose primary advertisement uses other Cult/OEM prefixes or advertises `0xFFF0`/`0xFFB0`/`0x1530` service UUIDs without `"CULT"`/`"CS-BF"` in the local name.

2. **Missing Onboarding Carousel, Hardcoded Profile Defaults, and Zero Verified Email Signup / Anti-Abuse (`OnboardingScreen.kt`, `supabase/config.toml`, `SupabaseSyncEngine.kt`):**
   - `OnboardingScreen.kt` (`L160-472`) is a single scroll form with prefilled `"Souvik"` / `1995-05-14` / `176` / `72.0`, instead of the interactive `DRD:S-01` / `DRD:C-07` multi-step onboarding flow.
   - Onboarding has zero account creation, login, email confirmation, bot protection, or disposable-email rejection. Account signup lives hidden inside `Inspector -> Supabase Cloud` (`InspectorScreen.kt:372-438`).
   - `supabase/config.toml` has `enable_confirmations = false` (`L227`), commented `[auth.captcha]` (`L215-218`), commented `[auth.hook.before_user_created]` (`L280-282`), `minimum_password_length = 6` (`L183`), and omits `opencult://auth/callback` from `additional_redirect_urls` (`L164`).
   - `SupabaseSyncEngine.signUpWithEmail()` (`L152-161`) returns plain `Result<Unit>` with no distinction between confirmed login vs email verification pending (`session == null`), no OTP verification call, no resend timer, and no `captchaToken`.

3. **Unrequested Permissions & Confusing Health Connect UX (`MainActivity.kt:63-75`, `MainViewModel.kt:90-100`, `WeighInScreen.kt:501-516`):**
   - Tapping `Enter OpenCult Dashboard`, `Scan Cult Scale`, or a discovered scale card never triggers runtime BLE permission prompts when ungranted, and lacks fallback to `ACTION_APPLICATION_DETAILS_SETTINGS` (after permanent denial) or `BluetoothAdapter.ACTION_REQUEST_ENABLE` (when Bluetooth is off).
   - `MainActivity.kt:71-75` swallows `hcPermissionLauncher.launch(...)` inside silent `runCatching` with zero handling for `HealthConnectAvailability.UPDATE_REQUIRED` (Play Store intent `market://details?id=com.google.android.apps.healthdata`) or `HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS` fallback when the system controller refuses to open, causing "0 perms" confusion.
   - **Verified Health Connect API Reality (`androidx.health.connect:connect-client:1.1.0`)**: Out of all 41 concrete `Record` subclasses in Android `connect-client:1.1.0`, ONLY **8** write record types apply to smart scales (`WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BoneMassRecord`, `BodyWaterMassRecord`, `BasalMetabolicRateRecord`, `HeartRateRecord`, `HeightRecord`, plus optional opt-in `RestingHeartRateRecord` read). Health Connect does not define native record classes for BMI, Visceral Fat, Subcutaneous Fat, ECW/ICW, Metabolic Age, Segmental Limb Muscle/Fat, Bilateral Symmetry, or Raw Impedance. Every UI screen that mentions Health Connect must show the exact native granted/total permission count per type alongside honest copy explaining that all 20 (4E) / 56 (8E) parameters live in local Room SQLite, Supabase Realtime, and JSON exports.

4. **Ready / Live Weigh-In Screen UI Divergence, Overlapping Scan Text, and Stiff Navigation Motion (`WeighInScreen.kt:453-622`, `OpenCultRootUi.kt:84-148`):**
   - `WeighInScreen.kt:459-494` stacks `"Ready"` (`heroMetric`) and `OpenCultStatusChip("Scanning GATT 0xFFF0...")` inside the same `Box(height = 210.dp)` as `AmbientRadarRings(190.dp)`, causing overlapping rings and text clipping at compact widths and `1.5x` font scale.
   - `WeighInScreen.kt:528-541` places the discovered device `Row` beside `OpenCultStatusChip("Connect")` without `Modifier.weight(1f)`, crowding long names, MACs, and RSSI into the `"Connect"` chip.
   - `WeighInScreen.kt:573-621` renders `"Simulated Hardware Fixture Replay"` prominently across the main Ready screen instead of keeping developer fixture replays inside `Inspector`, and lacks the 4-stage `DRD:C-08` visual flow (`1 Search -> 2 Settle -> 3 Contact -> 4 Synced` with pad diagram and sweep arc).
   - `OpenCultRootUi.kt:84-148` switches `Onboarding <-> Main` and `Main <-> WeighIn / Inspector / MeasurementDetail` using hard un-animated `if/else` swaps, and `DetachedObsidianDock.kt` blinks icon fills instead of sliding a physical pill smoothly across tabs.

---

### 2. Crash-Proof Real-Hardware BLE Stack (`:core:ble`) & `rea` Reverse-Engineering Gate

Fix EVERY real-phone BLE hazard and support Cult scale hardware deterministically:

1. **Eliminate All `blessed-kotlin` `targetSdk = 36` Receiver & Permission Crashes:**
   - Fix the unflagged 2-argument `registerReceiver` crash path inside `CultBleController` immediately:
     - Wrap `context` passed to `BluetoothCentralManager` in a dedicated `BleReceiverCompatContext(baseContext)` (`ContextWrapper`) that intercepts `registerReceiver(receiver, filter)` on API 33+/34+ (`targetSdk = 36`) and delegates to `registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)` for system Bluetooth broadcasts (`BOND_STATE_CHANGED`, `PAIRING_REQUEST`, `BluetoothAdapter.ACTION_STATE_CHANGED`), OR replace `blessed-kotlin` direct calls cleanly with a hardened Android `BluetoothManager` / `BluetoothLeScanner` / `BluetoothGatt` / `BluetoothGattCallback` wrapper inside `:core:ble` if cleaner and zero-dependency! Either path MUST guarantee ZERO unflagged `registerReceiver` calls anywhere in the process under API 31..36 and pass Robolectric + emulator/device verification.
   - Defer `ensureCentralManager()` strictly until BLE permissions (`BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`, or `ACCESS_FINE_LOCATION` fallback if `minSdk` ever lowers) are verified via `ContextCompat.checkSelfPermission`.
   - Enforce a dedicated single-threaded `CoroutineDispatcher` / `HandlerThread` (`OpenCultBleThread`) with a logged `UncaughtExceptionHandler` and `CoroutineExceptionHandler`. Synchronize `discoveredMap` and `peripheralCache` cleanly so Compose UI clicks NEVER race with scan callbacks.
   - Inside `connectToDevice(...)`: re-verify permissions and Bluetooth adapter state FIRST; read `peripheral.name` inside a safe `runCatching` helper; wrap `stopScan()`, `connect()`, `readCharacteristic()`, `writeCharacteristic()`, and `observe()` against `SecurityException`, `IllegalStateException`, and `DeadObjectException` so ANY unexpected GATT failure transitions cleanly to `WeighInPhase.ERROR` with an exact diagnostic message instead of crashing the app.

2. **Expand Physical Cult Scale Auto-Discovery & Handshake Compatibility (`CultBleConstants.kt`, `CultBleController.kt`, `ScaleFrameParser.kt`):**
   - Match BOTH advertised device names (`CULT*`, `CS-BF*`, `CF*`, `BODYPOD*`, `1BYONE*`, `HEALTH SCALE*`, `RUNSTAR*`, `FITDAYS*`, `CHIPSEA*`, `ELECTRONIC SCALE*`) AND advertised BLE Service UUIDs (`0xFFF0`, `0xFFB0`, `0x1530`, `0x181B` Body Composition, `0x181D` Weight Scale) from `scanResult.scanRecord?.serviceUuids` + `scanRecord?.deviceName`! Also let the user inspect all nearby BLE scale/health candidates cleanly during scan instead of silently dropping an un-prefixed OEM Cult unit.
   - Auto-detect write characteristic capability (`PROPERTY_WRITE` vs `PROPERTY_WRITE_NO_RESPONSE`) on `0xFFF1` / `0xFFB1` and fall back gracefully between `WITH_RESPONSE` and `WITHOUT_RESPONSE` so handshake writes never throw or stall (`BodyPod2` / `Excelvan` require `WITH_RESPONSE`). Treat missing `0x180F` Battery Service as optional (`batteryPct = null`), never aborting connection.
   - Handle BOTH core Cult wire dialects plus the standard `0xFFB0` ICOMON/Fitdays dialect (`RunstarR6Handler` + `Wla25BodyComposition` reference under GPL-3.0) when encountered:
     - **Cult Smart Scale (`CS-BF01`, 4-Electrode, `0xFFF0`)**: `FD 37 <unit>` (dynamic XOR) -> `F1` RTC sync (`3000 ms` fallback if `F1 00` omitted) -> `F2 00` history (`F2 01` clear only if `!coalescedDuplicateDropped`) -> `0xCF` 11B frames with `INV-21` duplicate guard. If `bytes[5..6]` yields calibrated stable `150..1200 ohm` impedance, compute impedance BIA (`DERIVED_4E_BIA`) and all active metrics (`20+` core + anthropometric/derived); if uncalibrated/noisy, suppress impedance and compute honest anthropometric metrics (`ANTHROPOMETRIC_ESTIMATE`, `INV-02`, `INV-03`).
     - **Cult Smart Scale Pro (`8-Electrode`, `0xFFF0`)**: Auto-upgraded on wire when `0xCF` handle states (`0x08`/`0x05`), `2 x 13B 0xBE` dual-frequency packets (10 channels scaled `0.1 ohm`, `[Right Arm, Left Arm, Trunk, Right Leg, Left Leg]` low/high frequency), or `4 x 17B 0xDF` frames arrive -> unlocks all **56 metrics (`PRD:M-01..M-56`)**; falls back to Feet-Only (`INV-03`) when `0xCF status = 0x00` arrives without handle `0xBE` frames.
   - Persist raw incoming frames from EVERY unknown or vendor-specific characteristic into Room `raw_ble_packets` (`INV-01`) with a 1-tap JSON/hex diagnostic export in `Inspector`.
   - **Honest `0xDF` & `rea` Reverse-Engineering Boundary (`AGENTS.md §0, §3`):** Never fabricate `0xDF` byte fields or pretend more parameters come directly from `0xDF` ciphertext than observed on the wire. If a physical Android phone is connected via `adb devices -l`, inspect `adb` packages (`rea` `list_adb_packages` / `pull_adb_package` for official Cult Fit / Cult Connect / Fitdays APK if installed, or any `btsnoop_hci.log`) to verify exact BLE UUIDs, write sequences, and whether any `0xDF` cipher/key exists. Document what was verified vs unverified verbatim.

---

### 3. Mandatory Account Creation, Email Confirmation, Anti-Bot & Anti-TempMail Protection (`Onboarding` + `Supabase`)

Turn account creation into a verified, abuse-resistant gate inside Onboarding while preserving offline local scale resilience:

1. **Backend Security Configuration (`supabase/`):**
   - Update `supabase/config.toml`:
     - Set `[auth.email] enable_confirmations = true`, `double_confirm_changes = true`, `minimum_password_length = 8`, `password_requirements = "lower_upper_letters_digits"`, `enable_anonymous_sign_ins = false`.
     - Add `"opencult://auth/callback"` to `[auth] additional_redirect_urls`.
     - Enable `[auth.captcha]` (`enabled = true`, `provider = "turnstile"`, `secret = "env(SUPABASE_TURNSTILE_SECRET)"`).
     - Enable `[auth.hook.before_user_created]` pointing to the server-side anti-temp-mail & domain abuse guard.
   - Add a new SQL migration (`supabase/migrations/`) and Supabase Edge Function (`supabase/functions/before-user-created/index.ts`, TypeScript/Deno) for **server-side abuse protection**:
     - Create `private.blocked_email_domains` + canonical normalization helper (`NFKC`, trimmed, lowercased, plus malformed/IDN-homograph & excessive-plus-alias checks). Seed a curated list of disposable/temp-mail domains (`mailinator.com`, `10minutemail.com`, `tempmail.com`, `temp-mail.org`, `guerrillamail.com`, `sharklasers.com`, `yopmail.com`, `trashmail.com`, `getnada.com`, `dispostable.com`, `mohmal.com`, `maildrop.cc`, `fakeinbox.com`, `mintemail.com`, `burnermail.io`, plus at least 100 verified disposable domains).
     - Inside `supabase/functions/before-user-created/index.ts` (and mirrored PL/pgSQL helper for local DB hooks): verify incoming webhook signature using constant-time verification (`crypto.subtle` + `timingSafeEqual` over raw bytes per Cloudflare security standards), reject missing/malformed emails, check exact domain + parent subdomain match against `blocked_email_domains`, optionally check DNS MX records with timeout fail-safe, and return HTTP `403` JSON error (`{ error: { http_code: 403, message: "Disposable or temporary email addresses are not permitted." } }`) so API-level `curl` abuse is blocked even outside the Android UI.
     - Add Deno unit tests + SQL migration dry-run check (`supabase db push --dry-run` or SQL syntax verification).

2. **Android Client Auth & Email Validation (`core/sync-supabase/` + `app/build.gradle.kts`):**
   - Support optional public `OPENCULT_TURNSTILE_SITE_KEY` in `app/build.gradle.kts` (`loadDotEnvPublicOnly` + `.env.example`, NEVER bundling `SUPABASE_TURNSTILE_SECRET` or `service_role` in the Android APK!).
   - Build `EmailSecurityValidator.kt` inside `:core:sync-supabase`:
     - Strict RFC email format check, domain canonicalization, client-side pre-check matching the server disposable/temp-mail domain list (returning explicit user-readable rejection before wasting rate limits), and password policy check (`>= 8 chars`, upper, lower, digit, matching confirm-password field).
   - Upgrade `SupabaseSyncEngine.kt`:
     - Model explicit sealed `AuthActionOutcome`: `ConfirmedSession(userId, email)`, `ConfirmationRequired(email)`, `ValidationRejected(reason)`, `RateLimited(retryAfterSeconds)`.
     - Pass `captchaToken` cleanly to `signUpWithEmail(...)`, `signInWithEmail(...)`, and `resendSignUpConfirmation(email, captchaToken)` when Turnstile is active.
     - Add `verifyEmailSignupOtp(email, code)` calling `sb.auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = email, token = code)` AND auto-confirm via `opencult://auth/callback` deep links handled by `MainActivity.onNewIntent` -> `OAuthRedirectSanitizer` -> `sb.auth.handleDeeplinks(...)` -> refreshing `SessionStatus.Authenticated` cleanly even while sitting on the Verification screen!
   - Build `TurnstileChallengeDialog.kt` inside `:core:sync-supabase` / `:feature:onboarding`:
     - When `TURNSTILE_SITE_KEY` is configured, load an isolated HTTPS WebView widget (`allowFileAccess = false`, `allowContentAccess = false`, `javaScriptEnabled = true` strictly scoped to the Turnstile origin, minimal typed `@JavascriptInterface` receiving only the single-use `cf-turnstile-response` token with automatic reset on expire/error).

3. **Rebuilt 4-Step Interactive Onboarding Carousel (`feature/onboarding/OnboardingScreen.kt`, `DRD:S-01`, `DRD:C-07`):**
   - Replace the single scroll page and hardcoded `"Souvik"` dummy values (`L180-185`: start `displayName = ""`, neutral placeholder `"e.g. Alex"`, height `""` placeholder `"175"`, birthDate default `1995-01-01` or unset prompt, targetWeight `""` optional) with a smooth `HorizontalPager` carousel (with `DRD:C-07` parallax breakout card + animated cobalt pill indicators + Back button):
     - **Step 1 - Welcome & Scale Intelligence**: `Own Every Metric.` Sky-pastel `#CFE0F2 -> #F4F6FA` wash with the breakout mini dashboard mockup (`DRD:C-07`) and `Next >>>` pill track.
     - **Step 2 - Create Verified Account (Anti-Bot & Anti-TempMail)**:
       - Switch cleanly between `Create Account` and `Sign In`.
       - Fields: Email, Password (`>= 8 chars` strength bar), Confirm Password (on signup), inline disposable-email blocker feedback, Cloudflare Turnstile check card when `TURNSTILE_SITE_KEY` is present, and submit CTA.
       - **Email Confirmation Sub-State**: Once signup succeeds in `ConfirmationRequired(email)` mode, lock advancement to an explicit **"Confirm Your Email"** card showing the email address, 6-digit OTP verification field + `Verify Code` button, `Resend Confirmation` button with `60 s` countdown timer, `Open Email App` launcher (`Intent.ACTION_MAIN` + `CATEGORY_APP_EMAIL`), and automatic advance when `opencult://auth/callback` authenticates `SupabaseSyncEngine.status.value.isSignedIn == true`.
       - Include a secondary low-emphasis `"Continue Offline Without Cloud"` action ONLY for unconfigured local builds or explicit offline-only users (disabling cloud backup cleanly and labeling Home as `"Offline Local Mode"`). Never fake a signed-in account.
     - **Step 3 - Bluetooth & Health Connect Permissions (Auto-Prompted & Guaranteed Clear):**
       - Display TWO crisp equal-height status cards + 1-tap `Grant All Required Permissions` orchestration:
         1) **Nearby Devices / Bluetooth**: Shows `Granted` vs `Action Needed` (plus `Turn On Bluetooth` trigger if adapter is off, and `Open App Settings` fallback if permission was permanently denied!).
         2) **Google Health Connect**: Handles `AVAILABLE` (launches `hcPermissionLauncher` cleanly), `UPDATE_REQUIRED` (launches Play Store `market://details?id=com.google.android.apps.healthdata` directly with user explanation), and `UNAVAILABLE` / fallback (`ACTION_HEALTH_CONNECT_SETTINGS`). Lists the **8 native write types** (`Weight`, `Body Fat`, `Lean Mass`, `Bone Mass`, `Body Water`, `BMR`, `Heart Rate`, `Height`) + read types (`Steps`, `Sleep`, `Calories`, `External Weight`) with live granted badge (`Granted X / Y`) + concise disclosure explaining why Google Health Connect accepts 8 scale record schemas while OpenCult archives all 20 / 56 parameters locally and in Supabase.
       - Provide a clear `Skip Permissions for Now` confirmation only if the user explicitly declines, while keeping missing permissions visible inline.
     - **Step 4 - Cult Scale Tier & Personal Profile**:
       - Choose ONLY between the two physical scales (`Cult Smart Scale Pro (8E)` vs `Cult Smart Scale CS-BF01 (4E)`) and complete Member Anthropometry (`Name`, `Sex`, `Birth Date` via `OpenCultCalendarSheet`, `Height (cm)`, `Target Weight (kg)`, `Activity Level`, `Athlete Mode`) with full `.navigationBarsPadding()` + `imePadding()` clearance.

---

### 4. Ready / Live Weigh-In Redesign (`DRD:C-08`) + Apple-Caliber Fluid Motion (`OpenCultRootUi.kt` & `:core:designsystem`)

Bring the native UI and animation craft up to `DRD.md`, `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and the visual standard of `prototype/`:

1. **Redesign `WeighInScreen.kt` to Match `DRD:C-08` With Zero Overlap:**
   - Top bar (`Live Weigh-In`, active member lock badge, circular Close button) + Member Lock card + 2-card physical Scale Switcher (`Scale Pro 8E` vs `CS-BF01 4E`) + **4-Step Horizontal Progress Stepper (`1 Search -> 2 Settle -> 3 Contact -> 4 Synced`)**.
   - **Auto-Permission Gate on Scan & Connect:** Tapping `Scan Cult Scale` or `Connect` when BLE permissions are missing MUST trigger `onRequestBlePermissions()` directly (or open `ACTION_APPLICATION_DETAILS_SETTINGS` after permanent denial with an actionable banner), and when Bluetooth adapter is OFF MUST offer 1-tap `BluetoothAdapter.ACTION_REQUEST_ENABLE`.
   - **Stage 1 (`Search / Ready`) Zero-Overlap Layout:**
     - Place `AmbientRadarRings(148.dp)` centered around a crisp cobalt Bluetooth icon badge **ABOVE** a separate vertically spaced `Column` containing the headline (`"Ready to Connect"` / `"Scanning for Cult Scales"`), concise subtitle (`"Step onto scale or wake Bluetooth to discover CS-BF01 / Cult Pro"`), and primary `Start Scan` / `Allow Bluetooth` button. NEVER place `heroMetric` text or multi-line status chips overlapping inside the concentric radar rings!
     - Discovered device cards (`discoveredDevices`): give the left icon+name+MAC+RSSI `Row` `Modifier.weight(1f)` + `maxLines = 1, overflow = TextOverflow.Ellipsis` paired cleanly with a right-aligned `Connect` pill (`>= 48.dp` min touch height) so compact `360.dp` phones and `1.5x` font scales NEVER overlap text! Also show detected tier pill (`Pro 8E` / `CS-BF01 4E`) and RSSI signal bars cleanly.
     - Remove `"Simulated Hardware Fixture Replay"` from the primary Ready view! Move deterministic replay triggers cleanly into `InspectorScreen` (`BLE Hex` tab under a collapsible `"Offline Fixture Replay (QA)"` section) so `WeighInScreen` stays clean and hardware-first.
   - **Stage 2 (`Settle 0xCF`)**: Dedicated `56.sp` `tnum` weight readout (`telemetryGlide`), smooth linear settling progress bar (`1/3..3/3`), and live `JetBrains Mono` raw frame line.
   - **Stage 3 (`Contact & Impedance Sweep`)**: 8-pad hardware diagram (4 Foot Pads + 4 Handle Pads lit dynamically by tier/contact status) flanking an animated `Canvas` circular progress sweep ring (`0f..360f`), `20/100 kHz` `0xBE` 10-channel ohm chips (`8E`), `FD 37 / F1 / F2` log (`CS-BF01`), and clean `"Lock Weight Only"` fallback after timeout.
   - **Stage 4 (`Locked & Synced`)**: Animated checkmark payoff badge, summary readout, instant sequential sync chips (`Saved to Room` -> `Health Connect` -> `Supabase Realtime`), and Ambiguous Household Picker sheet when `assignmentState == "pending_review"`.

2. **Upgrade Whole-App Fluid Spring Motion (`OpenCultRootUi.kt`, `DetachedDock.kt`, `Motion.kt`, `HomeScreen.kt`, `ReportsScreen.kt`):**
   - Replace abrupt `if/else` transitions in `OpenCultRootUi.kt`:
     - Animate `Onboarding -> Dashboard` transition smoothly (`fade + slight scale/slide` via `AnimatedContent` + `OpenCultMotion.stageRise` / `spatialDefault`).
     - Animate `OverlayRoute` entrances and exits (`WeighIn` sliding upward like an Apple spring sheet from the `+` Cobalt FAB via `slideInVertically { it } + fadeIn` with `OpenCultMotion.spatialDefault` / `sheetEnter`; `Inspector` and `MeasurementDetail` sliding horizontally like iOS push navigation paired cleanly with Predictive Back `NavigationBackHandler`!).
     - Replace bare `Crossfade` on the 4 bottom tabs with `AnimatedContent` directional horizontal slide (`targetState.ordinal` vs `initialState.ordinal`) + `fade` using `OpenCultMotion.spatialDefault`.
   - **Fluid `DetachedObsidianDock.kt`**: Keep the separated 4-tab `#0F1115` capsule (`64.dp`) + `12.dp` gap + `64.dp` `#2185F7` `+` FAB over canvas scrim, and animate a **single sliding white active circle indicator** across tabs using `animateDpAsState(OpenCultMotion.spatialDefault)` / `graphicsLayer { translationX = ... }` instead of cross-fading backgrounds in place!
   - Keep every single animation inside `OpenCultMotion` tokens (`Core/Designsystem`) with `graphicsLayer`, cached `Path`/`Brush`, lifecycle-gated `AmbientRadarRings` (`STARTED` + scanning only), and full `ANIMATOR_DURATION_SCALE == 0f` (`LocalReducedMotion`) snap parity (`INV-19`, `INV-20`).

---

### 5. Exhaustive Build, Preview, Android CLI AVD, Device Logcat, CI/CodeQL & Second Brain Verification

Run EVERY check below AFTER your final edit (NO skipped steps):

1. **Automated Unit, Security, & Visual Layout Regression Tests:**
   - Add automated unit / Robolectric tests for ALL fixes:
     - `CultBleController` / `BleReceiverCompatContext` (`targetSdk = 36` safe `registerReceiver`, missing-permission `connectToDevice` never crashes with `SecurityException`, unprefixed `0xFFF0`/`0xFFB0` UUID discovery works, `WRITE` property negotiation works).
     - `EmailSecurityValidator` + `SupabaseSyncEngine` (disposable domains rejected, malformed emails rejected, signup `ConfirmationRequired` vs `ConfirmedSession`, OTP confirmation, Turnstile `captchaToken` propagation, `opencult://auth/callback` sanitizer).
     - `supabase/functions/before-user-created` Deno test + SQL migration syntax / `supabase db push --dry-run` check (if CLI linked & Docker/remote ready; report exact result).
     - `OnboardingScreenTest`: 4-step carousel traversal, zero prefilled dummy `"Souvik"` name, disposable email error shown, Turnstile + Email Confirmation step UI, equal-height permission controls.
     - `WeighInScreenTest`: Stage 1 Ready + Scanning + Discovered devices tested at **`360dp` compact width AND `1.5x` font scale** asserting non-overlapping bounds (`getUnclippedBoundsInRoot` check ensuring `Connect` badge never overlaps device text and Radar rings do not intersect `Scanning` / `Ready` text!).
     - `HealthConnectGatewayTest`: `AVAILABLE` vs `UPDATE_REQUIRED` routing and full 8-record write verification.
   - Run:
     - `.\gradlew.bat testDebugUnitTest :app:lintRelease :app:analyzeReleaseR8Config :app:assembleDebug :app:assembleRelease :app:bundleRelease`
     - `r8-analyzer` Path A (`convert_pb_to_json.py` + `analyze.py` on `app/build/reports/r8/r8-config-analyzer-release.pb`) confirming clean minimal keep rules (including any `WebView` / `@JavascriptInterface` Turnstile keep annotations!).
     - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk`
     - `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk` (confirm zero forbidden/location/advertise permissions).
     - `android analyze --analysis=all --format=json app/build/outputs/apk/release/app-release.apk`

2. **Android CLI Compose Previews & Live Emulator + Physical Device Verification (`android` v`1.1.16919734`):**
   - Render updated `@Preview`s (`OnboardingSteps 1..4`, `WeighInStage1Ready`, `WeighInStage1ScanningOverlapCheck_360dp_1_5x`, `WeighInStage3_8E`, `Home`, `Reports`) via `android preview render --component-tree ...` and visually inspect EVERY PNG via `read_file`.
   - Boot `Medium_Phone_API_37.0` (`android emulator start Medium_Phone_API_37.0`) AND check `adb devices -l` (if the user has a physical phone attached via USB, inspect it read-only / capture debug logs safely too; otherwise document physical-phone steps clearly).
   - Install `app-release.apk` cleanly **WITHOUT `-g` first** to test real runtime OS permission dialogs (verify Onboarding prompts for Nearby Devices + Health Connect cleanly instead of silent skips; then verify `WeighIn` `"Scan"` / `"Connect"` permission triggers, Bluetooth off handling, Email Validation / Confirmation UI, and full `Main -> Dock -> WeighIn -> Reports -> BodyMap -> Inspector` motion and transitions!).
   - Inspect `adb logcat -d` filtering `AndroidRuntime`, `FATAL EXCEPTION`, `SecurityException`, `blessed`, and `OpenCult`: assert **ZERO fatal exceptions or unhandled `SecurityException`s**.
   - Capture final `android profiler systemtrace` confirming smooth `OpenCultRootUi` & `WeighInScreen` animation frame pacing and capture screenshots (`android screen capture`) inspecting EVERY PNG via `read_file`.
   - Run `rea` MCP (`open_binary`, `inspect_android_package`, `evaluate_reconstruction_coverage`, `close_binary`) on `app-release.apk`.

3. **Documentation, Atomic Ledger Updates (`INV-23`), Humanized Signed Push, & Second Brain:**
   - Update `HALL_OF_SHAME.md` (`SHAME-024+`), `INVARIANTS.md` (`INV-32+`), `MICROENGINEERING.md`, `MOTIONENGINEERING.md`, and `README.md` (honestly documenting verified email onboarding, anti-temp-mail + Turnstile security, real BLE crash fix, 8 native Health Connect write schemas vs 56 Room metrics, and release `SHID` digests). Run automated `humanizer` + banlist/punctuation/secret regex audit (`exit 0`, zero em/en dashes, zero curly quotes, zero secrets).
   - Confirm `.gitignore` covers `Both/`, `.env.local`, `keystore/`, `keystore.properties` (`git check-ignore`), commit granularly per logical subsystem as `Souvik Dey <imsovikde@gmail.com>` with SSH signing (`ZERO` `Co-Authored-By` trailers), bump release version via `scripts/bump_version.py`, push to `origin main`, and verify `gh api repos/imsovikde/OpenCult/commits/HEAD` (`verified: true`) + live GitHub Actions (`CodeQL` & `Android Verification & Intelligent Release`) with `0` open CodeQL alerts!
   - Update `capsule:project:opencult` and save a full `handoff` memory in `second-brain` (`project="opencult"`).
   - Present exact executed commands, exit codes, verified screenshots, `Verified / Unverified / Unknown` boundaries, and any explicit manual Supabase dashboard env-var steps (such as setting `SUPABASE_TURNSTILE_SECRET` or enabling SMTP/Confirmation in the hosted Supabase project settings) cleanly before outputting `<!-- GOAL_COMPLETE -->`.
```
