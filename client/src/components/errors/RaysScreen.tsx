import { Suspense, lazy, type ReactNode } from 'react'

import { ThemeToggle } from '@/components/ThemeToggle'

const SideRaysBackground = lazy(() => import('@/components/visual/SideRaysBackground'))

const TONES = {
  /** Something broke — red rays, matching the destructive palette. */
  danger: { rayColor1: '#ef4444', rayColor2: '#7f1d1d' },
  /** Nothing broke, the URL just doesn't exist — the app's usual blue rays. */
  neutral: { rayColor1: '#2b7fff', rayColor2: '#3c3cfa' },
} as const

interface RaysScreenProps {
  tone?: keyof typeof TONES
  children: ReactNode
}

/**
 * Full-viewport shell shared by the standalone error / not-found screens:
 * the WebGL ray background (lazy, so its chunk only loads when one of these
 * screens actually renders), a theme toggle, and a centered slot for the card.
 *
 * The background is pinned to `dark` on purpose — the rays are built for a
 * dark backdrop, and these screens replace the whole app chrome, so there's
 * no surrounding surface to clash with.
 */
function RaysScreen({ tone = 'danger', children }: RaysScreenProps) {
  const { rayColor1, rayColor2 } = TONES[tone]

  return (
    <div className="relative grid min-h-svh place-items-center p-4">
      <div className="dark absolute inset-0 z-0 bg-background pointer-events-none">
        <Suspense fallback={null}>
          <SideRaysBackground
            speed={2}
            rayColor1={rayColor1}
            rayColor2={rayColor2}
            intensity={2.2}
            spread={2}
            origin="top-right"
            saturation={1.4}
            blend={0.75}
            falloff={1.6}
            opacity={1}
          />
        </Suspense>
      </div>

      <div className="absolute right-4 top-4 z-10">
        <ThemeToggle />
      </div>

      {children}
    </div>
  )
}

export { RaysScreen }
export type { RaysScreenProps }
