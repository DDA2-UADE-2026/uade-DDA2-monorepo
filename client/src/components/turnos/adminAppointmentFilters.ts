import { z } from "zod"

import { APPOINTMENT_TIME_ZONE, appointmentToday } from "@/lib/appointment-flow"

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function currentYear() {
  return Number(appointmentToday().slice(0, 4))
}

function currentMonth() {
  return Number(appointmentToday().slice(5, 7))
}

export const adminAppointmentSearchSchema = z.object({
  centro: z.string().catch("").default(""),
  fecha: z.string().regex(DATE_PATTERN).catch(appointmentToday()).default(appointmentToday()),
})

export type AdminAppointmentSearch = z.infer<typeof adminAppointmentSearchSchema>

export const adminCalendarSearchSchema = z.object({
  centro: z.string().catch("").default(""),
  servicio: z.string().catch("").default(""),
  anio: z.coerce.number().int().min(2000).max(2100).catch(currentYear()).default(currentYear()),
  mes: z.coerce.number().int().min(1).max(12).catch(currentMonth()).default(currentMonth()),
})

export type AdminCalendarSearch = z.infer<typeof adminCalendarSearchSchema>

export const adminDaySearchSchema = z.object({
  centro: z.string().catch("").default(""),
  servicio: z.string().catch("").default(""),
  fecha: z.string().regex(DATE_PATTERN).catch(appointmentToday()).default(appointmentToday()),
})

export type AdminDaySearch = z.infer<typeof adminDaySearchSchema>

export function isValidAppointmentDate(value: string) {
  return DATE_PATTERN.test(value)
}

export function monthTitle(anio: number, mes: number) {
  return new Intl.DateTimeFormat("es-AR", {
    timeZone: APPOINTMENT_TIME_ZONE,
    month: "long",
    year: "numeric",
  }).format(new Date(Date.UTC(anio, mes - 1, 1)))
}

export function monthDayIso(anio: number, mes: number, dia: number) {
  return `${anio}-${String(mes).padStart(2, "0")}-${String(dia).padStart(2, "0")}`
}

export function daysInMonth(anio: number, mes: number) {
  return new Date(Date.UTC(anio, mes, 0)).getUTCDate()
}

export function leadingBlanks(anio: number, mes: number) {
  return (new Date(Date.UTC(anio, mes - 1, 1)).getUTCDay() + 6) % 7
}

export function shiftMonth(anio: number, mes: number, delta: -1 | 1) {
  const shifted = new Date(Date.UTC(anio, mes - 1 + delta, 1))
  return { anio: shifted.getUTCFullYear(), mes: shifted.getUTCMonth() + 1 }
}
