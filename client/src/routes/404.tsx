import { createFileRoute } from "@tanstack/react-router"

import { RouteNotFoundPage } from "@/components/errors/RouteNotFoundPage"

export const Route = createFileRoute("/404")({
  component: RouteComponent,
})

/** Same screen the router renders for unmatched URLs, reachable as a plain
 *  destination for code that would rather `redirect({ to: "/404" })` than
 *  `throw notFound()`. The attempted path is left out — here it's just
 *  "/404", which says nothing to the user. */
function RouteComponent() {
  return <RouteNotFoundPage path="" />
}
