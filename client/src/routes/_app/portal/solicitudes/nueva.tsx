import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/solicitudes/nueva")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/solicitudes/nueva"!</div>
}
