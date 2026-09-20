/* eslint-disable react-refresh/only-export-components */
import { z } from "zod"

import { Badge } from "@/components/ui/badge"
import type { ActivityResponse, CitizenActivityResponse } from "@/generated/types.gen"

export type ActivityStatus = NonNullable<ActivityResponse["status"]>

export const PAGE_SIZE = 10

export const activitySearchSchema = z.object({
  page: z.coerce.number().int().positive().catch(1).default(1),
})

export const activityStatusLabels: Record<ActivityStatus, string> = {
  DRAFT: "Borrador",
  OPEN: "Publicada",
  CLOSED: "Cerrada",
}

/** El ciclo es unidireccional: DRAFT → OPEN → CLOSED, y el cierre es terminal. */
export function canEditActivity(status?: ActivityStatus) {
  return status === "DRAFT"
}

export function canPublishActivity(status?: ActivityStatus) {
  return status === "DRAFT"
}

export function canCloseActivity(status?: ActivityStatus) {
  return status === "OPEN"
}

export function ActivityStatusBadge({ status }: { status?: ActivityStatus }) {
  return (
    <Badge variant={status === "OPEN" ? "default" : status === "CLOSED" ? "secondary" : "outline"}>
      {status ? activityStatusLabels[status] : "Sin estado"}
    </Badge>
  )
}

export function formatActivityDate(value?: string) {
  if (!value) return "—"

  const [year, month, day] = value.split("-").map(Number)
  if (!year || !month || !day) return "—"
  return new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short", year: "numeric" })
    .format(new Date(year, month - 1, day))
}

/** Una actividad de un solo día se muestra sin repetir la fecha. */
export function formatActivityDateRange(startDate?: string, endDate?: string) {
  if (!startDate && !endDate) return "—"
  if (!endDate || startDate === endDate) return formatActivityDate(startDate)
  if (!startDate) return formatActivityDate(endDate)
  return `${formatActivityDate(startDate)} — ${formatActivityDate(endDate)}`
}

export function formatActivityDateTime(value?: string) {
  if (!value) return "—"

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return date.toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" })
}

/** El backend acota `availableCapacity` a cero, pero el tipo generado la deja opcional. */
export function availableSeats(activity: CitizenActivityResponse) {
  return Math.max(0, Number(activity.availableCapacity ?? 0))
}

export function seatsLabel(activity: CitizenActivityResponse) {
  const seats = availableSeats(activity)
  if (seats === 0) return "Sin cupos disponibles"
  return `${seats} ${seats === 1 ? "cupo disponible" : "cupos disponibles"}`
}
