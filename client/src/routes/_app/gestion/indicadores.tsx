import { IconAlertTriangle, IconRefresh } from "@tabler/icons-react"
import { keepPreviousData, useQuery } from "@tanstack/react-query"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { z } from "zod"

import {
  AppointmentsTrendChart,
  ApplicationsTrendChart,
  OutcomesByProgramChart,
  ResolutionsDonut,
  StatusDistributionChart,
  VisitsTrendChart,
  WorkloadChart,
} from "@/components/indicators/IndicatorCharts"
import { AttentionPanel, CapacityMeters, EvaluationHeatmap } from "@/components/indicators/IndicatorPanels"
import { SectionTitle, StatTile } from "@/components/indicators/IndicatorUi"
import { OutletNavBreadcrumbs } from "@/components/layout/OutletNavBreadcrumbs"
import { OutletNavSidebarTrigger, OutletNavSticky, SidebarShell, SidebarShellContent } from "@/components/layout/OutletNav"
import { ProgramDatePicker } from "@/components/programs/ProgramDatePicker"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { getIndicatorsOptions, list1Options } from "@/generated/@tanstack/react-query.gen"
import {
  delta,
  formatCount,
  formatDays,
  formatRate,
  presetRange,
  RANGE_PRESETS,
  type Indicators,
  type RangePreset,
} from "@/lib/indicators"
import { cn } from "@/lib/utils"

const isoDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/)
const searchSchema = z.object({
  desde: isoDate.optional().catch(undefined),
  hasta: isoDate.optional().catch(undefined),
  programa: z.uuid().optional().catch(undefined),
})

export const Route = createFileRoute("/_app/gestion/indicadores")({
  validateSearch: searchSchema,
  component: RouteComponent,
})

const ALL_PROGRAMS = "__all__"

function RouteComponent() {
  const { user } = Route.useRouteContext()
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const canView = (user.permissions ?? []).includes("indicators:view")
  const fallback = presetRange("90d")
  const from = search.desde ?? fallback.from
  const to = search.hasta ?? fallback.to
  const activePreset = (Object.keys(RANGE_PRESETS) as RangePreset[]).find((preset) => {
    const range = presetRange(preset)
    return range.from === from && range.to === to
  })

  const indicators = useQuery({
    ...getIndicatorsOptions({ query: { from, to, programId: search.programa } }),
    enabled: canView,
    placeholderData: keepPreviousData,
  })
  const programs = useQuery({ ...list1Options({ query: { page: 0, size: 100 } }), enabled: canView })
  const programItems = [
    { value: ALL_PROGRAMS, label: "Todos los programas" },
    ...(programs.data?.content ?? []).filter((program) => program.id).map((program) => ({ value: program.id!, label: program.name ?? "Programa" })),
  ]
  const setSearch = (next: Partial<z.infer<typeof searchSchema>>) =>
    void navigate({ search: (previous) => ({ ...previous, ...next }), replace: true })

  return (
    <SidebarShell>
      <OutletNavSticky><OutletNavSidebarTrigger withSeparator /><OutletNavBreadcrumbs items={[{ label: "Indicadores" }]} /></OutletNavSticky>
      <SidebarShellContent>
        <div className="min-h-0 flex-1 overflow-auto">
          <main className="mx-auto w-full max-w-7xl space-y-5 p-4 lg:p-6">
            <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
              <div>
                <h1 className="font-heading text-2xl font-medium">Indicadores</h1>
                <p className="text-sm text-muted-foreground">Cobertura, cupos, evaluación, visitas y resultados del área de Desarrollo Social.</p>
              </div>
              {indicators.data && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>Actualizado {new Date(indicators.dataUpdatedAt).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" })}</span>
                  <Button size="xs" variant="ghost" disabled={indicators.isFetching} onClick={() => void indicators.refetch()}>
                    <IconRefresh className={indicators.isFetching ? "animate-spin" : undefined} />
                    <span className="sr-only sm:not-sr-only">Refrescar</span>
                  </Button>
                </div>
              )}
            </header>

            {!canView ? (
              <Alert><IconAlertTriangle /><AlertTitle>Sin acceso</AlertTitle><AlertDescription>Tu rol no tiene permiso para consultar los indicadores.</AlertDescription></Alert>
            ) : <>
              {/* Una sola fila de filtros: todo lo de abajo se calcula con el mismo período y programa. */}
              <section aria-label="Filtros" className="flex flex-wrap items-end gap-3 rounded-xl border bg-card p-3">
                <div className="space-y-1.5">
                  <Label id="indicators-range-label">Período</Label>
                  <ToggleGroup
                    variant="outline"
                    size="sm"
                    aria-labelledby="indicators-range-label"
                    value={activePreset ? [activePreset] : []}
                    onValueChange={(value: string[]) => {
                      const preset = value[0] as RangePreset | undefined
                      if (!preset) return
                      const range = presetRange(preset)
                      setSearch({ desde: range.from, hasta: range.to })
                    }}
                  >
                    {(Object.keys(RANGE_PRESETS) as RangePreset[]).map((preset) => <ToggleGroupItem key={preset} value={preset}>{RANGE_PRESETS[preset]}</ToggleGroupItem>)}
                  </ToggleGroup>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="indicators-from">Desde</Label>
                    <ProgramDatePicker id="indicators-from" value={from} disabled={{ after: new Date(to + "T00:00") }} onChange={(value) => value && setSearch({ desde: value, hasta: to })} />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="indicators-to">Hasta</Label>
                    <ProgramDatePicker id="indicators-to" value={to} disabled={{ before: new Date(from + "T00:00") }} onChange={(value) => value && setSearch({ desde: from, hasta: value })} />
                  </div>
                </div>
                <div className="flex min-w-56 flex-1 flex-col gap-1.5 sm:max-w-72">
                  <Label htmlFor="indicators-program">Programa</Label>
                  <Select items={programItems} value={search.programa ?? ALL_PROGRAMS} onValueChange={(value) => setSearch({ programa: !value || value === ALL_PROGRAMS ? undefined : value as string })}>
                    <SelectTrigger id="indicators-program" className="w-full"><SelectValue /></SelectTrigger>
                    <SelectContent>{programItems.map((item) => <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
              </section>

              {indicators.isPending ? <DashboardSkeleton /> : indicators.isError ? (
                <Alert variant="destructive">
                  <IconAlertTriangle />
                  <AlertTitle>No se pudieron cargar los indicadores</AlertTitle>
                  <AlertDescription className="flex flex-col items-start gap-2">
                    <span>{indicators.error.message ?? "Intentá nuevamente."}</span>
                    <Button size="sm" variant="outline" onClick={() => void indicators.refetch()}>Reintentar</Button>
                  </AlertDescription>
                </Alert>
              ) : (
                // Al cambiar filtros se conserva el render anterior, atenuado, hasta que llega el nuevo.
                <div aria-busy={indicators.isFetching} className={cn("space-y-5 transition-opacity", indicators.isPlaceholderData && "opacity-60")}>
                  <Dashboard data={indicators.data} />
                </div>
              )}
            </>}
          </main>
        </div>
      </SidebarShellContent>
    </SidebarShell>
  )
}

function Dashboard({ data }: { data: Indicators }) {
  const summary = data.summary ?? {}
  const stage = data.stageTimes ?? {}
  const reschedule = data.rescheduleRequests ?? {}
  const documents = data.documents ?? {}
  const appointments = data.appointments ?? {}
  const activities = data.activities ?? {}
  const rescheduleTotal = (reschedule.pending ?? 0) + (reschedule.approved ?? 0) + (reschedule.rejected ?? 0)
  const attentionTotal = (appointments.attended ?? 0) + (appointments.absent ?? 0)
  const attendanceTotal = (activities.present ?? 0) + (activities.absent ?? 0)

  return <>
    <section aria-label="Resumen" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
      <StatTile
        label="Solicitudes presentadas"
        value={formatCount(summary.applicationsSubmitted)}
        change={delta(summary.applicationsSubmitted ?? 0, summary.previousApplicationsSubmitted ?? 0)}
        hint={summary.previousApplicationsSubmitted === 0 ? "Sin presentaciones en el período anterior." : undefined}
      />
      <StatTile label="Tasa de aprobación" value={formatRate(summary.approvalRate)} hint={`Sobre ${formatCount(summary.resolved)} resueltas en el período.`} />
      <StatTile
        label="Cupo ocupado"
        value={formatRate(summary.capacityUsedRate)}
        hint={`${formatCount(summary.approvedTotal)} aprobadas de ${formatCount(summary.capacityTotal)} lugares en ediciones vigentes.`}
      />
      <StatTile label="Visitas realizadas" value={formatCount(summary.visitsRealized)} change={delta(summary.visitsRealized ?? 0, summary.previousVisitsRealized ?? 0)} />
      <StatTile label="Tiempo hasta resolver" value={formatDays(summary.averageDaysToResolution)} hint="Promedio desde la presentación." upIsGood={false} />
    </section>

    <SectionTitle title="Solicitudes" description="Presentaciones del período y su recorrido." />
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2"><ApplicationsTrendChart data={data} /></div>
      <ResolutionsDonut data={data} />
    </div>
    <div className="grid gap-4 lg:grid-cols-2">
      <StatusDistributionChart data={data} />
      <OutcomesByProgramChart data={data} />
    </div>

    <SectionTitle title="Cupos y tiempos" />
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2"><CapacityMeters data={data} /></div>
      <div className="grid content-start gap-3">
        <StatTile label="Presentación → evaluación" value={formatDays(stage.averageDaysToEvaluation)} />
        <StatTile label="Evaluación → resolución" value={formatDays(stage.averageDaysEvaluationToResolution)} />
        <StatTile label="Presentación → resolución" value={formatDays(stage.averageDaysToResolution)} hint={`Promedio de ${formatCount(stage.sampleSize)} solicitudes resueltas en el período.`} />
      </div>
    </div>

    <SectionTitle title="Evaluación social" />
    <div className="grid gap-4 lg:grid-cols-2">
      <EvaluationHeatmap data={data} />
      <AttentionPanel data={data} />
    </div>

    <SectionTitle title="Visitas y carga de trabajo" />
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2"><VisitsTrendChart data={data} /></div>
      <div className="grid content-start gap-3">
        <StatTile label="Pedidos de reprogramación" value={formatCount(rescheduleTotal)} hint="Presentados por ciudadanos en el período." />
        <StatTile label="Reprogramaciones aprobadas" value={formatCount(reschedule.approved)} hint={`${formatCount(reschedule.rejected)} rechazadas · ${formatCount(reschedule.pending)} pendientes.`} />
      </div>
    </div>
    <WorkloadChart data={data} />

    <SectionTitle title="Documentación, turnos y actividades" description="Turnos y actividades no dependen del programa elegido." />
    <div className="grid gap-3 sm:grid-cols-3">
      <StatTile label="Documentos pendientes de revisión" value={formatCount(documents.pending)} hint="Subidos en el período." />
      <StatTile label="Documentos válidos" value={formatCount(documents.valid)} />
      <StatTile label="Documentos observados" value={formatCount(documents.observed)} />
    </div>
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="lg:col-span-2"><AppointmentsTrendChart data={data} /></div>
      <div className="grid content-start gap-3">
        <StatTile label="Asistencia a turnos" value={formatRate(attentionTotal ? (appointments.attended ?? 0) / attentionTotal : null)} hint={`${formatCount(appointments.attended)} atendidos · ${formatCount(appointments.absent)} ausentes.`} />
        <StatTile label="Asistencia a actividades" value={formatRate(attendanceTotal ? (activities.present ?? 0) / attendanceTotal : null)} hint={`${formatCount(activities.enrollments)} inscripciones · ${formatCount(activities.unrecorded)} sin registrar.`} />
      </div>
    </div>
  </>
}

function DashboardSkeleton() {
  return (
    <div className="space-y-4" role="status" aria-label="Cargando indicadores">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">{Array.from({ length: 5 }, (_, index) => <Skeleton key={index} className="h-28 rounded-xl" />)}</div>
      <div className="grid gap-4 lg:grid-cols-3"><Skeleton className="h-80 rounded-xl lg:col-span-2" /><Skeleton className="h-80 rounded-xl" /></div>
      <span className="sr-only">Cargando indicadores…</span>
    </div>
  )
}
