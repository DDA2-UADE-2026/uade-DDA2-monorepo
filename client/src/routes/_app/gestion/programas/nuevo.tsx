import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion/programas/nuevo")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/gestion/programas/nuevo"!</div>
}
