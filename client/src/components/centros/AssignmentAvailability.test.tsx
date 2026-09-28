import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { client } from "@/generated/client.gen"
import type { ErrorResponse, ProfessionalAvailabilityResponse } from "@/generated/types.gen"
import { AssignmentAvailability } from "@/routes/_app/gestion/centros/$centroId/agenda"

vi.mock("@/components/centros/TimeRangeDialog", () => ({
  TimeRangeDialog: ({ error, onSubmit }: {
    error: ErrorResponse | null
    onSubmit: (value: { dayOfWeek: "MONDAY"; days: Array<"MONDAY" | "FRIDAY">; startTime: string; endTime: string }) => void
  }) => (
    <div role="dialog" aria-label="Nueva disponibilidad">
      <button onClick={() => onSubmit({ dayOfWeek: "MONDAY", days: ["MONDAY"], startTime: "10:00", endTime: "12:00" })}>
        Agregar idéntica
      </button>
      <button onClick={() => onSubmit({ dayOfWeek: "MONDAY", days: ["MONDAY"], startTime: "08:00", endTime: "12:00" })}>
        Agregar más amplia
      </button>
      <button onClick={() => onSubmit({ dayOfWeek: "MONDAY", days: ["MONDAY", "FRIDAY"], startTime: "10:00", endTime: "12:00" })}>
        Agregar dos días
      </button>
      {error?.message && <p role="alert">{error.message}</p>}
    </div>
  ),
}))

const assignmentId = "11111111-1111-4111-8111-111111111111"
const availabilityId = "22222222-2222-4222-8222-222222222222"
const originalConfig = client.getConfig()

function range(active: boolean, dayOfWeek: ProfessionalAvailabilityResponse["dayOfWeek"] = "MONDAY"): ProfessionalAvailabilityResponse {
  return {
    id: availabilityId,
    assignmentId,
    dayOfWeek,
    startTime: "10:00:00",
    endTime: "12:00:00",
    active,
  }
}

describe("disponibilidades profesionales en la agenda", () => {
  let queryClient: QueryClient
  let requests: Request[]
  let rows: ProfessionalAvailabilityResponse[]
  let rejectActivation: boolean
  let onChanged: () => void

  beforeEach(() => {
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    requests = []
    rows = []
    rejectActivation = false
    onChanged = vi.fn()
    client.setConfig({
      baseUrl: "http://localhost",
      fetch: vi.fn<typeof fetch>(async (input) => {
        const request = input as Request
        requests.push(request.clone())
        const url = new URL(request.url).pathname
        if (request.method === "GET") return Response.json(rows)
        if (request.method === "PATCH" && url.endsWith("/deactivate")) {
          rows = rows.map((item) => ({ ...item, active: false }))
          return Response.json(rows[0])
        }
        if (request.method === "PATCH" && url.endsWith("/activate")) {
          if (rejectActivation) {
            return Response.json({
              status: 409,
              code: "PROFESSIONAL_AVAILABILITY_OVERLAP",
              message: "La disponibilidad se superpone con otra franja efectiva del profesional.",
            }, { status: 409 })
          }
          rows = rows.map((item) => ({ ...item, active: true }))
          return Response.json(rows[0])
        }
        if (request.method === "POST") {
          const body = await request.json() as { dayOfWeek: "MONDAY"; startTime: string; endTime: string }
          const added = { ...range(true), id: "33333333-3333-4333-8333-333333333333", ...body }
          rows = [...rows, added]
          return Response.json(added, { status: 201 })
        }
        throw new Error(`Petición inesperada: ${request.method} ${url}`)
      }),
    })
  })

  afterEach(() => {
    queryClient.clear()
    client.setConfig(originalConfig)
  })

  function renderAvailability() {
    return render(
      <QueryClientProvider client={queryClient}>
        <AssignmentAvailability assignmentId={assignmentId} professionalName="Viewer User" onChanged={onChanged} />
      </QueryClientProvider>,
    )
  }

  it("mantiene la franja visible tras la baja y permite reactivar la misma fila", async () => {
    rows = [range(true)]
    const user = userEvent.setup()
    renderAvailability()

    const toggle = await screen.findByRole("switch", { name: "Disponibilidad Lunes 10:00–12:00" })
    expect(toggle).toHaveAttribute("aria-checked", "true")
    expect(screen.getByText("10:00–12:00")).toBeInTheDocument()
    await user.click(toggle)
    expect(await screen.findByText("10:00–12:00 (inactiva)")).toBeInTheDocument()
    expect(toggle).toHaveAttribute("aria-checked", "false")

    await user.click(toggle)
    await waitFor(() => expect(toggle).toHaveAttribute("aria-checked", "true"))
    expect(screen.queryByText("10:00–12:00 (inactiva)")).not.toBeInTheDocument()
    expect(screen.getByText("10:00–12:00")).toBeInTheDocument()
    expect(requests.filter((request) => request.method === "PATCH").map((request) => new URL(request.url).pathname)).toEqual([
      `/api/admin/professional-availability/${availabilityId}/deactivate`,
      `/api/admin/professional-availability/${availabilityId}/activate`,
    ])
    expect(requests.some((request) => request.method === "POST")).toBe(false)
    expect(onChanged).toHaveBeenCalledTimes(2)
  })

  it("conserva la franja inactiva si la reactivación choca con otra agenda", async () => {
    rows = [range(false)]
    rejectActivation = true
    const user = userEvent.setup()
    renderAvailability()

    const toggle = await screen.findByRole("switch", { name: "Disponibilidad Lunes 10:00–12:00" })
    expect(toggle).toHaveAttribute("aria-checked", "false")
    await user.click(toggle)
    expect(await screen.findByText("La disponibilidad se superpone con otra franja efectiva del profesional.")).toBeInTheDocument()
    expect(screen.getByText(/también cuentan las de otros servicios y centros/)).toBeInTheDocument()
    expect(screen.getByText("10:00–12:00 (inactiva)")).toBeInTheDocument()
    expect(toggle).toHaveAttribute("aria-checked", "false")
    expect(requests.filter((request) => request.method === "POST")).toHaveLength(0)
  })

  it("sugiere activar la franja idéntica pero permite agregar una más amplia", async () => {
    rows = [range(false)]
    const user = userEvent.setup()
    renderAvailability()
    await screen.findByText("10:00–12:00 (inactiva)")
    await user.click(screen.getByRole("button", { name: "Agregar" }))
    const dialog = screen.getByRole("dialog", { name: "Nueva disponibilidad" })

    await user.click(within(dialog).getByRole("button", { name: "Agregar idéntica" }))
    expect(within(dialog).getByText(/Ya existe una disponibilidad inactiva idéntica para Lunes/)).toBeInTheDocument()
    expect(requests.filter((request) => request.method === "POST")).toHaveLength(0)

    await user.click(within(dialog).getByRole("button", { name: "Agregar más amplia" }))
    await waitFor(() => expect(requests.filter((request) => request.method === "POST")).toHaveLength(1))
    expect(await requests.find((request) => request.method === "POST")!.json()).toMatchObject({
      startTime: "08:00", endTime: "12:00", dayOfWeek: "MONDAY",
    })
    expect(await screen.findByText("08:00–12:00")).toBeInTheDocument()
    expect(screen.getByText("10:00–12:00 (inactiva)")).toBeInTheDocument()
  })

  it("rechaza el alta múltiple completa si uno de los días coincide exactamente con una inactiva", async () => {
    rows = [range(false, "FRIDAY")]
    const user = userEvent.setup()
    renderAvailability()
    await screen.findByText("10:00–12:00 (inactiva)")
    await user.click(screen.getByRole("button", { name: "Agregar" }))
    await user.click(screen.getByRole("button", { name: "Agregar dos días" }))

    expect(screen.getByText(/Ya existe una disponibilidad inactiva idéntica para Viernes/)).toBeInTheDocument()
    expect(requests.some((request) => request.method === "POST")).toBe(false)
  })
})
