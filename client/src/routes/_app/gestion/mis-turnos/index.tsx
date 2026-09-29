import { IconCalendarEvent, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"

import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listProfessionalAppointmentsOptions } from "@/generated/@tanstack/react-query.gen"
import { appointmentToday, formatAppointmentRange } from "@/lib/appointment-flow"
import { attentionLabel, professionalTurnSearchSchema } from "@/lib/community-attention"

export const Route = createFileRoute("/_app/gestion/mis-turnos/")({
  validateSearch: professionalTurnSearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "PROFESIONAL_CENTRO") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const { fecha } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const turns = useQuery(listProfessionalAppointmentsOptions({ query: { date: fecha } }))

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Mis turnos" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full max-w-5xl space-y-5 p-4 lg:p-6">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h1 className="font-heading text-2xl">Mis turnos</h1>
                <p className="text-sm text-muted-foreground">Consultá tus turnos y registrá la asistencia del día.</p>
              </div>
              <div className="flex flex-wrap items-end gap-2">
                <div className="space-y-1">
                  <Label htmlFor="professional-turn-date">Fecha</Label>
                  <Input id="professional-turn-date" type="date" value={fecha}
                    onChange={(event) => {
                      if (event.target.value) void navigate({ search: { fecha: event.target.value } })
                    }} />
                </div>
                <Button variant="outline" onClick={() => void navigate({ search: { fecha: appointmentToday() } })}>Hoy</Button>
                <Button variant="outline" disabled={turns.isFetching} onClick={() => void turns.refetch()}>
                  <IconRefresh className={turns.isFetching ? "animate-spin" : undefined} />Actualizar
                </Button>
              </div>
            </div>

            {turns.isPending ? (
              <p role="status" className="text-sm text-muted-foreground">Cargando turnos…</p>
            ) : turns.isError ? (
              <div className="space-y-2 text-sm">
                <p>No se pudieron cargar tus turnos.</p>
                <Button variant="outline" onClick={() => void turns.refetch()}>Reintentar</Button>
              </div>
            ) : !turns.data.length ? (
              <div className="flex flex-col items-center gap-2 rounded-lg border p-8 text-center text-muted-foreground">
                <IconCalendarEvent className="size-7" />
                <p>No tenés turnos asignados para esta fecha.</p>
              </div>
            ) : (
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader><TableRow>
                    <TableHead>Horario</TableHead><TableHead>Ciudadano</TableHead>
                    <TableHead>Servicio y centro</TableHead><TableHead>Resultado</TableHead><TableHead>Acción</TableHead>
                  </TableRow></TableHeader>
                  <TableBody>
                    {turns.data.map((row) => {
                      const appointment = row.appointment
                      const label = attentionLabel(row)
                      return (
                        <TableRow key={appointment?.id}>
                          <TableCell className="whitespace-nowrap font-medium">{formatAppointmentRange(appointment?.startsAt, appointment?.endsAt)}</TableCell>
                          <TableCell>{row.citizenName ?? "—"}</TableCell>
                          <TableCell>{appointment?.serviceName ?? "—"}<span className="block text-xs text-muted-foreground">{appointment?.centerName}</span></TableCell>
                          <TableCell><Badge variant={label === "Cancelado" ? "secondary" : label === "Sin registrar" ? "outline" : "default"}>{label}</Badge></TableCell>
                          <TableCell>
                            {appointment?.id && <Button size="sm" variant="outline" render={<Link to="/gestion/mis-turnos/$turnoId" params={{ turnoId: appointment.id }} search={{ fecha }} />}>
                              {row.attention ? "Ver / corregir" : "Ver turno"}
                            </Button>}
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </div>
            )}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
