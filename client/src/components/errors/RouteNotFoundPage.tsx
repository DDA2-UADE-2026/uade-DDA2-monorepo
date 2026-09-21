import { useLocation } from '@tanstack/react-router'

import { cn } from '@/lib/utils'
import { NotFoundCard, type NotFoundCardProps } from '@/components/errors/NotFoundCard'
import { RaysScreen } from '@/components/errors/RaysScreen'

type RouteNotFoundPageProps = NotFoundCardProps

/**
 * Full-page fallback for TanStack Router's not-found architecture — wired as
 * `notFoundComponent` on the root route (so unmatched URLs anywhere land
 * here) and on the `_app/gestion` / `_app/portal` layouts, which override
 * `homeHref` so the way out stays inside the section the user was in.
 *
 * With the router's default `notFoundMode: 'fuzzy'`, an unmatched URL is
 * claimed by the *deepest* matched route that defines a `notFoundComponent`,
 * and that component replaces the route's own — so `/gestion/no-existe`
 * renders this screen instead of the gestión layout, sidebar included.
 */
function RouteNotFoundPage(props: RouteNotFoundPageProps) {
  const { pathname } = useLocation()

  return (
    <RaysScreen tone="neutral">
      <NotFoundCard path={pathname} {...props} className={cn('relative z-10 sm:max-w-md', props.className)} />
    </RaysScreen>
  )
}

export { RouteNotFoundPage }
export type { RouteNotFoundPageProps }
