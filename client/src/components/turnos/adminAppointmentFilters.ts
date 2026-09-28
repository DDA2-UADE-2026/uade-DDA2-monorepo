import { z } from "zod"

import { appointmentToday } from "@/lib/appointment-flow"

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

export const adminAppointmentSearchSchema = z.object({
  centro: z.string().catch("").default(""),
  fecha: z.string().regex(DATE_PATTERN).catch(appointmentToday()).default(appointmentToday()),
})

export type AdminAppointmentSearch = z.infer<typeof adminAppointmentSearchSchema>

export function isValidAppointmentDate(value: string) {
  return DATE_PATTERN.test(value)
}
