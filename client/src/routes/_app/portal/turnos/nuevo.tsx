import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/turnos/nuevo")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/turnos/nuevo"!</div>
}
