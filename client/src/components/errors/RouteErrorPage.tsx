import { Suspense, lazy } from 'react'
import type { ErrorComponentProps } from '@tanstack/react-router'

import { ErrorCard } from '@/components/errors/ErrorCard'
import { ThemeToggle } from '@/components/ThemeToggle'

const SideRaysBackground = lazy(() => import('@/components/visual/SideRaysBackground'))

interface RouteErrorPageProps extends ErrorComponentProps {
  title?: string
  description?: string
}

/**
 * Full-page fallback for TanStack Router's error architecture — wired as the
 * `errorComponent` on the root route, so it's the last boundary any error
 * bubbles up to when no closer route claims one (see `_auth`, `_app/gestion`,
 * `_app/portal` for nested, chrome-preserving boundaries). Any route can
 * still opt into this exact full-page treatment by passing it directly, or
 * reuse `ErrorCard` on its own for a custom shell.
 */
function RouteErrorPage(props: RouteErrorPageProps) {
  return (
    <div className="relative grid min-h-svh place-items-center p-4">
      <div className="dark absolute inset-0 z-0 bg-background pointer-events-none">
        <Suspense fallback={null}>
          <SideRaysBackground
            speed={2}
            rayColor1="#ef4444"
            rayColor2="#7f1d1d"
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

      <ErrorCard {...props} className="relative z-10 sm:max-w-md" />
    </div>
  )
}

export { RouteErrorPage }
export type { RouteErrorPageProps }
