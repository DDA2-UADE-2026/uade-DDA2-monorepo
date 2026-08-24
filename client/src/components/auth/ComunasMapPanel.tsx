import { Suspense, lazy } from 'react';
import { IconMapPin } from '@tabler/icons-react';

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
      className="dark pointer-events-none relative h-full w-full overflow-hidden bg-background"
    >
      <Suspense fallback={<div className="absolute inset-0 bg-zinc-950" />}>
        <ArgentinaLocationMap
          className="absolute inset-0"
          location={ipLocation}
        />
      </Suspense>

      <div className="absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t from-zinc-950/90 via-zinc-950/35 to-transparent sm:h-1/2" />
      <div className="absolute inset-y-0 left-0 w-1/3 bg-gradient-to-r from-zinc-950/25 to-transparent" />

      <div className="absolute inset-x-4 bottom-4 sm:inset-x-6 sm:bottom-6 md:inset-x-8 md:bottom-8 lg:inset-x-12 lg:bottom-12">
        <div className="max-w-xs rounded-2xl bg-zinc-950/45 p-4 text-white shadow-2xl backdrop-blur-sm">
          <div className="flex items-center gap-3.5">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-500 text-white">
              <IconMapPin className="size-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[10px] font-semibold uppercase text-blue-300">
                Tu ubicación aproximada
              </p>
              <p className="truncate text-base font-semibold text-white translate-y-px">
                {ipLocation.city}, {ipLocation.region}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
