/* ============================================================
   OpenCult prototype behaviour
   ============================================================ */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const clamp = (n, a, b) => Math.min(b, Math.max(a, n));

  const store = {
    get(key, fallback) {
      try {
        const raw = localStorage.getItem(key);
        return raw === null ? fallback : JSON.parse(raw);
      } catch (e) { return fallback; }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* preview may block storage */ }
    }
  };

  const members = [
    { name: 'Wyatt Anderson', rel: 'You', sex: 'Male', age: 32, h: 181, target: 72, weight: 74.2, delta: -0.4, fat: 18.4, fatDelta: -0.3, smm: 31.8, smmDelta: 0.2, water: 43.3, bmr: 1742, visc: 5, ini: 'WA', color: '#2E6BE6' },
    { name: 'Nora Anderson', rel: 'Partner', sex: 'Female', age: 30, h: 168, target: 60, weight: 61.9, delta: 0.2, fat: 24.1, fatDelta: 0.1, smm: 24.6, smmDelta: 0.1, water: 35.2, bmr: 1388, visc: 3, ini: 'NA', color: '#8C63D9' },
    { name: 'Elias Anderson', rel: 'Son', sex: 'Male', age: 9, h: 136, target: 30, weight: 31.6, delta: 0.3, fat: 17.2, fatDelta: 0.0, smm: 14.2, smmDelta: 0.1, water: 20.4, bmr: 1010, visc: 2, ini: 'EA', color: '#3FA97A' },
    { name: 'Maren Anderson', rel: 'Daughter', sex: 'Female', age: 12, h: 152, target: 42, weight: 43.4, delta: 0.1, fat: 21.8, fatDelta: -0.1, smm: 18.1, smmDelta: 0.1, water: 27.6, bmr: 1150, visc: 2, ini: 'MA', color: '#E1843B' },
    { name: 'Theo Anderson', rel: 'Father', sex: 'Male', age: 64, h: 175, target: 74, weight: 78.6, delta: 0.4, fat: 26.3, fatDelta: 0.2, smm: 29.4, smmDelta: 0.0, water: 41.8, bmr: 1640, visc: 9, ini: 'TA', color: '#6B7BA8' },
    { name: 'June Anderson', rel: 'Mother', sex: 'Female', age: 61, h: 162, target: 64, weight: 66.3, delta: -0.2, fat: 29.8, fatDelta: -0.1, smm: 23.8, smmDelta: 0.0, water: 35.6, bmr: 1320, visc: 7, ini: 'JA', color: '#C2658C' },
    { name: 'Caleb Foster', rel: 'Friend', sex: 'Male', age: 35, h: 178, target: 77, weight: 80.1, delta: 0.0, fat: 19.6, fatDelta: 0.0, smm: 34.2, smmDelta: 0.1, water: 45.9, bmr: 1815, visc: 6, ini: 'CF', color: '#3C9AA8' },
    { name: 'Priya Raman', rel: 'Friend', sex: 'Female', age: 29, h: 164, target: 56, weight: 57.8, delta: -0.3, fat: 22.4, fatDelta: -0.2, smm: 24.0, smmDelta: 0.1, water: 34.9, bmr: 1345, visc: 3, ini: 'PR', color: '#7E5BEF' },
    { name: 'Owen Hale', rel: 'Gym buddy', sex: 'Male', age: 41, h: 186, target: 84, weight: 87.9, delta: 0.5, fat: 16.8, fatDelta: 0.1, smm: 39.6, smmDelta: 0.2, water: 50.4, bmr: 1960, visc: 4, ini: 'OH', color: '#5B8C4A' }
  ];

  const SCALES = {
    pro: {
      key: 'pro', code: '8E', badge: '8E Pro',
      name: 'Cult Smart Scale Pro', short: 'Scale Pro',
      elect: '8-electrode', frames: '8E',
      tagline: '8-electrode · dual-frequency · 56 params'
    },
    bf01: {
      key: 'bf01', code: '4E', badge: 'CS-BF01 4E',
      name: 'Cult Smart Body Fat Scale', short: 'CS-BF01',
      elect: '4-electrode', frames: '4E',
      tagline: 'CS-BF01 · 4-electrode · 20+ params'
    },
    proFeet: {
      key: 'proFeet', code: '4E', badge: 'Pro Feet-Only',
      name: 'Cult Smart Scale Pro', short: 'Pro Feet-Only',
      elect: '4-electrode', frames: '4E',
      tagline: '8E hardware · feet-only · 20+ params'
    }
  };

  const initialTheme = (() => {
    const fromMarkup = document.documentElement.dataset.theme;
    if (fromMarkup === 'dark' || fromMarkup === 'light') return fromMarkup;
    const saved = store.get('oc-theme', 'light');
    return saved === 'dark' ? 'dark' : 'light';
  })();

  const state = {
    theme: initialTheme,
    view: 'gallery',
    scale: 'pro',
    step: 0,
    metric: 'weight',
    page: 1,
    active: 0,
    period: 'weekly',
    reportMetric: 'fat',
    bar: 3,
    mode: 'muscle',
    zone: 'trunk',
    search: '',
    open: { core: true },
    picked: [],
    wiStage: 1,
    wiAmbiguity: false,
    wiMember: null,
    wiWeight: 74.18,
    capture: true,
    frames: [],
    hcOut: { Weight: true, 'Body Fat': true, 'Lean Mass': true, 'Bone Mass': true, 'Body Water': true, BMR: true, 'Heart Rate': false },
    hcIn: { Steps: true, Sleep: true, Calories: true }
  };

  const el = {};
  const ids = ['phoneContent', 'dock', 'fab', 'obBack', 'obSkip', 'obTitle', 'obSub', 'obDots', 'obNext', 'obNextLabel', 'obMemberMini',
    'avatarBtn', 'avatarIni', 'greetLine', 'greetName', 'notifyBtn', 'drawerBtn', 'heroMetric', 'heroMetricLabel', 'heroValue', 'heroUnit', 'heroDelta',
    'heroBadge', 'heroSpark', 'pagePrev', 'pageNext', 'bentoWrap', 'waveMuscle', 'bentoSmm', 'bentoSmmSub', 'bentoWater', 'bentoWaterSub',
    'bentoBmr', 'bentoVisc', 'bentoSmmTitle', 'waveSteps', 'trendRange', 'trendVal', 'trendWave', 'reportShare', 'reportPeriod', 'reportMetrics', 'gaugeSvg',
    'gaugeVal', 'gaugeUnit', 'gaugeLegend', 'gaugeCap', 'chartTitle', 'chartSub', 'chartBadge', 'dmChart', 'segmentMode',
    'zoneDetail', 'zdValue', 'zdBadge', 'zoneBadge', 'balArm', 'balArmBar', 'balLeg', 'balLegBar', 'almVal', 'almBadge',
    'paramCount', 'paramSearch', 'paramGroups', 'paramEmpty', 'paramBadge', 'profileGrid', 'profileCount',
    'histCount', 'historyList', 'compareCount', 'compareLabel', 'compareBtn', 'hcOut', 'hcIn', 'backfillBtn', 'backfillNote', 'exportJson',
    'exportCsv', 'framePlay', 'captureState', 'frameList', 'frameStats', 'copyCapture', 'ohmGrid', 'ohmTitle', 'ohmSub', 'ohmBadge', 'consoleDev', 'memberList', 'wiSteps', 'wiWeight',
    'wiSettleBar', 'wiTicker', 'wiSettleNote', 'wiSettleSub', 'padFoot', 'padHandle', 'padHandleGroup', 'wiRing', 'wiRingVal', 'wiOhms', 'wiFinal', 'wiFinalSub',
    'wiPickWrap', 'wiPickList', 'wiAmbiguity', 'wiAction', 'wiEyebrow', 'wiBfBadge', 'wiProBadge', 'wiBfStrong', 'wiContactLead', 'wiContactSub', 'wiHandshake', 'wiHealthSub',
    'bodyScaleBanner', 'bodyScaleNote', 'bodySwitchScale', 'bodyScaleTag', 'bodyMapSub', 'bodyMapNote', 'reportMetricLabel',
    'scaleMode', 'studioTheme', 'studioThemeLabel', 'viewMode', 'screenRail', 'gallery', 'galleryGrid',
    'compareBody', 'amName', 'amAge', 'amSex', 'amHeight', 'amTarget', 'amSave',
    'errName', 'errAge', 'errHeight', 'errTarget', 'toast'];
  ids.forEach(id => { el[id] = document.getElementById(id); });

  /* ---------------- helpers ---------------- */
  const esc = (s) => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const signed = (v, dec = 1) => `${v > 0 ? '+' : ''}${v.toFixed(dec)}`;
  const commas = (v) => Number(v).toLocaleString('en-US');
  const currentScale = () => SCALES[state.scale] || SCALES.pro;
  const is8E = () => currentScale().code === '8E';

  let toastTimer = null;
  function toast(msg) {
    const t = el.toast;
    t.textContent = msg;
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.hidden = true; }, 2400);
  }

  /* ---------------- overlays ---------------- */
  let lastTrigger = null;
  let wiTimers = [];
  function clearWiTimers() {
    wiTimers.forEach(t => clearInterval(t));
    wiTimers = [];
  }

  function openOverlay(id) {
    const ov = document.getElementById(id);
    if (!ov) return;
    lastTrigger = document.activeElement;
    ov.hidden = false;
    const focusable = ov.querySelector('input, select, button:not([disabled])');
    if (focusable) setTimeout(() => { try { focusable.focus({ preventScroll: true }); } catch (e) {} }, 70);
  }

  function closeOverlay(id) {
    const ov = document.getElementById(id);
    if (!ov) return;
    ov.hidden = true;
    if (id === 'ovWeighin') clearWiTimers();
    if (lastTrigger && document.contains(lastTrigger)) {
      try { lastTrigger.focus({ preventScroll: true }); } catch (e) {}
    }
  }

  function trapFocus(e, ov) {
    const nodes = $$('button:not([disabled]), input, select, [tabindex]:not([tabindex="-1"])', ov)
      .filter(n => n.offsetParent !== null);
    if (!nodes.length) return;
    const first = nodes[0];
    const last = nodes[nodes.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  }

  /* ---------------- navigation ---------------- */
  const DOCKED = ['home', 'body', 'reports', 'profiles'];

  function showScreen(name) {
    state.screen = name;
    $$('.screen').forEach(s => { s.hidden = s.dataset.screen !== name; });
    if (el.phoneContent) el.phoneContent.scrollTop = 0;
    const docked = DOCKED.includes(name);
    el.dock.hidden = !docked;
    $$('.dock-btn').forEach(b => {
      const on = b.dataset.nav === name;
      b.classList.toggle('is-active', on);
      if (on) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    if (name === 'home') renderHome();
    if (name === 'reports') renderReports();
    if (name === 'body') renderBody();
    if (name === 'profiles') renderProfiles();
    if (name === 'history') renderHistory();
    if (name === 'sync') renderSync();
    if (name === 'inspector') renderFrames();
    $$('#screenRail [data-jump]').forEach(b => {
      const on = b.dataset.jump === name;
      b.classList.toggle('is-on', on);
      if (on) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    $$('.gallery-card').forEach(c => c.classList.toggle('is-current', c.dataset.screen === name));
  }

  function go(name) { showScreen(name); }

  /* ---------------- theme ---------------- */
  function applyTheme() {
    document.documentElement.dataset.theme = state.theme;
    const dark = state.theme === 'dark';
    $$('.theme-glyph use').forEach(u => u.setAttribute('href', dark ? '#i-sun' : '#i-moon'));
    $$('.theme-btn').forEach(b => b.setAttribute('aria-label', dark ? 'Switch to Daylight theme' : 'Switch to Obsidian Night theme'));
    const label = document.getElementById('themeLabel');
    if (label) label.textContent = dark ? 'Obsidian Night' : 'Daylight Clinical Pastel';
    const row = document.getElementById('themeRow');
    if (row) row.setAttribute('aria-label', dark ? 'Switch to Daylight theme' : 'Switch to Obsidian Night theme');
    if (el.studioThemeLabel) el.studioThemeLabel.textContent = dark ? 'Obsidian' : 'Daylight';
    if (el.studioTheme) el.studioTheme.setAttribute('aria-label', dark ? 'Switch to Daylight theme' : 'Switch to Obsidian Night theme');
  }

  function toggleTheme() {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    store.set('oc-theme', state.theme);
    applyTheme();
    toast(state.theme === 'dark' ? 'Obsidian Night enabled' : 'Daylight Clinical Pastel enabled');
  }

  /* ---------------- connected scale ---------------- */
  function applyScaleUI() {
    const s = currentScale();
    const eight = is8E();
    if (el.scaleMode) el.scaleMode.value = state.scale;
    if (el.bodyScaleTag) el.bodyScaleTag.textContent = s.badge;
    if (el.bodyScaleBanner) el.bodyScaleBanner.hidden = eight;
    if (el.bodyScaleNote) el.bodyScaleNote.textContent = s.key === 'proFeet'
      ? 'Pro Feet-Only estimates 20+ parameters'
      : '4E estimates 20+ parameters';
    if (el.bodyMapSub) el.bodyMapSub.textContent = eight ? '8-electrode · dual frequency' : '4-electrode · foot-to-foot';
    if (el.bodyMapNote) el.bodyMapNote.hidden = eight;
    if (el.paramBadge) el.paramBadge.textContent = s.code;
    if (el.wiEyebrow) el.wiEyebrow.textContent = s.name;
    if (el.wiBfBadge) {
      el.wiBfBadge.textContent = eight ? 'Nearby' : 'Selected';
      el.wiBfBadge.className = eight ? 'badge soft' : 'badge mint';
    }
    if (el.wiProBadge) {
      el.wiProBadge.textContent = eight ? 'Selected' : 'Nearby';
      el.wiProBadge.className = eight ? 'badge mint' : 'badge soft';
    }
    if (el.wiBfStrong) el.wiBfStrong.textContent = state.scale === 'proFeet' ? 'Pro Feet-Only' : 'CS-BF01';
    if (el.wiSettleSub) el.wiSettleSub.textContent = eight
      ? 'Weight settling · 0xCF stream'
      : 'Weight settling · FD 37 handshake';
    if (el.wiContactLead) el.wiContactLead.textContent = eight
      ? '8-electrode contact active'
      : '4-electrode foot contact active';
    if (el.wiContactSub) el.wiContactSub.textContent = eight
      ? 'Dual-frequency sweep 0xBE · 20 kHz / 100 kHz'
      : 'Single-frequency foot loop · arms and legs estimated';
    if (el.padHandleGroup) el.padHandleGroup.hidden = !eight;
    if (el.consoleDev) el.consoleDev.textContent = eight ? 'CULT Pro · 0xCF' : 'CULT BF01 · 0xFD';
    if (el.ohmTitle) el.ohmTitle.textContent = eight ? '10-channel impedance' : '4-channel impedance';
    if (el.ohmSub) el.ohmSub.textContent = eight ? '0xBE Set A · Set B · ohm' : '0xBE foot loop · ohm';
    if (el.ohmBadge) el.ohmBadge.textContent = eight ? 'Stable' : '4E';
    if (el.wiHandshake && eight) el.wiHandshake.hidden = true;
    $$('#ovWeighin [data-wiscale]').forEach(b => {
      const on = b.dataset.wiscale === (s.code === '8E' ? 'pro' : 'bf01');
      b.classList.toggle('is-on', on);
      b.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
  }

  function setScale(key, opts = {}) {
    if (!SCALES[key]) return;
    state.scale = key;
    state.frames = [];
    frameSeq = 0;
    for (let i = 0; i < 8; i++) state.frames.push(makeFrame());
    applyScaleUI();
    renderHome();
    if (state.screen === 'body') renderBody();
    renderReports();
    renderParams();
    renderOhms();
    renderFrames();
    buildGallery();
    if (!opts.silent) toast(`${currentScale().name} connected · ${currentScale().elect}`);
  }

  /* ---------------- view mode + gallery ---------------- */
  function setView(mode) {
    state.view = mode === 'phone' ? 'phone' : 'gallery';
    document.body.dataset.view = state.view;
    const shell = document.getElementById('phoneShell');
    if (shell) shell.hidden = state.view !== 'phone';
    if (el.gallery) el.gallery.hidden = state.view !== 'gallery';
    if (el.screenRail) el.screenRail.hidden = state.view !== 'phone';
    if (state.view === 'gallery') buildGallery();
    $$('#viewMode [data-viewmode]').forEach(b => {
      const on = b.dataset.viewmode === state.view;
      b.classList.toggle('is-on', on);
      b.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
  }

  const GALLERY = [
    { screen: 'onboarding', name: 'Onboarding', sub: '3-step intro' },
    { screen: 'home', name: 'Home', sub: 'Hero · bento' },
    { screen: 'reports', name: 'Reports', sub: 'Gauge · trend' },
    { screen: 'body', name: 'Body Studio', sub: '5 segments · 56 params' },
    { screen: 'history', name: 'History', sub: 'Compare weigh-ins' },
    { screen: 'profiles', name: 'Profiles', sub: 'Household of 10' },
    { screen: 'sync', name: 'Sync Center', sub: 'Health Connect' },
    { screen: 'inspector', name: 'Inspector', sub: 'Raw BLE frames' },
    { screen: 'weighin', name: 'Live weigh-in', sub: '4-stage BIA flow' }
  ];

  function stripIds(node) {
    if (node.removeAttribute) node.removeAttribute('id');
    if (node.querySelectorAll) $$('[id]', node).forEach(n => n.removeAttribute('id'));
    $$('[tabindex]', node).forEach(n => n.setAttribute('tabindex', '-1'));
    return node;
  }

  function buildGallery() {
    if (!el.galleryGrid) return;
    const shell = document.getElementById('phoneShell');
    if (!shell) return;
    el.galleryGrid.innerHTML = GALLERY.map(item => {
      const clone = stripIds(shell.cloneNode(true));
      clone.hidden = false;
      $$('.screen', clone).forEach(s => { s.hidden = s.dataset.screen !== (item.screen === 'weighin' ? 'home' : item.screen); });
      const docked = ['home', 'body', 'reports', 'profiles'].includes(item.screen);
      const dock = $('.dock', clone);
      if (dock) dock.hidden = !docked;
      $$('.overlay', clone).forEach(o => { o.hidden = o.id !== 'ovWeighin' || item.screen !== 'weighin'; });
      if (item.screen === 'weighin') {
        $$('.wi-stage', clone).forEach(s => { s.hidden = s.dataset.wstage !== '1'; });
        const action = $('.wi-foot .btn', clone);
        if (action) action.textContent = 'Start scan';
      }
      return `<div class="gallery-card ${state.screen === item.screen ? 'is-current' : ''}" data-screen="${item.screen}" role="button" tabindex="0" aria-label="Open ${item.name} prototype">
        <div class="gallery-device" aria-hidden="true" inert>${clone.outerHTML}</div>
        <div class="gallery-cap"><strong>${item.name}</strong><span>${item.sub}</span></div>
      </div>`;
    }).join('');
  }

  function activateCard(screen) {
    setView('phone');
    if (screen === 'weighin') {
      showScreen('home');
      resetWi();
      openOverlay('ovWeighin');
      return;
    }
    showScreen(screen);
  }

  /* ---------------- onboarding ---------------- */
  const OB_STEPS = [
    {
      title: 'All Your Body Composition Insights in One Place',
      sub: 'Connect 4-electrode and 8-electrode Cult scales, log 56 metrics locally, and auto-sync to Google Health Connect.',
      next: 'Next'
    },
    {
      title: 'Eight Electrodes. Fifty-Six Signals.',
      sub: 'Hold the handle while you stand. Dual-frequency BIA separates arms, trunk and legs into five measured segments.',
      next: 'Next'
    },
    {
      title: 'One Scale. Ten People.',
      sub: 'Every household member gets a profile, a ±2.5 kg recognition window and a live Supabase row that updates without refresh.',
      next: 'Get started'
    }
  ];

  function renderOnboarding() {
    const step = OB_STEPS[state.step];
    el.obTitle.textContent = step.title;
    el.obSub.textContent = step.sub;
    el.obNextLabel.textContent = step.next;
    el.obBack.disabled = state.step === 0;
    $$('.ob-panel').forEach(p => { p.hidden = Number(p.dataset.obpanel) !== state.step; });
    $$('#obDots span').forEach((d, i) => d.classList.toggle('on', i === state.step));
    el.obDots.setAttribute('aria-label', `Step ${state.step + 1} of 3`);
  }

  function fillOnboardingMembers() {
    el.obMemberMini.innerHTML = members.slice(0, 10).map((m, i) =>
      `<span class="mm ${i === state.active ? 'is-on' : ''}" style="background:${m.color}">${esc(m.ini)}</span>`
    ).join('');
  }

  /* ---------------- home ---------------- */
  function activeMember() { return members[state.active]; }

  const SPARK_SERIES = {
    weight: [75.1, 75.0, 74.9, 74.8, 74.6, 74.7, 74.5, 74.3, 74.2],
    fat: [19.3, 19.1, 19.0, 18.9, 19.1, 18.7, 18.6, 18.5, 18.4],
    smm: [30.9, 31.1, 31.0, 31.3, 31.2, 31.5, 31.4, 31.7, 31.8]
  };

  function heroData() {
    const m = activeMember();
    const map = {
      weight: { value: m.weight, unit: 'kg', delta: `${signed(m.delta)} kg vs Tuesday`, dec: 1 },
      fat: { value: m.fat, unit: '%', delta: `${signed(m.fatDelta)} pts vs Tuesday`, dec: 1 },
      smm: { value: m.smm, unit: 'kg', delta: `${signed(m.smmDelta || 0)} kg vs Tuesday`, dec: 1 }
    };
    return map[state.metric];
  }

  function heroStatus() {
    const m = activeMember();
    if (state.metric === 'fat') {
      if (m.fat < 20) return { label: 'Optimal', cls: 'mint' };
      if (m.fat < 26) return { label: 'Normal', cls: 'soft' };
      return { label: 'Elevated', cls: 'warn' };
    }
    if (state.metric === 'weight') {
      return Math.abs(m.weight - m.target) <= 3 ? { label: 'Optimal', cls: 'mint' } : { label: 'Normal', cls: 'soft' };
    }
    return m.smm / m.weight >= .4 ? { label: 'Optimal', cls: 'mint' } : { label: 'Normal', cls: 'soft' };
  }

  function renderSpark() {
    const series = SPARK_SERIES[state.metric];
    const m = activeMember();
    const shift = state.metric === 'weight' ? m.weight - 74.2 : state.metric === 'fat' ? m.fat - 18.4 : m.smm - 31.8;
    const vals = series.map(v => v + shift);
    const min = Math.min(...vals);
    const max = Math.max(...vals);
    const peak = vals.indexOf(max);
    el.heroSpark.innerHTML = vals.map((v, i) => {
      const h = 26 + ((v - min) / (max - min || 1)) * 66;
      return `<i class="${i === peak ? 'spike' : ''}" style="height:${h.toFixed(1)}%"></i>`;
    }).join('');
  }

  const METRIC_LABEL = { weight: 'Weight', fat: 'Body Fat', smm: 'Skeletal Muscle' };

  function renderHero() {
    const d = heroData();
    const status = heroStatus();
    el.heroValue.textContent = d.value.toFixed(d.dec);
    el.heroUnit.textContent = d.unit;
    el.heroDelta.textContent = `${currentScale().badge} · ${d.delta}`;
    el.heroBadge.textContent = status.label;
    el.heroBadge.className = `badge ${status.cls}`;
    if (el.heroMetricLabel) el.heroMetricLabel.textContent = METRIC_LABEL[state.metric];
    if (el.heroMetric) el.heroMetric.setAttribute('aria-label', `Hero metric ${METRIC_LABEL[state.metric]}. Change metric`);
    renderSpark();
  }

  function renderHeader() {
    const m = activeMember();
    el.avatarIni.textContent = m.ini;
    el.avatarBtn.style.background = m.color;
    el.avatarBtn.setAttribute('aria-label', `Switch household profile — currently ${m.name}`);
    el.greetName.textContent = m.name;
    el.greetLine.textContent = state.metric === 'weight' ? 'Good Morning' : 'Good Morning';
  }

  function renderBento() {
    const m = activeMember();
    const smmPct = (m.smm / m.weight) * 100;
    const waterPct = (m.water / m.weight) * 100;
    el.bentoSmm.textContent = smmPct.toFixed(1);
    if (el.bentoSmmTitle) el.bentoSmmTitle.textContent = is8E() ? 'Skeletal Muscle' : 'Muscle (4E est.)';
    el.bentoSmmSub.textContent = `${m.smm.toFixed(1)} kg · Janssen ${currentScale().code}`;
    el.bentoWater.textContent = waterPct.toFixed(1);
    el.bentoWaterSub.textContent = `ICW ${(m.water * .62).toFixed(1)} L · ECW ${(m.water * .38).toFixed(1)} L`;
    el.bentoBmr.textContent = commas(m.bmr);
    el.bentoVisc.textContent = `Visceral Level ${m.visc}`;
  }

  function drawWave(svg, values, key, opts = {}) {
    if (!svg) return;
    const vb = svg.viewBox.baseVal;
    const w = vb && vb.width ? vb.width : 170;
    const h = vb && vb.height ? vb.height : 64;
    const pad = 6;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const pts = values.map((v, i) => {
      const x = (i / (values.length - 1)) * w;
      const y = h - pad - ((v - min) / (max - min || 1)) * (h - pad * 2 - 6);
      return [x, y];
    });
    let d = `M${pts[0][0].toFixed(1)} ${pts[0][1].toFixed(1)}`;
    for (let i = 1; i < pts.length; i++) {
      const p0 = pts[i - 1];
      const p1 = pts[i];
      const cx = ((p0[0] + p1[0]) / 2).toFixed(1);
      d += ` C${cx} ${p0[1].toFixed(1)}, ${cx} ${p1[1].toFixed(1)}, ${p1[0].toFixed(1)} ${p1[1].toFixed(1)}`;
    }
    const last = pts[pts.length - 1];
    const gid = `wg-${key}`;
    const opacity = opts.opacity !== undefined ? opts.opacity : .26;
    const width = opts.width || 2.2;
    svg.innerHTML = `
      <defs>
        <linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="currentColor" stop-opacity="${opacity}"/>
          <stop offset="1" stop-color="currentColor" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <path d="${d} L${w} ${h} L0 ${h} Z" fill="url(#${gid})" stroke="none"/>
      <path d="${d}" fill="none" stroke="currentColor" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="${last[0].toFixed(1)}" cy="${last[1].toFixed(1)}" r="4.2" fill="var(--surface)" stroke="currentColor" stroke-width="2.2"/>
    `;
  }

  function renderWaves() {
    const m = activeMember();
    const smmBase = [29.8, 30.2, 30.5, 30.9, 31.1, 31.3, 31.5, 31.6, 31.8];
    drawWave(el.waveMuscle, smmBase.map(v => v + (m.smm - 31.8)), 'muscle');
    drawWave(el.waveSteps, [4100, 5200, 4800, 6100, 7300, 6900, 8420], 'steps');
    renderTrendWave();
  }

  function trendSeries() {
    const m = activeMember();
    const shift = m.fat - 18.4;
    if (el.trendRange.value === 'Monthly') {
      return [19.6, 19.3, 19.0, 18.9, 18.7, 18.6, 18.4].map(v => v + shift);
    }
    return [19.2, 19.0, 18.9, 18.7, 18.8, 18.6, 18.4].map(v => v + shift);
  }

  function renderTrendWave() {
    el.trendVal.textContent = activeMember().fat.toFixed(1);
    drawWave(el.trendWave, trendSeries(), 'trend', { opacity: .18, width: 2 });
    const labels = el.trendRange.value === 'Monthly'
      ? ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'Now']
      : ['Sat', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
    $$('.trend-days span').forEach((s, i) => { s.textContent = labels[i]; });
  }

  function renderHome() {
    renderHeader();
    renderHero();
    renderBento();
    renderWaves();
    setPage(state.page);
  }

  function setPage(page) {
    state.page = page === 2 ? 2 : 1;
    $$('#bentoWrap .bento-page').forEach(p => { p.hidden = Number(p.dataset.bentopage) !== state.page; });
    if (el.pagePrev) el.pagePrev.disabled = state.page === 1;
    if (el.pageNext) el.pageNext.disabled = state.page === 2;
  }

  function setActiveMember(idx) {
    if (!members[idx]) return;
    state.active = idx;
    renderHome();
    renderReports();
    renderProfiles();
    renderMemberList();
    fillOnboardingMembers();
    buildGallery();
    toast(`${members[idx].name} is now the active profile`);
  }

  /* ---------------- reports ---------------- */
  const REPORTS = {
    fat: {
      label: 'Average Body Fat', unit: '%', dec: 1, current: 18.4, deltaUnit: 'pts',
      zones: [4, 12, 22, 40], opt: [16, 18.5], legend: [20, 56, 24],
      daily: { labels: ['6a', '9a', '12p', '3p', '6p', '9p', '12a'], values: [19.2, 18.9, 18.8, 18.6, 18.7, 18.5, 18.4] },
      weekly: { labels: ['Sat', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri'], values: [18.9, 19.1, 18.8, 18.6, 18.7, 18.5, 18.4] },
      monthly: { labels: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'Now'], values: [19.6, 19.3, 19.0, 18.8, 18.6, 18.5, 18.4] }
    },
    weight: {
      label: 'Average Weight', unit: 'kg', dec: 1, current: 74.2, deltaUnit: 'kg',
      zones: [50, 66, 80, 100], opt: [68, 74.5], legend: [24, 52, 24],
      daily: { labels: ['6a', '9a', '12p', '3p', '6p', '9p', '12a'], values: [74.8, 74.9, 74.6, 74.5, 74.4, 74.3, 74.2] },
      weekly: { labels: ['Sat', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri'], values: [75.3, 75.4, 75.1, 75.0, 74.9, 74.6, 74.2] },
      monthly: { labels: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'Now'], values: [76.4, 76.1, 75.8, 75.5, 75.2, 74.8, 74.2] }
    },
    smm: {
      label: 'Skeletal Muscle', unit: 'kg', dec: 1, current: 31.8, deltaUnit: 'kg',
      zones: [20, 28, 38, 50], opt: [30, 33], legend: [16, 58, 26],
      daily: { labels: ['6a', '9a', '12p', '3p', '6p', '9p', '12a'], values: [31.2, 31.3, 31.4, 31.5, 31.6, 31.7, 31.8] },
      weekly: { labels: ['Sat', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri'], values: [31.2, 31.3, 31.4, 31.3, 31.5, 31.6, 31.8] },
      monthly: { labels: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'Now'], values: [30.6, 30.9, 31.1, 31.2, 31.4, 31.6, 31.8] }
    },
    ecw: {
      label: 'ECW/TBW Ratio', unit: '', dec: 3, current: 0.381, deltaUnit: '',
      zones: [0.30, 0.37, 0.40, 0.45], opt: [0.37, 0.39], legend: [18, 64, 18],
      daily: { labels: ['6a', '9a', '12p', '3p', '6p', '9p', '12a'], values: [0.389, 0.388, 0.387, 0.385, 0.384, 0.383, 0.381] },
      weekly: { labels: ['Sat', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri'], values: [0.389, 0.388, 0.386, 0.385, 0.384, 0.382, 0.381] },
      monthly: { labels: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'Now'], values: [0.392, 0.390, 0.388, 0.387, 0.385, 0.383, 0.381] }
    }
  };
  const ZONE_COLORS = ['#3B82F6', '#34C77B', '#F4B428'];
  const ZONE_NAMES = ['Low', 'Normal', 'High'];

  function reportShift(metric) {
    const m = activeMember();
    if (metric === 'fat') return m.fat - 18.4;
    if (metric === 'weight') return m.weight - 74.2;
    if (metric === 'smm') return m.smm - 31.8;
    return 0;
  }

  function gaugeAngle(v, zones, gap) {
    const total = zones[3] - zones[0];
    const usable = 180 - gap * 2;
    let a = 180;
    for (let i = 0; i < 3; i++) {
      const lo = zones[i];
      const hi = zones[i + 1];
      const span = ((hi - lo) / total) * usable;
      if (v <= hi || i === 2) {
        const t = clamp((v - lo) / (hi - lo || 1), 0, 1);
        return a + t * span;
      }
      a += span + gap;
    }
    return 180 + usable + gap * 2;
  }

  function polar(cx, cy, r, deg) {
    const rad = deg * Math.PI / 180;
    return [cx + r * Math.cos(rad), cy + r * Math.sin(rad)];
  }

  function renderGauge() {
    const cfg = REPORTS[state.reportMetric];
    const current = cfg.current + reportShift(state.reportMetric);
    const gap = 8;
    const total = cfg.zones[3] - cfg.zones[0];
    const usable = 180 - gap * 2;
    const cx = 120, cy = 120, r = 86;
    let a = 180;
    let paths = '';
    for (let i = 0; i < 3; i++) {
      const lo = cfg.zones[i], hi = cfg.zones[i + 1];
      const span = ((hi - lo) / total) * usable;
      const [x1, y1] = polar(cx, cy, r, a);
      const [x2, y2] = polar(cx, cy, r, a + span);
      paths += `<path d="M${x1.toFixed(1)} ${y1.toFixed(1)} A${r} ${r} 0 0 1 ${x2.toFixed(1)} ${y2.toFixed(1)}" fill="none" stroke="${ZONE_COLORS[i]}" stroke-width="18" stroke-linecap="round"/>`;
      a += span + gap;
    }
    const angle = gaugeAngle(current, cfg.zones, gap);
    const [tx, ty] = polar(cx, cy, r, angle);
    const zoneIdx = current >= cfg.zones[2] ? 2 : current >= cfg.zones[1] ? 1 : 0;
    const thumbColor = ZONE_COLORS[zoneIdx];
    el.gaugeSvg.innerHTML = `${paths}
      <circle cx="${tx.toFixed(1)}" cy="${ty.toFixed(1)}" r="9" fill="var(--surface)" stroke="${thumbColor}" stroke-width="4"/>`;
    el.gaugeSvg.setAttribute('aria-label', `${cfg.label} ${current.toFixed(cfg.dec)} ${cfg.unit || ''} — ${ZONE_NAMES[zoneIdx]} zone`);
    el.gaugeVal.textContent = current.toFixed(cfg.dec);
    el.gaugeUnit.textContent = cfg.unit;
    if (el.gaugeCap) el.gaugeCap.textContent = `${cfg.label.replace('Average ', '')} · ${statusOf(state.reportMetric, current)}`;
    el.gaugeLegend.innerHTML = ZONE_NAMES.map((n, i) =>
      `<span class="lg"><i style="background:${ZONE_COLORS[i]}"></i>${n} <b>${cfg.legend[i]}%</b></span>`
    ).join('');
  }

  function statusOf(metric, value) {
    const cfg = REPORTS[metric];
    if (value >= cfg.zones[2]) return 'High';
    const [lo, hi] = cfg.opt;
    const optimal = value >= lo && value <= hi ? 'Optimal' : null;
    if (value >= cfg.zones[1]) return optimal || 'Normal';
    return optimal || 'Low';
  }

  function tipClass(status) {
    if (status === 'High') return 'is-alert';
    if (status === 'Low') return 'is-warn';
    return '';
  }

  function renderChart() {
    const cfg = REPORTS[state.reportMetric];
    const data = cfg[state.period];
    const shift = reportShift(state.reportMetric);
    const values = data.values.map(v => v + shift);
    const labels = data.labels;
    if (state.bar > values.length - 1) state.bar = values.length - 1;
    const min = Math.min(...values);
    const max = Math.max(...values);
    const avg = values.reduce((a, b) => a + b, 0) / values.length;

    el.chartTitle.textContent = cfg.label;
    el.chartSub.textContent = `${state.period[0].toUpperCase() + state.period.slice(1)} trend · ${values.length} readings`;
    el.chartBadge.textContent = `${avg.toFixed(cfg.dec)} avg`;

    el.dmChart.innerHTML = values.map((v, i) => {
      const h = 34 + ((v - min) / (max - min || 1)) * 52;
      const on = i === state.bar;
      const status = statusOf(state.reportMetric, v);
      return `<div class="dm-col">
        <div class="dm-track" style="--h:${h.toFixed(1)}%">
          <button class="dm-bar ${on ? 'is-on' : ''}" data-i="${i}" style="height:${h.toFixed(1)}%"
            aria-pressed="${on}" aria-label="${esc(labels[i])} ${v.toFixed(cfg.dec)}${cfg.unit ? ' ' + cfg.unit : ''}, ${status}">
            <span class="dm-dot" aria-hidden="true"></span>
          </button>
          ${on ? `<span class="dm-tip ${tipClass(status)}">${status}</span><span class="dm-line" aria-hidden="true"></span>` : ''}
        </div>
        <span class="dm-day">${esc(labels[i])}</span>
      </div>`;
    }).join('');
  }

  function renderReports() {
    $$('#reportPeriod button').forEach(b => {
      const on = b.dataset.period === state.period;
      b.classList.toggle('is-on', on);
      b.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
    if (el.reportMetrics) el.reportMetrics.value = state.reportMetric;
    if (el.reportMetricLabel) el.reportMetricLabel.textContent = REPORTS[state.reportMetric].label.replace('Average ', '');
    renderGauge();
    renderChart();
  }

  /* ---------------- body studio ---------------- */
  const ZONES = { trunk: 'Trunk', larm: 'Left arm', rarm: 'Right arm', lleg: 'Left leg', rleg: 'Right leg' };
  const SEG_BASE = {
    muscle: {
      trunk: { v: 24.1, ref: [18, 28], dec: 1 },
      larm: { v: 1.44, ref: [1.1, 1.9], dec: 2 },
      rarm: { v: 1.42, ref: [1.1, 1.9], dec: 2 },
      lleg: { v: 8.0, ref: [6.4, 9.4], dec: 1 },
      rleg: { v: 7.9, ref: [6.4, 9.4], dec: 1 }
    },
    fat: {
      trunk: { v: 8.1, ref: [3.0, 7.5], dec: 1 },
      larm: { v: 0.62, ref: [0.3, 0.9], dec: 2 },
      rarm: { v: 0.61, ref: [0.3, 0.9], dec: 2 },
      lleg: { v: 2.4, ref: [1.6, 3.0], dec: 1 },
      rleg: { v: 2.3, ref: [1.6, 3.0], dec: 1 }
    }
  };

  function segmentValues(mode) {
    const m = activeMember();
    const muscleF = m.smm / 31.8;
    const fatMass = m.weight * m.fat / 100;
    const fatF = fatMass / 13.65;
    const factor = mode === 'muscle' ? muscleF : fatF;
    const out = {};
    Object.entries(SEG_BASE[mode]).forEach(([key, val]) => {
      const v = val.v * factor;
      const [lo, hi] = val.ref;
      let status = 'Optimal';
      if (v > hi) status = 'High';
      else if (v < lo) status = 'Low';
      else if (mode === 'fat' && v > hi * .92) status = 'Elevated';
      out[key] = { v, dec: val.dec, ref: val.ref, status };
    });
    return out;
  }

  function rangePositions(value, ref) {
    const max = ref[1] * 1.35;
    const min = 0;
    const mark = clamp((value - min) / (max - min) * 100, 2, 98);
    const left = clamp((ref[0] - min) / (max - min) * 100, 0, 92);
    const width = clamp((ref[1] - ref[0]) / (max - min) * 100, 8, 100 - left);
    return { mark, left, width };
  }

  function badgeFor(status) {
    if (status === 'Optimal' || status === 'Normal') return 'badge mint';
    if (status === 'Elevated') return 'badge warn';
    if (status === 'High') return 'badge alert';
    return 'badge soft';
  }

  function renderBody() {
    if (!is8E() && state.zone !== 'trunk') state.zone = 'trunk';
    const seg = segmentValues(state.mode);
    const eight = is8E();
    $$('.callout').forEach(c => {
      const key = c.dataset.zone;
      const s = seg[key];
      if (!s) return;
      const est = !eight && key !== 'trunk';
      const dotColor = est ? 'var(--ink-3)' : s.status === 'High' ? 'var(--alert)' : s.status === 'Low' ? 'var(--warn)' : 'var(--cobalt)';
      c.innerHTML = `<span class="co-val"><span class="num">${s.v.toFixed(s.dec)}</span> kg</span>
        <span class="co-st"><i class="dot" style="background:${dotColor}"></i>${est ? '4E est.' : s.status}</span>`;
      c.setAttribute('aria-label', `${ZONES[key]} ${s.v.toFixed(s.dec)} kilograms, ${est ? '4E estimate' : s.status}`);
      c.hidden = false;
    });
    renderZoneDetail(seg);
    renderBalance();
    $$('#segmentMode button').forEach(b => {
      const on = b.dataset.mode === state.mode;
      b.classList.toggle('is-on', on);
      b.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
    $$('.zone').forEach(z => {
      const est = !eight && z.dataset.zone !== 'trunk';
      z.classList.toggle('is-sel', z.dataset.zone === state.zone);
      z.classList.toggle('is-dim', est);
      if (est) { z.setAttribute('aria-disabled', 'true'); z.tabIndex = -1; }
      else { z.removeAttribute('aria-disabled'); z.tabIndex = 0; }
    });
    renderParams();
  }

  function renderBalance() {
    const m = activeMember();
    const mus = segmentValues('muscle');
    const arm = 100 - Math.abs(mus.larm.v - mus.rarm.v) / Math.max(mus.larm.v, mus.rarm.v) * 100;
    const leg = 100 - Math.abs(mus.lleg.v - mus.rleg.v) / Math.max(mus.lleg.v, mus.rleg.v) * 100;
    el.balArm.textContent = `${arm.toFixed(1)}%`;
    el.balArmBar.style.width = `${arm.toFixed(1)}%`;
    el.balLeg.textContent = `${leg.toFixed(1)}%`;
    el.balLegBar.style.width = `${leg.toFixed(1)}%`;
    const alm = 26.4 * (m.smm / 31.8) / ((m.h / 100) ** 2);
    const status = alm < 7.2 ? 'Low' : alm > 9.4 ? 'High' : 'Optimal';
    el.almVal.textContent = alm.toFixed(1);
    el.almBadge.textContent = status;
    el.almBadge.className = badgeFor(status);
  }

  function renderZoneDetail(seg) {
    const s = seg[state.zone];
    if (!s) return;
    const name = ZONES[state.zone];
    el.zoneDetail.querySelector('.zd-label').textContent = name;
    el.zdValue.textContent = `${s.v.toFixed(s.dec)} kg`;
    el.zdBadge.textContent = s.status;
    el.zdBadge.className = badgeFor(s.status).replace('badge ', '');
    el.zdBadge.classList.add('badge');
    el.zoneBadge.textContent = s.status;
    el.zoneBadge.className = badgeFor(s.status);
    const pos = rangePositions(s.v, s.ref);
    const opt = el.zoneDetail.querySelector('.pbar-opt');
    const mark = el.zoneDetail.querySelector('.pbar-mark');
    if (opt) { opt.style.left = `${pos.left}%`; opt.style.width = `${pos.width}%`; }
    if (mark) mark.style.left = `${pos.mark}%`;
    const foot = el.zoneDetail.querySelector('.zd-foot');
    const src = is8E() || state.zone === 'trunk' ? `${currentScale().code} BIA` : '4E estimate';
    if (foot) foot.textContent = `Reference ${s.ref[0].toFixed(1)}–${s.ref[1].toFixed(1)} kg · ${src}`;
  }

  function setZone(zone) {
    if (!ZONES[zone]) return;
    if (!is8E() && zone !== 'trunk') {
      toast('Arms and legs need the 8E handle — trunk is measured on 4E');
      return;
    }
    state.zone = zone;
    const seg = segmentValues(state.mode);
    $$('.zone').forEach(z => z.classList.toggle('is-sel', z.dataset.zone === zone));
    renderZoneDetail(seg);
  }

  /* ---------------- parameters ---------------- */
  const PARAMS = [
    { id: 'core', name: 'Core', items: [
      ['Weight', 74.2, 'kg', -0.4, 45, 110, 62, 78, '8E BIA', 1],
      ['BMI', 23.1, '', -0.1, 14, 40, 18.5, 24.9, 'Derived', 1],
      ['Body Fat', 18.4, '%', -0.3, 5, 45, 10, 20, '8E BIA', 1],
      ['Body Fat Mass', 13.7, 'kg', -0.5, 3, 45, 8, 16, '8E BIA', 1],
      ['Fat-Free Mass', 60.5, 'kg', 0.1, 30, 90, 50, 70, '8E BIA', 1],
      ['Fat-Free Mass Index', 18.9, 'kg/m²', 0.0, 12, 28, 16, 21, 'Derived', 1],
      ['Body Score', 82, 'pts', 2.0, 40, 100, 75, 100, '8E BIA', 0]
    ] },
    { id: 'adiposity', name: 'Adiposity', items: [
      ['Subcutaneous Fat', 14.9, '%', -0.3, 3, 40, 8, 18, '8E BIA', 1],
      ['Visceral Fat Level', 5, 'lvl', 0.0, 1, 20, 1, 9, '8E BIA', 0],
      ['Visceral Fat Mass', 0.9, 'kg', 0.0, 0.1, 6, 0.2, 1.5, '8E BIA', 1],
      ['Waist-Hip Ratio', 0.86, '', -0.01, 0.6, 1.2, 0.75, 0.90, 'Manual', 2],
      ['Waist Circumference', 84.5, 'cm', -0.5, 50, 140, 70, 90, 'Manual', 1],
      ['Hip Circumference', 98.2, 'cm', 0.0, 60, 150, 88, 104, 'Manual', 1],
      ['Android Fat', 19.8, '%', -0.4, 5, 45, 10, 22, '8E BIA', 1],
      ['Gynoid Fat', 16.1, '%', -0.2, 5, 45, 12, 24, '8E BIA', 1]
    ] },
    { id: 'muscle', name: 'Muscle & Bone', items: [
      ['Skeletal Muscle Mass', 31.8, 'kg', 0.2, 15, 50, 28, 38, '8E BIA', 1],
      ['Skeletal Muscle %', 42.8, '%', 0.3, 25, 55, 38, 48, '8E BIA', 1],
      ['Muscle Mass', 55.9, 'kg', 0.2, 25, 80, 45, 62, '8E BIA', 1],
      ['Bone Mass', 3.24, 'kg', 0.0, 1.5, 6, 2.8, 3.8, '8E BIA', 2],
      ['Protein %', 19.4, '%', 0.1, 10, 25, 16, 21, '8E BIA', 1],
      ['Mineral Mass', 4.1, 'kg', 0.0, 2, 7, 3.4, 4.6, '8E BIA', 1],
      ['Appendicular Lean Mass', 26.4, 'kg', 0.2, 10, 40, 22, 30, '8E BIA', 1],
      ['ALM / height²', 8.4, 'kg/m²', 0.1, 4, 14, 7.2, 9.4, '8E BIA', 1],
      ['Muscle Quality Score', 78, 'pts', 1.0, 40, 100, 65, 100, 'Derived', 0]
    ] },
    { id: 'hydration', name: 'Hydration & ICW/ECW', items: [
      ['Body Water', 58.4, '%', 0.2, 35, 75, 52, 62, '8E BIA', 1],
      ['Body Water Mass', 43.3, 'L', 0.1, 20, 65, 38, 48, '8E BIA', 1],
      ['Intracellular Water', 26.8, 'L', 0.1, 10, 40, 23, 30, '8E BIA', 1],
      ['Extracellular Water', 16.5, 'L', 0.0, 6, 26, 13, 18, '8E BIA', 1],
      ['ECW/TBW Ratio', 0.381, '', 0.002, 0.30, 0.45, 0.36, 0.39, '8E BIA', 3],
      ['Total Body Water', 43.3, 'L', 0.1, 20, 65, 38, 48, '8E BIA', 1],
      ['Hydration Score', 86, 'pts', 1.0, 40, 100, 75, 100, 'Derived', 0]
    ] },
    { id: 'segmental', name: 'Segmental 8E', items: [
      ['Left arm muscle', 1.44, 'kg', 0.01, 0.5, 3.0, 1.1, 1.9, '8E BIA', 2],
      ['Right arm muscle', 1.42, 'kg', 0.02, 0.5, 3.0, 1.1, 1.9, '8E BIA', 2],
      ['Trunk muscle', 24.1, 'kg', 0.1, 10, 35, 18, 28, '8E BIA', 1],
      ['Left leg muscle', 7.9, 'kg', 0.05, 3, 14, 6.4, 9.4, '8E BIA', 1],
      ['Right leg muscle', 8.0, 'kg', 0.04, 3, 14, 6.4, 9.4, '8E BIA', 1],
      ['Left arm fat', 0.62, 'kg', -0.01, 0.2, 2.0, 0.3, 0.9, '8E BIA', 2],
      ['Right arm fat', 0.61, 'kg', -0.01, 0.2, 2.0, 0.3, 0.9, '8E BIA', 2],
      ['Trunk fat', 8.1, 'kg', -0.1, 1, 20, 3.0, 7.5, '8E BIA', 1],
      ['Left leg fat', 2.4, 'kg', 0.0, 0.5, 8, 1.6, 3.0, '8E BIA', 1],
      ['Right leg fat', 2.3, 'kg', 0.0, 0.5, 8, 1.6, 3.0, '8E BIA', 1]
    ] },
    { id: 'metabolic', name: 'Metabolic & Cardiac', items: [
      ['BMR', 1742, 'kcal', 4.0, 900, 2600, 1550, 1950, '8E BIA', 0],
      ['Metabolic Age', 29, 'yrs', -1.0, 15, 80, 20, 34, 'Derived', 0],
      ['Heart Rate', 62, 'bpm', 0.0, 40, 120, 50, 75, 'Sensor', 0],
      ['Cardiac Index', 2.8, 'L/min/m²', 0.0, 1.5, 5, 2.4, 3.4, 'Sensor', 1],
      ['Visceral Resistance', 0.74, '', -0.01, 0.4, 1.2, 0.55, 0.85, 'Derived', 2],
      ['Pulse Wave Velocity', 6.1, 'm/s', 0.0, 3, 14, 4.5, 7.5, 'Sensor', 1],
      ['BMR / kg', 23.5, 'kcal/kg', 0.1, 15, 35, 20, 27, 'Derived', 1],
      ['Energy Expenditure', 2440, 'kcal', 12.0, 1200, 3800, 2100, 2800, '4E Est.', 0]
    ] },
    { id: 'raw', name: 'Raw Bioimpedance', items: [
      ['Z 20kHz Total', 512, 'Ω', -5.0, 300, 900, 420, 640, '8E BIA', 0],
      ['Z 100kHz Total', 458, 'Ω', -4.0, 260, 820, 370, 580, '8E BIA', 0],
      ['Z 20kHz Arm', 322, 'Ω', -3.0, 180, 600, 250, 400, '8E BIA', 0],
      ['Z 20kHz Leg', 248, 'Ω', -2.0, 140, 520, 190, 320, '8E BIA', 0],
      ['Phase Angle 50kHz', 6.4, '°', 0.1, 3, 10, 5.2, 7.4, '8E BIA', 1],
      ['Reactance', 18.2, 'Ω', 0.0, 5, 40, 12, 24, '8E BIA', 0],
      ['Resistance', 486, 'Ω', -6.0, 300, 900, 400, 620, '8E BIA', 0]
    ] }
  ].map(g => ({ ...g, items: g.items.map(it => ({
    n: it[0], v: it[1], unit: it[2], delta: it[3], min: it[4], max: it[5],
    optLo: it[6], optHi: it[7], src: it[8], dec: it[9]
  })) }));

  function patchParams(items, groupId) {
    const m = activeMember();
    const fatMass = m.weight * m.fat / 100;
    const muscleF = m.smm / 31.8;
    const fatF = fatMass / 13.65;
    const patch = {
      'Weight': m.weight,
      'BMI': m.weight / ((m.h / 100) ** 2),
      'Body Fat': m.fat,
      'Body Fat Mass': fatMass,
      'Fat-Free Mass': m.weight - fatMass,
      'Fat-Free Mass Index': (m.weight - fatMass) / ((m.h / 100) ** 2),
      'Skeletal Muscle Mass': m.smm,
      'Skeletal Muscle %': (m.smm / m.weight) * 100,
      'Muscle Mass': m.smm + (m.weight - m.smm) * .32,
      'Protein %': (m.smm / m.weight) * 100 * .453,
      'Body Water': (m.water / m.weight) * 100,
      'Body Water Mass': m.water,
      'Intracellular Water': m.water * .62,
      'Extracellular Water': m.water * .38,
      'Total Body Water': m.water,
      'BMR': m.bmr,
      'Visceral Fat Level': m.visc,
      'Visceral Fat Mass': m.weight * m.visc * .0022,
      'Subcutaneous Fat': m.fat * .82,
      'Android Fat': m.fat * 1.08,
      'Gynoid Fat': m.fat * .9,
      'Bone Mass': m.weight * .043,
      'Mineral Mass': m.weight * .055,
      'BMR / kg': m.bmr / m.weight
    };
    return items.map((p, i) => {
      let v = p.v;
      if (patch[p.n] !== undefined) v = patch[p.n];
      if (groupId === 'segmental') {
        v = p.v * (i < 5 ? muscleF : fatF);
      }
      if (groupId === 'muscle' && p.n === 'Appendicular Lean Mass') v = 26.4 * muscleF;
      if (groupId === 'muscle' && p.n === 'ALM / height²') v = (26.4 * muscleF) / ((m.h / 100) ** 2);
      return { ...p, v };
    });
  }

  function paramValue(p) {
    if (p.dec === 0) return commas(Math.round(p.v));
    return p.v.toFixed(p.dec);
  }

  const PARAM_4E = new Set([
    'Weight', 'BMI', 'Body Fat', 'Body Fat Mass', 'Fat-Free Mass', 'Body Water',
    'Body Water Mass', 'Total Body Water', 'BMR', 'Visceral Fat Level', 'Visceral Fat Mass',
    'Subcutaneous Fat', 'Muscle Mass', 'Bone Mass', 'Protein %', 'Metabolic Age', 'BMR / kg',
    'Waist-Hip Ratio', 'Waist Circumference', 'Hip Circumference', 'Hydration Score', 'Energy Expenditure'
  ]);

  function paramRow(p) {
    const pos = clamp((p.v - p.min) / (p.max - p.min) * 100, 2, 98);
    const left = clamp((p.optLo - p.min) / (p.max - p.min) * 100, 0, 94);
    const width = clamp((p.optHi - p.optLo) / (p.max - p.min) * 100, 5, 100 - left);
    const dcls = p.delta > 0 ? 'up' : p.delta < 0 ? 'down' : '';
    const locked = !is8E() && !PARAM_4E.has(p.n);
    return `<div class="prow ${locked ? 'is-locked' : ''}">
      <div class="prow-top">
        <div class="prow-name"><strong>${esc(p.n)}</strong><span class="prow-delta ${dcls}">${p.delta === 0 ? '0' : signed(p.delta, p.dec === 0 ? 0 : Math.min(p.dec, 3))}</span></div>
        <span class="prow-val">${paramValue(p)}<span> ${esc(p.unit)}</span></span>
      </div>
      <div class="prow-foot">
        <div class="pbar" role="img" aria-label="${esc(p.n)} ${paramValue(p)} ${esc(p.unit)}, clinical optimal ${p.optLo}-${p.optHi}">
          <span class="pbar-opt" style="left:${left.toFixed(1)}%;width:${width.toFixed(1)}%"></span>
          <span class="pbar-mark" style="left:${pos.toFixed(1)}%"></span>
        </div>
        <span class="prov">${locked ? '<span class="prov-lock">Requires 8E</span>' : esc(p.src)}</span>
      </div>
    </div>`;
  }

  function renderParams() {
    const q = state.search.trim().toLowerCase();
    let total = 0;
    let verified = 0;
    const html = PARAMS.map(g => {
      const items = patchParams(g.items, g.id)
        .filter(p => !q || p.n.toLowerCase().includes(q) || g.name.toLowerCase().includes(q));
      if (!items.length) return '';
      total += items.length;
      verified += items.filter(p => PARAM_4E.has(p.n)).length;
      const open = !!state.open[g.id];
      return `<section class="pgroup ${open ? 'is-open' : ''}" data-group="${g.id}">
        <button class="pgroup-head" aria-expanded="${open}">
          <strong>${esc(g.name)}</strong>
          <span class="count">${items.length}</span>
          <svg class="od-icon chev" aria-hidden="true"><use href="#i-next"/></svg>
        </button>
        <div class="pgroup-body" ${open ? '' : 'hidden'}>${items.map(paramRow).join('')}</div>
      </section>`;
    }).join('');
    el.paramGroups.innerHTML = html;
    el.paramGroups.hidden = total === 0;
    el.paramEmpty.hidden = total !== 0;
    if (el.paramBadge) el.paramBadge.textContent = currentScale().code;
    if (is8E()) {
      el.paramCount.textContent = q ? `${total} matches · 7 groups` : '56 parameters · 7 groups';
    } else {
      el.paramCount.textContent = q
        ? `${total} matches · ${verified} verified on 4E`
        : `${verified} verified · ${total - verified} need 8E`;
    }
  }

  /* ---------------- profiles ---------------- */
  function renderProfiles() {
    el.profileCount.textContent = `${members.length} / 10`;
    const cards = members.map((m, i) => {
      const active = i === state.active;
      return `<button class="pmember ${active ? 'is-active' : ''}" data-profile="${i}" aria-pressed="${active}" aria-label="${esc(m.name)}, ${esc(m.rel)}, ${active ? 'active profile' : 'set active profile'}">
        <div class="pm-top">
          <span class="m-avatar" style="background:${m.color}">${esc(m.ini)}</span>
          <span class="od-stat"><span class="pm-name">${esc(m.name)}</span><span class="pm-rel">${esc(m.rel)}</span></span>
        </div>
        <div class="pm-meta"><span>${m.age} yrs</span><span>${esc(m.sex)}</span><span>${m.h} cm</span></div>
        <div class="pm-target"><span>Target</span><strong class="num">${m.target.toFixed(1)} kg</strong></div>
        <div class="pm-tags">
          <span class="pm-window">±2.5 kg window</span>
          ${active ? '<span class="badge mint">Active</span>' : '<span class="badge soft">Ready</span>'}
        </div>
      </button>`;
    }).join('');
    const add = `<button class="add-member" id="addMemberBtn" ${members.length >= 10 ? 'disabled' : ''}>
      <span class="add-ic"><svg class="od-icon" aria-hidden="true"><use href="#i-plus"/></svg></span>
      ${members.length >= 10 ? 'Household full' : 'Add member (' + members.length + '/10)'}
      <small>${members.length >= 10 ? 'Remove a profile on the scale first' : 'Avatar, window and target weight'}</small>
    </button>`;
    el.profileGrid.innerHTML = cards + add;
  }

  function renderMemberList() {
    el.memberList.innerHTML = members.map((m, i) => {
      const active = i === state.active;
      return `<button class="member-row ${active ? 'is-on' : ''}" data-member="${i}" aria-pressed="${active}">
        <span class="m-avatar" style="background:${m.color}">${esc(m.ini)}</span>
        <span class="od-stat"><strong>${esc(m.name)}</strong><span>${esc(m.rel)} · ${m.h} cm</span></span>
        <span class="m-val num">${m.weight.toFixed(1)} kg</span>
        ${active ? '<svg class="od-icon m-check" aria-hidden="true"><use href="#i-check"/></svg>' : ''}
      </button>`;
    }).join('');
  }

  function fillOnboardingMini() {
    el.obMemberMini.innerHTML = members.map((m, i) =>
      `<span class="mm ${i === state.active ? 'is-on' : ''}" style="background:${m.color}">${esc(m.ini)}</span>`
    ).join('');
  }

  /* ---------------- history ---------------- */
  let ledger = [
    { id: 1, date: 'Nov 26', time: '07:12', device: '8E Pro', code: '8E', w: 74.2, fat: 18.4, hc: true },
    { id: 2, date: 'Nov 24', time: '07:05', device: '8E Pro', code: '8E', w: 74.6, fat: 18.7, hc: true },
    { id: 3, date: 'Nov 22', time: '07:18', device: 'CS-BF01', code: '4E', w: 74.6, fat: 18.8, hc: true },
    { id: 4, date: 'Nov 19', time: '21:40', device: 'Manual', code: 'MAN', w: 75.0, fat: 18.9, hc: false },
    { id: 5, date: 'Nov 17', time: '07:09', device: '8E Pro', code: '8E', w: 75.1, fat: 19.0, hc: true },
    { id: 6, date: 'Nov 15', time: '07:03', device: '8E Pro', code: '8E', w: 74.8, fat: 18.8, hc: true },
    { id: 7, date: 'Nov 12', time: '07:22', device: 'CS-BF01', code: '4E', w: 75.2, fat: 19.1, hc: true }
  ];
  let ledgerSeq = 8;

  function renderHistory() {
    el.histCount.textContent = String(ledger.length);
    el.historyList.innerHTML = ledger.map(r => {
      const picked = state.picked.includes(r.id);
      return `<button class="hrow ${picked ? 'is-picked' : ''}" data-id="${r.id}" aria-pressed="${picked}" aria-label="Weigh-in ${r.date} ${r.time}, ${r.w.toFixed(1)} kilograms, ${r.fat.toFixed(1)} percent body fat">
        <span class="pick" aria-hidden="true"><svg class="od-icon"><use href="#i-check"/></svg></span>
        <span class="device-ic">${esc(r.code)}</span>
        <span class="od-stat">
          <span class="h-date">${esc(r.date)}</span>
          <span class="h-meta">${esc(r.time)} · ${esc(r.device)} ${r.hc ? '<span class="h-hc">HC</span>' : '<span class="h-hc" style="background:var(--track);color:var(--ink-2)">LOCAL</span>'}</span>
        </span>
        <span class="h-val"><strong class="num">${r.w.toFixed(1)} kg</strong><span class="num">${r.fat.toFixed(1)}% fat</span></span>
      </button>`;
    }).join('');
    el.compareCount.textContent = `${state.picked.length} / 2`;
    const pickedRows = state.picked.map(id => ledger.find(r => r.id === id)).filter(Boolean);
    if (pickedRows.length === 2) {
      el.compareLabel.textContent = `${pickedRows[0].date} vs ${pickedRows[1].date}`;
    } else if (pickedRows.length === 1) {
      el.compareLabel.textContent = 'Pick one more weigh-in';
    } else {
      el.compareLabel.textContent = 'Select two weigh-ins';
    }
    el.compareBtn.disabled = state.picked.length !== 2;
  }

  function metricsFor(row) {
    const fatMass = row.w * row.fat / 100;
    const smm = Math.max(18, row.w * .43 - .6);
    const water = row.w * .584;
    const visc = Math.max(1, Math.round(3 + (row.fat - 18) * .6));
    const bmr = Math.round(1200 + row.w * 7.3);
    return [
      ['Weight', row.w, 'kg', 1],
      ['Body fat', row.fat, '%', 1],
      ['Skeletal muscle', smm, 'kg', 1],
      ['Body water', water, 'L', 1],
      ['Visceral level', visc, 'lvl', 0],
      ['BMR', bmr, 'kcal', 0]
    ];
  }

  function openCompare() {
    const [aId, bId] = state.picked;
    const a = ledger.find(r => r.id === aId);
    const b = ledger.find(r => r.id === bId);
    if (!a || !b) return;
    const first = ledger.indexOf(a) <= ledger.indexOf(b) ? a : b;
    const second = first === a ? b : a;
    const ma = metricsFor(first);
    const mb = metricsFor(second);
    const rows = ma.map((m, i) => {
      const v1 = m[1], v2 = mb[i][1];
      const delta = v2 - v1;
      const dec = m[3];
      const cls = delta > 0 ? 'up' : delta < 0 ? 'down' : '';
      return `<div class="cmp-row">
        <span>${esc(m[0])}</span>
        <b class="num">${dec === 0 ? commas(Math.round(v1)) : v1.toFixed(dec)}</b>
        <b class="num">${dec === 0 ? commas(Math.round(v2)) : v2.toFixed(dec)}</b>
        <em class="${cls}">${delta === 0 ? '±0' : signed(delta, dec)}</em>
      </div>`;
    }).join('');
    el.compareBody.innerHTML = `
      <div class="cmp-grid">
        <div class="cmp-card"><span>${esc(first.date)} · ${esc(first.device)}</span><strong class="num">${first.w.toFixed(1)} kg</strong><small>${first.fat.toFixed(1)}% body fat</small></div>
        <div class="cmp-card"><span>${esc(second.date)} · ${esc(second.device)}</span><strong class="num">${second.w.toFixed(1)} kg</strong><small>${second.fat.toFixed(1)}% body fat</small></div>
      </div>
      <div class="cmp-table">${rows}</div>
      <p class="form-note" style="margin-top:10px">Delta column reads ${esc(second.date)} minus ${esc(first.date)}. All 56 parameters stay available in Body Studio.</p>
      <button class="btn btn-primary wide" data-close="ovCompare">Done</button>`;
    openOverlay('ovCompare');
  }

  /* ---------------- sync ---------------- */
  const HC_OUT = [
    ['Weight', 'Every weigh-in writes one record'],
    ['Body Fat', 'Percentage and fat mass'],
    ['Lean Mass', 'Fat-free mass and skeletal muscle'],
    ['Bone Mass', 'Estimated mineral content'],
    ['Body Water', 'Total body water'],
    ['BMR', 'Basal metabolic rate'],
    ['Heart Rate', 'Resting pulse from handle sensors']
  ];
  const HC_IN = [
    ['Steps', 'Daily activity context'],
    ['Sleep', 'Duration and deep-sleep split'],
    ['Calories', 'Active and total burn']
  ];

  function switchRow(label, sub, on, key, group) {
    return `<div class="tog-row">
      <label for="sw-${group}-${key.replace(/\s+/g, '')}"><strong>${esc(label)}</strong><span>${esc(sub)}</span></label>
      <button class="switch" id="sw-${group}-${key.replace(/\s+/g, '')}" role="switch" aria-checked="${on}" data-key="${esc(key)}" data-group="${group}" aria-label="${esc(label)} sync"></button>
    </div>`;
  }

  function renderSync() {
    el.hcOut.innerHTML = HC_OUT.map(([k, s]) => switchRow(k, s, !!state.hcOut[k], k, 'out')).join('');
    el.hcIn.innerHTML = HC_IN.map(([k, s]) => switchRow(k, s, !!state.hcIn[k], k, 'in')).join('');
  }

  function setSwitch(btn, on) {
    btn.setAttribute('aria-checked', on ? 'true' : 'false');
    const g = btn.dataset.group;
    const k = btn.dataset.key;
    if (g === 'out') state.hcOut[k] = on; else state.hcIn[k] = on;
    toast(`${k} ${on ? 'will sync' : 'paused'} with Health Connect`);
  }

  /* ---------------- inspector ---------------- */
  let frameSeq = 0;
  function makeFrame() {
    const eight = is8E();
    const kinds = eight ? ['CF', 'BE-A', 'BE-B', 'DF'] : ['FD', 'F1', 'F2', 'BE'];
    const kind = kinds[frameSeq % kinds.length];
    frameSeq += 1;
    const len = eight
      ? (kind === 'CF' ? 11 : kind.startsWith('BE') ? 13 : 17)
      : (kind === 'FD' ? 2 : kind === 'F1' ? 17 : kind === 'F2' ? 2 : 13);
    const bytes = [];
    if (eight) {
      bytes.push(kind === 'CF' ? 0xCF : kind.startsWith('BE') ? 0xBE : 0xDF);
      bytes.push(kind === 'CF' ? 0x0B : kind === 'BE-A' ? 0x01 : kind === 'BE-B' ? 0x02 : 0x02);
    } else {
      bytes.push(kind === 'FD' ? 0xFD : kind === 'F1' ? 0xF1 : kind === 'F2' ? 0xF2 : 0xBE);
      bytes.push(kind === 'FD' ? 0x37 : kind === 'F2' ? 0x00 : 0x01);
    }
    while (bytes.length < len - 1) bytes.push(Math.floor(Math.random() * 256));
    const x = bytes.reduce((a, b) => a ^ b, 0);
    const fail = Math.random() < .08;
    const sent = fail ? (x ^ 0x7F) : x;
    bytes.push(sent);
    const hex = bytes.map(b => b.toString(16).padStart(2, '0').toUpperCase()).join(' ');
    const label = eight
      ? (kind === 'CF' ? '0xCF' : kind.startsWith('BE') ? '0xBE' : '0xDF')
      : (kind === 'FD' ? '0xFD' : kind === 'F1' ? '0xF1' : kind === 'F2' ? '0xF2' : '0xBE');
    const sub = eight
      ? (kind === 'BE-A' ? 'Set A' : kind === 'BE-B' ? 'Set B' : kind === 'DF' ? `TX ${(frameSeq % 4) + 1}` : 'Weight')
      : (kind === 'FD' ? 'Handshake' : kind === 'F1' ? 'Device info' : kind === 'F2' ? 'Session open' : `Foot loop ${(frameSeq % 4) + 1}`);
    return { kind, label, sub, len, hex, ok: !fail, x };
  }

  function pushFrame() {
    state.frames.unshift(makeFrame());
    if (state.frames.length > 14) state.frames.pop();
    renderFrames();
  }

  function renderFrames() {
    if (!el.frameList) return;
    el.frameList.innerHTML = state.frames.map(f => `<div class="frame">
      <div class="frame-head">
        <span class="frame-type">${esc(f.label)}</span>
        <span class="frame-len">${f.len}B · ${esc(f.sub)}</span>
        <span class="chk ${f.ok ? 'pass' : 'fail'}">${f.ok ? 'XOR PASS' : 'XOR FAIL'}</span>
      </div>
      <div class="frame-hex">${esc(f.hex)}</div>
    </div>`).join('');
    el.frameStats.textContent = is8E()
      ? `${state.frames.length} frames · CF 11B · BE 13B ×2 · DF 17B ×4`
      : `${state.frames.length} frames · FD 2B · F1 17B · F2 2B · BE 13B`;
  }

  function renderOhms() {
    const eight = is8E();
    const chans = eight ? [512, 498, 322, 318, 486, 470, 248, 251, 462, 455] : [512, 486, 470, 455];
    const names = eight ? ['L1', 'L2', 'R1', 'R2', 'H1', 'H2', 'H3', 'H4', 'T1', 'T2'] : ['L1', 'R1', 'L2', 'R2'];
    el.ohmGrid.innerHTML = chans.map((v, i) => `<div class="ohm-cell ${eight ? '' : 'is-est'}"><span>${names[i]}</span><strong>${v} Ω</strong>${eight ? '' : '<small>4E</small>'}</div>`).join('');
  }

  /* ---------------- weigh-in ---------------- */
  function resetWi() {
    clearWiTimers();
    state.wiStage = 1;
    state.wiMember = null;
    state.wiWeight = activeMember().weight;
    el.wiAmbiguity.setAttribute('aria-checked', 'false');
    state.wiAmbiguity = false;
    el.wiAction.disabled = false;
    if (el.wiHandshake) el.wiHandshake.hidden = true;
    applyScaleUI();
    setWiStage(1);
  }

  function setStepRail(stage) {
    $$('#wiSteps li').forEach((li, i) => {
      li.classList.toggle('is-on', i + 1 === stage);
      li.classList.toggle('is-done', i + 1 < stage);
    });
  }

  function setWiStage(stage) {
    clearWiTimers();
    state.wiStage = stage;
    setStepRail(stage);
    $$('.wi-stage').forEach(s => { s.hidden = Number(s.dataset.wstage) !== stage; });
    setWiAction(stage);
    if (stage === 2) startSettle();
    if (stage === 3) startContact();
    if (stage === 4) updateStage4();
  }

  function setWiAction(stage) {
    const btn = el.wiAction;
    btn.disabled = false;
    if (stage === 1) btn.textContent = 'Start scan';
    if (stage === 2) { btn.textContent = 'Reading…'; btn.disabled = true; }
    if (stage === 3) { btn.textContent = 'Lock reading'; btn.disabled = true; }
    if (stage === 4) {
      if (state.wiAmbiguity && !state.wiMember) {
        btn.textContent = 'Choose a member';
        btn.disabled = true;
      } else {
        btn.textContent = 'Done';
        btn.disabled = false;
      }
    }
  }

  function startSettle() {
    const target = activeMember().weight;
    let ticks = 0;
    el.wiSettleNote.textContent = 'Stand still with both feet centred on the ITO glass.';
    el.wiSettleBar.style.width = '0%';
    el.wiWeight.textContent = (target - .38).toFixed(2);
    const t = setInterval(() => {
      ticks += 1;
      const p = Math.min(1, ticks / 17);
      const jitter = (Math.random() - .5) * (1 - p) * .1;
      const value = target - .38 * (1 - p) + jitter;
      state.wiWeight = value;
      el.wiWeight.textContent = value.toFixed(2);
      el.wiSettleBar.style.width = `${Math.round(p * 100)}%`;
      const head = is8E() ? 'CF 0B' : 'FD 37';
      el.wiTicker.textContent = `${head} ${[1, 2, 3, 4, 5, 6, 7, 8].map(() => Math.floor(Math.random() * 256).toString(16).padStart(2, '0').toUpperCase()).join(' ')}`;
      if (p >= 1) {
        clearInterval(t);
        state.wiWeight = target;
        el.wiWeight.textContent = target.toFixed(2);
        el.wiSettleNote.textContent = `Weight stable at ${target.toFixed(2)} kg · ready for ${is8E() ? 'bioimpedance' : 'foot bioimpedance'}.`;
        el.wiAction.disabled = false;
        el.wiAction.textContent = 'Begin BIA';
      }
    }, 130);
    wiTimers.push(t);
  }

  function startContact() {
    const eight = is8E();
    let p = 0;
    const ring = el.wiRing;
    const setP = (v) => {
      el.wiRingVal.textContent = `${Math.round(v * 100)}%`;
      ring.style.strokeDashoffset = String(213.6 * (1 - v));
    };
    setP(0);
    el.wiOhms.innerHTML = '';
    el.wiAction.disabled = true;
    if (el.wiHandshake) {
      el.wiHandshake.hidden = eight;
      el.wiHandshake.innerHTML = eight ? '' :
        '<b>FD 37</b>  handshake accepted\n<b>F1</b>  device info · CS-BF01 · FW 2.4\n<b>F2 00</b>  measurement session open\n<b>BE</b>  foot loop · 4 channels';
    }
    const t = setInterval(() => {
      p = Math.min(1, p + (eight ? .055 : .072));
      setP(p);
      if (p >= 1) {
        clearInterval(t);
        el.wiAction.disabled = false;
        el.wiAction.textContent = 'Save reading';
        setTimeout(() => { if (state.wiStage === 3) setWiStage(4); }, 320);
      }
    }, 90);
    wiTimers.push(t);
    const ohms = eight
      ? [512, 498, 322, 318, 486, 470, 248, 251, 462, 455]
      : [512, 486, 470, 455];
    el.wiOhms.innerHTML = ohms.map((v, i) => `<span class="${eight ? '' : 'is-est'}">${eight ? 'CH' : 'F'}${i + 1} ${v}</span>`).join('');
  }

  function closestSecondMember() {
    const m = activeMember();
    let best = null;
    let dist = Infinity;
    members.forEach((x, i) => {
      if (i === state.active) return;
      const d = Math.abs(x.weight - m.weight);
      if (d < dist) { dist = d; best = i; }
    });
    return best === null ? (state.active + 1) % members.length : best;
  }

  function updateStage4() {
    const m = state.wiMember !== null ? members[state.wiMember] : activeMember();
    const weight = state.wiWeight;
    el.wiFinal.textContent = weight.toFixed(1);
    el.wiFinalSub.textContent = is8E()
      ? `${m.fat.toFixed(1)}% fat · ${m.smm.toFixed(1)} kg muscle`
      : `${m.fat.toFixed(1)}% fat · ${m.smm.toFixed(1)} kg muscle (est.)`;
    if (el.wiHealthSub) el.wiHealthSub.textContent = is8E()
      ? 'Weight · Body fat · Lean mass'
      : 'Weight · Body fat (aggregate only)';
    const pending = state.wiAmbiguity && state.wiMember === null;
    el.wiPickWrap.hidden = !pending;
    const doneList = $('.done-list', $('[data-wstage="4"]'));
    if (doneList) doneList.hidden = pending;
    if (pending) {
      const second = closestSecondMember();
      el.wiPickList.innerHTML = [state.active, second].map((idx, i) => {
        const mm = members[idx];
        return `<button class="pick-row" data-pick="${idx}">
          <span class="m-avatar" style="background:${mm.color}">${esc(mm.ini)}</span>
          <span class="od-stat"><strong>${esc(mm.name)}</strong><span>Last reading ${mm.weight.toFixed(1)} kg · within 2.5 kg</span></span>
          <span class="m-val">${(mm.weight).toFixed(1)} kg</span>
        </button>`;
      }).join('');
    }
    setWiAction(4);
  }

  function finishWeighin() {
    const m = state.wiMember !== null ? members[state.wiMember] : activeMember();
    if (state.wiMember !== null) state.active = state.wiMember;
    const weight = Math.round(state.wiWeight * 10) / 10;
    m.weight = weight;
    m.delta = 0;
    ledger.unshift({
      id: ledgerSeq++,
      date: 'Today', time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      device: currentScale().badge, code: currentScale().code, w: weight, fat: m.fat, hc: true
    });
    state.picked = [];
    closeOverlay('ovWeighin');
    renderHome();
    renderHistory();
    renderProfiles();
    buildGallery();
    toast(`Weigh-in saved · Health Connect + Supabase updated · ${currentScale().badge}`);
  }

  /* ---------------- exports ---------------- */
  function downloadFile(name, content, type) {
    try {
      const blob = new Blob([content], { type });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1500);
      toast(`Export ready · ${name}`);
    } catch (e) {
      toast('Export blocked in this preview');
    }
  }

  function exportJson() {
    const payload = {
      app: 'OpenCult prototype',
      exportedAt: new Date().toISOString(),
      activeProfile: activeMember().name,
      profiles: members.map(m => ({ name: m.name, relation: m.rel, sex: m.sex, age: m.age, heightCm: m.h, targetKg: m.target, lastWeightKg: m.weight, bodyFatPct: m.fat, skeletalMuscleKg: m.smm, bodyWaterL: m.water, bmr: m.bmr })),
      weighIns: ledger
    };
    downloadFile('opencult-export.json', JSON.stringify(payload, null, 2), 'application/json');
  }

  function exportCsv() {
    const head = 'date,time,device,weight_kg,body_fat_pct,health_connect';
    const lines = ledger.map(r => `${r.date},${r.time},${r.device},${r.w.toFixed(1)},${r.fat.toFixed(1)},${r.hc}`);
    downloadFile('opencult-weighins.csv', [head, ...lines].join('\n'), 'text/csv');
  }

  /* ---------------- start ---------------- */
  function bindStatic() {
    $('#obBack').addEventListener('click', () => { if (state.step > 0) { state.step -= 1; renderOnboarding(); } });
    $('#obNext').addEventListener('click', () => {
      if (state.step < 2) { state.step += 1; renderOnboarding(); return; }
      state.step = 0;
      renderOnboarding();
      showScreen('home');
    });
    $('#obSkip').addEventListener('click', () => { state.step = 0; renderOnboarding(); showScreen('home'); });

    $('#avatarBtn').addEventListener('click', () => { renderMemberList(); openOverlay('ovMembers'); });
    $('#notifyBtn').addEventListener('click', () => openOverlay('ovNotify'));
    $('#drawerBtn').addEventListener('click', () => openOverlay('ovDrawer'));
    $('#themeRow').addEventListener('click', toggleTheme);
    if (el.studioTheme) el.studioTheme.addEventListener('click', toggleTheme);

    const METRIC_CYCLE = ['weight', 'fat', 'smm'];
    if (el.heroMetric) el.heroMetric.addEventListener('click', () => {
      state.metric = METRIC_CYCLE[(METRIC_CYCLE.indexOf(state.metric) + 1) % METRIC_CYCLE.length];
      renderHero();
      renderHeader();
    });

    if (el.viewMode) el.viewMode.addEventListener('click', (e) => {
      const b = e.target.closest('[data-viewmode]');
      if (!b) return;
      setView(b.dataset.viewmode);
    });
    if (el.screenRail) el.screenRail.addEventListener('click', (e) => {
      const b = e.target.closest('[data-jump]');
      if (b) { showScreen(b.dataset.jump); return; }
      if (e.target.closest('[data-weighin]')) { resetWi(); openOverlay('ovWeighin'); }
    });
    if (el.galleryGrid) el.galleryGrid.addEventListener('click', (e) => {
      const card = e.target.closest('.gallery-card');
      if (card) activateCard(card.dataset.screen);
    });
    if (el.galleryGrid) el.galleryGrid.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      const card = e.target.closest('.gallery-card');
      if (!card) return;
      e.preventDefault();
      activateCard(card.dataset.screen);
    });
    if (el.scaleMode) el.scaleMode.addEventListener('change', () => setScale(el.scaleMode.value));
    if (el.bodySwitchScale) el.bodySwitchScale.addEventListener('click', () => setScale('pro'));
    $$('#ovWeighin [data-wiscale]').forEach(b => b.addEventListener('click', () => {
      setScale(b.dataset.wiscale);
      if (state.wiStage === 1) setWiStage(1);
    }));

    $('#pagePrev').addEventListener('click', () => setPage(1));
    $('#pageNext').addEventListener('click', () => setPage(2));

    $('#trendRange').addEventListener('change', renderTrendWave);

    $('#reportPeriod').addEventListener('click', (e) => {
      const b = e.target.closest('[data-period]');
      if (!b) return;
      state.period = b.dataset.period;
      renderReports();
    });
    if (el.reportMetrics) el.reportMetrics.addEventListener('change', () => {
      state.reportMetric = el.reportMetrics.value;
      state.bar = Math.min(3, REPORTS[state.reportMetric][state.period].values.length - 1);
      renderReports();
    });
    $('#dmChart').addEventListener('click', (e) => {
      const b = e.target.closest('.dm-bar');
      if (!b) return;
      state.bar = Number(b.dataset.i);
      renderChart();
    });
    $('#reportShare').addEventListener('click', () => {
      const cfg = REPORTS[state.reportMetric];
      const value = (cfg.current + reportShift(state.reportMetric)).toFixed(cfg.dec);
      copyText(`${cfg.label}: ${value}${cfg.unit ? ' ' + cfg.unit : ''} · OpenCult ${currentScale().badge} · ${state.period}`, 'Report summary copied');
    });

    $('#segmentMode').addEventListener('click', (e) => {
      const b = e.target.closest('[data-mode]');
      if (!b) return;
      state.mode = b.dataset.mode;
      renderBody();
    });

    const zoneActivate = (zone) => setZone(zone);
    $('.body-map').addEventListener('click', (e) => {
      const target = e.target.closest('[data-zone]');
      if (target) zoneActivate(target.dataset.zone);
    });
    $('.body-map').addEventListener('keydown', (e) => {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      const target = e.target.closest('[data-zone]');
      if (!target) return;
      e.preventDefault();
      zoneActivate(target.dataset.zone);
    });

    $('#paramSearch').addEventListener('input', (e) => {
      state.search = e.target.value;
      if (state.search.trim()) {
        PARAMS.forEach(g => { state.open[g.id] = true; });
      } else {
        state.open = { core: true };
      }
      renderParams();
    });
    $('#paramGroups').addEventListener('click', (e) => {
      const head = e.target.closest('.pgroup-head');
      if (!head) return;
      const group = head.closest('.pgroup');
      const id = group.dataset.group;
      state.open[id] = !state.open[id];
      renderParams();
    });

    $('#profileGrid').addEventListener('click', (e) => {
      const card = e.target.closest('[data-profile]');
      if (card) { setActiveMember(Number(card.dataset.profile)); return; }
      if (e.target.closest('#addMemberBtn')) {
        if (members.length >= 10) { toast('Household already has 10 profiles'); return; }
        resetForm();
        openOverlay('ovAddMember');
      }
    });

    $('#memberList').addEventListener('click', (e) => {
      const row = e.target.closest('[data-member]');
      if (!row) return;
      closeOverlay('ovMembers');
      setActiveMember(Number(row.dataset.member));
    });

    $('#historyList').addEventListener('click', (e) => {
      const row = e.target.closest('.hrow');
      if (!row) return;
      const id = Number(row.dataset.id);
      const idx = state.picked.indexOf(id);
      if (idx >= 0) state.picked.splice(idx, 1);
      else {
        if (state.picked.length >= 2) state.picked.shift();
        state.picked.push(id);
      }
      renderHistory();
    });
    $('#compareBtn').addEventListener('click', () => {
      if (state.picked.length === 2) openCompare();
    });

    $('#hcOut').addEventListener('click', (e) => {
      const b = e.target.closest('.switch');
      if (!b) return;
      setSwitch(b, b.getAttribute('aria-checked') !== 'true');
    });
    $('#hcIn').addEventListener('click', (e) => {
      const b = e.target.closest('.switch');
      if (!b) return;
      setSwitch(b, b.getAttribute('aria-checked') !== 'true');
    });
    $('#exportJson').addEventListener('click', exportJson);
    $('#exportCsv').addEventListener('click', exportCsv);

    let backfilling = false;
    $('#backfillBtn').addEventListener('click', () => {
      if (backfilling) return;
      backfilling = true;
      const btn = $('#backfillBtn');
      btn.disabled = true;
      btn.textContent = 'Backfilling…';
      let bar = $('#backfillBar');
      if (!bar) {
        bar = document.createElement('div');
        bar.className = 'backfill-bar';
        bar.id = 'backfillBar';
        bar.innerHTML = '<span style="width:0%"></span>';
        btn.closest('.sync-foot').after(bar);
      }
      const fill = bar.querySelector('span');
      let p = 0;
      const t = setInterval(() => {
        p = Math.min(100, p + 9);
        fill.style.width = `${p}%`;
        el.backfillNote.textContent = `${Math.round(1284 * p / 100)} / 1,284 records`;
        if (p >= 100) {
          clearInterval(t);
          backfilling = false;
          btn.disabled = false;
          btn.textContent = 'Backfill complete';
          el.backfillNote.textContent = '1,284 records written';
          toast('Health Connect history backfilled');
        }
      }, 160);
    });

    $('#framePlay').addEventListener('click', () => {
      state.capture = !state.capture;
      const use = $('#framePlay use');
      use.setAttribute('href', state.capture ? '#i-pause' : '#i-play');
      $('#framePlay').setAttribute('aria-label', state.capture ? 'Pause live capture' : 'Resume live capture');
      el.captureState.textContent = state.capture ? 'Live capture' : 'Paused';
    });
    $('#copyCapture').addEventListener('click', () => {
      const payload = {
        device: currentScale().name,
        protocol: is8E() ? '0xCF / 0xBE / 0xDF' : '0xFD / 0xF1 / 0xF2',
        capturedAt: new Date().toISOString(),
        checksum: 'XOR-8',
        frames: state.frames.map(f => ({ type: f.label, set: f.sub, length: f.len, hex: f.hex, checksum: f.ok ? 'PASS' : 'FAIL' }))
      };
      copyText(JSON.stringify(payload, null, 2), 'Capture JSON copied');
    });

    $('#fab').addEventListener('click', () => { resetWi(); openOverlay('ovWeighin'); });
    $('#wiAction').addEventListener('click', () => {
      const s = state.wiStage;
      if (s === 1) setWiStage(2);
      else if (s === 2) { if (!el.wiAction.disabled) setWiStage(3); }
      else if (s === 3) setWiStage(4);
      else if (s === 4) {
        if (state.wiAmbiguity && state.wiMember === null) return;
        finishWeighin();
      }
    });
    $('#wiAmbiguity').addEventListener('click', () => {
      state.wiAmbiguity = !state.wiAmbiguity;
      if (!state.wiAmbiguity) state.wiMember = null;
      el.wiAmbiguity.setAttribute('aria-checked', state.wiAmbiguity ? 'true' : 'false');
      if (state.wiStage === 4) updateStage4();
    });
    $('#wiPickList').addEventListener('click', (e) => {
      const row = e.target.closest('[data-pick]');
      if (!row) return;
      state.wiMember = Number(row.dataset.pick);
      updateStage4();
    });

    $('#amSave').addEventListener('click', addMemberFromForm);

    $$('.dock-btn').forEach(b => b.addEventListener('click', () => go(b.dataset.nav)));

    document.addEventListener('click', (e) => {
      const nav = e.target.closest('[data-go]');
      if (nav) {
        const ov = nav.closest('.overlay');
        if (ov) closeOverlay(ov.id);
        go(nav.dataset.go);
        return;
      }
      const closer = e.target.closest('[data-close]');
      if (closer) closeOverlay(closer.dataset.close);
    });

    document.addEventListener('keydown', (e) => {
      const ov = $$('.overlay').find(o => !o.hidden);
      if (!ov) return;
      if (e.key === 'Escape') { closeOverlay(ov.id); return; }
      if (e.key === 'Tab') trapFocus(e, ov);
    });
  }

  /* ---------------- add member ---------------- */
  function resetForm() {
    ['amName', 'amAge', 'amHeight', 'amTarget'].forEach(id => { const n = document.getElementById(id); if (n) n.value = ''; });
    ['errName', 'errAge', 'errHeight', 'errTarget'].forEach(id => { const n = document.getElementById(id); if (n) n.hidden = true; });
    $$('#ovAddMember .field').forEach(f => f.classList.remove('has-error'));
  }

  function addMemberFromForm() {
    const name = el.amName.value.trim();
    const age = Number(el.amAge.value);
    const height = Number(el.amHeight.value);
    const target = Number(el.amTarget.value);
    const errors = [];
    const setErr = (fieldId, errId, on) => {
      const err = document.getElementById(errId);
      if (err) err.hidden = !on;
      const f = document.getElementById(fieldId);
      if (f) f.closest('.field').classList.toggle('has-error', on);
    };
    const nameOk = name.length >= 2;
    const ageOk = Number.isFinite(age) && age >= 2 && age <= 110;
    const heightOk = Number.isFinite(height) && height >= 80 && height <= 230;
    const targetOk = Number.isFinite(target) && target >= 20 && target <= 300;
    setErr('amName', 'errName', !nameOk);
    setErr('amAge', 'errAge', !ageOk);
    setErr('amHeight', 'errHeight', !heightOk);
    setErr('amTarget', 'errTarget', !targetOk);
    if (!nameOk) errors.push(el.amName);
    if (!ageOk) errors.push(el.amAge);
    if (!heightOk) errors.push(el.amHeight);
    if (!targetOk) errors.push(el.amTarget);
    if (errors.length) { errors[0].focus(); return; }
    if (members.length >= 10) { toast('Household already has 10 profiles'); return; }
    const palette = ['#2E6BE6', '#8C63D9', '#3FA97A', '#E1843B', '#6B7BA8', '#C2658C', '#3C9AA8', '#7E5BEF', '#5B8C4A', '#D06A5A'];
    const sex = el.amSex.value;
    const weight = Number((target + .4).toFixed(1));
    const fat = sex === 'Female' ? 24.1 : sex === 'Male' ? 18.6 : 21.0;
    const smm = Number((weight * (sex === 'Female' ? .4 : .43)).toFixed(1));
    const initials = name.split(/\s+/).slice(0, 2).map(w => w[0].toUpperCase()).join('');
    members.push({
      name, rel: 'Household', sex, age, h: height, target,
      weight, delta: 0, fat, fatDelta: 0, smm, smmDelta: .1,
      water: Number((weight * .57).toFixed(1)), bmr: Math.round(370 + 21.6 * weight),
      visc: 4, ini: initials || 'OC', color: palette[members.length % palette.length]
    });
    closeOverlay('ovAddMember');
    renderProfiles();
    renderMemberList();
    fillOnboardingMini();
    fillOnboardingMembers();
    buildGallery();
    toast(`${name} added to the household`);
  }

  /* ---------------- clipboard ---------------- */
  async function copyText(text, okMsg) {
    try {
      await navigator.clipboard.writeText(text);
      toast(okMsg || 'Copied');
      return;
    } catch (e) { /* fall through */ }
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      ta.remove();
      toast(okMsg || 'Copied');
    } catch (e) {
      toast('Clipboard blocked in this preview');
    }
  }

  /* ---------------- boot ---------------- */
  function boot() {
    applyTheme();
    bindStatic();
    applyScaleUI();
    fillOnboardingMini();
    fillOnboardingMembers();
    renderOnboarding();
    renderHome();
    renderReports();
    renderParams();
    renderProfiles();
    renderMemberList();
    renderHistory();
    renderSync();
    renderOhms();
    for (let i = 0; i < 8; i++) state.frames.push(makeFrame());
    renderFrames();
    showScreen('onboarding');

    const embedded = document.documentElement.classList.contains('od-embed');
    const narrow = window.matchMedia('(max-width: 720px)').matches;
    setView(embedded || narrow ? 'phone' : 'gallery');
    buildGallery();

    setInterval(() => {
      if (!state.capture) return;
      const screen = $('.screen:not([hidden])');
      if (!screen || screen.dataset.screen !== 'inspector') return;
      pushFrame();
    }, 1900);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
