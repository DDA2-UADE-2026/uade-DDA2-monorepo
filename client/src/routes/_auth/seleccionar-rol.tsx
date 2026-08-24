import { createFileRoute } from "@tanstack/react-router"

import { RoleSwitcher } from "@/components/auth/RoleSwitcher"
import { requireAuthenticatedUser } from "@/lib/auth-route-guards"

export const Route = createFileRoute("/_auth/seleccionar-rol")({
  beforeLoad: ({ context }) => requireAuthenticatedUser(context.queryClient),
  component: RouteComponent,
})

function RouteComponent() {
  return <RoleSwitcher />
}
