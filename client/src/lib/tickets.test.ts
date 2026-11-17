import { describe, expect, it } from "vitest"

import {
  TICKET_STATES,
  formatFileSize,
  isTicketManageable,
  ticketCoreStatusLabel,
  ticketPriorityLabel,
  ticketStateLabel,
  ticketStateLabels,
  ticketStateStyles,
} from "@/lib/tickets"

describe("etiquetas de tickets", () => {
  it("traduce cada estado de M8 y tiene un estilo para cada uno", () => {
    for (const state of TICKET_STATES) {
      expect(ticketStateLabel(state)).toBe(ticketStateLabels[state])
      expect(ticketStateStyles[state]).toBeTruthy()
    }
  })

  it("muestra el valor tal cual si no es conocido y un guion si falta", () => {
    expect(ticketStateLabel("OTRO")).toBe("OTRO")
    expect(ticketStateLabel()).toBe("—")
    expect(ticketCoreStatusLabel("ROUTED")).toBe("Derivado")
    expect(ticketCoreStatusLabel("ARCHIVED")).toBe("ARCHIVED")
    expect(ticketCoreStatusLabel(undefined)).toBe("—")
    expect(ticketPriorityLabel("URGENT")).toBe("Urgente")
    expect(ticketPriorityLabel("P0")).toBe("P0")
    expect(ticketPriorityLabel("")).toBe("—")
  })
})

describe("isTicketManageable", () => {
  it("solo permite gestionar tickets activos del área M8", () => {
    expect(isTicketManageable({ state: "RECEIVED", responsibleAreaId: "M8" })).toBe(true)
    expect(isTicketManageable({ state: "IN_PROGRESS", responsibleAreaId: "M8" })).toBe(true)
    expect(isTicketManageable({ state: "IN_PROGRESS", responsibleAreaId: "M3" })).toBe(false)
    expect(isTicketManageable({ responsibleAreaId: "M8" })).toBe(false)
    for (const state of ["RESOLVED", "RETURNED", "CLOSED"] as const) {
      expect(isTicketManageable({ state, responsibleAreaId: "M8" })).toBe(false)
    }
  })
})

describe("formatFileSize", () => {
  it("usa la unidad más legible", () => {
    expect(formatFileSize(undefined)).toBeNull()
    expect(formatFileSize(0)).toBe("0 B")
    expect(formatFileSize(1023)).toBe("1023 B")
    expect(formatFileSize(1536)).toBe("1.5 KB")
    expect(formatFileSize(5 * 1024 * 1024)).toBe("5.0 MB")
  })
})
