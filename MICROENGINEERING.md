# OpenCult Applied Micro-Engineering Specification (`MICROENGINEERING.md`)

> **Living Craft Constitution.** Subordinate to `AGENTS.md`, `TRD.md`, `PRD.md`, `INVARIANTS.md`, and `HALL_OF_SHAME.md` on security and data rules, and binding on every screen, Canvas chart, touch target, elevation, and interaction state across OpenCult (both `prototype/` and native Android Jetpack Compose).

Every UI regression recorded in `HALL_OF_SHAME.md` MUST update the relevant rule in this file and add or tighten an invariant in `INVARIANTS.md` in the same commit.

---

## 1. Core Principles

1. **Physical mass on touch:** Interactive controls compress on pointer/finger down and spring back on release. Nothing changes state silently without visual acknowledgment.
2. **Perceptual response budget:**
   - `< 60 ms`: Touchdown scale + optional haptic tick (`Touchdown`).
   - `80 - 180 ms`: Spring payoff back to rest (`Payoff`).
   - `180 - 300 ms`: Bento page switches, sheet entrances, and accordion expansions.
   - `>= 650 ms`: Reserved for deliberate hold-to-confirm / destructive actions only.
3. **Clinical legibility over ornament:** Soft pastel surfaces, generous outer margins (`20.dp`), crisp contrast (`>= 4.5:1`), and tabular numerals keep attention on body telemetry instead of borders, noise gradients, or filler pills.

---

## 2. The 4-Stage Micro-Action Contract (Mandatory)

Every clickable, toggleable, or draggable control MUST implement all four stages. Omitting any stage is an incomplete implementation.

| Stage | Name | Android Jetpack Compose Mechanism | Prototype Web / CSS Parity |
|---|---|---|---|
| `1` | **Touchdown** | `Modifier.pointerInput` / `MutableInteractionSource` driving `graphicsLayer` scale compression + `LocalHapticFeedback` where warranted | `:active` scale compression without layout shift |
| `2` | **Tracking** | Gesture delta / drag progress driven via state lambda inside `graphicsLayer` or `Canvas` `DrawScope` (never forcing parent tree recomposition) | Pointer / scroll tracking via CSS custom props |
| `3` | **Release / Payoff** | Restitution using `OpenCultMotion.PressFeedback` (`stiffness = 560f`, `dampingRatio = 0.78f`) | Spring-matched cubic-bezier transition (`.18s`) |
| `4` | **Material Consequence** | Visible world update: dock active pill shifts inside `#0F1115`, chevron rotates `180 deg`, Sync / Health Connect ledger chip switches state, toast appears clear of dock | DOM state + `aria-pressed` / badge + toast |

### Scale Targets by Element Class

| Element Class | Touchdown Scale | Haptic on Trigger | Notes |
|---|---|---|---|
| Circle buttons, pager arrows `<` / `>`, dock tabs | `0.92f` | Optional light tick on tab switch | Silhouette stays `32.dp` / `36.dp` / `46.dp`; hitbox stays `>= 48.dp` |
| Cobalt Measure FAB (`64.dp`) | `0.94f` | Light tick on open | Paired `12.dp` beside the `#0F1115` dock pill |
| Bento cards, Hero card, Member / History rows | `0.97f` | LongPress tick only on long-press compare/delete | Preserve vertical `LazyColumn` scroll without capture conflict |
| Primary CTA (`Next >>>`, `Save reading`, `Connect`) | `0.96f` | `LongPress` tick once on weigh-in lock / save | Never double-fire haptic on coalesced duplicate `0xCF` packets |

---

## 3. Touch Targets, Insets, and Compact (`360.dp`) Geometry

Android is touch-first. Hover states in `prototype/` are desktop conveniences and never replace touch feedback or visible affordances.

1. **Hard Minimum Hitbox (`DRD:M-03`, `INV-18`):** Every interactive target (back circle button, `32.dp` `<` / `>` bento pager arrows, `36.dp` bento icon badges if interactive, `46.dp` dock icons, `Switch` toggles, `38.dp` stipple bars, and 5 body map limb regions) MUST maintain an interactive touch area of at least `48.dp x 48.dp` in Compose (`minimumInteractiveComponentSize()` or extended hit padding). The prototype models this via extended `::after` pseudo hit-areas (`prototype/styles.css` lines 2456-2471).
2. **Detached Floating Dock Clearance (`INV-13`):** The `#0F1115` capsule (`height = 64.dp`, `radius = 32.dp`) plus `#2185F7` FAB sits `12.dp` above `WindowInsets.navigationBars`. Every scrollable surface MUST include `dockHeight (64.dp) + 12.dp + navigationBarsBottom + 24.dp` (`>= 116.dp`) bottom content padding.
3. **Toast / Snackbar Anchor Rule:** Confirmation pills (`Saved locally`, `Health Connect written`) MUST clear the floating dock (`bottom >= 116.dp`) or sit pinned cleanly beneath the status bar so they never overlap the dock pill or screen primary buttons.
4. **Compact Width (`360.dp`) Overflow Gate:**
   - Never use viewport-width units (`100vw`) or fixed pixel widths wider than the parent container.
   - Stipple chart bar width narrows smoothly (`38.dp -> 32.dp`) with `5.dp` column spacing below `360.dp`, matching `prototype/styles.css:2427-2434`.
   - Compact hardware chips (`hw-tag`) collapse cleanly or ellipsize (`TextOverflow.Ellipsis`) before wrapping tabular numbers onto two lines.

---

## 4. Surface Materiality, Elevation, and Token Exclusivity (`INV-10`)

Every visual property MUST resolve through `OpenCultTheme` tokens (`DRD:T-01..T-25`). Hardcoding `Color(0x...)`, magic font sizes, or magic corner radii inside `feature/` composables is banned.

| Surface Tier | Daylight Clinical Pastel (`Default`) | Obsidian Night Mode |
|---|---|---|
| **Screen Canvas** | `#F4F6FA` (`bgCanvas`) | `#0A0D12` |
| **Elevated White / Dark Card** | `#FFFFFF` (`bgSurface`) + soft ambient shadow (`0.dp, 10.dp, 28.dp`, `inkPrimary` at `5%`) | `#131720` + `1.dp` `#232A38` hairline border (no muddy black shadows) |
| **Hero / Bento Pastels** | Sky `#CFE0F2`, Lavender `#DFD5F5`, Sage `#DCECCB`, Peach `#F5E2D6` | Sky `#152536`, Lavender `#221B36`, Sage `#192A1E`, Peach `#2E1F18` + subtle hairline border |
| **Detached Pill Dock** | `#0F1115` pill + `#FFFFFF` active circle (`46.dp`) | `#1B202B` pill + `#2B3546` hairline border + high-contrast active circle |
| **Modal Bottom Sheet** | Scrim `rgba(6, 9, 14, 0.48)` + `#FFFFFF` `28.dp` squircle + `1.dp` `#E4E9F1` border | Scrim + `#131720` `28.dp` squircle + `1.dp` `#232A38` border |
| **Onboarding Breakout Preview (`DRD:C-07`)** | Dark phone bezel (`22.dp` radius) holding miniature Home; Hero Weight card scales `1.06x` across outer edge with `16.dp` elevation | Same geometry adapted to Obsidian Night surface tokens |

### Navigation Chrome Hygiene

- Home top header contains the left profile avatar + greeting (`Good Morning` / member name) and right 2-action pill (`Sync Bell` + `Nav Drawer`). Do NOT crowd the Home top header with inline dropdowns, search boxes, or ASCII `|` / `·` control separators.
- All dividers in interactive pills use hairline rules or gaps, never typed ASCII `|` characters inside action buttons.

---

## 5. Custom Canvas Geometry Precision (`DRD:C-01..C-06`, `INV-11`)

Never substitute generic charting wrappers (MPAndroidChart, Vico defaults, stock M3 sliders) for OpenCult's custom `Canvas` / `DrawScope` drawings.

### A. `DRD:C-01` Hero 9-Column Spark-Bars
- Exactly 9 vertical stadium-cap capsules (`4.dp` wide, `6.dp` gap, rounded `2.dp` top/bottom caps).
- Relative height mapped cleanly between `26%` and `92%` of plot height so the smallest bar stays crisp.
- Baseline bars draw in `#263238` (Light) / `#CBD5E1` (Dark); flagged peak or out-of-range bars draw in `zoneCoral` (`#EF4444` / `#F87171`).

### B. `DRD:C-02` Bento Asymmetric Grid & Bezier Waves
- 2-column asymmetrical grid (`12.dp` gutter): Left tall Lavender card (`212.dp`) beside two stacked `100.dp` cards (Sage top, Peach bottom) with `12.dp` vertical gap.
- Wave curve uses midpoint quadratic/cubic smoothing (`2.dp` stroke, `StrokeCap.Round`, `StrokeJoin.Round`), vertical area gradient fading from `22%` alpha to `0%`, and a terminal `8.4.dp` hollow circular marker (`surface` fill + `2.2.dp` stroke).

### C. `DRD:C-03` 3-Zone Semi-Circular Segmented Arc Gauge
- `180 deg` sweep (`startAngle = 180f`), `18.dp` stroke width, `StrokeCap.Round` (flat `StrokeCap.Butt` ends are permanently banned per `SHAME-002`).
- `4 deg` angular gaps separating the 3 clinical zones (`Optimal` `#2185F7`, `Elevated` `#F4B428`, `High` `#EF4444` per `DRD.md`).
- Note documented drift (`SHAME-006`): `prototype/app.js` lines 584 & 620 currently draw `#3B82F6` / `#34C77B` / `#F4B428` with `gap = 8`. Native Android Compose MUST follow `DRD.md` (`#2185F7` / `#F4B428` / `#EF4444` with `4 deg` gaps) per `INV-22` until the prototype is aligned.
- Indicator thumb: `24.dp` surface-filled outer circle with `5.dp` ring stroke in the active zone's color, placed at exact polar coordinates `(cx + r * cos(theta), cy + r * sin(theta))`.

### D. `DRD:C-04` Stippled Dot-Matrix Bar Chart
- 7 columns (`38.dp` bar width, `14.dp` top corners, `5.dp` bottom corners; `32.dp` width on `<= 360.dp` screens).
- **Unselected bars:** Clip to bar `Path`, draw deterministic dot-matrix grid with `2.2.dp` dot radius, `6.dp` horizontal/vertical pitch, and **half-pitch (`3.dp`) horizontal stagger on odd rows** (`#1E242E` at `65%` alpha Light, `#CBD5E1` at `55%` alpha Dark). Do NOT replicate the prototype's single-layer non-staggered CSS grid (`prototype/styles.css:1164`).
- **Selected bar:** Solid `inkPrimary` (`#0F141C` Light / `#F4F6FA` Dark) fill, `6.dp` contrasting dot centered `12.dp` below the top cap, `1.dp` dashed vertical connector line, and floating status pill clamped cleanly inside chart bounds so edge bars (`Sat` / `Fri`) never clip offscreen.
- Keep the bottom of the Stipple card clean with weekday labels only; never embed an extra multi-column stats footer inside the chart card (`SHAME-002`).

### E. `DRD:C-06` 8-Electrode 5-Segment Body Map
- Clean front-view human silhouette split into 5 tappable `Path` regions (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`), `Muscle` vs `Fat` pill switch at top, clamped floating callout chips, and Left/Right + Upper/Lower balance bars below.
- When `scaleMode` is `4E CS-BF01` or `Pro Feet-Only` (`status = 0x00`), limb-specific direct measurements are disabled/suppressed cleanly with explicit provenance banners rather than inventing segmental kg figures (`INV-02`, `INV-03`).

---

## 6. Telemetry Typography and Hardware Honesty Micro-Rules

1. **Mandatory `tnum` (`INV-12`):** Apply `fontFeatureSettings = "tnum"` on every live weight, impedance ohm cell, body fat %, SMM kg, ECW/TBW ratio, delta string, and hex frame column. Width jitter during streaming ticks is a defect.
2. **Monospace Wire Console (`DRD:C-10`):** BLE inspector rows use `JetBrains Mono`, align byte offsets in `16`-column grid or wrapped byte tokens, and tag every frame with frame header (`0xCF` / `0xBE` / `0xDF` / `FD 37` / `F1` / `F2`), packet index, and XOR pass/fail pill.
3. **Dual-Scale Provenance Discipline (`INV-02`, `INV-14`):**
   - `Cult Smart Scale Pro (8E)`: Displays `8E BIA` badge, 10-channel `0xBE` (`20 kHz` + `100 kHz`) matrix, 4x `0xDF` counter, and all 56 parameters.
   - `Cult Smart Body Fat Scale (CS-BF01, 4E)`: Displays `4E Est.` badge, `FD 37 -> F1 -> F2 00` handshake log, weight-only `0xCF`, clearly marks anthropometric/single-loop derivations (`INV-02`), and syncs aggregate HC records honestly.
   - `Pro Feet-Only (status = 0x00)`: Suppresses all BIA compartments cleanly and writes verified `WeightRecord` only (`INV-03`).

---

## 7. Pre-Commit Micro-Engineering Audit

Before marking any UI or prototype change complete, verify every box:

- [ ] Every interactive element satisfies all 4 stages (`Touchdown`, `Tracking`, `Payoff`, `Consequence`) without blocking scroll.
- [ ] Every touch target has an interactive area `>= 48.dp x 48.dp` (`>= 44 px` in web prototype).
- [ ] Bottom dock inset (`>= 116.dp`) clears all scrollable content and toast/snackbar overlays on a `360.dp` viewport.
- [ ] Zero hardcoded `Color(0x...)`, font sizes, or radii appear inside `feature/` composables (`INV-10`).
- [ ] Light (`Daylight Clinical Pastel`) and Dark (`Obsidian Night`) modes both maintain `>= 4.5:1` contrast (`DRD:M-04`) and replace ambient shadows with `#232A38` hairlines in dark mode.
- [ ] All Canvas charts (`C-01`, `C-02`, `C-03`, `C-04`, `C-06`) render via Compose `DrawScope` with round arc caps, staggered stipples, and non-clipping callouts.
- [ ] Every live number, impedance readout, and hex dump uses `tnum` (`INV-12`).
- [ ] `4E CS-BF01`, `8E Pro`, and `Pro Feet-Only` states render accurate provenance tags without fake segmental claims (`INV-02`, `INV-03`, `INV-14`).

---

## 8. Permanently Banned Micro-Patterns

| Banned Pattern | Reason | Required Replacement |
|---|---|---|
| Stock Material `NavigationBar` or stock `FloatingActionButton` | Breaks detached `#0F1115` dock + `#2185F7` FAB geometry (`DRD:C-05`) | Custom `DetachedObsidianDock` + `CobaltMeasureFab` |
| Flat `StrokeCap.Butt` on `DRD:C-03` arc segments | Causes harsh square breaks along curved gauge (`SHAME-002`) | `StrokeCap.Round` with `4 deg` gaps |
| Extra multi-metric summary row inside Stippled Bar card | Violates chart card breathing room and hierarchy (`SHAME-002`) | Period label axis row only beneath bars |
| Un-staggered square dot grid on native Android `DRD:C-04` | Drifts from `DRD.md` staggered dot-matrix pattern (`SHAME-006`) | Canvas odd-row half-pitch staggered `2.2.dp` dots |
| Proportional (non-`tnum`) font on weigh-in digits or ohm cells | Causes horizontal column trembling on `130 ms` updates (`INV-12`) | `fontFeatureSettings = "tnum"` on numeric styles |
| Hover-only labels, tooltips, or action reveals | Unreachable on Android touch devices | Explicit tap/press selection + TalkBack labels |
| Faking 8-electrode segmental values in `4E` or `Pro Feet-Only` mode | Clinical dishonesty (`INV-02`, `INV-03`, `SHAME-003`) | Gate segmental cards by verified `ScaleHardwareMode` |
