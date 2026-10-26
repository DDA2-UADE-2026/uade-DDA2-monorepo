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
  trailingBlanks,
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
import { appointmentToday, isUuid } from "@/lib/appointment-flow"
import { cn } from "@/lib/utils"

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
  const trailing = trailingBlanks(search.anio, search.mes)
  const today = appointmentToday()
  const todayYear = Number(today.slice(0, 4))
  const todayMonth = Number(today.slice(5, 7))
  const isCurrentMonth = search.anio === todayYear && search.mes === todayMonth

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
                    <Button type="button" size="icon-sm" variant="outline" aria-label="Mes anterior" onClick={() => shift(-1)}>
                      <IconChevronLeft />
                    </Button>
                    <h2 className="min-w-44 text-center font-heading text-lg font-medium capitalize">{monthTitle(search.anio, search.mes)}</h2>
                    <Button type="button" size="icon-sm" variant="outline" aria-label="Mes siguiente" onClick={() => shift(1)}>
                      <IconChevronRight />
                    </Button>
                    <Button type="button" size="sm" variant="ghost" disabled={isCurrentMonth} onClick={() => setSearch({ anio: todayYear, mes: todayMonth })}>
                      Hoy
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
                    <div className="overflow-x-auto rounded-xl border bg-card">
                      <div className="min-w-2xl" role="grid" aria-label={`Calendario de ${monthTitle(search.anio, search.mes)}`}>
                        <div role="row" className="grid grid-cols-7 border-b bg-muted/40">
                          {WEEKDAYS.map((weekday, index) => (
                            <div key={weekday} role="columnheader" className={cn("py-2 text-center text-xs font-medium uppercase tracking-wide text-muted-foreground", index >= 5 && "text-muted-foreground/70")}>
                              {weekday}
                            </div>
                          ))}
                        </div>
                        <div className="grid grid-cols-7 gap-px bg-border">
                          {Array.from({ length: blanks }, (_, index) => (
                            <div key={`lead-${index}`} aria-hidden="true" className="min-h-24 bg-muted/30" />
                          ))}
                          {Array.from({ length: totalDays }, (_, index) => {
                            const day = index + 1
                            const iso = monthDayIso(search.anio, search.mes, day)
                            return (
                              <DayCell
                                key={iso}
                                day={day}
                                iso={iso}
                                info={days.get(iso)}
                                today={today}
                                weekend={(blanks + index) % 7 >= 5}
                                centro={search.centro}
                                servicio={search.servicio}
                              />
                            )
                          })}
                          {Array.from({ length: trailing }, (_, index) => (
                            <div key={`trail-${index}`} aria-hidden="true" className="min-h-24 bg-muted/30" />
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
                      <LegendItem dotClass="bg-blue-600 dark:bg-blue-400" label="Turnos confirmados" />
                      <LegendItem dotClass="bg-emerald-600 dark:bg-emerald-400" label="Horarios disponibles" />
                      <LegendItem dotClass="bg-red-600 dark:bg-red-400" label="Cancelados con horario retenido" />
                      <span className="flex items-center gap-1.5">
                        <span aria-hidden="true" className="flex size-4 items-center justify-center rounded-full bg-primary text-[9px] font-semibold text-primary-foreground">{Number(today.slice(8))}</span>
                        Hoy
                      </span>
                    </div>
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

function DayCell({ day, iso, info, today, weekend, centro, servicio }: {
  day: number
  iso: string
  info: AdminDaySummaryResponse | undefined
  today: string
  weekend: boolean
  centro: string
  servicio: string
}) {
  const confirmed = info?.confirmed ?? 0
  const free = info?.free ?? 0
  const retained = info?.retained ?? 0
  const empty = confirmed === 0 && free === 0 && retained === 0
  const noAgenda = info === undefined || (!info.hasAgenda && empty)
  // ISO dates compare lexicographically, so string comparison is enough here.
  const isToday = iso === today
  const isPast = iso < today

  return (
    <Link
      to="/gestion/turnos/agenda"
      search={{ centro, fecha: iso, servicio }}
      role="gridcell"
      aria-current={isToday ? "date" : undefined}
      aria-label={`${day}${isToday ? " (hoy)" : ""}: ${noAgenda ? "sin agenda" : `${confirmed} confirmados, ${free} libres${retained > 0 ? `, ${retained} retenidos` : ""}`}`}
      className={cn(
        "group relative flex min-h-24 flex-col gap-1.5 bg-card p-2 text-left transition-colors hover:bg-accent/60 focus-visible:z-10 focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-inset focus-visible:ring-ring/50",
        weekend && "bg-muted/20",
        isPast && "text-muted-foreground",
        isToday && "bg-primary/5 ring-2 ring-inset ring-primary hover:bg-primary/10",
      )}
    >
      <span className="flex items-center justify-between gap-1">
        <span
          className={cn(
            "flex size-6 items-center justify-center rounded-full text-sm font-medium tabular-nums",
            isToday && "bg-primary font-semibold text-primary-foreground",
          )}
        >
          {day}
        </span>
        {isToday && <span className="text-[10px] font-semibold uppercase tracking-wide text-primary">Hoy</span>}
      </span>
      {noAgenda ? (
        <span className="text-[11px] text-muted-foreground/80">Sin agenda</span>
      ) : (
        <span className={cn("flex flex-wrap gap-1", isPast && "opacity-70")}>
          <CountChip count={confirmed} label="conf." className="bg-blue-500/10 text-blue-700 dark:text-blue-300" />
          <CountChip count={free} label="lib." className="bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" />
          {retained > 0 && <CountChip count={retained} label="ret." className="bg-red-500/10 text-red-700 dark:text-red-300" />}
        </span>
      )}
    </Link>
  )
}

function CountChip({ count, label, className }: { count: number; label: string; className: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[11px] font-medium tabular-nums", count === 0 && "opacity-60", className)}>
      {count} <span className="font-normal">{label}</span>
    </span>
  )
}

function LegendItem({ dotClass, label }: { dotClass: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span aria-hidden="true" className={cn("size-2 rounded-full", dotClass)} />
      {label}
    </span>
  )
}
