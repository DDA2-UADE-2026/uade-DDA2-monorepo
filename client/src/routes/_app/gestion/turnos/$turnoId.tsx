import { IconAlertTriangle, IconArrowLeft, IconCalendarCheck, IconCalendarX } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect } from "@tanstack/react-router"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { getAdminAppointmentOptions } from "@/generated/@tanstack/react-query.gen"
import { appointmentStatusLabels, formatAppointmentDate, formatAppointmentRange, isUuid } from "@/lib/appointment-flow"

export const Route = createFileRoute("/_app/gestion/turnos/$turnoId")({
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { turnoId } = Route.useParams()
  const valid = isUuid(turnoId)
  const detail = useQuery({ ...getAdminAppointmentOptions({ path: { id: turnoId } }), enabled: valid })
  const row = detail.isPending || detail.isError ? undefined : detail.data
  const appointment = row?.appointment
  const citizenName = row?.citizenName
  const slotRetained = row?.slotRetained ?? false

  return (
    <div className="mx-auto w-full max-w-3xl space-y-4 p-4 lg:p-6">
      <Button size="sm" variant="ghost" render={<Link to="/gestion/turnos" />}>
        <IconArrowLeft />Volver a turnos
      </Button>
      {!valid ? (
        <Alert variant="destructive">
          <AlertTitle>No encontramos ese turno</AlertTitle>
          <AlertDescription>El identificador no es válido.</AlertDescription>
        </Alert>
      ) : detail.isPending ? (
        <p className="text-sm text-muted-foreground">Cargando turno…</p>
      ) : detail.isError || !row || !appointment?.id ? (
        <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
          <p>No se pudo cargar el turno.</p>
          <Button size="sm" variant="outline" onClick={() => detail.refetch()}>Reintentar</Button>
        </div>
      ) : (
        <>
          <Card>
            <CardHeader>
              <span className="flex flex-wrap gap-1">
                <Badge variant={appointment.status === "CANCELLED" ? "secondary" : "default"}>
                  {appointment.status === "CANCELLED" ? <IconCalendarX /> : <IconCalendarCheck />}
                  {appointment.status ? appointmentStatusLabels[appointment.status] : "—"}
                </Badge>
                {slotRetained && <Badge variant="outline">Horario retenido</Badge>}
              </span>
              <CardTitle className="font-heading text-xl">{appointment.serviceName}</CardTitle>
              <CardDescription>{appointment.centerName} · {appointment.centerAddress}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p><span className="font-medium">Ciudadano:</span> {citizenName ?? "—"}</p>
              <p><span className="font-medium">Fecha:</span> {formatAppointmentDate(appointment.startsAt?.slice(0, 10))}</p>
              <p><span className="font-medium">Horario:</span> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
              <p><span className="font-medium">Profesional:</span> {appointment.professionalName}</p>
              <p className="text-muted-foreground">Identificador: {appointment.id}</p>
            </CardContent>
          </Card>
          {slotRetained && (
            <Alert>
              <IconAlertTriangle />
              <AlertTitle>Horario retenido</AlertTitle>
              <AlertDescription>
                Este turno está cancelado y su horario todavía no vuelve a ofrecerse.
                Un administrativo puede habilitarlo manualmente.
              </AlertDescription>
            </Alert>
          )}
        </>
      )}
    </div>
  )
}
