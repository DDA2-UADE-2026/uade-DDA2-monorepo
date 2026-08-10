import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/"!</div>
}
