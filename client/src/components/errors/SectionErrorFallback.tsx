import type { ErrorComponentProps } from '@tanstack/react-router'

import { ErrorCard, type ErrorCardProps } from '@/components/errors/ErrorCard'

interface SectionErrorFallbackProps extends ErrorComponentProps {
  title?: string
  description?: string
  homeHref?: ErrorCardProps['homeHref']
  homeLabel?: string
}

/**
 * `errorComponent` for a layout route that owns persistent chrome (a sidebar,
 * a nav). Because TanStack Router only wraps a route in a CatchBoundary when
 * it has a resolved `errorComponent`, a route left unset here just passes
 * the error through to the nearest ancestor that does — so this is the
 * boundary a child route's crash bubbles up to, while the layout itself
 * (and its sidebar) keeps rendering around the empty `<Outlet/>` slot.
 */
function SectionErrorFallback({ homeHref = '/', homeLabel = 'Volver', ...props }: SectionErrorFallbackProps) {
  return (
    <div className="flex flex-1 items-center justify-center p-6">
      <ErrorCard {...props} homeHref={homeHref} homeLabel={homeLabel} className="sm:max-w-sm" />
    </div>
  )
}

export { SectionErrorFallback }
export type { SectionErrorFallbackProps }
