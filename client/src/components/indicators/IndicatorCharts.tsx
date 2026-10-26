import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, LabelList, Line, LineChart, Pie, PieChart, XAxis, YAxis } from "recharts"

import { ChartCard, EmptyChart, LegendKey } from "@/components/indicators/IndicatorUi"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { applicationStatusLabels } from "@/lib/application-flow"
import { formatBucket, formatBucketLong, formatCount, formatRate, type Bucket, type Indicators } from "@/lib/indicators"

// Marcas fijas del tablero: grilla de 1px sólida, barras finas con extremo redondeado,
// separación de 2px en el color de la superficie entre segmentos que se tocan.
const GRID = { vertical: false, stroke: "var(--border)", strokeDasharray: undefined } as const
const BAR_SIZE = 20
const GAP = { stroke: "var(--card)", strokeWidth: 2 } as const
const AXIS_TICK = { fontSize: 12 }

type LabelProps = { x?: number | string; y?: number | string; index?: number }

/** Etiqueta directa al final de una línea: identifica la serie sin depender solo de la leyenda. */
function endLabel(name: string, lastIndex: number) {
  return function EndLabel({ x, y, index }: LabelProps) {
    if (index !== lastIndex || x == null || y == null) return null
    return <text x={Number(x) + 8} y={Number(y)} dy={4} className="fill-muted-foreground text-xs">{name}</text>
  }
}

const hasValues = (rows: Array<Record<string, unknown>>, keys: string[]) =>
  rows.some((row) => keys.some((key) => Number(row[key] ?? 0) > 0))

// ---- Solicitudes ----

const trendConfig = {
  submitted: { label: "Presentadas", color: "var(--viz-1)" },
  resolved: { label: "Resueltas", color: "var(--viz-2)" },
} satisfies ChartConfig

export function ApplicationsTrendChart({ data }: { data: Indicators }) {
  const bucket = (data.bucket ?? "WEEK") as Bucket
  const rows = (data.applicationsTrend ?? []).map((point) => ({
    period: point.periodStart ?? "",
    submitted: point.submitted ?? 0,
    resolved: point.resolved ?? 0,
  }))
  return (
    <ChartCard
      title="Solicitudes en el tiempo"
      description={`Presentadas y resueltas por ${bucket === "WEEK" ? "semana" : "mes"}.`}
      table={{
        columns: [bucket === "WEEK" ? "Semana" : "Mes", "Presentadas", "Resueltas"],
        rows: rows.map((row) => [formatBucketLong(row.period, bucket), row.submitted, row.resolved]),
      }}
    >
      {!hasValues(rows, ["submitted", "resolved"]) ? <EmptyChart /> : <>
        <ChartContainer config={trendConfig} className="aspect-auto h-64 w-full">
          <AreaChart data={rows} margin={{ top: 8, right: 84, left: -16, bottom: 0 }}>
            <CartesianGrid {...GRID} />
            <XAxis dataKey="period" tickLine={false} axisLine={false} tick={AXIS_TICK} minTickGap={16} tickFormatter={(value: string) => formatBucket(value, bucket)} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={AXIS_TICK} width={48} />
            <ChartTooltip content={<ChartTooltipContent indicator="line" labelFormatter={(value) => formatBucketLong(String(value), bucket)} />} />
            {(["submitted", "resolved"] as const).map((key) => (
              <Area
                key={key}
                dataKey={key}
                type="monotone"
                stroke={`var(--color-${key})`}
                strokeWidth={2}
                fill={`var(--color-${key})`}
                fillOpacity={0.1}
                dot={false}
                activeDot={{ r: 4, stroke: "var(--card)", strokeWidth: 2 }}
              >
                <LabelList content={endLabel(trendConfig[key].label, rows.length - 1)} />
              </Area>
            ))}
          </AreaChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          <LegendKey shape="line" color="var(--viz-1)" label="Presentadas" />
          <LegendKey shape="line" color="var(--viz-2)" label="Resueltas" />
        </div>
      </>}
    </ChartCard>
  )
}

const statusConfig = { count: { label: "Solicitudes", color: "var(--viz-1)" } } satisfies ChartConfig

export function StatusDistributionChart({ data }: { data: Indicators }) {
  const rows = (data.statusDistribution ?? [])
    .filter((item) => item.status)
    .map((item) => ({ status: applicationStatusLabels[item.status!], count: item.count ?? 0 }))
  return (
    <ChartCard
      title="Etapa actual"
      description="Dónde están hoy las solicitudes presentadas en el período."
      table={{ columns: ["Estado", "Solicitudes"], rows: rows.map((row) => [row.status, row.count]) }}
    >
      {!hasValues(rows, ["count"]) ? <EmptyChart /> : (
        <ChartContainer config={statusConfig} className="aspect-auto w-full" style={{ height: rows.length * 32 + 16 }}>
          <BarChart data={rows} layout="vertical" margin={{ top: 0, right: 40, left: 8, bottom: 0 }}>
            <CartesianGrid horizontal={false} vertical stroke="var(--border)" />
            <XAxis type="number" hide allowDecimals={false} />
            <YAxis type="category" dataKey="status" tickLine={false} axisLine={false} tick={AXIS_TICK} width={150} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="line" />} />
            <Bar dataKey="count" fill="var(--color-count)" barSize={BAR_SIZE} radius={[0, 4, 4, 0]}>
              <LabelList dataKey="count" position="right" className="fill-foreground text-xs tabular-nums" />
            </Bar>
          </BarChart>
        </ChartContainer>
      )}
    </ChartCard>
  )
}

// Resultados: mismo color en todo el tablero, apilados en el orden validado (en curso → espera → aprobadas → rechazadas).
const outcomeConfig = {
  inProgress: { label: "En curso", color: "var(--viz-in-progress)" },
  waitlisted: { label: "Lista de espera", color: "var(--viz-waitlisted)" },
  approved: { label: "Aprobadas", color: "var(--viz-approved)" },
  rejected: { label: "Rechazadas", color: "var(--viz-rejected)" },
} satisfies ChartConfig
const OUTCOME_KEYS = ["inProgress", "waitlisted", "approved", "rejected"] as const

export function OutcomesByProgramChart({ data }: { data: Indicators }) {
  const rows = (data.programs ?? []).slice(0, 8).map((program) => ({
    program: program.programName ?? "Programa",
    inProgress: program.inProgress ?? 0,
    waitlisted: program.waitlisted ?? 0,
    approved: program.approved ?? 0,
    rejected: program.rejected ?? 0,
    submitted: program.submitted ?? 0,
  }))
  const hidden = (data.programs?.length ?? 0) - rows.length
  return (
    <ChartCard
      title="Cobertura por programa"
      description="Solicitudes presentadas en el período según su resultado actual."
      table={{
        columns: ["Programa", "Presentadas", "En curso", "Lista de espera", "Aprobadas", "Rechazadas", "Cobertura"],
        rows: (data.programs ?? []).map((program) => [
          program.programName ?? "Programa",
          program.submitted ?? 0,
          program.inProgress ?? 0,
          program.waitlisted ?? 0,
          program.approved ?? 0,
          program.rejected ?? 0,
          formatRate(program.submitted ? (program.approved ?? 0) / program.submitted : null),
        ]),
      }}
      footer={hidden > 0 && <p className="mt-2 text-xs text-muted-foreground">Se muestran los 8 programas con más solicitudes; el resto está en la vista de tabla.</p>}
    >
      {rows.length === 0 ? <EmptyChart /> : <>
        <ChartContainer config={outcomeConfig} className="aspect-auto w-full" style={{ height: rows.length * 40 + 16 }}>
          <BarChart data={rows} layout="vertical" margin={{ top: 0, right: 40, left: 8, bottom: 0 }}>
            <CartesianGrid horizontal={false} vertical stroke="var(--border)" />
            <XAxis type="number" hide allowDecimals={false} />
            <YAxis type="category" dataKey="program" tickLine={false} axisLine={false} tick={AXIS_TICK} width={150} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="line" />} />
            {OUTCOME_KEYS.map((key, index) => (
              <Bar
                key={key}
                dataKey={key}
                stackId="outcome"
                fill={`var(--color-${key})`}
                barSize={BAR_SIZE}
                {...GAP}
                radius={index === OUTCOME_KEYS.length - 1 ? [0, 4, 4, 0] : 0}
              >
                {index === OUTCOME_KEYS.length - 1 && <LabelList dataKey="submitted" position="right" className="fill-foreground text-xs tabular-nums" />}
              </Bar>
            ))}
          </BarChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          {OUTCOME_KEYS.map((key) => <LegendKey key={key} color={outcomeConfig[key].color} label={outcomeConfig[key].label} />)}
        </div>
      </>}
    </ChartCard>
  )
}

const resolutionConfig = {
  waitlisted: outcomeConfig.waitlisted,
  approved: outcomeConfig.approved,
  rejected: outcomeConfig.rejected,
} satisfies ChartConfig

export function ResolutionsDonut({ data }: { data: Indicators }) {
  const counts = data.resolutions ?? {}
  const rows = (["approved", "waitlisted", "rejected"] as const).map((key) => ({
    key,
    label: resolutionConfig[key].label,
    value: counts[key] ?? 0,
    fill: `var(--color-${key})`,
  }))
  const total = rows.reduce((sum, row) => sum + row.value, 0)
  return (
    <ChartCard
      title="Resoluciones"
      description="Decisiones registradas en el período."
      table={{ columns: ["Resultado", "Solicitudes", "Porcentaje"], rows: rows.map((row) => [row.label, row.value, formatRate(total ? row.value / total : null)]) }}
    >
      {total === 0 ? <EmptyChart message="No se resolvieron solicitudes en el período." /> : (
        <div className="flex flex-col items-center gap-4 sm:flex-row">
          <ChartContainer config={resolutionConfig} className="aspect-square h-44 shrink-0">
            <PieChart>
              <ChartTooltip content={<ChartTooltipContent nameKey="key" hideLabel />} />
              <Pie data={rows} dataKey="value" nameKey="key" innerRadius={52} outerRadius={80} {...GAP} isAnimationActive={false}>
                {rows.map((row) => <Cell key={row.key} fill={row.fill} />)}
              </Pie>
              <text x="50%" y="50%" textAnchor="middle" dominantBaseline="middle" className="fill-foreground font-heading text-2xl font-semibold">{formatCount(total)}</text>
            </PieChart>
          </ChartContainer>
          {/* Leyenda con valor y porcentaje: cada segmento queda etiquetado sin depender del color. */}
          <ul className="grid gap-2">
            {rows.map((row) => (
              <li key={row.key}><LegendKey color={resolutionConfig[row.key].color} label={row.label} value={`${formatCount(row.value)} · ${formatRate(row.value / total)}`} /></li>
            ))}
          </ul>
        </div>
      )}
    </ChartCard>
  )
}

// ---- Visitas ----

const visitsConfig = {
  realized: { label: "Realizadas", color: "var(--viz-3)" },
  notRealized: { label: "No realizadas", color: "var(--viz-2)" },
  cancelled: { label: "Canceladas", color: "var(--viz-1)" },
} satisfies ChartConfig
const VISIT_KEYS = ["realized", "notRealized", "cancelled"] as const

export function VisitsTrendChart({ data }: { data: Indicators }) {
  const bucket = (data.bucket ?? "WEEK") as Bucket
  const rows = (data.visitsTrend ?? []).map((point) => ({
    period: point.periodStart ?? "",
    realized: point.realized ?? 0,
    notRealized: point.notRealized ?? 0,
    cancelled: point.cancelled ?? 0,
    scheduled: point.scheduled ?? 0,
  }))
  const scheduled = rows.reduce((sum, row) => sum + row.scheduled, 0)
  return (
    <ChartCard
      title="Visitas y entrevistas"
      description={`Actuaciones con inicio en el período, por ${bucket === "WEEK" ? "semana" : "mes"} y resultado.`}
      table={{
        columns: [bucket === "WEEK" ? "Semana" : "Mes", "Realizadas", "No realizadas", "Canceladas", "Programadas"],
        rows: rows.map((row) => [formatBucketLong(row.period, bucket), row.realized, row.notRealized, row.cancelled, row.scheduled]),
      }}
      footer={scheduled > 0 && <p className="mt-2 text-xs text-muted-foreground">Además hay {formatCount(scheduled)} programada{scheduled === 1 ? "" : "s"} dentro del período, todavía sin resultado.</p>}
    >
      {!hasValues(rows, [...VISIT_KEYS]) ? <EmptyChart /> : <>
        <ChartContainer config={visitsConfig} className="aspect-auto h-64 w-full">
          <BarChart data={rows} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
            <CartesianGrid {...GRID} />
            <XAxis dataKey="period" tickLine={false} axisLine={false} tick={AXIS_TICK} minTickGap={16} tickFormatter={(value: string) => formatBucket(value, bucket)} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={AXIS_TICK} width={48} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="line" labelFormatter={(value) => formatBucketLong(String(value), bucket)} />} />
            {VISIT_KEYS.map((key, index) => (
              <Bar key={key} dataKey={key} stackId="visits" fill={`var(--color-${key})`} maxBarSize={24} {...GAP} radius={index === VISIT_KEYS.length - 1 ? [4, 4, 0, 0] : 0} />
            ))}
          </BarChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          {VISIT_KEYS.map((key) => <LegendKey key={key} color={visitsConfig[key].color} label={visitsConfig[key].label} />)}
        </div>
      </>}
    </ChartCard>
  )
}

// ---- Carga de trabajo ----

const workloadConfig = {
  activeCases: { label: "Casos activos", color: "var(--viz-1)" },
  scheduledVisits: { label: "Visitas programadas", color: "var(--viz-2)" },
} satisfies ChartConfig

export function WorkloadChart({ data }: { data: Indicators }) {
  const rows = (data.workload ?? []).map((worker) => ({
    name: worker.name || "Sin nombre",
    activeCases: worker.activeCases ?? 0,
    scheduledVisits: worker.scheduledVisits ?? 0,
  }))
  return (
    <ChartCard
      title="Carga de trabajo"
      description="Casos activos y visitas programadas por trabajador asignado, hoy."
      table={{ columns: ["Trabajador", "Casos activos", "Visitas programadas"], rows: rows.map((row) => [row.name, row.activeCases, row.scheduledVisits]) }}
    >
      {rows.length === 0 ? <EmptyChart message="No hay casos activos asignados." /> : <>
        <ChartContainer config={workloadConfig} className="aspect-auto w-full" style={{ height: rows.length * 52 + 16 }}>
          <BarChart data={rows} layout="vertical" margin={{ top: 0, right: 32, left: 8, bottom: 0 }} barGap={2}>
            <CartesianGrid horizontal={false} vertical stroke="var(--border)" />
            <XAxis type="number" hide allowDecimals={false} />
            <YAxis type="category" dataKey="name" tickLine={false} axisLine={false} tick={AXIS_TICK} width={140} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="line" />} />
            {(["activeCases", "scheduledVisits"] as const).map((key) => (
              <Bar key={key} dataKey={key} fill={`var(--color-${key})`} barSize={16} radius={[0, 4, 4, 0]}>
                <LabelList dataKey={key} position="right" className="fill-foreground text-xs tabular-nums" />
              </Bar>
            ))}
          </BarChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          <LegendKey color="var(--viz-1)" label="Casos activos" />
          <LegendKey color="var(--viz-2)" label="Visitas programadas" />
        </div>
      </>}
    </ChartCard>
  )
}

// ---- Turnos ----

const appointmentsConfig = {
  confirmed: { label: "Confirmados", color: "var(--viz-1)" },
  cancelled: { label: "Cancelados", color: "var(--viz-2)" },
} satisfies ChartConfig

export function AppointmentsTrendChart({ data }: { data: Indicators }) {
  const bucket = (data.bucket ?? "WEEK") as Bucket
  const rows = (data.appointments?.trend ?? []).map((point) => ({
    period: point.periodStart ?? "",
    confirmed: point.confirmed ?? 0,
    cancelled: point.cancelled ?? 0,
  }))
  return (
    <ChartCard
      title="Turnos de salud comunitaria"
      description={`Turnos con inicio en el período, por ${bucket === "WEEK" ? "semana" : "mes"}. No depende del programa elegido.`}
      table={{
        columns: [bucket === "WEEK" ? "Semana" : "Mes", "Confirmados", "Cancelados"],
        rows: rows.map((row) => [formatBucketLong(row.period, bucket), row.confirmed, row.cancelled]),
      }}
    >
      {!hasValues(rows, ["confirmed", "cancelled"]) ? <EmptyChart /> : <>
        <ChartContainer config={appointmentsConfig} className="aspect-auto h-56 w-full">
          <LineChart data={rows} margin={{ top: 8, right: 96, left: -16, bottom: 0 }}>
            <CartesianGrid {...GRID} />
            <XAxis dataKey="period" tickLine={false} axisLine={false} tick={AXIS_TICK} minTickGap={16} tickFormatter={(value: string) => formatBucket(value, bucket)} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={AXIS_TICK} width={48} />
            <ChartTooltip content={<ChartTooltipContent indicator="line" labelFormatter={(value) => formatBucketLong(String(value), bucket)} />} />
            {(["confirmed", "cancelled"] as const).map((key) => (
              <Line key={key} dataKey={key} type="monotone" stroke={`var(--color-${key})`} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" dot={false} activeDot={{ r: 4, stroke: "var(--card)", strokeWidth: 2 }}>
                <LabelList content={endLabel(appointmentsConfig[key].label, rows.length - 1)} />
              </Line>
            ))}
          </LineChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          <LegendKey shape="line" color="var(--viz-1)" label="Confirmados" />
          <LegendKey shape="line" color="var(--viz-2)" label="Cancelados" />
        </div>
      </>}
    </ChartCard>
  )
}
