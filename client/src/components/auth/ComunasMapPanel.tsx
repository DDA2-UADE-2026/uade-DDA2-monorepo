import { Suspense, lazy } from 'react';

import { useIpLocation } from '@/hooks/use-ip-city';

const ArgentinaLocationMap = lazy(() =>
  import('@/components/visual/ArgentinaLocationMap').then((module) => ({
    default: module.ArgentinaLocationMap,
  })),
);

export default function ComunasMapPanel() {
  const ipLocation = useIpLocation();

  return (
    <div
      aria-hidden
      className="pointer-events-none relative h-full w-full overflow-hidden bg-background"
    >
      <Suspense fallback={<div className="absolute inset-0 bg-background" />}>
        <ArgentinaLocationMap
          key={ipLocation.latitude + ':' + ipLocation.longitude}
          className="absolute inset-0"
          location={ipLocation}
        />
      </Suspense>

      <div className="absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t from-background/90 via-background/35 to-transparent sm:h-1/2" />
      <div className="absolute inset-y-0 left-0 w-1/3 bg-gradient-to-r from-background/25 to-transparent" />
    </div>
  );
}
