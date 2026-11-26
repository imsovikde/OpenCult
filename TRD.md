# OpenCult Technical Requirements Document (`TRD.md`)

Engineering architecture, BLE GATT wire protocol, dual-frequency 8-electrode BIA equations, Room + Supabase Postgres DDL with RLS, Realtime sync protocol, and Google Health Connect client specification.

---

## 1. Technology Stack and Module Topology (`TRD:ARCH-01..ARCH-08`)

- `TRD:ARCH-01` **Language & Build:** Kotlin `2.1+`, Android Gradle Plugin `8.9+`, `minSdk = 31` (Android 12), `targetSdk = 36` (Android 16), Java 21 bytecode.
- `TRD:ARCH-02` **UI Layer:** Jetpack Compose + Compose Material 3 + custom `Canvas` / `DrawScope` charts (`DRD:C-01..C-10`), `androidx.navigation.compose`, Coil / Vector graphics, zero legacy XML layouts.
- `TRD:ARCH-03` **DI & Async:** Hilt (`dagger.hilt.android`), Kotlin Coroutines + `StateFlow` / `SharedFlow`, `WorkManager` (`hilt-work`) for resilient background sync retries.
- `TRD:ARCH-04` **BLE Stack:** `com.welie.blessed:blessed-kotlin` wrapped in a dedicated `:core:ble` state machine with `BLUETOOTH_SCAN` (`neverForLocation`) and `BLUETOOTH_CONNECT` runtime permission handling.
- `TRD:ARCH-05` **Local Persistence:** Room SQLite (`2.7+` KSP) as single source of truth for UI + DataStore Preferences for local settings.
- `TRD:ARCH-06` **Cloud & Realtime:** `io.github.jan-tennert.supabase` (`postgrest-kt`, `realtime-kt`, `auth-kt`, Ktor CIO engine) backed by dedicated `OpenCult` Supabase project in `ap-south-1`.
- `TRD:ARCH-07` **Health Connect:** `androidx.health.connect:connect-client` (`1.1.0+`).
- `TRD:ARCH-08` **Gradle Modules:**
  - `:app` (Application shell, Hilt wiring, MainActivity, NavHost, Health Connect rationale activity)
  - `:core:designsystem` (`OpenCultTheme`, color/type/shape tokens, `DRD:C-01..C-10` Canvas primitives)
  - `:core:ble` (`ScaleScanner`, `CultProGattHandler`, `CultBasicGattHandler`, frame parsers, checksum validators)
  - `:core:bia` (Pure-Kotlin JVM-testable BIA and anthropometric engine, zero Android dependencies)
  - `:core:database` (Room entities, DAOs, migrations, `sync_outbox`)
  - `:core:sync-supabase` (Supabase Auth, PostgREST outbox drainer, Postgres Changes Realtime consumer)
  - `:core:healthconnect` (`HealthConnectGateway`, deterministic `clientRecordId`, `ChangesToken` reader)
  - `:feature:onboarding`, `:feature:home`, `:feature:weighin`, `:feature:bodymap`, `:feature:reports`, `:feature:profiles`, `:feature:inspector`

---

## 2. Wire-Level BLE Protocol Specification (`TRD:BLE-01..BLE-16`)

### 2.1 GATT Profile (Shared Family `0xFFF0`)

| Service UUID | Char UUID | Properties | Role |
|---|---|---|---|
| `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff1-0000-1000-8000-00805f9b34fb` | `WRITE`, `WRITE_NO_RESPONSE` | App -> Scale commands (`FD 37`, `F1`, `F2`, `FE`) |
| `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff4-0000-1000-8000-00805f9b34fb` | `NOTIFY` (CCCD `0x2902`) | Scale -> App measurement frames (`0xCF`, `0xBE`, `0xDF`, 2-byte ACKs) |
| `0000180f-0000-1000-8000-00805f9b34fb` | `00002a19-0000-1000-8000-00805f9b34fb` | `READ`, `NOTIFY` | Standard battery level (%) |

### 2.2 `0xCF` Live and Locked Weight Frame (11 bytes; 18+ bytes for OneByone History)

- `TRD:BLE-01` `data[0] == 0xCF`
- `TRD:BLE-02` `data[1]` = flags (`0x80` = handle engagement on Pro; on some 4E firmwares holds heart rate or OneByone impedance low byte)
- `TRD:BLE-03` `data[3..4]` = `uint16_le / 100.0f` = weight in `kg`
- `TRD:BLE-04` `data[9]` = phase/status byte:
  - `0x01`: live weight settling (stream to UI ticker, do not persist)
  - `0x00`: stable weight-only (no handle BIA on Pro, or settled frame on `CS-BF01`)
  - `0x08`: 8-electrode handle impedance measurement in progress (lock weight, stay connected)
  - `0x05`: 8-electrode handle measurement complete (wait for `0xBE` + `0xDF` completion)
  - `0x36`: OneByone classic locked frame
- `TRD:BLE-05` `data[10] == xor(data[0..9])` (strictly validated; frames failing checksum are logged in Inspector with `checksum_valid = false` and excluded from final metrics).
- `TRD:BLE-06` **Coalesced Duplicate vs 18-byte History Guard:** If `data.size >= 18` during history transfer (`F2 00`), treat as history timestamp `data[11..17]` ONLY if it is NOT a coalesced duplicate (`isLiveFrame(data) && data[11] == 0xCF`).

### 2.3 `0xBE` Raw Dual-Frequency Segmental Impedance Frames (13 bytes x 2 packets)

- `TRD:BLE-07` `data[0] == 0xBE`, `data.size >= 13`, `data[12] == xor(data[0..11])`.
- `TRD:BLE-08` `data[1]` = packet index: `0x00` (Set A) and `0x01` (Set B).
- `TRD:BLE-09` `data[2..11]` = 5 x `uint16_le` impedance values per packet (10 channels total: `SetA[0..4]`, `SetB[0..4]`).
- `TRD:BLE-10` On Cult Smart Scale Pro (per `CultSmartScaleProHandler` in `Both/openscale`), `SetA[2]` (`bytes[6..7]` of packet `0x00`, ~`248..550 ohm`) represents the primary whole-body path, while the remaining 9 channels carry multi-frequency and segmental paths (`200..3500 ohm` range depending on scaling factor `raw` vs `raw / 10.0`).
- `TRD:BLE-11` Both raw `uint16_le` values and scaled ohm values are persisted in `raw_ble_packets.decoded_impedances_ohm` so channel mapping calibration updates can recompute historical weigh-ins deterministically (`INV-01`).

### 2.4 `0xDF` Proprietary Body-Comp Frames (17 bytes x 4 packets)

- `TRD:BLE-12` `data[0] == 0xDF`, `data.size >= 17`. `seqA = data[2]`, `seqB = data[3]`.
- `TRD:BLE-13` Receiving all 4 `0xDF` packets (after `0xCF` stable handle lock and both `0xBE` packets) marks the 8-electrode session complete and triggers metric derivation + Room commit + Health Connect write + Supabase outbox enqueue. All 4 `0xDF` payloads are stored verbatim in `raw_ble_packets` without attempting unverified decryption (`INV-01`, `INV-02`).

### 2.5 Outbound `0xFFF1` Initialization (4-Electrode `CS-BF01` Compatibility)

- `TRD:BLE-14` Mode/unit frame: `FD 37 <unit: 00=kg,01=lb,02=st> <group: 01> 00 00 00 00 00 00 <xor[0..9]>`.
- `TRD:BLE-15` Clock sync frame: `F1 <YYYY_be> <MM> <DD> <HH> <mm> <ss>`, arming a `3000 ms` fallback timer in case the model omits `F1 00` ACK (mirrors `OneByoneHandler`).
- `TRD:BLE-16` History request `F2 00` -> clear `F2 01` if any `>= 18`-byte history frames arrive.

---

## 3. Pure-Kotlin BIA and Anthropometric Engine (`:core:bia`, `TRD:BIA-01..BIA-14`)

All math lives in pure Kotlin with zero Android dependencies and deterministic unit tests:

1. `TRD:BIA-01` **Plausibility Gate (`INV-03`):**
   - Require `age in 10..120`, `heightCm in 80.0..250.0`, `weightKg in 10.0..350.0`.
   - For 8-electrode `0xBE` mode: verify whole-body 50 kHz impedance `Z_50` within `150.0..1200.0 ohm` and segmental channels non-zero (`> 10.0 ohm`). If any check fails, mark `contactValid = false`, suppress BIA fields, and emit weight-only + anthropometric notice.
2. `TRD:BIA-02` **Whole-Body FFM and TBW (Sun et al., 2003):**
   - `h2r = (heightCm * heightCm) / Z_50`
   - Male `FFM = -10.68 + 0.65 * h2r + 0.26 * weightKg + 0.02 * Z_50`
   - Female `FFM = -9.53 + 0.69 * h2r + 0.17 * weightKg + 0.02 * Z_50`
   - `bodyFatPct = ((1.0 - FFM / weightKg) * 100.0).coerceIn(3.0, 65.0)`
   - `TBW_kg = 0.732 * FFM` (Pace and Rathbun 1945 hydration constant, reconciled with Sun 2003 TBW).
3. `TRD:BIA-03` **Dual-Frequency ECW, ICW, and BCM (De Lorenzo 1997 / S400 Pipeline):**
   - When both low-frequency (`Z_low`) and high-frequency (`Z_high`, with `Z_low > Z_high`) channels are present and `|Z_low - Z_high| / Z_high >= 0.01`, invert the two-band Cole model via bisection to recover `R_0` and `R_inf`.
   - Compute `ECW` from `R_0`, `ICW = TBW_L - ECW_L`, `ECW/TBW`, and `BCM = ICW / 0.70` (Wang 2004).
4. `TRD:BIA-04` **Skeletal Muscle Mass (Janssen et al., 2000):**
   - `SMM_kg = 0.401 * h2r + 3.825 * isMaleInt - 0.071 * age + 5.102`
   - `SMI = SMM_kg / heightM^2`
5. `TRD:BIA-05` **8-Electrode 5-Segment Muscle and Fat Decomposition:**
   - Using limb-path impedances (`Z_RA`, `Z_LA`, `Z_TR`, `Z_RL`, `Z_LL`) normalized by segment length ratios (`0.37 * H` arm, `0.30 * H` trunk, `0.48 * H` leg), compute raw segmental lean indices `L_seg^2 / Z_seg`, then apply mass-conserving normalization so:
     - `sum(segmentMuscleKg) == totalMuscleKg` (within `0.1 kg` rounding)
     - `sum(segmentFatKg) == fatMassKg` (within `0.1 kg` rounding)
   - Derive Bilateral Muscle Symmetry Index (`|LA - RA| / avg(LA, RA)`, `|LL - RL| / avg(LL, RL)`) and Appendicular Lean Mass (`ALM = LA + RA + LL + RL`).
6. `TRD:BIA-06` **Bone, Protein, Metabolic Rate, and Subcutaneous/Visceral Estimates:**
   - `boneMassKg = (if (isMale) 0.057 else 0.050) * FFM`
   - `proteinKg = FFM - TBW_kg - boneMassKg`
   - `bmrKcal = 370.0 + 21.6 * FFM` (Katch-McArdle when BIA `FFM` is valid)
   - Fallback on 4-electrode `CS-BF01` (no `Z_50`): Deurenberg (1991) BF%, Watson (1980) TBW, Mifflin-St Jeor (1990) BMR, labeled `ANTHROPOMETRIC_ESTIMATE`.

---

## 4. Database Schema, Supabase RLS, and Zero-Refresh Realtime (`TRD:DB-01..DB-15`, `TRD:SB-01..SB-09`)

### 4.1 PostgreSQL Schema (`supabase/migrations/20261126000001_opencult_initial_schema.sql`)

Five normalized tables under `public`, every table enforcing `ENABLE ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY`:

1. `public.household_profiles`: max 10 active profiles per `account_id` enforced by `BEFORE INSERT OR UPDATE` trigger `enforce_max_10_profiles()`.
2. `public.scale_devices`: registered 4E/8E BLE scales per `account_id`.
3. `public.measurements`: core measurement envelope + `metrics_json` (all 56 parameters) + `provenance_json` + `mutation_id` + `deleted_at` tombstone.
4. `public.raw_ble_packets`: immutable raw BLE packet frames (`SELECT`, `INSERT`, `DELETE` policies only; `UPDATE` denied by omission).
5. `public.health_connect_sync_ledger`: per-record idempotency state (`client_record_id`, `client_record_version`, `hc_record_uid`, `sync_status`).

All RLS policies evaluate `(SELECT auth.uid()) = account_id` so Postgres caches the UID per statement via `initPlan`. Tables `household_profiles`, `scale_devices`, `measurements`, and `health_connect_sync_ledger` are added to `supabase_realtime` publication with `REPLICA IDENTITY FULL`.

### 4.2 Room + Outbox + Realtime Sync Protocol

- Local Room mirrors all 5 entities plus `sync_outbox(id, table_name, row_id, op, payload_json, attempts, next_retry_at, last_error)`.
- **Write path:** BLE lock -> Room transaction inserts `measurements`, `raw_ble_packets`, `health_connect_sync_ledger`, and `sync_outbox` (`< 50 ms`) -> emits UI update immediately -> launches Health Connect write + triggers `OutboxDrainer`.
- **Realtime path:** Authenticated session opens `supabase.channel("opencult-sync").postgresChangeFlow<PostgresAction>(schema = "public")` for `household_profiles`, `scale_devices`, `measurements`, and `health_connect_sync_ledger`. Incoming rows whose `mutation_id` already exists in local recent mutations are ignored; new/updated/tombstoned rows upsert into Room atomically, updating UI flows in `<= 2 s` without pull-to-refresh.

---

## 5. Google Health Connect Implementation (`TRD:HC-01..HC-10`)

- **Manifest Contract:** Declare `<uses-permission>` for `READ_WEIGHT`, `WRITE_WEIGHT`, `WRITE_BODY_FAT`, `WRITE_LEAN_BODY_MASS`, `WRITE_BONE_MASS`, `WRITE_BODY_WATER_MASS`, `WRITE_BASAL_METABOLIC_RATE`, `WRITE_HEART_RATE`, `WRITE_HEIGHT`, `READ_STEPS`, `READ_SLEEP`, `READ_TOTAL_CALORIES_BURNED`, `READ_ACTIVE_CALORIES_BURNED`. Include `<activity-alias>` for `android.intent.action.VIEW_PERMISSION_USAGE` (Android 14+) and `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13).
- **Idempotent Upsert (`INV-04`):** Build every Health Connect record with `Metadata.manualEntry(clientRecordId = "opencult:${measurement.id}:${recordTag}", clientRecordVersion = measurement.version)` or `Metadata.autoRecorded(...)` pointing to the paired `ScaleDevice`.
- **Loop Prevention (`INV-05`):** When reading `WeightRecord` via `ChangesToken`, filter out `record.metadata.dataOrigin.packageName == context.packageName`.

---

## 6. Verification Matrix (`TRD:TEST-01..TEST-10`)

| Test ID | Scope | Target |
|---|---|---|
| `TRD:TEST-01` | `0xCF` parser, XOR checksum, coalesced duplicate vs 18B history | `:core:ble` JVM tests |
| `TRD:TEST-02` | `0xBE` 2-packet 10-channel parser + `0xDF` 4-packet completion state machine | `:core:ble` JVM tests |
| `TRD:TEST-03` | Dual-frequency Cole inversion, Sun/Janssen/Wang equations, 5-segment mass sum conservation | `:core:bia` JVM tests |
| `TRD:TEST-04` | Anthropometric `CS-BF01` fallback + provenance enum (`INV-02`, `INV-03`) | `:core:bia` JVM tests |
| `TRD:TEST-05` | Room 10-profile limit, ambiguous weight match (`±2.5 kg`), tombstone queries | `:core:database` Robolectric tests |
| `TRD:TEST-06` | Health Connect deterministic `clientRecordId`, `DataOrigin` echo filter, delete sync | `:core:healthconnect` JVM tests |
| `TRD:TEST-07` | Supabase `sync_outbox` retry + Realtime `mutation_id` dedup | `:core:sync-supabase` JVM tests |
| `TRD:TEST-08` | Remote SQL migration + RLS policy check via `supabase db push` | `supabase/` |
| `TRD:TEST-09` | Compose Canvas bounds, `tnum`, Daylight/Obsidian contrast, floating dock clearance (`INV-10..13`) | `:feature:*` Compose UI tests |
| `TRD:TEST-10` | Secret & `.gitignore` check (`Both/` excluded) | CI / pre-commit script |
