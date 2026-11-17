import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, expect, it, vi } from "vitest"

import { ActivityDialog } from "@/components/activities/ActivityDialog"
import { client } from "@/generated/client.gen"

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

it("al editar carga el detalle completo y guarda los cambios", async () => {
  const activity = { id: "act-1", name: "Taller de lectura", description: "Lectura compartida", location: "Biblioteca", startDate: "2026-11-20", endDate: "2026-12-20", capacity: 20 }
  const fetchMock = vi.fn<typeof fetch>(async (input) => {
    const request = input as Request
    return request.method === "PUT" ? Response.json({ ...activity, ...(await request.json()) }) : Response.json(activity)
  })
  client.setConfig({ baseUrl: "http://localhost", fetch: fetchMock })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const onSaved = vi.fn()
  const onOpenChange = vi.fn()
  const user = userEvent.setup()
  render(<QueryClientProvider client={queryClient}><ActivityDialog activityId="act-1" onOpenChange={onOpenChange} onSaved={onSaved} /></QueryClientProvider>)

  const name = await screen.findByLabelText("Nombre")
  expect(name).toHaveValue("Taller de lectura")
  await user.clear(name)
  await user.type(name, "Club de lectura")
  fireEvent.submit(document.getElementById("activity-form")!)

  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ id: "act-1", name: "Club de lectura" })))
  expect(onOpenChange).toHaveBeenCalledWith(false)
  const put = fetchMock.mock.calls.map(([input]) => input as Request).find((request) => request.method === "PUT")!
  expect(put.url).toContain("act-1")
})
