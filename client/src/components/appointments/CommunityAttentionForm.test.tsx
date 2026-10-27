import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { CommunityAttentionForm } from "@/components/appointments/CommunityAttentionForm"
import { client } from "@/generated/client.gen"
import type { ProfessionalAppointmentResponse } from "@/generated/types.gen"

const originalConfig = client.getConfig()
const appointmentId = "96172376-2255-450f-b318-b0707d10f31d"
const saved = vi.fn()
const conflict = vi.fn()
let queryClient: QueryClient
let requests: Request[]
let reply: () => Response

function renderForm(attention?: ProfessionalAppointmentResponse["attention"]) {
  const row: ProfessionalAppointmentResponse = {
    appointment: { id: appointmentId, status: "CONFIRMED", startsAt: "2020-01-01T12:00:00Z" },
    attention,
  }
  render(<QueryClientProvider client={queryClient}>
    <CommunityAttentionForm turnoId={appointmentId} row={row} onSaved={saved} onConflict={conflict} />
  </QueryClientProvider>)
}

describe("registro de atención comunitaria", () => {
  beforeEach(() => {
    queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
    requests = []
    saved.mockReset()
    conflict.mockReset()
    reply = () => Response.json({ result: "AUSENTE", version: 1 })
    client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async (input) => {
      requests.push(input as Request)
      return reply()
    }) })
  })

  afterEach(() => {
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  it("registra ausente sin enviar datos de atención", async () => {
    const user = userEvent.setup()
    renderForm()
    await user.click(screen.getByRole("button", { name: "Ausente" }))
    expect(screen.queryByLabelText("Fecha de atención")).not.toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Registrar resultado" }))
    await waitFor(() => expect(saved).toHaveBeenCalledOnce())
    expect(requests).toHaveLength(1)
    expect(requests[0].method).toBe("POST")
    expect(await requests[0].json()).toEqual({ result: "AUSENTE" })
  })

  it("corrige un atendido a ausente con versión vigente y sin conservar descripción", async () => {
    const user = userEvent.setup()
    renderForm({ result: "ATENDIDO", attendedOn: "2020-01-01", description: "Servicio anterior", version: 4 })
    expect(screen.getByLabelText("Servicio prestado")).toHaveValue("Servicio anterior")
    await user.click(screen.getByRole("button", { name: "Ausente" }))
    await user.click(screen.getByRole("button", { name: "Guardar corrección" }))
    await waitFor(() => expect(saved).toHaveBeenCalledOnce())
    expect(requests[0].method).toBe("PUT")
    expect(await requests[0].json()).toEqual({ result: "AUSENTE", version: 4 })
  })

  it("ante una versión obsoleta pide refrescar antes de corregir", async () => {
    const user = userEvent.setup()
    reply = () => Response.json({ code: "ATTENTION_CHANGED", message: "Versión cambiada" }, { status: 409 })
    renderForm({ result: "AUSENTE", version: 1 })
    await user.click(screen.getByRole("button", { name: "Guardar corrección" }))
    await waitFor(() => expect(conflict).toHaveBeenCalledOnce())
    expect(saved).not.toHaveBeenCalled()
    expect(screen.getByText(/revisá los datos vigentes/i)).toBeInTheDocument()
  })
})
