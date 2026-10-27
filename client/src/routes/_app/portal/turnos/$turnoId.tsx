import { createFileRoute } from "@tanstack/react-router"

import { AppointmentDetail } from "@/components/appointments/AppointmentDetail"

export const Route = createFileRoute("/_app/portal/turnos/$turnoId")({
  component: RouteComponent,
})

function RouteComponent() {
  const { turnoId } = Route.useParams()
  return <AppointmentDetail turnoId={turnoId} />
}
