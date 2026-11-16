import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { AppointmentCenterSelectionDialog, AppointmentServiceSelectionDialog } from "@/components/turnos/AppointmentSelectionDialogs"
import { client } from "@/generated/client.gen"

const originalConfig = client.getConfig()
const centerId = "11111111-1111-4111-8111-111111111111"

afterEach(() => {
  client.setConfig(originalConfig)
  vi.restoreAllMocks()
})

function renderPicker(element: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{element}</QueryClientProvider>)
}

it("permite seleccionar un centro de una página posterior", async () => {
  const onSelect = vi.fn()
  const requestedPages: number[] = []
  client.setConfig({
    baseUrl: "http://localhost",
    fetch: vi.fn<typeof fetch>(async (input) => {
      const url = new URL((input as Request).url)
      const page = Number(url.searchParams.get("page"))
      requestedPages.push(page)
      return Response.json({
        page,
        totalPages: 2,
        totalElements: 9,
        content: page === 0
          ? Array.from({ length: 8 }, (_, index) => ({ id: `centro-${index + 1}`, name: `Centro ${index + 1}` }))
          : [{ id: centerId, name: "Centro 9" }],
      })
    }),
  })

  const user = userEvent.setup()
  renderPicker(<AppointmentCenterSelectionDialog open onOpenChange={vi.fn()} onSelect={onSelect} selectedCenterId="" />)
  await screen.findByText("Centro 1")
  await user.click(screen.getByText("Siguiente"))
  const row = (await screen.findByText("Centro 9")).closest("tr")
  expect(row).not.toBeNull()
  await user.click(within(row!).getByRole("button", { name: "Seleccionar" }))

  expect(requestedPages).toEqual([0, 1])
  expect(onSelect).toHaveBeenCalledWith(centerId, expect.objectContaining({ id: centerId, name: "Centro 9" }))
})

it("pagina los servicios del centro y permite seleccionar el último", async () => {
  const onSelect = vi.fn()
  client.setConfig({
    baseUrl: "http://localhost",
    fetch: vi.fn<typeof fetch>(async () => Response.json(
      Array.from({ length: 9 }, (_, index) => ({
        id: `asignacion-${index + 1}`,
        active: true,
        service: { id: `servicio-${index + 1}`, name: `Servicio ${index + 1}` },
      })),
    )),
  })

  const user = userEvent.setup()
  renderPicker(<AppointmentServiceSelectionDialog open onOpenChange={vi.fn()} onSelect={onSelect} centerId={centerId} selectedServiceId="" />)
  await screen.findByText("Servicio 1")
  await user.click(screen.getByText("Siguiente"))
  const row = (await screen.findByText("Servicio 9")).closest("tr")
  expect(row).not.toBeNull()
  await user.click(within(row!).getByRole("button", { name: "Seleccionar" }))

  expect(onSelect).toHaveBeenCalledWith("servicio-9")
})
