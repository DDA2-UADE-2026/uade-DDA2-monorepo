import { IconArrowLeft } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect } from "@tanstack/react-router"

import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getProfessionalAppointmentOptions } from "@/generated/@tanstack/react-query.gen"
import { formatAppointmentDate, formatAppointmentRange, isUuid } from "@/lib/appointment-flow"
import { attentionLabel, professionalTurnSearchSchema } from "@/lib/community-attention"

export const Route = createFileRoute("/_app/gestion/mis-turnos/$turnoId")({
  validateSearch: professionalTurnSearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "PROFESIONAL_CENTRO") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { turnoId } = Route.useParams()
  const { fecha } = Route.useSearch()
  const detail = useQuery({ ...getProfessionalAppointmentOptions({ path: { id: turnoId } }), enabled: isUuid(turnoId) })
  const row = detail.data
  const appointment = row?.appointment

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Mis turnos", to: "/gestion/mis-turnos" }, { label: "Detalle" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <main className="mx-auto w-full max-w-3xl space-y-4 p-4 lg:p-6">
          <Button size="sm" variant="ghost" render={<Link to="/gestion/mis-turnos" search={{ fecha }} />}>
            <IconArrowLeft />Volver a mis turnos
          </Button>
          {!isUuid(turnoId) ? (
            <p>No encontramos ese turno.</p>
          ) : detail.isPending ? (
            <p role="status">Cargando turno…</p>
          ) : detail.isError || !appointment ? (
            <div className="space-y-2"><p>No se pudo cargar el turno.</p><Button variant="outline" onClick={() => void detail.refetch()}>Reintentar</Button></div>
          ) : (
            <Card>
              <CardHeader><Badge className="w-fit" variant="outline">{attentionLabel(row)}</Badge><CardTitle>{appointment.serviceName}</CardTitle></CardHeader>
              <CardContent className="space-y-2 text-sm">
                <p><strong>Ciudadano:</strong> {row.citizenName ?? "—"}</p>
                <p><strong>Centro:</strong> {appointment.centerName ?? "—"}</p>
                <p><strong>Fecha:</strong> {formatAppointmentDate(appointment.startsAt)}</p>
                <p><strong>Horario:</strong> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
              </CardContent>
            </Card>
          )}
        </main>
      </SidebarShellContent>
    </SidebarShell>
  )
}
