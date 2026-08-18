import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_auth/seleccionar-rol")({
  component: RouteComponent,
})

export function RouteComponent() {
  return <div>Hello "/_auth/change-role"!</div>
}
