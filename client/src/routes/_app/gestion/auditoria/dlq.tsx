import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/auditoria/dlq")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/auditoria/dlq"!</div>
}
