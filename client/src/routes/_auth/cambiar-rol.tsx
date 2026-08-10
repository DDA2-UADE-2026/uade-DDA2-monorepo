import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_auth/cambiar-rol")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_auth/change-role"!</div>
}
