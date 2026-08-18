import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/_auth/callback")({
  component: RouteComponent,
})

export function RouteComponent() {
  return <div>Hello "/_auth/callback"!</div>
}
