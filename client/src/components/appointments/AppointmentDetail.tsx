import { IconCalendarCheck } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"

import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { AppointmentError } from "@/components/appointments/AppointmentUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { getCitizenAppointmentOptions } from "@/generated/@tanstack/react-query.gen"
import type { AppointmentResponse } from "@/generated/types.gen"
import {
  appointmentStatusLabels,
  formatAppointmentDate,
  formatAppointmentRange,
  isUuid,
} from "@/lib/appointment-flow"

export function AppointmentDetail({ turnoId }: { turnoId: string }) {
  const valid = isUuid(turnoId)
  const detail = useQuery({ ...getCitizenAppointmentOptions({ path: { appointmentId: turnoId } }), enabled: valid })
  const appointment = detail.data as AppointmentResponse | undefined

  return (
    <ApplicationPage breadcrumbs={[{ label: "Mis turnos", to: "/portal/turnos" }, { label: "Confirmación" }]}>
      <ApplicationHeading
        title={appointment?.status === "CANCELLED" ? "Turno cancelado" : "Turno confirmado"}
        description={
          appointment?.status === "CANCELLED"
            ? "Este turno fue cancelado por la administración del centro."
            : "Guardá estos datos para el día de tu atención."
        }
      />
      {!valid ? (
        <Alert variant="destructive"><AlertTitle>No encontramos ese turno</AlertTitle><AlertDescription>El identificador no es válido.</AlertDescription></Alert>
      ) : detail.isPending ? <ApplicationLoading /> : detail.isError ? (
        <AppointmentError
          error={detail.error}
          title={detail.error.status === 404 ? "No encontramos ese turno" : "No pudimos consultar el turno"}
          retry={() => detail.refetch()}
        />
      ) : !appointment?.id ? (
        <Alert variant="destructive"><AlertTitle>No pudimos mostrar el turno</AlertTitle><AlertDescription>Volvé a consultar los horarios disponibles.</AlertDescription></Alert>
      ) : (
        <>
          <Card>
            <CardHeader>
              <Badge variant="default" className="w-fit">
                <IconCalendarCheck />{appointment.status ? appointmentStatusLabels[appointment.status] : "Confirmado"}
              </Badge>
              <CardTitle className="font-heading text-xl">{appointment.serviceName}</CardTitle>
              <CardDescription>{appointment.centerName} · {appointment.centerAddress}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p><span className="font-medium">Fecha:</span> {formatAppointmentDate(appointment.startsAt?.slice(0, 10))}</p>
              <p><span className="font-medium">Horario:</span> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
              <p><span className="font-medium">Profesional:</span> {appointment.professionalName}</p>
              <p className="text-muted-foreground">Número de confirmación: {appointment.id}</p>
            </CardContent>
          </Card>
          <div className="flex flex-col-reverse justify-between gap-3 sm:flex-row">
            <Button variant="outline" render={<Link to="/portal/turnos/nuevo" />}>Solicitar otro turno</Button>
            <Button render={<Link to="/portal/turnos" />}>Volver a mis turnos</Button>
          </div>
        </>
      )}
    </ApplicationPage>
  )
}
