import { describe, expect, it } from "vitest"

import type { ProfessionalAppointmentResponse } from "@/generated/types.gen"
import {
  appointmentDayKey,
  attentionLabel,
  attentionRequest,
  canRegisterAttention,
  professionalTurnSearchSchema,
  shiftTurnDate,
  turnViewRange,
} from "@/lib/community-attention"

describe("turnos profesionales", () => {
  const now = new Date("2026-09-29T12:00:00Z")
  const row = (startsAt: string, status: "CONFIRMED" | "CANCELLED" = "CONFIRMED"): ProfessionalAppointmentResponse => ({
    appointment: { startsAt, status },
  })

  it("habilita un turno confirmado desde el instante de inicio pero no uno cancelado", () => {
    expect(canRegisterAttention(row("2026-09-29T12:00:01Z"), now)).toBe(false)
    expect(canRegisterAttention(row("2026-09-29T12:00:00Z"), now)).toBe(true)
    expect(canRegisterAttention(row("2026-09-29T11:00:00Z", "CANCELLED"), now)).toBe(false)
  })

  it("distingue estado administrativo y resultado registrado", () => {
    expect(attentionLabel(row("2026-09-29T13:00:00Z"), now)).toBe("Aún no comenzó")
    expect(attentionLabel(row("2026-09-29T11:00:00Z"), now)).toBe("Sin registrar")
    expect(attentionLabel({ ...row("2026-09-29T11:00:00Z"), attention: { result: "AUSENTE" } }, now)).toBe("Ausente")
    expect(attentionLabel({ ...row("2026-09-29T11:00:00Z", "CANCELLED"), attention: { result: "ATENDIDO" } }, now)).toBe("Cancelado")
  })

  it("acepta fechas válidas de consulta y descarta valores inválidos", () => {
    expect(professionalTurnSearchSchema.parse({ fecha: "2026-09-28" }).fecha).toBe("2026-09-28")
    expect(professionalTurnSearchSchema.parse({ fecha: "2026-09-31" }).fecha).not.toBe("2026-09-31")
  })

  it("exige datos administrativos para atendido y omite los datos previos al cambiar a ausente", () => {
    expect(attentionRequest("ATENDIDO", "2026-09-30", "Servicio", "2026-09-29").error).toBeTruthy()
    expect(attentionRequest("ATENDIDO", "2026-09-29", "  ", "2026-09-29").error).toBeTruthy()
    expect(attentionRequest("ATENDIDO", "2026-09-29", "  Orientación  ", "2026-09-29").body).toEqual({
      result: "ATENDIDO", attendedOn: "2026-09-29", description: "Orientación",
    })
    expect(attentionRequest("AUSENTE", "2026-09-29", "Dato anterior").body).toEqual({ result: "AUSENTE" })
  })
})

describe("vistas de agenda profesional", () => {
  it("toma la vista diaria por defecto y descarta valores desconocidos", () => {
    expect(professionalTurnSearchSchema.parse({ fecha: "2026-10-25" }).vista).toBe("dia")
    expect(professionalTurnSearchSchema.parse({ fecha: "2026-10-25", vista: "anual" }).vista).toBe("dia")
  })

  it("arma la semana de lunes a domingo", () => {
    // 25/10/2026 es domingo: la semana empieza el lunes 19.
    const week = turnViewRange("2026-10-25", "semana")
    expect(week.from).toBe("2026-10-19")
    expect(week.to).toBe("2026-10-25")
    expect(week.days).toHaveLength(7)
  })

  it("completa la grilla mensual con semanas enteras sin superar 42 días", () => {
    // Octubre 2026 empieza jueves y termina sábado.
    const month = turnViewRange("2026-10-25", "mes")
    expect(month.from).toBe("2026-09-28")
    expect(month.to).toBe("2026-11-01")
    expect(month.days.length % 7).toBe(0)
    // Agosto 2026 empieza sábado y tiene 31 días: necesita seis semanas.
    expect(turnViewRange("2026-08-15", "mes").days).toHaveLength(42)
  })

  it("navega por día, semana y mes ajustando fin de mes", () => {
    expect(shiftTurnDate("2026-10-31", "dia", 1)).toBe("2026-11-01")
    expect(shiftTurnDate("2026-10-25", "semana", -1)).toBe("2026-10-18")
    expect(shiftTurnDate("2026-01-31", "mes", 1)).toBe("2026-02-28")
    expect(shiftTurnDate("2026-12-15", "mes", 1)).toBe("2027-01-15")
  })

  it("agrupa los turnos por día de Buenos Aires", () => {
    // 01:30 UTC del 26 sigue siendo 25 en Buenos Aires (UTC-3).
    expect(appointmentDayKey("2026-10-26T01:30:00Z")).toBe("2026-10-25")
  })
})
