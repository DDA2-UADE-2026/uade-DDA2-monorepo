import { IconAlertCircle, IconCalendarUser, IconChevronLeft, IconChevronRight, IconRefresh } from "@tabler/icons-react"
import { useQuery } from "@tanstack/react-query"
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router"

import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { UserChip, UserInline } from "@/components/UserChip"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { listProfessionalAppointmentsOptions } from "@/generated/@tanstack/react-query.gen"
import type { ProfessionalAppointmentResponse } from "@/generated/types.gen"
import { appointmentToday, formatAppointmentRange, formatAppointmentTime } from "@/lib/appointment-flow"
import {
  appointmentDayKey,
  attentionLabel,
  professionalTurnSearchSchema,
  shiftTurnDate,
  turnViewRange,
  turnViewTitle,
  type TurnView,
} from "@/lib/community-attention"
import { cn } from "@/lib/utils"

const WEEKDAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]
const VIEW_LABELS: Record<TurnView, string> = { dia: "Día", semana: "Semana", mes: "Mes" }
const PREVIOUS_LABELS: Record<TurnView, string> = { dia: "Día anterior", semana: "Semana anterior", mes: "Mes anterior" }
const NEXT_LABELS: Record<TurnView, string> = { dia: "Día siguiente", semana: "Semana siguiente", mes: "Mes siguiente" }
// Month cells show this many turns before collapsing the rest into "+N más".
const MONTH_CELL_LIMIT = 3

// Dot colors per attention label, readable in light and dark mode.
const statusDotClass: Record<string, string> = {
  Atendido: "bg-emerald-500 dark:bg-emerald-400",
  Ausente: "bg-red-500 dark:bg-red-400",
  Cancelado: "bg-muted-foreground/50",
  "Sin registrar": "bg-amber-500 dark:bg-amber-400",
  "Aún no comenzó": "bg-sky-500 dark:bg-sky-400",
}

export const Route = createFileRoute("/_app/gestion/mis-turnos/")({
  validateSearch: professionalTurnSearchSchema,
  component: RouteComponent,
})

function RouteComponent() {
  const { fecha, vista } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const go = (search: { fecha?: string; vista?: TurnView }) => void navigate({ search: (prev) => ({ ...prev, ...search }) })
  const range = turnViewRange(fecha, vista)
  const turns = useQuery(listProfessionalAppointmentsOptions({
    query: { date: range.from, ...(range.to !== range.from ? { to: range.to } : {}) },
  }))
  const today = appointmentToday()

  const byDay = new Map<string, ProfessionalAppointmentResponse[]>()
  for (const row of turns.data ?? []) {
    const key = appointmentDayKey(row.appointment?.startsAt)
    if (!key) continue
    byDay.set(key, [...(byDay.get(key) ?? []), row])
  }
  const total = turns.data?.length ?? 0

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
                <p className="text-sm text-muted-foreground">Consultá tus turnos y registrá la asistencia.</p>
              </div>
              <div className="flex flex-wrap items-end gap-2">
                <div className="space-y-1">
                  <Label htmlFor="professional-turn-date">Fecha</Label>
                  <ProgramDatePicker
                    id="professional-turn-date"
                    value={fecha}
                    onChange={(value) => { if (value) go({ fecha: value }) }}
                  />
                </div>
                <Button variant="outline" disabled={fecha === today} onClick={() => go({ fecha: today })}>Hoy</Button>
                <Button variant="outline" disabled={turns.isFetching} onClick={() => void turns.refetch()}>
                  <IconRefresh className={turns.isFetching ? "animate-spin" : undefined} /><span className="sr-only sm:not-sr-only">Refrescar</span>
                </Button>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex min-w-0 items-center gap-1">
                <Button size="icon-sm" variant="outline" aria-label={PREVIOUS_LABELS[vista]} onClick={() => go({ fecha: shiftTurnDate(fecha, vista, -1) })}>
                  <IconChevronLeft />
                </Button>
                <Button size="icon-sm" variant="outline" aria-label={NEXT_LABELS[vista]} onClick={() => go({ fecha: shiftTurnDate(fecha, vista, 1) })}>
                  <IconChevronRight />
                </Button>
                <h2 className="ml-2 truncate font-heading text-lg font-medium first-letter:uppercase" aria-live="polite">
                  {turnViewTitle(fecha, vista)}
                </h2>
              </div>
              <ToggleGroup
                variant="outline"
                size="sm"
                aria-label="Vista de la agenda"
                value={[vista]}
                onValueChange={(value: string[]) => { const next = value[0] as TurnView | undefined; if (next) go({ vista: next }) }}
              >
                {(Object.keys(VIEW_LABELS) as TurnView[]).map((view) => (
                  <ToggleGroupItem key={view} value={view} aria-label={`Vista ${VIEW_LABELS[view].toLowerCase()}`}>
                    {VIEW_LABELS[view]}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
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
            ) : vista === "dia" ? (
              <DayView rows={turns.data} fecha={fecha} />
            ) : (
              <>
                <p className="text-sm text-muted-foreground">
                  {total === 0 ? "Sin turnos en este período." : total === 1 ? "1 turno en este período" : `${total} turnos en este período`}
                </p>
                {vista === "semana"
                  ? <WeekView days={range.days} byDay={byDay} today={today} />
                  : <MonthView days={range.days} byDay={byDay} today={today} month={fecha.slice(0, 7)} />}
                <Legend />
              </>
            )}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function DayView({ rows, fecha }: { rows: ProfessionalAppointmentResponse[]; fecha: string }) {
  if (!rows.length) {
    return (
      <Empty className="border">
        <EmptyHeader>
          <EmptyMedia variant="icon"><IconCalendarUser /></EmptyMedia>
          <EmptyTitle>Sin turnos asignados</EmptyTitle>
          <EmptyDescription>No tenés turnos asignados para esta fecha.</EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Agenda del día</CardTitle>
        <CardDescription>{rows.length === 1 ? "1 turno asignado" : `${rows.length} turnos asignados`}</CardDescription>
      </CardHeader>
      <CardContent className="px-0">
        <Table className="min-w-[44rem]">
          <TableHeader><TableRow>
            <TableHead>Horario</TableHead><TableHead>Ciudadano</TableHead>
            <TableHead>Servicio y centro</TableHead><TableHead>Resultado</TableHead><TableHead>Acción</TableHead>
          </TableRow></TableHeader>
          <TableBody>
            {rows.map((row) => {
              const appointment = row.appointment
              const label = attentionLabel(row)
              return (
                <TableRow key={appointment?.id}>
                  <TableCell className="whitespace-nowrap font-medium">{formatAppointmentRange(appointment?.startsAt, appointment?.endsAt)}</TableCell>
                  <TableCell className="min-w-44"><UserChip user={{ id: row.citizenId, name: row.citizenName }} /></TableCell>
                  <TableCell>{appointment?.serviceName ?? "—"}<span className="block text-xs text-muted-foreground">{appointment?.centerName}</span></TableCell>
                  <TableCell><Badge variant={label === "Cancelado" ? "secondary" : label === "Sin registrar" ? "outline" : "default"}>{label}</Badge></TableCell>
                  <TableCell>
                    {appointment?.id && <Button size="sm" variant="outline" render={<Link to="/gestion/mis-turnos/$turnoId" params={{ turnoId: appointment.id }} search={{ fecha, vista: "dia" }} />}>
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
  )
}

function WeekView({ days, byDay, today }: { days: string[]; byDay: Map<string, ProfessionalAppointmentResponse[]>; today: string }) {
  return (
    <div className="grid gap-3 md:grid-cols-7 md:gap-2">
      {days.map((day, index) => {
        const rows = byDay.get(day) ?? []
        const isToday = day === today
        return (
          <section
            key={day}
            aria-label={`${WEEKDAYS[index]} ${Number(day.slice(8))}${isToday ? " (hoy)" : ""}`}
            className={cn("flex min-w-0 flex-col rounded-xl border bg-card md:min-h-64", isToday && "border-primary ring-1 ring-primary")}
          >
            <DayHeading day={day} weekday={WEEKDAYS[index]} isToday={isToday} count={rows.length} />
            <div className="flex flex-1 flex-col gap-1.5 p-2">
              {rows.length === 0
                ? <p className="px-1 py-2 text-xs text-muted-foreground">Sin turnos</p>
                : rows.map((row) => <TurnItem key={row.appointment?.id} row={row} day={day} vista="semana" detailed />)}
            </div>
          </section>
        )
      })}
    </div>
  )
}

function MonthView({ days, byDay, today, month }: {
  days: string[]
  byDay: Map<string, ProfessionalAppointmentResponse[]>
  today: string
  month: string
}) {
  return (
    <div className="overflow-x-auto rounded-xl border bg-card">
      <div className="min-w-2xl">
        <div className="grid grid-cols-7 border-b bg-muted/40">
          {WEEKDAYS.map((weekday) => (
            <div key={weekday} className="py-2 text-center text-xs font-medium uppercase tracking-wide text-muted-foreground">{weekday}</div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-px bg-border">
          {days.map((day) => {
            const rows = byDay.get(day) ?? []
            const isToday = day === today
            const outside = day.slice(0, 7) !== month
            const hidden = rows.length - MONTH_CELL_LIMIT
            return (
              <div
                key={day}
                className={cn(
                  "flex min-h-28 min-w-0 flex-col gap-1 bg-card p-1.5",
                  outside && "bg-muted/30",
                  isToday && "bg-primary/5 ring-2 ring-inset ring-primary",
                )}
              >
                <Link
                  to="/gestion/mis-turnos"
                  search={{ fecha: day, vista: "dia" }}
                  aria-label={`Ver el ${Number(day.slice(8))}: ${rows.length === 1 ? "1 turno" : `${rows.length} turnos`}${isToday ? " (hoy)" : ""}`}
                  aria-current={isToday ? "date" : undefined}
                  className={cn(
                    "flex size-6 items-center justify-center self-start rounded-full text-sm font-medium tabular-nums hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none",
                    outside && "text-muted-foreground",
                    isToday && "bg-primary font-semibold text-primary-foreground hover:bg-primary/90",
                  )}
                >
                  {Number(day.slice(8))}
                </Link>
                {rows.slice(0, MONTH_CELL_LIMIT).map((row) => <TurnItem key={row.appointment?.id} row={row} day={day} vista="mes" />)}
                {hidden > 0 && (
                  <Link to="/gestion/mis-turnos" search={{ fecha: day, vista: "dia" }} className="px-1 text-[11px] font-medium text-muted-foreground hover:text-foreground hover:underline">
                    +{hidden} más
                  </Link>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

function DayHeading({ day, weekday, isToday, count }: { day: string; weekday: string; isToday: boolean; count: number }) {
  return (
    <Link
      to="/gestion/mis-turnos"
      search={{ fecha: day, vista: "dia" }}
      aria-current={isToday ? "date" : undefined}
      className="flex items-center justify-between gap-2 border-b px-3 py-2 hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none md:rounded-t-xl"
    >
      <span className="flex items-center gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{weekday}</span>
        <span className={cn("flex size-6 items-center justify-center rounded-full text-sm font-medium tabular-nums", isToday && "bg-primary font-semibold text-primary-foreground")}>
          {Number(day.slice(8))}
        </span>
      </span>
      {count > 0 && <span className="text-xs tabular-nums text-muted-foreground">{count}</span>}
    </Link>
  )
}

function TurnItem({ row, day, vista, detailed = false }: { row: ProfessionalAppointmentResponse; day: string; vista: TurnView; detailed?: boolean }) {
  const appointment = row.appointment
  const label = attentionLabel(row)
  const cancelled = label === "Cancelado"
  const content = (
    <>
      <span className="flex items-center gap-1.5">
        <span aria-hidden="true" className={cn("size-2 shrink-0 rounded-full", statusDotClass[label])} />
        <span className="font-medium tabular-nums">{formatAppointmentTime(appointment?.startsAt)}</span>
        {!detailed && <span className="truncate">{row.citizenName ?? "—"}</span>}
      </span>
      {detailed && (
        <>
          <UserInline user={{ id: row.citizenId, name: row.citizenName }} className="mt-1 flex" />
          <span className="mt-0.5 block truncate text-muted-foreground">{appointment?.serviceName ?? "—"}</span>
        </>
      )}
    </>
  )
  const className = cn(
    "block min-w-0 rounded-md border border-transparent px-1.5 py-1 text-left text-[11px] transition-colors hover:border-border hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none",
    detailed && "border-border bg-background px-2 py-1.5 text-xs",
    cancelled && "text-muted-foreground line-through",
  )
  const accessibleName = `${formatAppointmentTime(appointment?.startsAt)} ${row.citizenName ?? ""}: ${label}`

  return appointment?.id ? (
    <Link to="/gestion/mis-turnos/$turnoId" params={{ turnoId: appointment.id }} search={{ fecha: day, vista }} aria-label={accessibleName} title={accessibleName} className={className}>
      {content}
    </Link>
  ) : <div className={className} title={accessibleName}>{content}</div>
}

function Legend() {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
      {Object.entries(statusDotClass).map(([label, dot]) => (
        <span key={label} className="flex items-center gap-1.5">
          <span aria-hidden="true" className={cn("size-2 rounded-full", dot)} />
          {label}
        </span>
      ))}
    </div>
  )
}
