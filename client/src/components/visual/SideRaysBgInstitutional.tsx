import { useEffect, useState } from 'react';

import { useTheme } from '@/hooks/use-theme';
import SideRaysBackground from '@/components/visual/SideRaysBackground';

const PALETTES = {
  light: { rayColor1: '#38BDF8', rayColor2: '#7DD3FC' },
  dark: { rayColor1: '#2b7fff', rayColor2: '#3c3cfa' },
} as const;

/** Resolves the "system" theme setting to an actual light/dark value, tracking OS changes live. */
function useResolvedTheme() {
  const { theme } = useTheme();
  const [systemTheme, setSystemTheme] = useState<'light' | 'dark'>(() =>
    window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light',
  );

  useEffect(() => {
    if (theme !== 'system') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => setSystemTheme(mq.matches ? 'dark' : 'light');
    onChange();
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [theme]);

  return theme === 'system' ? systemTheme : theme;
}

/** Institutional preset for SideRaysBackground, used behind the auth forms.
 *  Colors switch with the site theme: a lighter sky-blue duo on light mode,
 *  a more saturated blue duo on dark mode so the rays stay visible. */
function SideRaysBgInstitutional() {
  const resolvedTheme = useResolvedTheme();
  const { rayColor1, rayColor2 } = PALETTES[resolvedTheme];

  return (
    <SideRaysBackground
      speed={2.5}
      rayColor1={rayColor1}
      rayColor2={rayColor2}
      intensity={2}
      spread={2}
      origin="top-right"
      tilt={0}
      saturation={1.5}
      blend={0.75}
      falloff={1.6}
      opacity={1}
    />
  );
}

export default SideRaysBgInstitutional;
