import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import type { ReactNode } from "react"
import { toast } from "sonner"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { getCitizenAppointmentQueryKey } from "@/generated/@tanstack/react-query.gen"
import { SidebarProvider } from "@/components/ui/sidebar"
import { client } from "@/generated/client.gen"
import type { AvailableAppointmentSlotResponse } from "@/generated/types.gen"
import { appointmentAttemptStorageKey } from "@/lib/appointment-flow"
import { AppointmentConfirmation, AppointmentWizard } from "@/components/appointments/AppointmentWizard"

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))
vi.mock("@tanstack/react-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("@tanstack/react-router")>(),
  useNavigate: () => navigate,
  Link: ({ children }: { children?: ReactNode }) => <a href="#">{children}</a>,
}))

const originalConfig = client.getConfig()
let queryClient: QueryClient
let requests: Request[]
let respond: () => Promise<Response>

const serviceA = { id: "service-a", name: "Clínica", description: "Consulta general", durationMinutes: 60 }
const serviceB = { id: "service-b", name: "Pediatría", description: "Niños", durationMinutes: 30 }
const centerA = { centerServiceId: "cs-a", centerId: "c-a", name: "Centro Norte", address: "Calle 1", phone: undefined, email: undefined }

const slot: AvailableAppointmentSlotResponse = {
  professionalAssignmentId: "11111111-1111-4111-8111-111111111111",
  professionalId: 9,
  professionalName: "Prof Uno",
  startsAt: "2026-09-28T12:00:00-03:00",
  endsAt: "2026-09-28T13:00:00-03:00",
}

function mockFetch() {
  client.setConfig({
    baseUrl: "http://localhost",
    fetch: vi.fn<typeof fetch>(async (input) => {
      requests.push(input as Request)
      return respond()
    }),
  })
}

function renderWizard() {
  return render(<QueryClientProvider client={queryClient}><SidebarProvider><AppointmentWizard userId={7} /></SidebarProvider></QueryClientProvider>)
}

function renderConfirmation(stale = vi.fn()) {
  return render(<QueryClientProvider client={queryClient}><SidebarProvider>
    <AppointmentConfirmation
      userId={7}
      service={serviceA}
      center={centerA}
      date="2026-09-28"
      slot={slot}
      onStaleSlot={stale}
    />
  </SidebarProvider></QueryClientProvider>)
}

describe("wizard de turnos", () => {
  beforeEach(() => {
    vi.spyOn(toast, "error").mockReturnValue("appointment-error")
    navigate.mockReset()
    sessionStorage.clear()
    requests = []
    respond = async () => Response.json({}, { status: 500 })
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    mockFetch()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    queryClient.clear()
    sessionStorage.clear()
    client.setConfig(originalConfig)
  })

  it("avanza por servicio y centro y limpia la selección dependiente", async () => {
    respond = async () => Response.json([serviceA, serviceB])
    const user = userEvent.setup()
    renderWizard()
    await waitFor(() => expect(screen.getByText("Clínica")).toBeInTheDocument())
    expect(screen.queryByText("2. Centro")).not.toBeInTheDocument()

    respond = async () => Response.json([centerA])
    await user.click(within(screen.getByRole("region", { name: "Servicio" })).getAllByRole("button")[0])
    await waitFor(() => expect(screen.getByText("2. Centro")).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText("Centro Norte")).toBeInTheDocument())
    expect(requests.some((request) => new URL(request.url).pathname === "/api/citizen/appointment-services/service-a/centers")).toBe(true)

    await user.click(within(screen.getByRole("region", { name: "Centro" })).getByRole("button", { name: "Elegir" }))
    await waitFor(() => expect(screen.getByText("3. Fecha")).toBeInTheDocument())

    respond = async () => Response.json([])
    await user.click(within(screen.getByRole("region", { name: "Servicio" })).getAllByRole("button")[1])
    await waitFor(() => expect(screen.queryByText("3. Fecha")).not.toBeInTheDocument())
    await waitFor(() => expect(screen.getByText("Sin centros para este servicio")).toBeInTheDocument())
  })

  it("confirma una vez aunque se haga doble clic y navega al detalle", async () => {
    let finish!: (response: Response) => void
    respond = () => new Promise((resolve) => { finish = resolve })
    const user = userEvent.setup()
    renderConfirmation()
    expect(screen.getByText("Centro Norte")).toBeInTheDocument()
    expect(screen.getByText("Prof Uno")).toBeInTheDocument()

    await user.dblClick(screen.getByRole("button", { name: "Confirmar turno" }))
    await waitFor(() => expect(requests).toHaveLength(1))
    expect(screen.getByRole("button", { name: "Confirmando…" })).toBeDisabled()
    expect(requests[0].method).toBe("POST")
    expect(new URL(requests[0].url).pathname).toBe("/api/citizen/appointments")
    expect(await requests[0].json()).toEqual({
      professionalAssignmentId: "11111111-1111-4111-8111-111111111111",
      startsAt: "2026-09-28T12:00:00-03:00",
      endsAt: "2026-09-28T13:00:00-03:00",
    })
    expect(requests[0].headers.get("Idempotency-Key")).toMatch(/^[0-9a-f-]{36}$/)

    finish(Response.json({ id: "turno-1", status: "CONFIRMED" }, { status: 201 }))
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({
      to: "/portal/turnos/$turnoId", params: { turnoId: "turno-1" }, replace: true,
    }))
    expect(queryClient.getQueryData(getCitizenAppointmentQueryKey({ path: { appointmentId: "turno-1" } }))).toMatchObject({ id: "turno-1" })
    expect(sessionStorage.getItem(appointmentAttemptStorageKey(7, "11111111-1111-4111-8111-111111111111", "2026-09-28T12:00:00-03:00", "2026-09-28T13:00:00-03:00"))).toBeNull()
  })

  it("reutiliza la clave luego de un error para no duplicar el turno", async () => {
    respond = async () => Response.json({ status: 500, message: "No pudimos confirmar el turno." }, { status: 500 })
    const user = userEvent.setup()
    renderConfirmation()
    await user.click(screen.getByRole("button", { name: "Confirmar turno" }))
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("No se pudo completar la operación", {
      description: "No pudimos confirmar el turno.",
    }))
    const key = requests[0].headers.get("Idempotency-Key")
    expect(sessionStorage.getItem(appointmentAttemptStorageKey(7, "11111111-1111-4111-8111-111111111111", "2026-09-28T12:00:00-03:00", "2026-09-28T13:00:00-03:00"))).toBe(key)
    respond = async () => Response.json({ id: "turno-1", status: "CONFIRMED" }, { status: 200 })
    await user.click(screen.getByRole("button", { name: "Confirmar turno" }))
    await waitFor(() => expect(navigate).toHaveBeenCalledTimes(1))
    expect(requests).toHaveLength(2)
    expect(requests[1].headers.get("Idempotency-Key")).toBe(key)
  })

  it("ante un horario obsoleto avisa y vuelve a los horarios", async () => {
    const stale = vi.fn()
    respond = async () => Response.json(
      { status: 409, code: "APPOINTMENT_SLOT_UNAVAILABLE", message: "El horario dejó de estar disponible." },
      { status: 409 },
    )
    const user = userEvent.setup()
    renderConfirmation(stale)
    await user.click(screen.getByRole("button", { name: "Confirmar turno" }))
    await waitFor(() => expect(stale).toHaveBeenCalledTimes(1))
  })

  it("ante superposición del ciudadano muestra el mensaje dedicado", async () => {
    respond = async () => Response.json(
      { status: 409, code: "APPOINTMENT_CITIZEN_OVERLAP", message: "Se superpone con otro turno." },
      { status: 409 },
    )
    const user = userEvent.setup()
    renderConfirmation()
    await user.click(screen.getByRole("button", { name: "Confirmar turno" }))
    await waitFor(() => expect(screen.getByText("Ya tenés un turno en ese horario")).toBeInTheDocument())
  })

  it("muestra solo los horarios devueltos con su profesional", async () => {
    respond = async () => Response.json([serviceA])
    const user = userEvent.setup()
    renderWizard()
    await waitFor(() => expect(screen.getByText("Clínica")).toBeInTheDocument())
    // Sin fecha seleccionada no hay consulta de horarios.
    expect(requests.some((request) => new URL(request.url).pathname === "/api/citizen/appointment-slots")).toBe(false)
    expect(screen.queryByText("4. Horario y profesional")).not.toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Elegir" }))
    await waitFor(() => expect(screen.getByText("2. Centro")).toBeInTheDocument())
  })
})
