import { describe, expect, it } from "vitest"

import type { ProfessionalAppointmentResponse } from "@/generated/types.gen"
import { attentionLabel, attentionRequest, canRegisterAttention, professionalTurnSearchSchema } from "@/lib/community-attention"

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
