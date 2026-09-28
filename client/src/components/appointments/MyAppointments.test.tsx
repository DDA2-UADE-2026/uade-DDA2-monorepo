import { render, screen } from "@testing-library/react"
import type { ReactNode } from "react"
import { describe, expect, it, vi } from "vitest"

import { SidebarProvider } from "@/components/ui/sidebar"
import { MyAppointments } from "@/components/appointments/MyAppointments"
import type { AppointmentResponse } from "@/generated/types.gen"

vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  Link: ({ children }: { children?: ReactNode }) => <a href="#">{children}</a>,
}))

const turno = (overrides: Partial<AppointmentResponse>): AppointmentResponse => ({
  id: "96172376-2255-450f-b318-b0707d10f31d",
  status: "CONFIRMED",
  serviceName: "Clínica",
  centerName: "Centro Norte",
  centerAddress: "Calle 1",
  professionalName: "Prof Uno",
  startsAt: "2026-10-05T12:00:00Z",
  endsAt: "2026-10-05T13:00:00Z",
  ...overrides,
})

describe("mis turnos", () => {
  it("muestra próximos e historial con su estado", () => {
    render(
      <SidebarProvider>
        <MyAppointments
          upcoming={[turno({ id: "proximo-1" })]}
          history={[turno({ id: "cancelado-1", status: "CANCELLED" })]}
        />
      </SidebarProvider>,
    )
    expect(screen.getByText("Próximos")).toBeInTheDocument()
    expect(screen.getByText("Historial")).toBeInTheDocument()
    expect(screen.getByText("Confirmado")).toBeInTheDocument()
    expect(screen.getByText("Cancelado")).toBeInTheDocument()
    expect(screen.getAllByText("Ver detalle")).toHaveLength(2)
  })

  it("informa secciones vacías", () => {
    render(
      <SidebarProvider>
        <MyAppointments upcoming={[]} history={[]} />
      </SidebarProvider>,
    )
    expect(screen.getByText("No tenés próximos turnos.")).toBeInTheDocument()
    expect(screen.getByText("Todavía no hay turnos en tu historial.")).toBeInTheDocument()
  })
})
