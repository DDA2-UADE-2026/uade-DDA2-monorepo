import { Suspense, lazy, useEffect, useState } from 'react';
import { Outlet, createFileRoute } from '@tanstack/react-router';

// Keeps the dot-map engine, canvas and GeoJSON parsing out of the initial
// bundle — the auth routes need to render the form immediately.
const ComunasMapPanel = lazy(() => import('@/components/comunas-map-panel'));

// Tailwind's `md` breakpoint — kept as a constant so the JS mount gate and
// the `md:` classes below can't drift apart.
const MAP_BREAKPOINT = '(min-width: 768px)';

/** Only mount the map panel once there's actually room to show it split
 *  side-by-side. On mobile it's not shown at all — a decorative map banner
 *  competing with the form for a short, scrollable viewport is worse than
 *  no map, and skipping the mount avoids paying for the lazy chunk, the
 *  GeoJSON fetch and the rAF loop on phones. */
function useShowMapPanel() {
  const [show, setShow] = useState(() => window.matchMedia(MAP_BREAKPOINT).matches);
  useEffect(() => {
    const mq = window.matchMedia(MAP_BREAKPOINT);
    const onChange = () => setShow(mq.matches);
    onChange();
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);
  return show;
}

export const Route = createFileRoute('/_auth')({
  component: AuthLayout,
});

/**
 * Split-screen auth layout: form left, map right, at `md` and up. Below
 * `md` the map panel isn't rendered — just the centered form, full height.
 *
 * `_auth` stays pathless (leading underscore) so this layout — and the map's
 * animation cycle inside it — never unmounts while navigating between
 * /login, /register, /sso, etc.
 */
function AuthLayout() {
  const showMap = useShowMapPanel();

  return (
    <div className="grid min-h-svh bg-background md:grid-cols-2">
      <main className="flex min-h-svh items-center justify-center px-6 py-10 md:min-h-0 md:py-12">
        <Outlet />
      </main>

      {showMap ? (
        <div className="relative hidden border-l border-border md:block">
          <Suspense fallback={<div className="h-full w-full bg-background" />}>
            <ComunasMapPanel />
          </Suspense>
        </div>
      ) : null}
    </div>
  );
}
