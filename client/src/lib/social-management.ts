import { client } from "@/generated/client.gen"
import { queryOptions } from "@tanstack/react-query"

export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export type VisitStatus = "SCHEDULED" | "CANCELLED" | "REALIZED" | "NOT_REALIZED"
export type VisitType = "VISIT" | "INTERVIEW"
export type RequestStatus = "PENDING" | "APPROVED" | "REJECTED"
export type Level = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT"

export type Evaluation = {
  id: string; applicationId: string; applicationNumber: number; applicationStatus: string
  applicantUserId: number; applicantName: string; applicantEmail?: string
  programName: string; programEditionName: string; vulnerability: Level; priority: Priority
  professionalId: number; professionalName: string; professionalEmail?: string
  evaluatedAt: string; recommendation?: string
}

export type RescheduleSummary = { id: string; status: RequestStatus; requestedAt: string; reviewedAt: string | null }
export type Visit = {
  id: string; applicationId: string; applicationNumber: number; applicationStatus: string
  applicantUserId: number; applicantName: string; applicantEmail?: string
  programName: string; programEditionName: string; type: VisitType
  startsAt: string; endsAt: string; durationMinutes: number; location: string; status: VisitStatus
  responsibleId: number; responsibleName: string; responsibleEmail?: string
  scheduledAt: string; rescheduled: boolean; lastRescheduledAt: string | null
  latestRescheduleRequest: RescheduleSummary | null
  cancelledAt: string | null; cancellationReason?: string | null; cancelledByName: string | null
  resultRecordedAt: string | null; resultRecordedByName: string | null
  observations?: string | null; conclusions?: string | null
}
export type ManagedVisitResponse = Pick<Visit, "id" | "applicationId" | "applicationStatus" | "type" | "startsAt" | "endsAt" | "durationMinutes" | "location" | "status" | "responsibleId" | "responsibleName" | "scheduledAt" | "rescheduled" | "lastRescheduledAt" | "latestRescheduleRequest" | "cancelledAt" | "resultRecordedAt"> & {
  cancellationReason: string | null; observations: string | null; conclusions: string | null
}

export type VisitSchedule = { startsAt: string; endsAt: string; durationMinutes: number; location: string }
export type RescheduleRequest = {
  id: string; applicationId: string; applicationNumber: number; visitId: string; visitType: VisitType
  applicantUserId: number; applicantName: string; applicantEmail?: string
  responsibleId: number; responsibleName: string; responsibleEmail?: string
  programName: string; programEditionName: string; status: RequestStatus
  requestedAt: string; reviewedAt: string | null
  originalStartsAt: string; originalEndsAt: string; originalDurationMinutes: number
  approvedStartsAt: string | null; approvedEndsAt: string | null; approvedDurationMinutes: number | null
  reason?: string; scheduleAtRequest?: VisitSchedule; approvedSchedule?: VisitSchedule | null; reviewComment?: string | null
}

type Filters = Record<string, string | number | undefined>
const visitBase = "/api/admin/application-visits"
const evaluationBase = "/api/admin/application-evaluations"
const requestBase = "/api/admin/application-visit-reschedule-requests"

function query(filters: Filters): Record<string, string | number> {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== undefined && value !== "")) as Record<string, string | number>
}

async function get<T>(url: string, filters: Filters = {}): Promise<T> {
  const { data } = await client.get({ url, query: query(filters), throwOnError: true })
  return data as T
}

async function write<T>(method: "post" | "put" | "patch", url: string, body: object): Promise<T> {
  const { data } = await client[method]({
    url, body, bodySerializer: JSON.stringify, headers: { "Content-Type": "application/json" }, throwOnError: true,
  })
  return data as T
}

export const socialManagement = {
  evaluations: (filters: Filters) => get<Page<Evaluation>>(evaluationBase, filters),
  evaluation: (id: string) => get<Evaluation>(`${evaluationBase}/${id}`),
  createEvaluation: (applicationId: string, body: { vulnerability: Level; priority: Priority; recommendation: string }) =>
    write<Pick<Evaluation, "id" | "applicationId" | "vulnerability" | "priority" | "recommendation" | "professionalId" | "professionalName" | "evaluatedAt">>("post", `/api/applications/${applicationId}/evaluation`, body),
  visits: (filters: Filters) => get<Page<Visit>>(visitBase, filters),
  visit: (id: string) => get<Visit>(`${visitBase}/${id}`),
  createVisit: (applicationId: string, body: { type: VisitType; startsAt: string; durationMinutes: number; location: string }) =>
    write<ManagedVisitResponse>("post", `/api/applications/${applicationId}/visits`, body),
  rescheduleVisit: (applicationId: string, visitId: string, body: { startsAt: string; durationMinutes: number; location: string }) =>
    write<ManagedVisitResponse>("put", `/api/applications/${applicationId}/visits/${visitId}/schedule`, body),
  cancelVisit: (applicationId: string, visitId: string, reason: string) =>
    write<ManagedVisitResponse>("patch", `/api/applications/${applicationId}/visits/${visitId}/cancel`, { reason }),
  recordResult: (applicationId: string, visitId: string, body: { status: "REALIZED" | "NOT_REALIZED"; observations?: string; conclusions?: string }) =>
    write<ManagedVisitResponse>("post", `/api/applications/${applicationId}/visits/${visitId}/result`, body),
  requests: (filters: Filters) => get<Page<RescheduleRequest>>(requestBase, filters),
  request: (id: string) => get<RescheduleRequest>(`${requestBase}/${id}`),
  reviewRequest: (applicationId: string, visitId: string, requestId: string, body: { status: "APPROVED" | "REJECTED"; startsAt?: string; durationMinutes?: number; location?: string; comment?: string }) =>
    write<RescheduleRequest>("patch", `/api/applications/${applicationId}/visits/${visitId}/reschedule-requests/${requestId}/review`, body),
}

export const socialQueries = {
  evaluations: (filters: Filters) => queryOptions({ queryKey: ["social-evaluations", filters], queryFn: () => socialManagement.evaluations(filters) }),
  evaluation: (id: string) => queryOptions({ queryKey: ["social-evaluation", id], queryFn: () => socialManagement.evaluation(id) }),
  visits: (filters: Filters) => queryOptions({ queryKey: ["social-visits", filters], queryFn: () => socialManagement.visits(filters) }),
  visit: (id: string) => queryOptions({ queryKey: ["social-visit", id], queryFn: () => socialManagement.visit(id) }),
  requests: (filters: Filters) => queryOptions({ queryKey: ["social-reschedule-requests", filters], queryFn: () => socialManagement.requests(filters) }),
  request: (id: string) => queryOptions({ queryKey: ["social-reschedule-request", id], queryFn: () => socialManagement.request(id) }),
}

export const invalidateSocialQueries = (kind: string) => ({ predicate: (query: { queryKey: readonly unknown[] }) => query.queryKey[0] === kind })

export const visitTypeLabel: Record<VisitType, string> = { VISIT: "Visita", INTERVIEW: "Entrevista" }
export const visitStatusLabel: Record<VisitStatus, string> = { SCHEDULED: "Programada", CANCELLED: "Cancelada", REALIZED: "Realizada", NOT_REALIZED: "No realizada" }
export const requestStatusLabel: Record<RequestStatus, string> = { PENDING: "Pendiente", APPROVED: "Aprobada", REJECTED: "Rechazada" }
export const levelLabel: Record<Level, string> = { LOW: "Baja", MEDIUM: "Media", HIGH: "Alta", CRITICAL: "Crítica" }
export const priorityLabel: Record<Priority, string> = { LOW: "Baja", MEDIUM: "Media", HIGH: "Alta", URGENT: "Urgente" }

// Vulnerability and priority share one four-step scale: green, yellow, red, dark violet for the top level.
export type Severity = Level | Priority
export const severityBadgeClass: Record<Severity, string> = {
  LOW: "border-emerald-600/30 bg-emerald-500/15 text-emerald-800 dark:border-emerald-400/30 dark:bg-emerald-400/15 dark:text-emerald-300",
  MEDIUM: "border-yellow-600/40 bg-yellow-400/25 text-yellow-900 dark:border-yellow-300/30 dark:bg-yellow-300/15 dark:text-yellow-200",
  HIGH: "border-red-600/30 bg-red-500/15 text-red-800 dark:border-red-400/30 dark:bg-red-400/15 dark:text-red-300",
  CRITICAL: "border-violet-950 bg-violet-900 text-violet-50 dark:border-violet-500/50 dark:bg-violet-950 dark:text-violet-200",
  URGENT: "border-violet-950 bg-violet-900 text-violet-50 dark:border-violet-500/50 dark:bg-violet-950 dark:text-violet-200",
}
export const severityDotClass: Record<Severity, string> = {
  LOW: "bg-emerald-500 dark:bg-emerald-400",
  MEDIUM: "bg-yellow-500 dark:bg-yellow-300",
  HIGH: "bg-red-600 dark:bg-red-400",
  CRITICAL: "bg-violet-900 ring-1 ring-violet-400/60 dark:bg-violet-500",
  URGENT: "bg-violet-900 ring-1 ring-violet-400/60 dark:bg-violet-500",
}
export const formatSocialDate = (value: string | null | undefined) => value ? new Date(value).toLocaleString("es-AR", { dateStyle: "short", timeStyle: "short" }) : "—"
export const dateTimeLocal = (value: string) => {
  const date = new Date(value)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}
export const apiErrorMessage = (error: unknown) => {
  if (error && typeof error === "object") {
    const value = error as { message?: unknown; detail?: unknown }
    if (typeof value.message === "string") return value.message
    if (typeof value.detail === "string") return value.detail
  }
  return "No se pudo completar la operación. Revisá los datos e intentá nuevamente."
}
