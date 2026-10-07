import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { AdminAttentionSummary } from "@/components/appointments/AdminAttentionSummary"

describe("constancia administrativa", () => {
  it("muestra descripción solo si fue atendido", () => {
    const view = render(<AdminAttentionSummary attention={{
      result: "ATENDIDO", attendedOn: "2026-09-28", description: "Orientación comunitaria",
    }} />)
    expect(screen.getByText("Orientación comunitaria", { exact: false })).toBeInTheDocument()
    expect(screen.getByText("Atendido")).toBeInTheDocument()

    view.rerender(<AdminAttentionSummary attention={{ result: "AUSENTE" }} />)
    expect(screen.getByText("Ausente")).toBeInTheDocument()
    expect(screen.queryByText(/Orientación comunitaria/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Fecha de atención/)).not.toBeInTheDocument()
  })

  it("identifica turnos sin constancia", () => {
    render(<AdminAttentionSummary />)
    expect(screen.getByText("Sin registrar.")).toBeInTheDocument()
  })
})
