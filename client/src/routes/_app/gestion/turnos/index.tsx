import { IconBuilding, IconCalendarEvent, IconChevronLeft, IconChevronRight, IconRefresh, IconSearch } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"
import { useState } from "react"

import {
  adminCalendarSearchSchema,
  daysInMonth,
  leadingBlanks,
  monthDayIso,
  monthTitle,
  shiftMonth,
} from "@/components/turnos/adminAppointmentFilters"
import { AppointmentCenterSelectionDialog, AppointmentServiceSelectionDialog } from "@/components/turnos/AppointmentSelectionDialogs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import {
  getMunicipalCenterOptions,
  listAdminAppointmentMonthlySummaryOptions,
  listCenterServicesOptions,
} from "@/generated/@tanstack/react-query.gen"
import type { AdminDaySummaryResponse } from "@/generated/types.gen"
import { isUuid } from "@/lib/appointment-flow"

const WEEKDAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]

export const Route = createFileRoute("/_app/gestion/turnos/")({
  validateSearch: adminCalendarSearchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const setSearch = (patch: Partial<typeof search>) => navigate({ search: { ...search, ...patch } })
  const [centerDialogOpen, setCenterDialogOpen] = useState(false)
  const [serviceDialogOpen, setServiceDialogOpen] = useState(false)
  const selectedCenterDetail = useQuery({
    ...getMunicipalCenterOptions({ path: { id: search.centro } }),
    enabled: isUuid(search.centro),
  })
  const services = useQuery({
    ...listCenterServicesOptions({ path: { centerId: search.centro } }),
    enabled: isUuid(search.centro),
  })
  const serviceOptions = services.data ?? []
  const selectedService = serviceOptions.find((option) => option.service?.id === search.servicio)

  const summary = useQuery({
    ...listAdminAppointmentMonthlySummaryOptions({
      query: {
        centerId: search.centro,
        year: search.anio,
        month: search.mes,
        ...(search.servicio ? { serviceId: search.servicio } : {}),
      },
    }),
    enabled: isUuid(search.centro),
  })
  const days = new Map((summary.data ?? []).map((day) => [day.date, day]))

  const totalDays = daysInMonth(search.anio, search.mes)
  const blanks = leadingBlanks(search.anio, search.mes)

  const shift = (delta: -1 | 1) => setSearch(shiftMonth(search.anio, search.mes, delta))

  return (
    <SidebarShell>
      <OutletNavSticky>
        <OutletNavSidebarTrigger withSeparator />
        <OutletNavBreadcrumbs items={[{ label: "Turnos" }]} />
      </OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <main className="mx-auto w-full min-w-0 max-w-6xl space-y-6 p-4 lg:p-6">
            <header>
              <h1 className="font-heading text-2xl font-medium">Turnos</h1>
              <p className="mt-1 text-sm text-muted-foreground">Consultá la disponibilidad y los turnos de un centro por mes.</p>
            </header>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="min-w-0 space-y-1.5">
                <Label htmlFor="turnos-centro">Centro</Label>
                <Button id="turnos-centro" type="button" variant="outline" className="w-full min-w-0 justify-start font-normal" onClick={() => setCenterDialogOpen(true)}>
                  <IconBuilding />
                  <span className="truncate">{selectedCenterDetail.data?.name ?? "Seleccionar centro"}</span>
                  <IconSearch className="ml-auto" />
                </Button>
              </div>
              <div className="min-w-0 space-y-1.5">
                <Label htmlFor="turnos-servicio">Servicio</Label>
                <Button id="turnos-servicio" type="button" variant="outline" className="w-full min-w-0 justify-start font-normal" disabled={!isUuid(search.centro)} onClick={() => setServiceDialogOpen(true)}>
                  <IconCalendarEvent />
                  <span className="truncate">{selectedService?.service?.name ?? "Todos los servicios"}</span>
                  <IconSearch className="ml-auto" />
                </Button>
                {services.isError && (
                  <p className="text-xs text-destructive">
                    No se pudieron cargar los servicios.{" "}
                    <button type="button" className="underline" onClick={() => services.refetch()}>Reintentar</button>
                  </p>
                )}
              </div>
            </div>

            {!isUuid(search.centro) ? (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <IconCalendarEvent className="size-4" />
                Seleccioná un centro para ver su calendario de turnos.
              </p>
            ) : (
              <>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-1">
                    <Button type="button" size="icon-sm" variant="ghost" aria-label="Mes anterior" onClick={() => shift(-1)}>
                      <IconChevronLeft />
                    </Button>
                    <h2 className="min-w-44 text-center font-heading text-lg capitalize">{monthTitle(search.anio, search.mes)}</h2>
                    <Button type="button" size="icon-sm" variant="ghost" aria-label="Mes siguiente" onClick={() => shift(1)}>
                      <IconChevronRight />
                    </Button>
                  </div>
                  <Button type="button" size="xs" variant="ghost" onClick={() => summary.refetch()} disabled={summary.isFetching}>
                    <IconRefresh className={summary.isFetching ? "animate-spin" : undefined} />
                    <span className="sr-only sm:not-sr-only">Refrescar</span>
                  </Button>
                </div>
                {summary.isPending ? (
                  <p className="text-sm text-muted-foreground">Cargando calendario…</p>
                ) : summary.isError ? (
                  <div className="flex flex-col items-start gap-2 text-sm text-muted-foreground">
                    <p>No se pudo cargar el calendario.</p>
                    <Button size="sm" variant="outline" onClick={() => summary.refetch()}>Reintentar</Button>
                  </div>
                ) : (
                  <>
                    <div className="overflow-x-auto pb-1">
                    <div className="grid min-w-[42rem] grid-cols-7 gap-1.5" role="grid" aria-label={`Calendario de ${monthTitle(search.anio, search.mes)}`}>
                      {WEEKDAYS.map((weekday) => (
                        <div key={weekday} className="pb-1 text-center text-xs font-medium text-muted-foreground">
                          {weekday}
                        </div>
                      ))}
                      {Array.from({ length: blanks }, (_, index) => (
                        <div key={`blank-${index}`} aria-hidden="true" />
                      ))}
                      {Array.from({ length: totalDays }, (_, index) => {
                        const day = index + 1
                        const iso = monthDayIso(search.anio, search.mes, day)
                        const info = days.get(iso)
                        return <DayCell key={iso} day={day} iso={iso} info={info} centro={search.centro} servicio={search.servicio} />
                      })}
                    </div>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      <span className="font-medium text-blue-700 dark:text-blue-400">conf.</span> = turnos confirmados ·{" "}
                      <span className="font-medium text-emerald-700 dark:text-emerald-400">lib.</span> = horarios disponibles ·{" "}
                      <span className="font-medium text-red-700 dark:text-red-400">ret.</span> = cancelados con horario retenido
                    </p>
                  </>
                )}
              </>
            )}
            <AppointmentCenterSelectionDialog open={centerDialogOpen} onOpenChange={setCenterDialogOpen} selectedCenterId={search.centro} onSelect={(id) => setSearch({ centro: id, servicio: "" })} />
            <AppointmentServiceSelectionDialog open={serviceDialogOpen} onOpenChange={setServiceDialogOpen} centerId={search.centro} selectedServiceId={search.servicio} onSelect={(id) => setSearch({ servicio: id })} />
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function DayCell({ day, iso, info, centro, servicio }: {
  day: number
  iso: string
  info: AdminDaySummaryResponse | undefined
  centro: string
  servicio: string
}) {
  const confirmed = info?.confirmed ?? 0
  const free = info?.free ?? 0
  const retained = info?.retained ?? 0
  const empty = confirmed === 0 && free === 0 && retained === 0

  return (
    <Link
      to="/gestion/turnos/agenda"
      search={{ centro, fecha: iso, servicio }}
      aria-label={`${day}: ${confirmed} confirmados, ${free} libres${retained > 0 ? `, ${retained} retenidos` : ""}`}
      className="flex min-h-20 flex-col gap-0.5 rounded-xl border p-1.5 text-left transition-colors hover:border-muted-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="text-sm font-medium">{day}</span>
      {info === undefined || (!info.hasAgenda && empty) ? (
        <span className="text-[11px] text-muted-foreground">Sin agenda</span>
      ) : (
        <>
          <span className="flex items-center gap-1 text-[11px] font-medium text-blue-700 dark:text-blue-400">
            <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-blue-600 dark:bg-blue-400" />
            {confirmed} conf.
          </span>
          <span className="flex items-center gap-1 text-[11px] font-medium text-emerald-700 dark:text-emerald-400">
            <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-emerald-600 dark:bg-emerald-400" />
            {free} lib.
          </span>
          {retained > 0 && (
            <span className="flex items-center gap-1 text-[11px] font-medium text-red-700 dark:text-red-400">
              <span aria-hidden="true" className="size-1.5 shrink-0 rounded-full bg-red-600 dark:bg-red-400" />
              {retained} ret.
            </span>
          )}
        </>
      )}
    </Link>
  )
}
