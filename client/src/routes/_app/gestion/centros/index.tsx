import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/centros/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/centros/"!</div>
}
