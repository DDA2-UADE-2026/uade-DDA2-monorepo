import { queryOptions } from "@tanstack/react-query"

import { client } from "@/generated/client.gen"

export type CitizenVisitStatus = "SCHEDULED" | "CANCELLED" | "REALIZED" | "NOT_REALIZED"
export type CitizenVisitType = "VISIT" | "INTERVIEW"
export type CitizenRequestStatus = "PENDING" | "APPROVED" | "REJECTED"

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type CitizenVisit = {
  id: string
  applicationId: string
  applicationNumber: number
  type: CitizenVisitType
  startsAt: string
  endsAt: string
  durationMinutes: number
  location: string
  status: CitizenVisitStatus
  rescheduled: boolean
  lastRescheduledAt: string | null
  latestRescheduleRequest: { id: string; status: CitizenRequestStatus; requestedAt: string; reviewedAt: string | null } | null
  cancelledAt: string | null
  cancellationReason?: string | null
}

export type CitizenRescheduleRequest = {
  id: string
  applicationId: string
  applicationNumber: number
  visitId: string
  visitType: CitizenVisitType
  status: CitizenRequestStatus
  requestedAt: string
  reviewedAt: string | null
  approvedStartsAt: string | null
  approvedEndsAt: string | null
  approvedDurationMinutes: number | null
}

export type CitizenRescheduleRequestDetail = Omit<CitizenRescheduleRequest, "approvedStartsAt" | "approvedEndsAt" | "approvedDurationMinutes"> & {
  reason: string
  scheduleAtRequest: { startsAt: string; endsAt: string; durationMinutes: number; location: string }
  reviewComment: string | null
  approvedSchedule: { startsAt: string; endsAt: string; durationMinutes: number; location: string } | null
}

const visitPath = (applicationId: string) => `/api/applications/${encodeURIComponent(applicationId)}/visits`
const requestPath = (applicationId: string, visitId: string) =>
  `${visitPath(applicationId)}/${encodeURIComponent(visitId)}/reschedule-requests`

async function get<T>(url: string, page?: number, size?: number): Promise<T> {
  const { data } = await client.get({ url, query: page === undefined ? undefined : { page, size }, throwOnError: true })
  return data as T
}

export const citizenVisits = {
  list: (applicationId: string, page: number, size: number) =>
    get<Page<CitizenVisit>>(visitPath(applicationId), page, size),
  detail: (applicationId: string, visitId: string) =>
    get<CitizenVisit>(`${visitPath(applicationId)}/${encodeURIComponent(visitId)}`),
  requests: (applicationId: string, visitId: string, page: number, size: number) =>
    get<Page<CitizenRescheduleRequest>>(requestPath(applicationId, visitId), page, size),
  request: (applicationId: string, visitId: string, requestId: string) =>
    get<CitizenRescheduleRequestDetail>(`${requestPath(applicationId, visitId)}/${encodeURIComponent(requestId)}`),
  createRequest: async (applicationId: string, visitId: string, reason: string) => {
    const { data } = await client.post({
      url: requestPath(applicationId, visitId),
      body: { reason },
      bodySerializer: JSON.stringify,
      headers: { "Content-Type": "application/json" },
      throwOnError: true,
    })
    return data as CitizenRescheduleRequestDetail
  },
}

export const citizenVisitQueries = {
  list: (applicationId: string, page: number, size: number) => queryOptions({
    queryKey: ["citizen-visits", applicationId, page, size],
    queryFn: () => citizenVisits.list(applicationId, page, size),
  }),
  detail: (applicationId: string, visitId: string) => queryOptions({
    queryKey: ["citizen-visit", applicationId, visitId],
    queryFn: () => citizenVisits.detail(applicationId, visitId),
  }),
  requests: (applicationId: string, visitId: string, page: number, size: number) => queryOptions({
    queryKey: ["citizen-visit-requests", applicationId, visitId, page, size],
    queryFn: () => citizenVisits.requests(applicationId, visitId, page, size),
  }),
  request: (applicationId: string, visitId: string, requestId: string) => queryOptions({
    queryKey: ["citizen-visit-request", applicationId, visitId, requestId],
    queryFn: () => citizenVisits.request(applicationId, visitId, requestId),
  }),
}

export const citizenVisitTypeLabels: Record<CitizenVisitType, string> = { VISIT: "Visita", INTERVIEW: "Entrevista" }
export const citizenVisitStatusLabels: Record<CitizenVisitStatus, string> = {
  SCHEDULED: "Programada", CANCELLED: "Cancelada", REALIZED: "Realizada", NOT_REALIZED: "No realizada",
}
export const citizenRequestStatusLabels: Record<CitizenRequestStatus, string> = {
  PENDING: "Pendiente", APPROVED: "Aprobada", REJECTED: "Rechazada",
}

export function citizenVisitError(error: unknown): string {
  if (error && typeof error === "object" && "message" in error && typeof error.message === "string") return error.message
  return "No pudimos completar la operación. Intentá nuevamente."
}
