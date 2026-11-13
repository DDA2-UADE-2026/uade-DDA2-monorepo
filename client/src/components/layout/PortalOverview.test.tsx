import type { ReactNode } from "react"
import { screen } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { apiHarness } from "@/test/api-harness"
import type { AppointmentResponse, CitizenInterventionPlanSummaryResponse } from "@/generated/types.gen"
import { PortalOverview } from "./PortalOverview"

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#portal">{children}</a>,
}))

let api: ReturnType<typeof apiHarness>
let appointments: AppointmentResponse[]
let plans: CitizenInterventionPlanSummaryResponse[]
let failRequests: boolean

beforeEach(() => {
  appointments = []
  plans = []
  failRequests = false
  api = apiHarness((request) => {
    const path = new URL(request.url).pathname
    if (failRequests) return Response.json({ message: "Servicio temporalmente inaccesible" }, { status: 503 })
    if (path === "/api/citizen/appointments") return Response.json(appointments)
    if (path === "/api/intervention-plans") return Response.json({ content: plans, page: 0, size: 1, totalElements: plans.length, totalPages: plans.length ? 1 : 0 })
    throw new Error(`Unexpected request ${request.method} ${path}`)
  })
})

afterEach(() => api.restore())

describe("PortalOverview", () => {
  it("no aparece ni consulta datos sin permisos de seguimiento", () => {
    const { container } = api.render(<PortalOverview permissions={[]} />)
    expect(container).toBeEmptyDOMElement()
    expect(api.requests).toHaveLength(0)
  })

  it("informa cuando no hay turnos próximos ni planes", async () => {
    api.render(<PortalOverview permissions={["appointments:own:view", "applications:own:interventions:view"]} />)

    expect(await screen.findByText("No tenés próximos turnos programados.")).toBeInTheDocument()
    expect(screen.getByText("Todavía no tenés planes de intervención.")).toBeInTheDocument()
    expect(screen.getByRole("link", { name: /Ver mis turnos/ })).toHaveAttribute("href", "#portal")
    expect(screen.getByRole("link", { name: /Ver mis intervenciones/ })).toHaveAttribute("href", "#portal")
    expect(api.requests.map((request) => new URL(request.url).pathname).sort()).toEqual([
      "/api/citizen/appointments", "/api/intervention-plans",
    ])
  })

  it("muestra el próximo turno y el plan de acompañamiento", async () => {
    appointments = [{
      id: "appointment-1", status: "CONFIRMED", serviceName: "Clínica", centerName: "Centro Norte",
      centerAddress: "Calle 1", professionalName: "Profesional", startsAt: "2099-03-15T10:00:00Z", endsAt: "2099-03-15T11:00:00Z",
    }]
    plans = [{ id: "plan-1", programName: "Acompañamiento familiar", applicationNumber: 73, status: "IN_PROGRESS" }]
    api.render(<PortalOverview permissions={["appointments:own:view", "applications:own:interventions:view"]} />)

    expect(await screen.findByText("Clínica · Centro Norte")).toBeInTheDocument()
    expect(screen.getByText("Acompañamiento familiar")).toBeInTheDocument()
    expect(screen.getByText("En curso")).toBeInTheDocument()
    expect(screen.getByText("Solicitud N.º 73")).toBeInTheDocument()
  })

  it("presenta mensajes de contingencia si fallan ambas consultas", async () => {
    failRequests = true
    api.render(<PortalOverview permissions={["appointments:own:view", "applications:own:interventions:view"]} />)

    expect(await screen.findByText("No pudimos cargar tus turnos. Consultalos desde el listado.")).toBeInTheDocument()
    expect(screen.getByText("No pudimos cargar tus intervenciones. Consultalas desde el listado.")).toBeInTheDocument()
  })
})
