/**
 * Parser + cached loader for the official CABA comunas GeoJSON.
 *
 * Source: Buenos Aires Data → dataset "comunas" → recurso GeoJSON.
 * https://data.buenosaires.gob.ar/dataset/comunas
 *
 * The file is CRS84 (plain lon/lat), and each feature carries:
 *   { id, objeto: "COMUNA", comuna: 1, barrios: "Constitucion, Monserrat, ...",
 *     perimetro, area }
 *
 * The published barrio names are unaccented, so we restore the accents for
 * display — this UI is read by porteños and "Nunez" looks broken to them.
 */

import type { FlatPolygon, MapFeature } from './engine';

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

export function parseComunas(geojson: unknown): MapFeature[] {
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

const cache = new Map<string, Promise<MapFeature[]>>();

export function loadComunas(url: string): Promise<MapFeature[]> {
  let pending = cache.get(url);
  if (!pending) {
    pending = fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        return res.json();
      })
      .then(parseComunas)
      .catch((err) => {
        cache.delete(url); // let a later mount retry
        throw err;
      });
    cache.set(url, pending);
  }
  return pending;
}