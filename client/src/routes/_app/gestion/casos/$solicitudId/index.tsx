import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/casos/$solicitudId/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/casos/$solicitudId/"!</div>
}
