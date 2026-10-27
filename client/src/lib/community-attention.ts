import { z } from "zod"

import type { CreateAttentionRequest, ProfessionalAppointmentResponse } from "@/generated/types.gen"
import { appointmentToday } from "@/lib/appointment-flow"

export const TURN_VIEWS = ["dia", "semana", "mes"] as const
export type TurnView = (typeof TURN_VIEWS)[number]

export const professionalTurnSearchSchema = z.object({
  fecha: z.iso.date().catch(appointmentToday()).default(appointmentToday()),
  vista: z.enum(TURN_VIEWS).catch("dia").default("dia"),
})

// Calendar math runs on plain ISO dates in UTC so the device time zone never shifts a day.
function parseIsoDate(iso: string) {
  const [year, month, day] = iso.split("-").map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function toIsoDate(date: Date) {
  return date.toISOString().slice(0, 10)
}

export function addIsoDays(iso: string, days: number) {
  const date = parseIsoDate(iso)
  date.setUTCDate(date.getUTCDate() + days)
  return toIsoDate(date)
}

function mondayOf(iso: string) {
  return addIsoDays(iso, -((parseIsoDate(iso).getUTCDay() + 6) % 7))
}

function lastDayOfMonth(iso: string) {
  const date = parseIsoDate(iso)
  return toIsoDate(new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)))
}

/** Days covered by a view: Monday–Sunday weeks; the month view spans whole weeks (at most 42 days). */
export function turnViewRange(fecha: string, vista: TurnView): { from: string; to: string; days: string[] } {
  let from = fecha
  let to = fecha
  if (vista === "semana") {
    from = mondayOf(fecha)
    to = addIsoDays(from, 6)
  } else if (vista === "mes") {
    from = mondayOf(`${fecha.slice(0, 8)}01`)
    to = addIsoDays(mondayOf(lastDayOfMonth(fecha)), 6)
  }
  const days: string[] = []
  for (let day = from; day <= to; day = addIsoDays(day, 1)) days.push(day)
  return { from, to, days }
}

/** Moves one day, week or month; month steps keep the day of month, clamped to the target month's length. */
export function shiftTurnDate(fecha: string, vista: TurnView, delta: -1 | 1) {
  if (vista === "dia") return addIsoDays(fecha, delta)
  if (vista === "semana") return addIsoDays(fecha, 7 * delta)
  const date = parseIsoDate(fecha)
  const firstOfTarget = toIsoDate(new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + delta, 1)))
  const day = Math.min(date.getUTCDate(), Number(lastDayOfMonth(firstOfTarget).slice(8)))
  return `${firstOfTarget.slice(0, 8)}${String(day).padStart(2, "0")}`
}

export function turnViewTitle(fecha: string, vista: TurnView) {
  const format = (iso: string, options: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat("es-AR", { timeZone: "UTC", ...options }).format(parseIsoDate(iso))
  if (vista === "dia") return format(fecha, { weekday: "long", day: "numeric", month: "long", year: "numeric" })
  if (vista === "mes") return format(fecha, { month: "long", year: "numeric" })
  const { from, to } = turnViewRange(fecha, "semana")
  const sameMonth = from.slice(0, 7) === to.slice(0, 7)
  return `${format(from, sameMonth ? { day: "numeric" } : { day: "numeric", month: "short" })} – ${format(to, { day: "numeric", month: "short", year: "numeric" })}`
}

/** Local (Buenos Aires) calendar day of an appointment, to bucket it into a grid cell. */
export function appointmentDayKey(startsAt?: string) {
  return startsAt ? appointmentToday(new Date(startsAt)) : undefined
}

export function canRegisterAttention(row: ProfessionalAppointmentResponse, now = new Date()): boolean {
  const appointment = row.appointment
  return appointment?.status === "CONFIRMED" && !!appointment.startsAt &&
    new Date(appointment.startsAt).getTime() <= now.getTime()
}

export function attentionLabel(row: ProfessionalAppointmentResponse, now = new Date()): string {
  if (row.appointment?.status === "CANCELLED") return "Cancelado"
  if (row.attention?.result === "ATENDIDO") return "Atendido"
  if (row.attention?.result === "AUSENTE") return "Ausente"
  return canRegisterAttention(row, now) ? "Sin registrar" : "Aún no comenzó"
}

export function attentionRequest(
  result: CreateAttentionRequest["result"], attendedOn: string, description: string, today = appointmentToday(),
): { body: CreateAttentionRequest; error?: never } | { body?: never; error: string } {
  if (result === "AUSENTE") return { body: { result } }
  if (!z.iso.date().safeParse(attendedOn).success || attendedOn > today) {
    return { error: "Indicá una fecha de atención válida que no sea futura." }
  }
  const cleaned = description.trim()
  if (cleaned.length < 1 || cleaned.length > 500) {
    return { error: "Describí el servicio prestado en 1 a 500 caracteres." }
  }
  return { body: { result, attendedOn, description: cleaned } }
}
