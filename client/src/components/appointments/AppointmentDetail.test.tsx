import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { SidebarProvider } from "@/components/ui/sidebar"
import { client } from "@/generated/client.gen"
import { AppointmentDetail } from "@/components/appointments/AppointmentDetail"

function renderDetail(turnoId: string) {
  return render(<QueryClientProvider client={queryClient}><SidebarProvider><AppointmentDetail turnoId={turnoId} /></SidebarProvider></QueryClientProvider>)
}

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#">{children}</a>,
}))

const originalConfig = client.getConfig()
let queryClient: QueryClient
let requests: Request[]
let respond: () => Promise<Response>

const detail = {
  id: "96172376-2255-450f-b318-b0707d10f31d",
  status: "CONFIRMED",
  serviceName: "Clínica",
  centerName: "Centro Norte",
  centerAddress: "Calle 1",
  professionalName: "Prof Uno",
  startsAt: "2026-09-28T12:00:00Z",
  endsAt: "2026-09-28T13:00:00Z",
}

describe("detalle de confirmación", () => {
  beforeEach(() => {
    requests = []
    respond = async () => Response.json(detail)
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    client.setConfig({
      baseUrl: "http://localhost",
      fetch: vi.fn<typeof fetch>(async (input) => {
        requests.push(input as Request)
        return respond()
      }),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  it("muestra la confirmación en hora de Buenos Aires", async () => {
    renderDetail("96172376-2255-450f-b318-b0707d10f31d")
    await waitFor(() => expect(screen.getByText("Clínica")).toBeInTheDocument())
    expect(screen.getByText("Confirmado")).toBeInTheDocument()
    expect(screen.getByText("Centro Norte", { exact: false })).toBeInTheDocument()
    expect(screen.getByText("Prof Uno", { exact: false })).toBeInTheDocument()
    // 12:00 UTC es 09:00 en Buenos Aires.
    expect(screen.getByText("09:00 – 10:00", { exact: false })).toBeInTheDocument()
    expect(screen.getByText(/96172376-2255-450f-b318-b0707d10f31d/)).toBeInTheDocument()
    expect(new URL(requests[0].url).pathname).toBe("/api/citizen/appointments/96172376-2255-450f-b318-b0707d10f31d")
  })

  it("rechaza un identificador inválido sin consultar", () => {
    renderDetail("no-uuid")
    expect(screen.getByText("El identificador no es válido.")).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })

  it("muestra no encontrado para un turno ajeno con reintento", async () => {
    respond = async () => Response.json({ status: 404, code: "APPOINTMENT_RESOURCE_NOT_FOUND", message: "No existe." }, { status: 404 })
    renderDetail("96172376-2255-450f-b318-b0707d10f31d")
    await waitFor(() => expect(screen.getByText("No encontramos ese turno")).toBeInTheDocument())
    expect(screen.getByRole("button", { name: "Reintentar" })).toBeInTheDocument()
  })
})
