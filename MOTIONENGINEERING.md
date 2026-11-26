# OpenCult Motion Engineering Specification (`MOTIONENGINEERING.md`)

> **Binding Motion Contract.** Governs every spring curve, easing curve, gesture handoff, Canvas transition, live BLE stream interpolation, infinite loop lifecycle, and reduced-motion override in OpenCult (`DRD:M-01..M-02`, `prototype/styles.css`, and native Android Jetpack Compose).

Every motion defect or dropped-frame regression discovered during testing MUST be logged in `HALL_OF_SHAME.md` and encoded as a rule here and in `INVARIANTS.md`.

---

## 1. Motion Philosophy for Clinical Telemetry

1. **Physical clarity over spectacle:** Transitions exist to preserve spatial orientation (pager direction `<` vs `>`, sheet rise from dock FAB, accordion expansion) and show sensor settling. Telemetry displays MUST NOT bounce or oscillate around a biological measurement.
2. **RenderThread / GPU ownership:** Frame-by-frame motion runs inside Compose `graphicsLayer { ... }` and `DrawScope` / `Canvas` lambdas using deferred state reads (`() -> Float`). Parent layouts never remeasure or recompose during steady-state animations.
3. **Zero runaway background motion:** Infinite animations (BLE scan radar rings, active transfer indicator) run ONLY while that exact view is foreground-resumed and actively scanning, and stop cleanly when closed, backgrounded, or reduced motion is enabled.

---

## 2. Closed Motion Token System (`OpenCultMotion`)

Define all animation specs in a single `core/designsystem` file (`OpenCultMotion.kt` mirroring CSS rules in `prototype/styles.css`). Ad-hoc inline `spring(stiffness = ..., dampingRatio = ...)` or hardcoded millisecond literals inside `feature/` screens are banned (`INV-19`).

| Token Name | Jetpack Compose Spec | Prototype CSS / JS Equivalent | Intended Use |
|---|---|---|---|
| `MotionTokens.SpatialDefault` | `spring(stiffness = 420f, dampingRatio = 0.82f)` (`DRD:M-01`) | `transition: transform .18s ease` / directional transition | Bento `<`/`>` paging, dock active circle glide, segmented pill switch (`Daily`/`Weekly`/`Monthly`), body map selection highlight |
| `MotionTokens.PressFeedback` | `spring(stiffness = 560f, dampingRatio = 0.78f)` | `transform .18s ease` on `:active` | Button touchdown/release restitution (`0.92f` chrome, `0.94f` FAB, `0.97f` cards) |
| `MotionTokens.SheetEnter` | `tween(260, easing = CubicBezierEasing(0.20f, 0.80f, 0.20f, 1.00f))` | `@keyframes sheetIn .26s cubic-bezier(.2, .8, .2, 1)` (`styles.css:1957`) | Weigh-In modal, Profile switcher sheet, Ambiguous weigh-in picker, Compare sheet |
| `MotionTokens.SheetExit` | `tween(190, easing = FastOutLinearInEasing)` | Reverse sheet exit | Dismissing sheets downward toward FAB/dock |
| `MotionTokens.ScrimFade` | `tween(200, easing = LinearOutSlowInEasing)` | `@keyframes fadeIn .2s ease-out` (`styles.css:1944`) | Modal overlay scrim (`rgba(6, 9, 14, 0.48)`) |
| `MotionTokens.StageRise` | `tween(220, easing = FastOutSlowInEasing)` + `8.dp` upward offset | `@keyframes riseIn .22s-.26s ease-out` (`styles.css:2404`) | Screen switch entry, Weigh-In stage crossfade (`1 -> 2 -> 3 -> 4`), Accordion expand |
| `MotionTokens.TelemetryGlide` | `spring(stiffness = 280f, dampingRatio = 0.90f)` (near-critical, zero overshoot) | `setInterval` step smoothing (`app.js:1311-1358`) + `ring.style.strokeDashoffset` | Live `0xCF` weight digits, settle bar progress, `0xBE` sweep ring fill, arc gauge thumb angular glide |
| `MotionTokens.ListAppend` | `tween(200, easing = FastOutSlowInEasing)` (`-4.dp -> 0.dp`) | `@keyframes frameIn .2s ease-out` (`styles.css:2406`) | New BLE raw packet row arriving at top of Inspector ledger |
| `MotionTokens.AmbientRadar` | `infiniteRepeatable(tween(2600, easing = LinearOutSlowInEasing))` staggered at `0 ms / 850 ms / 1700 ms` | `@keyframes radar 2.6s ease-out infinite` (`styles.css:2147-2150`) | Stage 1 BLE discovery radar rings ONLY while Stage 1 is visible and scanning |
| `MotionTokens.ReducedInstant` | `snap()` (or `<= 80 ms` crossfade for visibility clarity) | `duration: .001ms !important` (`styles.css:2419-2423`) | All motion paths when `ANIMATOR_DURATION_SCALE == 0f` or `prefers-reduced-motion: reduce` |

---

## 3. Interaction & Screen Motion Specifications

### A. Onboarding Device-in-Device Parallax (`DRD:C-07`)
- Driven directly by `HorizontalPagerState.currentPageOffsetFraction` via `graphicsLayer { translationX = ... }`.
- As the user drags horizontally between Onboarding Steps `1 -> 2 -> 3`, the overflowing Hero Weight Card translates slightly ahead of the miniature phone frame (`~1.12x` horizontal offset) so depth feels coupled to the finger rather than delayed until settle.
- Bottom pill step indicator morphs cleanly between inactive `6.dp` circle and active `24.dp` cobalt pill via `MotionTokens.SpatialDefault`.

### B. Home Overview Bento Pager (`DRD:C-02`)
- `<` and `>` pager buttons translate outgoing page by `-28.dp` (next) or `+28.dp` (prev) while sliding incoming page in over `MotionTokens.SpatialDefault`, preventing sudden vertical collapse by fixing bento container height (`212.dp`).
- Bezier wave points remain stable during swipe; switching household members updates the curve smoothly via interpolated point Y-positions (`animateFloatAsState`).

### C. Reports Arc Gauge & Stippled Bar Scrub (`DRD:C-03`, `DRD:C-04`)
1. **Polar Arc Thumb Glide:** Changing period (`Daily / Weekly / Monthly`), metric (`Body Fat / Weight / SMM / ECW`), or member MUST animate the scalar angle `thetaDeg` (`180f..360f`) using `MotionTokens.TelemetryGlide` and compute `(x, y)` inside `Canvas` from `(cx + r * cos(theta), cy + r * sin(theta))`. Never interpolate X and Y linearly (Cartesian shortcut cuts chords inside the arc ring instead of riding the circular track).
2. **Stippled Bar Height & Focus Transitions:** Height changes animate inside `Canvas` clip bounds (`heightFraction` Float state); bottom weekday labels stay anchored in place. Tapping another bar transitions active pill/dot highlight within `180 ms` and fades floating `Normal` / `Elevated` / `High` tooltip pill without shifting neighboring columns.

### D. Live BLE Weigh-In Choreography (`DRD:C-08`, Stages `1..4`)
- **Stage 1 (Discover):** 3 concentric radar rings (`MotionTokens.AmbientRadar`, offsets `0 / 850 / 1700 ms`) run strictly while scanning. Tapping a discovered scale (`Cult Smart Scale Pro 8E` or `CS-BF01 4E`) or `Start scan` stops radar timers immediately and enters Stage 2.
- **Stage 2 (Weight Settle `0xCF`):** Incoming unstable weight frames feed `MotionTokens.TelemetryGlide` for the settle progress bar while the primary `tnum` readout (`56.sp`) updates crisply without artificial lag that masks real scale stabilization. Wait lock (`status = 0x05` / `0x01` / `0x00`) snaps readout to verified lock weight and fires one deduplicated haptic pulse.
- **Stage 3 (Contact & Impedance Sweep `0xBE` / `FD 37`):** Foot and hand pad indicators light sequentially (`0x01 -> 0x08 -> 0x05` for `8E`; foot pads + live `FD 37 / F1 / F2 00` log for `CS-BF01`). Ring progress sweeps smoothly (`strokeDashoffset` / Compose `drawArc` sweep angle `0f..360f`), pauses `<= 320 ms` at `100%` so the user reads completion, then advances to Stage 4.
- **Stage 4 (Lock & Sync Confirm):** Shows `Saved Locally` immediately (`< 50 ms`, `INV-06`), followed by crisp badge transitions as Health Connect write and Supabase push complete, or opens the Ambiguous Profile Picker immediately when `assignment_state = 'pending_review'` (`INV-09`).

### E. Floating Dock Pill & Compare Bar (`DRD:C-05`, `DRD:S-06`)
- Tab switches inside the `#0F1115` capsule glide the `46.dp` contrasting active circle horizontally (`MotionTokens.SpatialDefault`) instead of blinking between icons.
- History multi-select comparison bar slides upward from `navigationBarsBottom` using `MotionTokens.SheetEnter` and reserves extra `LazyColumn` bottom padding dynamically so bottom history cards remain scrollable above the compare bar.

### F. Bounded Stagger Rule (Lists & 56-Parameter Accordion)
- Initial entry or group expansion staggers list items by at most `24 ms` per visible item with a hard cumulative cap of `160 ms` (`max 6-7 staggered steps`). Items beyond index `6` appear at the cap with zero incremental delay so scrolling a 56-row parameter table never lags behind user finger velocity.

---

## 4. GPU & Lifecycle Discipline (120 Hz Rule)

| Rule | Implementation Requirement | Why |
|---|---|---|
| **GPU transform-only motion** | Animate `translationX/Y`, `scaleX/Y`, `alpha`, `rotationZ` inside `Modifier.graphicsLayer { ... }` | Avoids Android `Measure`/`Layout` passes at 120 Hz |
| **Deferred Canvas reads** | Pass `State<Float>` or `() -> Float` lambdas into `Canvas { ... }` | Prevents composable scope invalidation every animation frame |
| **Cached Canvas allocations** | Wrap `Path()`, `CornerPathEffect`, `DashPathEffect`, and `Brush` inside `remember(size, colors)` | Eliminates GC pressure during `onDraw` sweeps |
| **Lifecycle-gated infinite loops** | Wrap `AmbientRadar` and active-scan indicators inside `LifecycleResumeEffect` / `repeatOnLifecycle(STARTED)` | Prevents background CPU/GPU wakeup (`SHAME` battery hazard) |
| **Static steady-state indicators** | Replace idle continuous dot pulsing (`styles.css:1009`) on Home `Sync` status with motion ONLY while a BLE scan or active network push is executing | Respects battery and avoids ambient visual distraction on a medical dashboard |
| **Timer/animation cleanup** | Clear coroutine jobs / prototype `wiTimers` on stage switch, dismiss, or screen exit | Prevents orphan callbacks firing on closed sheets (`app.js:126-129`) |

---

## 5. Reduced Motion Parity (`DRD:M-02`, `INV-20`)

OpenCult MUST honor both Android system accessibility (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f` exposed via `LocalReducedMotion`) and web media preference (`@media (prefers-reduced-motion: reduce)` in `prototype/styles.css:2418-2424`).

When reduced motion is enabled:
1. Disable `AmbientRadar` concentric ripples, infinite pulses, and directional `translationX/Y` slides.
2. Snap pager tabs, sheet presentation, arc gauge thumb angle, bar heights, and weigh-in progress ring directly (or with a minimal `<= 80 ms` crossfade that conveys state change without motion sickness).
3. Keep all functional state progression (`320 ms` Stage 3 completion settle pause, unambiguous Stage 4 lock confirmation, Health Connect status badges, and TalkBack announcements) intact.

---

## 6. Pre-Commit Motion Audit Checklist

- [ ] Every Compose animation uses a named `MotionTokens.*` spec with zero inline `spring()` / magic duration literals outside `core/designsystem` (`INV-19`).
- [ ] All transforms and fades execute via `graphicsLayer` or `Canvas` lambdas with cached `Path` / `PathEffect` objects.
- [ ] `DRD:C-03` Arc Gauge thumb glides along polar radius coordinates (`angleDeg`), never cutting linear X/Y chords.
- [ ] `0xCF` weight digits and impedance bars use critically/near-critically damped `TelemetryGlide` (`dampingRatio >= 0.90f`) with zero numerical bounce overshoot.
- [ ] BLE lock haptics fire at most once per physical weigh-in lock edge, ignoring coalesced duplicate frames (`INV-21`).
- [ ] No infinite `rememberInfiniteTransition` or pulse loop stays active when the scan sheet closes, enters Stage 2+, or moves to background.
- [ ] Enabling `Animator duration scale = Off` (`0x`) removes all translations/ripples cleanly with zero broken states or frozen overlays (`INV-20`).

---

## 7. Permanently Banned Motion Anti-Patterns

| Banned Motion Pattern | Failure Mode | Required Replacement |
|---|---|---|
| Underdamped bounce (`dampingRatio < 0.72f`) on weights, fat %, or arc thumb | Makes clinical telemetry numbers look erratic and untrustworthy | `MotionTokens.TelemetryGlide` (`dampingRatio = 0.90f`) |
| Linear `(x, y)` interpolation on the curved `DRD:C-03` arc gauge | Thumb slices straight across the white card center instead of riding the arc | Interpolate `angleDeg`, then compute polar `(cx + r cos, cy + r sin)` |
| Ungated infinite `.pulse` / `rememberInfiniteTransition` running 24/7 | Drains battery and distracts user while screen is idle | Gate infinite loops to active BLE scanning (`Stage 1`) + `STARTED` lifecycle + `!reducedMotion` |
| Animating `Modifier.height()` / `Modifier.padding()` inside 60/120 Hz ticks | Triggers full Compose layout pass per frame and drops frames on mid-range phones | `graphicsLayer { scaleY / translationY }` or `Canvas` `drawRect` height |
| Allocating `Path()`, `Paint()`, or `DashPathEffect` per frame inside `DrawScope` | Causes GC jank during scroll or live weigh-in sweep | `remember(size)` cached drawing primitives |
| Repeating stagger delays linearly over 56 parameter rows | Bottom rows arrive `> 1 s` late after list open | Cap stagger at `160 ms` total (`max 6-7 items`) |
