import type { ApplicationDocumentResponse, ApplicationResponse, AvailableEnrollmentPeriodResponse, AvailableProgramDocumentRequirementResponse, AvailableProgramEditionResponse } from "@/generated/types.gen"

export const applicationStatusLabels: Record<NonNullable<ApplicationResponse["status"]>, string> = {
  DRAFT: "Borrador", SUBMITTED: "Presentada", IN_VALIDATION: "En validación",
  PENDING_DOCUMENTATION: "Documentación pendiente", IN_EVALUATION: "En evaluación",
  IN_VISIT: "En visita", APPROVED: "Aprobada", REJECTED: "Rechazada",
  WAITLISTED: "En lista de espera", CLOSED: "Cerrada",
}

export function isApplicationResolved(status: ApplicationResponse["status"]) {
  return status === "APPROVED" || status === "REJECTED" || status === "CLOSED"
}

export function enrollmentToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: "America/Argentina/Buenos_Aires", year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(now)
  return ["year", "month", "day"].map((type) => parts.find((part) => part.type === type)?.value).join("-")
}

export function canApplyToPeriod(edition: AvailableProgramEditionResponse, period: AvailableEnrollmentPeriodResponse, today = enrollmentToday()) {
  return applicationPeriodUnavailableReason(edition, period, today) === undefined
}

export function applicationPeriodUnavailableReason(edition: AvailableProgramEditionResponse, period: AvailableEnrollmentPeriodResponse, today = enrollmentToday()): string | undefined {
  if (edition.status !== "ACTIVE") {
    if (edition.status === "SUSPENDED") return "La edición está suspendida y no admite nuevas solicitudes."
    if (edition.status === "CLOSED") return "La edición está cerrada y no admite nuevas solicitudes."
    return "La edición todavía no está activa."
  }
  if (period.status !== "OPEN") {
    if (period.status === "SCHEDULED") return "El período está programado, pero todavía no se habilitó la inscripción."
    if (period.status === "SUSPENDED") return "La inscripción está suspendida."
    if (period.status === "CLOSED") return "La inscripción está cerrada."
    return "No se informó el estado del período de inscripción."
  }
  if (!period.id || !period.openDate || !period.closeDate) return "Faltan datos del período para habilitar la inscripción."
  if (today < period.openDate) return "La fecha de inicio de inscripción todavía no llegó."
  if (today > period.closeDate) return "La fecha de cierre de inscripción ya pasó."
}

export const enrollmentStatusLabels = { SCHEDULED: "Próximamente", OPEN: "Abierta", SUSPENDED: "Suspendida", CLOSED: "Cerrada" } as const

export function formatApplicationDate(value?: string) {
  if (!value) return "—"
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value)
  if (Number.isNaN(date.getTime())) return "—"
  return date.toLocaleDateString("es-AR", { day: "numeric", month: "short", year: "numeric" })
}

export function formatDocumentSize(bytes?: number) {
  if (bytes == null) return ""
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.ceil(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export const DOCUMENT_ACCEPT = ".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024

export function validateApplicationFile(file: Pick<File, "name" | "size" | "type">): string | undefined {
  if (file.size === 0) return "El archivo está vacío. Seleccioná otro."
  if (file.size > MAX_DOCUMENT_BYTES) return "El archivo supera los 10 MB permitidos."
  const extension = file.name.split(".").pop()?.toLowerCase()
  const expected = extension === "pdf" ? "application/pdf" : extension === "png" ? "image/png"
    : extension === "jpg" || extension === "jpeg" ? "image/jpeg" : undefined
  if (!expected || file.type.toLowerCase() !== expected) return "Seleccioná un archivo PDF, JPG o PNG con el formato correcto."
}

export function submissionStorageKey(userId: number, periodId: string) {
  return `application-submission:${userId}:${periodId}`
}

export function getSubmissionKey(userId: number, periodId: string) {
  const storageKey = submissionStorageKey(userId, periodId)
  try {
    const existing = sessionStorage.getItem(storageKey)
    if (existing) return existing
    const key = crypto.randomUUID()
    sessionStorage.setItem(storageKey, key)
    return key
  } catch {
    return crypto.randomUUID()
  }
}

export function clearSubmissionKey(userId: number, periodId: string) {
  try { sessionStorage.removeItem(submissionStorageKey(userId, periodId)) } catch { /* Storage may be disabled. */ }
}

export function applicationDocumentRequirements(application: ApplicationResponse, documents: ApplicationDocumentResponse[]) {
  const requirements = new Map<string, AvailableProgramDocumentRequirementResponse & { id: string }>()
  for (const requirement of application.documentRequirements ?? []) {
    if (requirement.id) requirements.set(requirement.id, { ...requirement, id: requirement.id })
  }
  for (const pending of application.pendingDocuments ?? []) {
    if (pending.requirementId && !requirements.has(pending.requirementId)) requirements.set(pending.requirementId, {
      id: pending.requirementId, name: pending.name, code: pending.code, required: true,
    })
  }
  for (const document of documents) {
    if (document.requirementId && !requirements.has(document.requirementId)) requirements.set(document.requirementId, {
      id: document.requirementId, name: document.requirementName, code: document.requirementCode, required: document.required,
    })
  }
  return [...requirements.values()]
}
