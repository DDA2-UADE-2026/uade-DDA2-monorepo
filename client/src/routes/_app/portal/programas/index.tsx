import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/programas/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/programas/"!</div>
}
