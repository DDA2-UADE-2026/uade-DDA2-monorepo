import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/actividades/$actividadId")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/actividades/$actividadId"!</div>
}
