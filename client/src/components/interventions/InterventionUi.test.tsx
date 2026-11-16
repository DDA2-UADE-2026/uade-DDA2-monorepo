import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { formatInterventionDate, InterventionProgressSummary, InterventionStatusBadge } from "@/components/interventions/InterventionUi"

describe("InterventionUi", () => {
  it.each([
    ["PENDING", "Pendiente"],
    ["IN_PROGRESS", "En curso"],
    ["COMPLETED", "Completada"],
  ] as const)("traduce el estado %s", (status, label) => {
    render(<InterventionStatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it("formatea fechas ISO sin desplazarlas por zona horaria", () => {
    expect(formatInterventionDate("2026-10-27")).toContain("27")
    expect(formatInterventionDate("2026-10-27")).toContain("2026")
  })

  it("muestra el progreso agregado del plan", () => {
    render(<InterventionProgressSummary completed={2} total={4} compact />)
    expect(screen.getByText("50%")).toBeInTheDocument()
    expect(screen.getByRole("progressbar")).toHaveAttribute("aria-valuenow", "50")
    expect(screen.getByText("2 de 4 acciones")).toBeInTheDocument()
  })
})
