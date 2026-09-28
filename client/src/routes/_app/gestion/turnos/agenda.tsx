import { IconArrowLeft, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"

import { adminDaySearchSchema } from "@/components/turnos/adminAppointmentFilters"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  getMunicipalCenterOptions,
  listAdminAppointmentDayAvailabilityOptions,
  listAdminAppointmentsOptions,
  listCenterServicesOptions,
} from "@/generated/@tanstack/react-query.gen"
import type { AdminAppointmentResponse, AdminFreeSlotResponse } from "@/generated/types.gen"
import { appointmentStatusLabels, formatAppointmentDate, formatAppointmentTime, isUuid } from "@/lib/appointment-flow"

export const Route = createFileRoute("/_app/gestion/turnos/agenda")({
  validateSearch: adminDaySearchSchema,
  beforeLoad: ({ context }) => {
    if (context.user.activeRole?.toUpperCase() !== "ADMIN") {
      throw redirect({ to: "/403", replace: true })
    }
  },
  component: RouteComponent,
})

type DayRow =
  | { key: string; startsAt: string; kind: "turno"; turno: AdminAppointmentResponse }
  | { key: string; startsAt: string; kind: "libre"; slot: AdminFreeSlotResponse }

function RouteComponent() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const validCenter = isUuid(search.centro)
  const backSearch = {
    centro: search.centro,
    servicio: search.servicio,
    anio: Number(search.fecha.slice(0, 4)),
    mes: Number(search.fecha.slice(5, 7)),
  }

  const center = useQuery({
    ...getMunicipalCenterOptions({ path: { id: search.centro } }),
    enabled: validCenter,
  })
  const services = useQuery({
    ...listCenterServicesOptions({ path: { centerId: search.centro } }),
    enabled: validCenter,
  })
  const serviceOptions = services.data ?? []
  const selectedService = serviceOptions.find((option) => option.service?.id === search.servicio)

  const turnos = useQuery({
    ...listAdminAppointmentsOptions({ query: { centerId: search.centro, date: search.fecha } }),
    enabled: validCenter,
  })
  const libres = useQuery({
    ...listAdminAppointmentDayAvailabilityOptions({
      query: {
        centerId: search.centro,
        date: search.fecha,
        ...(search.servicio ? { serviceId: search.servicio } : {}),
      },
    }),
    enabled: validCenter,
  })

  const filteredTurnos = (turnos.data ?? []).filter(
    (row) => !search.servicio || row.appointment?.serviceId === search.servicio,
  )
  const confirmed = filteredTurnos.filter((row) => row.appointment?.status === "CONFIRMED").length
  const retained = filteredTurnos.filter((row) => row.slotRetained).length
  const freeSlots = libres.data ?? []

  const rows: DayRow[] = [
    ...filteredTurnos.map((row) => ({
      key: `turno-${row.appointment?.id}`,
      startsAt: row.appointment?.startsAt ?? "",
      kind: "turno" as const,
      turno: row,
    })),
    ...freeSlots.map((slot) => ({
      key: `libre-${slot.professionalAssignmentId}-${slot.startsAt}`,
      startsAt: slot.startsAt ?? "",
      kind: "libre" as const,
      slot,
    })),
  ].sort((a, b) => a.startsAt.localeCompare(b.startsAt))

  const pending = turnos.isPending || libres.isPending
  const failed = turnos.isError || libres.isError
  const refetch = () => { turnos.refetch(); libres.refetch() }

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Turnos", to: "/gestion/turnos" }, { label: "Agenda del día" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-5xl space-y-4 p-4 lg:p-6">
            <Button size="sm" variant="ghost" render={<Link to="/gestion/turnos" search={backSearch} />}>
              <IconArrowLeft />Volver al calendario
            </Button>

      {!validCenter ? (
        <p className="text-sm text-muted-foreground">
          Elegí un centro en el calendario para ver la agenda del día.
        </p>
      ) : (
        <>
          <div>
            <h1 className="font-heading text-xl capitalize">{formatAppointmentDate(search.fecha)}</h1>
            <p className="text-sm text-muted-foreground">
              {center.data?.name ?? "Centro"} ·{" "}
              <span className="font-medium text-blue-700 dark:text-blue-400">{confirmed} conf.</span> ·{" "}
              <span className="font-medium text-emerald-700 dark:text-emerald-400">{freeSlots.length} lib.</span>
              {retained > 0 && (
                <>
                  {" · "}
                  <span className="font-medium text-red-700 dark:text-red-400">{retained} ret.</span>
                </>
              )}
            </p>
          </div>

          <div className="max-w-sm space-y-1.5">
            <Label htmlFor="agenda-servicio">Servicio</Label>
            <Select
              value={search.servicio}
              onValueChange={(value) => navigate({ search: { ...search, servicio: value ?? "" } })}
              disabled={services.isPending}
            >
              <SelectTrigger id="agenda-servicio" aria-label="Servicio">
                <SelectValue placeholder="Todos los servicios">
                  {() => selectedService?.service?.name ?? "Todos los servicios"}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Todos los servicios</SelectItem>
                {serviceOptions.map((option) => (
                  <SelectItem key={option.id} value={option.service?.id ?? ""}>
                    {option.service?.name}{option.active ? "" : " (inactivo)"}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {pending ? (
            <p className="text-sm text-muted-foreground">Cargando agenda…</p>
          ) : failed ? (
            <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
              <p>No se pudo cargar la agenda del día.</p>
              <Button size="sm" variant="outline" onClick={refetch}>Reintentar</Button>
            </div>
          ) : rows.length === 0 ? (
            <div className="flex items-center justify-between gap-2">
              <p className="text-sm text-muted-foreground">No hay turnos ni horarios para ese día.</p>
              <Button size="xs" variant="ghost" onClick={refetch} disabled={turnos.isFetching || libres.isFetching}>
                <IconRefresh className={turnos.isFetching || libres.isFetching ? "animate-spin" : undefined} />
                Actualizar
              </Button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Horario</TableHead>
                  <TableHead>Servicio</TableHead>
                  <TableHead>Profesional</TableHead>
                  <TableHead>Ciudadano</TableHead>
                  <TableHead>Estado</TableHead>
                  <TableHead className="w-px"><span className="sr-only">Acciones</span></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => row.kind === "libre" ? (
                  <TableRow key={row.key}>
                    <TableCell className="font-medium">
                      {formatAppointmentTime(row.slot.startsAt)} – {formatAppointmentTime(row.slot.endsAt)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{row.slot.serviceName ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{row.slot.professionalName ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">—</TableCell>
                    <TableCell><Badge variant="outline">Libre</Badge></TableCell>
                    <TableCell />
                  </TableRow>
                ) : (
                  <TableRow key={row.key}>
                    <TableCell className="font-medium">
                      {formatAppointmentTime(row.turno.appointment?.startsAt)} – {formatAppointmentTime(row.turno.appointment?.endsAt)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{row.turno.appointment?.serviceName ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{row.turno.appointment?.professionalName ?? "—"}</TableCell>
                    <TableCell>{row.turno.citizenName ?? "—"}</TableCell>
                    <TableCell>
                      <span className="flex flex-wrap gap-1">
                        <Badge variant={row.turno.appointment?.status === "CANCELLED" ? "secondary" : "default"}>
                          {row.turno.appointment?.status ? appointmentStatusLabels[row.turno.appointment.status] : "—"}
                        </Badge>
                        {row.turno.slotRetained && <Badge variant="outline">Horario retenido</Badge>}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Button
                        size="sm"
                        variant="outline"
                        render={<Link to="/gestion/turnos/$turnoId" params={{ turnoId: row.turno.appointment?.id ?? "" }} />}
                      >
                        Ver
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </>
      )}
          </div>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}
