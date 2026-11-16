/* eslint-disable react-refresh/only-export-components */
import { IconAlertCircle, IconClipboardHeart } from "@tabler/icons-react"
import type { ReactNode } from "react"

import { ApplicationPage } from "@/components/applications/ApplicationUi"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Progress, ProgressLabel } from "@/components/ui/progress"
import { Skeleton } from "@/components/ui/skeleton"
import type { ErrorResponse } from "@/generated/types.gen"

export type InterventionStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED"

export const interventionStatusLabel: Record<InterventionStatus, string> = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En curso",
  COMPLETED: "Completada",
}

export function InterventionStatusBadge({ status }: { status?: InterventionStatus }) {
  const variant = status === "COMPLETED" ? "default" : status === "IN_PROGRESS" ? "secondary" : "outline"
  return <Badge variant={variant}>{status ? interventionStatusLabel[status] : "Sin estado"}</Badge>
}

export function InterventionPage({ breadcrumbs, children }: Parameters<typeof ApplicationPage>[0]) {
  return <ApplicationPage breadcrumbs={breadcrumbs}>{children}</ApplicationPage>
}

export function InterventionHeading({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
    <div>
      <h1 className="flex items-center gap-2 font-heading text-2xl font-medium"><IconClipboardHeart className="size-6 text-primary" />{title}</h1>
      {description && <p className="mt-2 max-w-3xl text-sm text-muted-foreground">{description}</p>}
    </div>
    {action}
  </div>
}

export function InterventionError({ error, title, retry }: { error: ErrorResponse; title: string; retry?: () => void }) {
  return <Alert variant="destructive">
    <IconAlertCircle />
    <AlertTitle>{title}</AlertTitle>
    <AlertDescription className="flex flex-col items-start gap-3">
      <span>{error.message ?? "Intentá nuevamente en unos instantes."}</span>
      {error.fields?.length ? <ul className="list-disc pl-4">{error.fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}</ul> : null}
      {retry && <Button size="sm" variant="outline" onClick={retry}>Reintentar</Button>}
    </AlertDescription>
  </Alert>
}

export function InterventionLoading({ label = "Cargando intervención" }: { label?: string }) {
  return <div role="status" aria-label={label} className="space-y-5">
    <Skeleton className="h-8 w-64" />
    <Skeleton className="h-32 w-full" />
    <Skeleton className="h-64 w-full" />
    <span className="sr-only">{label}…</span>
  </div>
}

export function InterventionProgress({ statuses }: { statuses: Array<InterventionStatus | undefined> }) {
  const completed = statuses.filter((status) => status === "COMPLETED").length
  return <InterventionProgressSummary completed={completed} total={statuses.length} />
}

export function InterventionProgressSummary({ completed, total, compact = false }: { completed: number; total: number; compact?: boolean }) {
  const safeTotal = Math.max(0, total)
  const safeCompleted = Math.min(Math.max(0, completed), safeTotal)
  const value = safeTotal ? Math.round((safeCompleted / safeTotal) * 100) : 0
  return <Progress value={value} className={(compact ? "min-w-40 max-w-52 gap-1.5" : "max-w-md")}>
    <ProgressLabel className={compact ? "text-xs" : undefined}>{compact ? `${value}%` : "Progreso"}</ProgressLabel>
    <span className={"ml-auto text-xs text-muted-foreground tabular-nums" + (compact ? " " : " sm:text-sm")}>{safeCompleted} de {safeTotal} acciones</span>
  </Progress>
}

export function formatInterventionDate(value?: string | null) {
  if (!value) return "—"
  const [year, month, day] = value.slice(0, 10).split("-").map(Number)
  if (!year || !month || !day) return value
  return new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" }).format(new Date(year, month - 1, day))
}

export function formatInterventionDateTime(value?: string | null) {
  return value ? new Intl.DateTimeFormat("es-AR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—"
}

export function todayLocal() {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 10)
}
