import type { AppointmentResponse, AvailableAppointmentSlotResponse, ErrorResponse } from "@/generated/types.gen"

export const APPOINTMENT_TIME_ZONE = "America/Argentina/Buenos_Aires"

export const appointmentStatusLabels: Record<NonNullable<AppointmentResponse["status"]>, string> = {
  CONFIRMED: "Confirmado",
}

export function appointmentToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: APPOINTMENT_TIME_ZONE, year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(now)
  return ["year", "month", "day"].map((type) => parts.find((part) => part.type === type)?.value).join("-")
}

export function formatAppointmentDate(value?: string) {
  if (!value) return "—"
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value)
  if (Number.isNaN(date.getTime())) return "—"
  return new Intl.DateTimeFormat("es-AR", {
    timeZone: APPOINTMENT_TIME_ZONE, weekday: "long", day: "numeric", month: "long", year: "numeric",
  }).format(date)
}

export function formatAppointmentTime(value?: string) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return new Intl.DateTimeFormat("es-AR", {
    timeZone: APPOINTMENT_TIME_ZONE, hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(date)
}

export function formatAppointmentRange(start?: string, end?: string) {
  if (!start || !end) return "—"
  return `${formatAppointmentTime(start)} – ${formatAppointmentTime(end)}`
}

export function apiErrorCode(error: unknown): string | undefined {
  if (typeof error !== "object" || error === null) return undefined
  const code = (error as ErrorResponse).code
  return typeof code === "string" ? code : undefined
}

export function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)
}

function randomKey() {
  try {
    return crypto.randomUUID()
  } catch {
    return `${Date.now()}-${Math.floor(Math.random() * Number.MAX_SAFE_INTEGER)}`
  }
}

export function appointmentAttemptStorageKey(userId: number, assignmentId: string, startsAt: string, endsAt: string) {
  return `appointment-attempt:${userId}:${assignmentId}:${startsAt}:${endsAt}`
}

export function getAppointmentAttemptKey(userId: number, assignmentId: string, startsAt: string, endsAt: string) {
  const storageKey = appointmentAttemptStorageKey(userId, assignmentId, startsAt, endsAt)
  try {
    const existing = sessionStorage.getItem(storageKey)
    if (existing) return existing
    const key = randomKey()
    sessionStorage.setItem(storageKey, key)
    return key
  } catch {
    return randomKey()
  }
}

export function clearAppointmentAttemptKey(userId: number, assignmentId: string, startsAt: string, endsAt: string) {
  try { sessionStorage.removeItem(appointmentAttemptStorageKey(userId, assignmentId, startsAt, endsAt)) } catch { /* Storage may be disabled. */ }
}

export function slotFingerprint(slot: Pick<AvailableAppointmentSlotResponse, "professionalAssignmentId" | "startsAt" | "endsAt">) {
  return `${slot.professionalAssignmentId ?? ""}|${slot.startsAt ?? ""}|${slot.endsAt ?? ""}`
}
