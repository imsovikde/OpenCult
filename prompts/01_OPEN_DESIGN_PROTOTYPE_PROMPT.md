# OpenCult UI Prototype Prompt for Open Design

## How to Use This Prompt in Open Design

1. Open **Open Design** (`# Prototype` mode, as shown in your workspace).
2. Click `+` in the prompt bar and attach your UI reference screenshot (`media_1795661960541_dfba6bfd.jpg`).
3. Copy and paste the **entire fenced prompt below** into Open Design and run it.
4. Once the prototype renders, inspect all 9 interactive screens in both Daylight Clinical Pastel (default) and Obsidian Night Mode before locking `DRD.md` for Android implementation.

---

```text
Build a high-precision, interactive mobile application UI prototype for **OpenCult**, an open-source smart scale companion supporting the Cult Smart Scale Pro (8-electrode, dual-frequency, 56 body parameters) and Cult Smart Body Fat Scale CS-BF01 (4-electrode, 20+ parameters), with two-way Google Health Connect sync and zero-refresh Supabase Realtime sync across up to 10 household profiles.

Match the exact visual grammar, surface hierarchy, pastel bento palette, soft squircle geometry, chart styles, typography, and detached floating dock of the attached reference screenshot, elevated with clean micro-interactions and full body composition clarity. Do not use generic Material bars, generic line charts, or heavy outlines.

### 1. Visual System and Exact Tokens
- **Viewport:** `393 x 852 px` mobile frame, edge-to-edge safe areas, interactive navigation across all screens.
- **Default Theme (Daylight Clinical Pastel) + Live Theme Toggle (Obsidian Night):**
  - Canvas background: `#F4F6FA` (Dark: `#0A0D12`)
  - Elevated card surface: `#FFFFFF` with soft ambient shadow `0 10px 28px rgba(15, 20, 28, 0.05)` (Dark: `#131720` with `1px solid #232A38`)
  - Subtle pill / segmented track: `#EEF1F6` (Dark: `#1C222E`)
  - Primary ink: `#0F141C` (Dark: `#F4F6FA`)
  - Secondary ink: `#5B6472` (Dark: `#A7B1C2`)
  - Muted ink: `#8C95A4` (Dark: `#6F7A8C`)
  - Hero Powder Sky card: `#CFE0F2` (Dark: `#152536`)
  - Bento Lavender card: `#DFD5F5` (Dark: `#221B36`), Bezier wave stroke `#7E5BEF` (Dark: `#A688FA`)
  - Bento Sage card: `#DCECCB` (Dark: `#192A1E`)
  - Bento Peach card: `#F5E2D6` (Dark: `#2E1F18`)
  - Primary Cobalt action / optimal zone: `#2185F7`
  - Normal / Optimal badge: `#D6F5E3` fill with `#117A4B` text (Dark: `#123626` / `#5CE09B`)
  - Elevated warning zone: `#F4B428`
  - High alert zone / spike spark-bar: `#EF4444`
  - Active dark control / active bar: `#111111` (Dark: `#F4F6FA`)
  - Detached bottom dock pill: `#0F1115` with active item in `#FFFFFF` circle (`46px`)
- **Typography:** `Plus Jakarta Sans` (weights 500, 600, 700) for all UI and display text with `font-variant-numeric: tabular-nums` on every metric value; `JetBrains Mono` (weights 400, 500, 700) for raw BLE hex frames, impedance ohms, and sync IDs.
- **Radii:** Outer hero and report cards `28px`, bento tiles `22px`, circular icon badges `36px` (`#FFFFFF`), pills `999px`.

### 2. Interactive Screens to Prototype (All Linked via Working Navigation)

#### Screen 1: Onboarding (3-Step Flow)
- Sky-blue top wash (`#CBE1F7` fading into `#F4F6FA`).
- Top bar: `40px` white circular Back button on left; `Skip` pill (`#BED6EE`) on right.
- Bold 2-line headline (`28px`, line-height `34px`): `"All Your Body Composition Insights in One Place"`, subtitle below (`"Connect 4-electrode and 8-electrode Cult scales, log 56 metrics locally, and auto-sync to Google Health Connect."`), followed by a 3-dot progress indicator (`24px` `#2185F7` active capsule + `6px` muted dots).
- Centerpiece: Dark-bezel phone frame displaying a miniature Home screen, with an elevated white Hero Weight & Body Fat card (`74.2 kg`, `18.4% Fat`, `Optimal` mint pill, 9 vertical capsule spark-bars) breaking slightly outside the phone bezel (`1.06x` width, elevated shadow).
- Bottom white `60px` pill track housing a `#2185F7` `"Next >>>"` button that advances through Steps 1-3 (Step 2: 8-Electrode Handle + Health Connect Permissions; Step 3: 10 Household Profiles + Supabase Live Sync) and lands on Home.

#### Screen 2: Home Dashboard (Exact Bento Composition)
- **Top Header:** Left circular user avatar (`44px`) that opens the 10-Member Household Switcher; greeting `"Good Morning"` (`14px` `#5B6472`) above bold name `"Wyatt Anderson"` (`26px` SemiBold); right unified white pill containing notification/sync status bell + drawer/inspector menu button + daylight/night toggle.
- **Hero Composition Card (`#CFE0F2`, `28px` radius):**
  - Top-left pill switcher (`Weight` / `Body Fat %` / `SMM`); top-right mint `"Normal"` / `"Optimal"` badge + small `"8E Pro"` hardware tag.
  - Bottom-left: Giant `44px` tabular readout (`74.2 kg`) + compact delta (`-0.4 kg vs Tuesday`).
  - Bottom-right: **9-Column Vertical Rounded Capsule Spark-Bars** (`4px` width, `6px` gap, rounded ends) showing last 9 weigh-ins in `#263238`, with outlier spikes highlighted in `#EF4444`.
- **Overview Section Header:** `"Overview"` (`18px` SemiBold) on left; two `32px` circular pager buttons (`<` white, `>` solid `#111111`) on right that flip smoothly between Page 1 and Page 2.
- **Asymmetric 2-Column Bento Grid (`12px` gutter):**
  - **Page 1 (Body Composition):**
    - Left tall card (`#DFD5F5`, `212px` height): `"Skeletal Muscle"` + `36px` white muscle icon circle, `"42.8%"` (`26px` Bold) + `"31.8 kg · Janssen 8E"`, bottom smooth **Bezier Wave Line with soft violet gradient fill** (`#7E5BEF`) and hollow/ringed circular dot on the latest point.
    - Top-right card (`#DCECCB`, `100px` height): `"Body Water"` + `36px` white drop icon circle, `"58.4%"` + `"ICW 26.8 L · ECW 16.5 L"`.
    - Bottom-right card (`#F5E2D6`, `100px` height): `"Basal Metabolic"` + `36px` white flame icon circle, `"1,742 kcal"` + `"Visceral Level 5"`.
  - **Page 2 (Google Health Connect Live Context):**
    - Left tall card (`#DFD5F5`): `"Steps"` + wave chart (`"8,420"`).
    - Top-right card (`#DCECCB`): `"Sleep"` (`"7h 30min"`).
    - Bottom-right card (`#F5E2D6`): `"Calories Burned"` (`"2,149 kcal"`).
- **Bottom Card Preview:** `"Body Fat Trend"` white card (`28px` radius) with `"Weekly"` filter dropdown and instant Health Connect + Supabase Realtime live status chip (`"Live · Synced 12s ago"`).
- **Detached Bottom Bar (present on all main tabs):**
  - Left `#0F1115` capsule (`64px` height, `32px` radius) with 4 icons: `Home`, `Body Map`, `Reports`, `Profiles`. Active icon sits inside a `46px` `#FFFFFF` circle with `#0F1115` glyph.
  - Right detached `64px` `#2185F7` circular `+` button launching the Live Weigh-In modal.

#### Screen 3: Reports and Analytics (Exact Arc Gauge + Stippled Bar Chart)
- Top bar: `40px` circular Back button, `"Reports"` title, right Export/Share circle button.
- Pill Segmented Control: `Daily | Weekly (solid #111111 pill, white text) | Monthly` plus metric chip (`Body Fat %`, `Weight`, `Skeletal Muscle`, `ECW/TBW`).
- **3-Zone Semi-Circular Segmented Arc Gauge Card (`#FFFFFF`, `28px` radius):**
  - `180 deg` sweep, `18px` rounded-cap segments separated by `4 deg` gaps: `#2185F7` (Normal), `#F4B428` (Elevated), `#EF4444` (High).
  - White-filled circular thumb with colored outer border sitting on the active angle.
  - Centered readout (`"18.4 %"` or `"74.2 kg"`) + `"Overview"` subtitle.
  - Horizontal 3-dot legend below: `Normal (56%)`, `Elevated (28%)`, `High (16%)`.
- **Stippled Dot-Matrix Bar Chart Card (`"Average Body Fat"` / `"Weekly Trend"`, `#FFFFFF`, `28px` radius):**
  - 7 daily rounded-top columns (`Sat, Sun, Mon, Tue, Wed, Thu, Fri`).
  - Unselected bars render with a clean **stippled dot-matrix pattern** (`2px` dark dots inside `38px` rounded-top columns).
  - Tapping any bar turns it **solid `#111111`** with a `6px` white indicator dot near the top cap and a floating mint status pill (`"Normal"` / `"Optimal"`) connected by a vertical dashed line.

#### Screen 4: 8-Electrode 5-Segment Body Studio and 56-Parameter Explorer
- Interactive front anatomical body map split into 5 selectable zones (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`) with floating callout pills (`kg`, `%`, `Standard`/`High`).
- Top pill toggle switching `Segmental Muscle` vs `Segmental Fat`, plus Left-vs-Right bilateral symmetry progress bars (`Arm Balance 99.1%`, `Leg Balance 98.4%`) and Appendicular Lean Mass (`ALM/h^2`).
- Below: Searchable, categorized accordion listing all **56 parameters** across 7 physiological groups (Core, Adiposity, Muscle & Bone, Hydration & ICW/ECW, Segmental 8E, Metabolic & Cardiac, Raw Bioimpedance). Every parameter displays a clinical range bar, delta, and honest provenance pill (`8E BIA`, `Sensor`, `4E Est.`).

#### Screen 5: Live 8-Electrode BLE Weigh-In Simulation Modal
- Triggered by the `#2185F7` `+` FAB.
- Interactive 4-stage flow button/timer: `1. Searching (CULT Pro / CS-BF01)` -> `2. Weight Settling (0xCF live stream, tabular numbers)` -> `3. 8-Electrode Contact Active (4 foot pads + 4 handle pads lit in cobalt, dual-freq 0xBE progress ring)` -> `4. Locked & Synced` showing green badges: `Saved to Room`, `Written to Health Connect`, `Pushed to Supabase Realtime`.
- Also include an optional toggle demonstrating the `"2 profiles within 2.5 kg - Choose Member"` ambiguity picker.

#### Screen 6: History Ledger and Weigh-In Comparison
- Clean chronological table/cards of weigh-ins with scale icon (`8E Pro` vs `4E BF01` vs `Manual`), Health Connect badge, swipe/tap compare two sessions side-by-side showing delta across all 56 metrics.

#### Screen 7: 10-Member Household Profiles
- Grid of up to 10 member cards with avatar color, height/age/sex, target weight, auto-recognition window (`±2.5 kg`), and `+ Add Member (X/10)` button.

#### Screen 8: Health Connect and Supabase Sync Center
- Two clean status cards:
  - **Google Health Connect:** Permission status, per-type auto-write toggles (`Weight`, `Body Fat`, `Lean Mass`, `Bone Mass`, `Body Water`, `BMR`, `Heart Rate`), inbound context toggles (`Steps`, `Sleep`, `Calories`), `"Backfill All History"` button, and sync ledger counts.
  - **Supabase Realtime Cloud Sync:** Connection status (`Connected · ap-south-1 · Zero-Refresh Live`), account email, pending outbox count (`0 pending`), and `"Export JSON / CSV"` actions.

#### Screen 9: BLE Raw Packet Inspector
- `JetBrains Mono` live packet console displaying `0xCF` (11B), `0xBE` Set A & Set B (13B x 2 -> 10 ohm channels grid), and `0xDF` (17B x 4) hex frames with XOR checksum badges (`PASS`/`FAIL`), plus `"Copy Capture JSON"` button.

Ensure every screen leaves `96px` bottom clearance above the floating `#0F1115` dock and `#2185F7` FAB, uses generous breathing room, and maintains crisp contrast in both Daylight and Obsidian Night themes.
```
