import { IconCalendarPlus } from "@tabler/icons-react"
import { Link, createFileRoute } from "@tanstack/react-router"

import { ApplicationHeading, ApplicationPage } from "@/components/applications/ApplicationUi"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export const Route = createFileRoute("/_app/portal/turnos/")({
  component: RouteComponent,
})

function RouteComponent() {
  return (
    <ApplicationPage breadcrumbs={[{ label: "Mis turnos" }]}>
      <ApplicationHeading
        title="Mis turnos"
        description="Solicitá un turno de salud comunitaria en un centro municipal."
        action={<Button render={<Link to="/portal/turnos/nuevo" />}><IconCalendarPlus />Solicitar turno</Button>}
      />
      <Card>
        <CardHeader>
          <CardTitle>Solicitar un turno</CardTitle>
          <CardDescription>Elegí el servicio, el centro, la fecha y el horario. El turno queda confirmado de inmediato.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button render={<Link to="/portal/turnos/nuevo" />}><IconCalendarPlus />Solicitar turno</Button>
        </CardContent>
      </Card>
    </ApplicationPage>
  )
}
