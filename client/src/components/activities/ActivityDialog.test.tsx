import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { ActivityDialog } from "@/components/activities/ActivityDialog"
import { client } from "@/generated/client.gen"

vi.mock("@/components/programs/ProgramDatePicker", () => ({
  ProgramDatePicker: ({ value, onChange }: { value: string; onChange: (value: string) => void }) => (
    <input aria-label="date" type="date" value={value ?? ""} onChange={(event) => onChange(event.target.value)} />
  ),
}))

const originalConfig = client.getConfig()
afterEach(() => { client.setConfig(originalConfig); vi.restoreAllMocks() })

it("permite elegir un centro y editar el lugar manualmente", async () => {
  client.setConfig({ baseUrl: "http://localhost", fetch: vi.fn<typeof fetch>(async () => Response.json({ content: [{ id: "centro-1", name: "Centro Norte", address: "Calle 123" }], totalElements: 1, totalPages: 1 })) })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const user = userEvent.setup()
  render(<QueryClientProvider client={queryClient}><ActivityDialog onOpenChange={vi.fn()} /></QueryClientProvider>)
  await user.click(screen.getByRole("button", { name: "Seleccionar centro existente" }))
  const row = (await screen.findByText("Centro Norte")).closest("tr")!
  await user.click(within(row).getByRole("button", { name: "Seleccionar" }))
  const location = screen.getByLabelText("Lugar")
  expect(location).toHaveValue("Centro Norte · Calle 123")
  await user.clear(location)
  await user.type(location, "Plaza del barrio")
  expect(location).toHaveValue("Plaza del barrio")
})

it("valida y crea una actividad como borrador con los campos normalizados", async () => {
  const saved = vi.fn()
  const closed = vi.fn()
  const requests: { url: string; method: string; body: string }[] = []
  const fetchMock = vi.fn<typeof fetch>(async (input) => {
    const request = input instanceof Request ? input.clone() : new Request(input)
    requests.push({ url: request.url, method: request.method, body: await request.text() })
    return Response.json({
      id: "activity-1", name: "Taller de RCP", description: "Capacitación comunitaria", location: "Centro Norte",
      startDate: "2099-03-15", endDate: "2099-03-16", capacity: 25, status: "DRAFT",
    }, { status: 201 })
  })
  client.setConfig({ baseUrl: "http://localhost", fetch: fetchMock })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const user = userEvent.setup()
  render(<QueryClientProvider client={queryClient}><ActivityDialog onOpenChange={closed} onSaved={saved} /></QueryClientProvider>)

  const submit = screen.getByRole("button", { name: "Crear actividad" })
  await user.click(submit)
  expect(fetchMock).not.toHaveBeenCalled()
  await user.type(screen.getByLabelText("Nombre"), "  Taller de RCP  ")
  await user.type(screen.getByLabelText("Descripción"), "  Capacitación comunitaria  ")
  await user.type(screen.getByLabelText("Lugar"), "  Centro Norte  ")
  const dates = screen.getAllByLabelText("date")
  fireEvent.change(dates[0], { target: { value: "2099-03-15" } })
  fireEvent.change(dates[1], { target: { value: "2099-03-16" } })
  await user.type(screen.getByLabelText("Cupo"), "25")
  expect(submit).toBeEnabled()

  await user.click(submit)
  await screen.findByRole("button", { name: "Crear actividad" })
  await vi.waitFor(() => expect(saved).toHaveBeenCalledOnce())
  expect(closed).toHaveBeenCalledWith(false)
  expect(fetchMock).toHaveBeenCalledOnce()
  expect(requests[0].url).toContain("/api/admin/activities")
  expect(requests[0].method).toBe("POST")
  expect(JSON.parse(requests[0].body)).toEqual({
    name: "Taller de RCP", description: "Capacitación comunitaria", location: "Centro Norte",
    startDate: "2099-03-15", endDate: "2099-03-16", capacity: 25,
  })
})

it("rechaza un rango de fechas invertido y un cupo no positivo", async () => {
  const fetchMock = vi.fn<typeof fetch>()
  client.setConfig({ baseUrl: "http://localhost", fetch: fetchMock })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const user = userEvent.setup()
  render(<QueryClientProvider client={queryClient}><ActivityDialog onOpenChange={vi.fn()} /></QueryClientProvider>)

  await user.type(screen.getByLabelText("Nombre"), "Taller")
  await user.type(screen.getByLabelText("Descripción"), "Descripción")
  await user.type(screen.getByLabelText("Lugar"), "Centro")
  const dates = screen.getAllByLabelText("date")
  fireEvent.change(dates[0], { target: { value: "2099-03-16" } })
  fireEvent.change(dates[1], { target: { value: "2099-03-15" } })
  const capacity = screen.getByLabelText("Cupo")
  await user.type(capacity, "0")
  fireEvent.blur(dates[0])
  fireEvent.blur(dates[1])
  fireEvent.blur(capacity)

  expect(await screen.findByText("La fecha de finalización no puede ser anterior a la de inicio.")).toBeInTheDocument()
  expect(await screen.findByText("El cupo debe ser mayor a cero.")).toBeInTheDocument()
  expect(screen.getByRole("button", { name: "Crear actividad" })).toBeDisabled()
  expect(fetchMock).not.toHaveBeenCalled()
})
