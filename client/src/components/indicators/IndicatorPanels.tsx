import { IconAlertTriangle, IconCircleX } from "@tabler/icons-react"
import { Link } from "@tanstack/react-router"
import { useState } from "react"

import { ChartCard, EmptyChart } from "@/components/indicators/IndicatorUi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { applicationStatusLabels } from "@/lib/application-flow"
import { formatCount, formatDate, formatRate, type Indicators } from "@/lib/indicators"
import { levelLabel, priorityLabel, type Level, type Priority } from "@/lib/social-management"

// ---- Cupos ----

const editionStatusLabels: Record<string, string> = { ACTIVE: "Activa", SUSPENDED: "Suspendida", CLOSED: "Cerrada" }
const METER_LIMIT = 8

/** Severidad del cupo: el color siempre va con ícono y texto, nunca solo. */
function capacityState(rate: number) {
  if (rate >= 1) return { color: "var(--viz-critical)", label: "Cupo completo", Icon: IconCircleX }
  if (rate >= 0.8) return { color: "var(--viz-warning)", label: "Casi completo", Icon: IconAlertTriangle }
  return { color: "var(--viz-1)", label: null, Icon: null }
}

export function CapacityMeters({ data }: { data: Indicators }) {
  const [expanded, setExpanded] = useState(false)
  const editions = data.capacity ?? []
  const visible = expanded ? editions : editions.slice(0, METER_LIMIT)
  return (
    <ChartCard
      title="Cupos por edición"
      description="Aprobadas sobre el cupo de cada edición, hoy. No depende del período."
      table={{
        columns: ["Programa", "Edición", "Estado", "Aprobadas", "Cupo", "Ocupación", "Lista de espera"],
        rows: editions.map((edition) => [
          edition.programName ?? "",
          edition.editionName ?? "",
          editionStatusLabels[edition.status ?? ""] ?? edition.status ?? "",
          edition.approved ?? 0,
          edition.maxCapacity ?? 0,
          formatRate(edition.maxCapacity ? (edition.approved ?? 0) / edition.maxCapacity : null),
          edition.waitlisted ?? 0,
        ]),
      }}
      footer={editions.length > METER_LIMIT && (
        <Button size="sm" variant="ghost" className="mt-2" onClick={() => setExpanded((value) => !value)}>
          {expanded ? "Ver menos" : `Ver las ${editions.length} ediciones`}
        </Button>
      )}
    >
      {editions.length === 0 ? <EmptyChart message="No hay ediciones publicadas." /> : (
        <ul className="space-y-4">
          {visible.map((edition) => {
            const capacity = edition.maxCapacity ?? 0
            const approved = edition.approved ?? 0
            const rate = capacity > 0 ? approved / capacity : 0
            const state = capacityState(rate)
            return (
              <li key={edition.editionId} className="space-y-1.5">
                <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 text-sm">
                  <span className="min-w-0">
                    <span className="font-medium">{edition.programName}</span>
                    <span className="text-muted-foreground"> · {edition.editionName}</span>
                    {edition.status !== "ACTIVE" && <Badge variant="outline" className="ml-2">{editionStatusLabels[edition.status ?? ""] ?? edition.status}</Badge>}
                  </span>
                  <span className="tabular-nums text-muted-foreground">
                    <span className="font-medium text-foreground">{formatCount(approved)}</span> de {formatCount(capacity)} · {formatRate(capacity ? rate : null)}
                  </span>
                </div>
                <div
                  role="meter"
                  aria-label={`Ocupación de ${edition.programName} ${edition.editionName}`}
                  aria-valuemin={0}
                  aria-valuemax={capacity}
                  aria-valuenow={approved}
                  aria-valuetext={`${approved} de ${capacity} aprobadas`}
                  className="h-2 overflow-hidden rounded-full"
                  style={{ backgroundColor: "var(--viz-track)" }}
                >
                  <div className="h-full rounded-full" style={{ width: `${Math.min(rate, 1) * 100}%`, backgroundColor: state.color }} />
                </div>
                {(state.label || (edition.waitlisted ?? 0) > 0) && (
                  <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                    {state.Icon && <span className="inline-flex items-center gap-1 font-medium text-foreground"><state.Icon aria-hidden="true" className="size-3.5" style={{ color: state.color }} />{state.label}</span>}
                    {(edition.waitlisted ?? 0) > 0 && <span>{formatCount(edition.waitlisted)} en lista de espera</span>}
                  </p>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </ChartCard>
  )
}

// ---- Mapa de evaluación ----

const VULNERABILITY_ROWS: Level[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
const PRIORITY_COLUMNS: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"]
const SEQ_STEPS = 6

export function EvaluationHeatmap({ data }: { data: Indicators }) {
  const cells = data.evaluationMatrix ?? []
  const countOf = (vulnerability: Level, priority: Priority) =>
    cells.find((cell) => cell.vulnerability === vulnerability && cell.priority === priority)?.count ?? 0
  const max = Math.max(0, ...cells.map((cell) => cell.count ?? 0))
  const total = cells.reduce((sum, cell) => sum + (cell.count ?? 0), 0)
  return (
    <ChartCard
      title="Vulnerabilidad y prioridad"
      description="Evaluaciones sociales registradas en el período."
      table={{
        columns: ["Vulnerabilidad", ...PRIORITY_COLUMNS.map((priority) => `Prioridad ${priorityLabel[priority].toLowerCase()}`)],
        rows: VULNERABILITY_ROWS.map((vulnerability) => [levelLabel[vulnerability], ...PRIORITY_COLUMNS.map((priority) => countOf(vulnerability, priority))]),
      }}
    >
      {total === 0 ? <EmptyChart message="No se registraron evaluaciones en el período." /> : <>
        <div className="grid grid-cols-[auto_repeat(4,minmax(0,1fr))] gap-0.5 text-xs" role="table" aria-label="Evaluaciones por vulnerabilidad y prioridad">
          <div role="row" className="contents">
            <span role="columnheader" className="p-1 text-muted-foreground">Vuln. \ Prioridad</span>
            {PRIORITY_COLUMNS.map((priority) => <span key={priority} role="columnheader" className="p-1 text-center text-muted-foreground">{priorityLabel[priority]}</span>)}
          </div>
          {VULNERABILITY_ROWS.map((vulnerability) => (
            <div key={vulnerability} role="row" className="contents">
              <span role="rowheader" className="flex items-center p-1 pr-2 text-muted-foreground">{levelLabel[vulnerability]}</span>
              {PRIORITY_COLUMNS.map((priority) => {
                const count = countOf(vulnerability, priority)
                // Escala secuencial de un solo tono: más casos, paso más intenso. El cero queda en gris.
                const step = count === 0 ? 0 : Math.max(1, Math.ceil((count / max) * SEQ_STEPS))
                return (
                  <span
                    key={priority}
                    role="cell"
                    tabIndex={0}
                    title={`${levelLabel[vulnerability]} · prioridad ${priorityLabel[priority].toLowerCase()}: ${count}`}
                    aria-label={`Vulnerabilidad ${levelLabel[vulnerability].toLowerCase()}, prioridad ${priorityLabel[priority].toLowerCase()}: ${count} evaluaciones`}
                    className="flex h-12 items-center justify-center rounded-md font-medium tabular-nums outline-offset-2 transition-[filter] hover:brightness-110 focus-visible:outline-2 focus-visible:outline-ring"
                    style={step === 0
                      ? { backgroundColor: "var(--muted)", color: "var(--muted-foreground)" }
                      : { backgroundColor: `var(--viz-seq-${step})`, color: step <= 3 ? "var(--viz-seq-ink-light)" : "var(--viz-seq-ink-dark)" }}
                  >
                    {count}
                  </span>
                )
              })}
            </div>
          ))}
        </div>
        <div className="mt-3 flex items-center gap-2 text-xs text-muted-foreground" aria-hidden="true">
          <span>Menos</span>
          {Array.from({ length: SEQ_STEPS }, (_, index) => <span key={index} className="h-2 w-5 rounded-sm" style={{ backgroundColor: `var(--viz-seq-${index + 1})` }} />)}
          <span>Más</span>
        </div>
      </>}
    </ChartCard>
  )
}

// ---- Atención ----

export function AttentionPanel({ data }: { data: Indicators }) {
  const attention = data.attention ?? {}
  const oldest = attention.oldestAwaitingEvaluation ?? []
  const days = attention.staleAfterDays ?? 14
  return (
    <ChartCard
      title="Requieren atención"
      description="Estado actual, sin importar el período elegido."
      table={{
        columns: ["Solicitud", "Programa", "Estado", "Presentada", "Días esperando"],
        rows: oldest.map((item) => [
          `N.º ${item.applicationNumber}`,
          item.programName ?? "",
          item.status ? applicationStatusLabels[item.status] : "",
          item.submittedAt ? formatDate(item.submittedAt.slice(0, 10)) : "",
          item.daysWaiting ?? 0,
        ]),
      }}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <AttentionTile value={attention.unassigned ?? 0} label="Solicitudes activas sin trabajador asignado" />
        <AttentionTile value={attention.awaitingEvaluation ?? 0} label={`Sin evaluar hace más de ${days} días`} />
      </div>
      {oldest.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-xs font-medium text-muted-foreground">Las más antiguas sin evaluar</p>
          <ul className="divide-y rounded-lg border">
            {oldest.map((item) => (
              <li key={item.applicationId} className="flex flex-wrap items-center justify-between gap-2 p-2.5 text-sm">
                <Link to="/gestion/casos/$solicitudId/evaluacion" params={{ solicitudId: item.applicationId! }} className="min-w-0 hover:underline">
                  <span className="font-medium">N.º {item.applicationNumber}</span>
                  <span className="text-muted-foreground"> · {item.programName}</span>
                </Link>
                <span className="text-xs tabular-nums text-muted-foreground">{item.daysWaiting} días · {item.status ? applicationStatusLabels[item.status] : ""}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </ChartCard>
  )
}

function AttentionTile({ value, label }: { value: number; label: string }) {
  return (
    <div className="rounded-xl border p-3">
      <p className="flex items-center gap-2 font-heading text-2xl font-semibold">
        {value > 0 && <IconAlertTriangle aria-hidden="true" className="size-5" style={{ color: "var(--viz-warning)" }} />}
        {formatCount(value)}
      </p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  )
}
