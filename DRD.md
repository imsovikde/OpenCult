# OpenCult Design Requirements Document (`DRD.md`)

Binding visual, typographic, component, layout, and motion specification for **OpenCult**, derived directly from the reference design and adapted for 8-electrode (56-parameter) and 4-electrode (20+ parameter) body composition, Google Health Connect, and Supabase Realtime sync.

---

## 1. Visual Grammar and Theme Architecture (`DRD:T-*`)

OpenCult uses a **Daylight Clinical Pastel** default theme with an **Obsidian Night** dark theme toggle. Surfaces use soft squircle geometry (`24dp` / `28dp` corners), zero heavy outlines, high-contrast tabular numbers, and a detached dark floating bottom dock paired with a cobalt action button.

### 1.1 Color Tokens

| Token ID | Semantic Token | Daylight Mode (Default) | Obsidian Night Mode | Usage |
|---|---|---|---|---|
| `DRD:T-01` | `bgCanvas` | `#F4F6FA` | `#0A0D12` | Primary screen backdrop |
| `DRD:T-02` | `bgSurface` | `#FFFFFF` | `#131720` | Elevated report cards, sheets, circular controls |
| `DRD:T-03` | `bgSubtlePill` | `#EEF1F6` | `#1C222E` | Segmented control tracks, unselected toggles |
| `DRD:T-04` | `inkPrimary` | `#0F141C` | `#F4F6FA` | Primary numbers, headings, active bar (Daylight) |
| `DRD:T-05` | `inkSecondary` | `#5B6472` | `#A7B1C2` | Subtitles, units, timestamps |
| `DRD:T-06` | `inkMuted` | `#8C95A4` | `#6F7A8C` | Axis labels, captions, secondary metadata |
| `DRD:T-07` | `hairlineBorder` | `#E4E9F1` | `#232A38` | Subtle dividers and chip rings |
| `DRD:T-08` | `pastelSky` | `#CFE0F2` | `#152536` | Hero Weight / Body Fat card |
| `DRD:T-09` | `pastelLavender` | `#DFD5F5` | `#221B36` | Tall Bento card (Skeletal Muscle / Steps), wave `#7E5BEF` (light) / `#A688FA` (dark) |
| `DRD:T-10` | `pastelSage` | `#DCECCB` | `#192A1E` | Top-right Bento card (Body Water ECW-ICW / Sleep) |
| `DRD:T-11` | `pastelPeach` | `#F5E2D6` | `#2E1F18` | Bottom-right Bento card (BMR & Visceral / Calories) |
| `DRD:T-12a` | `accentCobalt` | `#2185F7` | `#3B96FF` | Primary CTA, Measure FAB, Optimal arc zone, active onboarding pill |
| `DRD:T-12b` | `statusNormalBg` / `Fg` | `#D6F5E3` / `#117A4B` | `#123626` / `#5CE09B` | `Normal` / `Optimal` / `Synced` pill badge |
| `DRD:T-12c` | `zoneAmber` | `#F4B428` | `#F7C04A` | `Elevated` arc zone |
| `DRD:T-12d` | `zoneCoral` | `#EF4444` | `#F87171` | `High` arc zone, out-of-band hero spark-bar spike |
| `DRD:T-12e` | `dockContainer` | `#0F1115` | `#1B202B` | Detached floating bottom navigation pill |

### 1.2 Typography Tokens (`DRD:T-13..T-18`)

- **UI and Display Font:** `Plus Jakarta Sans` (Weights `500`, `600`, `700`).
- **Telemetry Font:** `JetBrains Mono` (Weights `400`, `500`, `700`) for BLE packet hex, impedance ohms, and sync identifiers.
- **Tabular Numerals:** `fontFeatureSettings = "tnum"` is mandatory on every metric readout (`INV-12`).

| Token ID | Role | Size / Line Height | Weight | Tracking |
|---|---|---|---|---|
| `DRD:T-13` | `displayArc` | `48sp / 52sp` | Bold `700` | `-1.0sp` |
| `DRD:T-14` | `heroMetric` | `44sp / 48sp` | Bold `700` | `-1.0sp` |
| `DRD:T-15` | `greetingTitle` | `26sp / 32sp` | SemiBold `600` | `-0.4sp` |
| `DRD:T-16` | `bentoValue` | `26sp / 30sp` | Bold `700` | `-0.4sp` |
| `DRD:T-17` | `cardTitle` | `14sp / 18sp` | Medium `500` | `0.0sp` |
| `DRD:T-18` | `badgeAndCaption` | `12sp / 16sp` | SemiBold `600` / Regular `400` | `0.1sp` |

### 1.3 Radii, Spacing, and Elevation (`DRD:T-19..T-25`)

- `DRD:T-19` `radiusHeroCard`: `28.dp`
- `DRD:T-20` `radiusBentoCard`: `22.dp`
- `DRD:T-21` `radiusPill`: `999.dp`
- `DRD:T-22` `badgeIconCircle`: `36.dp` (`#FFFFFF` in Light, `#232936` in Dark)
- `DRD:T-23` `screenPaddingHorizontal`: `20.dp`
- `DRD:T-24` `bentoGutter`: `12.dp`
- `DRD:T-25` `ambientShadow`: `0.dp` X, `10.dp` Y, `28.dp` blur, `inkPrimary` at `5%` alpha (Light) or hairline `#232A38` border (Dark).

---

## 2. Signature Custom Components (`DRD:C-01..C-10`)

### `DRD:C-01` Hero Weight and Body Fat Card
- Surface: `pastelSky` (`#CFE0F2` / `#152536`), radius `28.dp`, padding `20.dp`.
- Top row: Metric switcher chip (`Weight` / `Body Fat %` / `SMM`) on left; `statusNormalBg`/`Fg` pill (`Normal` or `Optimal`) on right.
- Bottom-left: Large `heroMetric` value + unit (`74.2` + `kg`), plus compact delta line (`-0.4 kg vs last`) and scale badge (`8E Pro` or `4E BF01`).
- Bottom-right: Custom `Canvas` **9-Column Vertical Capsule Spark-Bars**, each bar `4.dp` wide with `6.dp` spacing and stadium caps; baseline sessions in `#263238` (Light) / `#CBD5E1` (Dark), flagged outlier sessions in `zoneCoral` (`#EF4444`).

### `DRD:C-02` Asymmetric 2-Column Overview Bento Grid with Pager
- Header row: `"Overview"` (`18.sp` SemiBold) on left; two `32.dp` circular pager buttons on right (`<` white/surface circle, `>` solid `#111111` circle in Light or `#F4F6FA` in Dark).
- Grid layout (`12.dp` gutter):
  - **Left column (`weight = 1f`, height `212.dp`)**: `pastelLavender` tall card with top-right `36.dp` white icon badge, primary value, and bottom **Bezier Wave Line + Soft Gradient Fill** (`#7E5BEF` stroke, circular point marker on latest reading).
  - **Right column (`weight = 1f`, stacked two `100.dp` cards with `12.dp` vertical gap)**:
    - Top: `pastelSage` card with top-right `36.dp` white icon badge and value.
    - Bottom: `pastelPeach` card with top-right `36.dp` white icon badge and value.
- **Page 1 (Body Composition):** Lavender = Skeletal Muscle Mass (`42.8%`, `31.8 kg`); Sage = Body Water + `ECW/TBW` (`58.4%`, `0.378`); Peach = BMR + Visceral Fat (`1,742 kcal`, `Level 5`).
- **Page 2 (Health Connect Daily Context):** Lavender = Steps (`8,420`); Sage = Sleep (`7h 30m`); Peach = Calories Burned (`2,149 kcal`).

### `DRD:C-03` 3-Zone Semi-Circular Segmented Arc Gauge
- Used on `Reports` screen inside a `bgSurface` (`#FFFFFF`) card (`28.dp` radius).
- `Canvas` geometry: `180 deg` sweep (`startAngle = 180f`), `18.dp` stroke width, `StrokeCap.Round`, separated into three proportional segments by `4 deg` gaps:
  1. Optimal / Normal (`#2185F7`)
  2. Elevated (`#F4B428`)
  3. High / Alert (`#EF4444`)
- Indicator thumb: `24.dp` outer white-filled circle with `5.dp` ring stroke colored to the active zone, positioned along the arc radius at the current reading angle.
- Center stack: `displayArc` readout + unit + `"Overview"` subtitle.
- Bottom legend: 3-item horizontal dot breakdown (`Normal 56%`, `Elevated 28%`, `High 16%`).

### `DRD:C-04` Stippled Dot-Matrix Bar Chart
- Used in the `Average / Trend` card on `Reports` (`bgSurface`, `28.dp` radius).
- 7 columns (`Sat..Fri` or `Mon..Sun`), `38.dp` width (`14.dp` top corners, flat bottom).
- Unselected bars: Clipped rounded-top bar path filled by a deterministic `Canvas` dot-matrix grid (`2.2.dp` dot radius, `6.dp` spacing, staggered odd rows, `#1E242E` at `65%` alpha in Light, `#CBD5E1` at `55%` alpha in Dark).
- Active / tapped bar: Solid `#111111` (Light) / `#F4F6FA` (Dark) fill, a `6.dp` contrasting dot `12.dp` below the top cap, and a floating `Normal`/`Optimal` mint badge above the top cap joined by a `1.dp` dashed line.

### `DRD:C-05` Detached Obsidian Pill Dock and Cobalt Measure FAB
- Pinned at bottom with `navigationBarsPadding()` + `12.dp` bottom margin, `20.dp` horizontal margin.
- Left container: `#0F1115` capsule (`height = 64.dp`, `radius = 32.dp`, weight `1f`) holding 4 destinations (`Home`, `Body Map`, `Reports`, `Profiles`). The active tab sits inside a `46.dp` `#FFFFFF` circle with `#0F1115` icon.
- Right button: `64.dp` circular `accentCobalt` (`#2185F7`) button (`+` / Scale icon) that opens the Live Weigh-In modal directly.

### `DRD:C-06` 8-Electrode 5-Segment Anatomical Body Map
- Front-view minimalist human silhouette rendered in `Canvas` / `Path` split into 5 independently tappable zones (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`).
- Mode pill toggle at top: `Segmental Muscle (kg / %)` vs `Segmental Fat (kg / %)`.
- Tapping a zone highlights it in `#2185F7` / `#7E5BEF` and pins floating callout chips showing mass, % of standard, and rating (`Low` / `Standard` / `High`).
- Below silhouette: Left-vs-Right and Upper-vs-Lower balance bars.

### `DRD:C-07` Onboarding Device-in-Device Preview and Pill CTA
- Background: Vertical wash `#CBE1F7` -> `#F4F6FA` (Light) or `#132235` -> `#0A0D12` (Dark).
- Top bar: `40.dp` white back circle + `Skip` pill (`#BED6EE` tint).
- Headline (`28.sp` Bold, 2 lines) + concise subtitle + 3-step capsule indicator (`24.dp` active `#2185F7` pill + `6.dp` inactive dots).
- Centerpiece: Dark-bezel phone frame (`22.dp` corner radius) displaying a miniature Home preview, with the Hero Weight/Fat card breaking horizontally outside the phone frame (`1.06x` width, elevated `16.dp` shadow).
- Bottom CTA track: `60.dp` white pill container holding a `#2185F7` `"Next >>>"` (or `"Connect Scale >>>"`) button.

### `DRD:C-08` Live 8-Electrode Weigh-In Sheet
- Full-height modal sheet showing:
  - Live `0xCF` weight digits (`tnum`, `56.sp` Bold).
  - 8-pad hardware contact diagram (4 foot pads + 4 handle pads) lighting up cobalt/mint as status moves `0x01 -> 0x08 -> 0x05`.
  - 2-packet `0xBE` impedance sweep indicator + `0xDF` capture counter (`0/4..4/4`).
  - Instant confirmation chips on lock: `Saved Locally`, `Health Connect Written`, `Supabase Synced`.

### `DRD:C-09` 56-Parameter Categorized Accordion and Clinical Range Bar
- Searchable list grouped into 7 expandable sections (Core & Weight, Adiposity & Fat Distribution, Muscle & Skeletal, Hydration & Cellular Fluids, Segmental 8-Electrode, Metabolic & Cardiovascular, Raw Bioimpedance).
- Each row shows value, unit, delta, provenance tag (`8E BIA`, `4E Est.`, `Sensor`), and a horizontal multi-zone clinical range bar with marker dot.

### `DRD:C-10` BLE Raw Packet Inspector Console
- `JetBrains Mono` stream of raw `0xCF`, `0xBE`, and `0xDF` frames with byte offsets, XOR checksum pass/fail badge, 10-channel `0xBE` ohm matrix (`Set A[0..4]`, `Set B[0..4]`), and 1-tap JSON/hex export.

---

## 3. Screen Inventory (`DRD:S-01..S-09`)

1. `DRD:S-01` **Onboarding Carousel** (3 steps: Body Composition Insights, 8-Electrode + Health Connect Sync, Household & Supabase Cloud).
2. `DRD:S-02` **Home Dashboard** (Greeting + Profile Avatar Switcher + Sync Status Bell + Hero Card + 2-Page Bento Overview + Trend Preview + Floating Dock).
3. `DRD:S-03` **Live BLE Weigh-In Modal** (4-electrode and 8-electrode adaptive states + ambiguous profile resolver sheet).
4. `DRD:S-04` **Reports and Analytics** (`Daily`/`Weekly`/`Monthly` pill switcher, 3-Zone Arc Gauge, Stippled Dot-Matrix Bar Chart, Metric Selector).
5. `DRD:S-05` **8-Electrode Body Map and 56-Parameter Explorer**.
6. `DRD:S-06` **History Ledger and Weigh-In Comparison**.
7. `DRD:S-07` **Household Profiles (up to 10 members)**.
8. `DRD:S-08` **Health Connect and Supabase Realtime Sync Center**.
9. `DRD:S-09` **BLE Raw Packet Inspector and Export**.

---

## 4. Motion and Accessibility (`DRD:M-01..M-05`)

- `DRD:M-01` Spring physics: `stiffness = 420f`, `dampingRatio = 0.82f` for bento paging, tab pill switches, and arc thumb glide.
- `DRD:M-02` Reduced motion: Respect `Settings.Global.ANIMATOR_DURATION_SCALE` and `LocalReducedMotion`; snap counts and arcs immediately when reduced motion is enabled.
- `DRD:M-03` Minimum touch target `48.dp x 48.dp` on all buttons, pager arrows, dock tabs, and body segments.
- `DRD:M-04` Minimum `4.5:1` text contrast across both Daylight Pastel and Obsidian Night themes.
- `DRD:M-05` TalkBack / accessibility content descriptions on every custom `Canvas` chart summarizing the numerical reading and zone classification.
