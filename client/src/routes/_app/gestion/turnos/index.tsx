import { IconCalendarEvent, IconChevronLeft, IconChevronRight, IconRefresh } from "@tabler/icons-react"
import { useInfiniteQuery, useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { useState } from "react"

import {
  adminCalendarSearchSchema,
  daysInMonth,
  leadingBlanks,
  monthDayIso,
  monthTitle,
  shiftMonth,
} from "@/components/turnos/adminAppointmentFilters"
import { adminCenterOptions } from "@/components/turnos/adminCenterOptions"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
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
  const [centerSearchInput, setCenterSearchInput] = useState("")
  const [centerSearch, setCenterSearch] = useState("")

  const centers = useInfiniteQuery(adminCenterOptions(centerSearch))
  const centerOptions = Array.from(
    new Map(centers.data?.pages.flatMap((page) => page.content ?? []).map((center) => [center.id, center]) ?? []).values(),
  )
  const selectedCenterDetail = useQuery({
    ...getMunicipalCenterOptions({ path: { id: search.centro } }),
    enabled: isUuid(search.centro),
  })
  const selectedCenter = centerOptions.find((center) => center.id === search.centro) ?? selectedCenterDetail.data
  const selectOptions = selectedCenter && !centerOptions.some((center) => center.id === selectedCenter.id)
    ? [selectedCenter, ...centerOptions]
    : centerOptions

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
          <div className="mx-2 space-y-4 py-2 sm:mx-4! lg:py-4">
            <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
              <div className="space-y-1.5">
                <Label htmlFor="turnos-centro">Centro</Label>
                <div className="flex gap-2">
                  <Input
                    aria-label="Buscar centro por nombre"
                    placeholder="Buscar centro por nombre…"
                    value={centerSearchInput}
                    onChange={(event) => setCenterSearchInput(event.target.value)}
                    onKeyDown={(event) => { if (event.key === "Enter") setCenterSearch(centerSearchInput.trim()) }}
                  />
                  <Button type="button" variant="outline" onClick={() => setCenterSearch(centerSearchInput.trim())}>
                    Buscar
                  </Button>
                </div>
                <Select
                  value={search.centro}
                  onValueChange={(value) => setSearch({ centro: value ?? "", servicio: "" })}
                  disabled={centers.isPending && !selectedCenter}
                >
                  <SelectTrigger id="turnos-centro" aria-label="Centro">
                    <SelectValue placeholder="Seleccioná un centro">
                      {() => selectedCenter?.name ?? "Seleccioná un centro"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {selectOptions.map((center) => (
                      <SelectItem key={center.id} value={center.id ?? ""}>{center.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {!centers.isPending && !centers.isError && centerOptions.length === 0 && (
                  <p className="text-xs text-muted-foreground">No hay centros con ese criterio.</p>
                )}
                {centers.hasNextPage && (
                  <Button type="button" size="sm" variant="ghost" disabled={centers.isFetchingNextPage} onClick={() => centers.fetchNextPage()}>
                    {centers.isFetchingNextPage ? "Cargando centros…" : "Cargar más centros"}
                  </Button>
                )}
                {centers.isFetchNextPageError && (
                  <p className="text-xs text-destructive">
                    No se pudieron cargar más centros.{" "}
                    <button type="button" className="underline" onClick={() => centers.fetchNextPage()}>Reintentar</button>
                  </p>
                )}
                {centers.isError && !centers.isFetchNextPageError && (
                  <p className="text-xs text-destructive">
                    No se pudieron cargar los centros.{" "}
                    <button type="button" className="underline" onClick={() => centers.refetch()}>Reintentar</button>
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="turnos-servicio">Servicio</Label>
                <Select
                  value={search.servicio}
                  onValueChange={(value) => setSearch({ servicio: value ?? "" })}
                  disabled={!isUuid(search.centro) || services.isPending}
                >
                  <SelectTrigger id="turnos-servicio" aria-label="Servicio">
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
                <div className="flex items-center justify-between gap-2">
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
                    Actualizar
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
                    <div className="grid grid-cols-7 gap-1.5" role="grid" aria-label={`Calendario de ${monthTitle(search.anio, search.mes)}`}>
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
                    <p className="text-xs text-muted-foreground">
                      <span className="font-medium text-blue-700 dark:text-blue-400">conf.</span> = turnos confirmados ·{" "}
                      <span className="font-medium text-emerald-700 dark:text-emerald-400">lib.</span> = horarios disponibles ·{" "}
                      ret. = cancelados con horario retenido
                    </p>
                  </>
                )}
              </>
            )}
          </div>
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
          {retained > 0 && <span className="text-[11px] text-muted-foreground">{retained} ret.</span>}
        </>
      )}
    </Link>
  )
}
