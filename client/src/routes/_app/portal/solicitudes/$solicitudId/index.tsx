import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/solicitudes/$solicitudId/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/solicitudes/$solicitudId/"!</div>
}
