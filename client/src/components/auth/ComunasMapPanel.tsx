import { useState } from 'react';

import { ComunasDotMap, type MapFeature } from '@/components/visual/comunas-dot-map';
import { ThemeToggle } from '@/components/ThemeToggle';

export default function ComunasMapPanel() {
  const [active, setActive] = useState<MapFeature | null>(null);

  return (
    <div aria-hidden className="pointer-events-none relative h-full w-full overflow-hidden bg-background">
      <ComunasDotMap
        className="absolute inset-0"
        onActiveChange={(feature) => setActive(feature)}
      />

      <div className="absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t from-background/60 via-background/40 to-transparent sm:h-1/2" />

      <div className="pointer-events-auto absolute inset-x-4 top-4 sm:inset-x-6 sm:top-6 md:inset-x-10 md:top-10 lg:inset-x-14 lg:top-14">
        <ThemeToggle />
      </div>

      {active ? (
        <div className="absolute inset-x-4 bottom-4 sm:inset-x-6 sm:bottom-6 md:inset-x-10 md:bottom-10 lg:inset-x-14 lg:bottom-14">
          <p className="-mb-8 select-none text-[4.5rem] font-bold leading-none text-foreground/8 sm:-mb-15 sm:text-[7rem] md:-mb-17 md:text-[10rem] lg:-mb-20 lg:text-[12rem] dark:text-foreground/5">
            {active.id.padStart(2, '0')}
          </p>
          <p className="text-base font-medium text-foreground sm:text-lg md:text-xl">Comuna {active.id}</p>
          <p className="mt-1 line-clamp-2 max-w-xs text-sm text-muted-foreground sm:max-w-sm md:max-w-md lg:max-w-lg">
            {active.sublabels.join(', ')}
          </p>
        </div>
      ) : null}
    </div>
  );
}
