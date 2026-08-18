import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute(
  "/_app/gestion/programas/$programaId/requisitos",
)({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/programas/$programaId/requisitos"!</div>
}
