import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { client } from "@/generated/client.gen"
import { OpeningHoursSection } from "@/routes/_app/gestion/centros/$centroId/agenda"

const centerId = "11111111-1111-4111-8111-111111111111"
const openingHourId = "22222222-2222-4222-8222-222222222222"
const originalConfig = client.getConfig()

afterEach(() => client.setConfig(originalConfig))

it("muestra el estado de apertura y permite desactivar y reactivar con el switch", async () => {
  let active = true
  const requests: string[] = []
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  client.setConfig({
    baseUrl: "http://localhost",
    fetch: vi.fn<typeof fetch>(async (input) => {
      const request = input as Request
      const path = new URL(request.url).pathname
      requests.push(`${request.method} ${path}`)
      if (request.method === "PATCH") active = path.endsWith("/activate")
      const row = {
        id: openingHourId,
        centerId,
        dayOfWeek: "MONDAY",
        startTime: "08:00:00",
        endTime: "12:00:00",
        active,
      }
      return Response.json(request.method === "GET" ? [row] : row)
    }),
  })

  try {
    const user = userEvent.setup()
    render(<QueryClientProvider client={queryClient}><OpeningHoursSection centroId={centerId} /></QueryClientProvider>)

    const toggle = await screen.findByRole("switch", { name: "Horario de apertura Lunes 08:00–12:00" })
    expect(toggle).toHaveAttribute("aria-checked", "true")
    expect(screen.getByText("08:00–12:00")).toBeInTheDocument()

    await user.click(toggle)
    expect(await screen.findByText("08:00–12:00 (inactiva)")).toBeInTheDocument()
    expect(toggle).toHaveAttribute("aria-checked", "false")

    await user.click(toggle)
    await waitFor(() => expect(toggle).toHaveAttribute("aria-checked", "true"))
    expect(screen.getByText("08:00–12:00")).toBeInTheDocument()
    expect(requests.filter((request) => request.startsWith("PATCH "))).toEqual([
      `PATCH /api/admin/center-opening-hours/${openingHourId}/deactivate`,
      `PATCH /api/admin/center-opening-hours/${openingHourId}/activate`,
    ])
  } finally {
    queryClient.clear()
  }
})
