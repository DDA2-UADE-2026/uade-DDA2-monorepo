import { createFileRoute, Outlet } from "@tanstack/react-router"

import { requireAuthenticatedUser } from "@/lib/auth-route-guards"

export const Route = createFileRoute("/_app")({
  beforeLoad: ({ context }) => requireAuthenticatedUser(context.queryClient),
  component: RouteComponent,
})

function RouteComponent() {
  return <Outlet />
}
