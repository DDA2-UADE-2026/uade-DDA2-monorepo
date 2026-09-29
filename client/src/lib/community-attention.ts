import { z } from "zod"

import type { ProfessionalAppointmentResponse } from "@/generated/types.gen"
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
