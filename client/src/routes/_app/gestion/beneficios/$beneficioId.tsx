import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/beneficios/$beneficioId")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/beneficios/$beneficioId"!</div>
}
