import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/auditoria/eventos")({
  component: RouteComponent,
})

export function RouteComponent() {
  return <div>Hello "/_app/gestion/auditoria/eventos"!</div>
}
