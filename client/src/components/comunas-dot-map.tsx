/* eslint-disable react-refresh/only-export-components */
import { useEffect, useRef, useState } from 'react';
import { IconLoader } from '@tabler/icons-react';

import { cn } from '@/lib/utils';

export type FlatRing = number[];
export type FlatPolygon = FlatRing[];

export interface MapFeature {
  id: string;
  label: string;
  sublabels: string[];
  polygons: FlatPolygon[];
}

const ACCENTS: Record<string, string> = {
  Constitucion: 'Constitución',
  'San Nicolas': 'San Nicolás',
  Nunez: 'Núñez',
  'Velez Sarsfield': 'Vélez Sarsfield',
  'Villa Ortuzar': 'Villa Ortúzar',
  'Villa Pueyrredon': 'Villa Pueyrredón',
  'Villa Gral. Mitre': 'Villa General Mitre',
  'Villa General Mitre': 'Villa General Mitre',
  Agronomia: 'Agronomía',
  'Parque Avellaneda': 'Parque Avellaneda',
  'Nueva Pompeya': 'Nueva Pompeya',
};

function prettyBarrio(name: string): string {
  const trimmed = name.trim();
  return ACCENTS[trimmed] ?? trimmed;
}

function flattenPolygon(coords: unknown): FlatPolygon {
  const rings = Array.isArray(coords) ? (coords as number[][][]) : [];
  return rings.map((ring) => {
    const flat: number[] = new Array(ring.length * 2);
    for (let i = 0; i < ring.length; i++) {
      flat[i * 2] = ring[i][0];
      flat[i * 2 + 1] = ring[i][1];
    }
    return flat;
  });
}

interface GeoFeature {
  geometry?: { type?: string; coordinates?: unknown };
  properties?: Record<string, unknown>;
  id?: unknown;
}

function parseComunas(geojson: unknown): MapFeature[] {
  const fc = geojson as { features?: GeoFeature[] };
  const out: MapFeature[] = [];

  for (const f of fc?.features ?? []) {
    const geom = f.geometry;
    if (!geom?.coordinates) continue;

    const props = f.properties ?? {};
    const rawId = props.comuna ?? props.COMUNAS ?? props.COMUNA ?? props.id ?? f.id;
    const num = Number(rawId);
    const id = Number.isFinite(num) ? String(num) : String(rawId ?? out.length + 1);

    const rawBarrios = String(props.barrios ?? props.BARRIOS ?? '');
    const sublabels = rawBarrios
      .split(',')
      .map(prettyBarrio)
      .filter(Boolean)
      .sort((a, b) => a.localeCompare(b, 'es'));

    let polygons: FlatPolygon[] = [];
    if (geom.type === 'Polygon') {
      polygons = [flattenPolygon(geom.coordinates)];
    } else if (geom.type === 'MultiPolygon') {
      polygons = (geom.coordinates as unknown[]).map(flattenPolygon);
    }
    polygons = polygons.filter((p) => p.length && p[0].length >= 6);
    if (!polygons.length) continue;

    out.push({ id, label: `Comuna ${id}`, sublabels, polygons });
  }

  out.sort((a, b) => Number(a.id) - Number(b.id));
  return out;
}

const comunasCache = new Map<string, Promise<MapFeature[]>>();

function loadComunas(url: string): Promise<MapFeature[]> {
  let pending = comunasCache.get(url);
  if (!pending) {
    pending = fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        return res.json();
      })
      .then(parseComunas)
      .catch((err) => {
        comunasCache.delete(url); // let a later mount retry
        throw err;
      });
    comunasCache.set(url, pending);
  }
  return pending;
}

const RAD = Math.PI / 180;

const IDLE_LEVELS = 8;
const ACTIVE_LEVELS = 16;
const SLOTS = IDLE_LEVELS + ACTIVE_LEVELS;
const BLOOM_FROM = SLOTS - 4;

export interface DotMapColors {
  base: string;
  accent: string;
}

export interface DotMapOptions {
  gap: number;
  dotSize: number;
  fit: number;
  offsetX: number;
  offsetY: number;
  cycleMs: number;
  revealMs: number;
  spreadMs: number;
  fadeMs: number;
  alphaOutside: number;
  alphaIdle: number;
  alphaSweep: number;
  activeGrow: number;
  bloom: boolean;
  maxDots: number;
  reducedMotion: boolean;
}

const DEFAULT_OPTIONS: DotMapOptions = {
  gap: 13,
  dotSize: 2.4,
  fit: 0.86,
  offsetX: 0,
  offsetY: 0,
  cycleMs: 4600,
  revealMs: 760,
  spreadMs: 1080,
  fadeMs: 620,
  alphaOutside: 0.20,
  alphaIdle: 0.60,
  alphaSweep: 0.13,
  activeGrow: 3.4,
  bloom: true,
  maxDots: 20000,
  reducedMotion: false,
};

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

function hash01(i: number): number {
  let x = Math.imul(i + 1, 0x9e3779b1);
  x ^= x >>> 15;
  x = Math.imul(x, 0x85ebca6b);
  x ^= x >>> 13;
  return (x >>> 0) / 4294967296;
}

function resolveRgb(color: string, fallback: [number, number, number]): [number, number, number] {
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

function normalizeCssColor(value: string): string {
  const s = value.trim();
  if (!s) return s;
  if (/^(#|rgb|hsl|oklch|oklab|lab|lch|color|var)/i.test(s)) return s;
  if (/^[\d.]+\s+[\d.]+%\s+[\d.]+%$/.test(s)) return `hsl(${s})`;
  return s;
}

interface Projected {
  bbox: [number, number, number, number];
  polys: Float64Array[][];
}

class DotMapEngine {
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
  private wantsToRun = false;

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
    this.baseRgb = resolveRgb(normalizeCssColor(colors.accent), this.baseRgb);
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
    if (this.wantsToRun && !this.running) this.start();
    else if (!this.running) this.paint();
  }

  start() {
    this.wantsToRun = true;
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
    this.wantsToRun = false;
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
      const mix = Math.min(1, t * 1.6);
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
      this.bloomPalette.push(`rgba(${ar},${ag},${ab},${(0.11 * t).toFixed(3)})`);
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
  private paint() {
    this.frame(performance.now(), true);
  }

  private frame(now: number, still = false) {
    const gap = now - this.last;
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

    this.counts.fill(0);
    for (let i = 0; i < n; i++) {
      const own = this.owner[i];
      let inten = this.intensity[i];

      if (active >= 0 && own === active) {
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

    let acc = 0;
    for (let s = 0; s < SLOTS; s++) {
      this.offsets[s] = acc;
      this.cursor[s] = acc;
      acc += this.counts[s];
    }
    for (let i = 0; i < n; i++) this.order[this.cursor[this.slot[i]]++] = i;

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

export interface ComunasDotMapProps {
  src?: string;
  className?: string;
  options?: Partial<DotMapOptions>;
  paused?: boolean;
  onActiveChange?: (feature: MapFeature, index: number) => void;
}

function readColors(el: HTMLElement): DotMapColors {
  const cs = getComputedStyle(el);
  const pick = (...names: string[]) => {
    for (const n of names) {
      const v = cs.getPropertyValue(n).trim();
      if (v) return v;
    }
    return '';
  };
  return {
    base: pick('--dotmap-base', '--muted-foreground', '--foreground') || '#94a3b8',
    accent: pick('--dotmap-accent', '--primary') || '#38bdf8',
  };
}

export function ComunasDotMap({
  src = '/geo/comunas.geojson',
  className,
  options,
  paused = false,
  onActiveChange,
}: ComunasDotMapProps) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const engineRef = useRef<DotMapEngine | null>(null);
  const callbackRef = useRef(onActiveChange);
  const pausedRef = useRef(paused);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    callbackRef.current = onActiveChange;
  }, [onActiveChange]);

  useEffect(() => {
    const host = hostRef.current;
    const canvas = canvasRef.current;
    if (!host || !canvas) return;

    const engine = new DotMapEngine(canvas);
    engineRef.current = engine;
    engine.onActiveChange = (index, feature) => {
      if (feature) callbackRef.current?.(feature, index);
    };

    const applyColors = () => engine.setColors(readColors(host));
    applyColors();

    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const applyMotion = () => engine.setOptions({ reducedMotion: motionQuery.matches });
    applyMotion();
    motionQuery.addEventListener('change', applyMotion);

    const themeObserver = new MutationObserver(applyColors);
    themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class', 'style', 'data-theme'],
    });

    let resizeFrame = 0;
    const observer = new ResizeObserver((entries) => {
      const rect = entries[0]?.contentRect;
      if (!rect) return;
      cancelAnimationFrame(resizeFrame);
      resizeFrame = requestAnimationFrame(() => {
        engine.resize(rect.width, rect.height, window.devicePixelRatio || 1);
      });
    });
    observer.observe(host);

    const handleVisibility = () => {
      if (document.hidden) engine.stop();
      else if (!pausedRef.current) engine.start();
    };
    document.addEventListener('visibilitychange', handleVisibility);

    if (!pausedRef.current) engine.start();

    return () => {
      cancelAnimationFrame(resizeFrame);
      observer.disconnect();
      themeObserver.disconnect();
      motionQuery.removeEventListener('change', applyMotion);
      document.removeEventListener('visibilitychange', handleVisibility);
      engine.destroy();
      engineRef.current = null;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    loadComunas(src)
      .then((features) => {
        if (cancelled) return;
        engineRef.current?.setFeatures(features);
        setReady(true);
      })
      .catch((error) => {
        if (cancelled) return;
        console.warn(`[ComunasDotMap] could not load ${src}`, error);
        setReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, [src]);

  const optionsKey = JSON.stringify(options ?? {});
  useEffect(() => {
    engineRef.current?.setOptions(JSON.parse(optionsKey) as Partial<DotMapOptions>);
  }, [optionsKey]);

  useEffect(() => {
    pausedRef.current = paused;
    const engine = engineRef.current;
    if (!engine) return;
    if (paused || document.hidden) engine.stop();
    else engine.start();
  }, [paused]);

  return (
    <div ref={hostRef} aria-hidden className={cn('relative h-full w-full overflow-hidden', className)}>
      <canvas
        ref={canvasRef}
        className={cn(
          'block h-full w-full transition-opacity duration-1200 ease-out',
          ready ? 'opacity-100' : 'opacity-0',
        )}
      />
      <div
        className={cn(
          'absolute inset-0 flex items-center justify-center transition-opacity duration-300 ease-out',
          ready ? 'pointer-events-none opacity-0' : 'opacity-100',
        )}
      >
        <IconLoader className="size-6 animate-spin text-muted-foreground" />
      </div>
    </div>
  );
}
