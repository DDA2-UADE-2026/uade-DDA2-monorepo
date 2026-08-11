import { useEffect, useRef, useState } from 'react';

import { cn } from '@/lib/utils';
import { loadComunas } from '@/lib/dot-map/comunas';
import { DotMapEngine, type DotMapColors, type DotMapOptions, type MapFeature } from '@/lib/dot-map/engine';

export interface ComunaDotMapProps {
  /** Path to the simplified comunas GeoJSON in /public. */
  src?: string;
  className?: string;
  options?: Partial<DotMapOptions>;
  paused?: boolean;
  onActiveChange?: (feature: MapFeature, index: number) => void;
}

/**
 * Reads colours from CSS custom properties so the backdrop follows the theme.
 * Set --dotmap-base / --dotmap-accent to override; otherwise it borrows the
 * shadcn tokens.
 */
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

export function ComunaDotMap({
  src = '/geo/comunas.geojson',
  className,
  options,
  paused = false,
  onActiveChange,
}: ComunaDotMapProps) {
  const hostRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const engineRef = useRef<DotMapEngine | null>(null);
  const callbackRef = useRef(onActiveChange);
  const pausedRef = useRef(paused);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    callbackRef.current = onActiveChange;
  }, [onActiveChange]);

  // Engine lifecycle: created once, kept alive across route changes.
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

    // shadcn toggles `.dark` on <html>; re-resolve the palette when it changes.
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

  // Geometry. Until it resolves the engine still renders the ambient grid, so a
  // slow or failed fetch degrades to a plain dot field rather than a blank page.
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
        console.warn(`[ComunaDotMap] could not load ${src}`, error);
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
          'block h-full w-full transition-opacity duration-[1200ms] ease-out',
          ready ? 'opacity-100' : 'opacity-0',
        )}
      />
    </div>
  );
}