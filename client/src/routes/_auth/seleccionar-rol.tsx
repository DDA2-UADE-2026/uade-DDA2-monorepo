import { createFileRoute } from "@tanstack/react-router"

import { RoleSwitcher } from "@/components/auth/RoleSwitcher"

export const Route = createFileRoute("/_auth/seleccionar-rol")({
  component: RouteComponent,
})

function RouteComponent() {
  return <RoleSwitcher />
}
