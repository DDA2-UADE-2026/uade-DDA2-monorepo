/**
 * Dot-matrix map engine.
 *
 * Renders a full-viewport orthogonal grid of square dots. Dots that fall inside
 * a feature polygon (a CABA comuna) are lit; the engine cycles through features,
 * revealing each one with a wave that radiates from its centroid.
 *
 * Framework-agnostic — no React, no map library. One canvas, one rAF loop.
 *
 * Performance notes:
 *  - Grid membership is computed once per layout (resize), not per frame.
 *  - Per frame the dots are counting-sorted into 24 alpha/colour "slots" so the
 *    draw loop only touches `fillStyle` 24 times instead of ~10.000 times.
 *  - Squares via fillRect, not arcs — roughly 4x cheaper at this dot count.
 */

const RAD = Math.PI / 180;

const IDLE_LEVELS = 8;
const ACTIVE_LEVELS = 16;
const SLOTS = IDLE_LEVELS + ACTIVE_LEVELS;
const BLOOM_FROM = SLOTS - 4;

export type FlatRing = number[]; // [lon, lat, lon, lat, ...]
export type FlatPolygon = FlatRing[]; // [outerRing, ...holes]

export interface MapFeature {
  id: string;
  label: string;
  sublabels: string[];
  polygons: FlatPolygon[];
}

export interface DotMapColors {
  base: string;
  accent: string;
}

export interface DotMapOptions {
  /** Grid spacing in CSS px. Lower = denser = prettier = slower. */
  gap: number;
  /** Base square edge length in CSS px. */
  dotSize: number;
  /** Fraction of the smaller viewport axis the city should occupy. */
  fit: number;
  /** Recentre the city, as a fraction of viewport width/height. */
  offsetX: number;
  offsetY: number;
  /** Time each feature stays active. */
  cycleMs: number;
  /** Ramp time for a single dot to reach full intensity. */
  revealMs: number;
  /** Extra delay between the centroid dot and the outermost dot. */
  spreadMs: number;
  /** Exponential decay constant for dots that just went inactive. */
  fadeMs: number;
  alphaOutside: number;
  alphaIdle: number;
  /** Amplitude of the slow survey sweep across in-city dots. */
  alphaSweep: number;
  /** px added to the edge of a fully-lit dot. */
  activeGrow: number;
  bloom: boolean;
  /** Hard ceiling on dot count; gap is widened automatically to respect it. */
  maxDots: number;
  reducedMotion: boolean;
}

export const DEFAULT_OPTIONS: DotMapOptions = {
  gap: 13,
  dotSize: 2.4,
  fit: 0.86,
  offsetX: 0,
  offsetY: 0,
  cycleMs: 3800,
  revealMs: 620,
  spreadMs: 900,
  fadeMs: 520,
  alphaOutside: 0.07,
  alphaIdle: 0.2,
  alphaSweep: 0.13,
  activeGrow: 2.2,
  bloom: true,
  maxDots: 14000,
  reducedMotion: false,
};

/* ------------------------------------------------------------------ utils */

function ringContains(x: number, y: number, r: Float64Array): boolean {
  let inside = false;
  for (let i = 0, j = r.length - 2; i < r.length; j = i, i += 2) {
    const xi = r[i];
    const yi = r[i + 1];
    const xj = r[j];
    const yj = r[j + 1];
    if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
  }
  return inside;
}

function polysContain(x: number, y: number, polys: Float64Array[][]): boolean {
  for (let p = 0; p < polys.length; p++) {
    const rings = polys[p];
    if (!ringContains(x, y, rings[0])) continue;
    let inHole = false;
    for (let r = 1; r < rings.length; r++) {
      if (ringContains(x, y, rings[r])) {
        inHole = true;
        break;
      }
    }
    if (!inHole) return true;
  }
  return false;
}

/** Deterministic 0..1 from an integer — stable dot texture across resizes. */
function hash01(i: number): number {
  let x = Math.imul(i + 1, 0x9e3779b1);
  x ^= x >>> 15;
  x = Math.imul(x, 0x85ebca6b);
  x ^= x >>> 13;
  return (x >>> 0) / 4294967296;
}

/**
 * Resolve any CSS colour the browser can parse (oklch, hsl, hex, colour
 * function) down to sRGB bytes, by painting one pixel and reading it back.
 * Cheap and only runs on theme change.
 */
export function resolveRgb(color: string, fallback: [number, number, number]): [number, number, number] {
  try {
    const c = document.createElement('canvas');
    c.width = 1;
    c.height = 1;
    const g = c.getContext('2d', { willReadFrequently: true });
    if (!g) return fallback;
    g.fillStyle = '#000';
    g.fillRect(0, 0, 1, 1);
    const before = g.fillStyle;
    g.fillStyle = color;
    if (g.fillStyle === before && color.trim() !== '#000') {
      // The browser rejected the string and kept the previous value.
      const probe = document.createElement('span');
      probe.style.color = color;
      if (!probe.style.color) return fallback;
    }
    g.fillRect(0, 0, 1, 1);
    const d = g.getImageData(0, 0, 1, 1).data;
    return [d[0], d[1], d[2]];
  } catch {
    return fallback;
  }
}

/** Legacy shadcn stores bare HSL channels ("222.2 47.4% 11.2%"). Wrap them. */
export function normalizeCssColor(value: string): string {
  const s = value.trim();
  if (!s) return s;
  if (/^(#|rgb|hsl|oklch|oklab|lab|lch|color|var)/i.test(s)) return s;
  if (/^[\d.]+\s+[\d.]+%\s+[\d.]+%$/.test(s)) return `hsl(${s})`;
  return s;
}

/* ----------------------------------------------------------------- engine */

interface Projected {
  bbox: [number, number, number, number];
  polys: Float64Array[][];
}

export class DotMapEngine {
  onActiveChange?: (index: number, feature: MapFeature | undefined) => void;

  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private opts: DotMapOptions = { ...DEFAULT_OPTIONS };
  private features: MapFeature[] = [];
  private proj: Projected[] = [];
  private hasDots: boolean[] = [];

  private w = 0;
  private h = 0;
  private dpr = 1;

  private n = 0;
  private cap = 0;
  private px = new Float32Array(0);
  private py = new Float32Array(0);
  private owner = new Int16Array(0);
  private dist = new Float32Array(0);
  private size = new Float32Array(0);
  private intensity = new Float32Array(0);
  private slot = new Uint8Array(0);
  private order = new Int32Array(0);
  private counts = new Int32Array(SLOTS);
  private offsets = new Int32Array(SLOTS);
  private cursor = new Int32Array(SLOTS);

  private baseRgb: [number, number, number] = [148, 163, 184];
  private accentRgb: [number, number, number] = [56, 189, 248];
  private palette: string[] = [];
  private bloomPalette: string[] = [];

  private active = 0;
  private activeStart = 0;
  private last = 0;
  private raf = 0;
  private running = false;

  constructor(canvas: HTMLCanvasElement, options?: Partial<DotMapOptions>) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) throw new Error('DotMapEngine: 2D context unavailable');
    this.ctx = ctx;
    if (options) Object.assign(this.opts, options);
    this.buildPalette();
  }

  setOptions(options: Partial<DotMapOptions>) {
    const needsLayout =
      ('gap' in options && options.gap !== this.opts.gap) ||
      ('fit' in options && options.fit !== this.opts.fit) ||
      ('dotSize' in options && options.dotSize !== this.opts.dotSize) ||
      ('offsetX' in options && options.offsetX !== this.opts.offsetX) ||
      ('offsetY' in options && options.offsetY !== this.opts.offsetY) ||
      ('maxDots' in options && options.maxDots !== this.opts.maxDots);
    Object.assign(this.opts, options);
    this.buildPalette();
    if (needsLayout) this.layout();
    if (!this.running) this.paint();
  }

  setColors(colors: DotMapColors) {
    this.baseRgb = resolveRgb(normalizeCssColor(colors.base), this.baseRgb);
    this.accentRgb = resolveRgb(normalizeCssColor(colors.accent), this.accentRgb);
    this.buildPalette();
    if (!this.running) this.paint();
  }

  setFeatures(features: MapFeature[]) {
    this.features = features;
    this.active = 0;
    this.activeStart = performance.now();
    this.layout();
    this.onActiveChange?.(this.active, this.features[this.active]);
  }

  resize(width: number, height: number, dpr = 1) {
    const w = Math.max(1, Math.round(width));
    const h = Math.max(1, Math.round(height));
    const d = Math.max(1, Math.min(dpr, 2));
    if (w === this.w && h === this.h && d === this.dpr) return;
    this.w = w;
    this.h = h;
    this.dpr = d;
    this.canvas.width = Math.round(w * d);
    this.canvas.height = Math.round(h * d);
    this.canvas.style.width = `${w}px`;
    this.canvas.style.height = `${h}px`;
    this.layout();
    if (!this.running) this.paint();
  }

  start() {
    if (this.running || !this.w) return;
    this.running = true;
    const now = performance.now();
    if (this.last) this.activeStart += now - this.last; // resume where we paused
    this.last = now;
    const loop = (t: number) => {
      if (!this.running) return;
      this.frame(t);
      this.raf = requestAnimationFrame(loop);
    };
    this.raf = requestAnimationFrame(loop);
  }

  stop() {
    this.running = false;
    cancelAnimationFrame(this.raf);
  }

  destroy() {
    this.stop();
    this.features = [];
    this.proj = [];
  }

  get activeIndex() {
    return this.active;
  }

  setActive(index: number) {
    if (!this.features.length) return;
    this.active = ((index % this.features.length) + this.features.length) % this.features.length;
    this.activeStart = performance.now();
    this.onActiveChange?.(this.active, this.features[this.active]);
  }

  /* ------------------------------------------------------------- internals */

  private buildPalette() {
    const [br, bg, bb] = this.baseRgb;
    const [ar, ag, ab] = this.accentRgb;
    const { alphaIdle, alphaSweep } = this.opts;
    const maxIdle = alphaIdle + alphaSweep;

    const p: string[] = [];
    for (let s = 0; s < IDLE_LEVELS; s++) {
      const a = maxIdle * (s / (IDLE_LEVELS - 1));
      p.push(`rgba(${br},${bg},${bb},${a.toFixed(3)})`);
    }
    for (let k = 0; k < ACTIVE_LEVELS; k++) {
      const t = k / (ACTIVE_LEVELS - 1);
      const mix = Math.min(1, t * 1.35); // reach accent hue before peak alpha
      const r = Math.round(br + (ar - br) * mix);
      const g = Math.round(bg + (ag - bg) * mix);
      const b = Math.round(bb + (ab - bb) * mix);
      const a = alphaIdle + (1 - alphaIdle) * t;
      p.push(`rgba(${r},${g},${b},${a.toFixed(3)})`);
    }
    this.palette = p;

    this.bloomPalette = [];
    for (let s = BLOOM_FROM; s < SLOTS; s++) {
      const t = (s - IDLE_LEVELS) / (ACTIVE_LEVELS - 1);
      this.bloomPalette.push(`rgba(${ar},${ag},${ab},${(0.055 * t).toFixed(3)})`);
    }
  }

  private alloc(n: number) {
    if (n <= this.cap) {
      this.n = n;
      return;
    }
    this.cap = n;
    this.n = n;
    this.px = new Float32Array(n);
    this.py = new Float32Array(n);
    this.owner = new Int16Array(n);
    this.dist = new Float32Array(n);
    this.size = new Float32Array(n);
    this.intensity = new Float32Array(n);
    this.slot = new Uint8Array(n);
    this.order = new Int32Array(n);
  }

  private layout() {
    const { w, h } = this;
    if (!w || !h) {
      this.n = 0;
      return;
    }

    const opts = this.opts;

    // 1. Web Mercator bounds over every ring.
    let mnx = Infinity;
    let mny = Infinity;
    let mxx = -Infinity;
    let mxy = -Infinity;
    for (const f of this.features) {
      for (const poly of f.polygons) {
        for (const ring of poly) {
          for (let i = 0; i < ring.length; i += 2) {
            const x = ring[i] * RAD;
            const y = Math.log(Math.tan(Math.PI / 4 + (ring[i + 1] * RAD) / 2));
            if (x < mnx) mnx = x;
            if (x > mxx) mxx = x;
            if (y < mny) mny = y;
            if (y > mxy) mxy = y;
          }
        }
      }
    }

    // 2. Project every ring into screen space once, and cache feature bboxes.
    if (this.features.length && Number.isFinite(mnx)) {
      const bw = mxx - mnx || 1;
      const bh = mxy - mny || 1;
      const scale = opts.fit * Math.min(w / bw, h / bh);
      const cx = w / 2 + opts.offsetX * w;
      const cy = h / 2 + opts.offsetY * h;
      const midX = (mnx + mxx) / 2;
      const midY = (mny + mxy) / 2;

      this.proj = this.features.map((f) => {
        let x0 = Infinity;
        let y0 = Infinity;
        let x1 = -Infinity;
        let y1 = -Infinity;
        const polys = f.polygons.map((poly) =>
          poly.map((ring) => {
            const out = new Float64Array(ring.length);
            for (let i = 0; i < ring.length; i += 2) {
              const X = cx + (ring[i] * RAD - midX) * scale;
              const Y = cy - (Math.log(Math.tan(Math.PI / 4 + (ring[i + 1] * RAD) / 2)) - midY) * scale;
              out[i] = X;
              out[i + 1] = Y;
              if (X < x0) x0 = X;
              if (X > x1) x1 = X;
              if (Y < y0) y0 = Y;
              if (Y > y1) y1 = Y;
            }
            return out;
          }),
        );
        return { bbox: [x0, y0, x1, y1] as [number, number, number, number], polys };
      });
    } else {
      this.proj = [];
    }

    // 3. Grid, widened if it would exceed the dot budget.
    let gap = opts.gap;
    const estimate = (Math.ceil(w / gap) + 1) * (Math.ceil(h / gap) + 1);
    if (estimate > opts.maxDots) gap *= Math.sqrt(estimate / opts.maxDots);

    const cols = Math.ceil(w / gap) + 1;
    const rows = Math.ceil(h / gap) + 1;
    const ox = (w - (cols - 1) * gap) / 2;
    const oy = (h - (rows - 1) * gap) / 2;
    this.alloc(cols * rows);

    const nf = this.proj.length;
    const sumX = new Float64Array(nf);
    const sumY = new Float64Array(nf);
    const count = new Float64Array(nf);

    let k = 0;
    for (let r = 0; r < rows; r++) {
      const y = oy + r * gap;
      for (let c = 0; c < cols; c++, k++) {
        const x = ox + c * gap;
        this.px[k] = x;
        this.py[k] = y;

        let own = -1;
        for (let fi = 0; fi < nf; fi++) {
          const b = this.proj[fi].bbox;
          if (x < b[0] || x > b[2] || y < b[1] || y > b[3]) continue;
          if (polysContain(x, y, this.proj[fi].polys)) {
            own = fi;
            break;
          }
        }
        this.owner[k] = own;
        if (own >= 0) {
          sumX[own] += x;
          sumY[own] += y;
          count[own]++;
        }

        const rnd = hash01(k);
        this.size[k] = opts.dotSize * (own >= 0 ? 0.92 + 0.22 * rnd : 0.7 + 0.5 * rnd);
        this.intensity[k] = 0;
      }
    }

    // 4. Normalised distance from each dot to its feature centroid — this is
    //    what staggers the reveal wave.
    const cxs = new Float64Array(nf);
    const cys = new Float64Array(nf);
    const maxd = new Float64Array(nf);
    this.hasDots = [];
    for (let fi = 0; fi < nf; fi++) {
      this.hasDots.push(count[fi] > 0);
      if (count[fi] > 0) {
        cxs[fi] = sumX[fi] / count[fi];
        cys[fi] = sumY[fi] / count[fi];
      }
    }
    const n = this.n;
    for (let i = 0; i < n; i++) {
      const o = this.owner[i];
      if (o < 0) {
        this.dist[i] = 0;
        continue;
      }
      const dx = this.px[i] - cxs[o];
      const dy = this.py[i] - cys[o];
      const d = Math.sqrt(dx * dx + dy * dy);
      this.dist[i] = d;
      if (d > maxd[o]) maxd[o] = d;
    }
    for (let i = 0; i < n; i++) {
      const o = this.owner[i];
      if (o >= 0 && maxd[o] > 0) this.dist[i] /= maxd[o];
    }
  }

  private advance(now: number) {
    const total = this.features.length;
    if (!total) return;
    for (let step = 1; step <= total; step++) {
      const next = (this.active + step) % total;
      if (this.hasDots[next] !== false) {
        this.active = next;
        break;
      }
    }
    this.activeStart = now;
    this.onActiveChange?.(this.active, this.features[this.active]);
  }

  /** One static repaint — used when paused, resized or re-themed. */
  private paint() {
    this.frame(performance.now(), true);
  }

  private frame(now: number, still = false) {
    const gap = now - this.last;
    // Tab was backgrounded (or we just resumed): don't burn the whole cycle.
    if (gap > 500) this.activeStart += gap;
    const dt = still ? 0 : Math.min(64, gap || 16);
    this.last = now;

    const { ctx, w, h, n } = this;
    ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    if (!n) return;

    const o = this.opts;
    const rm = o.reducedMotion;
    const elapsed = now - this.activeStart;
    const decay = dt ? 1 - Math.exp(-dt / (rm ? o.fadeMs * 1.6 : o.fadeMs)) : 0;
    const spread = rm ? 0 : o.spreadMs;
    const reveal = rm ? o.revealMs * 1.6 : o.revealMs;
    const active = this.features.length ? this.active : -1;
    const maxIdle = o.alphaIdle + o.alphaSweep;
    const outsideSlot = Math.round((o.alphaOutside / maxIdle) * (IDLE_LEVELS - 1));
    const sweepPhase = now * 0.00096;

    // --- classify -----------------------------------------------------------
    this.counts.fill(0);
    for (let i = 0; i < n; i++) {
      const own = this.owner[i];
      let inten = this.intensity[i];

      if (own === active) {
        const local = (elapsed - this.dist[i] * spread) / reveal;
        const t = local <= 0 ? 0 : local >= 1 ? 1 : local * local * (3 - 2 * local);
        if (t > inten) inten = t;
      } else if (inten > 0.001) {
        inten -= inten * decay;
      } else {
        inten = 0;
      }
      this.intensity[i] = inten;

      let s: number;
      if (inten > 0.02) {
        s = IDLE_LEVELS + ((inten * (ACTIVE_LEVELS - 1) + 0.5) | 0);
        if (s >= SLOTS) s = SLOTS - 1;
      } else if (own < 0) {
        s = outsideSlot;
      } else {
        let a = o.alphaIdle;
        if (!rm && o.alphaSweep > 0) {
          // Narrow band sweeping diagonally across the city — the "survey line".
          let b = Math.sin((this.px[i] * 0.55 + this.py[i]) * 0.01 - sweepPhase) * 0.5 + 0.5;
          const b2 = b * b;
          b = b2 * b2 * b2;
          a += o.alphaSweep * b;
        }
        s = ((a / maxIdle) * (IDLE_LEVELS - 1) + 0.5) | 0;
      }
      this.slot[i] = s;
      this.counts[s]++;
    }

    // --- counting sort into slot order (allocation free) --------------------
    let acc = 0;
    for (let s = 0; s < SLOTS; s++) {
      this.offsets[s] = acc;
      this.cursor[s] = acc;
      acc += this.counts[s];
    }
    for (let i = 0; i < n; i++) this.order[this.cursor[this.slot[i]]++] = i;

    // --- draw: bloom underlay, then dots ------------------------------------
    if (o.bloom && !rm) {
      for (let s = BLOOM_FROM; s < SLOTS; s++) {
        const c = this.counts[s];
        if (!c) continue;
        ctx.fillStyle = this.bloomPalette[s - BLOOM_FROM];
        const start = this.offsets[s];
        for (let k = start; k < start + c; k++) {
          const i = this.order[k];
          const sz = (this.size[i] + o.activeGrow) * 3;
          ctx.fillRect(this.px[i] - sz / 2, this.py[i] - sz / 2, sz, sz);
        }
      }
    }

    for (let s = 1; s < SLOTS; s++) {
      const c = this.counts[s];
      if (!c) continue;
      ctx.fillStyle = this.palette[s];
      const grow = s >= IDLE_LEVELS ? (o.activeGrow * (s - IDLE_LEVELS)) / (ACTIVE_LEVELS - 1) : 0;
      const start = this.offsets[s];
      for (let k = start; k < start + c; k++) {
        const i = this.order[k];
        const sz = this.size[i] + grow;
        ctx.fillRect(this.px[i] - sz / 2, this.py[i] - sz / 2, sz, sz);
      }
    }

    if (!still && elapsed >= (rm ? o.cycleMs * 2 : o.cycleMs)) this.advance(now);
  }
}