import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute(
  "/_app/gestion/casos/$solicitudId/evaluacion",
)({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/casos/$solicitudId/evaluacion"!</div>
}
