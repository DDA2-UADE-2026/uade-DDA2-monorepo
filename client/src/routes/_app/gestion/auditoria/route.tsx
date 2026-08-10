import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/auditoria")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/auditoria"!</div>
}
