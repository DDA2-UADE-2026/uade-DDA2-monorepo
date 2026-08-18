import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_app/gestion")({
  component: RouteComponent,
})

export function RouteComponent() {
  return <div>Hello "/_app/gestion"!</div>
}
