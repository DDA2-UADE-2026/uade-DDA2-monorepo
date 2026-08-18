import { createFileRoute } from "@tanstack/react-router"

export const Route = createFileRoute("/403")({
  component: RouteComponent,
})

export function RouteComponent() {
  return <div>Hello "/403"!</div>
}
