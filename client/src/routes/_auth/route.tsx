import { Suspense, lazy, useEffect, useState } from 'react';
import { Outlet, createFileRoute } from '@tanstack/react-router';

import { BackendStatusPill } from '@/components/BackendStatusPill';
import { RouteErrorPage } from '@/components/errors/RouteErrorPage';

const SideRaysBackground = lazy(() => import('@/components/visual/SideRaysBackground'));
const ComunasMapPanel = lazy(() => import('@/components/auth/ComunasMapPanel'));
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
  errorComponent: RouteErrorPage,
});

function AuthLayout() {
  const showMap = useShowMapPanel();

  return (
    <div className="grid min-h-svh bg-background md:grid-cols-2">
      <main className="flex min-h-svh items-center justify-center px-6 py-10 md:min-h-0 md:py-12 relative! not-order-2">
        <div className="absolute inset-0 pointer-events-none z-0! dark:opacity-100 opacity-45">
          <Suspense fallback={null}>
            <SideRaysBackground
              speed={2.5}
              rayColor1={'#2b7fff'}
              rayColor2={'#3c3cfa'}
              intensity={2}
              spread={2}
              origin="top-right"
              tilt={0}
              saturation={1.5}
              blend={0.75}
              falloff={1.6}
              opacity={1}
            />
          </Suspense>
        </div>
        <div className="flex w-full flex-col items-center gap-4">
          <Outlet />
          <BackendStatusPill />
        </div>
      </main>

      {showMap ? (
        <div className="relative hidden border-l border-border md:block">
          <Suspense fallback={
            <div className="h-full w-full bg-background" />
          }>
            <ComunasMapPanel />
          </Suspense>
        </div>
      ) : null}
    </div>
  );
}
