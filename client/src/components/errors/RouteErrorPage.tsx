import type { ErrorComponentProps } from '@tanstack/react-router'

import { cn } from '@/lib/utils'
import { ErrorCard, type ErrorCardProps } from '@/components/errors/ErrorCard'
import { RaysScreen } from '@/components/errors/RaysScreen'

interface RouteErrorPageProps
  extends ErrorComponentProps,
    Pick<ErrorCardProps, 'title' | 'description' | 'homeHref' | 'homeLabel' | 'className'> {}

/**
 * Full-page fallback for TanStack Router's error architecture — wired as the
 * `errorComponent` on the root route, so it's the last boundary any error
 * bubbles up to when no closer route claims one, and on the `_auth`,
 * `_app/gestion` and `_app/portal` layouts, where it catches crashes in the
 * layout itself (its `beforeLoad`, its chrome) — the ones that leave no shell
 * to render a smaller fallback inside. Crashes in a *page* below those
 * layouts are caught earlier by their `CatchBoundary` + `SectionErrorFallback`
 * so the sidebar survives. Any route can still opt into this full-page
 * treatment by passing it directly, or reuse `ErrorCard` in a custom shell.
 */
function RouteErrorPage({ className, ...props }: RouteErrorPageProps) {
  return (
    <RaysScreen tone="danger">
      <ErrorCard {...props} className={cn('relative z-10 sm:max-w-md', className)} />
    </RaysScreen>
  )
}

export { RouteErrorPage }
export type { RouteErrorPageProps }
