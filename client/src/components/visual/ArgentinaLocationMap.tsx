import { useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { IconMapPin } from '@tabler/icons-react';
import { Map, Marker, setWorkerUrl } from 'maplibre-gl';
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url';
setWorkerUrl(workerUrl);
import type { IpLocation } from '@/hooks/use-ip-city';
import { useTheme } from '@/hooks/use-theme';
import { cn } from '@/lib/utils';
import 'maplibre-gl/dist/maplibre-gl.css';

const MAP_STYLES = {
  dark: '/geo/fabrizio-custom-darkmatter-tiles.json',
  light: '/geo/fabrizio-custom-positron-tiles.json',
} as const;
type ResolvedTheme = keyof typeof MAP_STYLES;
const ARGENTINA_CENTER: [number, number] = [-64, -38];
const ARGENTINA_ZOOM = 3.15;
const DESTINATION_ZOOM = 14.75;
const FLY_DELAY_MS = 1100;
const FLY_DURATION_MS = 7000;

/** Bearing en el que aterriza el vuelo; la órbita continúa desde acá. */
const ORBIT_START_BEARING = -25;
const ORBIT_PITCH = 45;
const ORBIT_DEG_PER_SEC = 6; // 360 / 6 = 60s por vuelta

interface ArgentinaLocationMapProps {
  className?: string;
  location: IpLocation & { isResolved: boolean };
}

function systemTheme(): ResolvedTheme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';
}

function subscribeToSystemTheme(onStoreChange: () => void) {
  const media = window.matchMedia('(prefers-color-scheme: dark)');
  media.addEventListener('change', onStoreChange);
  return () => media.removeEventListener('change', onStoreChange);
}

function useResolvedTheme(): ResolvedTheme {
  const { theme } = useTheme();
  const preferredTheme = useSyncExternalStore(
    subscribeToSystemTheme,
    systemTheme,
    (): ResolvedTheme => 'light',
  );
  return theme === 'system' ? preferredTheme : theme;
}

function createLocationMarker() {
  const markerElement = document.createElement('div');
  markerElement.className = 'pointer-events-none';

  const markerVisual = document.createElement('div');
  markerVisual.className =
    'relative flex size-16 scale-75 items-center justify-center opacity-0 transition-all duration-700';

  const pulse = document.createElement('span');
  pulse.className = 'absolute size-16 animate-ping rounded-full bg-primary/20';

  const halo = document.createElement('span');
  halo.className =
    'absolute size-9 rounded-full bg-primary/20 ring-1 ring-primary/40';

  const point = document.createElement('span');
  point.className =
    'relative size-4 rounded-full border-[3px] border-primary-foreground bg-primary';
  point.style.boxShadow =
    '0 0 0 6px color-mix(in oklab, var(--primary) 22%, transparent), 0 8px 24px rgba(0,0,0,0.35)';

  markerVisual.append(pulse, halo, point);
  markerElement.append(markerVisual);

  return { markerElement, markerVisual };
}

export function ArgentinaLocationMap({
  className,
  location,
}: ArgentinaLocationMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const activeStyleRef = useRef<string | null>(null);
  const [showLocationLabel, setShowLocationLabel] = useState(false);
  const resolvedTheme = useResolvedTheme();
  const mapStyle = MAP_STYLES[resolvedTheme];

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !location.isResolved) return;

    const reduceMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;
    const flyDuration = reduceMotion ? 900 : FLY_DURATION_MS;
    const delay = reduceMotion ? 150 : FLY_DELAY_MS;
    const finalPitch = reduceMotion ? 0 : ORBIT_PITCH;
    const finalBearing = reduceMotion ? 0 : ORBIT_START_BEARING;
    const destination: [number, number] = [
      location.longitude,
      location.latitude,
    ];
    const { markerElement, markerVisual } = createLocationMarker();

    const map = new Map({
      container,
      style: mapStyle,
      center: ARGENTINA_CENTER,
      zoom: ARGENTINA_ZOOM,
      bearing: 0,
      pitch: 0,
      interactive: false,
      attributionControl: false,
      renderWorldCopies: false,
    });
    mapRef.current = map;
    activeStyleRef.current = mapStyle;

    const marker = new Marker({ element: markerElement, anchor: 'center' })
      .setLngLat(destination)
      .addTo(map);

    let flyTimer: number | undefined;
    let finishTimer: number | undefined;
    let orbitRaf: number | undefined;
    let lastFrame = 0;
    let orbiting = false;

    const orbitStep = (now: number) => {
      const delta = (now - lastFrame) / 1000;
      lastFrame = now;
      map.setBearing(map.getBearing() + ORBIT_DEG_PER_SEC * delta);
      orbitRaf = requestAnimationFrame(orbitStep);
    };

    // Reinicia el reloj antes de pedir el frame: si la pestaña estuvo oculta,
    // performance.now() avanzó igual y el primer delta sería enorme.
    const resumeOrbit = () => {
      lastFrame = performance.now();
      orbitRaf = requestAnimationFrame(orbitStep);
    };

    const pauseOrbit = () => {
      if (orbitRaf !== undefined) {
        cancelAnimationFrame(orbitRaf);
        orbitRaf = undefined;
      }
    };

    const startOrbit = () => {
      if (reduceMotion) return;
      orbiting = true;
      if (!document.hidden) resumeOrbit();
    };

    // orbiting evita que volver a la pestaña durante el vuelo arranque el giro antes de tiempo.
    const handleVisibility = () => {
      if (!orbiting) return;
      if (document.hidden) pauseOrbit();
      else if (orbitRaf === undefined) resumeOrbit();
    };

    const startFlight = () => {
      flyTimer = window.setTimeout(() => {
        markerVisual.classList.remove('scale-75', 'opacity-0');
        markerVisual.classList.add('scale-100', 'opacity-100');

        map.flyTo({
          center: destination,
          zoom: DESTINATION_ZOOM,
          bearing: finalBearing,
          pitch: finalPitch,
          duration: flyDuration,
          curve: 1.15,
          essential: true,
        });

        finishTimer = window.setTimeout(() => {
          map.jumpTo({
            center: destination,
            zoom: DESTINATION_ZOOM,
            bearing: map.getBearing(),
            pitch: finalPitch,
          });
          setShowLocationLabel(true);
          startOrbit();
        }, flyDuration);
      }, delay);
    };

    map.once('load', startFlight);
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      if (flyTimer !== undefined) window.clearTimeout(flyTimer);
      if (finishTimer !== undefined) window.clearTimeout(finishTimer);
      document.removeEventListener('visibilitychange', handleVisibility);
      pauseOrbit();
      map.off('load', startFlight);
      marker.remove();
      map.remove();
      if (mapRef.current === map) mapRef.current = null;
    };
    // El cambio de tema actualiza el estilo sobre este mismo mapa en el efecto
    // siguiente, sin reiniciar el vuelo ni la orbita.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.isResolved, location.latitude, location.longitude]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || activeStyleRef.current === mapStyle) return;
    activeStyleRef.current = mapStyle;
    map.setStyle(mapStyle);
  }, [mapStyle]);

  return (
    <div className={cn('relative overflow-hidden bg-background', className)}>
      <div
        ref={containerRef}
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
        }}
      />

      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.16),transparent_45%,rgba(37,99,235,0.1))] dark:bg-[linear-gradient(135deg,rgba(9,9,11,0.12),transparent_45%,rgba(9,9,11,0.25))]" />
      <div className="pointer-events-none absolute inset-x-0 top-0 h-32 bg-gradient-to-b from-background/35 to-transparent" />

      <div
        className={cn(
          'pointer-events-none absolute left-1/2 top-1/2 mt-30 flex -translate-x-1/2 items-center gap-3 text-foreground transition-all duration-700',
          showLocationLabel ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0',
        )}
      >
        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg shadow-primary/25">
          <IconMapPin className="size-5" />
        </span>
        <div className="whitespace-nowrap [text-shadow:0_2px_12px_rgba(255,255,255,0.95)] dark:[text-shadow:0_2px_12px_rgba(0,0,0,0.8)]">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-primary">
            Tu ubicación aproximada
          </p>
          <p className="mt-0.5 text-xl font-semibold tracking-tight">
            {location.city}, {location.region}
          </p>
        </div>
      </div>

      <div
        className={cn(
          'pointer-events-none absolute left-1/2 top-1/2 -mt-38 flex -translate-x-1/2 items-center gap-3 text-foreground transition-all duration-700',
          showLocationLabel ? 'translate-y-0 opacity-100' : '-translate-y-2 opacity-0',
        )}
      >
        <div className="whitespace-nowrap [text-shadow:0_2px_12px_rgba(255,255,255,0.95)] dark:[text-shadow:0_2px_12px_rgba(0,0,0,0.8)]">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-primary">
            Conectado desde
          </p>
        </div>
      </div>

      <p className="pointer-events-none absolute bottom-4 left-4 right-4 text-center text-[9px] font-medium text-foreground/80 md:bottom-6 md:left-6 md:right-6">
        © OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors
      </p>
    </div>
  );
}
