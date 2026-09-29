import { z } from "zod"

import type { CreateAttentionRequest, ProfessionalAppointmentResponse } from "@/generated/types.gen"
import { appointmentToday } from "@/lib/appointment-flow"

export const professionalTurnSearchSchema = z.object({
  fecha: z.iso.date().catch(appointmentToday()).default(appointmentToday()),
})

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
