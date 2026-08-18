import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/programas/$programaId")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/programas/$programaId"!</div>
}
