import { createFileRoute } from "@tanstack/react-router"

import { AppointmentWizard } from "@/components/appointments/AppointmentWizard"

export const Route = createFileRoute("/_app/portal/turnos/nuevo")({
  component: RouteComponent,
})

function RouteComponent() {
  const { user } = Route.useRouteContext()
  return user.id != null ? <AppointmentWizard userId={user.id} /> : null
}
