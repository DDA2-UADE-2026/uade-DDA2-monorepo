import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/portal/campanias/")({
  component: RouteComponent,
})

function RouteComponent() {
  return <div>Hello "/_app/portal/campanias/"!</div>
}
