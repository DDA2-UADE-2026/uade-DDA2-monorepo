import { IconCalendarEvent, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"

import { adminAppointmentSearchSchema } from "@/components/turnos/adminAppointmentFilters"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { listAdminAppointmentsOptions, listMunicipalCentersOptions } from "@/generated/@tanstack/react-query.gen"
import { appointmentStatusLabels, formatAppointmentRange } from "@/lib/appointment-flow"

export const Route = createFileRoute("/_app/gestion/turnos/")({
  validateSearch: adminAppointmentSearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

function RouteComponent() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const setSearch = (patch: Partial<typeof search>) => navigate({ search: { ...search, ...patch } })

  const centers = useQuery(listMunicipalCentersOptions({ query: { active: true, size: 200 } }))
  const centerOptions = centers.data?.content ?? []
  const selectedCenter = centerOptions.find((center) => center.id === search.centro)

  const appointments = useQuery({
    ...listAdminAppointmentsOptions({ query: { centerId: search.centro, date: search.fecha } }),
    enabled: search.centro.length > 0,
  })
  const rows = appointments.data ?? []

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Turnos" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-2 space-y-4 py-2 sm:mx-4! lg:py-4">
            <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
              <div className="space-y-1.5">
                <Label htmlFor="turnos-centro">Centro</Label>
                <Select
                  value={search.centro}
                  onValueChange={(value) => setSearch({ centro: value ?? "" })}
                  disabled={centers.isPending}
                >
                  <SelectTrigger id="turnos-centro" aria-label="Centro">
                    <SelectValue placeholder="Seleccioná un centro">
                      {() => selectedCenter?.name ?? "Seleccioná un centro"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {centerOptions.map((center) => (
                      <SelectItem key={center.id} value={center.id ?? ""}>{center.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {centers.isError && (
                  <p className="text-xs text-destructive">
                    No se pudieron cargar los centros.{" "}
                    <button type="button" className="underline" onClick={() => centers.refetch()}>Reintentar</button>
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="turnos-fecha">Fecha</Label>
                <Input
                  id="turnos-fecha"
                  type="date"
                  value={search.fecha}
                  onChange={(event) => { if (event.target.value) setSearch({ fecha: event.target.value }) }}
                />
              </div>
            </div>

            {search.centro.length === 0 ? (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <IconCalendarEvent className="size-4" />
                Seleccioná un centro para ver sus turnos del día.
              </p>
            ) : appointments.isPending ? (
              <p className="text-sm text-muted-foreground">Cargando turnos…</p>
            ) : appointments.isError ? (
              <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                <p>No se pudieron cargar los turnos.</p>
                <Button size="sm" variant="outline" onClick={() => appointments.refetch()}>Reintentar</Button>
              </div>
            ) : rows.length === 0 ? (
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm text-muted-foreground">No hay turnos para ese centro y fecha.</p>
                <Button size="xs" variant="ghost" onClick={() => appointments.refetch()} disabled={appointments.isFetching}>
                  <IconRefresh className={appointments.isFetching ? "animate-spin" : undefined} />
                  Actualizar
                </Button>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
                  <span>{rows.length} turno{rows.length === 1 ? "" : "s"}</span>
                  <Button size="xs" variant="ghost" onClick={() => appointments.refetch()} disabled={appointments.isFetching}>
                    <IconRefresh className={appointments.isFetching ? "animate-spin" : undefined} />
                    Actualizar
                  </Button>
                </div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Horario</TableHead>
                      <TableHead>Ciudadano</TableHead>
                      <TableHead>Servicio</TableHead>
                      <TableHead>Profesional</TableHead>
                      <TableHead>Estado</TableHead>
                      <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {rows.map((row) => {
                      const detail = row.appointment
                      return (
                        <TableRow key={detail?.id}>
                          <TableCell className="font-medium">
                            {formatAppointmentRange(detail?.startsAt, detail?.endsAt)}
                          </TableCell>
                          <TableCell>{row.citizenName ?? "—"}</TableCell>
                          <TableCell className="text-muted-foreground">{detail?.serviceName ?? "—"}</TableCell>
                          <TableCell className="text-muted-foreground">{detail?.professionalName ?? "—"}</TableCell>
                          <TableCell>
                            <span className="flex flex-wrap gap-1">
                              <Badge variant={detail?.status === "CANCELLED" ? "secondary" : "default"}>
                                {detail?.status ? appointmentStatusLabels[detail.status] : "—"}
                              </Badge>
                              {row.slotRetained && <Badge variant="outline">Horario retenido</Badge>}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Button size="sm" variant="outline" render={<Link to="/gestion/turnos/$turnoId" params={{ turnoId: detail?.id ?? "" }} />}>
                              Ver
                            </Button>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </>
            )}
          </div>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
