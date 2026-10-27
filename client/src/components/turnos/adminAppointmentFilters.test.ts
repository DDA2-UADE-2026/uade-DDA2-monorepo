import { describe, expect, it } from "vitest"

import {
  adminAppointmentSearchSchema,
  adminCalendarSearchSchema,
  daysInMonth,
  leadingBlanks,
  trailingBlanks,
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

  it("completa la última semana de la grilla", () => {
    // Octubre 2026 termina un sábado: queda el domingo vacío.
    expect(trailingBlanks(2026, 10)).toBe(1)
    // Mayo 2026 termina un domingo: sin celdas vacías al final.
    expect(trailingBlanks(2026, 5)).toBe(0)
    // Febrero 2027 empieza lunes y tiene 28 días: cuatro semanas exactas.
    expect(leadingBlanks(2027, 2) + daysInMonth(2027, 2) + trailingBlanks(2027, 2)).toBe(28)
  })
})
