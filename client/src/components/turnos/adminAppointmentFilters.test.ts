import { describe, expect, it } from "vitest"

import {
  adminAppointmentSearchSchema,
  adminCalendarSearchSchema,
  daysInMonth,
  leadingBlanks,
  monthDayIso,
  shiftMonth,
} from "@/components/turnos/adminAppointmentFilters"
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

describe("adminCalendarSearchSchema", () => {
  it("aplica valores por defecto del mes actual", () => {
    const today = appointmentToday()
    expect(adminCalendarSearchSchema.parse({})).toEqual({
      centro: "",
      servicio: "",
      anio: Number(today.slice(0, 4)),
      mes: Number(today.slice(5, 7)),
    })
  })

  it("reemplaza mes y año fuera de rango", () => {
    expect(adminCalendarSearchSchema.parse({ anio: 1999, mes: 13 })).toMatchObject({
      anio: Number(appointmentToday().slice(0, 4)),
      mes: Number(appointmentToday().slice(5, 7)),
    })
  })
})

describe("utilidades del calendario", () => {
  it("calcula días, formato ISO y desplazamiento de mes", () => {
    expect(daysInMonth(2026, 10)).toBe(31)
    expect(monthDayIso(2026, 10, 7)).toBe("2026-10-07")
    expect(shiftMonth(2026, 1, -1)).toEqual({ anio: 2025, mes: 12 })
    expect(shiftMonth(2026, 12, 1)).toEqual({ anio: 2027, mes: 1 })
  })

  it("alinea la grilla desde el lunes", () => {
    // Octubre 2026 empieza un jueves: tres celdas vacías.
    expect(leadingBlanks(2026, 10)).toBe(3)
    // Junio 2026 empieza un lunes: sin celdas vacías.
    expect(leadingBlanks(2026, 6)).toBe(0)
  })
})
