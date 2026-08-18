import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/campanias/$actividadId/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/campanias/$actividadId/"!</div>
}
