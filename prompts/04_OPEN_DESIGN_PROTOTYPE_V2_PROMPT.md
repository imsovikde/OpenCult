# OpenCult v2 Open Design Prototype Prompt (Multi-Screen Minimalist + Dual-Scale 4E/8E)

## How to Use This Prompt in Open Design

1. Open **Open Design** (`# Prototype` mode) with your working directory pointed at `C:\Users\imsov\OneDrive\Desktop\Open Scale project\prototype`.
2. Attach your UI reference screenshot (`media_1795661960541_dfba6bfd.jpg`).
3. Copy and paste the **entire fenced prompt below** into Open Design so it upgrades `prototype/index.html`, `prototype/styles.css`, and `prototype/app.js` in place while keeping the existing working logic intact and fixing every visual, layout, and dual-scale gap.

---

```text
Upgrade the existing OpenCult web prototype inside the `prototype/` folder (`index.html`, `styles.css`, `app.js`) into a clean, minimalist, pixel-accurate, multi-screen interactive prototype for Android that matches the attached reference screenshot closely while supporting BOTH of the user's scales end to end:

1. **Cult Smart Scale** (`CS-BF01`, 4-electrode ITO foot-only, 20+ anthropometric + sensor parameters, BLE `0xFFF0` / `0xFFF1` OneByone handshake + `0xCF` 11-byte frames).
2. **Cult Smart Scale Pro** (8-electrode with retractable handle, dual-frequency `20 kHz / 100 kHz`, 56 whole-body + segmental + cellular parameters, BLE `0xCF` + 2x `0xBE` 10-channel impedances + 4x `0xDF` capture packets).

### A. Critical Defects in Current `prototype/` That You MUST Fix First

1. **Remove iPhone Hardware Chrome (This is an Android App):**
   - Delete `.dynamic-island` and Apple `.home-indicator` and change `data-platform="iphone"` to `data-platform="android"`.
   - Use a clean modern Android flagship bezel: subtle centered punch-hole camera dot (`10px`), Android status bar (`9:41`, Wi-Fi, 5G, battery), and thin Android gesture pill at bottom (`108px x 4px`).
2. **Fix the "Only One Screen Available" Problem with a Dual Presentation Shell:**
   - Above the mobile viewport (completely outside the Android frame, in a clean lightweight **Prototype Studio Bar**), add three instant controls so reviewers see the entire product immediately:
     - **View Mode:** `Gallery (All Screens)` vs `Interactive Phone (1:1)`.
       - In **Gallery Mode**, render a crisp horizontal/grid gallery of interactive Android frames side by side (starting with the flagship 3-screen composition from the reference image: **Onboarding**, **Home**, and **Reports**, followed by **Body Studio**, **Live Dual-Scale Weigh-In**, **History**, **Profiles (10)**, **Sync Center**, and **Packet Inspector**). Every frame is interactive, and clicking `"Open 1:1"` on any card focuses it in `Interactive Phone` mode.
       - In **Interactive Phone (1:1)** mode, show a clean pill screen-switcher rail directly above the Android device (`Onboarding`, `Home`, `Reports`, `Body Studio`, `History`, `Profiles`, `Sync`, `Inspector`, plus `Start Weigh-In` button) so zero screens are hidden behind buried menus, while all in-app buttons, bottom dock tabs, and sheets remain 100% functional.
     - **Connected Scale Mode (Global Live Switcher):**
       - `Cult Smart Scale Pro (8-Electrode · 56 Params)`
       - `Cult Smart Scale CS-BF01 (4-Electrode · 20+ Params)`
       - `Smart Scale Pro Feet-Only (No Handle · 4E Fallback)`
       Switching the connected scale updates **every single screen** live in real time (Home Hero hardware note, Bento availability, Body Studio 5-Segment Body Map active/disabled state, 20 vs 56 parameter availability + provenance badges, Live Weigh-In pad diagram, and Packet Inspector hex stream).
     - **Theme Switcher:** `Daylight Pastel` (default) vs `Obsidian Night`.
3. **Strip Out Visual Clutter to Match the Minimalist Reference Screenshot (`media_1795661960541_dfba6bfd.jpg`):**
   - Look closely at the reference screenshot. Its beauty comes from quiet whitespace, soft pastel surfaces, zero noisy sub-badges, and ultra-clean hierarchy. Apply these exact refinements:
   - **Onboarding Screen (Screen 1):**
     - Sky-blue top wash (`#CBE1F7` -> `#F4F6FA`), `40px` white circular Back button + `#BED6EE` `Skip` pill, crisp 2-line headline (`28px` Bold) + concise 2-line subtitle + 3-step indicator (`24px` `#2185F7` pill + `6px` dots).
     - **Fix the embedded mini-phone:** Make the embedded device frame larger (`264px` wide) and anchor it lower so its bottom fades gently into `#F4F6FA` right above the CTA track (`mask-image: linear-gradient(180deg, #000 74%, transparent 100%)`), matching the cropped floating mockup in the screenshot. Inside it, show the clean Home header (`Good Morning`, `Wyatt Anderson`, 2-icon pill) and **ONE elevated white Hero card breaking outside the phone width (`1.08x`)** with soft shadow, metric title, mint `"Normal"` pill, large tabular value, and 9 vertical capsule bars. Below it inside the mini-phone show clean uncluttered Lavender/Sage tiles (no tiny `6px` micro-footers).
     - **Fix the bottom CTA:** Inside the `60px` white pill track, place a `#2185F7` inner pill (`Next` left-aligned inside the blue capsule, `>>>` triple chevrons right-aligned inside the track/pill), matching the reference screenshot button silhouette.
   - **Home Screen (Screen 2):**
     - **Header:** Circular avatar (`44px`), `"Good Morning"` + bold name, and on the right a **clean 2-icon white pill** (`Bell` + `Menu/Hamburger` ONLY; move the theme toggle cleanly into the Menu sheet and Studio Bar so the header stays calm and exact).
     - **Hero Card (`#CFE0F2`, `28px` radius, soft surface with zero heavy shadow):**
       - Top-left: Clean label `"Weight"` with a subtle tiny caret (`Weight v`) that taps/cycles smoothly between `Weight`, `Body Fat %`, and `Skeletal Muscle`, replacing the cramped 3-tab track inside the sky card.
       - Top-right: Single clean mint `"Normal"` / `"Optimal"` pill (no extra boxy `8E Pro` tag next to it; show a quiet single caption `"8E Pro · -0.4 kg vs Tue"` or `"4E CS-BF01 · -0.4 kg vs Tue"` neatly under the big readout).
       - Bottom-left: Crisp `44px` tabular readout (`74.2 kg`) + single delta/device line.
       - Bottom-right: **9-Column Vertical Rounded Capsule Spark-Bars** (`4px` width, `6px` gap, `#263238` normal, `#EF4444` spike).
     - **Overview Header:** `"Overview"` (`18px` SemiBold) + `32px` circular `<` (`#FFFFFF`) and `>` (`#111111`) pager buttons.
     - **Asymmetric 2-Column Bento Grid (`12px` gutter):**
       - **Page 1 (Body Metrics):**
         - Left tall Lavender card (`#DFD5F5`, `212px`): Title `"Skeletal Muscle"` (8E) or `"Lean Mass"` (4E), `36px` white icon circle top-right, bold value (`42.8 %` or `60.5 kg`), and at the bottom a **single clean `#7E5BEF` wave line with ONE hollow white-filled circular dot** (keep the lavender card pure and calm without heavy text stacks or noisy gradients).
         - Top-right Sage card (`#DCECCB`, `100px`): Title `"Body Water"`, `36px` white icon circle, bold value (`58.4 %`).
         - Bottom-right Peach card (`#F5E2D6`, `100px`): Title `"Calories / BMR"`, `36px` white icon circle, bold value (`1,742 kcal`).
       - **Page 2 (Google Health Connect Daily Context):**
         - Left tall Lavender card (`#DFD5F5`, `212px`): `"Steps"`, `36px` white shoe/steps icon circle, `"8,420"`, clean wave + dot.
         - Top-right Sage card (`#DCECCB`, `100px`): `"Sleep"`, `36px` white moon icon circle, `"7h 30min"`.
         - Bottom-right Peach card (`#F5E2D6`, `100px`): `"Calories"`, `36px` white flame icon circle, `"2,149 kcal"`.
       - Clicking any bento tile navigates to its relevant deep view (`Body Studio` or `Reports` or `Sync Center`).
     - **Bottom Trend Card (`#FFFFFF`, `28px` radius):**
       - Single clean row header (`"Body Fat Trend"` + `"Weekly v"` pill dropdown) and smooth trend curve with weekday labels (`Sat..Fri`). Remove the pulsing green `Live · Synced 12s ago` pill from inside this card so it keeps the calm white look of the screenshot (keep live sync telemetry cleanly in Sync Center and the Bell notification dot).
     - **Bottom Floating Bar:** Detached `#0F1115` pill (`64px` height, `32px` radius, 4 destinations with active item inside a `46px` `#FFFFFF` circle) + detached `64px` `#2185F7` `+` FAB.
   - **Reports Screen (Screen 3):**
     - Top bar: `<-` circle, `"Reports"`, Share circle.
     - Segmented track: `Daily | Weekly (solid #111111 pill, white text) | Monthly`. Remove the cluttered horizontal chip strip between the period track and the gauge! Put a compact pill selector (`Body Fat % v` / `Weight v` / `SMM v` / `ECW/TBW v`) unobtrusively inside the Gauge card's top-right corner so the screen whitespace matches the reference screenshot.
     - **3-Zone Semi-Circular Arc Gauge Card:**
       - Fix SVG `stroke-linecap` from `butt` to **`round`** (`stroke-width: 18`, `gap: 8 deg`), with crisp colors `#2185F7` (`Normal`), `#F4B428` (`Elevated`), `#EF4444` (`High`).
       - Thumb: Crisp `20px` white-filled circle with `5px` colored border sitting on the active arc angle.
       - Center: `44px` Bold tabular readout (`18.4 %` / `74.2 kg`) + `"Overview"` subtitle.
       - Bottom legend: 3 horizontal items (`Normal (56%)`, `Elevated (28%)`, `High (16%)`) with colored dots.
     - **Stippled Dot-Matrix Bar Chart Card (`Average Body Fat` / `Average Weight`):**
       - Keep **only** the clean header + 7 rounded-top columns (`Sat..Fri`) + weekday labels. Remove the noisy 3-column metrics footer (`86% | -0.3 pts | 7`) inside this card!
       - Make the unselected stippled dot-matrix bars crisper (`2px` dark `#1E242E` dots at `0.55` alpha on a `5.5px` grid inside `38px` rounded-top bars).
       - Clicking any bar turns it **solid `#111111`** with a `6px` white dot near the top, a short vertical dashed stem above it, and a floating mint `"Normal"` / `"Optimal"` pill right above.

### B. Deep Dual-Scale Integration Across Body Studio, Live Weigh-In, and Packet Inspector

4. **8-Electrode (`Cult Smart Scale Pro`) vs 4-Electrode (`Cult Smart Scale CS-BF01`) Full Behavior:**
   - **Live Weigh-In Sheet (`+` FAB):**
     - **Step 1 (Scale Picker / Scan):** Let the user tap either **Cult Smart Scale Pro (8-Electrode · Dual-Freq · 56 Params)** or **Cult Smart Scale CS-BF01 (4-Electrode · Foot-Only · 20+ Params)** or **Pro Feet-Only (No Handle)** directly!
     - **Step 2 (`0xCF` Settling):** Streams realistic `0xCF` 11-byte frames (`CF 00 00 <wt-LE> ... <status> <xor>`) with live tabular weight readout (`74.18 kg`).
     - **Step 3 (Adaptive Electrodes):**
       - On **8E Pro (Handle Active):** All **8 pads** (4 foot + 4 handle pads) light up `#2185F7`, progress ring runs `20 kHz / 100 kHz` `0xBE` sweep, and 10 raw ohm chips appear.
       - On **4E `CS-BF01` (or Pro Feet-Only):** **Only the 4 foot pads** light up `#2185F7`, the 4 handle pads show cleanly dimmed (`"Foot-Only Scale · No Handle"`), the scale sends `FD 37` / `F1` / `F2 00` OneByone handshake + `0xCF` lock (`status 0x00`), and derives the 20+ anthropometric + sensor metrics honestly.
     - **Step 4 (Saved & Synced):** Shows `"Saved to Room"`, `"Written to Google Health Connect"`, and `"Synced to Supabase Realtime (0 pending)"`, plus optional `"2 profiles within +-2.5 kg"` ambiguity resolver toggle.
   - **Body Studio (Screen 4):**
     - Keep the minimalist anatomical 5-segment silhouette (`Left Arm`, `Right Arm`, `Trunk`, `Left Leg`, `Right Leg`) with floating callouts (`Segmental Muscle` vs `Segmental Fat`), bilateral arm/leg symmetry bars, and searchable 7-group parameter accordion (all **56 parameters**).
     - When **4-Electrode `CS-BF01`** or **Pro Feet-Only** is active:
       - Gracefully dim the 5 segmental zones on the silhouette and display a calm banner: `"Segmental 5-zone isolation requires 8-Electrode Handle (Cult Smart Scale Pro). Showing 22 verified 4-electrode & anthropometric parameters."` with a 1-tap `"Switch to 8E Pro"` preview button.
       - Inside the Parameter Explorer, keep all 22 available metrics live (labeled `Sensor` or `4E Est.`) and show `"Requires 8E"` cleanly on segmental/ICW/ECW rows so there is zero fake precision (`INV-02`).
   - **Packet Inspector (Screen 9):**
     - Dynamically stream **8E Pro frames** (`0xCF` 11B, `0xBE` Set A & B 13B x 2 -> 10 ohm cells, `0xDF` 17B x 4) when `8E Pro` is active, and **4E `CS-BF01` frames** (`TX FD 37 00 01 ...`, `TX F1 ...`, `RX F1 00`, `TX F2 00`, `RX CF 00 ... 00 <xor>`) when `CS-BF01` is active, with valid XOR checksum badges and `"Copy Capture JSON"`.

### C. Preserve All Existing Screens and Flows Cleanly
- Keep **History & Side-by-Side Comparison**, **10-Member Household Profiles (`+ Add Member` up to 10/10)**, and **Google Health Connect + Supabase Realtime Sync Center (with Backfill + JSON/CSV Export)** interactive and styled in the exact same calm Daylight Clinical Pastel / Obsidian Night design language, with `96px` bottom clearance above the floating dock on all docked tabs.
```
