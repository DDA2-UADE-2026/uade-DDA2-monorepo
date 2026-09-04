import { createFileRoute } from "@tanstack/react-router"

import { RoleSwitcher } from "@/components/auth/RoleSwitcher"
import { requireRoleChoice } from "@/lib/auth-route-guards"

export const Route = createFileRoute("/_auth/seleccionar-rol")({
  beforeLoad: async ({ context }) => ({
    roleChoice: await requireRoleChoice(context.queryClient),
  }),
  component: RouteComponent,
})

function RouteComponent() {
  const { roleChoice } = Route.useRouteContext()
  return <RoleSwitcher roleChoice={roleChoice} />
}
