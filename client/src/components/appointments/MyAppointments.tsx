import { IconCalendarCheck, IconCalendarX } from "@tabler/icons-react"
import { Link } from "@tanstack/react-router"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import type { AppointmentResponse } from "@/generated/types.gen"
import {
  appointmentStatusLabels,
  formatAppointmentDate,
  formatAppointmentRange,
} from "@/lib/appointment-flow"

export function MyAppointments({ upcoming, history }: {
  upcoming: AppointmentResponse[]
  history: AppointmentResponse[]
}) {
  return (
    <div className="space-y-6">
      <section aria-label="Próximos turnos" className="space-y-3">
        <h2 className="font-heading text-lg">Próximos</h2>
        {upcoming.length === 0 ? (
          <p className="text-sm text-muted-foreground">No tenés próximos turnos.</p>
        ) : (
          upcoming.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} />)
        )}
      </section>
      <section aria-label="Historial de turnos" className="space-y-3">
        <h2 className="font-heading text-lg">Historial</h2>
        {history.length === 0 ? (
          <p className="text-sm text-muted-foreground">Todavía no hay turnos en tu historial.</p>
        ) : (
          history.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} />)
        )}
      </section>
    </div>
  )
}

function AppointmentCard({ appointment }: { appointment: AppointmentResponse }) {
  const cancelled = appointment.status === "CANCELLED"
  return (
    <Card>
      <CardHeader>
        <Badge variant={cancelled ? "secondary" : "default"} className="w-fit">
          {cancelled ? <IconCalendarX /> : <IconCalendarCheck />}
          {appointment.status ? appointmentStatusLabels[appointment.status] : "—"}
        </Badge>
        <CardTitle className="font-heading text-xl">{appointment.serviceName}</CardTitle>
        <CardDescription>{appointment.centerName} · {appointment.centerAddress}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-2 text-sm">
        <p><span className="font-medium">Fecha:</span> {formatAppointmentDate(appointment.startsAt?.slice(0, 10))}</p>
        <p><span className="font-medium">Horario:</span> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
        <p><span className="font-medium">Profesional:</span> {appointment.professionalName}</p>
        <div>
          <Button size="sm" variant="outline" render={<Link to="/portal/turnos/$turnoId" params={{ turnoId: appointment.id ?? "" }} />}>
            Ver detalle
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
