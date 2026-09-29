import { IconArrowLeft } from "@tabler/icons-react"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, createFileRoute, redirect } from "@tanstack/react-router"
import { useState } from "react"

import { CommunityAttentionForm } from "@/components/appointments/CommunityAttentionForm"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getProfessionalAppointmentOptions, getProfessionalAppointmentQueryKey } from "@/generated/@tanstack/react-query.gen"
import { APPOINTMENT_TIME_ZONE, formatAppointmentDate, formatAppointmentRange, isUuid } from "@/lib/appointment-flow"
import { attentionLabel, canRegisterAttention, professionalTurnSearchSchema } from "@/lib/community-attention"

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
  const queryClient = useQueryClient()
  const [saved, setSaved] = useState(false)
  const [stale, setStale] = useState(false)
  const detail = useQuery({ ...getProfessionalAppointmentOptions({ path: { id: turnoId } }), enabled: isUuid(turnoId) })
  const row = detail.data
  const appointment = row?.appointment

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: getProfessionalAppointmentQueryKey({ path: { id: turnoId } }) })
    void queryClient.invalidateQueries({
      predicate: (query) => (query.queryKey[0] as { _id?: string } | undefined)?._id === "listProfessionalAppointments",
    })
  }

  function onSaved() {
    setSaved(true)
    setStale(false)
    refresh()
  }

  function onConflict() {
    setSaved(false)
    setStale(true)
    refresh()
  }

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
            <>
              <Card>
                <CardHeader><Badge className="w-fit" variant="outline">{attentionLabel(row)}</Badge><CardTitle>{appointment.serviceName}</CardTitle></CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <p><strong>Ciudadano:</strong> {row.citizenName ?? "—"}</p>
                  <p><strong>Centro:</strong> {appointment.centerName ?? "—"}</p>
                  <p><strong>Fecha:</strong> {formatAppointmentDate(appointment.startsAt)}</p>
                  <p><strong>Horario:</strong> {formatAppointmentRange(appointment.startsAt, appointment.endsAt)}</p>
                </CardContent>
              </Card>

              {saved && <Alert role="status"><AlertTitle>Constancia guardada</AlertTitle><AlertDescription>El resultado del turno se actualizó correctamente.</AlertDescription></Alert>}
              {stale && <Alert role="status"><AlertTitle>Se actualizó el turno</AlertTitle><AlertDescription>Revisá el resultado vigente antes de guardar de nuevo.</AlertDescription></Alert>}

              {row.attention && <Card>
                <CardHeader><CardTitle>Constancia vigente</CardTitle></CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <p><strong>Resultado:</strong> {attentionLabel(row)}</p>
                  {row.attention.result === "ATENDIDO" && <>
                    <p><strong>Fecha de atención:</strong> {formatAppointmentDate(row.attention.attendedOn ? `${row.attention.attendedOn}T12:00:00-03:00` : undefined)}</p>
                    <p className="whitespace-pre-wrap wrap-break-word"><strong>Servicio prestado:</strong> {row.attention.description}</p>
                  </>}
                  {row.attention.updatedAt && <p className="text-muted-foreground">Última actualización: {new Intl.DateTimeFormat("es-AR", {
                    dateStyle: "short", timeStyle: "short", timeZone: APPOINTMENT_TIME_ZONE,
                  }).format(new Date(row.attention.updatedAt))}</p>}
                </CardContent>
              </Card>}

              {canRegisterAttention(row) ? (
                <CommunityAttentionForm key={row.attention?.version ?? "new"} turnoId={turnoId} row={row} onSaved={onSaved} onConflict={onConflict} />
              ) : (
                <p className="text-sm text-muted-foreground">{appointment.status === "CANCELLED"
                  ? "Este turno fue cancelado y no admite registrar asistencia."
                  : "Podrás registrar la asistencia cuando comience el turno."}</p>
              )}
            </>
          )}
        </main>
      </SidebarShellContent>
    </SidebarShell>
  )
}
