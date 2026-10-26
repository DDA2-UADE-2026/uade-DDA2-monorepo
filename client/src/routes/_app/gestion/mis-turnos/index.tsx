import { IconAlertCircle, IconCalendarUser, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"

import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listProfessionalAppointmentsOptions } from "@/generated/@tanstack/react-query.gen"
import { appointmentToday, formatAppointmentRange } from "@/lib/appointment-flow"
import { attentionLabel, professionalTurnSearchSchema } from "@/lib/community-attention"
import { UserChip } from "@/components/UserChip"

export const Route = createFileRoute("/_app/gestion/mis-turnos/")({
  validateSearch: professionalTurnSearchSchema,
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
        <OutletNavBreadcrumbs items={[{ label: "Turnos asignados" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full min-w-0 max-w-6xl space-y-6 p-4 lg:p-6">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h1 className="font-heading text-2xl font-medium">Turnos asignados</h1>
                <p className="text-sm text-muted-foreground">Consultá tus turnos y registrá la asistencia del día.</p>
              </div>
              <div className="flex flex-wrap items-end gap-2">
                <div className="space-y-1">
                  <Label htmlFor="professional-turn-date">Fecha</Label>
                  <ProgramDatePicker
                    id="professional-turn-date"
                    value={fecha}
                    onChange={(value) => {
                      if (value) void navigate({ search: { fecha: value } })
                    }}
                  />
                </div>
                <Button variant="outline" onClick={() => void navigate({ search: { fecha: appointmentToday() } })}>Hoy</Button>
                <Button variant="outline" disabled={turns.isFetching} onClick={() => void turns.refetch()}>
                  <IconRefresh className={turns.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span>
                </Button>
              </div>
            </div>

            {turns.isPending ? (
              <div role="status" aria-label="Cargando turnos" className="space-y-3">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : turns.isError ? (
              <Alert variant="destructive">
                <IconAlertCircle />
                <AlertTitle>No se pudieron cargar tus turnos</AlertTitle>
                <AlertDescription><Button size="sm" variant="outline" className="mt-2" onClick={() => void turns.refetch()}>Reintentar</Button></AlertDescription>
              </Alert>
            ) : !turns.data.length ? (
              <Empty className="border">
                <EmptyHeader>
                  <EmptyMedia variant="icon"><IconCalendarUser /></EmptyMedia>
                  <EmptyTitle>Sin turnos asignados</EmptyTitle>
                  <EmptyDescription>No tenés turnos asignados para esta fecha.</EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <Card>
                <CardHeader>
                  <CardTitle>Agenda del día</CardTitle>
                  <CardDescription>{turns.data.length === 1 ? "1 turno asignado" : `${turns.data.length} turnos asignados`}</CardDescription>
                </CardHeader>
                <CardContent className="px-0">
                <Table className="min-w-[44rem]">
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
                          <TableCell className="min-w-44"><UserChip user={{ id: row.citizenId, name: row.citizenName }} /></TableCell>
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
                </CardContent>
              </Card>
            )}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
