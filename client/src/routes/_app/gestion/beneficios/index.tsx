import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/beneficios/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/beneficios/"!</div>
}
