import { useEffect, useState } from 'react';

import { ComunaDotMap } from '@/components/comuna-dot-map';
import { loadComunas } from '@/lib/dot-map/comunas';
import type { MapFeature } from '@/lib/dot-map/engine';

const GEO_SRC = '/geo/comunas.geojson';

interface ActiveState {
  feature: MapFeature;
  index: number;
}

/**
 * Full-bleed backdrop for the unauthenticated routes.
 *
 * Layering, bottom to top:
 *   1. dot grid (canvas)
 *   2. radial vignette — pulls focus to the centre where the card sits
 *   3. bottom scrim — guarantees contrast for the readout and the footer
 *   4. comuna readout
 */
export function AuthBackdrop() {
  const [active, setActive] = useState<ActiveState | null>(null);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    let cancelled = false;
    // Already cached by ComunaDotMap — this does not cost a second request.
    loadComunas(GEO_SRC)
      .then((features) => {
        if (!cancelled) setTotal(features.length);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
      <ComunaDotMap
        src={GEO_SRC}
        className="absolute inset-0"
        onActiveChange={(feature, index) => setActive({ feature, index })}
      />

      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,transparent_38%,var(--background)_92%)] opacity-90" />
      <div className="absolute inset-x-0 bottom-0 h-2/5 bg-gradient-to-t from-background via-background/70 to-transparent" />

      {active ? (
        <div className="absolute bottom-8 left-8 flex flex-col gap-3 sm:bottom-12 sm:left-12">
          <div key={active.feature.id} className="motion-safe:animate-in motion-safe:fade-in motion-safe:slide-in-from-bottom-1 duration-700">
            <p className="font-mono text-[0.625rem] uppercase tracking-[0.28em] text-muted-foreground">
              Comuna
            </p>
            <p className="font-mono text-5xl font-medium tabular-nums leading-none text-foreground sm:text-6xl">
              {active.feature.id.padStart(2, '0')}
            </p>
            <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted-foreground sm:max-w-sm">
              {active.feature.sublabels.join(' · ')}
            </p>
          </div>

          {total > 1 ? (
            <div className="flex items-end gap-[3px]" role="presentation">
              {Array.from({ length: total }, (_, i) => (
                <span
                  key={i}
                  className={
                    i === active.index
                      ? 'h-3 w-[2px] bg-primary transition-all duration-500'
                      : 'h-1.5 w-[2px] bg-muted-foreground/30 transition-all duration-500'
                  }
                />
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

/* ---------------------------------------------------------------------------
 * Wiring — src/routes/_auth.tsx
 *
 * Mount the backdrop on a *pathless* layout route so /login, /register and /sso
 * are siblings underneath it. The backdrop then never unmounts as the user
 * moves between them: the animation carries straight through the transition
 * instead of restarting on Comuna 1 every time.
 *
 *   import { Outlet, createFileRoute } from '@tanstack/react-router';
 *   import { AuthBackdrop } from '@/components/auth-backdrop';
 *
 *   export const Route = createFileRoute('/_auth')({ component: AuthLayout });
 *
 *   function AuthLayout() {
 *     return (
 *       <div className="relative isolate min-h-svh overflow-hidden bg-background">
 *         <AuthBackdrop />
 *         <main className="relative z-10 grid min-h-svh place-items-center px-6 py-12">
 *           <Outlet />
 *         </main>
 *       </div>
 *     );
 *   }
 *
 * Files: src/routes/_auth/login.tsx, _auth/register.tsx, _auth/sso.tsx
 *
 * `isolate` on the wrapper creates a stacking context, so `z-10` on <main>
 * is enough to keep the form above the canvas without negative z-index tricks.
 * `min-h-svh` (not vh) avoids the mobile browser chrome jump.
 * ------------------------------------------------------------------------- */