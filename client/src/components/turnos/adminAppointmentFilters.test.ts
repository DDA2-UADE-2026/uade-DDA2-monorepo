import { describe, expect, it } from "vitest"

import { adminAppointmentSearchSchema } from "@/components/turnos/adminAppointmentFilters"
import { appointmentToday } from "@/lib/appointment-flow"

describe("adminAppointmentSearchSchema", () => {
  it("aplica centro vacío y fecha de hoy por defecto", () => {
    expect(adminAppointmentSearchSchema.parse({})).toEqual({ centro: "", fecha: appointmentToday() })
  })

  it("conserva centro y fecha válidos", () => {
    expect(adminAppointmentSearchSchema.parse({ centro: "centro-1", fecha: "2026-10-05" })).toEqual({
      centro: "centro-1",
      fecha: "2026-10-05",
    })
  })

  it("reemplaza una fecha inválida por la de hoy", () => {
    expect(adminAppointmentSearchSchema.parse({ centro: "centro-1", fecha: "ayer" })).toEqual({
      centro: "centro-1",
      fecha: appointmentToday(),
    })
  })
})
