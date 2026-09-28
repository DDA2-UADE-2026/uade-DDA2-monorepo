import { describe, expect, it } from "vitest"

import {
  APPOINTMENT_TIME_ZONE,
  apiErrorCode,
  appointmentAttemptStorageKey,
  appointmentStatusLabels,
  appointmentToday,
  clearAppointmentAttemptKey,
  formatAppointmentRange,
  formatAppointmentTime,
  getAppointmentAttemptKey,
  isUuid,
  slotFingerprint,
} from "@/lib/appointment-flow"

describe("appointment-flow", () => {
  it("usa la zona municipal como fecha actual", () => {
    // 2026-09-21 02:00 UTC todavía es 2026-09-20 en Buenos Aires.
    expect(appointmentToday(new Date("2026-09-21T02:00:00Z"))).toBe("2026-09-20")
    expect(APPOINTMENT_TIME_ZONE).toBe("America/Argentina/Buenos_Aires")
  })

  it("formatea horarios en la zona municipal aunque el dispositivo esté en otra", () => {
    // 12:00 UTC es 09:00 en Buenos Aires.
    expect(formatAppointmentTime("2026-09-21T12:00:00Z")).toBe("09:00")
    expect(formatAppointmentRange("2026-09-21T12:00:00Z", "2026-09-21T13:00:00Z")).toBe("09:00 – 10:00")
    expect(formatAppointmentTime("no-fecha")).toBe("—")
    expect(formatAppointmentRange(undefined, undefined)).toBe("—")
  })

  it("etiqueta el estado confirmado y valida UUID", () => {
    expect(appointmentStatusLabels.CONFIRMED).toBe("Confirmado")
    expect(isUuid("96172376-2255-450f-b318-b0707d10f31d")).toBe(true)
    expect(isUuid("no-uuid")).toBe(false)
  })

  it("extrae el código estable del error", () => {
    expect(apiErrorCode({ code: "APPOINTMENT_SLOT_UNAVAILABLE" })).toBe("APPOINTMENT_SLOT_UNAVAILABLE")
    expect(apiErrorCode(null)).toBeUndefined()
    expect(apiErrorCode({ code: 409 })).toBeUndefined()
  })

  it("conserva la clave del intento para reintentos y la libera al confirmar", () => {
    sessionStorage.clear()
    const first = getAppointmentAttemptKey(7, "a1", "s", "e")
    expect(getAppointmentAttemptKey(7, "a1", "s", "e")).toBe(first)
    expect(getAppointmentAttemptKey(7, "a1", "s", "otro")).not.toBe(first)
    expect(sessionStorage.getItem(appointmentAttemptStorageKey(7, "a1", "s", "e"))).toBe(first)
    clearAppointmentAttemptKey(7, "a1", "s", "e")
    expect(sessionStorage.getItem(appointmentAttemptStorageKey(7, "a1", "s", "e"))).toBeNull()
    sessionStorage.clear()
  })

  it("distingue slots por asignación y horario", () => {
    const slot = { professionalAssignmentId: "a1", startsAt: "s", endsAt: "e" }
    expect(slotFingerprint(slot)).toBe("a1|s|e")
    expect(slotFingerprint({ ...slot, startsAt: "otro" })).not.toBe(slotFingerprint(slot))
  })
})
