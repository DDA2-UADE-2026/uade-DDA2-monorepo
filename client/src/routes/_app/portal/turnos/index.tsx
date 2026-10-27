import { IconCalendarPlus } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute } from "@tanstack/react-router"

import { ApplicationHeading, ApplicationLoading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { AppointmentError } from "@/components/appointments/AppointmentUi"
import { MyAppointments } from "@/components/appointments/MyAppointments"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { listCitizenAppointmentsOptions } from "@/generated/@tanstack/react-query.gen"
import { splitAppointments } from "@/lib/appointment-flow"

export const Route = createFileRoute("/_app/portal/turnos/")({
  component: RouteComponent,
})

function RouteComponent() {
  const turnos = useQuery(listCitizenAppointmentsOptions())
  const { upcoming, history } = splitAppointments(turnos.data ?? [])

  return (
    <ApplicationPage breadcrumbs={[{ label: "Mis turnos" }]}>
      <ApplicationHeading
        title="Mis turnos"
        description="Consultá tus turnos de salud comunitaria y su estado."
        action={<Button render={<Link to="/portal/turnos/nuevo" />}><IconCalendarPlus />Solicitar turno</Button>}
      />
      {turnos.isPending ? <ApplicationLoading /> : turnos.isError ? (
        <AppointmentError
          error={turnos.error}
          title="No pudimos cargar tus turnos"
          retry={() => turnos.refetch()}
        />
      ) : upcoming.length === 0 && history.length === 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>Todavía no tenés turnos</CardTitle>
            <CardDescription>Elegí el servicio, el centro, la fecha y el horario. El turno queda confirmado de inmediato.</CardDescription>
          </CardHeader>
          <CardContent>
            <Button render={<Link to="/portal/turnos/nuevo" />}><IconCalendarPlus />Solicitar turno</Button>
          </CardContent>
        </Card>
      ) : (
        <MyAppointments upcoming={upcoming} history={history} />
      )}
    </ApplicationPage>
  )
}
