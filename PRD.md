# OpenCult Product Requirements Document (`PRD.md`)

---

## 1. Problem Statement and Product Vision

Owners of Cult smart scales (both the 8-electrode **Cult Smart Scale Pro** marketed for 50+ parameters and the 4-electrode **Cult Smart Body Fat Scale** marketed for 20+ parameters) face four concrete product failures in the official Cult Fit / Cult Connect ecosystem:

1. **No Google Health Connect write support:** Weight and body composition metrics cannot sync automatically to Health Connect or share across Android fitness apps.
2. **Closed 50+ parameter output:** Raw multi-frequency and segmental impedances are locked inside the vendor app, while existing open-source apps like `openScale` read only 1 of the 10 raw `0xBE` impedance channels on the Pro scale and log just 7 basic metrics.
3. **Cloud lock-in without multi-device live ownership:** Users lack lifetime local storage, raw sensor packet preservation, and instant zero-refresh sync across devices they control.
4. **Inconsistent multi-scale handling:** Households using both the 8-electrode and 4-electrode Cult scales have no single transparent app that distinguishes direct sensor measurements from anthropometric estimates.

**OpenCult** is a GPL-3.0 native Android application (Kotlin 2.x + Jetpack Compose) that connects directly to both Cult scales over Bluetooth Low Energy, preserves every raw sensor frame forever, derives and explains up to 56 transparent body parameters, writes and reads Google Health Connect automatically, and syncs live in real time across devices via Supabase Postgres.

---

## 2. Supported Hardware Tier Matrix (`PRD:HW-01..HW-06`)

| ID | Hardware Mode | BLE Match & Protocol | Electrodes & Frequencies | Output Tier |
|---|---|---|---|---|
| `PRD:HW-01` | Cult Smart Scale Pro (with handle held) | Name prefix `CULT*`; service `0xFFF0`, notify `0xFFF4`; `0xCF` (`0x08`/`0x05`) + 2x `0xBE` (10 impedances) + 4x `0xDF` | 8 electrodes (4 foot + 4 hand), dual frequency | Full 56-parameter 8-electrode dual-frequency pipeline |
| `PRD:HW-02` | Cult Smart Scale Pro (feet only, handle unheld) | Name prefix `CULT*`; `0xCF` status `0x00` (no `0xBE` handles) | 4 foot pads only | Weight-only sensor read + explicit anthropometric fallback (labeled `ANTHROPOMETRIC_ESTIMATE`) |
| `PRD:HW-03` | Cult Smart Body Fat Scale (`CS-BF01`, 20-param model) | `0xFFF0` (`0xFFF1` write, `0xFFF4` notify); 11-byte `0xCF` weight frames + optional OneByone `FD 37`/`F1`/`F2` handshake | 4 foot pads (no usable unencrypted BLE impedance on `CS-BF01`) | Direct weight + HR (when present) + 20+ transparent anthropometric estimates (`INV-02`) |
| `PRD:HW-04` | Manual Entry | User input form (weight, optional waist/hip/neck/calipers/note) | None | Derived anthropometric + circumference ratios (`BMI`, `WHR`, `WHtR`, Navy/Caliper fat) |
| `PRD:HW-05` | Health Connect Inbound Weight | Imported `WeightRecord` from third-party apps (filtered by `DataOrigin != packageName`) | External | Weight + BMI + user-configured derived metrics |
| `PRD:HW-06` | Hardware Auto-Switch | Household uses both scales concurrently | Mixed `4E` / `8E` | Automatic handler routing by BLE advertisement and packet format (`INV-14`) |

---

## 3. Complete 56-Parameter Catalog (`PRD:M-01..M-56`)

Every parameter below has a fixed `identity`, canonical unit, and explicit provenance tag (`INV-02`).

| ID | Parameter Name | Unit | Category | 8E Pro | 4E CS-BF01 | Basis / Provenance |
|---|---|---|---|---|---|---|
| `PRD:M-01` | Weight | `kg` | Core | Yes | Yes | `DIRECT_SENSOR` (`0xCF` bytes `[3..4]`) |
| `PRD:M-02` | Body Mass Index (BMI) | `kg/m2` | Core | Yes | Yes | `weight / height_m^2` |
| `PRD:M-03` | WHO BMI Category | `text` | Core | Yes | Yes | WHO adult cutoffs |
| `PRD:M-04` | Ideal Body Weight (Devine) | `kg` | Core | Yes | Yes | Devine (1974) by height & sex |
| `PRD:M-05` | Weight Delta to Target | `kg` | Core | Yes | Yes | Profile target minus current weight |
| `PRD:M-06` | Heart Rate | `bpm` | Cardiovascular | Conditional | Conditional | `DIRECT_SENSOR` when emitted by unit |
| `PRD:M-07` | Cardiac Index Score | `score` | Cardiovascular | Conditional | Conditional | Resting HR band vs age reference |
| `PRD:M-08` | Whole-Body Impedance (50 kHz) | `ohm` | Raw BIA | Yes | No | `DIRECT_SENSOR` (`0xBE` Set A/B) |
| `PRD:M-09` | Whole-Body Impedance (High Freq) | `ohm` | Raw BIA | Yes | No | `DIRECT_SENSOR` (`0xBE` Set A/B) |
| `PRD:M-10` | Cole Extracellular Resistance `R0` | `ohm` | Raw BIA | Yes | No | `BIA_DUAL_FREQ_8E` two-band inversion |
| `PRD:M-11` | Cole Infinite Resistance `Rinf` | `ohm` | Raw BIA | Yes | No | `BIA_DUAL_FREQ_8E` two-band inversion |
| `PRD:M-12` | Body Fat Percentage | `%` | Adiposity | Yes | Est. | `BIA_DUAL_FREQ_8E` (Pro) / Deurenberg 1991 (`CS-BF01`) |
| `PRD:M-13` | Fat Mass | `kg` | Adiposity | Yes | Est. | `weight * bodyFatPct / 100` |
| `PRD:M-14` | Fat Mass Index (FMI) | `kg/m2` | Adiposity | Yes | Est. | `fatMassKg / height_m^2` |
| `PRD:M-15` | Fat-Free Mass (FFM / LBM) | `kg` | Core Comp | Yes | Est. | Sun (2003) / `weight - fatMass` |
| `PRD:M-16` | Fat-Free Mass Index (FFMI) | `kg/m2` | Core Comp | Yes | Est. | `ffmKg / height_m^2` |
| `PRD:M-17` | Subcutaneous Fat Percentage | `%` | Adiposity | Yes | Est. | `BIA_DUAL_FREQ_8E` |
| `PRD:M-18` | Subcutaneous Fat Mass | `kg` | Adiposity | Yes | Est. | `weight * subqFatPct / 100` |
| `PRD:M-19` | Visceral Fat Level | `level (1..30)` | Adiposity | Yes | Est. | Multi-compartment / anthropometric regression |
| `PRD:M-20` | Obesity Degree | `%` | Adiposity | Yes | Yes | `(weight - idealWeight) / idealWeight * 100` |
| `PRD:M-21` | Total Body Water (TBW) | `%` | Hydration | Yes | Est. | `0.732 * FFM / weight` (Pro) / Watson 1980 (`CS-BF01`) |
| `PRD:M-22` | Total Body Water Mass | `kg` | Hydration | Yes | Est. | `TBW_L * 0.99513` |
| `PRD:M-23` | Total Body Water Volume | `L` | Hydration | Yes | Est. | `TBW` liters |
| `PRD:M-24` | Extracellular Water (ECW) | `L` | Hydration | Yes | No | `BIA_DUAL_FREQ_8E` De Lorenzo Eq. B2 on `R0` |
| `PRD:M-25` | Extracellular Water % | `%` | Hydration | Yes | No | `ECW / weight * 100` |
| `PRD:M-26` | Intracellular Water (ICW) | `L` | Hydration | Yes | No | `TBW - ECW` |
| `PRD:M-27` | Intracellular Water % | `%` | Hydration | Yes | No | `ICW / weight * 100` |
| `PRD:M-28` | Hydration Ratio (`ECW / TBW`) | `ratio` | Hydration | Yes | No | Edema / fluid balance indicator (`0.36..0.40`) |
| `PRD:M-29` | Skeletal Muscle Mass (SMM) | `kg` | Muscle | Yes | No | `BIA_DUAL_FREQ_8E` Janssen (2000) |
| `PRD:M-30` | Skeletal Muscle Percentage | `%` | Muscle | Yes | No | `SMM / weight * 100` |
| `PRD:M-31` | Skeletal Muscle Index (SMI) | `kg/m2` | Muscle | Yes | No | `SMM / height_m^2` |
| `PRD:M-32` | Total Muscle Mass | `kg` | Muscle | Yes | Est. | `FFM - boneMass` |
| `PRD:M-33` | Total Muscle Rate | `%` | Muscle | Yes | Est. | `totalMuscleKg / weight * 100` |
| `PRD:M-34` | Muscle-to-Fat Ratio | `ratio` | Muscle | Yes | Est. | `skeletalMuscleKg / fatMassKg` |
| `PRD:M-35` | Body Cell Mass (BCM) | `kg` | Cellular | Yes | No | `ICW / 0.70` (Wang 2004) |
| `PRD:M-36` | Bone Mineral Content (Bone Mass) | `kg` | Skeletal | Yes | Est. | `0.057 (M) / 0.050 (F) * FFM` |
| `PRD:M-37` | Bone Mass Percentage | `%` | Skeletal | Yes | Est. | `boneMassKg / weight * 100` |
| `PRD:M-38` | Protein Mass | `kg` | Cellular | Yes | Est. | `FFM - TBW_kg - boneMassKg` |
| `PRD:M-39` | Protein Percentage | `%` | Cellular | Yes | Est. | `proteinKg / weight * 100` |
| `PRD:M-40` | Inorganic Salt / Mineral Mass | `kg` | Cellular | Yes | Est. | Total mineral compartment estimate |
| `PRD:M-41` | Basal Metabolic Rate (BMR) | `kcal/day` | Metabolic | Yes | Yes | Katch-McArdle (Pro) / Mifflin-St Jeor (`CS-BF01`) |
| `PRD:M-42` | TDEE (Sedentary 1.2x) | `kcal/day` | Metabolic | Yes | Yes | `BMR * 1.2` |
| `PRD:M-43` | TDEE (Active / Profile Level) | `kcal/day` | Metabolic | Yes | Yes | `BMR * profileActivityMultiplier` |
| `PRD:M-44` | Metabolic Body Age | `years` | Metabolic | Yes | Yes | Body-fat band + BMR delta vs age norm |
| `PRD:M-45` | Composite Body Score | `0..100` | Metabolic | Yes | Est. | Transparent weighted score (BF%, SMM, Visceral, ECW/TBW) |
| `PRD:M-46` | Left Arm Muscle Mass | `kg` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-47` | Right Arm Muscle Mass | `kg` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-48` | Trunk Muscle Mass | `kg` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-49` | Left Leg Muscle Mass | `kg` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-50` | Right Leg Muscle Mass | `kg` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-51` | Left Arm Fat Mass & % | `kg / %` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-52` | Right Arm Fat Mass & % | `kg / %` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-53` | Trunk Fat Mass & % | `kg / %` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-54` | Left Leg Fat Mass & % | `kg / %` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-55` | Right Leg Fat Mass & % | `kg / %` | Segmental 8E | Yes | No | `BIA_DUAL_FREQ_8E` (`0xBE` segmental channel) |
| `PRD:M-56` | Bilateral Muscle Symmetry Index | `% diff` | Segmental 8E | Yes | No | Arm L/R ratio, Leg L/R ratio, Appendicular Lean Mass (ALM/h^2) |

---

## 4. Google Health Connect First-Class Sync (`PRD:HC-01..HC-12`)

1. `PRD:HC-01` One-tap permission flow inside Onboarding and Sync Center covering all write + read types.
2. `PRD:HC-02` Auto-write on every locked weigh-in for the primary active Health Connect user profile (configurable per household member).
3. `PRD:HC-03` Outbound Health Connect record types written with deterministic `clientRecordId` (`INV-04`):
   - `WeightRecord`
   - `BodyFatRecord`
   - `LeanBodyMassRecord`
   - `BoneMassRecord`
   - `BodyWaterMassRecord`
   - `BasalMetabolicRateRecord`
   - `HeartRateRecord` (when captured)
   - `HeightRecord` (when profile height changes)
4. `PRD:HC-04` Inbound Health Connect reads for Home Bento Page 2: `StepsRecord`, `SleepSessionRecord`, `TotalCaloriesBurnedRecord` (with `ActiveCaloriesBurnedRecord` fallback).
5. `PRD:HC-05` Optional inbound `WeightRecord` sync from external fitness apps, strictly excluding `DataOrigin == applicationId` to prevent echo loops.
6. `PRD:HC-06` Historical backfill action: syncs all historical measurements for the selected profile in batches with live progress and retry ledger.
7. `PRD:HC-07` Soft-delete propagation: deleting a weigh-in in OpenCult deletes the matching Health Connect records (`INV-05`, `INV-16`).

---

## 5. Supabase Realtime Zero-Refresh Sync and Household Profiles (`PRD:SB-01..SB-10`, `PRD:PR-01..PR-08`)

1. `PRD:SB-01` Offline-first guest start: app works 100% locally in Room without forcing sign-in.
2. `PRD:SB-02` Account linking: signing in with Supabase Auth (Google / Email OTP) uploads local Room rows via `sync_outbox` and opens live Postgres Changes streams.
3. `PRD:SB-03` Zero-refresh convergence: any insert, edit, profile change, or soft delete on Device A appears automatically on Device B within `2 s` (`INV-07`).
4. `PRD:PR-01` Supports up to **10 household profiles** per account (enforced in UI and Postgres trigger).
5. `PRD:PR-02` Auto-recognition matches incoming BLE weigh-ins within `auto_match_window_kg` (`2.5 kg` default) and flags multi-match or zero-match weigh-ins as `pending_review` (`INV-09`).

---

## 6. BLE Reverse-Engineering Workbench and Data Portability (`PRD:RE-01..RE-06`, `PRD:EX-01..EX-05`)

1. `PRD:RE-01` Built-in BLE Raw Packet Inspector shows live `0xCF`, `0xBE`, and `0xDF` hex frames, checksum verification, and 10-channel `0xBE` ohm grid (`DRD:C-10`).
2. `PRD:RE-02` Side-by-side calibration field lets users enter Cult Connect reference values next to captured `0xDF` packets to aid future community decoding.
3. `PRD:EX-01` Lossless JSON export/import (profiles, measurements, raw BLE hex frames, Health Connect ledger).
4. `PRD:EX-02` Wide CSV export matching standard body-composition column headers.

---

## 7. Non-Goals (`PRD:NG-01..PRD:NG-06`)

- `PRD:NG-01` Not a medical diagnosis tool; all estimates carry clinical trend disclaimers.
- `PRD:NG-02` Zero paywalls, zero subscriptions, zero locked parameters.
- `PRD:NG-03` Zero ad SDKs or third-party behavioral analytics.
- `PRD:NG-04` No mandatory cloud account required for local BLE weigh-ins or Health Connect export.
- `PRD:NG-05` No unverified claims about `0xDF` cipher keys unless proven by captured traffic and tests.
- `PRD:NG-06` No iOS build in v1 (Android-first for native BLE GATT and Google Health Connect).
