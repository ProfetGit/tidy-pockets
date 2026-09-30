// Tidy Pockets icon v2: a messy heap of redstone, gold and diamond blocks shivers, pops, and every block flies into
// three sorted towers (bottoms land first). The towers do a ta-da wave, hold, then hop back into the heap. Run inside
// Blockbench (free format project):
//   eval(require('fs').readFileSync('<this file>', 'utf8')); window.TP = TP; TP.loadTextures()   // then, in a later call:
//   TP.build(); TP.animate(); TP.camera(); TP.render(0, 79)                                      // 1600px frames -> frames/
// Other sessions share this Blockbench app: the global is TP (never VM), and every entry point first selects this
// project's tab, because `Project` is whichever tab happens to be selected.
// Blocks live in the `rig` group (yaw 45): west = left face, south = right face. Each block's outer group pivots at
// its bottom centre (position + scale, keyed in rig space), the inner `_rot` group at its centre (rotation).
// Every channel is sampled once per frame (linear keys), so rendered frames land exactly on the computed poses.
var TP = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/TidyPockets/dev/icon/';
  const TEX = DIR + 'sprites/';
  const FPS = 25, DT = 1 / FPS, LEN = 3.2, FR = Math.round(LEN * FPS);
  const CAM_POS = [0, 60, 104], CAM_TARGET = [0, 16, 0];
  let CAM_PAN = [0, 14, 0], CAM_ZOOM = 0.3;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const RAD = Math.PI / 180, YAW = 45, S2 = Math.SQRT1_2;
  const Z_FX = 60;

  const TYPES = ['redstone', 'gold', 'diamond'];   // tower 0 (left) .. 2 (right)
  const SP = 19;                                   // tower spacing along rig (1,0,1), i.e. screen right
  // heap, given as screen-ish (u = right, y = up, v = toward camera) plus rotation; slot = [tower, level]
  const BLOCKS = [
    { type: 0, slot: [0, 0], heap: [-24, 0, -6], rot: [0, 25, 0] },
    { type: 2, slot: [2, 0], heap: [-6, 0, 6], rot: [0, -20, 0] },
    { type: 1, slot: [1, 0], heap: [14, 0, -8], rot: [0, 10, 0] },
    { type: 0, slot: [0, 1], heap: [28, 0, 6], rot: [0, 40, 0] },
    { type: 1, slot: [1, 1], heap: [4, 0, 18], rot: [0, 5, 0] },
    { type: 1, slot: [1, 2], heap: [-16, 13, 0], rot: [-20, 15, 25] },
    { type: 2, slot: [2, 2], heap: [8, 14, 2], rot: [15, -30, -18] },
    { type: 2, slot: [2, 1], heap: [22, 11, -4], rot: [0, 60, 30] },
    { type: 0, slot: [0, 2], heap: [-2, 27, -2], rot: [25, 20, -15] },
  ];

  // frame numbers (25 fps)
  const T = {
    shiver: [7, 8, 9], squash: 10, launch: 13,
    land: (tower, level) => 20 + 3 * level + tower,   // one clack per frame, bottoms first, left to right
    tada: 31, glints: [44, 52],
    reset: 62, resetLand: [68, 69, 69, 70, 70, 72, 72, 73, 74],   // per block, back in the heap
  };
  const P = {
    apex: [30, 40, 50],       // pivot apex height for level 0..2 on the way up
    resetApex: 16,            // extra height over the higher end on the way back
    spin: [[360, 0, 0], [0, 0, -360], [0, 0, 360], [-360, 0, 0], [0, 0, 360], [360, 0, 0], [0, 0, -360], [-360, 0, 0], [0, 0, 360]],
  };

  const heapPos = b => { const [u, y, v] = b.heap; return [(u - v) * S2, y, (u + v) * S2]; };
  const slotPos = b => { const d = (b.slot[0] - 1) * SP; return [d, 16 * b.slot[1], d]; };
  const rigToWorld = p => new THREE.Vector3(...p).applyEuler(new THREE.Euler(0, YAW * RAD, 0));
  const worldToScreen = w => new THREE.Vector3(...(w.toArray ? w.toArray() : w)).sub(O).applyEuler(new THREE.Euler(-PITCH * RAD, 0, 0)).add(O);
  const rigToScreen = p => worldToScreen(rigToWorld(p));

  const NAME = 'tidy_icon_anim';   // saving the .bbmodel renames the tab after the file
  const UUID = 'd9106250-ae55-5454-8f2b-a2e7184f73c3';
  const OWNER = 'tidypockets';
  function lockedByOther() {
    const l = window.BB_LOCK;
    return l && l.owner !== OWNER && l.until > Date.now() ? l.owner : null;
  }
  function use() {
    const other = lockedByOther();
    if (other) throw new Error('BB_LOCK held by ' + other + ', try again later');
    const mine = p => p.uuid === UUID || p.name === NAME;
    if (Project && mine(Project)) return;
    const p = ModelProject.all.find(mine);
    if (!p) throw new Error('no ' + NAME + ' project open');
    p.select();
    if (!mine(Project)) throw new Error('could not select ' + NAME);
  }

  const tex = {};
  function loadTextures() {
    use();
    Texture.all.slice().forEach(t => t.remove(true));
    for (const f of fs.readdirSync(TEX).filter(f => f.endsWith('.png') && /^(redstone|gold|diamond)_|^fx_/.test(f))) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + f).toString('base64');
      tex[f.slice(0, -4)] = new Texture({ name: f }).fromDataURL(url).add(false);
    }
    return Object.keys(tex).join(',');
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
  }

  function group(name, origin, parent, rotation) {
    const g = new Group({ name, origin, rotation: rotation || [0, 0, 0] });
    g.addTo(parent); g.init();
    return g;
  }
  const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
  function cube(name, from, to, parent, faceTex, opts) {
    const c = new Cube(Object.assign({ name, from, to, box_uv: false }, opts || {}));
    c.addTo(parent); c.init();
    for (const f of FACES) {
      const spec = faceTex[f] || faceTex.all;
      if (spec) c.faces[f].extend({ texture: tex[spec[0]].uuid, uv: spec[1] });
      else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  function plane(name, centre, size, parent, texName, uv) {
    const [x, y, z] = centre, h = size / 2;
    return cube(name, [x - h, y - h, z], [x + h, y + h, z], parent, { south: [texName, uv || [0, 0, 16, 16]] });
  }
  const FULL = [0, 0, 16, 16];

  const G = {};
  function build() {
    use();
    ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));

    G.rig = group('rig', [0, 0, 0], undefined, [0, YAW, 0]);
    BLOCKS.forEach((b, i) => {
      const t = TYPES[b.type];
      const g = G['blk_' + i] = group('blk_' + i, [0, 0, 0], G.rig);
      const r = G['blk_' + i + '_rot'] = group('blk_' + i + '_rot', [0, 8, 0], g);
      cube('block_' + i, [-8, 0, -8], [8, 16, 8], r, {
        up: [t + '_top', FULL], west: [t + '_left', FULL], south: [t + '_right', FULL],
        east: [t + '_left', FULL], north: [t + '_right', FULL], down: [t + '_right', FULL],
      });
    });

    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    // a spark per landing, at the landed block's top front corner; a star over each tower for the ta-da
    BLOCKS.forEach((b, i) => {
      const s = slotPos(b), c = rigToScreen([s[0], s[1] + 16, s[2]]);
      G['spark_' + i] = group('spark_' + i, [c.x, c.y, Z_FX + i * 0.1], G.screen);
      plane('spark_plane', [c.x, c.y, Z_FX + i * 0.1], 7, G['spark_' + i], 'fx_spark');
    });
    [0, 1, 2].forEach(k => {
      const d = (k - 1) * SP, c = rigToScreen([d, 48, d]);
      G['star_' + k] = group('star_' + k, [c.x, c.y + 8, Z_FX + 2 + k * 0.1], G.screen);
      plane('star_plane', [c.x, c.y + 8, Z_FX + 2 + k * 0.1], 14, G['star_' + k], 'fx_star');
    });
    [0, 1].forEach(k => {
      G['glint_' + k] = group('glint_' + k, [0, 0, Z_FX + 4], G.screen);
      plane('glint_plane', [0, 0, Z_FX + 4], 7, G['glint_' + k], 'fx_spark');
    });

    Canvas.updateAll();
    return Outliner.elements.length;
  }

  // ---- animation: per-frame samples ----
  let A = null;
  function K(g, ch, f, v) {
    const [x, y, z] = typeof v === 'number' ? [v, v, v] : v;
    A.getBoneAnimator(g).addKeyframe({ channel: ch, time: f * DT, interpolation: 'linear', data_points: [{ x, y, z }] });
  }
  const frames = () => Array.from({ length: FR + 1 }, (_, f) => f);

  // ballistic flight from p0 to p1 over n frames with the pivot apex at apexY: positions for k = 0..n
  function flight(p0, p1, n, apexY) {
    const Tf = n * DT, H = apexY - p0[1], dy = p1[1] - p0[1];
    const g = Math.pow((Math.sqrt(2 * H) + Math.sqrt(2 * (H - dy))) / Tf, 2), vy = Math.sqrt(2 * g * H);
    return Array.from({ length: n + 1 }, (_, k) => {
      const t = k * DT, u = k / n;
      return [p0[0] + (p1[0] - p0[0]) * u, p0[1] + vy * t - g * t * t / 2, p0[2] + (p1[2] - p0[2]) * u];
    });
  }
  const lerp3 = (a, b, u) => a.map((v, i) => v + (b[i] - v) * u);

  // per block, per frame: pos (rig), rot, scale [sx, sy]; plus which tower it stands in (for the lift)
  function tracks() {
    const st = BLOCKS.map(() => ({ pos: [], rot: [], sc: [], tower: [] }));
    BLOCKS.forEach((b, i) => {
      const s = st[i], hp = heapPos(b), sp = slotPos(b), [tw, lv] = b.slot;
      const fl = T.land(tw, lv), fr = T.resetLand[i];
      const up = flight(hp, sp, fl - T.launch, P.apex[lv]);
      const down = flight(sp, hp, fr - T.reset, Math.max(sp[1], hp[1]) + P.resetApex);
      const spin = P.spin[i];
      for (let f = 0; f <= FR; f++) {
        let pos = hp, rot = b.rot, tower = -1;
        if (f > T.launch && f < fl) {
          const k = f - T.launch, u = k / (fl - T.launch);
          pos = up[k]; rot = lerp3(b.rot.map((v, j) => v + spin[j]), [0, 0, 0], u);
        } else if (f >= fl && f <= T.reset) { pos = sp; rot = [0, 0, 0]; tower = tw; }
        else if (f > T.reset && f < fr) {
          const k = f - T.reset, u = k / (fr - T.reset);
          pos = down[k]; rot = lerp3([0, 0, 0], b.rot.map((v, j) => v - spin[j]), u);
        }
        s.pos[f] = pos; s.rot[f] = rot; s.tower[f] = tower; s.sc[f] = [1, 1];
      }
      // heap shiver + squash, launch stretch
      T.shiver.forEach((f, k) => { s.pos[f] = [hp[0] + [0.5, -0.6, 0.4][k] * (i % 2 ? 1 : -1), hp[1], hp[2] + [-0.4, 0.5, -0.5][k]]; });
      s.sc[T.squash] = [1.05, 0.93]; s.sc[T.squash + 1] = [1.1, 0.86]; s.sc[T.squash + 2] = [1.12, 0.84];
      s.sc[T.launch] = [0.88, 1.2]; s.sc[T.launch + 1] = [0.94, 1.1];
      // landing on the tower
      s.sc[fl] = [1.28, 0.72]; s.sc[fl + 1] = [0.9, 1.12]; s.sc[fl + 2] = [1.05, 0.96];
      // back in the heap
      s.sc[T.reset] = [0.88, 1.18]; s.sc[T.reset + 1] = [0.95, 1.08];
      s.sc[fr] = [1.2, 0.8]; s.sc[fr + 1] = [0.94, 1.07]; s.sc[fr + 2] = [1.02, 0.98];
    });
    // a landing squashes the blocks under it a little
    BLOCKS.forEach((b, i) => {
      const [tw, lv] = b.slot, fl = T.land(tw, lv);
      BLOCKS.forEach((o, j) => {
        if (o.slot[0] === tw && o.slot[1] < lv) {
          const k = o.slot[1] === lv - 1 ? 1 : 0.5;
          st[j].sc[fl] = [1 + 0.1 * k, 1 - 0.12 * k]; st[j].sc[fl + 1] = [1 - 0.03 * k, 1 + 0.04 * k];
        }
      });
    });
    // ta-da wave, anticipation before the reset: whole towers
    for (let tw = 0; tw < 3; tw++) {
      const f0 = T.tada + tw;
      const wave = { [f0]: [1.1, 0.88], [f0 + 1]: [0.93, 1.1], [f0 + 2]: [1.03, 0.97], [T.reset - 3]: [1.04, 0.95], [T.reset - 2]: [1.08, 0.9], [T.reset - 1]: [1.1, 0.87] };
      BLOCKS.forEach((b, i) => { if (b.slot[0] === tw) for (const f in wave) st[i].sc[f] = wave[f]; });
    }
    // lift: a block standing in a tower rides on the squash of the blocks below it
    for (let f = 0; f <= FR; f++) {
      BLOCKS.forEach((b, i) => {
        if (st[i].tower[f] < 0) return;
        let lift = 0;
        BLOCKS.forEach((o, j) => { if (o.slot[0] === b.slot[0] && o.slot[1] < b.slot[1] && st[j].tower[f] >= 0) lift += 16 * (st[j].sc[f][1] - 1); });
        st[i].pos[f] = [st[i].pos[f][0], st[i].pos[f][1] + lift, st[i].pos[f][2]];
      });
    }
    return st;
  }

  function animate() {
    use();
    ensureTex(); ensureG();
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    const st = tracks();
    st.forEach((s, i) => frames().forEach(f => {
      const q = f % FR, [sx, sy] = s.sc[q];
      K(G['blk_' + i], 'position', f, s.pos[q]);
      K(G['blk_' + i], 'scale', f, [sx, sy, sx]);
      K(G['blk_' + i + '_rot'], 'rotation', f, s.rot[q]);
    }));
    frames().forEach(f => {
      const q = f % FR;
      BLOCKS.forEach((b, i) => {
        const d = q - T.land(b.slot[0], b.slot[1]);
        K(G['spark_' + i], 'scale', f, [0.7, 1.2, 0.8, 0][d] || 0);
        K(G['spark_' + i], 'rotation', f, [0, 0, d >= 0 && d < 3 ? d * 25 : 0]);
      });
      [0, 1, 2].forEach(k => {
        const d = q - (T.tada + 2 + k);
        K(G['star_' + k], 'scale', f, [0.6, 1.25, 1, 0.5][d] || 0);
        K(G['star_' + k], 'rotation', f, [0, 0, d >= 0 && d < 4 ? d * 12 : 0]);
      });
      T.glints.forEach((g, k) => {
        const d = q - g;
        K(G['glint_' + k], 'scale', f, [0.6, 1.2, 0.8][d] || 0);
      });
    });
    // glints sit on tower tops
    [[1, 2], [2, 1]].forEach(([tw, lv], k) => {
      const d = (tw - 1) * SP, c = rigToScreen([d - 4, 16 * (lv + 1), d + 5]);
      frames().forEach(f => K(G['glint_' + k], 'position', f, [c.x, c.y, 0]));
    });
    Animator.preview();
    return 'animated ' + FR + ' frames';
  }
  function ensureG() {
    if (!Object.keys(G).length) Group.all.forEach(g => { G[g.name] = g; });
  }

  // ---- camera + render ----
  function camera(zoom, pan) {
    use();
    const p = Preview.selected;
    p.setProjectionMode(true);
    const pn = pan || CAM_PAN;
    const pos = CAM_POS.map((v, i) => v + pn[i]), tgt = CAM_TARGET.map((v, i) => v + pn[i]);
    p.camera.position.set(...pos);
    p.controls.target.set(...tgt);
    p.camera.lookAt(...tgt);
    p.camera.zoom = zoom || CAM_ZOOM; p.camera.updateProjectionMatrix();
    p.controls.update();
  }
  function setTime(t) {
    use();
    Timeline.setTime(t);
    Animator.preview();
  }
  function render(first, last, res, dir) {
    use();
    res = res || 1600;
    dir = dir || DIR + 'frames/';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const shot = f => new Promise(done => {
      setTime(f * DT);
      Screencam.advancedScreenshot(Preview.selected, { angle_preset: 'view', resolution: [res, res], anti_aliasing: 'none', shading: false }, url => {
        fs.writeFileSync(dir + 'frame_' + String(f).padStart(3, '0') + '.png', Buffer.from(url.split(',')[1], 'base64'));
        done();
      });
    });
    window.BB_LOCK = { owner: OWNER, until: Date.now() + 120000 };
    return (async () => {
      try { for (let f = first; f <= last; f++) await shot(f); } finally { window.BB_LOCK = null; }
      return `rendered ${first}..${last}`;
    })();
  }
  function scaleSweep() {
    use();
    const bad = [];
    for (let f = 0; f <= FR; f++) {
      setTime(f * DT);
      Group.all.forEach(g => { const s = g.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(g.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }
  function save() {
    use();
    Codecs.project.write(Codecs.project.compile(), DIR + NAME + '.bbmodel');
    return 'saved';
  }
  function setCam(zoom, pan) { CAM_ZOOM = zoom; if (pan) CAM_PAN = pan; }

  return { NAME, use, FPS, DT, LEN, FR, T, P, BLOCKS, PITCH, G, tex, loadTextures, build, animate, camera, render, setTime, scaleSweep, save, setCam, rigToScreen, tracks };
})();
