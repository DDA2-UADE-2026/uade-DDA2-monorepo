import { Bar, BarChart, CartesianGrid, LabelList, XAxis, YAxis } from "recharts"

import { ChartCard, EmptyChart, LegendKey } from "@/components/indicators/IndicatorUi"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import type { CoreModuleDashboard } from "@/generated/types.gen"
import { coreStatusLabel } from "@/lib/core-events"
import { formatCount } from "@/lib/indicators"

// Mismas marcas que el tablero de indicadores.
const GRID = { vertical: false, stroke: "var(--border)", strokeDasharray: undefined } as const
const BAR_SIZE = 20
const AXIS_TICK = { fontSize: 12 }

function formatHour(value: string, long = false) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return long
    ? date.toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" })
    : date.toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" })
}

const volumeConfig = {
  accepted: { label: "Aceptados", color: "var(--viz-1)" },
  rejected: { label: "Rechazados", color: "var(--viz-rejected)" },
} satisfies ChartConfig

export function HourlyVolumeChart({ data }: { data: CoreModuleDashboard }) {
  const rows = (data.volumeByHour ?? []).map((point) => {
    const total = point.total ?? 0
    const rejected = point.rejected ?? 0
    return { hour: point.hour ?? "", accepted: Math.max(0, total - rejected), rejected, total }
  })
  const multiDay = (data.windowHours ?? 24) > 24
  return (
    <ChartCard
      title="Volumen por hora"
      description={`Eventos de nuestro módulo procesados por Core en las últimas ${data.windowHours ?? 24} h.`}
      table={{
        columns: ["Hora", "Total", "Aceptados", "Rechazados"],
        rows: rows.map((row) => [formatHour(row.hour, true), row.total, row.accepted, row.rejected]),
      }}
    >
      {!rows.some((row) => row.total > 0) ? <EmptyChart message="Sin eventos en la ventana elegida." /> : <>
        <ChartContainer config={volumeConfig} className="aspect-auto h-64 w-full">
          <BarChart data={rows} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
            <CartesianGrid {...GRID} />
            <XAxis dataKey="hour" tickLine={false} axisLine={false} tick={AXIS_TICK} minTickGap={24} tickFormatter={(value: string) => formatHour(value, multiDay)} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={AXIS_TICK} width={48} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="dot" labelFormatter={(value) => formatHour(String(value), true)} />} />
            <Bar dataKey="accepted" stackId="volume" fill="var(--color-accepted)" maxBarSize={BAR_SIZE} />
            <Bar dataKey="rejected" stackId="volume" fill="var(--color-rejected)" maxBarSize={BAR_SIZE} radius={[4, 4, 0, 0]} />
          </BarChart>
        </ChartContainer>
        <div className="mt-2 flex flex-wrap gap-4">
          <LegendKey color="var(--viz-1)" label="Aceptados" />
          <LegendKey color="var(--viz-rejected)" label="Rechazados" />
        </div>
      </>}
    </ChartCard>
  )
}

const countConfig = { count: { label: "Eventos", color: "var(--viz-1)" } } satisfies ChartConfig

/** Barras horizontales con etiqueta directa; sirve para estados, tipos y motivos. */
function RankingChart({ title, description, columns, rows, color = "var(--viz-1)", empty }: {
  title: string
  description: string
  columns: [string, string]
  rows: Array<{ label: string; count: number }>
  color?: string
  empty: string
}) {
  return (
    <ChartCard title={title} description={description} table={{ columns, rows: rows.map((row) => [row.label, row.count]) }}>
      {!rows.some((row) => row.count > 0) ? <EmptyChart message={empty} /> : (
        <ChartContainer config={{ count: { ...countConfig.count, color } }} className="aspect-auto w-full" style={{ height: rows.length * 32 + 16 }}>
          <BarChart data={rows} layout="vertical" margin={{ top: 0, right: 48, left: 8, bottom: 0 }}>
            <CartesianGrid horizontal={false} vertical stroke="var(--border)" />
            <XAxis type="number" hide allowDecimals={false} />
            <YAxis type="category" dataKey="label" tickLine={false} axisLine={false} tick={AXIS_TICK} width={170} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent indicator="line" />} />
            <Bar dataKey="count" fill="var(--color-count)" barSize={BAR_SIZE} radius={[0, 4, 4, 0]}>
              <LabelList dataKey="count" position="right" className="fill-foreground text-xs tabular-nums" formatter={(value) => formatCount(Number(value))} />
            </Bar>
          </BarChart>
        </ChartContainer>
      )}
    </ChartCard>
  )
}

const sortedEntries = (map?: Record<string, number>) =>
  Object.entries(map ?? {}).sort(([, a], [, b]) => b - a)

export function TopPublishedTypesChart({ data }: { data: CoreModuleDashboard }) {
  const rows = (data.published?.topTypes ?? []).map((type) => ({ label: type.eventType ?? "—", count: type.total ?? 0 }))
  return (
    <RankingChart
      title="Eventos enviados por tipo"
      description="Tipos que más publicamos, acumulado histórico."
      columns={["Tipo de evento", "Publicados"]}
      rows={rows}
      empty="Todavía no publicamos eventos."
    />
  )
}

export function ReceivedByStatusChart({ data }: { data: CoreModuleDashboard }) {
  const rows = sortedEntries(data.received?.byStatus).map(([status, count]) => ({ label: coreStatusLabel(status), count }))
  return (
    <RankingChart
      title="Eventos recibidos por estado"
      description="Entregas de Core hacia nuestra cola, según su estado."
      columns={["Estado", "Entregas"]}
      rows={rows}
      color="var(--viz-2)"
      empty="Todavía no recibimos eventos."
    />
  )
}

export function DeadLettersChart({ data }: { data: CoreModuleDashboard }) {
  const rows = sortedEntries(data.deadLetters?.byReason).map(([reason, count]) => ({ label: reason, count }))
  return (
    <RankingChart
      title="Dead letters por motivo"
      description={`${formatCount(data.deadLetters?.open)} abiertas que agotaron sus reintentos.`}
      columns={["Motivo", "Abiertas"]}
      rows={rows}
      color="var(--viz-rejected)"
      empty="No hay dead letters abiertas."
    />
  )
}
